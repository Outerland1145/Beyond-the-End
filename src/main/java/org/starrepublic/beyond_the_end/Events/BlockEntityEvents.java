package org.starrepublic.beyond_the_end.Events;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public final class BlockEntityEvents {
    private BlockEntityEvents() {}

    public static void addValidBlocks(BlockEntityTypeAddBlocksEvent event) {

        event.modify(
                BlockEntityType.CAMPFIRE,
                BlockRegister.END_CAMPFIRE.get()
        );
    }
}
