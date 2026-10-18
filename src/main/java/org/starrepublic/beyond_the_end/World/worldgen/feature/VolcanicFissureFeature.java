package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shallow jagged thermal fracture. Entire path stays in the feature write region. */
public final class VolcanicFissureFeature extends Feature<VolcanicFeatureConfigs.Fissure> {
    public VolcanicFissureFeature() { super(VolcanicFeatureConfigs.Fissure.CODEC); }
    private record Column(int x, int y, int z, boolean inner) {}

    @Override
    public boolean place(FeaturePlaceContext<VolcanicFeatureConfigs.Fissure> ctx) {
        var c = ctx.config();
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        int length = VolcanicFeatureUtil.between(random, c.minLength(), c.maxLength());
        double theta = random.nextDouble() * Math.PI * 2;
        double x = ctx.origin().getX();
        double z = ctx.origin().getZ();
        Map<Long, Column> planned = new LinkedHashMap<>();

        // Plan and validate BEFORE any writes. No partial cut if the fracture leaves the region.
        for (int i = 0; i < length; i++) {
            int cx = (int) Math.round(x);
            int cz = (int) Math.round(z);
            int width = 1 + random.nextInt(c.maxWidth());
            for (int ox = -width - 1; ox <= width + 1; ox++) {
                for (int oz = -width - 1; oz <= width + 1; oz++) {
                    if (Math.hypot(ox, oz) > width + 0.6) continue;
                    int px = cx + ox, pz = cz + oz;
                    if (!VolcanicFeatureUtil.writable(level, new BlockPos(px, ctx.origin().getY(), pz)))
                        return false;
                    long key = BlockPos.asLong(px, 0, pz);
                    boolean inner = Math.hypot(ox, oz) <= width + 0.1;
                    Column old = planned.get(key);
                    if (old != null) {
                        // Another section of the line can upgrade a bank cell to trench interior.
                        if (inner && !old.inner()) planned.put(key, new Column(px, old.y(), pz, true));
                        continue;
                    }
                    int y = VolcanicFeatureUtil.groundY(level, px, pz);
                    if (Math.abs(y - ctx.origin().getY()) > 6) continue;
                    if (!VolcanicFeatureUtil.isGround(level, px, y, pz, c.groundBlocks())) continue;
                    planned.put(key, new Column(px, y, pz, inner));
                }
            }
            theta += (random.nextDouble() - 0.5) * 0.45;
            x += Math.cos(theta);
            z += Math.sin(theta);
        }
        boolean lava = random.nextInt(100) < c.lavaChancePercent();
        if (planned.size() < 6) return false;
        boolean wrote = false;
        // Place the complete planned network without recomputing heightmaps after carving.
        for (Column column : planned.values()) {
            if (!column.inner()) {
                BlockPos bank = new BlockPos(column.x(), column.y(), column.z());
                if (VolcanicFeatureUtil.replaceable(level, bank, c.replaceableBlocks()))
                    wrote |= VolcanicFeatureUtil.set(level, bank, c.banks().pick(random), c.replaceableBlocks());
                continue;
            }
            int depth = 1 + random.nextInt(c.maxDepth());
            BlockPos floor = new BlockPos(column.x(), column.y() - depth, column.z());
            if (!VolcanicFeatureUtil.replaceable(level, floor, c.replaceableBlocks())) continue;
            // First line the bottom, then clear overburden only if all states are safe to replace.
            boolean safe = true;
            for (int y = floor.getY() + 1; y <= column.y(); y++) {
                if (!VolcanicFeatureUtil.replaceable(level, new BlockPos(column.x(), y, column.z()), c.replaceableBlocks())) {
                    safe = false;
                    break;
                }
            }
            if (!safe) continue;
            wrote |= VolcanicFeatureUtil.set(level, floor, c.bed().pick(random), c.replaceableBlocks());
            for (int y = floor.getY() + 1; y <= column.y(); y++) {
                BlockPos at = new BlockPos(column.x(), y, column.z());
                boolean low = y < column.y();
                wrote |= VolcanicFeatureUtil.set(level, at,
                        lava && low ? c.lava() : Blocks.AIR.defaultBlockState(), c.replaceableBlocks());
            }

        }
        return wrote;
    }
}
