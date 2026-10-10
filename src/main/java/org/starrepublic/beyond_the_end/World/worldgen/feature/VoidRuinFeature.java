package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;

import net.minecraft.util.RandomSource;

import net.minecraft.world.level.WorldGenLevel;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;

public class VoidRuinFeature
        extends Feature<NoneFeatureConfiguration> {

    public VoidRuinFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {
        if (
                !EndTerrainController
                        .isCustomTerrainEnabled()
        ) {

            return false;
        }
        BlockPos origin =
                context.origin();

        if (!EndTerrainController.isVoid(
                origin.getX(),
                origin.getZ()
        )) {
            return false;
        }

        RandomSource random =
                context.random();

        /*
         * 廢墟非常罕見。
         */

        if (random.nextFloat() > 0.15F) {
            return false;
        }

        WorldGenLevel level =
                context.level();

        int y =
                50 + random.nextInt(31);

        BlockPos center =
                new BlockPos(
                        origin.getX(),
                        y,
                        origin.getZ()
                );

        /*
         * 建築不能直接懸空，
         * 所以先做一個承載小島。
         */

        generateBaseIsland(
                level,
                center,
                random
        );

        /*
         * 廢墟。
         */

        generateRuin(
                level,
                center.above(),
                random
        );

        return true;
    }

    private void generateBaseIsland(
            WorldGenLevel level,
            BlockPos center,
            RandomSource random
    ) {

        int radius =
                6 + random.nextInt(5);

        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        for (
                int x = -radius;
                x <= radius;
                x++
        ) {

            for (
                    int z = -radius;
                    z <= radius;
                    z++
            ) {

                double distance =
                        Math.sqrt(
                                x * x
                                        +
                                        z * z
                        );

                if (
                        distance
                                >
                                radius
                                        -
                                        random.nextDouble()
                                                * 1.8D
                ) {
                    continue;
                }

                int depth =
                        Math.max(
                                1,
                                (int) (
                                        (radius - distance)
                                                * 0.55D
                                )
                        );

                for (
                        int y = 0;
                        y >= -depth;
                        y--
                ) {

                    mutable.set(
                            center.getX() + x,
                            center.getY() + y,
                            center.getZ() + z
                    );

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

    private void generateRuin(
            WorldGenLevel level,
            BlockPos center,
            RandomSource random
    ) {

        /*
         * 破敗方形基座。
         */

        for (
                int x = -3;
                x <= 3;
                x++
        ) {

            for (
                    int z = -3;
                    z <= 3;
                    z++
            ) {

                /*
                 * 隨機缺損。
                 */

                if (
                        random.nextFloat()
                                <
                                0.18F
                ) {
                    continue;
                }

                place(
                        level,
                        center.offset(
                                x,
                                0,
                                z
                        ),
                        Blocks.END_STONE_BRICKS
                );
            }
        }

        /*
         * 四個殘柱。
         */

        generateColumn(
                level,
                center.offset(
                        -3,
                        1,
                        -3
                ),
                2 + random.nextInt(6),
                random
        );

        generateColumn(
                level,
                center.offset(
                        3,
                        1,
                        -3
                ),
                2 + random.nextInt(6),
                random
        );

        generateColumn(
                level,
                center.offset(
                        -3,
                        1,
                        3
                ),
                2 + random.nextInt(6),
                random
        );

        generateColumn(
                level,
                center.offset(
                        3,
                        1,
                        3
                ),
                2 + random.nextInt(6),
                random
        );

        /*
         * 中央破碎柱。
         */

        int centerHeight =
                3 + random.nextInt(7);

        for (
                int y = 1;
                y <= centerHeight;
                y++
        ) {

            if (
                    random.nextFloat()
                            <
                            0.13F
            ) {
                continue;
            }

            Block block =
                    random.nextBoolean()
                            ?
                            Blocks.PURPUR_PILLAR
                            :
                            Blocks.PURPUR_BLOCK;

            place(
                    level,
                    center.above(y),
                    block
            );
        }
    }

    private void generateColumn(
            WorldGenLevel level,
            BlockPos pos,
            int height,
            RandomSource random
    ) {

        for (
                int y = 0;
                y < height;
                y++
        ) {

            /*
             * 高處越容易斷裂。
             */

            float breakChance =
                    0.05F
                            +
                            y * 0.035F;

            if (
                    random.nextFloat()
                            <
                            breakChance
            ) {
                continue;
            }

            Block block =
                    random.nextFloat()
                            <
                            0.68F
                            ?
                            Blocks.END_STONE_BRICKS
                            :
                            Blocks.PURPUR_PILLAR;

            place(
                    level,
                    pos.above(y),
                    block
            );
        }
    }

    private void place(
            WorldGenLevel level,
            BlockPos pos,
            Block block
    ) {

        if (
                level.isEmptyBlock(pos)
        ) {

            BlockState state =
                    block.defaultBlockState();

            level.setBlock(
                    pos,
                    state,
                    2
            );
        }
    }
}
