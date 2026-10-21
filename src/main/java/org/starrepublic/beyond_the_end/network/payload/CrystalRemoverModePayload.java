package org.starrepublic.beyond_the_end.network.payload;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Events.CrystalRemoverEvents;

public record CrystalRemoverModePayload(int direction)
        implements CustomPacketPayload {

    public static final Type<CrystalRemoverModePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "crystal_remover_mode"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            CrystalRemoverModePayload
            > STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CrystalRemoverModePayload::direction,
            CrystalRemoverModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /*
     * 伺服器接收模式切換
     */
    public static void handle(
            CrystalRemoverModePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {

            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            CrystalRemoverEvents.changeMode(
                    player,
                    payload.direction()
            );
        });
    }

    /*
     * 網路封包註冊
     */
    public static void register(
            RegisterPayloadHandlersEvent event
    ) {
        event.registrar("1").playToServer(
                TYPE,
                STREAM_CODEC,
                CrystalRemoverModePayload::handle
        );
    }
}
