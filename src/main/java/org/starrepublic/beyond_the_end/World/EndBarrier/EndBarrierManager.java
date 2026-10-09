package org.starrepublic.beyond_the_end.World.EndBarrier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.starrepublic.beyond_the_end.Data.BeyondEndSavedData;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

public final class EndBarrierManager {

    private EndBarrierManager() {
    }
    public static final double BARRIER_RADIUS = BTEConfigs.SERVER.barrierRadius.get();

    /**
     * 開始產生視覺扭曲的距離。
     */
    public static final double DISTORTION_START_RADIUS = BARRIER_RADIUS*2;

    private static final double BARRIER_RADIUS_SQR =
            BARRIER_RADIUS * BARRIER_RADIUS;

    public static boolean isBarrierEnabled(ServerLevel level) {

        return !BeyondEndSavedData
                .get(level)
                .isMainIslandBarrierUnlocked();
    }

    public static void unlockBarrier(ServerLevel level) {

        BeyondEndSavedData
                .get(level)
                .unlockMainIslandBarrier();

        syncBarrierStateToAllPlayers(level);
    }

    public static void lockBarrier(ServerLevel level) {

        BeyondEndSavedData
                .get(level)
                .lockMainIslandBarrier();

        syncBarrierStateToAllPlayers(level);
    }

    public static boolean isInsideBarrier(ServerPlayer player) {

        double x = player.getX();
        double z = player.getZ();

        return x * x + z * z
                <= BARRIER_RADIUS_SQR;
    }

    public static double getHorizontalDistance(
            double x,
            double z
    ) {

        return Math.sqrt(
                x * x + z * z
        );
    }

    public static void syncBarrierState(
            ServerPlayer player
    ) {

        boolean enabled =
                isBarrierEnabled(player.serverLevel());

        PacketDistributor.sendToPlayer(
                player,
                new BarrierStatePayload(enabled)
        );
    }

    public static void syncBarrierStateToAllPlayers(
            ServerLevel level
    ) {

        boolean enabled =
                isBarrierEnabled(level);

        PacketDistributor.sendToAllPlayers(
                new BarrierStatePayload(enabled)
        );
    }
}
