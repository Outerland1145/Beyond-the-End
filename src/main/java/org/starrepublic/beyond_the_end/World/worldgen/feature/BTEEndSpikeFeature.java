package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;

import java.util.List;

public class BTEEndSpikeFeature
        extends Feature<SpikeConfiguration> {

    public BTEEndSpikeFeature() {
        super(SpikeConfiguration.CODEC);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<SpikeConfiguration> context
    ) {
        WorldGenLevel level = context.level();
        SpikeConfiguration config = context.config();

        /*
         * 資料包沒有指定柱子列表：
         * 讀取目前世界的配置化環形排列。
         *
         * 若明確提供列表：
         * 使用指定列表，供龍復活時重建單根柱子。
         */
        List<SpikeFeature.EndSpike> spikes =
                config.getSpikes().isEmpty()
                        ? BTEEndSpikeLayout.getSpikes(level.getSeed())
                        : config.getSpikes();

        boolean generated = false;

        for (SpikeFeature.EndSpike spike : spikes) {

            /*
             * 正常地物生成只在柱子中心所屬的
             * Chunk 中執行，避免重複生成。
             */
            if (!spike.isCenterWithinChunk(context.origin())) {
                continue;
            }

            int centerX = spike.getCenterX();
            int centerZ = spike.getCenterZ();
            int radius = spike.getRadius();
            int height = spike.getHeight();

            if (radius < 1
                    || height <= level.getMinBuildHeight()
                    || height + 5 >= level.getMaxBuildHeight()) {
                continue;
            }

            /*
             * 黑曜石柱主體。
             *
             * 與原版相近，從 Y=0 向上建立
             * 圓形黑曜石柱體，並清理柱頂周圍空間。
             */
            int minY = Math.max(
                    0,
                    level.getMinBuildHeight()
            );

            int maxY = Math.min(
                    height + 10,
                    level.getMaxBuildHeight() - 1
            );

            BlockPos.MutableBlockPos mutable =
                    new BlockPos.MutableBlockPos();

            for (int x = centerX - radius - 1;
                 x <= centerX + radius + 1; x++) {

                for (int z = centerZ - radius - 1;
                     z <= centerZ + radius + 1; z++) {

                    int dx = x - centerX;
                    int dz = z - centerZ;

                    boolean inside =
                            dx * dx + dz * dz
                                    <= radius * radius + 1;

                    for (int y = minY; y <= maxY; y++) {

                        mutable.set(x, y, z);

                        if (inside && y < height) {

                            level.setBlock(
                                    mutable,
                                    Blocks.OBSIDIAN.defaultBlockState(),
                                    2
                            );

                        } else if (y > 65) {

                            level.setBlock(
                                    mutable,
                                    Blocks.AIR.defaultBlockState(),
                                    2
                            );
                        }
                    }
                }
            }

            // 柱頂基岩
            BlockPos top = new BlockPos(
                    centerX,
                    height,
                    centerZ
            );

            level.setBlock(
                    top,
                    Blocks.BEDROCK.defaultBlockState(),
                    2
            );

            /*
             * 有籠子的柱子：
             * 建立 5x5 鐵欄杆籠子。
             */
            if (spike.isGuarded()) {

                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        for (int y = 0; y <= 3; y++) {

                            boolean xEdge = Math.abs(x) == 2;
                            boolean zEdge = Math.abs(z) == 2;
                            boolean roof = y == 3;

                            // 只生成四周牆面及頂部
                            if (!xEdge && !zEdge && !roof) {
                                continue;
                            }

                            // 按照籠子造型直接設定鐵柵欄連接方向
                            boolean connectNS = xEdge || roof;
                            boolean connectEW = zEdge || roof;

                            BlockState bars = Blocks.IRON_BARS
                                    .defaultBlockState()
                                    .setValue(
                                            IronBarsBlock.NORTH,
                                            connectNS && z != -2
                                    )
                                    .setValue(
                                            IronBarsBlock.SOUTH,
                                            connectNS && z != 2
                                    )
                                    .setValue(
                                            IronBarsBlock.WEST,
                                            connectEW && x != -2
                                    )
                                    .setValue(
                                            IronBarsBlock.EAST,
                                            connectEW && x != 2
                                    );

                            // 修正高度：不再額外增加一格
                            level.setBlock(
                                    top.offset(x, y, z),
                                    bars,
                                    3
                            );
                        }
                    }
                }
            }

            // 原版柱頂的火
            level.setBlock(
                    top.above(),
                    Blocks.FIRE.defaultBlockState(),
                    3
            );

            EndCrystal crystal = new EndCrystal(
                    level.getLevel(),
                    centerX + 0.5,
                    height + 1.0,
                    centerZ + 0.5
            );

            crystal.setBeamTarget(
                    config.getCrystalBeamTarget()
            );

            crystal.setInvulnerable(
                    config.isCrystalInvulnerable()
            );

            level.addFreshEntity(crystal);

            generated = true;
        }

        return generated;
    }
}

