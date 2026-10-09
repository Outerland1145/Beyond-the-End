package org.starrepublic.beyond_the_end.World.EndBarrier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class EndBarrierRejectionManager {
    private EndBarrierRejectionManager() {
    }

    /**
     * 從動畫開始到真正傳送的 Server Tick。
     *
     * 14 Tick 約 0.7 秒。
     */
    private static final int TELEPORT_TICK = 14;

    private static final Map<UUID, Integer>
            REJECTING_PLAYERS =
            new HashMap<>();

    public static boolean isRejecting(
            ServerPlayer player
    ) {

        return REJECTING_PLAYERS
                .containsKey(player.getUUID());
    }

    public static void begin(
            ServerPlayer player
    ) {

        if (isRejecting(player)) {
            return;
        }

        REJECTING_PLAYERS.put(
                player.getUUID(),
                0
        );

        PacketDistributor.sendToPlayer(
                player,
                new BarrierRejectPayload()
        );
    }

    public static void tick(
            MinecraftServer server
    ) {

        Iterator<Map.Entry<UUID, Integer>>
                iterator =
                REJECTING_PLAYERS
                        .entrySet()
                        .iterator();

        while (iterator.hasNext()) {

            Map.Entry<UUID, Integer> entry =
                    iterator.next();

            ServerPlayer player =
                    server
                            .getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            /*
             * 玩家斷線。
             */
            if (player == null) {
                iterator.remove();
                continue;
            }

            int ticks =
                    entry.getValue() + 1;

            /*
             * 等待 Client 白屏完成。
             */
            if (ticks < TELEPORT_TICK) {

                entry.setValue(ticks);

                /*
                 * 可選：
                 * 在驅逐過程減少玩家高速繼續飛行。
                 */
                player.setDeltaMovement(
                        player.getDeltaMovement()
                                .scale(0.45D)
                );

                continue;
            }

            teleportBack(player);

            iterator.remove();
        }
    }

    private static void teleportBack(
            ServerPlayer player
    ) {

        ServerLevel end =
                player.serverLevel();

        BlockPos spawn =
                ServerLevel.END_SPAWN_POINT;

        player.teleportTo(
                end,
                spawn.getX() + 0.5D,
                spawn.getY(),
                spawn.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );

        player.setDeltaMovement(
                0.0D,
                0.0D,
                0.0D
        );

        PacketDistributor.sendToPlayer(
                player,
                new BarrierReturnedPayload()
        );
    }
}
