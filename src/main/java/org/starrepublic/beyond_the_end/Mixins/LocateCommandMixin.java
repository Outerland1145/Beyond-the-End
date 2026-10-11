package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.level.levelgen.structure.Structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;


@Mixin(LocateCommand.class)
public abstract class LocateCommandMixin {

    private static final ResourceLocation VANILLA_END_CITY =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft",
                    "end_city"
            );

    @Inject(
            method = "locateStructure",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void beyondTheEnd$blockVanillaEndCityLocate(
            CommandSourceStack source,
            ResourceOrTagKeyArgument.Result<Structure> structure,
            CallbackInfoReturnable<Integer> cir
    ) {

        if (BTEConfigs.SERVER.enablevanillaendcity.get()) {
            return;
        }

        boolean isVanillaEndCity = structure
                .unwrap()
                .left()
                .map(resourceKey ->
                        resourceKey.location().equals(VANILLA_END_CITY)
                )
                .orElse(false);

        if (!isVanillaEndCity) {
            return;
        }

        source.sendFailure(
                Component.translatable(
                        "commands.beyond_the_end.end_city_disabled"
                )
        );

        cir.setReturnValue(0);
    }
}
