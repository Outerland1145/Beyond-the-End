package org.starrepublic.beyond_the_end.World.EndBarrier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public record BarrierStatePayload(
        boolean enabled
) implements CustomPacketPayload {

    public static final Type<BarrierStatePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "barrier_state"
                    )
            );

    public static final StreamCodec<
                ByteBuf,
                BarrierStatePayload
                > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    BarrierStatePayload::enabled,
                    BarrierStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
