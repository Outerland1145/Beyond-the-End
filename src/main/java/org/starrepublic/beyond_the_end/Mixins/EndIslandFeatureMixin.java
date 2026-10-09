package org.starrepublic.beyond_the_end.Mixins;


import net.minecraft.world.level.levelgen.feature.EndIslandFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;

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
}
