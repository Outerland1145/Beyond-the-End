package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import org.starrepublic.beyond_the_end.Beyond_the_end;

public final class ConfiguredFeatureKeys {

    public static final ResourceKey<ConfiguredFeature<?, ?>>
            ENCHANTED_LANDS_BONEMEAL =
            create(
                    "enchanted_lands_bonemeal"
            );


    private static ResourceKey<ConfiguredFeature<?, ?>> create(
            String name
    ) {

        return ResourceKey.create(
                Registries.CONFIGURED_FEATURE,

                ResourceLocation.fromNamespaceAndPath(
                        Beyond_the_end.MODID,
                        name
                )
        );
    }


    private ConfiguredFeatureKeys() {
    }
}
