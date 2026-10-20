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
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipe;
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipeInput;
import org.starrepublic.beyond_the_end.recipes.essence_altar.FireAltarLogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class FireEssenceAltarBlockEntity
        extends EssenceAltarBlockEntity {

    private int fireTickCounter = 0;

    // normal / speed / efficiency / smelting
    private String powderMode = "normal";
    private String recipeFailure = "";

    // 末影粉效果剩餘 tick
    private int powderTicks = 0;

    // 用於驗證配方執行過程中效果是否中斷
    private long powderSession = 0L;

    // 有效基座清單，不保存 NBT
    private final List<BlockPos> firePedestals = new ArrayList<>();

    public FireEssenceAltarBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                BlockEntityRegister.FIRE_ESSENCE_ALTAR.get(),
                pos,
                state
        );
    }

    @Override
    public String getEssenceType() {
        return "fire";
    }

    // ========================================================
    // Fire recipe lookup
    // ========================================================

    public Optional<RecipeHolder<EssenceAltarRecipe>>
    findMatchingRecipe(ItemStack stack) {

        if (!(level instanceof ServerLevel serverLevel)
                || stack.isEmpty()) {
            return Optional.empty();
        }

        EssenceAltarRecipeInput input =
                new EssenceAltarRecipeInput(
                        "fire",
                        List.of(stack.copy())
                );

        return serverLevel.getRecipeManager().getRecipeFor(
                RecipeRegister.ESSENCE_ALTAR_TYPE.get(),
                input,
                serverLevel
        );
    }

    // ========================================================
    // Pedestals
    // ========================================================

    public List<BlockPos> getFirePedestals() {
        return List.copyOf(firePedestals);
    }

    public void setFirePedestals(
            Collection<BlockPos> positions
    ) {
        firePedestals.clear();
        firePedestals.addAll(positions);
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
    // ========================================================
    // Modes
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

    public String getEffectivePowderMode() {

        // 熔煉模式不依賴末影粉計時
        if ("smelting".equals(powderMode)) {
            return "smelting";
        }

        if (powderTicks <= 0) {
            return "normal";
        }

        return powderMode;
    }

    public void cyclePowderMode() {

        if (level == null || level.isClientSide) {
            return;
        }

        switch (powderMode) {

            case "normal" -> powderMode = "speed";

            case "speed" -> powderMode = "efficiency";

            case "efficiency" -> powderMode = "smelting";

            default -> powderMode = "normal";
        }

        powderSession++;
        markUpdated();
    }

    // ========================================================
    // End powder
    // ========================================================

    public boolean addPowderMinute() {

        if (level == null || level.isClientSide) {
            return false;
        }

        // 普通與預留的熔煉模式不接受末影粉
        if (!"speed".equals(powderMode)
                && !"efficiency".equals(powderMode)) {
            return false;
        }

        if (powderTicks > Integer.MAX_VALUE - 1200) {
            return false;
        }

        powderTicks += 1200;

        markUpdated();

        return true;
    }

    public void tickPowderEffect() {

        if (level == null || level.isClientSide) {
            return;
        }

        if (powderTicks <= 0) {
            return;
        }

        powderTicks--;

        // 每秒同步一次，供 Jade 讀取
        if (powderTicks % 20 == 0) {
            markUpdated();
        }

        if (powderTicks == 0) {
            powderSession++;
            markUpdated();
        }
    }

    // ========================================================
    // Server ticker
    // ========================================================

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FireEssenceAltarBlockEntity altar
    ) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        altar.fireTickCounter++;

        altar.tickPowderEffect();

        FireAltarLogic.tick(
                serverLevel,
                altar,
                altar.fireTickCounter
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

        tag.putString("PowderMode", powderMode);
        tag.putInt("PowderTicks", powderTicks);
        tag.putLong("PowderSession", powderSession);
        tag.putString("RecipeFailure", recipeFailure);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        String savedMode = tag.getString("PowderMode");

        powderMode = switch (savedMode) {

            case "speed", "efficiency", "smelting" -> savedMode;

            default -> "normal";
        };

        powderTicks = Math.max(
                0,
                tag.getInt("PowderTicks")
        );

        powderSession = tag.getLong("PowderSession");
        recipeFailure = tag.getString("RecipeFailure");
        fireTickCounter = 0;
        firePedestals.clear();
    }
}

