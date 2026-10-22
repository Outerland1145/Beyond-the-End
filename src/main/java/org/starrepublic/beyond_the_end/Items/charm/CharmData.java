package org.starrepublic.beyond_the_end.Items.charm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record CharmData(
        ResourceLocation type,
        int level
) {

    public static final Codec<CharmData> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC
                                    .fieldOf("type")
                                    .forGetter(CharmData::type),

                            Codec.intRange(1, 64)
                                    .fieldOf("level")
                                    .forGetter(CharmData::level)

                    ).apply(instance, CharmData::new)
            );

    public static final StreamCodec<ByteBuf, CharmData>
            STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            CharmData::type,

            ByteBufCodecs.VAR_INT,
            CharmData::level,

            CharmData::new
    );
}
