package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VoidCrystalSpikeFeature
        extends Feature<NoneFeatureConfiguration> {

    /*
     * 依照你目前上傳檔案中的實際設定保留 Y < 40。
     * 如果你要恢復原本規劃的 Y < 30，只改這一個值即可。
     */
    private static final int MAX_ANCHOR_Y = 40;

    /*
     * =========================================================
     * 黑曜石底盤
     * =========================================================
     */

    private static final int MIN_BASE_RADIUS = 4;
    private static final int MAX_BASE_RADIUS = 7;

    /*
     * 底盤向島內嵌入的深度。
     *
     * 注意：這只控制造型，不再拿 MAX_EMBED_DEPTH 去做整片嚴格判定。
     */
    private static final int MIN_EMBED_DEPTH = 4;
    private static final int MAX_EMBED_DEPTH = 8;

    /* 從島底向下突出多少。 */
    private static final int MIN_BASE_DEPTH = 4;
    private static final int MAX_BASE_DEPTH = 7;

    /* 黑曜石 -> 混合 -> 水晶的過渡層數。 */
    private static final int TRANSITION_LAYERS = 4;

    /*
     * 底盤實際邊緣有 noise，因此 footprint 多檢查 1 格。
     */
    private static final int BASE_CHECK_MARGIN = 1;

    /*
     * =========================================================
     * 水晶尖刺
     * =========================================================
     */

    private static final int MIN_SPIKE_LENGTH = 15;
    private static final int MAX_SPIKE_LENGTH = 30;

    private static final int MIN_CRYSTAL_RADIUS = 3;
    private static final int MAX_CRYSTAL_RADIUS = 5;

    private static final int MIN_BRANCHES = 0;
    private static final int MAX_BRANCHES = 3;

    private static final int MIN_BUDDING_BLOCKS = 5;
    private static final int MAX_BUDDING_BLOCKS = 10;

    public VoidCrystalSpikeFeature(
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

        /*
         * 1. 找中心附著點。
         */
        BlockPos anchor = findAnchor(
                level,
                origin.getX(),
                origin.getZ()
        );

        if (anchor == null) {
            return false;
        }

        /*
         * 2. 先決定「這一次」真正的底盤半徑。
         *
         * 原本版本在知道實際半徑之前，直接用全域 MAX_BASE_RADIUS=7
         * 掃整圈，會讓 radius=4/5 的地物也被 radius=7 的地形缺口淘汰，
         * 這是很容易造成「幾乎完全不生成」的主因之一。
         */
        int baseRadius =
                MIN_BASE_RADIUS
                        + random.nextInt(
                        MAX_BASE_RADIUS
                                - MIN_BASE_RADIUS
                                + 1
                );

        /*
         * 3. 依照「這一次實際底盤 footprint」做完整判定。
         *
         * anchor Y 這個平面不能凹進島內；
         * 但向下凸出的島體完全允許。
         */
        if (!isValidBaseArea(
                level,
                anchor,
                baseRadius
        )) {
            return false;
        }

        int embedDepth =
                MIN_EMBED_DEPTH
                        + random.nextInt(
                        MAX_EMBED_DEPTH
                                - MIN_EMBED_DEPTH
                                + 1
                );

        int baseDepth =
                MIN_BASE_DEPTH
                        + random.nextInt(
                        MAX_BASE_DEPTH
                                - MIN_BASE_DEPTH
                                + 1
                );

        int crystalRadius =
                MIN_CRYSTAL_RADIUS
                        + random.nextInt(
                        MAX_CRYSTAL_RADIUS
                                - MIN_CRYSTAL_RADIUS
                                + 1
                );

        int availableDepth =
                anchor.getY()
                        - baseDepth
                        - level.getMinBuildHeight()
                        - 2;

        if (availableDepth < MIN_SPIKE_LENGTH) {
            return false;
        }

        int maximumLength =
                Math.min(
                        MAX_SPIKE_LENGTH,
                        availableDepth
                );

        int spikeLength =
                MIN_SPIKE_LENGTH
                        + random.nextInt(
                        maximumLength
                                - MIN_SPIKE_LENGTH
                                + 1
                );

        Set<BlockPos> crystals = new HashSet<>();

        /*
         * 4. 生成嵌入式黑曜石底盤與黑曜石 -> 水晶混合過渡區。
         */
        generateEmbeddedBase(
                level,
                random,
                anchor,
                baseRadius,
                embedDepth,
                baseDepth,
                crystalRadius,
                crystals
        );

        /*
         * 5. 主水晶尖刺和底盤最後幾層重疊，
         *    避免出現一條整齊的水平分界線。
         */
        BlockPos crystalStart =
                anchor.below(
                        Math.max(
                                1,
                                baseDepth - 2
                        )
                );

        generateCrystalSpike(
                level,
                random,
                crystalStart,
                spikeLength,
                crystalRadius,
                0,
                0,
                crystals
        );

        /*
         * 6. 小型分叉。
         */
        int branches =
                MIN_BRANCHES
                        + random.nextInt(
                        MAX_BRANCHES
                                - MIN_BRANCHES
                                + 1
                );

        for (int i = 0; i < branches; i++) {
            generateBranch(
                    level,
                    random,
                    crystalStart,
                    spikeLength,
                    crystalRadius,
                    crystals
            );
        }

        /*
         * 7. 把部分表面水晶改成母岩。
         */
        placeBuddingBlocks(
                level,
                random,
                crystals
        );

        return !crystals.isEmpty();
    }

    /*
     * =========================================================
     * 中心附著點判定
     * =========================================================
     *
     * 維持你要求的簡單規則：
     * - Y < MAX_ANCHOR_Y
     * - 本身不是 Air
     * - 下方是 Air
     * - 上、東、西、南、北都不是 Air
     */
    private BlockPos findAnchor(
            WorldGenLevel level,
            int x,
            int z
    ) {

        int startY =
                Math.min(
                        MAX_ANCHOR_Y - 1,
                        level.getMaxBuildHeight() - 1
                );

        for (
                int y = startY;
                y > level.getMinBuildHeight();
                y--
        ) {

            BlockPos pos = new BlockPos(x, y, z);

            if (isValidAnchor(level, pos)) {
                return pos;
            }
        }

        return null;
    }

    private boolean isValidAnchor(
            WorldGenLevel level,
            BlockPos pos
    ) {

        if (pos.getY() >= MAX_ANCHOR_Y) {
            return false;
        }

        if (level.getBlockState(pos).isAir()) {
            return false;
        }

        if (!level.getBlockState(pos.below()).isAir()) {
            return false;
        }

        if (level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return false;
            }
        }

        return true;
    }

    /*
     * =========================================================
     * 底盤 footprint 判定
     * =========================================================
     *
     * 關鍵修正：
     * 不再每次都用全域 MAX_BASE_RADIUS 檢查。
     * 只檢查「這一次實際生成底盤的最大範圍」。
     *
     * anchor Y 平面：
     * - Air -> 島底向內凹，拒絕。
     * - 非 Air -> 可以。
     *
     * anchor Y 以下如果還有更多方塊，代表島底向下凸出，允許。
     */
    private boolean isValidBaseArea(
            WorldGenLevel level,
            BlockPos anchor,
            int actualBaseRadius
    ) {

        int checkRadius = actualBaseRadius + BASE_CHECK_MARGIN;

        for (int dx = -checkRadius; dx <= checkRadius; dx++) {
            for (int dz = -checkRadius; dz <= checkRadius; dz++) {

                if (dx * dx + dz * dz > checkRadius * checkRadius) {
                    continue;
                }

                BlockPos checkPos = anchor.offset(dx, 0, dz);

                if (level.getBlockState(checkPos).isAir()) {
                    return false;
                }
            }
        }

        return true;
    }

    /*
     * =========================================================
     * 黑曜石 / 哭泣黑曜石底盤 + 水晶過渡區
     * =========================================================
     */
    private void generateEmbeddedBase(
            WorldGenLevel level,
            RandomSource random,
            BlockPos anchor,
            int baseRadius,
            int embedDepth,
            int baseDepth,
            int crystalRadius,
            Set<BlockPos> crystals
    ) {

        /*
         * yOffset > 0：嵌進島內
         * yOffset = 0：原始島底平面
         * yOffset < 0：向虛空突出
         */
        for (
                int yOffset = embedDepth - 1;
                yOffset >= -baseDepth;
                yOffset--
        ) {

            double radius;

            if (yOffset >= 0) {

                double embedProgress =
                        yOffset
                                / (double) Math.max(
                                1,
                                embedDepth
                        );

                /*
                 * 在島體內保持寬厚，讓底盤真正嵌進島裡。
                 */
                radius =
                        baseRadius
                                - embedProgress
                                * 0.45D;
            }
            else {

                int depth = -yOffset;

                double progress =
                        depth
                                / (double) Math.max(
                                1,
                                baseDepth
                        );

                /*
                 * 向下逐漸從底盤半徑收束到水晶主幹半徑。
                 */
                radius =
                        baseRadius
                                + (
                                crystalRadius + 0.75D
                                        - baseRadius
                        ) * progress;
            }

            int integerRadius = (int) Math.ceil(radius);

            for (int dx = -integerRadius; dx <= integerRadius; dx++) {
                for (int dz = -integerRadius; dz <= integerRadius; dz++) {

                    double distance =
                            Math.sqrt(
                                    dx * dx
                                            + dz * dz
                            );

                    double edgeNoise = random.nextDouble() * 0.75D;

                    if (distance > radius + edgeNoise) {
                        continue;
                    }

                    BlockPos placePos = anchor.offset(dx, yOffset, dz);
                    BlockState old = level.getBlockState(placePos);

                    if (old.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    if (yOffset >= 0) {
                        /*
                         * 嵌入島體時只替換原本就存在的方塊。
                         * 如果島體更上方剛好有洞穴，不主動用底盤把洞補滿。
                         */
                        if (old.isAir()) {
                            continue;
                        }
                    }

                    /*
                     * ------------------------------
                     * 過渡區
                     * ------------------------------
                     */
                    int downwardDepth = Math.max(0, -yOffset);

                    int transitionStart =
                            Math.max(
                                    1,
                                    baseDepth
                                            - TRANSITION_LAYERS
                                            + 1
                            );

                    double transitionProgress = 0.0D;

                    if (downwardDepth >= transitionStart) {

                        transitionProgress =
                                (
                                        downwardDepth
                                                - transitionStart
                                                + 1
                                )
                                        / (double) TRANSITION_LAYERS;

                        transitionProgress =
                                Math.min(
                                        1.0D,
                                        transitionProgress
                                );
                    }

                    /*
                     * 越靠中心、越往下，越容易變成水晶。
                     * 再加少量 jitter，避免形成一條規則水平分界。
                     */
                    double radial =
                            radius <= 0.0D
                                    ? 0.0D
                                    : distance / radius;

                    double centerBias =
                            1.0D
                                    - Math.min(
                                    1.0D,
                                    radial
                            );

                    double jitter =
                            (random.nextDouble() - 0.5D)
                                    * 0.12D;

                    float crystalChance =
                            (float) Math.max(
                                    0.0D,
                                    Math.min(
                                            1.0D,
                                            transitionProgress
                                                    * (
                                                    0.52D
                                                            + centerBias
                                                            * 0.38D
                                            )
                                                    + jitter
                                    )
                            );

                    if (
                            transitionProgress > 0.0D
                                    && random.nextFloat() < crystalChance
                    ) {

                        level.setBlock(
                                placePos,
                                BlockRegister
                                        .VOID_CRYSTAL.crystalblock
                                        .get()
                                        .defaultBlockState(),
                                Block.UPDATE_CLIENTS
                        );

                        crystals.add(placePos.immutable());
                        continue;
                    }

                    /*
                     * ------------------------------
                     * 黑曜石 / 哭泣黑曜石
                     * ------------------------------
                     */
                    float cryingChance =
                            0.20F
                                    + (float) transitionProgress
                                    * 0.25F;

                    if (downwardDepth % 2 == 1) {
                        cryingChance += 0.08F;
                    }

                    BlockState baseState =
                            random.nextFloat() < cryingChance
                                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                                    : Blocks.OBSIDIAN.defaultBlockState();

                    level.setBlock(
                            placePos,
                            baseState,
                            Block.UPDATE_CLIENTS
                    );
                }
            }
        }
    }

    /*
     * =========================================================
     * 虛空水晶尖刺
     * =========================================================
     */
    private void generateCrystalSpike(
            WorldGenLevel level,
            RandomSource random,
            BlockPos start,
            int length,
            int startRadius,
            int endOffsetX,
            int endOffsetZ,
            Set<BlockPos> crystals
    ) {

        for (int depth = 0; depth < length; depth++) {

            double progress =
                    depth
                            / (double) Math.max(
                            1,
                            length - 1
                    );

            double radius =
                    Math.max(
                            0.35D,
                            startRadius
                                    * Math.pow(
                                    1.0D - progress,
                                    0.62D
                            )
                    );

            int offsetX =
                    (int) Math.round(
                            endOffsetX
                                    * progress
                    );

            int offsetZ =
                    (int) Math.round(
                            endOffsetZ
                                    * progress
                    );

            BlockPos center =
                    start.offset(
                            offsetX,
                            -depth,
                            offsetZ
                    );

            generateCrystalLayer(
                    level,
                    random,
                    center,
                    radius,
                    crystals
            );
        }
    }

    private void generateCrystalLayer(
            WorldGenLevel level,
            RandomSource random,
            BlockPos center,
            double radius,
            Set<BlockPos> crystals
    ) {

        int ir =
                Math.max(
                        1,
                        (int) Math.ceil(radius)
                );

        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {

                double distanceSquared =
                        dx * dx
                                + dz * dz;

                double maxDistance =
                        radius * radius
                                + random.nextDouble()
                                * 0.6D;

                if (distanceSquared > maxDistance) {
                    continue;
                }

                BlockPos placePos = center.offset(dx, 0, dz);

                if (placePos.getY() <= level.getMinBuildHeight()) {
                    continue;
                }

                BlockState existing = level.getBlockState(placePos);

                /*
                 * 主尖刺要和混合過渡區重疊，
                 * 所以允許覆蓋黑曜石、哭泣黑曜石和已生成的水晶。
                 */
                if (
                        !existing.isAir()
                                && !existing.is(Blocks.OBSIDIAN)
                                && !existing.is(Blocks.CRYING_OBSIDIAN)
                                && !existing.is(
                                BlockRegister
                                        .VOID_CRYSTAL.crystalblock
                                        .get()
                        )
                ) {
                    continue;
                }

                level.setBlock(
                        placePos,
                        BlockRegister
                                .VOID_CRYSTAL.crystalblock
                                .get()
                                .defaultBlockState(),
                        Block.UPDATE_CLIENTS
                );

                crystals.add(placePos.immutable());
            }
        }
    }

    /*
     * =========================================================
     * 小型分叉
     * =========================================================
     */
    private void generateBranch(
            WorldGenLevel level,
            RandomSource random,
            BlockPos mainStart,
            int mainLength,
            int mainRadius,
            Set<BlockPos> crystals
    ) {

        int depth =
                1 + random.nextInt(
                        Math.max(
                                1,
                                mainLength / 3
                        )
                );

        BlockPos branchStart = mainStart.below(depth);

        int length =
                Math.max(
                        5,
                        (int) (
                                mainLength
                                        * (
                                        0.30D
                                                + random.nextDouble()
                                                * 0.25D
                                )
                        )
                );

        int radius = Math.max(2, mainRadius - 2);

        int offsetX = random.nextInt(7) - 3;
        int offsetZ = random.nextInt(7) - 3;

        if (offsetX == 0 && offsetZ == 0) {
            offsetX = random.nextBoolean() ? 3 : -3;
        }

        generateCrystalSpike(
                level,
                random,
                branchStart,
                length,
                radius,
                offsetX,
                offsetZ,
                crystals
        );
    }

    /*
     * =========================================================
     * 虛空水晶母岩
     * =========================================================
     */
    private void placeBuddingBlocks(
            WorldGenLevel level,
            RandomSource random,
            Set<BlockPos> crystals
    ) {

        if (crystals.isEmpty()) {
            return;
        }

        List<BlockPos> surface = new ArrayList<>();

        for (BlockPos pos : crystals) {
            if (isCrystalSurface(level, pos)) {
                surface.add(pos);
            }
        }

        if (surface.isEmpty()) {
            return;
        }

        int amount =
                MIN_BUDDING_BLOCKS
                        + random.nextInt(
                        MAX_BUDDING_BLOCKS
                                - MIN_BUDDING_BLOCKS
                                + 1
                );

        amount = Math.min(amount, surface.size());

        for (int i = 0; i < amount; i++) {

            int index = random.nextInt(surface.size());
            BlockPos pos = surface.remove(index);

            level.setBlock(
                    pos,
                    BlockRegister
                            .VOID_CRYSTAL.crystalbuddingblock
                            .get()
                            .defaultBlockState(),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    private boolean isCrystalSurface(
            WorldGenLevel level,
            BlockPos pos
    ) {

        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }

        return false;
    }
}

