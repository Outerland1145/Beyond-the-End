package org.starrepublic.beyond_the_end.World.worldgen.structure.volcano;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.slf4j.Logger;
import org.starrepublic.beyond_the_end.Register.JigsawRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One giant bounding box, processed only within the current chunkBox.
 * All parameters, the exact resolved datapack palette, and all river nodes are
 * saved in the piece NBT. World reload and chunk order do not reroll anything.
 */
public final class LargeVolcanoPiece extends StructurePiece {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MATERIAL_SALT = 0x52DD5C844890A319L;

    private final int centerX, baseY, centerZ, radius, height;
    private final double craterRatio;
    private final boolean lavaLake;
    private final long seed;
    private final LargeVolcanoPalette palette;
    private final List<LargeVolcanoRivers.Node> rivers;
    private final LargeVolcanoGeometry geometry;
    private final double craterLavaHeightRatio, craterLavaFillRatio;
    private final int foundationExtensionRadius, foundationExtensionHeight;

    public LargeVolcanoPiece(int centerX, int baseY, int centerZ,
                             int radius, int height, double craterRatio,
                             boolean lavaLake, long seed,
                             LargeVolcanoPalette palette,
                             List<LargeVolcanoRivers.Node> rivers,
                             double craterLavaHeightRatio, double craterLavaFillRatio,
                             int foundationExtensionRadius, int foundationExtensionHeight) {
        super(JigsawRegister.LARGE_VOLCANO_PIECE.get(), 0,
                makeBox(centerX, baseY, centerZ, radius, height, foundationExtensionRadius));
        this.centerX = centerX;
        this.baseY = baseY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.height = height;
        this.craterRatio = craterRatio;
        this.lavaLake = lavaLake;
        this.seed = seed;
        this.palette = palette;
        this.rivers = List.copyOf(rivers);
        this.craterLavaHeightRatio = craterLavaHeightRatio;
        this.craterLavaFillRatio = craterLavaFillRatio;
        this.foundationExtensionRadius = foundationExtensionRadius;
        this.foundationExtensionHeight = foundationExtensionHeight;
        this.geometry = new LargeVolcanoGeometry(centerX, baseY, centerZ,
                radius, height, craterRatio, seed, craterLavaHeightRatio, craterLavaFillRatio);
    }

    /** Called by StructurePieceType.ContextlessType. */
    public LargeVolcanoPiece(CompoundTag nbt) {
        super(JigsawRegister.LARGE_VOLCANO_PIECE.get(), nbt);
        this.centerX = nbt.getInt("CenterX");
        this.baseY = nbt.getInt("BaseY");
        this.centerZ = nbt.getInt("CenterZ");
        this.radius = nbt.getInt("Radius");
        this.height = nbt.getInt("Height");
        this.craterRatio = nbt.getDouble("CraterRatio");
        this.lavaLake = nbt.getBoolean("LavaLake");
        this.seed = nbt.getLong("VolcanoSeed");
        this.palette = readPalette(nbt);
        this.rivers = readRivers(nbt.getIntArray("RiverNodes"));
        // Old saved pieces retain phase-2 behavior; never make unfinished old
        // volcanoes suddenly acquire a skirt or a higher lava lake.
        this.craterLavaHeightRatio = nbt.contains("CraterLavaHeightRatio")
                ? nbt.getDouble("CraterLavaHeightRatio") : 0.51;
        this.craterLavaFillRatio = nbt.contains("CraterLavaFillRatio")
                ? nbt.getDouble("CraterLavaFillRatio") : 0.68;
        this.foundationExtensionRadius = nbt.getInt("FoundationExtensionRadius");
        this.foundationExtensionHeight = nbt.getInt("FoundationExtensionHeight");
        this.geometry = new LargeVolcanoGeometry(centerX, baseY, centerZ,
                radius, height, craterRatio, seed, craterLavaHeightRatio, craterLavaFillRatio);
    }

    private static BoundingBox makeBox(int x, int y, int z, int radius, int height,
                                       int extensionRadius) {
        // The footprint includes the native-terrain skirt outside the mountain.
        // Max 15% radial edge noise, plus a safety margin.
        int horizontal = radius + (int) Math.ceil(radius * 0.16) + extensionRadius + 8;
        return new BoundingBox(x - horizontal, y - 64, z - horizontal,
                x + horizontal, y + (int) Math.ceil(height * 1.30) + 20,
                z + horizontal);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
        nbt.putInt("CenterX", centerX);
        nbt.putInt("BaseY", baseY);
        nbt.putInt("CenterZ", centerZ);
        nbt.putInt("Radius", radius);
        nbt.putInt("Height", height);
        nbt.putDouble("CraterRatio", craterRatio);
        nbt.putBoolean("LavaLake", lavaLake);
        nbt.putLong("VolcanoSeed", seed);
        nbt.putIntArray("RiverNodes", writeRivers(rivers));
        nbt.putDouble("CraterLavaHeightRatio", craterLavaHeightRatio);
        nbt.putDouble("CraterLavaFillRatio", craterLavaFillRatio);
        nbt.putInt("FoundationExtensionRadius", foundationExtensionRadius);
        nbt.putInt("FoundationExtensionHeight", foundationExtensionHeight);
        // Save resolved blockstates with this structure start. A later datapack
        // change does not half-repaint a volcano whose chunks are incomplete.
        Tag encoded = LargeVolcanoPalette.CODEC.encodeStart(NbtOps.INSTANCE, palette)
                .result().orElseThrow(() -> new IllegalStateException("Unable to encode volcano palette"));
        nbt.put("VolcanoPalette", encoded);
    }

    private static LargeVolcanoPalette readPalette(CompoundTag nbt) {
        if (!nbt.contains("VolcanoPalette", Tag.TAG_COMPOUND)) return LargeVolcanoPalette.DEFAULT;
        return LargeVolcanoPalette.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("VolcanoPalette"))
                .resultOrPartial(s -> LOGGER.error("Invalid stored volcano palette: {}", s))
                .orElse(LargeVolcanoPalette.DEFAULT);
    }

    private static int[] writeRivers(List<LargeVolcanoRivers.Node> nodes) {
        int[] packed = new int[nodes.size() * 4];
        for (int i = 0; i < nodes.size(); i++) {
            LargeVolcanoRivers.Node n = nodes.get(i);
            packed[4 * i] = n.x();
            packed[4 * i + 1] = n.z();
            packed[4 * i + 2] = n.lavaY();
            packed[4 * i + 3] = n.width();
        }
        return packed;
    }

    private static List<LargeVolcanoRivers.Node> readRivers(int[] packed) {
        if (packed.length % 4 != 0) {
            LOGGER.warn("Invalid volcano river node array length {}", packed.length);
            return List.of();
        }
        List<LargeVolcanoRivers.Node> result = new ArrayList<>(packed.length / 4);
        for (int i = 0; i < packed.length; i += 4) {
            result.add(new LargeVolcanoRivers.Node(
                    packed[i], packed[i + 1], packed[i + 2], packed[i + 3]));
        }
        return List.copyOf(result);
    }

    private record RiverOverlay(boolean channel, int lavaY, double distanceScore) {}

    /** Pre-rasterize only the cells intersecting this chunk; no neighbor reads. */
    private Map<Long, RiverOverlay> localRivers(BoundingBox box) {
        Map<Long, RiverOverlay> result = new HashMap<>();
        for (LargeVolcanoRivers.Node node : rivers) {
            int reach = node.width() + 2;
            if (node.x() + reach < box.minX() || node.x() - reach > box.maxX()
                    || node.z() + reach < box.minZ() || node.z() - reach > box.maxZ()) continue;
            int x0 = Math.max(box.minX(), node.x() - reach);
            int x1 = Math.min(box.maxX(), node.x() + reach);
            int z0 = Math.max(box.minZ(), node.z() - reach);
            int z1 = Math.min(box.maxZ(), node.z() + reach);
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    double dist = Math.hypot(x - node.x(), z - node.z());
                    if (dist > node.width() + 1.25) continue;
                    boolean water = dist <= node.width() - 0.15;
                    double score = dist / Math.max(1, node.width());
                    long key = columnKey(x, z);
                    RiverOverlay old = result.get(key);
                    // Channel wins over banks; closest node wins within a role.
                    if (old == null || (water && !old.channel())
                            || (water == old.channel() && score < old.distanceScore())) {
                        result.put(key, new RiverOverlay(water, node.lavaY(), score));
                    }
                }
            }
        }
        return result;
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator chunkGenerator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        // No reads/writes outside the active chunkBox: fluid ticks are queued,
        // not executed synchronously, so this is safe during world generation.
        int maxFootprint = radius + (int) Math.ceil(radius * 0.16)
                + foundationExtensionRadius + 8;
        int minX = Math.max(chunkBox.minX(), centerX - maxFootprint);
        int maxX = Math.min(chunkBox.maxX(), centerX + maxFootprint);
        int minZ = Math.max(chunkBox.minZ(), centerZ - maxFootprint);
        int maxZ = Math.min(chunkBox.maxZ(), centerZ + maxFootprint);
        if (minX > maxX || minZ > maxZ) return;

        BlockState nativeFiller = findNativeFiller(level, chunkGenerator);
        Map<Long, RiverOverlay> localRivers = localRivers(chunkBox);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double normalized = geometry.normalizedRadiusAt(x, z);
                // The skirt starts near the end of the volcano and fades onto
                // the unmodified natural surface beyond its irregular rim.
                if (normalized > 1.0 + (double) (foundationExtensionRadius + 2) / radius)
                    continue;

                int naturalTop = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (naturalTop <= level.getMinBuildHeight() + 2) {
                    // Do not produce unsupported bridges over the End void.
                    continue;
                }

                LargeVolcanoGeometry.Shape shape = geometry.shape(x, z);
                int skirtRise = transitionRise(x, z, normalized);

                if (!shape.inside()) {
                    // Outside of the volcano, an irregular lip of dimension-
                    // native terrain gradually tapers to the unchanged island.
                    if (skirtRise <= 0) continue;
                    int top = Math.min(level.getMaxBuildHeight() - 1, naturalTop + skirtRise);
                    int embed = Math.min(12, 4 + skirtRise * 2);
                    for (int y = Math.max(chunkBox.minY(), naturalTop - embed);
                         y <= Math.min(top, chunkBox.maxY()); y++) {
                        put(level, pos, chunkBox, x, y, z, nativeFiller);
                    }
                    continue;
                }

                int top = Math.min(level.getMaxBuildHeight() - 1,
                        geometry.top(x, z, naturalTop, shape) + skirtRise);
                int embed = 4 + (int) Math.round(8 * (1 - shape.normalizedDistance()));
                int bottom = Math.max(level.getMinBuildHeight(), Math.min(naturalTop, top) - embed);

                // Body and its dimension-native underfill. In the outer 25% of
                // the mountain, native rock gradually mixes into volcanic rock.
                for (int y = Math.max(bottom, chunkBox.minY());
                     y <= Math.min(top, chunkBox.maxY()); y++) {
                    BlockState state = chooseBody(x, y, z, top - y, shape);
                    if (normalized > 0.75) {
                        double nativeChance = smoothStep((normalized - 0.75) / 0.25);
                        // More native material below the skin, and near the toe.
                        if (y <= naturalTop) nativeChance = Math.max(nativeChance, 0.65);
                        if (geometry.random01(x, y, z, 0x329B13DFF0A8C23DL) < nativeChance)
                            state = nativeFiller;
                    }
                    put(level, pos, chunkBox, x, y, z, state);
                }

                // Crater hollow and wide/high lava lake. Physical lava is only
                // placed where the terrain is BELOW the lake surface.
                if (shape.craterInterior()) {
                    int cavityTop = Math.min(level.getMaxBuildHeight() - 1,
                            baseY + (int) Math.ceil(height * 1.30) + 12);
                    clear(level, pos, chunkBox, x, z, top + 1, cavityTop);
                    if (lavaLake && geometry.isCraterLakeColumn(x, z, shape)) {
                        int lakeTop = Math.min(geometry.craterLavaY(), chunkBox.maxY());
                        for (int y = Math.max(top + 1, chunkBox.minY()); y <= lakeTop; y++) {
                            if (y == lakeTop) {
                                // Only the exposed surface needs a wake-up
                                // tick; avoid thousands of needless ticks in
                                // the solid body of a deep lava lake.
                                putFluid(level, pos, chunkBox, x, y, z, palette.lava());
                            } else {
                                put(level, pos, chunkBox, x, y, z, palette.lava());
                            }
                        }
                    }
                }

                // Carve the preplanned river only after the mountain exists.
                RiverOverlay overlay = localRivers.get(columnKey(x, z));
                if (overlay != null && !shape.craterInterior()) {
                    if (overlay.channel()) {
                        int fluidLevel = Math.min(overlay.lavaY(), top - 1);
                        if (top - fluidLevel <= 9) {
                            clear(level, pos, chunkBox, x, z, fluidLevel + 1, top + 2);
                            put(level, pos, chunkBox, x, fluidLevel - 1, z,
                                    palette.riverBed().pick(seed, x, fluidLevel - 1, z, MATERIAL_SALT));
                            put(level, pos, chunkBox, x, fluidLevel - 2, z,
                                    palette.riverBed().pick(seed, x, fluidLevel - 2, z, MATERIAL_SALT));
                            // Explicit tick: setBlock(...,2) alone doesn't
                            // activate a stationary structure-generated fluid.
                            putFluid(level, pos, chunkBox, x, fluidLevel, z, palette.lava());
                        }
                    } else {
                        for (int y = top - 2; y <= top; y++) {
                            put(level, pos, chunkBox, x, y, z,
                                    palette.riverBank().pick(seed, x, y, z, MATERIAL_SALT));
                        }
                    }
                }
            }
        }
    }

    private int transitionRise(int x, int z, double normalized) {
        if (foundationExtensionRadius <= 0 || foundationExtensionHeight <= 0) return 0;
        double strength;
        if (normalized <= 1.0) {
            // Start lifting the terrain near the mountain toe.
            strength = smoothStep((normalized - 0.72) / 0.28);
        } else {
            // Fade back down to original island height over the extension.
            double outwardBlocks = (normalized - 1.0) * radius;
            double noisyExtent = foundationExtensionRadius
                    * (1.0 + geometry.transitionNoise(x, z) * 0.18);
            strength = smoothStep(1.0 - outwardBlocks / Math.max(1.0, noisyExtent));
        }
        return Math.max(0, (int) Math.round(foundationExtensionHeight * strength));
    }

    private static double smoothStep(double v) {
        v = Math.max(0.0, Math.min(1.0, v));
        return v * v * (3.0 - 2.0 * v);
    }

    private BlockState findNativeFiller(WorldGenLevel level, ChunkGenerator generator) {
        if (palette.foundationBlock().isPresent()) return palette.foundationBlock().get();
        if (generator instanceof NoiseBasedChunkGenerator noise)
            return noise.generatorSettings().value().defaultBlock();
        // A non-noise chunk generator has no universal defaultBlock API;
        // prefer a datapack "foundation_block" override for such dimensions.
        if (level.getLevel().dimension() == Level.END) return Blocks.END_STONE.defaultBlockState();
        if (level.getLevel().dimension() == Level.NETHER) return Blocks.NETHERRACK.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    /** Schedule the actual fluid (including modded fluids) in the generated
     *  chunk. Using an immutable position is important: pos is mutable. */
    private static void putFluid(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                                 BoundingBox box, int x, int y, int z, BlockState state) {
        put(level, pos, box, x, y, z, state);
        if (x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ()
                || y < box.minY() || y > box.maxY()) return;
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return;
        pos.set(x, y, z);
        var placedFluid = level.getFluidState(pos);
        if (!placedFluid.isEmpty()) {
            Fluid fluid = placedFluid.getType();
            level.scheduleTick(pos.immutable(), fluid, Math.max(1, fluid.getTickDelay(level)));
        }
    }

    private BlockState chooseBody(int x, int y, int z, int depth,
                                  LargeVolcanoGeometry.Shape shape) {
        LargeVolcanoPalette.WeightedPalette group;
        if (depth > 4) group = palette.interior();
        else if (shape.craterInterior()) group = palette.crater();
        else if (shape.rimHeight() > height * 0.065) group = palette.rim();
        else group = palette.surface();
        return group.pick(seed, x, y, z, MATERIAL_SALT);
    }

    private static void clear(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                              BoundingBox box, int x, int z, int minY, int maxY) {
        for (int y = Math.max(box.minY(), minY);
             y <= Math.min(box.maxY(), maxY); y++) {
            put(level, pos, box, x, y, z, Blocks.AIR.defaultBlockState());
        }
    }

    private static void put(WorldGenLevel level, BlockPos.MutableBlockPos pos,
                            BoundingBox box, int x, int y, int z, BlockState state) {
        if (x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ()
                || y < box.minY() || y > box.maxY()
                || y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return;
        pos.set(x, y, z);
        if (level.getBlockState(pos).is(Blocks.BEDROCK)) return;
        level.setBlock(pos, state, 2);
    }
}
