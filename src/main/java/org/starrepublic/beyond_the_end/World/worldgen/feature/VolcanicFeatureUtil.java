package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/** Shared, bounded worldgen helpers. All writes are confined to the writable region. */
public final class VolcanicFeatureUtil {
    public static final Codec<List<ResourceLocation>> BLOCK_IDS = ResourceLocation.CODEC.listOf();
    private VolcanicFeatureUtil() {}

    public static boolean hasId(BlockState state, List<ResourceLocation> allowed) {
        return allowed.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static boolean writable(WorldGenLevel level, BlockPos pos) {
        return pos.getY() >= level.getMinBuildHeight()
                && pos.getY() < level.getMaxBuildHeight()
                && level.ensureCanWrite(pos);
    }

    /** Check the entire horizontal footprint before starting an atomic-looking terrain feature. */
    public static boolean writableFootprint(WorldGenLevel level, int cx, int y, int cz, int r) {
        return writable(level, new BlockPos(cx - r, y, cz - r))
                && writable(level, new BlockPos(cx + r, y, cz - r))
                && writable(level, new BlockPos(cx - r, y, cz + r))
                && writable(level, new BlockPos(cx + r, y, cz + r));
    }

    public static int groundY(WorldGenLevel level, int x, int z) {
        // Avoid detecting plants as the actual ground. Never call for a point outside the write region.
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    public static boolean isGround(WorldGenLevel level, int x, int y, int z,
                                   List<ResourceLocation> groundIds) {
        BlockPos pos = new BlockPos(x, y, z);
        return writable(level, pos) && hasId(level.getBlockState(pos), groundIds)
                && level.getBlockState(pos.above()).isAir();
    }

    public static boolean replaceable(WorldGenLevel level, BlockPos pos,
                                      List<ResourceLocation> replaceableIds) {
        if (!writable(level, pos)) return false;
        BlockState before = level.getBlockState(pos);
        return before.isAir() || hasId(before, replaceableIds);
    }

    public static boolean set(WorldGenLevel level, BlockPos pos, BlockState next,
                              List<ResourceLocation> replaceableIds) {
        if (!replaceable(level, pos, replaceableIds)) return false;
        if (!next.canSurvive(level, pos)) return false;
        boolean changed = level.setBlock(pos, next, 2);
        if (changed && !next.getFluidState().isEmpty()) {
            // Features don't automatically wake fluids placed with flag 2.
            level.scheduleTick(pos.immutable(), next.getFluidState().getType(),
                    Math.max(1, next.getFluidState().getType().getTickDelay(level)));
        }
        return changed;
    }

    public static double hash01(long seed, int x, int z) {
        long h = seed ^ ((long) x * 0x9E3779B97F4A7C15L) ^ ((long) z * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    public static int between(net.minecraft.util.RandomSource r, int min, int max) {
        return min + r.nextInt(max - min + 1);
    }
}
