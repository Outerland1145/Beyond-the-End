package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;
import org.starrepublic.beyond_the_end.Register.Tags;
import org.starrepublic.beyond_the_end.recipes.essence_altar.DragonAltarLogic;
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipe;
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipeInput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DragonEssenceAltarBlockEntity
        extends EssenceAltarBlockEntity {

    // ========================================================
    // 龍之精華祭壇資料
    // ========================================================
    // 0 = 普通、1 = 加速、2 = 節省精華
    private int powderMode = 0;

    // 祭壇儲存的末影粉數量
    private int storedPowder = 0;

    // 用來保存實際投入的物品，方便破壞時返還
    private ItemStack powderItem = ItemStack.EMPTY;

    private static final int MAX_POWDER = 256;
    private int dragonTickCounter = 0;
    private String recipeFailure = "";
    private boolean dragonSpeedUpgraded = false;
    private boolean dragonRangeUpgraded = false;
    private boolean dragonEfficiencyUpgraded = false;

    // 由世界掃描取得，不需要儲存 NBT
    private final List<BlockPos> dragonPedestals = new ArrayList<>();

    // ========================================================
    // 建構
    // ========================================================

    public DragonEssenceAltarBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                BlockEntityRegister.DRAGON_ESSENCE_ALTAR.get(),
                pos,
                state
        );
    }
    // ========================================================
// End Powder Mode
// ========================================================

    public int getPowderMode() {
        return powderMode;
    }

    public int cyclePowderMode() {

        if (level == null || level.isClientSide) {
            return powderMode;
        }

        powderMode = (powderMode + 1) % 3;

        markUpdated();

        return powderMode;
    }

    public int getStoredPowder() {
        return storedPowder;
    }

// ========================================================
// Add End Powder
// ========================================================

    public int addPowder(ItemStack source, int amount) {

        if (level == null || level.isClientSide
                || source.isEmpty() || amount <= 0) {
            return 0;
        }

        if (!source.is(Tags.END_POWDER)) {
            return 0;
        }

        // 不混合不同物品或不同資料元件的粉末
        if (storedPowder > 0
                && !ItemStack.isSameItemSameComponents(
                powderItem,
                source
        )) {
            return 0;
        }

        int accepted = Math.min(
                Math.min(amount, source.getCount()),
                MAX_POWDER - storedPowder
        );

        if (accepted <= 0) {
            return 0;
        }

        if (storedPowder == 0) {
            powderItem = source.copyWithCount(1);
        }

        storedPowder += accepted;

        markUpdated();

        return accepted;
    }

// ========================================================
// Consume End Powder
// ========================================================

    public boolean consumePowder() {

        if (level == null || level.isClientSide
                || storedPowder <= 0) {
            return false;
        }

        storedPowder--;

        if (storedPowder == 0) {
            powderItem = ItemStack.EMPTY;
        }

        markUpdated();

        return true;
    }

// ========================================================
// Return Unused End Powder
// ========================================================

    public void dropUnusedPowder(ServerLevel serverLevel) {

        if (storedPowder <= 0 || powderItem.isEmpty()) {
            return;
        }

        ItemStack template = powderItem.copy();
        int remaining = storedPowder;

        // 先清空內部資料，防止重複返還
        storedPowder = 0;
        powderItem = ItemStack.EMPTY;

        setChanged();

        if (!serverLevel.getGameRules().getBoolean(
                GameRules.RULE_DOBLOCKDROPS
        )) {
            return;
        }

        while (remaining > 0) {

            int count = Math.min(
                    remaining,
                    template.getMaxStackSize()
            );

            Block.popResource(
                    serverLevel,
                    worldPosition,
                    template.copyWithCount(count)
            );

            remaining -= count;
        }
    }
    public String getRecipeFailure() {
        return recipeFailure;
    }

    public void setRecipeFailure(String reason) {

        if (reason == null) {
            reason = "";
        }

        if (recipeFailure.equals(reason)) {
            return;
        }

        recipeFailure = reason;

        markUpdated();
    }
    @Override
    public String getEssenceType() {
        return "dragon";
    }

    // ========================================================
    // 配方搜尋
    // ========================================================

    public Optional<RecipeHolder<EssenceAltarRecipe>>
    findMatchingRecipe(List<ItemStack> inputItems) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }

        if (inputItems == null || inputItems.isEmpty()) {
            return Optional.empty();
        }

        EssenceAltarRecipeInput input =
                new EssenceAltarRecipeInput(
                        getEssenceType(),
                        inputItems
                );

        return serverLevel.getRecipeManager().getRecipeFor(
                RecipeRegister.ESSENCE_ALTAR_TYPE.get(),
                input,
                serverLevel
        );
    }

    // 方便直接傳入單個物品
    public Optional<RecipeHolder<EssenceAltarRecipe>>
    findMatchingRecipe(ItemStack stack) {

        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        return findMatchingRecipe(List.of(stack));
    }

    // ========================================================
    // 升級狀態
    // ========================================================

    public boolean isDragonSpeedUpgraded() {
        return dragonSpeedUpgraded;
    }

    public boolean isDragonRangeUpgraded() {
        return dragonRangeUpgraded;
    }

    public boolean isDragonEfficiencyUpgraded() {
        return dragonEfficiencyUpgraded;
    }

    public boolean upgradeDragonSpeed() {

        if (level == null || level.isClientSide
                || dragonSpeedUpgraded) {
            return false;
        }

        dragonSpeedUpgraded = true;

        markUpdated();

        return true;
    }

    public boolean upgradeDragonRange() {

        if (level == null || level.isClientSide
                || dragonRangeUpgraded) {
            return false;
        }

        dragonRangeUpgraded = true;

        markUpdated();

        return true;
    }

    public boolean upgradeDragonEfficiency() {

        if (level == null || level.isClientSide
                || dragonEfficiencyUpgraded) {
            return false;
        }

        dragonEfficiencyUpgraded = true;

        markUpdated();

        return true;
    }

    // ========================================================
    // 基座管理
    // ========================================================

    public List<BlockPos> getDragonPedestals() {
        return List.copyOf(dragonPedestals);
    }

    public void setDragonPedestals(Collection<BlockPos> positions) {

        dragonPedestals.clear();

        dragonPedestals.addAll(positions);
    }

    // ========================================================
    // 伺服器 Tick
    // ========================================================

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            DragonEssenceAltarBlockEntity altar
    ) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        altar.dragonTickCounter++;

        DragonAltarLogic.tick(
                serverLevel,
                altar,
                altar.dragonTickCounter
        );
    }

    // ========================================================
    // NBT
    // ========================================================

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.putBoolean(
                "DragonSpeed",
                dragonSpeedUpgraded
        );

        tag.putBoolean(
                "DragonRange",
                dragonRangeUpgraded
        );

        tag.putBoolean(
                "DragonEfficiency",
                dragonEfficiencyUpgraded
        );
        tag.putString("RecipeFailure", recipeFailure);
        tag.putInt("PowderMode", powderMode);
        tag.putInt("StoredPowder", storedPowder);

        if (!powderItem.isEmpty()) {
            tag.put("PowderItem", powderItem.save(registries));
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        dragonSpeedUpgraded =
                tag.getBoolean("DragonSpeed");

        dragonRangeUpgraded =
                tag.getBoolean("DragonRange");

        dragonEfficiencyUpgraded =
                tag.getBoolean("DragonEfficiency");

        dragonPedestals.clear();

        dragonTickCounter = 0;
        recipeFailure = tag.getString("RecipeFailure");
        powderMode = Math.clamp(
                tag.getInt("PowderMode"),
                0,
                2
        );

        storedPowder = Math.clamp(
                tag.getInt("StoredPowder"),
                0,
                MAX_POWDER
        );

        powderItem = tag.contains("PowderItem", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(
                registries,
                tag.getCompound("PowderItem")
        )
                : ItemStack.EMPTY;

        // 避免空的物品資料對應到非零粉末數量
        if (powderItem.isEmpty()) {
            storedPowder = 0;
        }
    }
}
