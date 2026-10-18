package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

/** All tunable dimensions, validity rules and block palettes are in configured_feature JSONs. */
public final class VolcanicFeatureConfigs {
    private VolcanicFeatureConfigs() {}

    public record Columns(int minHeight, int maxHeight, int count, int spread, int maxWidth,
                          List<ResourceLocation> groundBlocks, List<ResourceLocation> replaceableBlocks,
                          VolcanicPalette core, VolcanicPalette crust) implements FeatureConfiguration {
        public static final Codec<Columns> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(2, 24).fieldOf("min_height").forGetter(Columns::minHeight),
                Codec.intRange(2, 24).fieldOf("max_height").forGetter(Columns::maxHeight),
                Codec.intRange(1, 16).fieldOf("column_count").forGetter(Columns::count),
                Codec.intRange(1, 8).fieldOf("spread").forGetter(Columns::spread),
                Codec.intRange(1, 3).fieldOf("max_width").forGetter(Columns::maxWidth),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("ground_blocks").forGetter(Columns::groundBlocks),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("replaceable_blocks").forGetter(Columns::replaceableBlocks),
                VolcanicPalette.CODEC.fieldOf("core").forGetter(Columns::core),
                VolcanicPalette.CODEC.fieldOf("crust").forGetter(Columns::crust)
        ).apply(i, Columns::new));
        public Columns { if (minHeight > maxHeight) throw new IllegalArgumentException("min_height > max_height"); }
    }

    public record Rubble(int minRadius, int maxRadius, int maxDepth, int boulderChance,
                         List<ResourceLocation> groundBlocks, List<ResourceLocation> replaceableBlocks,
                         VolcanicPalette surface, VolcanicPalette boulder) implements FeatureConfiguration {
        public static final Codec<Rubble> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(2, 9).fieldOf("min_radius").forGetter(Rubble::minRadius),
                Codec.intRange(2, 9).fieldOf("max_radius").forGetter(Rubble::maxRadius),
                Codec.intRange(1, 4).fieldOf("max_depth").forGetter(Rubble::maxDepth),
                Codec.intRange(0, 100).fieldOf("boulder_chance_percent").forGetter(Rubble::boulderChance),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("ground_blocks").forGetter(Rubble::groundBlocks),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("replaceable_blocks").forGetter(Rubble::replaceableBlocks),
                VolcanicPalette.CODEC.fieldOf("surface").forGetter(Rubble::surface),
                VolcanicPalette.CODEC.fieldOf("boulder").forGetter(Rubble::boulder)
        ).apply(i, Rubble::new));
        public Rubble { if (minRadius > maxRadius) throw new IllegalArgumentException("min_radius > max_radius"); }
    }

    public record Fissure(int minLength, int maxLength, int maxWidth, int maxDepth,
                          int lavaChancePercent, List<ResourceLocation> groundBlocks,
                          List<ResourceLocation> replaceableBlocks, VolcanicPalette bed,
                          VolcanicPalette banks, BlockState lava) implements FeatureConfiguration {
        public static final Codec<Fissure> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(3, 14).fieldOf("min_length").forGetter(Fissure::minLength),
                Codec.intRange(3, 14).fieldOf("max_length").forGetter(Fissure::maxLength),
                Codec.intRange(1, 2).fieldOf("max_width").forGetter(Fissure::maxWidth),
                Codec.intRange(1, 4).fieldOf("max_depth").forGetter(Fissure::maxDepth),
                Codec.intRange(0, 100).fieldOf("lava_chance_percent").forGetter(Fissure::lavaChancePercent),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("ground_blocks").forGetter(Fissure::groundBlocks),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("replaceable_blocks").forGetter(Fissure::replaceableBlocks),
                VolcanicPalette.CODEC.fieldOf("bed").forGetter(Fissure::bed),
                VolcanicPalette.CODEC.fieldOf("banks").forGetter(Fissure::banks),
                BlockState.CODEC.fieldOf("lava").forGetter(Fissure::lava)
        ).apply(i, Fissure::new));
        public Fissure { if (minLength > maxLength) throw new IllegalArgumentException("min_length > max_length"); }
    }

    public record SmallVolcano(int minRadius, int maxRadius, int minHeight, int maxHeight,
                               double craterRatio, int lavaChancePercent, double lavaHeightRatio,
                               List<ResourceLocation> groundBlocks, List<ResourceLocation> replaceableBlocks,
                               VolcanicPalette surface, VolcanicPalette core, VolcanicPalette rim,
                               VolcanicPalette craterFloor, BlockState lava) implements FeatureConfiguration {
        public static final Codec<SmallVolcano> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(4, 11).fieldOf("min_radius").forGetter(SmallVolcano::minRadius),
                Codec.intRange(4, 11).fieldOf("max_radius").forGetter(SmallVolcano::maxRadius),
                Codec.intRange(3, 20).fieldOf("min_height").forGetter(SmallVolcano::minHeight),
                Codec.intRange(3, 20).fieldOf("max_height").forGetter(SmallVolcano::maxHeight),
                Codec.doubleRange(0.16, 0.50).fieldOf("crater_ratio").forGetter(SmallVolcano::craterRatio),
                Codec.intRange(0, 100).fieldOf("lava_chance_percent").forGetter(SmallVolcano::lavaChancePercent),
                Codec.doubleRange(0.35, 0.88).fieldOf("lava_height_ratio").forGetter(SmallVolcano::lavaHeightRatio),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("ground_blocks").forGetter(SmallVolcano::groundBlocks),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("replaceable_blocks").forGetter(SmallVolcano::replaceableBlocks),
                VolcanicPalette.CODEC.fieldOf("surface").forGetter(SmallVolcano::surface),
                VolcanicPalette.CODEC.fieldOf("core").forGetter(SmallVolcano::core),
                VolcanicPalette.CODEC.fieldOf("rim").forGetter(SmallVolcano::rim),
                VolcanicPalette.CODEC.fieldOf("crater_floor").forGetter(SmallVolcano::craterFloor),
                BlockState.CODEC.fieldOf("lava").forGetter(SmallVolcano::lava)
        ).apply(i, SmallVolcano::new));
        public SmallVolcano {
            if (minRadius > maxRadius || minHeight > maxHeight)
                throw new IllegalArgumentException("small volcano min > max");
        }
    }

    public record Flames(int attempts, int spread, int chancePercent, BlockState fire,
                         List<ResourceLocation> groundBlocks, List<ResourceLocation> replaceableBlocks,
                         int supportConversionPercent, VolcanicPalette support) implements FeatureConfiguration {
        public static final Codec<Flames> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 32).fieldOf("attempts").forGetter(Flames::attempts),
                Codec.intRange(1, 8).fieldOf("spread").forGetter(Flames::spread),
                Codec.intRange(0, 100).fieldOf("chance_percent").forGetter(Flames::chancePercent),
                BlockState.CODEC.fieldOf("fire").forGetter(Flames::fire),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("ground_blocks").forGetter(Flames::groundBlocks),
                VolcanicFeatureUtil.BLOCK_IDS.fieldOf("replaceable_blocks").forGetter(Flames::replaceableBlocks),
                Codec.intRange(0, 100).fieldOf("support_conversion_percent").forGetter(Flames::supportConversionPercent),
                VolcanicPalette.CODEC.fieldOf("support").forGetter(Flames::support)
        ).apply(i, Flames::new));
    }
}
