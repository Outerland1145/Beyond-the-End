package org.starrepublic.beyond_the_end.World.worldgen.biome;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public class InfestedLands {
    private InfestedLands() {}
    public static final ResourceKey<Biome> InfestedLands =
            ResourceKey.create(
                    Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "infested_lands"
                    )
            );
}
