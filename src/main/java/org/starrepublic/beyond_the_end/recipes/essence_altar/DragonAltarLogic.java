package org.starrepublic.beyond_the_end.recipes.essence_altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import org.starrepublic.beyond_the_end.Entitys.blockentity.DragonEssenceAltarBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DragonAltarLogic {

    private static final String ITEM_DATA =
            "beyond_the_end_dragon_altar";

    private static final int PEDESTAL_INTERVAL = 20;
    private static final int ITEM_INTERVAL = 4;

    private DragonAltarLogic() {
    }

    // ========================================================
    // Main ticker
    // ========================================================

    public static void tick(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar,
            int ticks
    ) {

        if (ticks == 1 || ticks % PEDESTAL_INTERVAL == 0) {
            refreshPedestals(level, altar);
        }

        if (ticks % ITEM_INTERVAL == 0) {

            for (BlockPos pedestal : altar.getDragonPedestals()) {
                processPedestal(level, altar, pedestal);
            }
        }
    }

    // ========================================================
    // Pedestal detection
    // ========================================================

    private static void refreshPedestals(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar
    ) {

        BlockPos center = altar.getBlockPos();

        int range = altar.isDragonRangeUpgraded() ? 4 : 3;
        int maximum = altar.isDragonRangeUpgraded() ? 12 : 8;

        Map<Block, List<BlockPos>> candidates = new HashMap<>();

        // 不強制載入尚未載入的區塊
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {

                BlockPos pos = center.offset(x, 0, z);

                if (!level.hasChunkAt(pos)) {
                    return;
                }
            }
        }

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {

                // 祭壇周圍的一圈不允許作為基座
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    continue;
                }

                BlockPos pos = center.offset(x, 0, z);

                if (!isValidPedestal(level, pos)) {
                    continue;
                }

                Block block = level.getBlockState(pos).getBlock();

                candidates
                        .computeIfAbsent(
                                block,
                                ignored -> new ArrayList<>()
                        )
                        .add(pos.immutable());
            }
        }

        // 優先選取數量最多且沒有超過上限的同類基座
        List<BlockPos> selected = candidates.entrySet()
                .stream()
                .filter(entry ->
                        entry.getValue().size() <= maximum
                )
                .sorted(
                        Comparator
                                .<Map.Entry<Block, List<BlockPos>>>
                                        comparingInt(
                                        entry -> entry.getValue().size()
                                )
                                .reversed()
                                .thenComparing(
                                        entry -> BuiltInRegistries.BLOCK
                                                .getKey(entry.getKey())
                                                .toString()
                                )
                )
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(List::of);

        altar.setDragonPedestals(selected);

        // 若原本使用的基座不再有效，釋放上面的物品
        releaseInvalidItems(level, altar);
    }

    private static boolean isValidPedestal(
            ServerLevel level,
            BlockPos pos
    ) {

        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return false;
        }

        // 基座必須有碰撞體積
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }

        // 基座上方必須為空氣
        if (!level.isEmptyBlock(pos.above())) {
            return false;
        }

        // 水平四個方向必須沒有碰撞箱
        for (Direction direction : Direction.Plane.HORIZONTAL) {

            BlockPos adjacent = pos.relative(direction);

            if (!level.getBlockState(adjacent)
                    .getCollisionShape(level, adjacent)
                    .isEmpty()) {
                return false;
            }
        }

        return true;
    }

    // ========================================================
    // Item processing
    // ========================================================

    private static void processPedestal(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar,
            BlockPos pedestal
    ) {

        double x = pedestal.getX() + 0.5D;
        double y = pedestal.getY() + 1.15D;
        double z = pedestal.getZ() + 0.5D;

        AABB area = new AABB(
                x - 0.30D, y - 0.30D, z - 0.30D,
                x + 0.30D, y + 0.35D, z + 0.30D
        );

        List<ItemEntity> items =
                level.getEntitiesOfClass(ItemEntity.class, area);

        ItemEntity activeItem = null;

        // 優先尋找已經被這個祭壇固定的物品
        for (ItemEntity item : items) {

            if (isOwnedBy(item, altar.getBlockPos(), pedestal)) {
                activeItem = item;
                break;
            }
        }

        // 基座目前沒有正在注魔的物品
        if (activeItem == null) {

            for (ItemEntity item : items) {

                if (!item.isAlive() || item.getItem().isEmpty()) {
                    continue;
                }

                // 不能拿走其他祭壇正在處理的物品
                if (isClaimed(item)) {
                    continue;
                }

                Optional<RecipeHolder<EssenceAltarRecipe>> recipe =
                        altar.findMatchingRecipe(
                                List.of(item.getItem())
                        );

                if (recipe.isEmpty()) {
                    continue;
                }

                activeItem = claimItem(
                        level,
                        altar,
                        pedestal,
                        item
                );

                if (activeItem != null) {
                    break;
                }
            }
        }

        if (activeItem == null) {
            return;
        }

        if (!activeItem.isAlive() || activeItem.getItem().isEmpty()) {
            return;
        }

        // 如果其他機制把材料合併成多件，將多餘的部分分離
        if (!separateExtraItems(level, activeItem)) {
            return;
        }

        // 在沒有足夠精華時也維持基座上的位置
        stabilizeItem(activeItem, pedestal);

        Optional<RecipeHolder<EssenceAltarRecipe>> found =
                altar.findMatchingRecipe(
                        List.of(activeItem.getItem())
                );

        // 資料包被修改、材料不再匹配等情況
        if (found.isEmpty()) {
            releaseItem(activeItem);
            return;
        }

        if (altar.getEssence() <= 0) {
            return;
        }

        // HEE 的物品轉化不是每次更新都會推進
        if (level.random.nextInt(5) != 0) {
            return;
        }

        RecipeHolder<EssenceAltarRecipe> holder = found.get();
        EssenceAltarRecipe recipe = holder.value();

        CompoundTag data = activeItem
                .getPersistentData()
                .getCompound(ITEM_DATA);

        // 配方重新載入或發生切換時清除舊進度
        String recipeId = holder.id().toString();

        if (!recipeId.equals(data.getString("Recipe"))) {

            data.putString("Recipe", recipeId);
            data.putInt("Progress", 0);
        }

        int progress = data.getInt("Progress");

        int requiredCost = recipe.getCost();

        // 效率升級：降低精華消耗
        if (altar.isDragonEfficiencyUpgraded()) {

            requiredCost = Math.max(
                    recipe.getCost() >> 1,
                    (int) Math.ceil(recipe.getCost() * 0.65D)
            );
        }

        requiredCost = Math.max(1, requiredCost);

        int operations = altar.isDragonSpeedUpgraded() ? 2 : 1;

        for (int i = 0; i < operations; i++) {

            if (progress >= requiredCost) {
                break;
            }

            // 每次推進消耗一點精華
            if (!altar.consumeEssence(1)) {
                break;
            }

            progress++;

            data.putInt("Progress", progress);
            activeItem.getPersistentData().put(ITEM_DATA, data);

            spawnProgressParticles(level, pedestal);

            if (progress >= requiredCost) {

                EssenceAltarRecipeInput input =
                        new EssenceAltarRecipeInput(
                                "dragon",
                                List.of(activeItem.getItem().copy())
                        );

                ItemStack result = recipe.assemble(
                        input,
                        level.registryAccess()
                );

                if (result.isEmpty()) {
                    releaseItem(activeItem);
                    return;
                }

                // 一份材料只執行一次轉化
                activeItem.setItem(result);

                releaseItem(activeItem);

                activeItem.setDeltaMovement(
                        new Vec3(0.0D, 0.12D, 0.0D)
                );

                spawnCompletionParticles(level, pedestal);
                return;
            }
        }
    }

    // ========================================================
    // Claiming and splitting items
    // ========================================================

    private static ItemEntity claimItem(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar,
            BlockPos pedestal,
            ItemEntity original
    ) {

        ItemStack stack = original.getItem();

        if (stack.isEmpty()) {
            return null;
        }

        ItemEntity claimed;

        if (stack.getCount() > 1) {

            ItemStack single = stack.copyWithCount(1);

            ItemEntity separated = new ItemEntity(
                    level,
                    pedestal.getX() + 0.5D,
                    pedestal.getY() + 1.15D,
                    pedestal.getZ() + 0.5D,
                    single
            );

            separated.setDeltaMovement(Vec3.ZERO);

            // 先確定新實體成功生成，才減少原有物品數量
            if (!level.addFreshEntity(separated)) {
                return null;
            }

            original.setItem(
                    stack.copyWithCount(stack.getCount() - 1)
            );

            claimed = separated;

        } else {
            claimed = original;
        }

        CompoundTag data = new CompoundTag();

        data.putLong("Owner", altar.getBlockPos().asLong());
        data.putLong("Pedestal", pedestal.asLong());
        data.putInt("Progress", 0);
        data.putString("Recipe", "");

        claimed.getPersistentData().put(ITEM_DATA, data);

        claimed.setNoGravity(true);
        claimed.setDeltaMovement(Vec3.ZERO);
        claimed.setPickUpDelay(50);

        claimed.setExtendedLifetime();

        stabilizeItem(claimed, pedestal);

        return claimed;
    }

    private static boolean separateExtraItems(
            ServerLevel level,
            ItemEntity item
    ) {

        ItemStack stack = item.getItem();

        if (stack.getCount() <= 1) {
            return true;
        }

        ItemStack excess = stack.copyWithCount(
                stack.getCount() - 1
        );

        ItemEntity extra = new ItemEntity(
                level,
                item.getX() + 0.8D,
                item.getY() + 0.3D,
                item.getZ() + 0.8D,
                excess
        );

        extra.setDeltaMovement(
                new Vec3(0.12D, 0.10D, 0.12D)
        );

        if (!level.addFreshEntity(extra)) {
            return false;
        }

        item.setItem(stack.copyWithCount(1));
        return true;
    }

    private static void stabilizeItem(
            ItemEntity item,
            BlockPos pedestal
    ) {

        item.setNoGravity(true);
        item.setDeltaMovement(Vec3.ZERO);

        item.setPos(
                pedestal.getX() + 0.5D,
                pedestal.getY() + 1.15D,
                pedestal.getZ() + 0.5D
        );

        // 持續注魔期間避免普通物品壽命耗盡
        item.setExtendedLifetime();
    }

    // ========================================================
    // Owner validation
    // ========================================================

    private static boolean isClaimed(ItemEntity item) {
        return item.getPersistentData()
                .contains(ITEM_DATA, Tag.TAG_COMPOUND);
    }

    private static boolean isOwnedBy(
            ItemEntity item,
            BlockPos altar,
            BlockPos pedestal
    ) {

        if (!isClaimed(item)) {
            return false;
        }

        CompoundTag data = item.getPersistentData()
                .getCompound(ITEM_DATA);

        return data.getLong("Owner") == altar.asLong()
                && data.getLong("Pedestal") == pedestal.asLong();
    }

    // ========================================================
    // Release and cleanup
    // ========================================================

    private static void releaseItem(ItemEntity item) {

        item.getPersistentData().remove(ITEM_DATA);

        item.setNoGravity(false);
        item.setNoPickUpDelay();
    }

    private static void releaseInvalidItems(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar
    ) {

        BlockPos center = altar.getBlockPos();

        AABB area = new AABB(center).inflate(
                5.5D, 2.5D, 5.5D
        );

        for (ItemEntity item :
                level.getEntitiesOfClass(ItemEntity.class, area)) {

            if (!isClaimed(item)) {
                continue;
            }

            CompoundTag data = item.getPersistentData()
                    .getCompound(ITEM_DATA);

            if (data.getLong("Owner") != center.asLong()) {
                continue;
            }

            BlockPos pedestal = BlockPos.of(
                    data.getLong("Pedestal")
            );

            if (!altar.getDragonPedestals().contains(pedestal)) {
                releaseItem(item);
            }
        }
    }

    public static void releaseAll(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar
    ) {

        BlockPos center = altar.getBlockPos();

        AABB area = new AABB(center).inflate(
                5.5D, 2.5D, 5.5D
        );

        for (ItemEntity item :
                level.getEntitiesOfClass(ItemEntity.class, area)) {

            if (!isClaimed(item)) {
                continue;
            }

            CompoundTag data = item.getPersistentData()
                    .getCompound(ITEM_DATA);

            if (data.getLong("Owner") == center.asLong()) {
                releaseItem(item);
            }
        }

        altar.setDragonPedestals(List.of());
    }

    // ========================================================
    // Effects
    // ========================================================

    private static void spawnProgressParticles(
            ServerLevel level,
            BlockPos pedestal
    ) {

        level.sendParticles(
                ParticleTypes.PORTAL,
                pedestal.getX() + 0.5D,
                pedestal.getY() + 1.2D,
                pedestal.getZ() + 0.5D,
                2,
                0.15D,
                0.15D,
                0.15D,
                0.025D
        );
    }

    private static void spawnCompletionParticles(
            ServerLevel level,
            BlockPos pedestal
    ) {

        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                pedestal.getX() + 0.5D,
                pedestal.getY() + 1.15D,
                pedestal.getZ() + 0.5D,
                35,
                0.35D,
                0.40D,
                0.35D,
                0.10D
        );
    }
}

