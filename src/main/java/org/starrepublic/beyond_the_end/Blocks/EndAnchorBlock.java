package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.starrepublic.beyond_the_end.Data.EndAnchorData;
import org.starrepublic.beyond_the_end.Register.DataRegister;

public class EndAnchorBlock extends Block {

    public EndAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private void setAnchor(
            Level level,
            BlockPos pos,
            Player player
    ) {

        if (level.isClientSide) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        /*
         * 錨定方塊只允許在終界設定。
         */
        if (!level.dimension().equals(Level.END)) {
            return;
        }

        /*
         * 注意：
         *
         * 資料是寫進「這個玩家」，
         * 不是寫進方塊。
         */
        serverPlayer.setData(
                DataRegister.END_ANCHOR.get(),
                EndAnchorData.create(pos)
        );

        serverPlayer.displayClientMessage(
                Component.translatable(
                        "message.beyond_the_end.anchor_set"
                ),
                true
        );
    }


    /*
     * 空手右鍵
     */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {

        setAnchor(
                level,
                pos,
                player
        );

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }


    /*
     * 手持物品右鍵
     *
     * 加這個之後，不管玩家手上拿什麼，
     * 右鍵錨定方塊都會設定錨點。
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {

        setAnchor(
                level,
                pos,
                player
        );

        return ItemInteractionResult.SUCCESS;
    }
}
