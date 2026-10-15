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

    /*
     * 一次生成幾根主尖刺
     */
    private static final int MIN_SPIKES = 3;
    private static final int MAX_SPIKES = 6;

    /*
     * 整個尖刺群範圍
     */
    private static final int CLUSTER_RADIUS = 14;

    /*
     * 單根尖刺高度：20 ~ 40
     */
    private static final int MIN_HEIGHT = 20;
    private static final int MAX_HEIGHT = 40;

    /*
     * 單根底部粗細
     */
    private static final int MIN_BASE_RADIUS = 3;
    private static final int MAX_BASE_RADIUS = 6;

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

            /*
             * 向中心集中，避免過散
             */
            offsetX /= 2;
            offsetZ /= 2;

            int x = origin.getX() + offsetX;
            int z = origin.getZ() + offsetZ;

            int surfaceY = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            ) - 1;

            BlockPos basePos = new BlockPos(x, surfaceY, z);
            BlockState ground = level.getBlockState(basePos);

            if (!ground.is(Tags.ENCHANTED_FEATURE_GROUND)) {
                continue;
            }

            if (!level.getBlockState(basePos.above()).isAir()) {
                continue;
            }

            int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
            int baseRadius = MIN_BASE_RADIUS + random.nextInt(MAX_BASE_RADIUS - MIN_BASE_RADIUS + 1);

            /*
             * 頂端傾斜，讓尖刺不是每根都完全筆直
             */
            int tiltX = random.nextInt(5) - 2; // -2 ~ 2
            int tiltZ = random.nextInt(5) - 2;

            if (generateMainSpike(level, random, basePos, height, baseRadius, tiltX, tiltZ)) {
                generated++;

                /*
                 * 給部分主尖刺附帶小側刺
                 */
                int shardCount = random.nextInt(3); // 0~2
                for (int s = 0; s < shardCount; s++) {
                    generateSideShard(level, random, basePos, height, baseRadius);
                }
            }
        }

        return generated > 0;
    }

    private boolean generateMainSpike(
            WorldGenLevel level,
            RandomSource random,
            BlockPos base,
            int height,
            int baseRadius,
            int tiltX,
            int tiltZ
    ) {

        int placed = 0;

        /*
         * 插入地下 2 格，讓根部穩
         */
        for (int y = -2; y < height; y++) {

            double progress = Math.max(0.0D, y / (double) height);

            /*
             * 這個 taper 會讓底部厚，中上段開始快速收尖
             *
             * 指數越小越尖
             */
            double radius = Math.max(
                    0.45D,
                    baseRadius * Math.pow(1.0D - progress, 0.62D)
            );

            /*
             * 高度越高，中心越向 tilt 方向偏移
             */
            int centerOffsetX = (int) Math.round(tiltX * progress * 2.0D);
            int centerOffsetZ = (int) Math.round(tiltZ * progress * 2.0D);

            BlockPos layerCenter = base.offset(centerOffsetX, y, centerOffsetZ);

            int searchRadius = (int) Math.ceil(radius);

            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {

                    /*
                     * 橢圓化 + 不規則化
                     */
                    double xStretch = 0.85D + random.nextDouble() * 0.3D;
                    double zStretch = 0.85D + random.nextDouble() * 0.3D;

                    double nx = dx / (radius * xStretch);
                    double nz = dz / (radius * zStretch);

                    double dist = nx * nx + nz * nz;

                    /*
                     * 邊緣加入抖動，避免太數學化
                     */
                    double threshold = 1.0D + random.nextDouble() * 0.18D;

                    if (dist > threshold) {
                        continue;
                    }

                    BlockPos placePos = layerCenter.offset(dx, 0, dz);
                    BlockState existing = level.getBlockState(placePos);

                    if (!canReplace(existing)) {
                        continue;
                    }

                    /*
                     * 少量哭泣黑曜石點綴
                     */
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
        int sideBaseRadius = Math.max(2, mainBaseRadius - 1);

        int offsetX = random.nextInt(mainBaseRadius * 2 + 5) - (mainBaseRadius + 2);
        int offsetZ = random.nextInt(mainBaseRadius * 2 + 5) - (mainBaseRadius + 2);

        BlockPos sideBase = mainBase.offset(offsetX, 0, offsetZ);

        int tiltX = random.nextInt(5) - 2;
        int tiltZ = random.nextInt(5) - 2;

        generateMainSpike(level, random, sideBase, sideHeight, sideBaseRadius, tiltX, tiltZ);
    }

    private boolean canReplace(BlockState state) {
        return state.isAir()
                || state.is(Tags.ENCHANTED_FEATURE_GROUND)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN);
    }
}
