package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import org.starrepublic.beyond_the_end.Entitys.blockentity.DragonEssenceAltarBlockEntity;

public final class DragonAltarModeInteraction {

    private DragonAltarModeInteraction() {
    }

    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {

        Player player = event.getEntity();

        // 只處理潛行主手右鍵
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()
                || player.isSpectator()) {
            return;
        }

        // 只處理龍之精華祭壇
        if (!(event.getLevel().getBlockEntity(event.getPos())
                instanceof DragonEssenceAltarBlockEntity altar)) {
            return;
        }

        if (!event.getLevel().isClientSide) {

            int mode = altar.cyclePowderMode();

            String modeName;

            if (mode == 1) {
                modeName = "加速配方";
            }
            else if (mode == 2) {
                modeName = "節省龍之精華";
            }
            else {
                modeName = "普通模式";
            }

            player.displayClientMessage(
                    Component.literal(
                            "龍之精華祭壇：末影粉模式 - " + modeName
                    ),
                    true
            );
        }

        // 阻止潛行時意外放置方塊或投入物品
        event.setCancellationResult(
                InteractionResult.sidedSuccess(
                        event.getLevel().isClientSide
                )
        );

        event.setCanceled(true);
    }
}
