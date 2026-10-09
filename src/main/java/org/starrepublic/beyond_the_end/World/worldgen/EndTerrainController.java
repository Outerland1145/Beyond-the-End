package org.starrepublic.beyond_the_end.World.worldgen;

import net.minecraft.util.Mth;

public final class EndTerrainController {

    private EndTerrainController() {}

    /*
     * ==============================
     * 區域半徑
     * ==============================
     */

    public static final double NORMAL_RADIUS = 10_000.0D;

    /*
     * 13000 開始：
     *
     * 不再保留任何原版大島形狀。
     */
    public static final double FULL_FRACTURED_RADIUS = 13_000.0D;

    public static final double VOID_RADIUS = 20_000.0D;

    public static final long NORMAL_RADIUS_SQR =
            100_000_000L;

    public static final long FULL_FRACTURED_RADIUS_SQR =
            169_000_000L;

    public static final long VOID_RADIUS_SQR =
            400_000_000L;

    /*
     * 強制虛空。
     */
    public static final double VOID_DENSITY =
            -1.0D;

    public static EndRegion getRegion(
            int x,
            int z
    ) {

        long distanceSq =
                (long) x * x
                        +
                        (long) z * z;

        if (
                distanceSq
                        <
                        NORMAL_RADIUS_SQR
        ) {

            return EndRegion.NORMAL;
        }

        if (
                distanceSq
                        <
                        VOID_RADIUS_SQR
        ) {

            return EndRegion.FRACTURED;
        }

        return EndRegion.VOID;
    }

    public static boolean isNormal(
            int x,
            int z
    ) {

        return getRegion(x, z)
                ==
                EndRegion.NORMAL;
    }

    public static boolean isFractured(
            int x,
            int z
    ) {

        return getRegion(x, z)
                ==
                EndRegion.FRACTURED;
    }

    public static boolean isVoid(
            int x,
            int z
    ) {

        return getRegion(x, z)
                ==
                EndRegion.VOID;
    }

    public static double distanceFromCenter(
            int x,
            int z
    ) {

        return Math.sqrt(
                (double) x * x
                        +
                        (double) z * z
        );
    }

    /*
     * 10000 → 13000
     *
     * 0 = 原版
     * 1 = 完全碎島
     */
    public static double getFracturedTransition(
            double distance
    ) {

        return smoothStep(
                10_000.0D,
                13_000.0D,
                distance
        );
    }

    /*
     * 13000 → 20000
     *
     * 用於控制碎島逐漸縮小、變稀疏。
     */
    public static double getDeepFracturedProgress(
            double distance
    ) {

        return Mth.clamp(
                (
                        distance
                                -
                                13_000.0D
                )
                        /
                        7_000.0D,
                0.0D,
                1.0D
        );
    }

    /*
     * 19000 → 20000
     *
     * 最終消失。
     */
    public static double getVoidTransition(
            double distance
    ) {

        return smoothStep(
                19_000.0D,
                20_000.0D,
                distance
        );
    }

    public static double smoothStep(
            double min,
            double max,
            double value
    ) {

        double x =
                Mth.clamp(
                        (value - min)
                                /
                                (max - min),
                        0.0D,
                        1.0D
                );

        return x
                *
                x
                *
                (
                        3.0D
                                -
                                2.0D * x
                );
    }

    public static double lerp(
            double delta,
            double start,
            double end
    ) {

        return start
                +
                delta
                        *
                        (end - start);
    }
}
