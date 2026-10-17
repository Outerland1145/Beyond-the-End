package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.*;

public class SpookyTreeFeature extends Feature<SpookyTreeConfiguration> {

    /*
     * Wyrmwood 類型：
     *
     *             █████████████████
     *        █████████████████████████
     *          █████████████████████
     *          ││││       ││││
     *           \          /
     *            \        /
     *             \██████/
     *               ███
     *              ███
     *             ███
     *             ███
     *            ████
     *            ████
     *
     * 特徵：
     *
     * 1. 高大主幹
     * 2. 主幹不是完全垂直
     * 3. 下粗上細
     * 4. 高處生成大型放射狀枝條
     * 5. 樹冠非常寬、但相對扁平
     * 6. 樹冠不是球型
     * 7. 樹冠下有大量垂落植物
     */

    public SpookyTreeFeature(Codec<SpookyTreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpookyTreeConfiguration> context) {

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        SpookyTreeConfiguration config = context.config();

        /*
         * Heightmap placement 後，origin 理論上應該是地面上方第一格。
         */
        BlockPos groundPos = origin.below();

        if (!isValidGround(level, groundPos)) {
            return false;
        }

        /*
         * 防止在島嶼懸崖最邊緣生成整棵大樹。
         *
         * 這裡不是要求完全平地，
         * 只是確保樹根附近確實存在足夠地面。
         */
        if (!hasEnoughGround(level, groundPos)) {
            return false;
        }

        /*
         * Stygian Wyrmwood 是相當高的樹。
         *
         * 16 ~ 23 格主幹高度。
         */
        int height = 16 + random.nextInt(8);

        /*
         * 所有東西先存在記憶體，
         * 最後才真正寫進世界。
         *
         * 可以避免：
         *
         * 樹生成到一半撞牆
         * ↓
         * 留下一棵半截樹。
         */
        Map<BlockPos, Direction.Axis> logs = new LinkedHashMap<>();
        Set<BlockPos> leaves = new HashSet<>();
        Set<BlockPos> hanging = new HashSet<>();

        /*
         * =========================================================
         * 1. 主幹
         * =========================================================
         */

        List<BlockPos> trunkCenters =
                buildTwistedTrunk(origin, height, random, logs);

        BlockPos crownCenter =
                trunkCenters.get(trunkCenters.size() - 1);

        /*
         * =========================================================
         * 2. 高位大型枝條
         * =========================================================
         */

        int branchCount = 5 + random.nextInt(3);

        List<BlockPos> branchTips = new ArrayList<>();

        double angleOffset =
                random.nextDouble() * Math.PI * 2.0D;

        for (int i = 0; i < branchCount; i++) {

            /*
             * 大致平均向四周展開，
             * 但加入隨機偏移。
             *
             * 避免所有樹看起來像完美星形。
             */
            double angle =
                    angleOffset
                            + ((Math.PI * 2.0D) / branchCount) * i
                            + (random.nextDouble() - 0.5D) * 0.55D;

            /*
             * Wyrmwood 最重要的特徵之一就是
             * 樹冠橫向尺寸非常大。
             */
            int branchLength =
                    6 + random.nextInt(5);

            /*
             * 枝條通常從主幹上方約 65% ~ 90% 開始。
             */
            int startIndex =
                    Mth.clamp(
                            (int) (height * (0.64D + random.nextDouble() * 0.20D)),
                            0,
                            trunkCenters.size() - 1
                    );

            BlockPos start =
                    trunkCenters.get(startIndex);

            int dx =
                    Mth.floor(Math.cos(angle) * branchLength);

            int dz =
                    Mth.floor(Math.sin(angle) * branchLength);

            /*
             * 枝條不是完全水平。
             *
             * 通常略微向上。
             */
            int rise =
                    2 + random.nextInt(4);

            BlockPos tip =
                    start.offset(
                            dx,
                            rise,
                            dz
                    );

            /*
             * 中間再加入一個控制點，
             * 讓枝條不是單純一條直線。
             */
            int midX =
                    Mth.floor(dx * 0.50D);

            int midZ =
                    Mth.floor(dz * 0.50D);

            BlockPos middle =
                    start.offset(
                            midX,
                            1 + random.nextInt(3),
                            midZ
                    );

            createBranch(
                    start,
                    middle,
                    logs
            );

            createBranch(
                    middle,
                    tip,
                    logs
            );

            /*
             * 枝條根部加粗。
             */
            thickenBranchBase(
                    start,
                    middle,
                    logs
            );

            branchTips.add(tip);
        }

        /*
         * =========================================================
         * 3. 次級枝條
         * =========================================================
         *
         * 每根大枝偶爾再次分叉。
         */

        List<BlockPos> secondaryTips = new ArrayList<>();

        for (BlockPos tip : new ArrayList<>(branchTips)) {

            if (random.nextFloat() > 0.72F) {
                continue;
            }

            double baseAngle =
                    Math.atan2(
                            tip.getZ() - crownCenter.getZ(),
                            tip.getX() - crownCenter.getX()
                    );

            double angle =
                    baseAngle
                            + (random.nextBoolean() ? 1 : -1)
                            * (0.45D + random.nextDouble() * 0.55D);

            int length =
                    3 + random.nextInt(4);

            BlockPos secondaryTip =
                    tip.offset(
                            Mth.floor(Math.cos(angle) * length),
                            random.nextInt(3),
                            Mth.floor(Math.sin(angle) * length)
                    );

            createBranch(
                    tip,
                    secondaryTip,
                    logs
            );

            secondaryTips.add(secondaryTip);
        }

        branchTips.addAll(secondaryTips);

        /*
         * =========================================================
         * 4. 中央樹冠
         * =========================================================
         */

        createFlatCanopy(
                crownCenter.above(),
                4 + random.nextInt(2),
                random,
                leaves
        );

        /*
         * =========================================================
         * 5. 各大型枝條上的扁平樹冠
         * =========================================================
         *
         * 不製作一顆巨大球體。
         *
         * 而是讓每根大枝各自形成一片 canopy，
         * 最終彼此重疊。
         *
         * 這才會得到 Stygian 類型的巨大平坦冠層。
         */

        for (BlockPos tip : branchTips) {

            int radius =
                    3 + random.nextInt(2);

            createFlatCanopy(
                    tip.above(),
                    radius,
                    random,
                    leaves
            );
        }

        /*
         * =========================================================
         * 6. 填補部分樹冠空隙
         * =========================================================
         *
         * 注意不是完全填滿。
         *
         * Wyrmwood 的輪廓應該依然帶有很多裂口與不規則邊緣。
         */

        connectNearbyCanopies(
                crownCenter,
                branchTips,
                random,
                leaves
        );

        /*
         * =========================================================
         * 7. 垂落枝葉
         * =========================================================
         */

        buildHangingFoliage(
                crownCenter,
                leaves,
                hanging,
                random
        );

        /*
         * =========================================================
         * 8. 空間檢查
         * =========================================================
         */

        if (!canGenerate(
                level,
                logs.keySet(),
                leaves,
                hanging
        )) {
            return false;
        }

        /*
         * =========================================================
         * 9. 放置原木
         * =========================================================
         */

        for (Map.Entry<BlockPos, Direction.Axis> entry
                : logs.entrySet()) {

            BlockPos pos = entry.getKey();

            BlockState state =
                    config.trunkProvider()
                            .getState(random, pos);

            state =
                    setLogAxis(
                            state,
                            entry.getValue()
                    );

            level.setBlock(
                    pos,
                    state,
                    Block.UPDATE_CLIENTS
            );
        }

        /*
         * =========================================================
         * 10. 計算樹葉 distance
         * =========================================================
         *
         * 這點非常重要。
         *
         * 不然如果你的樹葉繼承 LeavesBlock，
         * 巨大樹冠可能會在生成後開始自然消失。
         */

        Map<BlockPos, Integer> leafDistances =
                calculateLeafDistances(
                        logs.keySet(),
                        leaves
                );

        /*
         * =========================================================
         * 11. 放置樹葉
         * =========================================================
         */

        for (BlockPos pos : leaves) {

            if (!canReplace(level, pos)) {
                continue;
            }

            BlockState state =
                    config.leavesProvider()
                            .getState(random, pos);

            Integer distance =
                    leafDistances.get(pos);

            if (distance != null
                    && state.hasProperty(LeavesBlock.DISTANCE)) {

                state =
                        state.setValue(
                                LeavesBlock.DISTANCE,
                                Mth.clamp(distance, 1, 7)
                        );
            }

            if (state.hasProperty(LeavesBlock.PERSISTENT)) {

                state =
                        state.setValue(
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

        /*
         * =========================================================
         * 12. 放置垂落植物
         * =========================================================
         */

        for (BlockPos pos : hanging) {

            if (!level.getBlockState(pos).isAir()) {
                continue;
            }

            BlockState state =
                    config.hangingProvider()
                            .getState(random, pos);

            /*
             * 如果你暫時直接拿 LeavesBlock 當 hanging，
             * 就將它設成 persistent，
             * 避免最下面那些距離木頭太遠而自行腐爛。
             *
             * 之後最好換成真正的垂葉/藤蔓方塊。
             */
            if (state.hasProperty(LeavesBlock.PERSISTENT)) {

                state =
                        state.setValue(
                                LeavesBlock.PERSISTENT,
                                true
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

    /*
     * =============================================================
     * 主幹
     * =============================================================
     */

    private List<BlockPos> buildTwistedTrunk(
            BlockPos origin,
            int height,
            RandomSource random,
            Map<BlockPos, Direction.Axis> logs
    ) {

        List<BlockPos> centers =
                new ArrayList<>();

        int offsetX = 0;
        int offsetZ = 0;

        int nextTurn =
                3 + random.nextInt(3);

        Direction horizontalDirection =
                Direction.Plane.HORIZONTAL
                        .getRandomDirection(random);

        for (int y = 0; y < height; y++) {

            /*
             * 每隔數格讓樹幹偏移一格。
             *
             * 因此不是：
             *
             * │
             * │
             * │
             *
             * 而比較像：
             *
             *   │
             *  │
             *  │
             * │
             */
            if (y == nextTurn
                    && y < height - 4) {

                if (random.nextFloat() < 0.75F) {

                    offsetX += horizontalDirection.getStepX();
                    offsetZ += horizontalDirection.getStepZ();

                    /*
                     * 不允許樹幹一路飄走。
                     */
                    offsetX =
                            Mth.clamp(offsetX, -3, 3);

                    offsetZ =
                            Mth.clamp(offsetZ, -3, 3);
                }

                if (random.nextFloat() < 0.65F) {

                    horizontalDirection =
                            Direction.Plane.HORIZONTAL
                                    .getRandomDirection(random);
                }

                nextTurn +=
                        2 + random.nextInt(4);
            }

            BlockPos center =
                    origin.offset(
                            offsetX,
                            y,
                            offsetZ
                    );

            centers.add(center);

            float progress =
                    y / (float) height;

            /*
             * 下方非常粗，
             * 中間逐漸變細，
             * 頂部變成單格主幹。
             */

            if (progress < 0.48F) {

                addThickTrunk(
                        center,
                        logs
                );

            } else if (progress < 0.76F) {

                addMediumTrunk(
                        center,
                        logs,
                        y
                );

            } else {

                addLog(
                        logs,
                        center,
                        Direction.Axis.Y
                );
            }
        }

        return centers;
    }

    private void addThickTrunk(
            BlockPos center,
            Map<BlockPos, Direction.Axis> logs
    ) {

        addLog(logs, center, Direction.Axis.Y);
        addLog(logs, center.east(), Direction.Axis.Y);
        addLog(logs, center.south(), Direction.Axis.Y);
        addLog(logs, center.east().south(), Direction.Axis.Y);
    }

    private void addMediumTrunk(
            BlockPos center,
            Map<BlockPos, Direction.Axis> logs,
            int y
    ) {

        addLog(
                logs,
                center,
                Direction.Axis.Y
        );

        /*
         * 每隔一層在不同方向留一塊，
         * 讓 2x2 → 1x1 的過渡不會突然縮掉。
         */
        switch (y & 3) {

            case 0 ->
                    addLog(
                            logs,
                            center.east(),
                            Direction.Axis.Y
                    );

            case 1 ->
                    addLog(
                            logs,
                            center.south(),
                            Direction.Axis.Y
                    );

            case 2 ->
                    addLog(
                            logs,
                            center.west(),
                            Direction.Axis.Y
                    );

            default ->
                    addLog(
                            logs,
                            center.north(),
                            Direction.Axis.Y
                    );
        }
    }

    /*
     * =============================================================
     * 枝條
     * =============================================================
     */

    private void createBranch(
            BlockPos start,
            BlockPos end,
            Map<BlockPos, Direction.Axis> logs
    ) {

        int dx =
                end.getX() - start.getX();

        int dy =
                end.getY() - start.getY();

        int dz =
                end.getZ() - start.getZ();

        int steps =
                Math.max(
                        Math.abs(dx),
                        Math.max(
                                Math.abs(dy),
                                Math.abs(dz)
                        )
                );

        if (steps == 0) {
            return;
        }

        BlockPos previous =
                start;

        for (int i = 0; i <= steps; i++) {

            double t =
                    i / (double) steps;

            int x =
                    Mth.floor(
                            Mth.lerp(
                                    t,
                                    start.getX(),
                                    end.getX()
                            ) + 0.5D
                    );

            int y =
                    Mth.floor(
                            Mth.lerp(
                                    t,
                                    start.getY(),
                                    end.getY()
                            ) + 0.5D
                    );

            int z =
                    Mth.floor(
                            Mth.lerp(
                                    t,
                                    start.getZ(),
                                    end.getZ()
                            ) + 0.5D
                    );

            BlockPos current =
                    new BlockPos(x, y, z);

            Direction.Axis axis =
                    getAxis(
                            previous,
                            current
                    );

            addLog(
                    logs,
                    current,
                    axis
            );

            previous =
                    current;
        }
    }

    private void thickenBranchBase(
            BlockPos start,
            BlockPos next,
            Map<BlockPos, Direction.Axis> logs
    ) {

        Direction.Axis axis =
                getAxis(start, next);

        /*
         * 大枝連接主幹的位置略粗。
         */
        addLog(
                logs,
                start.above(),
                axis
        );

        addLog(
                logs,
                start.below(),
                axis
        );
    }

    /*
     * =============================================================
     * 樹冠
     * =============================================================
     */

    private void createFlatCanopy(
            BlockPos center,
            int radius,
            RandomSource random,
            Set<BlockPos> leaves
    ) {

        /*
         * 樹冠只有約 3 格厚，
         * 但橫向可以 7~9 格。
         *
         * 這就是避免 Minecraft 常見「球型樹冠」的關鍵。
         */

        for (int dy = -1; dy <= 1; dy++) {

            int layerRadius =
                    switch (dy) {
                        case 0 -> radius;
                        default -> radius - 1;
                    };

            for (int x = -layerRadius;
                 x <= layerRadius;
                 x++) {

                for (int z = -layerRadius;
                     z <= layerRadius;
                     z++) {

                    double distance =
                            (x * x)
                                    / (double) (layerRadius * layerRadius)
                                    +
                                    (z * z)
                                            / (double) (layerRadius * layerRadius);

                    /*
                     * 圓形大致輪廓。
                     */
                    if (distance > 1.12D) {
                        continue;
                    }

                    /*
                     * 外圍隨機挖掉部分樹葉，
                     * 產生破碎、不規則的 canopy。
                     */
                    if (distance > 0.70D
                            && random.nextFloat() < 0.26F) {
                        continue;
                    }

                    /*
                     * 中央也留非常少量孔洞。
                     */
                    if (distance < 0.70D
                            && random.nextFloat() < 0.035F) {
                        continue;
                    }

                    leaves.add(
                            center.offset(
                                    x,
                                    dy,
                                    z
                            )
                    );
                }
            }
        }

        /*
         * 上方少數突起。
         */
        int upperRadius =
                Math.max(1, radius - 2);

        for (int x = -upperRadius;
             x <= upperRadius;
             x++) {

            for (int z = -upperRadius;
                 z <= upperRadius;
                 z++) {

                if (x * x + z * z
                        > upperRadius * upperRadius) {
                    continue;
                }

                if (random.nextFloat() < 0.30F) {
                    continue;
                }

                leaves.add(
                        center.offset(
                                x,
                                2,
                                z
                        )
                );
            }
        }
    }

    private void connectNearbyCanopies(
            BlockPos center,
            List<BlockPos> tips,
            RandomSource random,
            Set<BlockPos> leaves
    ) {

        for (BlockPos tip : tips) {

            int dx =
                    tip.getX() - center.getX();

            int dz =
                    tip.getZ() - center.getZ();

            int steps =
                    Math.max(
                            Math.abs(dx),
                            Math.abs(dz)
                    );

            if (steps == 0) {
                continue;
            }

            for (int i = 2;
                 i < steps;
                 i += 2) {

                double t =
                        i / (double) steps;

                BlockPos point =
                        new BlockPos(
                                Mth.floor(
                                        Mth.lerp(
                                                t,
                                                center.getX(),
                                                tip.getX()
                                        ) + 0.5D
                                ),

                                Mth.floor(
                                        Mth.lerp(
                                                t,
                                                center.getY(),
                                                tip.getY()
                                        ) + 0.5D
                                ) + 1,

                                Mth.floor(
                                        Mth.lerp(
                                                t,
                                                center.getZ(),
                                                tip.getZ()
                                        ) + 0.5D
                                )
                        );

                /*
                 * 只做很小的橋接。
                 *
                 * 不把整個 canopy 填成大圓盤。
                 */
                if (random.nextFloat() < 0.70F) {

                    createSmallLeafPatch(
                            point,
                            random,
                            leaves
                    );
                }
            }
        }
    }

    private void createSmallLeafPatch(
            BlockPos center,
            RandomSource random,
            Set<BlockPos> leaves
    ) {

        for (int x = -1; x <= 1; x++) {

            for (int z = -1; z <= 1; z++) {

                if (Math.abs(x)
                        + Math.abs(z) > 1
                        && random.nextBoolean()) {
                    continue;
                }

                leaves.add(
                        center.offset(
                                x,
                                0,
                                z
                        )
                );
            }
        }
    }

    /*
     * =============================================================
     * 垂葉 / 垂枝
     * =============================================================
     */

    private void buildHangingFoliage(
            BlockPos crownCenter,
            Set<BlockPos> leaves,
            Set<BlockPos> hanging,
            RandomSource random
    ) {

        List<BlockPos> underside =
                new ArrayList<>();

        for (BlockPos leaf : leaves) {

            /*
             * 只從樹冠底面往下長。
             */
            if (leaves.contains(leaf.below())) {
                continue;
            }

            double dx =
                    leaf.getX()
                            - crownCenter.getX();

            double dz =
                    leaf.getZ()
                            - crownCenter.getZ();

            double horizontalDistance =
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    );

            /*
             * 主要讓 canopy 外圍垂下。
             * 中央附近比較少。
             */
            if (horizontalDistance < 3.5D) {
                continue;
            }

            underside.add(leaf);
        }

        Collections.shuffle(
                underside,
                new Random(random.nextLong())
        );

        int desired =
                Math.min(
                        underside.size(),
                        14 + random.nextInt(13)
                );

        for (int i = 0;
             i < desired;
             i++) {

            BlockPos start =
                    underside.get(i);

            /*
             * 2 ~ 8 格長。
             */
            int length =
                    2 + random.nextInt(7);

            /*
             * 極少數做成非常長的垂枝。
             */
            if (random.nextFloat() < 0.20F) {
                length +=
                        2 + random.nextInt(4);
            }

            for (int y = 1;
                 y <= length;
                 y++) {

                BlockPos pos =
                        start.below(y);

                if (leaves.contains(pos)) {
                    break;
                }

                hanging.add(pos);
            }
        }
    }

    /*
     * =============================================================
     * 葉片 distance
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
         * 與木頭直接相鄰的葉子 = distance 1
         */
        for (BlockPos log : logs) {

            for (Direction direction
                    : Direction.values()) {

                BlockPos neighbour =
                        log.relative(direction);

                if (!leaves.contains(neighbour)) {
                    continue;
                }

                if (result.containsKey(neighbour)) {
                    continue;
                }

                result.put(neighbour, 1);
                queue.add(neighbour);
            }
        }

        /*
         * 逐層向外傳播到 6。
         */
        while (!queue.isEmpty()) {

            BlockPos current =
                    queue.removeFirst();

            int distance =
                    result.get(current);

            if (distance >= 6) {
                continue;
            }

            for (Direction direction
                    : Direction.values()) {

                BlockPos next =
                        current.relative(direction);

                if (!leaves.contains(next)) {
                    continue;
                }

                int nextDistance =
                        distance + 1;

                Integer existing =
                        result.get(next);

                if (existing != null
                        && existing <= nextDistance) {
                    continue;
                }

                result.put(
                        next,
                        nextDistance
                );

                queue.add(next);
            }
        }

        /*
         * 理論上 canopy 每個部分都有枝條支撐。
         *
         * 如果真的有離木頭太遠的，
         * 就讓它 distance = 7。
         */
        for (BlockPos leaf : leaves) {

            result.putIfAbsent(
                    leaf,
                    7
            );
        }

        return result;
    }

    /*
     * =============================================================
     * 生成判定
     * =============================================================
     */

    private boolean canGenerate(
            WorldGenLevel level,
            Set<BlockPos> logs,
            Set<BlockPos> leaves,
            Set<BlockPos> hanging
    ) {

        /*
         * 木頭是核心。
         *
         * 任何主要枝幹撞到不可取代方塊，
         * 整棵樹直接取消。
         */
        for (BlockPos pos : logs) {

            if (!isInsideBuildHeight(level, pos)) {
                return false;
            }

            if (!canReplace(level, pos)) {
                return false;
            }
        }

        /*
         * 葉子超出世界高度也不允許。
         */
        for (BlockPos pos : leaves) {

            if (!isInsideBuildHeight(level, pos)) {
                return false;
            }
        }

        for (BlockPos pos : hanging) {

            if (!isInsideBuildHeight(level, pos)) {
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

    private boolean isInsideBuildHeight(
            WorldGenLevel level,
            BlockPos pos
    ) {

        return pos.getY()
                > level.getMinBuildHeight()
                && pos.getY()
                < level.getMaxBuildHeight();
    }

    /*
     * =============================================================
     * 地面判斷
     * =============================================================
     */

    private boolean isValidGround(
            WorldGenLevel level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);

        /*
         * 目前先採用：
         *
         * 只要上表面可以支撐方塊即可。
         *
         * 因為這個 PlacedFeature 本身只會被加入侵蝕之地。
         *
         * 如果你要絕對限制到：
         *
         * 侵蝕終界石
         * 附草侵蝕終界石
         *
         * 我建議你在這裡直接使用你自己的 BlockRegister。
         */
        return state.isFaceSturdy(
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
         * 3x3 區域至少七格有支撐。
         *
         * 避免：
         *
         *       TREE
         *        ██
         * --------█
         *
         * 這種貼著島邊生成。
         */
        for (int x = -1; x <= 1; x++) {

            for (int z = -1; z <= 1; z++) {

                BlockPos check =
                        center.offset(
                                x,
                                0,
                                z
                        );

                BlockState state =
                        level.getBlockState(check);

                if (!state.isAir()
                        && state.isFaceSturdy(
                        level,
                        check,
                        Direction.UP
                )) {

                    supported++;
                }
            }
        }

        return supported >= 7;
    }

    /*
     * =============================================================
     * 工具
     * =============================================================
     */

    private void addLog(
            Map<BlockPos, Direction.Axis> logs,
            BlockPos pos,
            Direction.Axis axis
    ) {

        /*
         * 主幹優先。
         *
         * 避免橫向枝條穿過主幹時
         * 把主幹原木 axis 改掉。
         */
        logs.putIfAbsent(
                pos.immutable(),
                axis
        );
    }

    private BlockState setLogAxis(
            BlockState state,
            Direction.Axis axis
    ) {

        if (state.hasProperty(
                RotatedPillarBlock.AXIS
        )) {

            return state.setValue(
                    RotatedPillarBlock.AXIS,
                    axis
            );
        }

        return state;
    }

    private Direction.Axis getAxis(
            BlockPos from,
            BlockPos to
    ) {

        int dx =
                Math.abs(
                        to.getX() - from.getX()
                );

        int dy =
                Math.abs(
                        to.getY() - from.getY()
                );

        int dz =
                Math.abs(
                        to.getZ() - from.getZ()
                );

        if (dy >= dx
                && dy >= dz) {

            return Direction.Axis.Y;
        }

        if (dx >= dz) {

            return Direction.Axis.X;
        }

        return Direction.Axis.Z;
    }
}
