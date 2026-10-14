package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class DecorativeEndGatewayBlockEntity extends TheEndPortalBlockEntity {

    public DecorativeEndGatewayBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(

                BlockEntityRegister.DECORATIVE_END_GATEWAY.get(),
                pos,
                state
        );
    }

    /*
     * 如果旁邊也是同一種星空方塊，
     * 就不畫兩個方塊中間的面。
     *
     * 這讓它可以做成大面積牆壁。
     */
    @Override
    public boolean shouldRenderFace(Direction direction) {

        if (level == null) {
            return true;
        }

        BlockState neighbor =
                level.getBlockState(worldPosition.relative(direction));

        return !neighbor.is(BlockRegister.DECORATIVE_END_GATEWAY);
    }
}
