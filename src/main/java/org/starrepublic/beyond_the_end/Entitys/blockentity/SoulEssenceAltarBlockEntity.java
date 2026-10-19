package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;

public class SoulEssenceAltarBlockEntity
        extends EssenceAltarBlockEntity {

    public SoulEssenceAltarBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                BlockEntityRegister.SOUL_ESSENCE_ALTAR.get(),
                pos,
                state
        );
    }

    @Override
    public String getEssenceType() {
        return "soul";
    }
}
