package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SpookyTreeConfiguration(
        BlockStateProvider trunkProvider,
        BlockStateProvider leavesProvider,
        BlockStateProvider hangingProvider
) implements FeatureConfiguration {

    public static final Codec<SpookyTreeConfiguration> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(

                    BlockStateProvider.CODEC
                            .fieldOf("trunk_provider")
                            .forGetter(SpookyTreeConfiguration::trunkProvider),

                    BlockStateProvider.CODEC
                            .fieldOf("leaves_provider")
                            .forGetter(SpookyTreeConfiguration::leavesProvider),

                    BlockStateProvider.CODEC
                            .fieldOf("hanging_provider")
                            .forGetter(SpookyTreeConfiguration::hangingProvider)

            ).apply(instance, SpookyTreeConfiguration::new));
}
