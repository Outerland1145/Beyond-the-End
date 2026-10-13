package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class EndSurfaceVegetationFeature
        extends Feature<NoneFeatureConfiguration> {


    /*
     * 每個 Feature 落點附近掃描：
     *
     * 7 × 7
     */
    private static final int RADIUS = 3;


    /*
     * 表面終界石變成終界沙的機率。
     */
    private static final float SAND_CHANCE = 0.30F;


    /*
     * 如果這格成功變成沙，
     * 沙上長草的機率。
     */
    private static final float GRASS_ON_SAND_CHANCE = 0.60F;


    /*
     * 沒有變成沙的原版終界石，
     * 仍然有較低概率長草。
     */
    private static final float GRASS_ON_END_STONE_CHANCE = 0.18F;


    /*
     * 已決定生成草後，
     * 高草的比例。
     */
    private static final float TALL_GRASS_CHANCE = 0.25F;


    public EndSurfaceVegetationFeature(
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

        boolean placedAnything = false;


        for (int dx = -RADIUS;
             dx <= RADIUS;
             dx++) {

            for (int dz = -RADIUS;
                 dz <= RADIUS;
                 dz++) {


                /*
                 * 不做規則圓形。
                 *
                 * 稍微隨機跳過部分格子，
                 * 讓整體更自然。
                 *
                 * 這不是草群聚，
                 * 只是避免 Feature 本身形成正方形。
                 */
                if (random.nextFloat() < 0.12F) {
                    continue;
                }


                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;


                BlockPos surface =
                        EndSurfaceFeatureHelper.getSurface(
                                level,
                                x,
                                z
                        );


                BlockState ground =
                        level.getBlockState(surface);


                /*
                 * 只處理原版終界石。
                 */
                if (!ground.is(Blocks.END_STONE)) {
                    continue;
                }


                /*
                 * 必須是真正表面。
                 *
                 * 上面空氣
                 * 下面不是空氣
                 */
                if (!EndSurfaceFeatureHelper.isValidSurface(
                        level,
                        surface
                )) {
                    continue;
                }


                /*
                 * =========================================
                 * 第一種結果：
                 *
                 * End Stone → End Sand
                 * =========================================
                 */

                if (random.nextFloat() < SAND_CHANCE) {

                    this.setBlock(
                            level,
                            surface,
                            BlockRegister.END_SAND
                                    .get()
                                    .defaultBlockState()
                    );

                    placedAnything = true;


                    /*
                     * 沙子不一定長草。
                     */
                    if (random.nextFloat()
                            < GRASS_ON_SAND_CHANCE) {

                        EndSurfaceFeatureHelper.placeRandomGrass(
                                level,
                                surface,
                                random,
                                TALL_GRASS_CHANCE
                        );
                    }

                    continue;
                }


                /*
                 * =========================================
                 * 第二種結果：
                 *
                 * End Stone 保持不變，
                 * 但是仍然可能長草。
                 * =========================================
                 */

                if (random.nextFloat()
                        < GRASS_ON_END_STONE_CHANCE) {

                    if (EndSurfaceFeatureHelper.placeRandomGrass(
                            level,
                            surface,
                            random,
                            TALL_GRASS_CHANCE
                    )) {

                        placedAnything = true;
                    }
                }
            }
        }


        return placedAnything;
    }
}
