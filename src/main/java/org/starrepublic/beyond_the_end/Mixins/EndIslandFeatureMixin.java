package org.starrepublic.beyond_the_end.Mixins;


import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.EndIslandFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;
import org.starrepublic.beyond_the_end.World.worldgen.feature.EndIslandDecorator;

@Mixin(EndIslandFeature.class)
public abstract class EndIslandFeatureMixin {
    @Inject(
            method = "place",
            at = @At("HEAD"),
            cancellable = true
    )
    private void beyondTheEnd$preventVanillaSmallIslandsInVoid(
            FeaturePlaceContext<NoneFeatureConfiguration> context,
            CallbackInfoReturnable<Boolean> cir
    ) {

        int x =
                context.origin().getX();

        int z =
                context.origin().getZ();

        /*
         * 20000+
         *
         * 完全禁止原版小型終界島。
         */
        if (
                EndTerrainController.isVoid(
                        x,
                        z
                )
        ) {

            cir.setReturnValue(false);
        }
    }
    @Inject(
            method = "place",
            at = @At("RETURN")
    )
    private void beyondTheEnd$decorateEndIsland(
            FeaturePlaceContext<NoneFeatureConfiguration> context,
            CallbackInfoReturnable<Boolean> cir
    ) {

        /*
         * 原版生成失敗，
         * 就不做任何事情。
         */
        if (!cir.getReturnValue()) {
            return;
        }

        EndIslandDecorator.decorate(
                context.level(),
                context.origin(),
                context.random()
        );
    }
}
