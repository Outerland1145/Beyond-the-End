package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.function.Supplier;

public class WoodPillarBlock extends RotatedPillarBlock {
    private final Supplier<? extends RotatedPillarBlock> strippedBlock;


    public WoodPillarBlock(
            Supplier<? extends RotatedPillarBlock> strippedBlock,
            BlockBehaviour.Properties properties
    ) {
        super(properties);
        this.strippedBlock = strippedBlock;
    }


    @Override
    public BlockState getToolModifiedState(
            BlockState state,
            UseOnContext context,
            ItemAbility itemAbility,
            boolean simulate
    ) {

        if (itemAbility == ItemAbilities.AXE_STRIP) {

            BlockState strippedState =
                    strippedBlock.get()
                            .defaultBlockState();

            /*
             * 保留原木方向。
             *
             * 例如：
             *
             * X 軸原木
             * ↓
             * X 軸去皮原木
             */
            return strippedState.setValue(
                    AXIS,
                    state.getValue(AXIS)
            );
        }

        return super.getToolModifiedState(
                state,
                context,
                itemAbility,
                simulate
        );
    }
}
