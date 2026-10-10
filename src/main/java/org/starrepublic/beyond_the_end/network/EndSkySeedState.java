package org.starrepublic.beyond_the_end.network;


public final class EndSkySeedState {
    public static final long UNSET =
            Long.MIN_VALUE;

    private static volatile long skySeed =
            UNSET;

    private EndSkySeedState() {
    }

    public static long getSkySeed() {
        return skySeed;
    }

    public static void setSkySeed(long seed) {
        skySeed = seed;
    }

    public static void clear() {
        skySeed = UNSET;
    }

}
