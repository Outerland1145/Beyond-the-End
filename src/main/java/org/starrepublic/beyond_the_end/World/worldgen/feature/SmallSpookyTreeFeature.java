package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SmallSpookyTreeFeature extends Feature<SpookyTreeConfiguration> {


    private enum CrownType {
        PYRAMID,
        BULGING,
        SPHERICAL
    }

    public SmallSpookyTreeFeature(Codec<SpookyTreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpookyTreeConfiguration> context) {

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        SpookyTreeConfiguration config = context.config();

        BlockPos ground = origin.below();

        /*
         * 必須有真正地面。
         */
        if (!isValidGround(level, ground)) {
            return false;
        }

        /*
         * 小型樹不像大型幽靈木需要巨大的底盤，
         * 但仍避免直接貼著島嶼懸崖生成。
         */
        if (!hasEnoughGround(level, ground)) {
            return false;
        }



        int treeHeight =
                4 + random.nextInt(
                        4 + random.nextInt(4)
                );



        CrownType type;

        if (random.nextInt(4) == 0) {

            type = CrownType.PYRAMID;

        } else {

            type = random.nextBoolean()
                    ? CrownType.BULGING
                    : CrownType.SPHERICAL;
        }

        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();

        /*
         * =========================================================
         * 樹幹
         * =========================================================
         */

        for (int y = 0; y < treeHeight; y++) {

            logs.add(
                    origin.above(y)
            );
        }



        switch (type) {

            case PYRAMID ->
                    generatePyramidCrown(
                            origin,
                            treeHeight,
                            random,
                            leaves
                    );

            case BULGING ->
                    generateBulgingCrown(
                            origin,
                            treeHeight,
                            random,
                            leaves,
                            false
                    );

            case SPHERICAL ->
                    generateBulgingCrown(
                            origin,
                            treeHeight,
                            random,
                            leaves,
                            true
                    );
        }

        /*
         * =========================================================
         * 完整空間判定
         * =========================================================
         *
         * 先判斷，成功後才放置。
         *
         * 避免生成到一半才發現撞到方塊，
         * 留下一根半截樹。
         */

        if (!canGenerate(
                level,
                logs,
                leaves
        )) {
            return false;
        }

        /*
         * =========================================================
         * 原木
         * =========================================================
         */

        for (BlockPos pos : logs) {

            boolean exposedEnd =
                    hasExposedLogEnd(
                            level,
                            pos,
                            logs,
                            leaves
                    );


            BlockState state =
                    exposedEnd
                            ? config.woodProvider()
                            .getState(random, pos)
                            : config.trunkProvider()
                            .getState(random, pos);


            if (!exposedEnd
                    && state.hasProperty(
                    RotatedPillarBlock.AXIS
            )) {

                state = state.setValue(
                        RotatedPillarBlock.AXIS,
                        Direction.Axis.Y
                );
            }

            level.setBlock(
                    pos,
                    state,
                    Block.UPDATE_CLIENTS
            );
        }

        Map<BlockPos, Integer> distances =
                calculateLeafDistances(
                        logs,
                        leaves
                );

        /*
         * =========================================================
         * 放置樹葉
         * =========================================================
         */

        for (BlockPos pos : leaves) {

            if (!canReplace(level, pos)) {
                continue;
            }

            BlockState state =
                    config.leavesProvider()
                            .getState(random, pos);

            if (state.hasProperty(
                    LeavesBlock.DISTANCE
            )) {

                state = state.setValue(
                        LeavesBlock.DISTANCE,
                        distances.getOrDefault(
                                pos,
                                7
                        )
                );
            }

            if (state.hasProperty(
                    LeavesBlock.PERSISTENT
            )) {

                state = state.setValue(
                        LeavesBlock.PERSISTENT,
                        false
                );
            }

            level.setBlock(
                    pos,
                    state,
                    Block.UPDATE_CLIENTS
            );
        }

        return true;
    }



    private void generatePyramidCrown(
            BlockPos origin,
            int treeHeight,
            RandomSource random,
            Set<BlockPos> leaves
    ) {

        /*
         * 原 HEE：
         *
         * treeHeight < 5
         *     ? 0
         *     : rand.nextInt(3)
         */

        int bottomExtraHeight =
                treeHeight < 5
                        ? 0
                        : random.nextInt(3);

        /*
         * 樹冠有時會稍微離樹幹頂部再高一點，
         * 這也是原始 HEE 那種奇怪幾何感的來源。
         */

        int yOffset =
                bottomExtraHeight > 0
                        ? random.nextInt(3)
                        : random.nextInt(2);

        int baseY =
                origin.getY() + treeHeight;

        int topY =
                baseY + yOffset;

        /*
         * 中央豎線。
         */

        for (int y = topY;
             y >= baseY;
             y--) {

            leaves.add(
                    new BlockPos(
                            origin.getX(),
                            y,
                            origin.getZ()
                    )
            );
        }

        /*
         * 第二層：
         *
         *     #
         *    ###
         */

        addCardinalLeaves(
                origin.getX(),
                topY - 1,
                origin.getZ(),
                1,
                leaves
        );

        /*
         * 底部 3x3 空心中心層。
         *
         * 中心位置本身由中軸處理，
         * 周圍八格形成完整樹冠。
         */

        int lowestY =
                topY
                        - 3
                        - bottomExtraHeight;

        for (int y = topY - 2;
             y >= lowestY;
             y--) {

            addRing3x3(
                    origin.getX(),
                    y,
                    origin.getZ(),
                    leaves
            );
        }
    }



    private void generateBulgingCrown(
            BlockPos origin,
            int treeHeight,
            RandomSource random,
            Set<BlockPos> leaves,
            boolean spherical
    ) {

        int bulgeHeight =
                treeHeight < 5
                        ? 1
                        : 1 + random.nextInt(3);

        int baseY =
                origin.getY() + treeHeight;

        /*
         * 頂部中央三格。
         *
         *       #
         *       #
         *       #
         */

        for (int y = baseY + 2;
             y >= baseY;
             y--) {

            leaves.add(
                    new BlockPos(
                            origin.getX(),
                            y,
                            origin.getZ()
                    )
            );
        }

        /*
         * 中央縱向四方向膨脹。
         *
         *       #
         *      ###
         *      ###
         *      ###
         *
         */

        for (int offsetY = 0;
             offsetY <= 2 + bulgeHeight;
             offsetY++) {

            int y =
                    baseY
                            + 1
                            - offsetY;

            addCardinalLeaves(
                    origin.getX(),
                    y,
                    origin.getZ(),
                    1,
                    leaves
            );
        }

        /*
         * 鼓起區域增加四個對角。
         *
         * 形成：
         *
         *   ###
         *   ###
         *   ###
         */

        for (int bulgeY = 0;
             bulgeY <= bulgeHeight;
             bulgeY++) {

            int y =
                    baseY - bulgeY;

            leaves.add(
                    new BlockPos(
                            origin.getX() - 1,
                            y,
                            origin.getZ() - 1
                    )
            );

            leaves.add(
                    new BlockPos(
                            origin.getX() - 1,
                            y,
                            origin.getZ() + 1
                    )
            );

            leaves.add(
                    new BlockPos(
                            origin.getX() + 1,
                            y,
                            origin.getZ() - 1
                    )
            );

            leaves.add(
                    new BlockPos(
                            origin.getX() + 1,
                            y,
                            origin.getZ() + 1
                    )
            );

            /*
             * SPHERICAL 類型額外向：
             *
             * 北南東西
             *
             * 再延伸兩格。
             *
             * 因此最大直徑會達到 5 格。
             */

            if (spherical) {

                addCardinalLeaves(
                        origin.getX(),
                        y,
                        origin.getZ(),
                        2,
                        leaves
                );
            }
        }
    }

    /*
     * =============================================================
     * 樹冠工具
     * =============================================================
     */

    private void addCardinalLeaves(
            int x,
            int y,
            int z,
            int distance,
            Set<BlockPos> leaves
    ) {

        leaves.add(
                new BlockPos(
                        x + distance,
                        y,
                        z
                )
        );

        leaves.add(
                new BlockPos(
                        x - distance,
                        y,
                        z
                )
        );

        leaves.add(
                new BlockPos(
                        x,
                        y,
                        z + distance
                )
        );

        leaves.add(
                new BlockPos(
                        x,
                        y,
                        z - distance
                )
        );
    }

    private void addRing3x3(
            int x,
            int y,
            int z,
            Set<BlockPos> leaves
    ) {

        for (int dx = -1;
             dx <= 1;
             dx++) {

            for (int dz = -1;
                 dz <= 1;
                 dz++) {

                if (dx == 0
                        && dz == 0) {
                    continue;
                }

                leaves.add(
                        new BlockPos(
                                x + dx,
                                y,
                                z + dz
                        )
                );
            }
        }
    }

    /*
     * =============================================================
     * 生成前判定
     * =============================================================
     */

    private boolean canGenerate(
            WorldGenLevel level,
            Set<BlockPos> logs,
            Set<BlockPos> leaves
    ) {

        for (BlockPos pos : logs) {

            if (!insideBuildHeight(
                    level,
                    pos
            )) {
                return false;
            }

            if (!canReplace(
                    level,
                    pos
            )) {
                return false;
            }
        }

        for (BlockPos pos : leaves) {

            if (!insideBuildHeight(
                    level,
                    pos
            )) {
                return false;
            }

            /*
             * 這一版用嚴格檢查。
             *
             * 不允許樹冠直接吃進：
             *
             * 建築
             * 固體岩石
             * 其他不可替換方塊
             */
            if (!logs.contains(pos)
                    && !canReplace(
                    level,
                    pos
            )) {

                return false;
            }
        }

        return true;
    }

    private boolean canReplace(
            WorldGenLevel level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);

        return state.isAir()
                || state.is(
                BlockTags.REPLACEABLE_BY_TREES
        );
    }

    private boolean insideBuildHeight(
            WorldGenLevel level,
            BlockPos pos
    ) {

        return pos.getY()
                >= level.getMinBuildHeight()

                && pos.getY()
                < level.getMaxBuildHeight();
    }

    /*
     * =============================================================
     * 地面
     * =============================================================
     */

    private boolean isValidGround(
            WorldGenLevel level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);

        return !state.isAir()
                && state.isFaceSturdy(
                level,
                pos,
                Direction.UP
        );
    }

    private boolean hasEnoughGround(
            WorldGenLevel level,
            BlockPos center
    ) {

        int supported = 0;

        /*
         * 小樹只要求 3x3 中至少五格有支撐。
         *
         * 比大型幽靈木寬鬆很多，
         * 但依舊不會很容易掛在島嶼邊緣。
         */

        for (int dx = -1;
             dx <= 1;
             dx++) {

            for (int dz = -1;
                 dz <= 1;
                 dz++) {

                BlockPos pos =
                        center.offset(
                                dx,
                                0,
                                dz
                        );

                BlockState state =
                        level.getBlockState(pos);

                if (!state.isAir()
                        && state.isFaceSturdy(
                        level,
                        pos,
                        Direction.UP
                )) {

                    supported++;
                }
            }
        }

        return supported >= 5;
    }

    /*
     * =============================================================
     * 原木年輪暴露判定
     * =============================================================
     */

    private boolean hasExposedLogEnd(
            WorldGenLevel level,
            BlockPos pos,
            Set<BlockPos> logs,
            Set<BlockPos> leaves
    ) {

        return isLogEndExposed(
                level,
                pos.above(),
                logs,
                leaves
        ) || isLogEndExposed(
                level,
                pos.below(),
                logs,
                leaves
        );
    }

    private boolean isLogEndExposed(
            WorldGenLevel level,
            BlockPos neighbouringPos,
            Set<BlockPos> logs,
            Set<BlockPos> leaves
    ) {

        /*
         * 未來這裡會被：
         *
         * 原木
         * 或樹葉
         *
         * 填滿，就不算暴露。
         */

        if (logs.contains(neighbouringPos)
                || leaves.contains(neighbouringPos)) {

            return false;
        }

        /*
         * 已有固體地形擋住也不算暴露。
         */

        BlockState state =
                level.getBlockState(
                        neighbouringPos
                );

        return state.isAir()
                || state.is(
                BlockTags.REPLACEABLE_BY_TREES
        );
    }

    /*
     * =============================================================
     * 樹葉距離
     * =============================================================
     */

    private Map<BlockPos, Integer> calculateLeafDistances(
            Set<BlockPos> logs,
            Set<BlockPos> leaves
    ) {

        Map<BlockPos, Integer> result =
                new HashMap<>();

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        /*
         * 與原木直接相鄰：
         *
         * distance = 1
         */

        for (BlockPos log : logs) {

            for (Direction direction :
                    Direction.values()) {

                BlockPos neighbour =
                        log.relative(direction);

                if (!leaves.contains(neighbour)) {
                    continue;
                }

                if (result.containsKey(neighbour)) {
                    continue;
                }

                result.put(
                        neighbour,
                        1
                );

                queue.add(
                        neighbour
                );
            }
        }

        /*
         * 向外傳播。
         */

        while (!queue.isEmpty()) {

            BlockPos current =
                    queue.removeFirst();

            int distance =
                    result.get(current);

            if (distance >= 6) {
                continue;
            }

            for (Direction direction :
                    Direction.values()) {

                BlockPos next =
                        current.relative(direction);

                if (!leaves.contains(next)) {
                    continue;
                }

                int nextDistance =
                        distance + 1;

                Integer previous =
                        result.get(next);

                if (previous != null
                        && previous <= nextDistance) {
                    continue;
                }

                result.put(
                        next,
                        nextDistance
                );

                queue.addLast(
                        next
                );
            }
        }

        /*
         * 理論上 HEE 小型樹冠不可能遠到 distance > 7。
         *
         * 但保留保險值。
         */

        for (BlockPos leaf : leaves) {

            result.putIfAbsent(
                    leaf,
                    7
            );
        }

        return result;
    }
}
