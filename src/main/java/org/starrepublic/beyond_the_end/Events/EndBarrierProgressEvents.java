package org.starrepublic.beyond_the_end.Events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.starrepublic.beyond_the_end.Data.BeyondEndSavedData;
import org.starrepublic.beyond_the_end.World.progression.EndBarrierProgressManager;

public final class EndBarrierProgressEvents {
    private EndBarrierProgressEvents() {
    }


    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {

        if (!(event.getEntity()
                instanceof ServerPlayer player)) {

            return;
        }

        /*
         * 每秒檢查一次。
         */
        if (player.tickCount % 20 != 0) {
            return;
        }

        BeyondEndSavedData data =
                BeyondEndSavedData.get(
                        player.serverLevel()
                );

        if (data.hasObtainedElytra()) {
            return;
        }

        if (!playerHasElytra(player)) {
            return;
        }

        EndBarrierProgressManager
                .markElytraObtained(
                        player.serverLevel()
                );
    }


    private static boolean playerHasElytra(
            ServerPlayer player
    ) {

        /*
         * Inventory
         */
        if (
                player.getInventory()
                        .contains(
                                new ItemStack(
                                        Items.ELYTRA
                                )
                        )
        ) {

            return true;
        }

        /*
         * Chest Equipment
         */
        ItemStack chest =
                player.getItemBySlot(
                        EquipmentSlot.CHEST
                );

        return chest.is(
                Items.ELYTRA
        );
    }
}
