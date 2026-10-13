package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public final class EndIslandDecorator {

    /*
     * 島嶼表面的終界石有多少比例變成終界沙。
     *
     * 0.35 = 35%
     */
    private static final float SAND_CHANCE = 0.35F;

    /*
     * 已經變成終界沙之後，
     * 有多少比例生成草。
     */
    private static final float GRASS_CHANCE = 0.55F;

    /*
     * 如果決定生成草，
     * 有多少機率是高草。
     */
    private static final float TALL_GRASS_CHANCE = 0.25F;


    /*
     * 原版 End Island 很小，
     * 掃描 8 格基本足夠。
     */
    private static final int HORIZONTAL_RADIUS = 8;

    /*
     * 原版島嶼主要向下延伸。
     */
    private static final int DOWN_SCAN = 12;

    /*
     * origin 上面稍微也掃一下，
     * 防止生成位置存在高度偏差。
     */
    private static final int UP_SCAN = 3;


    private EndIslandDecorator() {
    }


    public static void decorate(
            WorldGenLevel level,
            BlockPos origin,
            RandomSource random
    ) {

        for (int x = -HORIZONTAL_RADIUS;
             x <= HORIZONTAL_RADIUS;
             x++) {

            for (int z = -HORIZONTAL_RADIUS;
                 z <= HORIZONTAL_RADIUS;
                 z++) {

                /*
                 * 每一個 X/Z 柱子從上往下找。
                 *
                 * 這樣找到的就是島嶼真正的頂面。
                 */
                for (int y = UP_SCAN;
                     y >= -DOWN_SCAN;
                     y--) {

                    BlockPos pos =
                            origin.offset(x, y, z);

                    BlockState state =
                            level.getBlockState(pos);


                    /*
                     * 只處理原版終界石。
                     */
                    if (!state.is(Blocks.END_STONE)) {
                        continue;
                    }


                    /*
                     * ==============================
                     * 必須是小島「頂部表面」
                     * ==============================
                     */

                    BlockPos abovePos = pos.above();
                    BlockPos belowPos = pos.below();

                    BlockState aboveState =
                            level.getBlockState(abovePos);

                    BlockState belowState =
                            level.getBlockState(belowPos);


                    /*
                     * 上面必須是空氣。
                     *
                     * 否則這顆終界石是在島體內部。
                     */
                    if (!aboveState.isAir()) {
                        continue;
                    }


                    /*
                     * 下面不能是空氣。
                     *
                     * 這可以排除：
                     *
                     * 懸空凸出的單格終界石
                     * 島嶼非常薄的邊緣
                     *
                     * 只讓有實際島體支撐的地方變成沙。
                     */
                    if (belowState.isAir()) {
                        break;
                    }


                    /*
                     * ==============================
                     * 隨機替換成終界沙
                     * ==============================
                     */

                    if (random.nextFloat() < SAND_CHANCE) {

                        placeSandAndGrass(
                                level,
                                pos,
                                random
                        );
                    }


                    /*
                     * 已經找到這個 X/Z 的頂部了。
                     *
                     * 不再繼續往下掃，
                     * 否則會把島體內部也改成沙。
                     */
                    break;
                }
            }
        }
    }


    private static void placeSandAndGrass(
            WorldGenLevel level,
            BlockPos groundPos,
            RandomSource random
    ) {

        /*
         * ==============================
         * END STONE → END SAND
         * ==============================
         */

        level.setBlock(
                groundPos,
                BlockRegister.END_SAND
                        .get()
                        .defaultBlockState(),
                Block.UPDATE_ALL
        );


        /*
         * 沙子不一定長草。
         *
         * 所以會同時存在：
         *
         * 裸露終界沙
         * 終界沙 + 矮草
         * 終界沙 + 高草
         */
        if (random.nextFloat() >= GRASS_CHANCE) {
            return;
        }


        BlockPos plantPos =
                groundPos.above();


        if (!level.getBlockState(plantPos).isAir()) {
            return;
        }


        /*
         * ==============================
         * 高草
         * ==============================
         */

        if (random.nextFloat()
                < TALL_GRASS_CHANCE) {

            BlockPos upperPos =
                    plantPos.above();

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
                            Block.UPDATE_ALL
                    );

                    return;
                }
            }
        }


        /*
         * ==============================
         * 矮草
         * ==============================
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
                    Block.UPDATE_ALL
            );
        }
    }
}
