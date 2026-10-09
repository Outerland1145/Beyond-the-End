package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.starrepublic.beyond_the_end.World.EndBarrier.EndBarrierPostProcessor;

@Mixin(GameRenderer.class)
public abstract class EndBarrierRendererMixin {
    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void beyondTheEnd$renderBarrierEffect(
            DeltaTracker deltaTracker,
            boolean renderLevel,
            CallbackInfo ci
    ) {

        float partialTick =
                deltaTracker
                        .getGameTimeDeltaPartialTick(
                                true
                        );

        EndBarrierPostProcessor
                .render(partialTick);
    }
}
