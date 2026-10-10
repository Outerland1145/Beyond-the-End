package org.starrepublic.beyond_the_end.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EndSkySeedPayload(
        long skySeed
) implements CustomPacketPayload {

    public static final Type<EndSkySeedPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "beyond_the_end",
                            "end_sky_seed"
                    )
            );

    public static final StreamCodec<
                ByteBuf,
                EndSkySeedPayload
                > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    EndSkySeedPayload::skySeed,
                    EndSkySeedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
