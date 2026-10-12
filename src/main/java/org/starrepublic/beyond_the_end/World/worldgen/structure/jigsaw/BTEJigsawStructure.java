package org.starrepublic.beyond_the_end.World.worldgen.structure.jigsaw;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.starrepublic.beyond_the_end.Register.JigsawRegister;

import java.util.List;
import java.util.Optional;

/**
 * Controlled BTE Jigsaw structure.
 *
 * Important: findGenerationPoint deliberately does NOT assemble the city.
 * It only creates one cheap deferred StructurePiece. This prevents /locate from
 * repeatedly assembling a huge city while it scans structure-start chunks.
 */
public final class BTEJigsawStructure extends Structure {

    public static final MapCodec<BTEJigsawStructure> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(

                    BTEJigsawStructure.settingsCodec(instance),

                    StructureTemplatePool.CODEC
                            .fieldOf("start_pool")
                            .forGetter(structure -> structure.startPool),

                    HeightProvider.CODEC
                            .fieldOf("start_height")
                            .forGetter(structure -> structure.startHeight),

                    Heightmap.Types.CODEC
                            .optionalFieldOf("project_start_to_heightmap")
                            .forGetter(structure -> structure.projectStartToHeightmap),

                    Codec.intRange(1, 2048)
                            .optionalFieldOf("max_distance_from_center", 192)
                            .forGetter(structure -> structure.maxDistanceFromCenter),

                    BTEJigsawRules.Layout.CODEC
                            .optionalFieldOf(
                                    "layout",
                                    BTEJigsawRules.Layout.DEFAULT
                            )
                            .forGetter(structure -> structure.layout),

                    BTEJigsawRules.Requirement.CODEC
                            .listOf()
                            .optionalFieldOf(
                                    "requirements",
                                    List.of()
                            )
                            .forGetter(structure -> structure.requirements),

                    LiquidSettings.CODEC
                            .optionalFieldOf(
                                    "liquid_settings",
                                    net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.DEFAULT_LIQUID_SETTINGS
                            )
                            .forGetter(structure -> structure.liquidSettings)

            ).apply(instance, BTEJigsawStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final HeightProvider startHeight;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final BTEJigsawRules.Layout layout;
    private final List<BTEJigsawRules.Requirement> requirements;
    private final LiquidSettings liquidSettings;

    public BTEJigsawStructure(
            StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            HeightProvider startHeight,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            BTEJigsawRules.Layout layout,
            List<BTEJigsawRules.Requirement> requirements,
            LiquidSettings liquidSettings
    ) {
        super(settings);

        this.startPool = startPool;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.layout = layout;
        this.requirements = List.copyOf(requirements);
        this.liquidSettings = liquidSettings;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(
            GenerationContext context
    ) {
        int startYOffset = startHeight.sample(
                context.random(),
                new WorldGenerationContext(
                        context.chunkGenerator(),
                        context.heightAccessor()
                )
        );

        BlockPos provisionalStart =
                context.chunkPos().getMiddleBlockPosition(startYOffset);

        ResourceLocation startPoolId = startPool
                .unwrapKey()
                .orElseThrow(() -> new IllegalStateException(
                        "BTE controlled_jigsaw start_pool must be a registered template pool"
                ))
                .location();

        BTEDeferredJigsawPiece deferredPiece =
                new BTEDeferredJigsawPiece(
                        provisionalStart,
                        context.heightAccessor().getMinBuildHeight(),
                        context.heightAccessor().getMaxBuildHeight() - 1,
                        startPoolId,
                        projectStartToHeightmap,
                        maxDistanceFromCenter,
                        layout,
                        requirements,
                        liquidSettings
                );

        return Optional.of(
                new GenerationStub(
                        provisionalStart,
                        builder -> builder.addPiece(deferredPiece)
                )
        );
    }

    @Override
    public StructureType<?> type() {
        return JigsawRegister.CONTROLLED_JIGSAW.get();
    }
}
