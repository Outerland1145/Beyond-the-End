package org.starrepublic.beyond_the_end.Entitys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;

public class EndNetherPortalBlockEntity  extends TheEndPortalBlockEntity {

    public EndNetherPortalBlockEntity(
            BlockPos pos,
            BlockState state) {

        super(
                BlockEntityRegister.END_NETHER_PORTAL.get(),
                pos,
                state
        );
    }

    @Override
    public boolean shouldRenderFace(Direction direction) {

        Direction.Axis axis =
                this.getBlockState()
                        .getValue(BlockStateProperties.HORIZONTAL_AXIS);

        /*
         * AXIS X:
         *
         * ███████████
         *
         * 門面沿 X 延伸
         * 所以觀看面是 NORTH / SOUTH
         *
         *
         * AXIS Z:
         *
         * 門面沿 Z 延伸
         * 所以觀看面是 EAST / WEST
         */

        if (axis == Direction.Axis.X) {
            return direction == Direction.NORTH
                    || direction == Direction.SOUTH;
        }

        return direction == Direction.EAST
                || direction == Direction.WEST;
    }
}
