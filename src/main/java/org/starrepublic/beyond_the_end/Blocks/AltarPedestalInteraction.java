package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starrepublic.beyond_the_end.Entitys.blockentity.AltarPedestalBlockEntity;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public final class AltarPedestalInteraction {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AltarPedestalInteraction.class);

    private AltarPedestalInteraction() {
    }

    // ========================================================
    // 統一的基座互動
    // ========================================================

    public static void interact(
            Player player,
            AltarPedestalBlockEntity pedestal,
            ItemStack heldItem,
            boolean entireStack
    ) {

        if (player.level().isClientSide || player.isSpectator()) {
            return;
        }

        // ====================================================
        // 空手：取出物品
        // ====================================================

        if (heldItem.isEmpty()) {

            int amount = entireStack
                    ? Integer.MAX_VALUE
                    : 1;

            ItemStack extracted = pedestal.extract(amount);

            if (extracted.isEmpty()) {
                return;
            }

            player.getInventory().add(extracted);

            if (!extracted.isEmpty()) {
                player.drop(extracted, false);
            }

            return;
        }

        // ====================================================
        // 手持物品：放入基座
        // ====================================================

        int amount = entireStack
                ? heldItem.getCount()
                : 1;

        int inserted = pedestal.insert(heldItem, amount);

        if (inserted > 0 && !player.getAbilities().instabuild) {
            heldItem.shrink(inserted);
        }
    }

    // ========================================================
    // 接管所有對專用基座的右鍵
    // ========================================================

    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {

        Player player = event.getEntity();

        if (event.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()
                || player.isSpectator()) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (!level.getBlockState(pos).is(
                BlockRegister.ALTAR_PEDESTAL.get()
        )) {
            return;
        }

        if (!(level.getBlockEntity(pos)
                instanceof AltarPedestalBlockEntity pedestal)) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();

        if (!level.isClientSide) {

            if (heldItem.isEmpty()) {

                // 潛行＋空手：取出全部物品
                interact(
                        player,
                        pedestal,
                        ItemStack.EMPTY,
                        true
                );

            } else if (pedestal.isInputMode()) {

                // 潛行＋手持物品：放入整組
                interact(
                        player,
                        pedestal,
                        heldItem,
                        true
                );
            }
        }

        event.setCancellationResult(
                InteractionResult.sidedSuccess(level.isClientSide)
        );

        event.setCanceled(true);
    }
}
