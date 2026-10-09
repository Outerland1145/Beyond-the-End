package org.starrepublic.beyond_the_end.Events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.starrepublic.beyond_the_end.Renders.particle.EndVoidParticleController;
import org.starrepublic.beyond_the_end.World.EndBarrier.ClientBarrierState;
import org.starrepublic.beyond_the_end.World.EndBarrier.EndBarrierManager;

public final class ClientEndEffectEvents {
    private ClientEndEffectEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {

        /*
         * 永久環境粒子。
         */
        EndVoidParticleController.tick();

        Minecraft minecraft =
                Minecraft.getInstance();

        LocalPlayer player =
                minecraft.player;

        if (player == null
                || minecraft.level == null) {

            ClientBarrierState.reset();
            return;
        }

        /*
         * 驅逐動畫優先級最高。
         */
        if (ClientBarrierState.getPhase()
                == ClientBarrierState.Phase.REJECTING) {

            ClientBarrierState
                    .tickRejection();

            return;
        }

        /*
         * 返回平台後淡出。
         */
        if (ClientBarrierState.getPhase()
                == ClientBarrierState.Phase.FADING) {

            ClientBarrierState
                    .tickFadeOut();

            return;
        }

        /*
         * 不在終界。
         */
        if (minecraft.level.dimension()
                != Level.END) {

            ClientBarrierState
                    .tickNormal(0.0F);

            return;
        }

        /*
         * 結界已解除。
         */
        if (!ClientBarrierState
                .isBarrierEnabled()) {

            ClientBarrierState
                    .tickNormal(0.0F);

            return;
        }

        double x = player.getX();
        double z = player.getZ();

        double distance =
                Math.sqrt(
                        x * x +
                                z * z
                );

        float distortion =
                calculateDistortion(
                        distance
                );

        ClientBarrierState
                .tickNormal(distortion);
    }

    private static float calculateDistortion(
            double distance
    ) {

        double start =
                EndBarrierManager
                        .DISTORTION_START_RADIUS;

        double barrier =
                EndBarrierManager
                        .BARRIER_RADIUS;

        if (distance >= start) {
            return 0.0F;
        }

        if (distance <= barrier) {
            return 1.0F;
        }

        double linear =
                (start - distance)
                        /
                        (start - barrier);

        /*
         * 平方曲線。
         *
         * 遠處很弱，
         * 靠近 250 時急劇增強。
         */
        return (float) (
                linear * linear
        );
    }
}
