package org.starrepublic.beyond_the_end.Renders.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class EndVoidParticleController {
    private EndVoidParticleController() {
    }
    public static final double INNER_RADIUS = 250.0D;
    public static final double OUTER_RADIUS = 1000.0D;

    /*
     * 玩家周圍粒子生成距離。
     */
    private static final double MIN_PARTICLE_DISTANCE = 3;
    private static final double MAX_PARTICLE_DISTANCE = 28;

    /*
     * Y 軸範圍。
     */
    private static final double VERTICAL_RANGE = 14D;

    /*
     * 每 Tick 生成數量。
     *
     * 20 TPS：
     * 約 160～320 顆 / 秒。
     */
    private static final int MIN_PARTICLES_PER_TICK = 10;
    private static final int MAX_PARTICLES_PER_TICK = 18;

    public static void tick() {

        Minecraft minecraft =
                Minecraft.getInstance();

        LocalPlayer player =
                minecraft.player;

        ClientLevel level =
                minecraft.level;

        if (player == null || level == null) {
            return;
        }

        if (level.dimension() != Level.END) {
            return;
        }

        double x = player.getX();
        double z = player.getZ();

        double distanceSqr =
                x * x + z * z;

        if (
                distanceSqr < INNER_RADIUS * INNER_RADIUS
                        ||
                        distanceSqr > OUTER_RADIUS * OUTER_RADIUS
        ) {
            return;
        }

        spawnVoidParticles(
                level,
                player
        );
    }

    private static void spawnVoidParticles(
            ClientLevel level,
            LocalPlayer player
    ) {

        RandomSource random =
                level.random;

        int amount =
                MIN_PARTICLES_PER_TICK
                        + random.nextInt(
                        MAX_PARTICLES_PER_TICK
                                - MIN_PARTICLES_PER_TICK
                                + 1
                );

        for (int i = 0; i < amount; i++) {

            /*
             * 水平方向任意角度。
             */
            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0D;

            /*
             * 與玩家的距離。
             */
            double distance =
                    MIN_PARTICLE_DISTANCE
                            + random.nextDouble()
                            * (
                            MAX_PARTICLE_DISTANCE
                                    - MIN_PARTICLE_DISTANCE
                    );

            double px =
                    player.getX()
                            + Math.cos(angle)
                            * distance;

            double pz =
                    player.getZ()
                            + Math.sin(angle)
                            * distance;

            /*
             * 大範圍上下分布。
             */
            double py =
                    player.getY()
                            + (
                            random.nextDouble()
                                    * 2.0D
                                    - 1.0D
                    )
                            * VERTICAL_RANGE;

            /*
             * 非常慢的漂移。
             */
            double vx =
                    (
                            random.nextDouble()
                                    - 0.5D
                    ) * 0.008D;

            double vy =
                    (
                            random.nextDouble()
                                    - 0.5D
                    ) * 0.005D;

            double vz =
                    (
                            random.nextDouble()
                                    - 0.5D
                    ) * 0.008D;
            if (random.nextFloat() < 0.8F) {

                level.addParticle(
                        ParticleTypes.END_ROD,
                        px,
                        py,
                        pz,
                        vx,
                        vy,
                        vz
                );

            } else {

                level.addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        px,
                        py,
                        pz,
                        vx * 0.5D,
                        vy * 0.5D,
                        vz * 0.5D
                );
            }
        }
    }
}
