package org.starrepublic.beyond_the_end.Events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.starrepublic.beyond_the_end.Utl.EndSkySeedUtil;
import org.starrepublic.beyond_the_end.network.payload.EndSkySeedPayload;

public final class EndSkySyncEvents {

    private EndSkySyncEvents() {
    }

    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event
    ) {

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {

            return;
        }

        /*
         * 整個 Minecraft 世界使用相同 world seed。
         *
         * 我們固定從 Overworld 取得，
         * 不受玩家登入時所在 dimension 影響。
         */
        long worldSeed =
                player.serverLevel()
                        .getServer()
                        .overworld()
                        .getSeed();

        /*
         * 不直接把真正 world seed 傳給 Client。
         */
        long skySeed =
                EndSkySeedUtil.createEndSkySeed(
                        worldSeed
                );

        PacketDistributor.sendToPlayer(
                player,
                new EndSkySeedPayload(
                        skySeed
                )
        );
    }
}
