package org.starrepublic.beyond_the_end.World.worldgen.biomedefinition;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

public record EndBiomeData(
        ResourceKey<Biome> biome,
        double generationChance,
        SurfaceRules.RuleSource surfaceRule
) {
}
