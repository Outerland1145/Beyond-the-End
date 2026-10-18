package org.starrepublic.beyond_the_end.World.worldgen.structure.volcano;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * Datapack-defined materials for one volcano STRUCTURE entry.
 *
 * The palette is deliberately a field of worldgen/structure/<name>.json.
 * It is decoded when the datapack registries load; no ad-hoc resource reload
 * listener or global mutable palette is necessary.
 *
 * Each state is a real Minecraft BlockState.CODEC value:
 *   {"Name":"minecraft:basalt"}
 *   {"Name":"minecraft:basalt", "Properties":{"axis":"y"}}
 */
public record LargeVolcanoPalette(
        WeightedPalette surface,
        WeightedPalette interior,
        WeightedPalette rim,
        WeightedPalette crater,
        WeightedPalette riverBed,
        WeightedPalette riverBank,
        BlockState lava,
        Optional<BlockState> foundationBlock
) {
    public static final Codec<LargeVolcanoPalette> CODEC = RecordCodecBuilder.create(i -> i.group(
            WeightedPalette.CODEC.fieldOf("surface").forGetter(LargeVolcanoPalette::surface),
            WeightedPalette.CODEC.fieldOf("interior").forGetter(LargeVolcanoPalette::interior),
            WeightedPalette.CODEC.fieldOf("rim").forGetter(LargeVolcanoPalette::rim),
            WeightedPalette.CODEC.fieldOf("crater").forGetter(LargeVolcanoPalette::crater),
            WeightedPalette.CODEC.fieldOf("river_bed").forGetter(LargeVolcanoPalette::riverBed),
            WeightedPalette.CODEC.fieldOf("river_bank").forGetter(LargeVolcanoPalette::riverBank),
            BlockState.CODEC.optionalFieldOf("lava", Blocks.LAVA.defaultBlockState())
                    .forGetter(LargeVolcanoPalette::lava),
            BlockState.CODEC.optionalFieldOf("foundation_block")
                    .forGetter(LargeVolcanoPalette::foundationBlock)
    ).apply(i, LargeVolcanoPalette::new));

    public static final LargeVolcanoPalette DEFAULT = new LargeVolcanoPalette(
            WeightedPalette.of(Blocks.BASALT.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState()),
            WeightedPalette.of(Blocks.BASALT.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState()),
            WeightedPalette.of(Blocks.OBSIDIAN.defaultBlockState(), Blocks.BASALT.defaultBlockState()),
            WeightedPalette.of(Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState()),
            WeightedPalette.of(Blocks.BASALT.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState()),
            WeightedPalette.of(Blocks.BLACKSTONE.defaultBlockState(), Blocks.BASALT.defaultBlockState()),
            Blocks.LAVA.defaultBlockState(),
            Optional.empty()
    );

    public record WeightedBlock(BlockState state, int weight) {
        public static final Codec<WeightedBlock> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(WeightedBlock::state),
                Codec.intRange(1, 4096).optionalFieldOf("weight", 1).forGetter(WeightedBlock::weight)
        ).apply(i, WeightedBlock::new));
    }

    public record WeightedPalette(List<WeightedBlock> entries) {
        public static final Codec<WeightedPalette> CODEC = WeightedBlock.CODEC.listOf()
                .xmap(WeightedPalette::new, WeightedPalette::entries);

        public WeightedPalette {
            entries = List.copyOf(entries);
            if (entries.isEmpty() || entries.size() > 64) {
                throw new IllegalArgumentException("Volcano palette requires 1..64 entries");
            }
        }

        public static WeightedPalette of(BlockState... states) {
            return new WeightedPalette(java.util.Arrays.stream(states)
                    .map(s -> new WeightedBlock(s, 1))
                    .toList());
        }

        public BlockState pick(long seed, int x, int y, int z, long salt) {
            int total = 0;
            for (WeightedBlock block : entries) total += block.weight();
            long hash = LargeVolcanoGeometry.hash(seed, x, y, z, salt);
            int roll = (int) Long.remainderUnsigned(hash, total);
            for (WeightedBlock block : entries) {
                roll -= block.weight();
                if (roll < 0) return block.state();
            }
            return entries.get(entries.size() - 1).state();
        }
    }
}
