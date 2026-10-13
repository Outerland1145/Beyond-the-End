package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public final class EndSurfaceFeatureHelper {

    private EndSurfaceFeatureHelper() {
    }


    /**
     * 取得指定 X/Z 最上方的實體表面。
     *
     * WORLD_SURFACE_WG 取得的是表面上方的位置，
     * 所以需要 -1。
     */
    public static BlockPos getSurface(
            WorldGenLevel level,
            int x,
            int z
    ) {

        int y = level.getHeight(
                Heightmap.Types.WORLD_SURFACE_WG,
                x,
                z
        ) - 1;

        return new BlockPos(x, y, z);
    }


    /**
     * 基本的終界表面判定：
     *
     * 上面必須是空氣
     * 下面必須不是空氣
     */
    public static boolean isValidSurface(
            WorldGenLevel level,
            BlockPos pos
    ) {

        return level.getBlockState(pos.above()).isAir()
                && !level.getBlockState(pos.below()).isAir();
    }


    /**
     * 放置隨機高 / 矮草。
     *
     * tallChance：
     * 決定已經要生成草之後，有多少機率是高草。
     */
    public static boolean placeRandomGrass(
            WorldGenLevel level,
            BlockPos groundPos,
            RandomSource random,
            float tallChance
    ) {

        BlockPos plantPos = groundPos.above();

        if (!level.getBlockState(plantPos).isAir()) {
            return false;
        }


        /*
         * =====================================
         * 高草
         * =====================================
         */

        if (random.nextFloat() < tallChance) {

            BlockPos upperPos = plantPos.above();

            if (level.getBlockState(upperPos).isAir()) {

                BlockState tallGrass =
                        BlockRegister.END_Tall_Grass_Block
                                .get()
                                .defaultBlockState();

                if (tallGrass.canSurvive(
                        level,
                        plantPos
                )) {

                    DoublePlantBlock.placeAt(
                            level,
                            tallGrass,
                            plantPos,
                            Block.UPDATE_CLIENTS
                    );

                    return true;
                }
            }
        }


        /*
         * =====================================
         * 矮草
         * =====================================
         */

        BlockState shortGrass =
                BlockRegister.END_Short_Grass_Block
                        .get()
                        .defaultBlockState();

        if (shortGrass.canSurvive(
                level,
                plantPos
        )) {

            level.setBlock(
                    plantPos,
                    shortGrass,
                    Block.UPDATE_CLIENTS
            );

            return true;
        }

        return false;
    }
}
