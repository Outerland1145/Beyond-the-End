package org.starrepublic.beyond_the_end.World.EndBarrier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public record BarrierReturnedPayload()
        implements CustomPacketPayload {

    public static final Type<BarrierReturnedPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "barrier_returned"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            BarrierReturnedPayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new BarrierReturnedPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
