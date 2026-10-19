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
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        // 只處理主手，避免一次操作執行兩次
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        // 只處理本模組的專用基座
        if (!level.getBlockState(pos).is(
                BlockRegister.ALTAR_PEDESTAL.get()
        )) {
            return;
        }

        // ====================================================
        // 確認 BlockEntity
        // ====================================================

        BlockEntity entity = level.getBlockEntity(pos);

        if (!(entity instanceof AltarPedestalBlockEntity pedestal)) {

            // 暫時保留除錯訊息
            LOGGER.error(
                    "[BTE] Pedestal interaction failed! Side={}, Pos={}, Block={}, BlockEntity={}",
                    level.isClientSide ? "CLIENT" : "SERVER",
                    pos,
                    level.getBlockState(pos).getBlock().getClass().getName(),
                    entity == null
                            ? "NULL"
                            : entity.getClass().getName()
            );

            return;
        }

        // ====================================================
        // 所有基座互動走同一個入口
        // ====================================================

        ItemStack heldItem = player.getMainHandItem();

        // 客戶端只回傳互動成功，不修改物品
        if (!level.isClientSide) {

            interact(
                    player,
                    pedestal,
                    heldItem,
                    player.isShiftKeyDown()
            );

            // 暫時保留除錯訊息
            LOGGER.info(
                    "[BTE] Pedestal interaction executed. Pos={}, Item={}, Count={}",
                    pos,
                    pedestal.getItem().getItem(),
                    pedestal.getItem().getCount()
            );
        }

        // 阻止後續一般方塊與物品交互
        event.setCancellationResult(
                InteractionResult.sidedSuccess(level.isClientSide)
        );

        event.setCanceled(true);
    }
}
