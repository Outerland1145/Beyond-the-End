package org.starrepublic.beyond_the_end.World.EndBarrier;

import net.minecraft.util.Mth;

public final class EndBarrierClientState {

    private static float distortion = 0.0F;

    private static float whiteout = 0.0F;

    private EndBarrierClientState() {
    }

    public static float getDistortion() {
        return distortion;
    }

    public static void setDistortion(float value) {
        distortion = Mth.clamp(value, 0.0F, 1.0F);
    }

    public static float getWhiteout() {
        return whiteout;
    }

    public static void setWhiteout(float value) {
        whiteout = Mth.clamp(value, 0.0F, 1.0F);
    }

    public static void reset() {
        distortion = 0.0F;
        whiteout = 0.0F;
    }
}
