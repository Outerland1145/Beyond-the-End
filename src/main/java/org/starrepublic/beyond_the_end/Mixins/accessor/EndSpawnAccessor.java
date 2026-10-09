package org.starrepublic.beyond_the_end.Mixins.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface EndSpawnAccessor {

    @Accessor("END_SPAWN_POINT")
    @Mutable
    static void beyondTheEnd$setEndSpawnPoint(
            BlockPos pos
    ) {
        throw new AssertionError();
    }
}
