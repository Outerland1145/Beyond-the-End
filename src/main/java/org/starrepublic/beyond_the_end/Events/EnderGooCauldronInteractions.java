package org.starrepublic.beyond_the_end.Events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.starrepublic.beyond_the_end.Blocks.EnderGooCauldronBlock;
import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.ItemRegister;

public final class EnderGooCauldronInteractions {
    private EnderGooCauldronInteractions() {
    }

    public static void bootstrap() {

        /*
         * Vanilla empty cauldron
         * +
         * Ender Goo Bucket
         *
         * ↓
         *
         * Ender Goo filled cauldron
         */
        CauldronInteraction.EMPTY
                .map()
                .put(
                        ItemRegister.ENDER_GOO_BUCKET.get(),

                        (
                                state,
                                level,
                                pos,
                                player,
                                hand,
                                stack
                        ) -> CauldronInteraction.emptyBucket(
                                level,
                                pos,
                                player,
                                hand,
                                stack,

                                BlockRegister
                                        .ENDER_GOO_CAULDRON
                                        .get()
                                        .defaultBlockState(),

                                SoundEvents.BUCKET_EMPTY_LAVA
                        )
                );


        /*
         * Ender Goo Cauldron
         * +
         * Empty Bucket
         *
         * ↓
         *
         * Vanilla Cauldron
         * +
         * Ender Goo Bucket
         */
        EnderGooCauldronBlock.INTERACTIONS
                .map()
                .put(
                        Items.BUCKET,

                        (
                                state,
                                level,
                                pos,
                                player,
                                hand,
                                stack
                        ) -> CauldronInteraction.fillBucket(
                                state,
                                level,
                                pos,
                                player,
                                hand,
                                stack,

                                new ItemStack(
                                        ItemRegister
                                                .ENDER_GOO_BUCKET
                                                .get()
                                ),

                                ignored -> true,

                                SoundEvents.BUCKET_FILL_LAVA
                        )
                );
        EnderGooCauldronBlock.INTERACTIONS
                .map()
                .put(
                        Items.BUCKET,
                        EnderGooCauldronInteractions::takeEnderGoo
                );
    }
    private static ItemInteractionResult takeEnderGoo(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            ItemStack emptyBucket
    ) {

        if (!level.isClientSide) {

            ItemStack gooBucket =
                    new ItemStack(
                            ItemRegister.ENDER_GOO_BUCKET.get()
                    );

            player.setItemInHand(
                    hand,
                    ItemUtils.createFilledResult(
                            emptyBucket,
                            player,
                            gooBucket
                    )
            );

            /*
             * 回到真正的 Vanilla 空鍋釜。
             */
            level.setBlockAndUpdate(
                    pos,
                    Blocks.CAULDRON.defaultBlockState()
            );

            level.playSound(
                    null,
                    pos,
                    SoundEvents.BUCKET_FILL_LAVA,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );

            level.gameEvent(
                    player,
                    GameEvent.FLUID_PICKUP,
                    pos
            );

            player.awardStat(
                    Stats.USE_CAULDRON
            );
        }

        return ItemInteractionResult.sidedSuccess(
                level.isClientSide
        );
    }
}
