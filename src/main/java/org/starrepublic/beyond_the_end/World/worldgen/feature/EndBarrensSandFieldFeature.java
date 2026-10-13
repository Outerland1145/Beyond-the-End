package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class EndBarrensSandFieldFeature
        extends Feature<NoneFeatureConfiguration> {


    /*
     * 大型沙面的大小。
     */
    private static final int MIN_RADIUS = 7;
    private static final int MAX_RADIUS = 13;


    /*
     * 沙面上的草生成率。
     */
    private static final float GRASS_CHANCE = 0.42F;


    /*
     * 草生成後，高草的概率。
     */
    private static final float TALL_GRASS_CHANCE = 0.22F;


    public EndBarrensSandFieldFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }


    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();


        int radius = Mth.nextInt(
                random,
                MIN_RADIUS,
                MAX_RADIUS
        );


        int placed = 0;


        for (int dx = -radius;
             dx <= radius;
             dx++) {

            for (int dz = -radius;
                 dz <= radius;
                 dz++) {


                double distance =
                        Math.sqrt(
                                dx * dx
                                        + dz * dz
                        );


                /*
                 * 完全超出圓形範圍。
                 */
                if (distance > radius) {
                    continue;
                }


                /*
                 * =========================================
                 * 不規則邊緣
                 *
                 * 中心區全部保留。
                 *
                 * 最外側約 2 格則隨機缺失，
                 * 避免生成一個完美圓盤。
                 * =========================================
                 */

                double edgeDistance =
                        radius - distance;


                if (edgeDistance < 2.0D) {

                    float edgeChance =
                            (float) (edgeDistance / 2.0D);

                    if (random.nextFloat() > edgeChance) {
                        continue;
                    }
                }


                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;


                BlockPos surface =
                        EndSurfaceFeatureHelper.getSurface(
                                level,
                                x,
                                z
                        );


                /*
                 * 必須符合大型沙面的嚴格地形條件。
                 */
                if (!canBecomeSandField(
                        level,
                        surface
                )) {
                    continue;
                }


                /*
                 * =========================================
                 * 轉換為沙
                 * =========================================
                 */

                BlockState current =
                        level.getBlockState(surface);


                if (!current.is(
                        BlockRegister.END_SAND.get()
                )) {

                    this.setBlock(
                            level,
                            surface,
                            BlockRegister.END_SAND
                                    .get()
                                    .defaultBlockState()
                    );
                }


                placed++;


                /*
                 * =========================================
                 * 草
                 *
                 * 維持你要的完全獨立隨機。
                 * 不做聚集。
                 * =========================================
                 */

                if (random.nextFloat()
                        < GRASS_CHANCE) {

                    EndSurfaceFeatureHelper.placeRandomGrass(
                            level,
                            surface,
                            random,
                            TALL_GRASS_CHANCE
                    );
                }
            }
        }


        return placed > 0;
    }


    /**
     * 大型沙面的嚴格生成條件。
     *
     * 需求：
     *
     * 1. 當前必須是 End Stone 或 End Sand
     * 2. 上方必須是空氣
     * 3. 下方必須非空氣
     * 4. 水平周圍 8 格全部非空氣
     *
     * 因此：
     *
     * 懸崖
     * 島緣
     * 細小突起
     * 薄平台邊緣
     *
     * 都不會被沙化。
     */
    private boolean canBecomeSandField(
            WorldGenLevel level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);


        if (!state.is(Blocks.END_STONE)
                && !state.is(
                BlockRegister.END_SAND.get()
        )) {

            return false;
        }


        /*
         * 上方必須空氣。
         */
        if (!level.getBlockState(
                pos.above()
        ).isAir()) {

            return false;
        }


        /*
         * 下方必須有島體。
         */
        if (level.getBlockState(
                pos.below()
        ).isAir()) {

            return false;
        }


        /*
         * =========================================
         * 水平周圍八格：
         *
         * X X X
         * X O X
         * X X X
         *
         * 全部必須非空氣。
         * =========================================
         */

        for (int dx = -1;
             dx <= 1;
             dx++) {

            for (int dz = -1;
                 dz <= 1;
                 dz++) {

                if (dx == 0 && dz == 0) {
                    continue;
                }


                BlockPos neighbor =
                        pos.offset(
                                dx,
                                0,
                                dz
                        );


                if (level.getBlockState(
                        neighbor
                ).isAir()) {

                    return false;
                }
            }
        }


        return true;
    }
}
