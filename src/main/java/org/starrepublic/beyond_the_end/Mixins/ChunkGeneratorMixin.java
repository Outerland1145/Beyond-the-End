package org.starrepublic.beyond_the_end.Mixins;


import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;
import org.starrepublic.beyond_the_end.World.worldgen.feature.NestIslandDecorationGenerator;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Inject(
            method = "applyBiomeDecoration",
            at = @At("TAIL")
    )
    private void beyondTheEnd$decorateNestIsland(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structureManager,
            CallbackInfo ci
    ) {

        /*
         * 只處理終界。
         */
        if (
                !level
                        .getLevel()
                        .dimension()
                        .equals(
                                Level.END
                        )
        ) {

            return;
        }


        if (
                !EndTerrainController
                        .isCustomTerrainEnabled()
        ) {

            return;
        }


        NestIslandDecorationGenerator
                .decorateChunk(
                        level,
                        chunk
                );
    }
}
