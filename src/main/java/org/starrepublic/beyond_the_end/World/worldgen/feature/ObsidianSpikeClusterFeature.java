package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.starrepublic.beyond_the_end.Register.Tags;

public class ObsidianSpikeClusterFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_SPIKES = 3;
    private static final int MAX_SPIKES = 6;

    private static final int CLUSTER_RADIUS = 14;

    private static final int MIN_HEIGHT = 20;
    private static final int MAX_HEIGHT = 40;

    private static final int MIN_BASE_RADIUS = 3;
    private static final int MAX_BASE_RADIUS = 6;

    /* 底盤埋入地表的深度。 */
    private static final int BASE_EMBED_DEPTH = 3;

    /* 驗證底盤時額外多掃一圈，避免貼著懸崖生成。 */
    private static final int BASE_CHECK_MARGIN = 1;

    /* 底盤範圍內允許的最大地表高差。 */
    private static final int MAX_SURFACE_VARIATION = 5;

    public ObsidianSpikeClusterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int count = MIN_SPIKES + random.nextInt(MAX_SPIKES - MIN_SPIKES + 1);
        int generated = 0;

        for (int i = 0; i < count; i++) {

            int offsetX = random.nextInt(CLUSTER_RADIUS * 2 + 1) - CLUSTER_RADIUS;
            int offsetZ = random.nextInt(CLUSTER_RADIUS * 2 + 1) - CLUSTER_RADIUS;

            offsetX /= 2;
            offsetZ /= 2;

            int x = origin.getX() + offsetX;
            int z = origin.getZ() + offsetZ;

            /*
             * 先決定「這一根」尖刺的實際底盤半徑，
             * 再依照實際 footprint 檢查，而不是永遠拿 MAX_BASE_RADIUS 檢查。
             */
            int baseRadius = MIN_BASE_RADIUS + random.nextInt(MAX_BASE_RADIUS - MIN_BASE_RADIUS + 1);

            BlockPos basePos = findEmbeddedBase(level, x, z, baseRadius);
            if (basePos == null) {
                continue;
            }

            int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);

            int tiltX = random.nextInt(5) - 2;
            int tiltZ = random.nextInt(5) - 2;

            if (generateMainSpike(
                    level,
                    random,
                    basePos,
                    height,
                    baseRadius,
                    tiltX,
                    tiltZ,
                    BASE_EMBED_DEPTH
            )) {

                generated++;

                /*
                 * 側刺直接從主體中下段長出，
                 * 不再各自產生一個新的裸露底盤。
                 */
                int shardCount = random.nextInt(3); // 0~2
                for (int s = 0; s < shardCount; s++) {
                    generateSideShard(level, random, basePos, height, baseRadius);
                }
            }
        }

        return generated > 0;
    }

    /*
     * 找出底盤真正要埋入地形的位置。
     *
     * 1. 掃描實際底盤半徑 + 1 格安全邊。
     * 2. 地表高差太大就拒絕。
     * 3. 以最低地表為基準，向下埋 BASE_EMBED_DEPTH 格。
     * 4. 該地下平面在整個 footprint 中都不能出現 Air。
     */
    private BlockPos findEmbeddedBase(
            WorldGenLevel level,
            int centerX,
            int centerZ,
            int actualBaseRadius
    ) {

        int checkRadius = actualBaseRadius + BASE_CHECK_MARGIN;

        int centerSurfaceY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                centerX,
                centerZ
        ) - 1;

        BlockPos centerSurfacePos = new BlockPos(centerX, centerSurfaceY, centerZ);
        BlockState centerGround = level.getBlockState(centerSurfacePos);

        if (!centerGround.is(Tags.ENCHANTED_FEATURE_GROUND)) {
            return null;
        }

        if (!level.getBlockState(centerSurfacePos.above()).isAir()) {
            return null;
        }

        int minSurfaceY = Integer.MAX_VALUE;
        int maxSurfaceY = Integer.MIN_VALUE;

        for (int dx = -checkRadius; dx <= checkRadius; dx++) {
            for (int dz = -checkRadius; dz <= checkRadius; dz++) {

                if (dx * dx + dz * dz > checkRadius * checkRadius) {
                    continue;
                }

                int surfaceY = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        centerX + dx,
                        centerZ + dz
                ) - 1;

                minSurfaceY = Math.min(minSurfaceY, surfaceY);
                maxSurfaceY = Math.max(maxSurfaceY, surfaceY);
            }
        }

        if (minSurfaceY == Integer.MAX_VALUE) {
            return null;
        }

        if (maxSurfaceY - minSurfaceY > MAX_SURFACE_VARIATION) {
            return null;
        }

        int baseY = minSurfaceY - BASE_EMBED_DEPTH;

        /*
         * 這一層就是埋在地底的底盤平面。
         * 如果這裡有 Air，代表洞穴/懸空會讓底盤側面直接裸露。
         */
        for (int dx = -checkRadius; dx <= checkRadius; dx++) {
            for (int dz = -checkRadius; dz <= checkRadius; dz++) {

                if (dx * dx + dz * dz > checkRadius * checkRadius) {
                    continue;
                }

                BlockPos checkPos = new BlockPos(
                        centerX + dx,
                        baseY,
                        centerZ + dz
                );

                if (level.getBlockState(checkPos).isAir()) {
                    return null;
                }
            }
        }

        return new BlockPos(centerX, baseY, centerZ);
    }

    private boolean generateMainSpike(
            WorldGenLevel level,
            RandomSource random,
            BlockPos base,
            int height,
            int baseRadius,
            int tiltX,
            int tiltZ,
            int embedDepth
    ) {

        int placed = 0;
        int totalLayers = height + embedDepth;

        for (int layer = 0; layer < totalLayers; layer++) {

            int visibleLayer = layer - embedDepth;

            double progress;
            if (visibleLayer <= 0) {
                progress = 0.0D;
            }
            else {
                progress = Math.min(
                        1.0D,
                        visibleLayer / (double) Math.max(1, height - 1)
                );
            }

            double radius;

            if (visibleLayer < 0) {
                /* 地下的真正底盤保持最粗。 */
                radius = baseRadius;
            }
            else {
                /*
                 * 到真正地表時先縮小約一圈，
                 * 讓自然地面包住底盤外緣。
                 */
                double visibleBaseRadius = Math.max(1.5D, baseRadius - 1.0D);

                radius = Math.max(
                        0.45D,
                        visibleBaseRadius * Math.pow(1.0D - progress, 0.62D)
                );
            }

            int centerOffsetX = (int) Math.round(tiltX * progress * 2.0D);
            int centerOffsetZ = (int) Math.round(tiltZ * progress * 2.0D);

            BlockPos layerCenter = base.offset(centerOffsetX, layer, centerOffsetZ);
            int searchRadius = (int) Math.ceil(radius);

            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {

                    double xStretch = 0.85D + random.nextDouble() * 0.3D;
                    double zStretch = 0.85D + random.nextDouble() * 0.3D;

                    double nx = dx / (radius * xStretch);
                    double nz = dz / (radius * zStretch);

                    double dist = nx * nx + nz * nz;
                    double threshold = 1.0D + random.nextDouble() * 0.18D;

                    if (dist > threshold) {
                        continue;
                    }

                    BlockPos placePos = layerCenter.offset(dx, 0, dz);
                    BlockState existing = level.getBlockState(placePos);

                    /*
                     * 地表附近仍視為嵌入區。
                     * 尖刺可以吃進自然地形，避免坡面一側裸露。
                     */
                    boolean embeddedZone = layer <= embedDepth + MAX_SURFACE_VARIATION + 1;

                    if (embeddedZone) {
                        if (existing.is(Blocks.BEDROCK)) {
                            continue;
                        }
                    }
                    else if (!canReplace(existing)) {
                        continue;
                    }

                    BlockState spikeState = random.nextFloat() < 0.08F
                            ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                            : Blocks.OBSIDIAN.defaultBlockState();

                    level.setBlock(placePos, spikeState, Block.UPDATE_CLIENTS);
                    placed++;
                }
            }
        }

        return placed > 0;
    }

    private void generateSideShard(
            WorldGenLevel level,
            RandomSource random,
            BlockPos mainBase,
            int mainHeight,
            int mainBaseRadius
    ) {

        int sideHeight = 8 + random.nextInt(12); // 8~19
        int sideBaseRadius = Math.max(2, mainBaseRadius - 2);

        int maxOffset = Math.max(1, mainBaseRadius - 1);

        int offsetX = random.nextInt(maxOffset * 2 + 1) - maxOffset;
        int offsetZ = random.nextInt(maxOffset * 2 + 1) - maxOffset;

        int startLayer = BASE_EMBED_DEPTH + 1 + random.nextInt(4);

        BlockPos sideBase = mainBase.offset(offsetX, startLayer, offsetZ);

        int tiltX = random.nextInt(5) - 2;
        int tiltZ = random.nextInt(5) - 2;

        if (tiltX == 0 && tiltZ == 0) {
            tiltX = random.nextBoolean() ? 2 : -2;
        }

        generateMainSpike(
                level,
                random,
                sideBase,
                sideHeight,
                sideBaseRadius,
                tiltX,
                tiltZ,
                0
        );
    }

    private boolean canReplace(BlockState state) {
        return state.isAir()
                || state.is(Tags.ENCHANTED_FEATURE_GROUND)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN);
    }
}

