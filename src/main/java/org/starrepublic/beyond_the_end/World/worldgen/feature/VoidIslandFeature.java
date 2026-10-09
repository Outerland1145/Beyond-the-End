package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;

import net.minecraft.util.RandomSource;

import net.minecraft.world.level.WorldGenLevel;

import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;

public class VoidIslandFeature
        extends Feature<NoneFeatureConfiguration> {

    public VoidIslandFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {

        BlockPos origin =
                context.origin();

        /*
         * 保證只在 20000+。
         */

        if (!EndTerrainController.isVoid(
                origin.getX(),
                origin.getZ()
        )) {
            return false;
        }

        RandomSource random =
                context.random();

        /*
         * ==========================================
         * 額外稀有度
         * ==========================================
         *
         * PlacedFeature 已有 rarity_filter，
         * 這裡再做一次可以讓虛無更加空曠。
         */

        if (random.nextFloat() > 0.32F) {
            return false;
        }

        WorldGenLevel level =
                context.level();

        /*
         * 隨機高度。
         *
         * 終界主體通常在 Y 60 左右，
         * 這裡故意讓小島上下浮動。
         */

        int centerY =
                42 + random.nextInt(45);

        BlockPos center =
                new BlockPos(
                        origin.getX(),
                        centerY,
                        origin.getZ()
                );

        /*
         * 小島半徑。
         */

        int radiusX =
                3 + random.nextInt(6);

        int radiusZ =
                3 + random.nextInt(6);

        int thickness =
                2 + random.nextInt(4);

        generateIsland(
                level,
                center,
                radiusX,
                radiusZ,
                thickness,
                random
        );

        return true;
    }

    private void generateIsland(
            WorldGenLevel level,
            BlockPos center,
            int radiusX,
            int radiusZ,
            int thickness,
            RandomSource random
    ) {

        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        for (
                int x = -radiusX;
                x <= radiusX;
                x++
        ) {

            for (
                    int z = -radiusZ;
                    z <= radiusZ;
                    z++
            ) {

                /*
                 * 橢圓。
                 */

                double nx =
                        (double) x / radiusX;

                double nz =
                        (double) z / radiusZ;

                double horizontalDistance =
                        nx * nx
                                +
                                nz * nz;

                /*
                 * 加入隨機邊緣，
                 * 防止變成太規則的飛碟。
                 */

                double edgeNoise =
                        random.nextDouble()
                                * 0.28D;

                if (
                        horizontalDistance
                                >
                                1.0D - edgeNoise
                ) {
                    continue;
                }

                /*
                 * 中央厚，邊緣薄。
                 */

                int localDepth =
                        Math.max(
                                1,
                                (int) (
                                        thickness
                                                *
                                                (
                                                        1.0D
                                                                -
                                                                horizontalDistance
                                                )
                                )
                                        +
                                        random.nextInt(2)
                        );

                for (
                        int y = 0;
                        y >= -localDepth;
                        y--
                ) {

                    mutable.set(
                            center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z
                    );

                    /*
                     * 不覆蓋已有內容。
                     */

                    if (
                            level.isEmptyBlock(
                                    mutable
                            )
                    ) {

                        level.setBlock(
                                mutable,
                                Blocks.END_STONE
                                        .defaultBlockState(),
                                2
                        );
                    }
                }
            }
        }
    }
}
