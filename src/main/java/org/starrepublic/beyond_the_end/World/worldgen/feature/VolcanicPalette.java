package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** JSON-configured weighted BlockState palette; no hardcoded block registry IDs. */
public record VolcanicPalette(List<Entry> entries) {
    public record Entry(BlockState state, int weight) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(Entry::state),
                Codec.intRange(1, 1000).fieldOf("weight").forGetter(Entry::weight)
        ).apply(i, Entry::new));
    }

    public static final Codec<VolcanicPalette> CODEC = Entry.CODEC.listOf()
            .xmap(VolcanicPalette::new, VolcanicPalette::entries);

    public VolcanicPalette {
        entries = List.copyOf(entries);
        if (entries.isEmpty() || entries.size() > 64)
            throw new IllegalArgumentException("Volcanic palette must have 1..64 entries");
    }

    public BlockState pick(RandomSource random) {
        int total = 0;
        for (Entry e : entries) total += e.weight();
        int roll = random.nextInt(total);
        for (Entry e : entries) {
            roll -= e.weight();
            if (roll < 0) return e.state();
        }
        return entries.getLast().state(); // unreachable, but safe
    }
}
