package org.starrepublic.beyond_the_end.Events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.Tags;

public class InteractionEvents {
    @SubscribeEvent
    public static void onFlintAndSteelUse(
            PlayerInteractEvent.RightClickBlock event
    ) {

        ItemStack stack = event.getItemStack();

        /*
         * 只處理原版打火石。
         */
        if (!stack.is(Items.FLINT_AND_STEEL)) {
            return;
        }

        Level level = event.getLevel();

        BlockPos clickedPos = event.getPos();

        BlockState clickedState =
                level.getBlockState(clickedPos);


        /*
         * 點擊的方塊必須允許生成終界火。
         */
        if (!clickedState.is(
                Tags.END_FIRE_BASE_BLOCKS
        )) {
            return;
        }


        Direction face = event.getFace();

        if (face == null) {
            return;
        }


        /*
         * 和打火石原版行為相同：
         *
         * 火生成在被點擊面的外側。
         */
        BlockPos firePos =
                clickedPos.relative(face);


        BlockState replacedState =
                level.getBlockState(firePos);


        /*
         * 那一格必須可以被火取代。
         */
        if (!replacedState.canBeReplaced()) {
            return;
        }


        /*
         * 如果你的 End Fire 有自己的：
         *
         * EndFireBlock.getState(...)
         *
         * 就把這行換成你的方法。
         */
        BlockState fireState =
                BlockRegister.EndFireBlock
                        .get()
                        .defaultBlockState();


        /*
         * 終界火必須能在這個位置存活。
         */
        if (!fireState.canSurvive(
                level,
                firePos
        )) {
            return;
        }


        Player player = event.getEntity();


        /*
         * 真正修改世界只在 Server。
         */
        if (!level.isClientSide) {

            level.setBlock(
                    firePos,
                    fireState,
                    11
            );


            level.playSound(
                    null,
                    firePos,
                    SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    level.random.nextFloat() * 0.4F + 0.8F
            );


            EquipmentSlot slot =
                    event.getHand()
                            == InteractionHand.MAIN_HAND

                            ? EquipmentSlot.MAINHAND
                            : EquipmentSlot.OFFHAND;


            /*
             * 消耗 1 點打火石耐久。
             */
            stack.hurtAndBreak(
                    1,
                    player,
                    slot
            );


            level.gameEvent(
                    player,
                    GameEvent.BLOCK_PLACE,
                    firePos
            );
        }


        /*
         * 阻止原版 FlintAndSteelItem
         * 接著再生成普通火。
         */
        event.setCancellationResult(
                InteractionResult.sidedSuccess(
                        level.isClientSide
                )
        );

        event.setCanceled(true);
    }
}
