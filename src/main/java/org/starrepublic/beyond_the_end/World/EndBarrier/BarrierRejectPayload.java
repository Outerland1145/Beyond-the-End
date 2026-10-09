package org.starrepublic.beyond_the_end.World.EndBarrier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public record BarrierRejectPayload()
        implements CustomPacketPayload {

    public static final Type<BarrierRejectPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "barrier_reject"
                    )
            );

    public static final StreamCodec<
                ByteBuf,
                BarrierRejectPayload
                > STREAM_CODEC =
            StreamCodec.unit(
                    new BarrierRejectPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
