package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.properties.WoodType;

public class WoodFenceGateBlock extends FenceGateBlock {

    public WoodFenceGateBlock(
            WoodType woodType,
            Properties properties
    ) {
        super(woodType, properties);
    }
}
