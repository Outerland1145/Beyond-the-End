package org.starrepublic.beyond_the_end.Utl;

public final class EndSkySeedUtil {

    private static final long END_SKY_SALT =
            0x5E12D7B94C38AF21L;

    private EndSkySeedUtil() {
    }

    public static long createEndSkySeed(long worldSeed) {

        return mixSeed(
                worldSeed,
                END_SKY_SALT
        );
    }

    private static long mixSeed(
            long seed,
            long salt
    ) {

        long z = seed + salt;

        z =
                (z ^ (z >>> 30))
                        * 0xBF58476D1CE4E5B9L;

        z =
                (z ^ (z >>> 27))
                        * 0x94D049BB133111EBL;

        return z ^ (z >>> 31);
    }
}
