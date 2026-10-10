package org.starrepublic.beyond_the_end.Mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.starrepublic.beyond_the_end.World.worldgen.biomedefinition.EndBiomeDataCache;

@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseBasedChunkGeneratorMixin {


    @Inject(
            method = "surfaceRule",
            at = @At("RETURN"),
            cancellable = true
    )
    private void beyondTheEnd$injectBiomeSurfaceRules(
            CallbackInfoReturnable<SurfaceRules.RuleSource> cir
    ) {

        SurfaceRules.RuleSource custom =
                EndBiomeDataCache.surfaceRule();


        /*
         * 沒有任何 datapack biome surface。
         *
         * 保持 Minecraft 原本設定。
         */
        if (custom == null) {
            return;
        }


        SurfaceRules.RuleSource vanilla =
                cir.getReturnValue();


        /*
         * 自訂規則必須放前面。
         *
         * custom 沒有匹配目前 biome 時，
         * 才 fallback 到原版 surface rule。
         */
        cir.setReturnValue(
                SurfaceRules.sequence(
                        custom,
                        vanilla
                )
        );
    }
}
