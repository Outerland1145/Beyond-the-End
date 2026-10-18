package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** Scattered flames. JSON chooses the precise blockstate, including a modded End fire. */
public final class BurningFlameFeature extends Feature<VolcanicFeatureConfigs.Flames> {
    public BurningFlameFeature() { super(VolcanicFeatureConfigs.Flames.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<VolcanicFeatureConfigs.Flames> ctx) {
        var c = ctx.config();
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        if (!VolcanicFeatureUtil.writableFootprint(level, origin.getX(), origin.getY(), origin.getZ(), c.spread()))
            return false;
        boolean wrote = false;
        for (int i = 0; i < c.attempts(); i++) {
            if (random.nextInt(100) >= c.chancePercent()) continue;
            int x = origin.getX() + random.nextInt(c.spread() * 2 + 1) - c.spread();
            int z = origin.getZ() + random.nextInt(c.spread() * 2 + 1) - c.spread();
            int y = VolcanicFeatureUtil.groundY(level, x, z);
            if (!VolcanicFeatureUtil.isGround(level, x, y, z, c.groundBlocks())) continue;
            BlockPos flame = new BlockPos(x, y + 1, z);
            if (!VolcanicFeatureUtil.writable(level, flame) || !level.getBlockState(flame).isAir()) continue;
            if (random.nextInt(100) < c.supportConversionPercent()) {
                // For vanilla fire, convert the substrate into a permanent-fire support (e.g. magma).
                BlockPos base = flame.below();
                var support = c.support().pick(random);
                if (!level.getBlockState(base).equals(support)
                        && !VolcanicFeatureUtil.set(level, base, support, c.replaceableBlocks()))
                    continue;
            }
            if (!c.fire().canSurvive(level, flame)) continue;
            if (level.setBlock(flame, c.fire(), 2)) {
                // Natural Minecraft fire needs its next tick to age/spread/extinguish as normal.
                level.scheduleTick(flame, c.fire().getBlock(), 30 + random.nextInt(20));
                wrote = true;
            }
        }
        return wrote;
    }
}
