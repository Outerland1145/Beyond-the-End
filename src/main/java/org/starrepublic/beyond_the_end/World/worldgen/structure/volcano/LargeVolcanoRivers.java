package org.starrepublic.beyond_the_end.World.worldgen.structure.volcano;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plans 2..5 downhill channels BEFORE the structure is placed.
 *
 * Only ChunkGenerator#getFirstFreeHeight is queried. No getChunk / WorldGenLevel
 * access and no writes across chunks. Store all planned nodes in the Piece NBT.
 * A channel step samples eight neighbors and prefers the lowest forward point.
 * Minor rises are carved away (max 5 blocks), with monotonically descending
 * lava levels. Major uphill obstacles terminate a branch rather than forcing
 * an impossible floating or ascending stream.
 */
public final class LargeVolcanoRivers {
    private LargeVolcanoRivers() {}

    public record Node(int x, int z, int lavaY, int width) {}

    private record Candidate(int x, int z, int y, int width, double score) {}

    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DZ = {-1, -1, -1, 0, 0, 1, 1, 1};

    public static List<Node> plan(Structure.GenerationContext context,
                                  LargeVolcanoGeometry geometry, long seed,
                                  double craterRatio, int minRivers, int maxRivers,
                                  int maxCutDepth) {
        RandomSource random = RandomSource.create(seed ^ 0x55662C1185A94E17L);
        int expected = minRivers + random.nextInt(maxRivers - minRivers + 1);
        Map<Long, Integer> terrainCache = new HashMap<>();
        List<List<Node>> paths = new ArrayList<>();
        double firstAngle = random.nextDouble() * 2 * Math.PI;
        int mainCount = Math.min(expected, 2 + random.nextInt(2)); // 2..3 main channels

        for (int i = 0; i < mainCount; i++) {
            double angle = firstAngle + (2 * Math.PI * i / mainCount)
                    + (random.nextDouble() - 0.5) * 0.35;
            int startX = geometry.centerX() + (int) Math.round(Math.cos(angle)
                    * geometry.radius() * craterRatio * 1.37);
            int startZ = geometry.centerZ() + (int) Math.round(Math.sin(angle)
                    * geometry.radius() * craterRatio * 1.37);
            int startTop = modelTop(context, geometry, terrainCache, startX, startZ);
            if (startTop == Integer.MIN_VALUE) continue;
            Node source = new Node(startX, startZ, startTop - 1, 2 + random.nextInt(2));
            List<Node> path = walk(context, geometry, terrainCache, random, source, 0.0, maxCutDepth);
            if (path.size() >= 10) paths.add(path);
        }

        // Branch from an existing river at 35..65% of its length.
        // This makes real Y-connected tributaries, not decorative random lines.
        int attempts = 0;
        while (paths.size() < expected && attempts++ < expected * 5 && !paths.isEmpty()) {
            List<Node> parent = paths.get(random.nextInt(Math.min(mainCount, paths.size())));
            if (parent.size() < 18) continue;
            int index = (int) (parent.size() * (0.35 + random.nextDouble() * 0.30));
            Node origin = parent.get(Math.min(index, parent.size() - 1));
            // +- ~35 to 55 degrees from the outward radial direction
            double turn = (random.nextBoolean() ? 1 : -1) * (0.60 + random.nextDouble() * 0.35);
            List<Node> branch = walk(context, geometry, terrainCache, random,
                    new Node(origin.x(), origin.z(), origin.lavaY(), 1 + random.nextInt(2)),
                    turn, maxCutDepth);
            if (branch.size() >= 9) paths.add(branch);
        }

        List<Node> result = new ArrayList<>();
        for (List<Node> path : paths) result.addAll(path);
        return List.copyOf(result);
    }

    private static List<Node> walk(Structure.GenerationContext context,
                                   LargeVolcanoGeometry geometry,
                                   Map<Long, Integer> cache,
                                   RandomSource random,
                                   Node source, double angleBias,
                                   int maxCutDepth) {
        List<Node> path = new ArrayList<>();
        path.add(source);
        Node current = source;
        int maxSteps = 2 * geometry.radius() + 48;
        for (int step = 0; step < maxSteps; step++) {
            double dxCenter = (double) current.x() - geometry.centerX();
            double dzCenter = (double) current.z() - geometry.centerZ();
            double dist = Math.sqrt(dxCenter * dxCenter + dzCenter * dzCenter);
            if (dist >= geometry.radius() * 0.95) break;
            double radialAngle = Math.atan2(dzCenter, dxCenter);
            // Branches fan away at the beginning, then naturally turn downhill.
            double steering = radialAngle + angleBias * Math.max(0, 1 - step / 26.0);
            Candidate best = null;
            for (int i = 0; i < 8; i++) {
                int x = current.x() + DX[i], z = current.z() + DZ[i];
                double nextDistance = Math.hypot((double) x - geometry.centerX(), (double) z - geometry.centerZ());
                if (nextDistance <= dist + 0.15) continue; // outward-progress-only: no loops
                LargeVolcanoGeometry.Shape shape = geometry.shape(x, z);
                if (!shape.inside() || shape.craterInterior()) continue;
                int originalTop = modelTop(context, geometry, cache, x, z);
                if (originalTop == Integer.MIN_VALUE) continue;
                int lavaY = Math.min(current.lavaY(), originalTop - 1);
                int requiredCut = originalTop - lavaY;
                if (requiredCut > maxCutDepth) continue;
                double direction = Math.atan2(DZ[i], DX[i]);
                double alignment = Math.cos(direction - steering);
                int drop = current.lavaY() - lavaY;
                double score = (alignment * 3.8)
                        - requiredCut * 0.9
                        + Math.min(4, drop) * 0.65
                        - Math.max(0, drop - 5) * 1.5
                        + random.nextDouble() * 1.0;
                if (best == null || score > best.score()) {
                    int width = Math.max(1, Math.min(3,
                            current.width() + random.nextInt(3) - 1));
                    best = new Candidate(x, z, lavaY, width, score);
                }
            }
            if (best == null) break;
            current = new Node(best.x(), best.z(), best.y(), best.width());
            path.add(current);
        }
        return path;
    }

    private static int modelTop(Structure.GenerationContext context,
                                LargeVolcanoGeometry geometry,
                                Map<Long, Integer> cache,
                                int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        Integer existing = cache.get(key);
        if (existing != null) return existing;
        int natural = context.chunkGenerator().getFirstFreeHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState()) - 1;
        int result = natural <= context.heightAccessor().getMinBuildHeight() + 2
                ? Integer.MIN_VALUE : geometry.top(x, z, natural, geometry.shape(x, z));
        cache.put(key, result);
        return result;
    }
}
