package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.function.Supplier;

public class WoodButtonBlock extends ButtonBlock {

    private static final int DEFAULT_PRESS_TICKS = 30;


    public WoodButtonBlock(
            BlockSetType type,
            Properties properties
    ) {
        super(
                type,
                DEFAULT_PRESS_TICKS,
                properties
        );
    }


    public WoodButtonBlock(
            BlockSetType type,
            int pressTicks,
            Properties properties
    ) {
        super(
                type,
                pressTicks,
                properties
        );
    }
}
