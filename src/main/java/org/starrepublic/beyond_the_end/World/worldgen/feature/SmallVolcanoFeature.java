package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** Compact cinder cone with a real concave crater and optional magma pool. */
public final class SmallVolcanoFeature extends Feature<VolcanicFeatureConfigs.SmallVolcano> {
    public SmallVolcanoFeature() { super(VolcanicFeatureConfigs.SmallVolcano.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<VolcanicFeatureConfigs.SmallVolcano> ctx) {
        var c = ctx.config();
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        int cx = ctx.origin().getX(), cz = ctx.origin().getZ();
        int radius = VolcanicFeatureUtil.between(random, c.minRadius(), c.maxRadius());
        int height = VolcanicFeatureUtil.between(random, c.minHeight(), c.maxHeight());
        int cy = VolcanicFeatureUtil.groundY(level, cx, cz);
        if (!VolcanicFeatureUtil.isGround(level, cx, cy, cz, c.groundBlocks())) return false;
        if (cy + height + 4 >= level.getMaxBuildHeight() || cy - 5 <= level.getMinBuildHeight()) return false;
        if (!VolcanicFeatureUtil.writableFootprint(level, cx, cy, cz, radius + 2)) return false;

        // Terrain validation: do not balance cones on an overhang, void edge or lava basin.
        int good = 0;
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            int x = cx + (int) Math.round(Math.cos(a) * radius * 0.70);
            int z = cz + (int) Math.round(Math.sin(a) * radius * 0.70);
            int gy = VolcanicFeatureUtil.groundY(level, x, z);
            if (Math.abs(gy - cy) <= 4 && VolcanicFeatureUtil.isGround(level, x, gy, z, c.groundBlocks())) good++;
        }
        if (good < 10) return false;

        boolean lava = random.nextInt(100) < c.lavaChancePercent();
        int lavaY = cy + (int) Math.floor(height * c.lavaHeightRatio());
        double craterR = radius * c.craterRatio();
        long seed = random.nextLong();
        boolean wrote = false;
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                int x = cx + dx, z = cz + dz;
                double rough = VolcanicFeatureUtil.hash01(seed, x / 2, z / 2) * 2.0 - 1.0;
                double effectiveRadius = radius * (1 + 0.055 * rough);
                double d = Math.hypot(dx, dz);
                if (d >= effectiveRadius) continue;
                int naturalY = VolcanicFeatureUtil.groundY(level, x, z);
                if (Math.abs(naturalY - cy) > 6) continue;
                if (!VolcanicFeatureUtil.isGround(level, x, naturalY, z, c.groundBlocks())) continue;

                double n = d / effectiveRadius;
                boolean crater = d < craterR;
                double relativeHeight;
                if (crater) {
                    double t = d / craterR;
                    // Bowl floor 37% high; crater rim is at full cone height.
                    relativeHeight = height * (0.37 + 0.63 * t * t);
                } else {
                    double t = Mth.clamp((effectiveRadius - d) / (effectiveRadius - craterR), 0, 1);
                    relativeHeight = height * Math.pow(t, 1.27);
                }
                relativeHeight = Math.max(0, relativeHeight + rough * Math.min(1.5, height * 0.06)
                        * Math.sin(Math.PI * n));
                double blend = Math.pow(1 - n, 1.25);
                int foundation = (int) Math.round(Mth.lerp(blend, naturalY, cy));
                int top = foundation + (int) Math.ceil(relativeHeight);
                if (!crater) top = Math.max(top, naturalY);
                if (top < level.getMinBuildHeight() + 2 || top >= level.getMaxBuildHeight() - 2) continue;

                // Embed two layers into the island so there is no disk-shaped exposed base.
                for (int y = Math.min(naturalY, top) - 2; y <= top; y++) {
                    BlockPos at = new BlockPos(x, y, z);
                    var palette = y == top ? (crater ? c.craterFloor() :
                            (Math.abs(d - craterR) < 2 ? c.rim() : c.surface())) : c.core();
                    wrote |= VolcanicFeatureUtil.set(level, at, palette.pick(random), c.replaceableBlocks());
                }

                // Carve any previously-existing surface inside the crater, but do not destroy protected blocks.
                if (crater && naturalY > top) {
                    for (int y = top + 1; y <= naturalY; y++) {
                        BlockPos at = new BlockPos(x, y, z);
                        wrote |= VolcanicFeatureUtil.set(level, at, Blocks.AIR.defaultBlockState(), c.replaceableBlocks());
                    }
                }
                if (lava && crater && d <= craterR * 0.82 && top < lavaY) {
                    for (int y = top + 1; y <= lavaY; y++) {
                        wrote |= VolcanicFeatureUtil.set(level, new BlockPos(x, y, z),
                                c.lava(), c.replaceableBlocks());
                    }
                }
            }
        }
        return wrote;
    }
}
