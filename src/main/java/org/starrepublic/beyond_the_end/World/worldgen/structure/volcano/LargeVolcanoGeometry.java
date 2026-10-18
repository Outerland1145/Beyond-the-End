package org.starrepublic.beyond_the_end.World.worldgen.structure.volcano;

import net.minecraft.util.Mth;

/**
 * Pure, coordinate-based volcano geometry. No WorldGenLevel and no chunk loads.
 * Both the river planner and the chunk-local structure piece call EXACTLY this
 * implementation, so their top surfaces agree.
 */
public final class LargeVolcanoGeometry {
    public record Shape(boolean inside, double distance, double normalizedDistance,
                        double craterRadius, double height, boolean craterInterior, double rimHeight) {
        public static final Shape OUTSIDE = new Shape(false, 0, 1, 0, 0, false, 0);
    }

    private final int centerX, baseY, centerZ, radius, volcanoHeight;
    private final double craterRatio;
    private final double lavaHeightRatio, lavaFillRatio;
    private final long seed;

    public LargeVolcanoGeometry(int centerX, int baseY, int centerZ,
                                int radius, int volcanoHeight, double craterRatio, long seed,
                                double lavaHeightRatio, double lavaFillRatio) {
        this.centerX = centerX;
        this.baseY = baseY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.volcanoHeight = volcanoHeight;
        this.craterRatio = craterRatio;
        this.seed = seed;
        this.lavaHeightRatio = lavaHeightRatio;
        this.lavaFillRatio = lavaFillRatio;
    }

    public int centerX() { return centerX; }
    public int centerZ() { return centerZ; }
    public int radius() { return radius; }

    /** Normalized radial distance including the same noisy edge as shape().
     *  Values greater than 1 are OUTSIDE the volcano and are used by the
     *  native-material transition skirt. */
    public double normalizedRadiusAt(int x, int z) {
        double dx = (double) x - centerX, dz = (double) z - centerZ;
        double angle = Math.atan2(dz, dx);
        double edgeNoise = fbm(x * 0.020, z * 0.020, 0x1A2B3C4D5E6F7801L);
        int ridgeCount = 5 + stableInt(0x517CC1B727220A95L, 4);
        double phase = stableUnit(0x632BE59BD9B4E019L) * Math.PI * 2;
        double outlineWave = Math.sin(angle * ridgeCount + phase + edgeNoise * 1.5);
        double effectiveRadius = radius * (1 + edgeNoise * 0.10 + outlineWave * 0.045);
        return Math.hypot(dx, dz) / Math.max(1.0, effectiveRadius);
    }

    public double transitionNoise(int x, int z) {
        return fbm(x * 0.055, z * 0.055, 0x5782E77946A51C7FL);
    }

    public Shape shape(int x, int z) {
        double dx = (double) x - centerX;
        double dz = (double) z - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double angle = Math.atan2(dz, dx);
        double t = normalizedRadiusAt(x, z);
        if (t > 1.0) return Shape.OUTSIDE;
        t = Mth.clamp(t, 0, 1);
        double edgeNoise = fbm(x * 0.020, z * 0.020, 0x1A2B3C4D5E6F7801L);
        int ridgeCount = 5 + stableInt(0x517CC1B727220A95L, 4);
        double phase = stableUnit(0x632BE59BD9B4E019L) * Math.PI * 2;

        double outerProfile = volcanoHeight * Math.pow(1 - t, 1.34);
        double ridge = Math.sin(angle * ridgeCount + phase + edgeNoise * 1.7);
        double ridgeHeight = ridge * volcanoHeight * 0.065 * Math.pow(1 - t, 0.65);
        double mediumNoise = fbm(x * 0.060, z * 0.060, 0x6A09E667F3BCC909L);
        double rough = (edgeNoise * 0.045 + mediumNoise * 0.026)
                * volcanoHeight * Math.pow(1 - t, 0.70);

        double craterNoise = fbm(x * 0.035, z * 0.035, 0xBB67AE8584CAA73BL);
        double craterRadius = radius * craterRatio * (1 + craterNoise * 0.07);
        double depression = 0;
        if (distance < craterRadius) {
            double craterT = 1 - distance / craterRadius;
            depression = volcanoHeight * 0.55 * Math.pow(craterT, 1.35);
        }
        double rimOffset = (distance - craterRadius * 1.08) / Math.max(3, craterRadius * 0.28);
        double rimHeight = volcanoHeight * 0.14 * Math.exp(-rimOffset * rimOffset);
        double h = Math.max(0, outerProfile + ridgeHeight + rough + rimHeight - depression);
        return new Shape(true, distance, t, craterRadius, h,
                distance < craterRadius * 0.96, rimHeight);
    }

    /**
     * Surface projected on original generator terrain, NOT on already modified chunks.
     * This formula is shared by river planning and actual mountain placement.
     */
    public int top(int x, int z, int naturalTop, Shape shape) {
        if (!shape.inside()) return naturalTop;
        double foundationWeight = smoothStep(Mth.clamp((1 - shape.normalizedDistance()) / 0.80, 0, 1));
        int foundationY = (int) Math.round(lerp(foundationWeight, naturalTop, baseY));
        int top = foundationY + (int) Math.round(shape.height());
        return shape.craterInterior() ? top : Math.max(naturalTop, top);
    }

    public int craterLavaY() {
        return baseY + (int) Math.round(volcanoHeight * lavaHeightRatio);
    }

    public boolean isCraterLakeColumn(int x, int z, Shape shape) {
        return shape.craterInterior()
                && shape.distance() <= shape.craterRadius() * lavaFillRatio;
    }

    public double random01(int x, int y, int z, long salt) {
        return (hash(seed, x, y, z, salt) >>> 11) * 0x1.0p-53;
    }

    private double stableUnit(long salt) {
        return (mix64(seed ^ salt) >>> 11) * 0x1.0p-53;
    }

    private int stableInt(long salt, int bound) {
        return Math.min(bound - 1, (int) (stableUnit(salt) * bound));
    }

    private double fbm(double x, double z, long salt) {
        double value = 0, amplitude = 1, frequency = 1, norm = 0;
        for (int i = 0; i < 4; i++) {
            value += valueNoise(x * frequency, z * frequency,
                    salt + i * 0x9E3779B97F4A7C15L) * amplitude;
            norm += amplitude;
            amplitude *= 0.5;
            frequency *= 2;
        }
        return value / norm;
    }

    private double valueNoise(double x, double z, long salt) {
        int x0 = Mth.floor(x), z0 = Mth.floor(z);
        double sx = smoothStep(x - x0), sz = smoothStep(z - z0);
        double a = lerp(sx, lattice(x0, z0, salt), lattice(x0 + 1, z0, salt));
        double b = lerp(sx, lattice(x0, z0 + 1, salt), lattice(x0 + 1, z0 + 1, salt));
        return lerp(sz, a, b);
    }

    private double lattice(int x, int z, long salt) {
        return ((hash(seed, x, 0, z, salt) >>> 11) * 0x1.0p-53) * 2 - 1;
    }

    private static double smoothStep(double t) { return t * t * (3 - 2 * t); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }

    public static long hash(long seed, int x, int y, int z, long salt) {
        return mix64(seed ^ salt
                ^ ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) y * 0xD1B54A32D192ED03L)
                ^ ((long) z * 0x94D049BB133111EBL));
    }

    public static long mix64(long v) {
        v = (v ^ (v >>> 30)) * 0xBF58476D1CE4E5B9L;
        v = (v ^ (v >>> 27)) * 0x94D049BB133111EBL;
        return v ^ (v >>> 31);
    }
}
