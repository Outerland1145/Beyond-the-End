package org.starrepublic.beyond_the_end.World.worldgen.biomedefinition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

public record EndBiomeDefinition(
        ResourceKey<Biome> biome,
        double generationChance,
        SurfaceRules.RuleSource surfaceRule
) {

    public static final Codec<EndBiomeDefinition> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(

                            ResourceKey.codec(Registries.BIOME)
                                    .fieldOf("biome")
                                    .forGetter(EndBiomeDefinition::biome),

                            Codec.doubleRange(0.0D, 1.0D)
                                    .fieldOf("generation_chance")
                                    .forGetter(EndBiomeDefinition::generationChance),

                            SurfaceRules.RuleSource.CODEC
                                    .fieldOf("surface_rule")
                                    .forGetter(EndBiomeDefinition::surfaceRule)

                    ).apply(instance, EndBiomeDefinition::new)
            );
}
