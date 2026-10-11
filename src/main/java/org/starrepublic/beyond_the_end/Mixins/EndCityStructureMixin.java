package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

import java.util.Optional;

@Mixin(EndCityStructure.class)
public abstract class EndCityStructureMixin {

    @Inject(
            method = "findGenerationPoint",
            at = @At("HEAD"),
            cancellable = true
    )
    private void beyondTheEnd$disableVanillaEndCity(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir
    ) {

        if (!BTEConfigs.SERVER.enablevanillaendcity.get()) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
