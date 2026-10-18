package org.starrepublic.beyond_the_end.World.worldgen.structure.volcano;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.starrepublic.beyond_the_end.Register.JigsawRegister;

import java.util.List;
import java.util.Optional;

/**
 * Phase 2: plans rivers once at STRUCTURE_STARTS, then adds a single large
 * chunk-clipped StructurePiece. Does not load neighboring chunks.
 */
public final class LargeVolcanoStructure extends Structure {
    public static final MapCodec<LargeVolcanoStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            LargeVolcanoStructure.settingsCodec(i),
            Codec.intRange(32, 192).fieldOf("min_radius").forGetter(v -> v.minRadius),
            Codec.intRange(32, 192).fieldOf("max_radius").forGetter(v -> v.maxRadius),
            Codec.intRange(20, 160).fieldOf("min_height").forGetter(v -> v.minHeight),
            Codec.intRange(20, 160).fieldOf("max_height").forGetter(v -> v.maxHeight),
            Codec.doubleRange(0.12, 0.40).optionalFieldOf("min_crater_ratio", 0.20).forGetter(v -> v.minCraterRatio),
            Codec.doubleRange(0.12, 0.40).optionalFieldOf("max_crater_ratio", 0.28).forGetter(v -> v.maxCraterRatio),
            Codec.doubleRange(0, 1).optionalFieldOf("lava_lake_chance", 0.75).forGetter(v -> v.lavaLakeChance),
            Codec.intRange(2, 5).optionalFieldOf("min_rivers", 2).forGetter(v -> v.minRivers),
            Codec.intRange(2, 5).optionalFieldOf("max_rivers", 5).forGetter(v -> v.maxRivers),
            Codec.intRange(2, 10).optionalFieldOf("river_max_cut_depth", 6).forGetter(v -> v.riverMaxCutDepth),
            LargeVolcanoPalette.CODEC.fieldOf("palette").forGetter(v -> v.palette),
            Codec.doubleRange(0.20, 0.95).optionalFieldOf("crater_lava_height_ratio", 0.78)
                    .forGetter(v -> v.craterLavaHeightRatio),
            Codec.doubleRange(0.1, 0.96).optionalFieldOf("crater_lava_fill_ratio", 0.96)
                    .forGetter(v -> v.craterLavaFillRatio),
            Codec.intRange(0, 48).optionalFieldOf("foundation_extension_radius", 22)
                    .forGetter(v -> v.foundationExtensionRadius),
            Codec.intRange(0, 12).optionalFieldOf("foundation_extension_height", 5)
                    .forGetter(v -> v.foundationExtensionHeight)
    ).apply(i, LargeVolcanoStructure::new));

    private final int minRadius, maxRadius, minHeight, maxHeight;
    private final double minCraterRatio, maxCraterRatio, lavaLakeChance;
    private final int minRivers, maxRivers, riverMaxCutDepth;
    private final LargeVolcanoPalette palette;
    private final double craterLavaHeightRatio, craterLavaFillRatio;
    private final int foundationExtensionRadius, foundationExtensionHeight;

    public LargeVolcanoStructure(StructureSettings settings,
                                 int minRadius, int maxRadius,
                                 int minHeight, int maxHeight,
                                 double minCraterRatio, double maxCraterRatio,
                                 double lavaLakeChance,
                                 int minRivers, int maxRivers, int riverMaxCutDepth,
                                 LargeVolcanoPalette palette,
                                 double craterLavaHeightRatio, double craterLavaFillRatio,
                                 int foundationExtensionRadius, int foundationExtensionHeight) {
        super(settings);
        if (minRadius > maxRadius || minHeight > maxHeight
                || minCraterRatio > maxCraterRatio || minRivers > maxRivers) {
            throw new IllegalArgumentException("Invalid large volcano min/max configuration");
        }
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minCraterRatio = minCraterRatio;
        this.maxCraterRatio = maxCraterRatio;
        this.lavaLakeChance = lavaLakeChance;
        this.minRivers = minRivers;
        this.maxRivers = maxRivers;
        this.riverMaxCutDepth = riverMaxCutDepth;
        this.palette = palette;
        this.craterLavaHeightRatio = craterLavaHeightRatio;
        this.craterLavaFillRatio = craterLavaFillRatio;
        this.foundationExtensionRadius = foundationExtensionRadius;
        this.foundationExtensionHeight = foundationExtensionHeight;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        RandomSource random = context.random();
        int radius = between(random, minRadius, maxRadius);
        int height = between(random, minHeight, maxHeight);
        double craterRatio = minCraterRatio
                + random.nextDouble() * (maxCraterRatio - minCraterRatio);
        boolean lake = random.nextDouble() < lavaLakeChance;
        int centerX = context.chunkPos().getMiddleBlockX();
        int centerZ = context.chunkPos().getMiddleBlockZ();
        int baseY = surface(context, centerX, centerZ);
        int minY = context.heightAccessor().getMinBuildHeight();
        int maxY = context.heightAccessor().getMaxBuildHeight();
        if (baseY < minY + 5 || baseY + Math.ceil(height * 1.30) + 20 >= maxY) {
            return Optional.empty();
        }
        if (!validTerrain(context, centerX, baseY, centerZ, radius)
                || !validBiomes(context, centerX, baseY, centerZ, radius)) {
            return Optional.empty();
        }

        long volcanoSeed = LargeVolcanoGeometry.mix64(
                context.seed() ^ context.chunkPos().toLong() ^ random.nextLong());
        LargeVolcanoGeometry model = new LargeVolcanoGeometry(
                centerX, baseY, centerZ, radius, height, craterRatio, volcanoSeed,
                craterLavaHeightRatio, craterLavaFillRatio);
        List<LargeVolcanoRivers.Node> rivers = LargeVolcanoRivers.plan(
                context, model, volcanoSeed, craterRatio,
                minRivers, maxRivers, riverMaxCutDepth);
        LargeVolcanoPiece piece = new LargeVolcanoPiece(
                centerX, baseY, centerZ, radius, height, craterRatio,
                lake, volcanoSeed, palette, rivers,
                craterLavaHeightRatio, craterLavaFillRatio,
                foundationExtensionRadius, foundationExtensionHeight);
        return Optional.of(new GenerationStub(new BlockPos(centerX, baseY, centerZ),
                builder -> builder.addPiece(piece)));
    }

    private static boolean validTerrain(GenerationContext ctx, int cx, int cy, int cz, int radius) {
        // Look at the island well inside its footprint, not neighboring chunks.
        int valid = 1;
        int min = cy, max = cy;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2 / 16;
            int x = cx + (int) Math.round(Math.cos(angle) * radius * 0.62);
            int z = cz + (int) Math.round(Math.sin(angle) * radius * 0.62);
            int y = surface(ctx, x, z);
            if (y <= ctx.heightAccessor().getMinBuildHeight() + 4 || Math.abs(y - cy) > 32) continue;
            valid++;
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        return valid >= 13 && max - min <= 36;
    }

    private static boolean validBiomes(GenerationContext ctx, int cx, int cy, int cz, int radius) {
        int valid = isValidBiome(ctx, cx, cy, cz) ? 1 : 0;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2 / 16;
            int x = cx + (int) Math.round(Math.cos(angle) * radius * 0.55);
            int z = cz + (int) Math.round(Math.sin(angle) * radius * 0.55);
            if (isValidBiome(ctx, x, cy, z)) valid++;
        }
        return valid >= 12;
    }

    private static boolean isValidBiome(GenerationContext ctx, int x, int y, int z) {
        Holder<Biome> biome = ctx.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                ctx.randomState().sampler());
        return ctx.validBiome().test(biome);
    }

    private static int surface(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstFreeHeight(x, z,
                Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState()) - 1;
    }

    private static int between(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    @Override
    public StructureType<?> type() {
        return JigsawRegister.LARGE_VOLCANO.get();
    }
}
