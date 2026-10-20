package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;
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

    private int dragonTickCounter = 0;
    private String recipeFailure = "";
    private boolean dragonSpeedUpgraded = false;
    private boolean dragonRangeUpgraded = false;
    private boolean dragonEfficiencyUpgraded = false;
    // ========================================================
    // 末影粉模式
    // ========================================================

    // normal / speed / efficiency
    private String powderMode = "normal";

    // 剩餘加成時間，單位：tick
    private int powderTicks = 0;

    // 用來識別效果是否曾經中斷
    private long powderSession = 0L;
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
    // 末影粉模式管理
    // ========================================================

    public String getPowderMode() {
        return powderMode;
    }

    public int getPowderTicks() {
        return powderTicks;
    }

    public long getPowderSession() {
        return powderSession;
    }

    // 取得目前真正生效的模式
    public String getEffectivePowderMode() {

        if (powderTicks <= 0) {
            return "normal";
        }

        return powderMode;
    }

    // 潛行右鍵切換模式
    public void cyclePowderMode() {

        if (level == null || level.isClientSide) {
            return;
        }

        if (powderMode.equals("normal")) {
            powderMode = "speed";

        } else if (powderMode.equals("speed")) {
            powderMode = "efficiency";

        } else {
            powderMode = "normal";
        }

        // 切換模式會使原有配方工作階段失效
        powderSession++;

        markUpdated();
    }

    // 消耗一個末影粉，增加一分鐘效果
    public boolean addPowderMinute() {

        if (level == null || level.isClientSide) {
            return false;
        }

        // 普通模式不消耗末影粉
        if (powderMode.equals("normal")) {
            return false;
        }

        // 防止整數溢出
        powderTicks = (int) Math.min(
                Integer.MAX_VALUE,
                (long) powderTicks + 1200L
        );

        markUpdated();

        return true;
    }

    // 每個伺服器 tick 更新一次
    public void tickPowderEffect() {

        if (level == null || level.isClientSide) {
            return;
        }

        if (powderTicks <= 0) {
            return;
        }

        powderTicks--;

        // 每秒標記一次存檔
        if (powderTicks % 20 == 0) {
            setChanged();
        }

        // 加成到期，原本配方不能繼續享有加成
        if (powderTicks == 0) {

            powderSession++;

            markUpdated();
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

        // 更新末影粉效果時間
        altar.tickPowderEffect();

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
        tag.putString("PowderMode", powderMode);
        tag.putInt("PowderTicks", powderTicks);
        tag.putLong("PowderSession", powderSession);
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
        String savedMode = tag.getString("PowderMode");

        powderMode =
                savedMode.equals("speed") ||
                        savedMode.equals("efficiency")
                        ? savedMode
                        : "normal";

        powderTicks = Math.max(
                0,
                tag.getInt("PowderTicks")
        );

        powderSession = tag.getLong("PowderSession");
    }
}
