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

        /*
         * 客戶端不實際處理。
         */
        if (level.isClientSide) {
            return;
        }


        /*
         * ==============================
         * 非終界使用
         * ==============================
         *
         * 與床 / 重生錨類似：
         * 直接爆炸。
         */
        if (level.dimension() != Level.END) {

            /*
             * 先移除錨定方塊。
             *
             * 這樣即使未來你把錨定方塊設成
             * 高爆炸抗性，它也不會在爆炸後留下。
             */
            level.removeBlock(
                    pos,
                    false
            );


            /*
             * 使用原版「錯誤重生點爆炸」
             * DamageSource。
             */
            level.explode(
                    null,

                    level.damageSources()
                            .badRespawnPointExplosion(
                                    pos.getCenter()
                            ),

                    null,

                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,

                    /*
                     * 與床類型爆炸相近的強度。
                     */
                    5.0F,

                    /*
                     * 是否產生火焰。
                     */
                    true,

                    Level.ExplosionInteraction.BLOCK
            );

            return;
        }


        /*
         * ==============================
         * 終界正常設定錨點
         * ==============================
         */
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

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
