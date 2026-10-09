package org.starrepublic.beyond_the_end.Events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.starrepublic.beyond_the_end.World.EndBarrier.EndBarrierManager;
import org.starrepublic.beyond_the_end.World.EndBarrier.EndBarrierRejectionManager;

public final class EndBarrierEvents {
    private EndBarrierEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().dimension()
                != Level.END) {
            return;
        }

        /*
         * 結界已永久解除。
         */
        if (!EndBarrierManager
                .isBarrierEnabled(
                        player.serverLevel()
                )) {
            return;
        }

        /*
         * 已經正在播放驅逐動畫，
         * 不重複觸發。
         */
        if (EndBarrierRejectionManager
                .isRejecting(player)) {
            return;
        }

        if (!EndBarrierManager
                .isInsideBarrier(player)) {
            return;
        }

        EndBarrierRejectionManager
                .begin(player);
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {

        EndBarrierRejectionManager.tick(
                event.getServer()
        );
    }

    @SubscribeEvent
    public static void onLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {

        if (event.getEntity()
                instanceof ServerPlayer player) {

            EndBarrierManager
                    .syncBarrierState(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {

        if (event.getEntity()
                instanceof ServerPlayer player) {

            EndBarrierManager
                    .syncBarrierState(player);
        }
    }
}
