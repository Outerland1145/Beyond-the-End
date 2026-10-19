package org.starrepublic.beyond_the_end.recipes.essence_altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Entitys.blockentity.AltarPedestalBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.DragonEssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class DragonAltarLogic {

    private static final int PEDESTAL_INTERVAL = 20;
    private static final int PROCESS_INTERVAL = 4;

    private DragonAltarLogic() {
    }

    // ========================================================
    // 主 Tick
    // ========================================================

    public static void tick(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar,
            int ticks
    ) {

        if (ticks == 1 || ticks % PEDESTAL_INTERVAL == 0) {
            refreshPedestals(level, altar);
        }

        if (ticks % PROCESS_INTERVAL != 0) {
            return;
        }

        List<AltarPedestalBlockEntity> inputs = new ArrayList<>();

        for (BlockPos pos : altar.getDragonPedestals()) {

            AltarPedestalBlockEntity pedestal =
                    getPedestal(level, pos);

            if (pedestal == null
                    || !pedestal.isInputMode()
                    || pedestal.isEmpty()) {
                continue;
            }

            if (!isAvailableToAltar(level, altar, pedestal)) {
                continue;
            }

            // 只有找到配方的輸入基座才需要輸出位置
            if (altar.findMatchingRecipe(pedestal.getItem()).isEmpty()) {
                continue;
            }

            inputs.add(pedestal);
        }

        // 沒有任何可執行配方，不顯示輸出失敗
        if (inputs.isEmpty()) {
            altar.setRecipeFailure("");
            return;
        }

        // ====================================================
        // 執行前檢查輸出基座
        // ====================================================

        String status = checkOutputStatus(level, altar);

        if (!status.isEmpty()) {

            altar.setRecipeFailure(status);

            // 不消耗精華，不清空原有注魔進度
            return;
        }

        altar.setRecipeFailure("");

        // ====================================================
        // 正式執行注魔
        // ====================================================

        for (AltarPedestalBlockEntity pedestal : inputs) {

            // 執行上一份配方後，空位可能已經被占用
            String currentStatus = checkOutputStatus(level, altar);

            if (!currentStatus.isEmpty()) {
                altar.setRecipeFailure(currentStatus);
                return;
            }

            processPedestal(level, altar, pedestal);
        }
    }

    // ========================================================
    // 基座取得
    // ========================================================

    @Nullable
    private static AltarPedestalBlockEntity getPedestal(
            ServerLevel level,
            BlockPos pos
    ) {

        if (!level.hasChunkAt(pos)) {
            return null;
        }

        if (!level.getBlockState(pos).is(
                BlockRegister.ALTAR_PEDESTAL.get()
        )) {
            return null;
        }

        BlockEntity entity = level.getBlockEntity(pos);

        return entity instanceof AltarPedestalBlockEntity pedestal
                ? pedestal
                : null;
    }

    // ========================================================
    // 掃描 7×7 / 9×9 內的所有專用基座
    // ========================================================

    private static void refreshPedestals(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar
    ) {

        BlockPos center = altar.getBlockPos();

        int range = altar.isDragonRangeUpgraded() ? 4 : 3;

        List<BlockPos> candidates = new ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {

                if (x == 0 && z == 0) {
                    continue;
                }

                BlockPos pos = center.offset(x, 0, z);

                if (!level.hasChunkAt(pos)) {
                    continue;
                }

                if (!level.getBlockState(pos).is(
                        BlockRegister.ALTAR_PEDESTAL.get()
                )) {
                    continue;
                }

                if (!(level.getBlockEntity(pos)
                        instanceof AltarPedestalBlockEntity)) {
                    continue;
                }

                candidates.add(pos.immutable());
            }
        }

        candidates.sort(
                Comparator
                        .comparingDouble(
                                (BlockPos pos) -> pos.distSqr(center)
                        )
                        .thenComparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getZ)
        );

        // 解除已離開範圍的基座
        for (BlockPos oldPos : altar.getDragonPedestals()) {

            if (candidates.contains(oldPos)) {
                continue;
            }

            if (!level.hasChunkAt(oldPos)) {
                continue;
            }

            if (level.getBlockEntity(oldPos)
                    instanceof AltarPedestalBlockEntity pedestal) {

                pedestal.release(center);
            }
        }

        altar.setDragonPedestals(candidates);
    }
    private static String checkOutputStatus(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar
    ) {

        boolean hasOutput = false;
        boolean hasFreeOutput = false;

        for (BlockPos pos : altar.getDragonPedestals()) {

            AltarPedestalBlockEntity pedestal =
                    getPedestal(level, pos);

            if (pedestal == null || !pedestal.isOutputMode()) {
                continue;
            }

            hasOutput = true;

            if (pedestal.isEmpty()
                    && isAvailableToAltar(level, altar, pedestal)) {

                hasFreeOutput = true;
                break;
            }
        }

        if (!hasOutput) {
            return "no_output";
        }

        if (!hasFreeOutput) {
            return "output_full";
        }

        return "";
    }

    // ========================================================
    // 檢查另一座祭壇是否占用基座
    // ========================================================

    private static boolean isAvailableToAltar(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar,
            AltarPedestalBlockEntity pedestal
    ) {

        BlockPos owner = pedestal.getOwnerAltar();

        if (owner == null || owner.equals(altar.getBlockPos())) {
            return true;
        }

        // 不強制載入其他祭壇所處區塊
        if (!level.hasChunkAt(owner)) {
            return false;
        }

        BlockEntity entity = level.getBlockEntity(owner);

        if (entity instanceof EssenceAltarBlockEntity) {
            return false;
        }

        // 已經不存在的祭壇
        pedestal.release(owner);

        return true;
    }

    // ========================================================
    // 尋找可用的空輸出基座
    // ========================================================

    @Nullable
    private static AltarPedestalBlockEntity findEmptyPedestal(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar,
            BlockPos inputPos
    ) {

        List<AltarPedestalBlockEntity> available =
                new ArrayList<>();

        for (BlockPos pos : altar.getDragonPedestals()) {

            if (pos.equals(inputPos)) {
                continue;
            }

            AltarPedestalBlockEntity pedestal =
                    getPedestal(level, pos);

            if (pedestal == null) {
                continue;
            }

            // 必須是輸出狀態
            if (!pedestal.isOutputMode()) {
                continue;
            }

            // 必須完全沒有物品
            if (!pedestal.isEmpty()) {
                continue;
            }

            if (!isAvailableToAltar(level, altar, pedestal)) {
                continue;
            }

            available.add(pedestal);
        }

        if (available.isEmpty()) {
            return null;
        }

        available.sort(
                Comparator
                        .comparingDouble(
                                (AltarPedestalBlockEntity pedestal) ->
                                        pedestal.getBlockPos().distSqr(inputPos)
                        )
                        .thenComparingInt(
                                pedestal -> pedestal.getBlockPos().getX()
                        )
                        .thenComparingInt(
                                pedestal -> pedestal.getBlockPos().getZ()
                        )
        );

        return available.get(0);
    }

    // ========================================================
    // 配方執行
    // ========================================================

    private static void processPedestal(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar,
            AltarPedestalBlockEntity inputPedestal
    ) {
        // 只有輸入模式可執行配方
        if (!inputPedestal.isInputMode()) {
            return;
        }
        BlockPos altarPos = altar.getBlockPos();

        if (!isAvailableToAltar(level, altar, inputPedestal)) {
            return;
        }

        ItemStack inputStack = inputPedestal.getItem();

        if (inputStack.isEmpty()) {

            inputPedestal.release(altarPos);
            return;
        }

        // 取得資料包配方
        Optional<RecipeHolder<EssenceAltarRecipe>> found =
                altar.findMatchingRecipe(inputStack);

        if (found.isEmpty()) {

            inputPedestal.release(altarPos);
            return;
        }

        RecipeHolder<EssenceAltarRecipe> holder = found.get();
        EssenceAltarRecipe recipe = holder.value();

        // ====================================================
        // 先檢查空輸出基座
        // ====================================================

        AltarPedestalBlockEntity outputPedestal =
                findEmptyPedestal(
                        level,
                        altar,
                        inputPedestal.getBlockPos()
                );

        if (outputPedestal == null) {

            // 沒有空基座，暫停工作
            // 已有的注魔進度保留
            return;
        }

        if (!inputPedestal.claim(altarPos)) {
            return;
        }

        // ====================================================
        // 計算精華成本
        // ====================================================

        int requiredCost = recipe.getCost();

        if (altar.isDragonEfficiencyUpgraded()) {

            requiredCost = Math.max(
                    1,
                    (int) Math.ceil(requiredCost * 0.65D)
            );
        }

        // 防止配方成本、產物或輸入資料改變後沿用舊進度
        String recipeSignature =
                holder.id()
                        + "#"
                        + requiredCost
                        + "#"
                        + ItemStack.hashItemAndComponents(inputStack)
                        + "#"
                        + ItemStack.hashItemAndComponents(recipe.getResult())
                        + "#"
                        + recipe.getResult().getCount();

        inputPedestal.setActiveRecipe(recipeSignature);

        // 之前已經完成進度，但當時缺少空基座
        if (inputPedestal.getProgress() >= requiredCost) {

            finishRecipe(
                    level,
                    altar,
                    inputPedestal,
                    outputPedestal,
                    recipe
            );

            return;
        }

        // 缺少精華時暫停，不清除進度
        if (altar.getEssence() <= 0) {
            return;
        }

        // ====================================================
        // 祭壇 → 原料基座 粒子流
        // ====================================================

        spawnParticleStream(
                level,
                altarPos,
                inputPedestal.getBlockPos()
        );

        // 保留原本隨機推進機制
        if (level.random.nextInt(5) != 0) {
            return;
        }

        int operations = altar.isDragonSpeedUpgraded() ? 2 : 1;

        for (int i = 0; i < operations; i++) {

            if (inputPedestal.getProgress() >= requiredCost) {
                break;
            }

            if (!altar.consumeEssence(1)) {
                break;
            }

            inputPedestal.advanceProgress();

            if (inputPedestal.getProgress() >= requiredCost) {

                // 完成時重新尋找可用的空基座
                AltarPedestalBlockEntity destination =
                        findEmptyPedestal(
                                level,
                                altar,
                                inputPedestal.getBlockPos()
                        );

                if (destination != null) {

                    finishRecipe(
                            level,
                            altar,
                            inputPedestal,
                            destination,
                            recipe
                    );
                }

                return;
            }
        }
    }

    // ========================================================
    // 完成配方：將產物放入空基座
    // ========================================================

    private static boolean finishRecipe(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar,
            AltarPedestalBlockEntity inputPedestal,
            AltarPedestalBlockEntity outputPedestal,
            EssenceAltarRecipe recipe
    ) {

        BlockPos altarPos = altar.getBlockPos();

        if (inputPedestal == outputPedestal) {
            return false;
        }

        if (inputPedestal.isEmpty() || !outputPedestal.isEmpty()) {
            return false;
        }

        if (!isAvailableToAltar(level, altar, outputPedestal)) {
            return false;
        }

        EssenceAltarRecipeInput recipeInput =
                new EssenceAltarRecipeInput(
                        "dragon",
                        List.of(inputPedestal.getItem())
                );

        ItemStack result = recipe.assemble(
                recipeInput,
                level.registryAccess()
        );

        if (result.isEmpty()
                || result.getCount() > result.getMaxStackSize()) {
            return false;
        }

        // 先鎖定輸出基座
        if (!outputPedestal.claim(altarPos)) {
            return false;
        }

        // ========================================================
// 驗證輸入與輸出基座
// ========================================================

        if (!inputPedestal.isInputMode()
                || !outputPedestal.isOutputMode()) {
            return false;
        }

        if (inputPedestal == outputPedestal) {
            return false;
        }

        if (inputPedestal.isEmpty() || !outputPedestal.isEmpty()) {
            return false;
        }

        if (!isAvailableToAltar(level, altar, outputPedestal)) {
            return false;
        }

// 原本的 recipeInput 和 result 組裝邏輯保留

        if (!outputPedestal.canAcceptCraftedResult(result)) {
            return false;
        }

        // ========================================================
        // 鎖定輸出基座
        // ========================================================

        if (!outputPedestal.claim(altar.getBlockPos())) {
            return false;
        }

        // ========================================================
        // 配方完成
        // ========================================================

        if (!outputPedestal.placeCraftedResult(result)) {

            outputPedestal.release(altar.getBlockPos());
            return false;
        }

        // 確定成品已存入後，才扣掉一份材料
        ItemStack consumed = inputPedestal.extract(1);

        if (consumed.isEmpty()) {

            // 回滾輸出，避免憑空產生物品
            outputPedestal.extract(result.getCount());
            outputPedestal.release(altar.getBlockPos());

            return false;
        }

        inputPedestal.release(altar.getBlockPos());
        outputPedestal.release(altar.getBlockPos());

        spawnCompletionParticles(
                level,
                outputPedestal.getBlockPos()
        );

        return true;
    }

    // ========================================================
    // 粒子：祭壇向基座輸送能量
    // ========================================================

    private static void spawnParticleStream(
            ServerLevel level,
            BlockPos altar,
            BlockPos pedestal
    ) {

        Vec3 start = new Vec3(
                altar.getX() + 0.5D,
                altar.getY() + 1.25D,
                altar.getZ() + 0.5D
        );

        Vec3 end = new Vec3(
                pedestal.getX() + 0.5D,
                pedestal.getY() + 1.0D,
                pedestal.getZ() + 0.5D
        );

        double phase =
                (level.getGameTime() % 20L) / 20.0D;

        for (int i = 0; i < 4; i++) {

            double t =
                    (phase + i * 0.25D) % 1.0D;

            Vec3 point = start.lerp(end, t);

            double arc =
                    Math.sin(t * Math.PI) * 0.30D;

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    point.x,
                    point.y + arc,
                    point.z,
                    1,
                    0.015D,
                    0.015D,
                    0.015D,
                    0.0D
            );
        }
    }

    private static void spawnCompletionParticles(
            ServerLevel level,
            BlockPos pos
    ) {

        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                30,
                0.25D,
                0.25D,
                0.25D,
                0.08D
        );
    }

    // ========================================================
    // 祭壇被破壞：釋放基座
    // ========================================================

    public static void releaseAll(
            ServerLevel level,
            DragonEssenceAltarBlockEntity altar
    ) {

        BlockPos center = altar.getBlockPos();

        // 按最大升級範圍檢查
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {

                BlockPos pos = center.offset(x, 0, z);

                AltarPedestalBlockEntity pedestal =
                        getPedestal(level, pos);

                if (pedestal != null) {
                    pedestal.release(center);
                }
            }
        }

        altar.setDragonPedestals(List.of());
    }
}

