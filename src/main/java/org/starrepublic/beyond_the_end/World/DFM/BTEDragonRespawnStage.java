package org.starrepublic.beyond_the_end.World.DFM;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import org.starrepublic.beyond_the_end.World.worldgen.feature.BTEEndSpikeLayout;

import java.util.List;

public enum BTEDragonRespawnStage {

    START,
    PREPARING_TO_SUMMON_PILLARS,
    SUMMONING_PILLARS,
    SUMMONING_DRAGON,
    END;

    /**
     * 自訂復活動畫。
     *
     * 回傳目前 Tick 結束後應進入的階段。
     * 不呼叫原版 DragonRespawnAnimation。
     */
    public BTEDragonRespawnStage tick(
            ServerLevel level,
            BTEDragonFightManager fight,
            List<EndCrystal> crystals,
            int time
    ) {
        BlockPos origin = fight.getFightOrigin();

        BlockPos center = new BlockPos(
                origin.getX(),
                128,
                origin.getZ()
        );

        switch (this) {

            case START -> {

                if (time == 0) {

                    // 祭壇水晶集中指向中心上空。
                    for (EndCrystal crystal : crystals) {
                        crystal.setBeamTarget(center);
                    }
                }

                if (time >= 1) {
                    return PREPARING_TO_SUMMON_PILLARS;
                }
            }

            case PREPARING_TO_SUMMON_PILLARS -> {

                if (time % 10 == 0) {

                    level.sendParticles(
                            ParticleTypes.END_ROD,
                            center.getX() + 0.5,
                            center.getY() + 0.5,
                            center.getZ() + 0.5,
                            20,
                            2.0,
                            2.0,
                            2.0,
                            0.1
                    );
                }

                if (time >= 100) {
                    return SUMMONING_PILLARS;
                }
            }

            case SUMMONING_PILLARS -> {

                /*
                 * 使用 Beyond The End 的實際柱子列表。
                 *
                 * 每根柱子分配 40 Tick。
                 *
                 * 10 根 = 400 Tick
                 * 15 根 = 600 Tick
                 * 20 根 = 800 Tick
                 */
                List<SpikeFeature.EndSpike> spikes = BTEEndSpikeLayout.getSpikes(level);

                int index = time / 40;
                int localTime = time % 40;

                if (index >= spikes.size()) {
                    return SUMMONING_DRAGON;
                }

                SpikeFeature.EndSpike spike =
                        spikes.get(index);

                BlockPos spikeTop = new BlockPos(
                        spike.getCenterX(),
                        spike.getHeight() + 1,
                        spike.getCenterZ()
                );

                // 指向準備重建的黑曜石柱。
                if (localTime == 0) {

                    for (EndCrystal crystal : crystals) {
                        crystal.setBeamTarget(spikeTop);
                    }
                }

                // 重建之前顯示粒子。
                if (localTime >= 30 && localTime < 39) {

                    level.sendParticles(
                            ParticleTypes.END_ROD,
                            spikeTop.getX() + 0.5,
                            spikeTop.getY() + 0.5,
                            spikeTop.getZ() + 0.5,
                            8,
                            2.0,
                            3.0,
                            2.0,
                            0.05
                    );
                    level.sendParticles(
                            ParticleTypes.REVERSE_PORTAL,
                            spikeTop.getX() + 0.5,
                            spikeTop.getY() + 1.0,
                            spikeTop.getZ() + 0.5,
                            12,
                            1.5, 2.0, 1.5,
                            0.08
                    );
                }

                // 每根柱子的最後一個 Tick 執行重建。
                if (localTime == 39) {

                    boolean rebuilt = fight.regenerateSpike(
                            spike,
                            center
                    );

                    if (rebuilt) {

                        double x = spikeTop.getX() + 0.5;
                        double y = spikeTop.getY() + 0.5;
                        double z = spikeTop.getZ() + 0.5;

                        // 真正的爆炸，會產生爆炸聲、傷害和擊退。
                        level.explode(
                                null,
                                x, y, z,
                                5.0F,
                                Level.ExplosionInteraction.NONE
                        );

                        // 額外的終界能量爆發。
                        level.sendParticles(
                                ParticleTypes.DRAGON_BREATH,
                                x, y, z,
                                80,
                                2.5, 2.5, 2.5,
                                0.12
                        );

                        level.sendParticles(
                                ParticleTypes.FALLING_OBSIDIAN_TEAR,
                                x, y + 1.0, z,
                                60,
                                3.0, 2.0, 3.0,
                                0.04
                        );

                        level.sendParticles(
                                ParticleTypes.FLASH,
                                x, y, z,
                                1,
                                0, 0, 0,
                                0
                        );
                    }
                }
            }

            case SUMMONING_DRAGON -> {

                if (time == 0) {

                    for (EndCrystal crystal : crystals) {
                        crystal.setBeamTarget(center);
                    }
                }

                if (time >= 80 && time % 5 == 0) {

                    level.sendParticles(
                            ParticleTypes.EXPLOSION,
                            center.getX() + 0.5,
                            center.getY() + 0.5,
                            center.getZ() + 0.5,
                            10,
                            4.0,
                            4.0,
                            4.0,
                            0.05
                    );
                }

                if (time >= 100) {
                    return END;
                }
            }

            case END -> {
                return END;
            }
        }

        return this;
    }
}

