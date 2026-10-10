package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.starrepublic.beyond_the_end.World.EndBarrier.BarrierRejectPayload;
import org.starrepublic.beyond_the_end.World.EndBarrier.BarrierReturnedPayload;
import org.starrepublic.beyond_the_end.World.EndBarrier.BarrierStatePayload;
import org.starrepublic.beyond_the_end.World.EndBarrier.ClientBarrierState;
import org.starrepublic.beyond_the_end.network.EndSkySeedState;
import org.starrepublic.beyond_the_end.network.payload.EndSkySeedPayload;

public final class PayloadRegistry {

    private PayloadRegistry() {
    }

    @SubscribeEvent
    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {

        PayloadRegistrar registrar =
                event.registrar("1");

        registrar.playToClient(
                BarrierStatePayload.TYPE,
                BarrierStatePayload.STREAM_CODEC,
                (payload, context) -> {

                    ClientBarrierState
                            .setBarrierEnabled(
                                    payload.enabled()
                            );
                }
        );

        registrar.playToClient(
                BarrierRejectPayload.TYPE,
                BarrierRejectPayload.STREAM_CODEC,
                (payload, context) -> {

                    ClientBarrierState
                            .beginRejection();
                }
        );

        registrar.playToClient(
                BarrierReturnedPayload.TYPE,
                BarrierReturnedPayload.STREAM_CODEC,
                (payload, context) -> {

                    ClientBarrierState
                            .beginFadeOut();
                }
        );

        registrar.playToClient(
                EndSkySeedPayload.TYPE,
                EndSkySeedPayload.STREAM_CODEC,
                PayloadRegistry::handleEndSkySeed
        );
    }
    private static void handleEndSkySeed(
            EndSkySeedPayload payload,
            IPayloadContext context
    ) {

        EndSkySeedState.setSkySeed(
                payload.skySeed()
        );
    }
}
