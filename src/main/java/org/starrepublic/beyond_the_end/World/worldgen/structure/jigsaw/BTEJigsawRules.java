package org.starrepublic.beyond_the_end.World.worldgen.structure.jigsaw;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Data-driven limits for Beyond The End controlled Jigsaw structures.
 */
public final class BTEJigsawRules {

    private BTEJigsawRules() {}

    /**
     * Layout limits for one assembled structure.
     *
     * maxDepth is BTE's own traversal limit. It is not vanilla JigsawStructure's
     * MAX_DEPTH and therefore is not restricted to 20.
     *
     * attempts is only used when the structure is actually assembled. /locate
     * never runs the assembler after the deferred-piece rewrite.
     */
    public record Layout(
            int maxDepth,
            int minPieces,
            int maxPieces,
            int attempts
    ) {

        public static final Layout DEFAULT =
                new Layout(
                        48,
                        20,
                        96,
                        8
                );

        public static final Codec<Layout> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(

                        Codec.intRange(1, 256)
                                .optionalFieldOf(
                                        "max_depth",
                                        DEFAULT.maxDepth()
                                )
                                .forGetter(Layout::maxDepth),

                        Codec.intRange(1, 4096)
                                .optionalFieldOf(
                                        "min_pieces",
                                        DEFAULT.minPieces()
                                )
                                .forGetter(Layout::minPieces),

                        Codec.intRange(1, 4096)
                                .optionalFieldOf(
                                        "max_pieces",
                                        DEFAULT.maxPieces()
                                )
                                .forGetter(Layout::maxPieces),

                        Codec.intRange(1, 128)
                                .optionalFieldOf(
                                        "attempts",
                                        DEFAULT.attempts()
                                )
                                .forGetter(Layout::attempts)

                ).apply(instance, Layout::new));

        public Layout {
            if (minPieces > maxPieces) {
                throw new IllegalArgumentException(
                        "min_pieces cannot be greater than max_pieces"
                );
            }
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("MaxDepth", maxDepth);
            tag.putInt("MinPieces", minPieces);
            tag.putInt("MaxPieces", maxPieces);
            tag.putInt("Attempts", attempts);
            return tag;
        }

        public static Layout load(CompoundTag tag) {
            int maxDepth = tag.contains("MaxDepth")
                    ? tag.getInt("MaxDepth")
                    : DEFAULT.maxDepth();

            int minPieces = tag.contains("MinPieces")
                    ? tag.getInt("MinPieces")
                    : DEFAULT.minPieces();

            int maxPieces = tag.contains("MaxPieces")
                    ? tag.getInt("MaxPieces")
                    : DEFAULT.maxPieces();

            int attempts = tag.contains("Attempts")
                    ? tag.getInt("Attempts")
                    : DEFAULT.attempts();

            return new Layout(
                    Math.max(1, maxDepth),
                    Math.max(1, minPieces),
                    Math.max(Math.max(1, minPieces), maxPieces),
                    Math.max(1, attempts)
            );
        }
    }

    /**
     * Requirement for a logical component.
     *
     * component is NOT a pool id and NOT a structure-template id. It is the
     * logical id placed on BTEComponentPoolElement, for example
     * beyond_the_end:fat_tower.
     */
    public record Requirement(
            ResourceLocation component,
            int minCount,
            int maxCount,
            int minDepth,
            int maxDepth,
            int priority
    ) {

        public static final Codec<Requirement> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(

                        ResourceLocation.CODEC
                                .fieldOf("component")
                                .forGetter(Requirement::component),

                        Codec.intRange(0, 1024)
                                .optionalFieldOf("min_count", 0)
                                .forGetter(Requirement::minCount),

                        Codec.intRange(0, 1024)
                                .optionalFieldOf("max_count", 1024)
                                .forGetter(Requirement::maxCount),

                        Codec.intRange(0, 256)
                                .optionalFieldOf("min_depth", 0)
                                .forGetter(Requirement::minDepth),

                        Codec.intRange(0, 256)
                                .optionalFieldOf("max_depth", 256)
                                .forGetter(Requirement::maxDepth),

                        Codec.INT
                                .optionalFieldOf("priority", 0)
                                .forGetter(Requirement::priority)

                ).apply(instance, Requirement::new));

        public Requirement {
            if (minCount > maxCount) {
                throw new IllegalArgumentException(
                        component + ": min_count cannot be greater than max_count"
                );
            }

            if (minDepth > maxDepth) {
                throw new IllegalArgumentException(
                        component + ": min_depth cannot be greater than max_depth"
                );
            }
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Component", component.toString());
            tag.putInt("MinCount", minCount);
            tag.putInt("MaxCount", maxCount);
            tag.putInt("MinDepth", minDepth);
            tag.putInt("MaxDepth", maxDepth);
            tag.putInt("Priority", priority);
            return tag;
        }

        public static Requirement load(CompoundTag tag) {
            ResourceLocation component = ResourceLocation.tryParse(
                    tag.getString("Component")
            );

            if (component == null) {
                throw new IllegalArgumentException(
                        "Invalid BTE Jigsaw component id in saved structure piece: "
                                + tag.getString("Component")
                );
            }

            return new Requirement(
                    component,
                    Math.max(0, tag.getInt("MinCount")),
                    Math.max(0, tag.getInt("MaxCount")),
                    Math.max(0, tag.getInt("MinDepth")),
                    Math.max(0, tag.getInt("MaxDepth")),
                    tag.getInt("Priority")
            );
        }
    }
}
