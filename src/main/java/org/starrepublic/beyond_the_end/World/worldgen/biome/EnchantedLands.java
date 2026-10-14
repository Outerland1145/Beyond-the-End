package org.starrepublic.beyond_the_end.World.worldgen.biome;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public class EnchantedLands {
    private EnchantedLands() {}
    public static final ResourceKey<Biome> EnchantedLands =
            ResourceKey.create(
                    Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "enchanted_lands"
                    )
            );
}
