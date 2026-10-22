package org.starrepublic.beyond_the_end.Items.rune;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RuneDefinition(int modelIndex) {

    public static final Codec<RuneDefinition> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.intRange(7, 4096)
                                    .fieldOf("model_index")
                                    .forGetter(
                                            RuneDefinition::modelIndex
                                    )
                    ).apply(instance, RuneDefinition::new)
            );
}
