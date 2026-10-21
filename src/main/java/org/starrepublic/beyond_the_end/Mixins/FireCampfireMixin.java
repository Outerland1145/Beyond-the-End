package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.starrepublic.beyond_the_end.recipes.essence_altar.FireCampfireTickBridge;
import org.starrepublic.beyond_the_end.recipes.essence_altar.FireSmeltingAura;

@Mixin(CampfireBlockEntity.class)
public abstract class FireCampfireMixin
        implements FireCampfireTickBridge {

    @Shadow
    @Final
    private int[] cookingProgress;

    @Shadow
    @Final
    private int[] cookingTime;

    @Shadow
    @Final
    private NonNullList<ItemStack> items;

    @Unique
    private double beyondTheEnd$extraProgress = 0.0D;

    // ========================================================
    // 原版營火烹飪後額外推進進度
    // ========================================================

    @Inject(
            method = "cookTick",
            at = @At("TAIL")
    )
    private static void beyondTheEnd$afterVanillaCampfireTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CampfireBlockEntity campfire,
            CallbackInfo ci
    ) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!state.is(Blocks.CAMPFIRE)
                && !state.is(Blocks.SOUL_CAMPFIRE)) {
            return;
        }

        if (!state.getValue(CampfireBlock.LIT)) {
            return;
        }

        ((FireCampfireTickBridge) campfire)
                .beyondTheEnd$afterCampfireTick(serverLevel);
    }

    @Override
    public void beyondTheEnd$afterCampfireTick(
            ServerLevel level
    ) {

        CampfireBlockEntity campfire =
                (CampfireBlockEntity) (Object) this;

        FireSmeltingAura.Aura aura =
                FireSmeltingAura.find(
                        level,
                        campfire.getBlockPos()
                );

        if (aura == null) {
            beyondTheEnd$extraProgress = 0.0D;
            return;
        }

        // ====================================================
        // 分數進度累積
        // ====================================================

        double bonus =
                (aura.speedPercent() - 100) / 100.0D;

        beyondTheEnd$extraProgress += bonus;

        int extraTicks =
                (int) beyondTheEnd$extraProgress;

        beyondTheEnd$extraProgress -= extraTicks;

        if (extraTicks <= 0) {
            return;
        }

        boolean changed = false;

        // ====================================================
        // 四個營火欄位
        // ====================================================

        for (int i = 0; i < items.size(); i++) {

            if (items.get(i).isEmpty()) {
                continue;
            }

            if (cookingTime[i] <= 1) {
                continue;
            }

            int oldProgress = cookingProgress[i];

            // 完成配方仍交由原版 cookTick 執行，
            // 確保原版產物生成與烹飪事件正常運作
            cookingProgress[i] = Math.min(
                    cookingTime[i] - 1,
                    oldProgress + extraTicks
            );

            if (cookingProgress[i] != oldProgress) {
                changed = true;
            }
        }

        if (changed) {
            campfire.setChanged();
        }
    }
}

