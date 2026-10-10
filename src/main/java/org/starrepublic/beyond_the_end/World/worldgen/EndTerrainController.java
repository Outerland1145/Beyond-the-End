package org.starrepublic.beyond_the_end.World.worldgen;

import net.minecraft.util.Mth;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

public final class EndTerrainController {

    private EndTerrainController() {}

    /*
     * ==============================
     * 區域半徑
     * ==============================
     */

    //public static final double NORMAL_RADIUS = 10_000.0D;

    /*
     * 13000 開始：
     *
     * 不再保留任何原版大島形狀。
     */
    public static final long
            FULL_FRACTURED_OFFSET = 3_000L;

    /*
     * 最後 1000 格開始進入虛無。
     *
     * 預設：
     * 19000 → 20000
     */
    public static final long
            VOID_TRANSITION_OFFSET = 9_000L;

    /*
     * 整個破碎區寬度。
     */
    public static final long
            FRACTURED_BAND_WIDTH = 10_000L;

    public static final double VOID_RADIUS = 10_000L;

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

    public static boolean
    isCustomTerrainEnabled() {

        return BTEConfigs.SERVER
                .enableCustomEndTerrain
                .get();
    }
    public static long getNormalRadius() {

        return BTEConfigs.SERVER
                .normalEndRadius
                .get()
                .longValue();
    }
    public static long
    getFullFracturedRadius() {

        return getNormalRadius()
                +
                FULL_FRACTURED_OFFSET;
    }

    /*
     * ============================================================
     * 虛無過渡開始
     * ============================================================
     *
     * normal + 9000
     */

    public static long
    getVoidTransitionRadius() {

        return getNormalRadius()
                +
                VOID_TRANSITION_OFFSET;
    }

    /*
     * ============================================================
     * 虛無區開始
     * ============================================================
     *
     * normal + 10000
     */

    public static long getVoidRadius() {

        return getNormalRadius()
                +
                FRACTURED_BAND_WIDTH;
    }


    public static EndRegion getRegion(
            int x,
            int z
    ) {

        double distance =
                distanceFromCenter(
                        x,
                        z
                );

        if (
                distance
                        <
                        getNormalRadius()
        ) {

            return EndRegion.NORMAL;
        }

        if (
                distance
                        <
                        getVoidRadius()
        ) {

            return EndRegion.FRACTURED;
        }

        return EndRegion.VOID;
    }

    public static boolean isNormal(
            int x,
            int z
    ) {

        return getRegion(
                x,
                z
        )
                ==
                EndRegion.NORMAL;
    }

    public static boolean isFractured(
            int x,
            int z
    ) {

        return getRegion(
                x,
                z
        )
                ==
                EndRegion.FRACTURED;
    }

    public static boolean isVoid(
            int x,
            int z
    ) {

        return getRegion(
                x,
                z
        )
                ==
                EndRegion.VOID;
    }

    /*
     * ============================================================
     * 距離
     * ============================================================
     */

    public static double distanceFromCenter(
            int x,
            int z
    ) {

        /*
         * 不使用：
         *
         * int x*x
         *
         * 避免溢位。
         */

        double dx = x;
        double dz = z;

        return Math.sqrt(
                dx * dx
                        +
                        dz * dz
        );
    }

    /*
     * ============================================================
     * Normal → Fractured
     * ============================================================
     *
     * normalRadius：
     * 0
     *
     * normalRadius + 3000：
     * 1
     */

    public static double
    getFracturedTransition(
            double distance
    ) {

        double normal =
                getNormalRadius();

        double fullFractured =
                getFullFracturedRadius();

        return smoothStep(
                normal,
                fullFractured,
                distance
        );
    }

    /*
     * ============================================================
     * 純破碎區進度
     * ============================================================
     *
     * normal + 3000：
     * 0
     *
     * normal + 10000：
     * 1
     */

    public static double
    getDeepFracturedProgress(
            double distance
    ) {

        double start =
                getFullFracturedRadius();

        double end =
                getVoidRadius();

        return Mth.clamp(
                (
                        distance
                                -
                                start
                )
                        /
                        (
                                end
                                        -
                                        start
                        ),
                0.0D,
                1.0D
        );
    }

    /*
     * ============================================================
     * Fractured → Void
     * ============================================================
     *
     * normal + 9000：
     * 0
     *
     * normal + 10000：
     * 1
     */

    public static double
    getVoidTransition(
            double distance
    ) {

        return smoothStep(
                getVoidTransitionRadius(),
                getVoidRadius(),
                distance
        );
    }

    /*
     * ============================================================
     * Utility
     * ============================================================
     */

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
