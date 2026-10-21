package org.starrepublic.beyond_the_end.Items.charm.rune;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record RuneData(ResourceLocation id) {

    public static final Codec<RuneData> CODEC =
            ResourceLocation.CODEC.xmap(
                    RuneData::new,
                    RuneData::id
            );

    public static final StreamCodec<ByteBuf, RuneData>
            STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            RuneData::id,
            RuneData::new
    );
}
