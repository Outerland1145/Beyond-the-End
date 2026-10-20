package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

import org.starrepublic.beyond_the_end.recipes.essence_altar.FireFurnaceTickBridge;
import org.starrepublic.beyond_the_end.recipes.essence_altar.FireSmeltingAura;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FireFurnaceMixin
        implements FireFurnaceTickBridge {

    @Shadow int litTime;
    @Shadow int cookingProgress;
    @Shadow int cookingTotalTime;

    @Shadow
    @Final
    private RecipeType<? extends AbstractCookingRecipe> recipeType;

    @Shadow
    protected NonNullList<ItemStack> items;

    // ========================================================
    // 原版的配方驗證及熔煉方法
    // ========================================================

    @Invoker("canBurn")
    public static boolean beyondTheEnd$canBurn(
            RegistryAccess registryAccess,
            RecipeHolder<?> recipe,
            NonNullList<ItemStack> inventory,
            int maxStackSize,
            AbstractFurnaceBlockEntity furnace
    ) {
        throw new AssertionError();
    }

    @Invoker("burn")
    public static boolean beyondTheEnd$burn(
            RegistryAccess registryAccess,
            RecipeHolder<?> recipe,
            NonNullList<ItemStack> inventory,
            int maxStackSize,
            AbstractFurnaceBlockEntity furnace
    ) {
        throw new AssertionError();
    }

    // ========================================================
    // 工作進度
    // ========================================================

    @Unique
    private double beyondTheEnd$extraProgress = 0.0D;

    @Unique
    private int beyondTheEnd$fuelFreeProgress = 0;

    @Unique
    private int beyondTheEnd$originalCookTime = 200;

    @Unique
    private String beyondTheEnd$fuelFreeRecipe = "";

    @Unique
    private boolean beyondTheEnd$fuelFreeRunning = false;

    // ========================================================
    // Vanilla Tick 結束後執行
    // ========================================================

    @Inject(
            method = "serverTick",
            at = @At("TAIL")
    )
    private static void beyondTheEnd$afterVanillaTick(
            Level level,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfo ci
    ) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ((FireFurnaceTickBridge) furnace)
                .beyondTheEnd$afterFurnaceTick(serverLevel);
    }

    @Override
    public void beyondTheEnd$afterFurnaceTick(
            ServerLevel level
    ) {

        AbstractFurnaceBlockEntity furnace =
                (AbstractFurnaceBlockEntity) (Object) this;

        // 只支援原版三種熔爐
        BlockEntityType<?> type = furnace.getType();

        if (type != BlockEntityType.FURNACE
                && type != BlockEntityType.BLAST_FURNACE
                && type != BlockEntityType.SMOKER) {

            beyondTheEnd$stopFuelFree();
            beyondTheEnd$extraProgress = 0.0D;
            return;
        }

        FireSmeltingAura.Aura aura =
                FireSmeltingAura.find(
                        level,
                        furnace.getBlockPos()
                );

        if (aura == null) {

            beyondTheEnd$stopFuelFree();
            beyondTheEnd$extraProgress = 0.0D;
            return;
        }

        // ====================================================
        // 有燃料且正在燃燒：正常加速
        // ====================================================

        if (litTime > 0) {

            beyondTheEnd$stopFuelFree();

            if (items.get(0).isEmpty()
                    || cookingTotalTime <= 1) {
                beyondTheEnd$extraProgress = 0.0D;
                return;
            }

            RecipeHolder<? extends AbstractCookingRecipe> recipe =
                    beyondTheEnd$getRecipe(level);

            if (recipe == null
                    || !beyondTheEnd$canBurn(
                    level.registryAccess(),
                    recipe,
                    items,
                    furnace.getMaxStackSize(),
                    furnace
            )) {
                beyondTheEnd$extraProgress = 0.0D;
                return;
            }

            // 150% = 原版 1 tick + 額外平均 0.5 tick
            // 300% = 原版 1 tick + 額外 2 tick
            double bonus =
                    (aura.speedPercent() - 100) / 100.0D;

            beyondTheEnd$extraProgress += bonus;

            int extraTicks =
                    (int) beyondTheEnd$extraProgress;

            beyondTheEnd$extraProgress -= extraTicks;

            // 最後一次完成配方交給原版 serverTick，
            // 確保原版輸出、燃料及經驗統計照常執行
            if (extraTicks > 0 && cookingProgress > 0) {

                cookingProgress = Math.min(
                        cookingTotalTime - 1,
                        cookingProgress + extraTicks
                );

                furnace.setChanged();
            }

            return;
        }

        // ====================================================
        // 無燃料模式
        // ====================================================

        beyondTheEnd$extraProgress = 0.0D;

        // 只有空燃料槽，而且加速總量 >200%，
        // 才能啟動無燃料熔煉
        if (!aura.allowFuelFree()
                || !items.get(1).isEmpty()
                || items.get(0).isEmpty()) {

            beyondTheEnd$stopFuelFree();
            return;
        }

        RecipeHolder<? extends AbstractCookingRecipe> recipe =
                beyondTheEnd$getRecipe(level);

        if (recipe == null) {
            beyondTheEnd$stopFuelFree();
            return;
        }

        if (!beyondTheEnd$canBurn(
                level.registryAccess(),
                recipe,
                items,
                furnace.getMaxStackSize(),
                furnace
        )) {
            beyondTheEnd$stopFuelFree();
            return;
        }

        // 配方或者輸入物品變更後，重新開始
        String signature =
                recipe.id()
                        + "#"
                        + ItemStack.hashItemAndComponents(items.get(0));

        if (!signature.equals(beyondTheEnd$fuelFreeRecipe)) {

            beyondTheEnd$fuelFreeRecipe = signature;
            beyondTheEnd$fuelFreeProgress = 0;
        }

        int baseTime = Math.max(
                1,
                recipe.value().getCookingTime()
        );

        int requiredTime = (int) Math.min(
                Integer.MAX_VALUE,
                (long) baseTime * 2L
        );

        beyondTheEnd$originalCookTime = baseTime;
        beyondTheEnd$fuelFreeRunning = true;

        // 使用獨立計時器，不受末影粉加速影響
        beyondTheEnd$fuelFreeProgress++;

        // 同步到原版 GUI 的進度欄位
        cookingTotalTime = requiredTime;

        cookingProgress = Math.min(
                beyondTheEnd$fuelFreeProgress,
                requiredTime
        );

        // ====================================================
        // 無燃料配方完成
        // ====================================================

        if (beyondTheEnd$fuelFreeProgress >= requiredTime) {

            boolean crafted = beyondTheEnd$burn(
                    level.registryAccess(),
                    recipe,
                    items,
                    furnace.getMaxStackSize(),
                    furnace
            );

            if (crafted) {

                // 沿用原版的配方經驗統計
                furnace.setRecipeUsed(recipe);

                beyondTheEnd$fuelFreeProgress = 0;
                beyondTheEnd$fuelFreeRecipe = "";

                cookingProgress = 0;
                cookingTotalTime = baseTime;
            }
        }

        furnace.setChanged();
    }

    // ========================================================
    // 取得原版熔煉配方
    // ========================================================

    @Unique
    private RecipeHolder<? extends AbstractCookingRecipe>
    beyondTheEnd$getRecipe(ServerLevel level) {

        if (items.get(0).isEmpty()) {
            return null;
        }

        return level.getRecipeManager()
                .getRecipeFor(
                        recipeType,
                        new SingleRecipeInput(items.get(0)),
                        level
                )
                .orElse(null);
    }

    // ========================================================
    // 停止無燃料模式
    // ========================================================

    @Unique
    private void beyondTheEnd$stopFuelFree() {

        if (beyondTheEnd$fuelFreeRunning) {

            cookingProgress = 0;

            // 恢復原始配方的時間，避免切回普通
            // 熔煉後沿用兩倍的 cookingTotalTime
            cookingTotalTime = beyondTheEnd$originalCookTime;
        }

        beyondTheEnd$fuelFreeRunning = false;
        beyondTheEnd$fuelFreeProgress = 0;
        beyondTheEnd$fuelFreeRecipe = "";
    }

    // ========================================================
    // NBT：保存無燃料加工進度
    // ========================================================

    @Inject(
            method = "saveAdditional",
            at = @At("TAIL")
    )
    private void beyondTheEnd$saveFuelFreeData(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {

        tag.putBoolean(
                "BTEFuelFreeRunning",
                beyondTheEnd$fuelFreeRunning
        );

        tag.putInt(
                "BTEFuelFreeProgress",
                beyondTheEnd$fuelFreeProgress
        );

        tag.putInt(
                "BTEOriginalCookTime",
                beyondTheEnd$originalCookTime
        );

        tag.putString(
                "BTEFuelFreeRecipe",
                beyondTheEnd$fuelFreeRecipe
        );
    }

    @Inject(
            method = "loadAdditional",
            at = @At("TAIL")
    )
    private void beyondTheEnd$loadFuelFreeData(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {

        beyondTheEnd$fuelFreeRunning =
                tag.getBoolean("BTEFuelFreeRunning");

        beyondTheEnd$fuelFreeProgress = Math.max(
                0,
                tag.getInt("BTEFuelFreeProgress")
        );

        beyondTheEnd$originalCookTime = Math.max(
                1,
                tag.contains("BTEOriginalCookTime")
                        ? tag.getInt("BTEOriginalCookTime")
                        : 200
        );

        beyondTheEnd$fuelFreeRecipe =
                tag.getString("BTEFuelFreeRecipe");
    }
}

