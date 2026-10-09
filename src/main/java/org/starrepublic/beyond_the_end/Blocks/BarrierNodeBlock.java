package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.starrepublic.beyond_the_end.World.progression.BarrierNodeDirection;
import org.starrepublic.beyond_the_end.World.progression.EndBarrierProgressManager;

public class BarrierNodeBlock extends Block {

    public static final EnumProperty<BarrierNodeDirection>
            DIRECTION =
            EnumProperty.create(
                    "direction",
                    BarrierNodeDirection.class
            );


    public BarrierNodeBlock(
            Properties properties
    ) {

        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(
                                DIRECTION,
                                BarrierNodeDirection.NORTH
                        )
        );
    }


    /*
     * =========================
     * BlockState
     * =========================
     */

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    Block,
                    BlockState
                    > builder
    ) {

        builder.add(
                DIRECTION
        );
    }


    /*
     * =========================
     * Destroy
     * =========================
     */

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {

        /*
         * Server only
         */
        if (!level.isClientSide()
                && level instanceof ServerLevel serverLevel) {

            BarrierNodeDirection direction =
                    state.getValue(DIRECTION);

            EndBarrierProgressManager
                    .markNodeDestroyed(
                            serverLevel,
                            direction
                    );
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }
}
