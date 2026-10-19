package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;

public class FireEssenceAltarBlockEntity
        extends EssenceAltarBlockEntity {

    public FireEssenceAltarBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                BlockEntityRegister.FIRE_ESSENCE_ALTAR.get(),
                pos,
                state
        );
    }

    @Override
    public String getEssenceType() {
        return "fire";
    }
}
