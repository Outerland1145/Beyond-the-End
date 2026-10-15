package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.Tags;

import java.util.ArrayList;
import java.util.List;

public class EnchantedGrassMeadowFeature
        extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_RADIUS = 11;
    private static final int MAX_RADIUS = 15;

    /*
     * 每一片草原由數個互相重疊的草團組成。
     */
    private static final int MIN_PATCHES = 4;
    private static final int MAX_PATCHES = 7;

    /*
     * 草非常密。
     */
    private static final float GRASS_DENSITY = 0.82F;

    /*
     * 約 18% 是高草。
     */
    private static final float TALL_GRASS_CHANCE = 0.18F;


    public EnchantedGrassMeadowFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }


    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {

        WorldGenLevel level =
                context.level();

        RandomSource random =
                context.random();

        BlockPos origin =
                context.origin();


        int radius =
                MIN_RADIUS
                        + random.nextInt(
                        MAX_RADIUS - MIN_RADIUS + 1
                );


        int patchCount =
                MIN_PATCHES
                        + random.nextInt(
                        MAX_PATCHES - MIN_PATCHES + 1
                );


        List<GrassPatch> patches =
                new ArrayList<>();


        /*
         * 生成數個草原核心。
         *
         * 它們互相重疊後，
         * 就不會形成完美圓形。
         */
        for (int i = 0; i < patchCount; i++) {

            int offsetX =
                    random.nextInt(
                            radius + 1
                    ) - radius / 2;

            int offsetZ =
                    random.nextInt(
                            radius + 1
                    ) - radius / 2;

            int patchRadius =
                    5 + random.nextInt(5);


            patches.add(
                    new GrassPatch(
                            origin.getX() + offsetX,
                            origin.getZ() + offsetZ,
                            patchRadius
                    )
            );
        }


        int placed =
                0;


        for (
                int dx = -radius;
                dx <= radius;
                dx++
        ) {

            for (
                    int dz = -radius;
                    dz <= radius;
                    dz++
            ) {

                int x =
                        origin.getX() + dx;

                int z =
                        origin.getZ() + dz;


                /*
                 * 不在任何草團裡就跳過。
                 */
                if (!insideAnyPatch(
                        x,
                        z,
                        patches
                )) {
                    continue;
                }


                /*
                 * 即使在草原中也保留約 18% 空隙。
                 *
                 * 不會變成每一格都長草的人工地毯。
                 */
                if (
                        random.nextFloat()
                                > GRASS_DENSITY
                ) {
                    continue;
                }


                int y =
                        level.getHeight(
                                Heightmap.Types
                                        .MOTION_BLOCKING_NO_LEAVES,
                                x,
                                z
                        );


                BlockPos plantPos =
                        new BlockPos(
                                x,
                                y,
                                z
                        );


                BlockPos groundPos =
                        plantPos.below();


                BlockState ground =
                        level.getBlockState(
                                groundPos
                        );


                /*
                 * 只能生在附魔之地的自然地表。
                 */
                if (
                        !ground.is(
                                Tags.ENCHANTED_FEATURE_GROUND
                        )
                ) {
                    continue;
                }


                /*
                 * 植物位置必須為空。
                 */
                if (
                        !level.getBlockState(
                                plantPos
                        ).isAir()
                ) {
                    continue;
                }


                /*
                 * =========================
                 * Tall Grass
                 * =========================
                 */
                if (
                        random.nextFloat()
                                < TALL_GRASS_CHANCE
                ) {

                    /*
                     * 換成你的矮草註冊名稱。
                     */
                    BlockState shortState =
                            BlockRegister
                                    .Enchanted_Tall_Grass_Block
                                    .get()
                                    .defaultBlockState();


                    if (
                            !shortState.canSurvive(
                                    level,
                                    plantPos
                            )
                    ) {
                        continue;
                    }


                    level.setBlock(
                            plantPos,
                            shortState,
                            Block.UPDATE_CLIENTS
                    );


                    placed++;
                }

                /*
                 * =========================
                 * Short Grass
                 * =========================
                 */
                else {

                    /*
                     * 換成你的矮草註冊名稱。
                     */
                    BlockState shortState =
                            BlockRegister
                                    .Enchanted_Short_Grass_Block
                                    .get()
                                    .defaultBlockState();


                    if (
                            !shortState.canSurvive(
                                    level,
                                    plantPos
                            )
                    ) {
                        continue;
                    }


                    level.setBlock(
                            plantPos,
                            shortState,
                            Block.UPDATE_CLIENTS
                    );


                    placed++;
                }
            }
        }


        return placed > 0;
    }


    private boolean insideAnyPatch(
            int x,
            int z,
            List<GrassPatch> patches
    ) {

        for (GrassPatch patch : patches) {

            int dx =
                    x - patch.x();

            int dz =
                    z - patch.z();


            if (
                    dx * dx + dz * dz
                            <= patch.radius()
                            * patch.radius()
            ) {
                return true;
            }
        }

        return false;
    }


    private record GrassPatch(
            int x,
            int z,
            int radius
    ) {
    }
}
