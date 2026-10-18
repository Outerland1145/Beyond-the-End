package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** Broken, crooked groups of columns (not a copy of vanilla basalt columns). */
public final class VolcanicColumnsFeature extends Feature<VolcanicFeatureConfigs.Columns> {
    public VolcanicColumnsFeature() { super(VolcanicFeatureConfigs.Columns.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<VolcanicFeatureConfigs.Columns> ctx) {
        var c = ctx.config();
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        int r = c.spread() + c.maxWidth() + 1;
        if (!VolcanicFeatureUtil.writableFootprint(level, origin.getX(), origin.getY(), origin.getZ(), r))
            return false;
        boolean wrote = false;
        for (int pillar = 0; pillar < c.count(); pillar++) {
            int px = origin.getX() + random.nextInt(c.spread() * 2 + 1) - c.spread();
            int pz = origin.getZ() + random.nextInt(c.spread() * 2 + 1) - c.spread();
            int ground = VolcanicFeatureUtil.groundY(level, px, pz);
            if (!VolcanicFeatureUtil.isGround(level, px, ground, pz, c.groundBlocks())) continue;
            if (Math.abs(ground - origin.getY()) > 10) continue;

            int height = VolcanicFeatureUtil.between(random, c.minHeight(), c.maxHeight());
            int baseWidth = 1 + random.nextInt(c.maxWidth());
            double leanX = (random.nextDouble() - 0.5) * 1.6;
            double leanZ = (random.nextDouble() - 0.5) * 1.6;
            for (int dy = 0; dy < height; dy++) {
                double t = dy / (double) height;
                int midX = px + (int) Math.round(leanX * t);
                int midZ = pz + (int) Math.round(leanZ * t);
                double layerRadius = Math.max(0.55, baseWidth * (1.0 - 0.60 * t));
                for (int ox = -baseWidth; ox <= baseWidth; ox++) {
                    for (int oz = -baseWidth; oz <= baseWidth; oz++) {
                        double jitter = VolcanicFeatureUtil.hash01(level.getSeed() ^ (long) pillar * 13L,
                                midX + ox + dy * 23, midZ + oz - dy * 17);
                        if (Math.hypot(ox, oz) > layerRadius + (jitter - 0.5) * 0.25) continue;
                        BlockPos at = new BlockPos(midX + ox, ground + dy, midZ + oz);
                        var palette = (Math.hypot(ox, oz) >= layerRadius - 0.85 || dy >= height - 2)
                                ? c.crust() : c.core();
                        wrote |= VolcanicFeatureUtil.set(level, at, palette.pick(random), c.replaceableBlocks());
                    }
                }
            }
        }
        return wrote;
    }
}
