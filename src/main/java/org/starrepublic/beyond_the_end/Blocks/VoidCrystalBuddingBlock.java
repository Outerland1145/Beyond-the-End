package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

public class VoidCrystalBuddingBlock extends Block {

    /*
     * 和原版 Budding Amethyst 類似：
     *
     * 每次 random tick 約 1/5 機率嘗試生長。
     */
    private static final int GROWTH_CHANCE = 5;

    public VoidCrystalBuddingBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected void randomTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {

        /*
         * =========================================
         * 1. 高度限制
         * =========================================
         */

        if (!canGrowAtHeight(level, pos)) {
            return;
        }


        /*
         * =========================================
         * 2. 生長機率
         * =========================================
         */

        if (random.nextInt(GROWTH_CHANCE) != 0) {
            return;
        }


        /*
         * =========================================
         * 3. 隨機選擇一面
         * =========================================
         */

        Direction direction =
                Direction.getRandom(random);

        BlockPos targetPos =
                pos.relative(direction);

        BlockState targetState =
                level.getBlockState(targetPos);


        /*
         * =========================================
         * 4. 空氣 → 第一階段晶芽
         * =========================================
         */

        if (canCrystalGrowInto(level, targetPos)) {

            BlockState newState =
                    createCrystalState(
                            BlockRegister.VOID_CRYSTAL.smallcrystalbud
                                    .get()
                                    .defaultBlockState(),

                            direction,

                            level.getFluidState(targetPos)
                    );


            level.setBlockAndUpdate(
                    targetPos,
                    newState
            );

            return;
        }


        /*
         * =========================================
         * 5. 小晶芽 → 中晶芽
         * =========================================
         */

        if (
                targetState.is(
                        BlockRegister.VOID_CRYSTAL.smallcrystalbud.get()
                )
                        && isFacing(
                        targetState,
                        direction
                )
        ) {

            growTo(
                    level,
                    targetPos,
                    targetState,

                    BlockRegister.VOID_CRYSTAL.mediumcrystalbud
                            .get()
                            .defaultBlockState(),

                    direction
            );

            return;
        }


        /*
         * =========================================
         * 6. 中晶芽 → 大晶芽
         * =========================================
         */

        if (
                targetState.is(
                        BlockRegister.VOID_CRYSTAL.mediumcrystalbud.get()
                )
                        && isFacing(
                        targetState,
                        direction
                )
        ) {

            growTo(
                    level,
                    targetPos,
                    targetState,

                    BlockRegister.VOID_CRYSTAL.largecrystalbud
                            .get()
                            .defaultBlockState(),

                    direction
            );

            return;
        }


        /*
         * =========================================
         * 7. 大晶芽 → 完整晶簇
         * =========================================
         */

        if (
                targetState.is(
                        BlockRegister.VOID_CRYSTAL.largecrystalbud.get()
                )
                        && isFacing(
                        targetState,
                        direction
                )
        ) {

            growTo(
                    level,
                    targetPos,
                    targetState,

                    BlockRegister.VOID_CRYSTAL.crystalcluster
                            .get()
                            .defaultBlockState(),

                    direction
            );
        }
    }


    /*
     * =============================================
     * 高度限制
     * =============================================
     */

    private boolean canGrowAtHeight(
            ServerLevel level,
            BlockPos pos
    ) {

        int minY =
                level.getMinBuildHeight();

        int allowedHeight =
                BTEConfigs.SERVER
                        .VoidCrystalGrowthHeight
                        .get();


        return pos.getY()
                < minY + allowedHeight;
    }


    /*
     * =============================================
     * 是否能生成第一階段晶芽
     * =============================================
     */

    private boolean canCrystalGrowInto(
            ServerLevel level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);


        /*
         * 普通空氣。
         */
        if (state.isAir()) {
            return true;
        }


        /*
         * 保留 Amethyst Cluster 類方塊原版式
         * Waterlogged 支援。
         *
         * 終界通常沒有水，
         * 但其他模組維度可能有。
         */
        FluidState fluidState =
                level.getFluidState(pos);

        return fluidState.is(FluidTags.WATER)
                && fluidState.isSource();
    }


    /*
     * =============================================
     * 建立具有朝向的晶體 BlockState
     * =============================================
     */

    private BlockState createCrystalState(
            BlockState state,
            Direction direction,
            FluidState replacedFluid
    ) {

        if (
                state.hasProperty(
                        AmethystClusterBlock.FACING
                )
        ) {

            state = state.setValue(
                    AmethystClusterBlock.FACING,
                    direction
            );
        }


        if (
                state.hasProperty(
                        AmethystClusterBlock.WATERLOGGED
                )
        ) {

            state = state.setValue(
                    AmethystClusterBlock.WATERLOGGED,

                    replacedFluid.is(
                            FluidTags.WATER
                    )
            );
        }


        return state;
    }


    /*
     * =============================================
     * 生長到下一階段
     * =============================================
     */

    private void growTo(
            ServerLevel level,
            BlockPos pos,
            BlockState oldState,
            BlockState newState,
            Direction direction
    ) {

        /*
         * 保留 Waterlogged。
         */
        boolean waterlogged =
                oldState.hasProperty(
                        AmethystClusterBlock.WATERLOGGED
                )
                        && oldState.getValue(
                        AmethystClusterBlock.WATERLOGGED
                );


        newState =
                createCrystalState(
                        newState,
                        direction,
                        level.getFluidState(pos)
                );


        if (
                newState.hasProperty(
                        AmethystClusterBlock.WATERLOGGED
                )
        ) {

            newState = newState.setValue(
                    AmethystClusterBlock.WATERLOGGED,
                    waterlogged
            );
        }


        level.setBlockAndUpdate(
                pos,
                newState
        );
    }


    /*
     * 只有和母岩連接方向一致的晶芽，
     * 才能由該母岩繼續催生成長。
     */
    private boolean isFacing(
            BlockState state,
            Direction direction
    ) {

        if (
                !state.hasProperty(
                        AmethystClusterBlock.FACING
                )
        ) {
            return false;
        }


        return state.getValue(
                AmethystClusterBlock.FACING
        ) == direction;
    }
}
