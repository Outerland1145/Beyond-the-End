package org.starrepublic.beyond_the_end.World.EndBarrier;

import net.minecraft.util.Mth;

public final class ClientBarrierState {

    private ClientBarrierState() {
    }

    private static boolean barrierEnabled = true;

    private static float distortion = 0.0F;
    private static float whiteout = 0.0F;

    private static Phase phase = Phase.NORMAL;

    private static int phaseTicks = 0;

    public enum Phase {
        NORMAL,
        REJECTING,
        FADING
    }

    public static boolean isBarrierEnabled() {
        return barrierEnabled;
    }

    public static void setBarrierEnabled(boolean enabled) {

        barrierEnabled = enabled;

        if (!enabled) {

            distortion = 0.0F;
            whiteout = 0.0F;

            phase = Phase.NORMAL;
            phaseTicks = 0;
        }
    }

    public static float getDistortion() {
        return distortion;
    }

    public static float getWhiteout() {
        return whiteout;
    }

    public static Phase getPhase() {
        return phase;
    }

    public static void beginRejection() {

        if (phase != Phase.NORMAL) {
            return;
        }

        phase = Phase.REJECTING;
        phaseTicks = 0;
    }

    public static void beginFadeOut() {

        phase = Phase.FADING;
        phaseTicks = 0;

        whiteout = 1.0F;
    }

    public static void tickNormal(
            float targetDistortion
    ) {

        if (phase != Phase.NORMAL) {
            return;
        }

        /*
         * 做平滑過渡，避免距離每 Tick 改變時 shader 抖動。
         */
        distortion =
                Mth.lerp(
                        0.12F,
                        distortion,
                        targetDistortion
                );

        whiteout =
                Mth.lerp(
                        0.20F,
                        whiteout,
                        0.0F
                );
    }

    public static void tickRejection() {

        if (phase != Phase.REJECTING) {
            return;
        }

        phaseTicks++;

        /*
         * 前半段：
         * 螺旋迅速增強。
         */
        if (phaseTicks <= 8) {

            distortion =
                    Mth.clamp(
                            distortion + 0.13F,
                            0.0F,
                            1.5F
                    );

            return;
        }

        /*
         * 後半段：
         * 保持極強螺旋，同時白屏。
         */
        distortion = 1.5F;

        float progress =
                (phaseTicks - 8) / 6.0F;

        whiteout =
                Mth.clamp(
                        progress,
                        0.0F,
                        1.0F
                );
    }

    public static void tickFadeOut() {

        if (phase != Phase.FADING) {
            return;
        }

        phaseTicks++;

        /*
         * 約半秒淡出。
         */
        float progress =
                phaseTicks / 10.0F;

        whiteout =
                1.0F - Mth.clamp(
                        progress,
                        0.0F,
                        1.0F
                );

        distortion =
                1.0F - Mth.clamp(
                        progress,
                        0.0F,
                        1.0F
                );

        if (phaseTicks >= 10) {

            phase = Phase.NORMAL;

            distortion = 0.0F;
            whiteout = 0.0F;

            phaseTicks = 0;
        }
    }

    public static void reset() {

        distortion = 0.0F;
        whiteout = 0.0F;

        phase = Phase.NORMAL;
        phaseTicks = 0;
    }
}
