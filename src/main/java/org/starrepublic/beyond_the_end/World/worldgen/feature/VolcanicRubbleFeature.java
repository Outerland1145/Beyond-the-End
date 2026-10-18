package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** Irregular scorched-rock patch plus occasional embedded rubble ridges. */
public final class VolcanicRubbleFeature extends Feature<VolcanicFeatureConfigs.Rubble> {
    public VolcanicRubbleFeature() { super(VolcanicFeatureConfigs.Rubble.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<VolcanicFeatureConfigs.Rubble> ctx) {
        var c = ctx.config();
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        int radius = VolcanicFeatureUtil.between(random, c.minRadius(), c.maxRadius());
        if (!VolcanicFeatureUtil.writableFootprint(level, origin.getX(), origin.getY(), origin.getZ(), radius + 1))
            return false;
        long shapeSeed = random.nextLong();
        boolean wrote = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                double noise = VolcanicFeatureUtil.hash01(shapeSeed, x / 2, z / 2);
                double dist = Math.hypot(dx, dz) / (radius * (0.80 + 0.30 * noise));
                if (dist > 1.0 || random.nextDouble() < dist * 0.21) continue;
                int y = VolcanicFeatureUtil.groundY(level, x, z);
                if (!VolcanicFeatureUtil.isGround(level, x, y, z, c.groundBlocks())) continue;
                if (Math.abs(y - origin.getY()) > 6) continue;
                int depth = 1 + random.nextInt(c.maxDepth());
                for (int d = 0; d < depth; d++) {
                    BlockPos at = new BlockPos(x, y - d, z);
                    if (!VolcanicFeatureUtil.replaceable(level, at, c.replaceableBlocks())) break;
                    wrote |= VolcanicFeatureUtil.set(level, at, c.surface().pick(random), c.replaceableBlocks());
                }
                if (random.nextInt(100) < c.boulderChance() && dist < 0.83) {
                    int moundHeight = 1 + random.nextInt(2);
                    for (int h = 1; h <= moundHeight; h++) {
                        BlockPos at = new BlockPos(x, y + h, z);
                        if (!level.getBlockState(at).isAir()) break;
                        wrote |= VolcanicFeatureUtil.set(level, at, c.boulder().pick(random), c.replaceableBlocks());
                    }
                }
            }
        }
        return wrote;
    }
}
