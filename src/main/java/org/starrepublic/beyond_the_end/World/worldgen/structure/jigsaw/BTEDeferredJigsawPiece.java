package org.starrepublic.beyond_the_end.World.worldgen.structure.jigsaw;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.slf4j.Logger;
import org.starrepublic.beyond_the_end.Register.JigsawRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cheap placeholder stored in the vanilla StructureStart.
 *
 * findGenerationPoint() creates only this piece. /locate therefore sees a
 * normal valid structure start but never runs the expensive BTE assembler.
 *
 * When chunks are actually being decorated, postProcess() lazily builds the
 * complete Jigsaw layout once, caches it on this StructurePiece instance, and
 * then delegates placement to only the generated PoolElementStructurePieces
 * intersecting the current chunk bounding box.
 */
public final class BTEDeferredJigsawPiece extends StructurePiece {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TAG_START_X = "BTEStartX";
    private static final String TAG_START_Y = "BTEStartY";
    private static final String TAG_START_Z = "BTEStartZ";
    private static final String TAG_START_POOL = "BTEStartPool";
    private static final String TAG_HEIGHTMAP = "BTEHeightmap";
    private static final String TAG_MAX_DISTANCE = "BTEMaxDistance";
    private static final String TAG_LAYOUT = "BTELayout";
    private static final String TAG_REQUIREMENTS = "BTERequirements";
    private static final String TAG_LIQUID = "BTELiquid";

    private final BlockPos provisionalStart;
    private final ResourceLocation startPoolId;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final BTEJigsawRules.Layout layout;
    private final List<BTEJigsawRules.Requirement> requirements;
    private final LiquidSettings liquidSettings;

    /**
     * Runtime-only cache. It is deliberately not serialized; the assembler is
     * deterministic, so a save/reload reconstructs exactly the same layout.
     */
    private volatile boolean assemblyComputed = false;
    private Optional<BTEJigsawAssembler.AssemblyResult> cachedAssembly =
            Optional.empty();

    public BTEDeferredJigsawPiece(
            BlockPos provisionalStart,
            int minBuildHeight,
            int maxBuildHeight,
            ResourceLocation startPoolId,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            BTEJigsawRules.Layout layout,
            List<BTEJigsawRules.Requirement> requirements,
            LiquidSettings liquidSettings
    ) {
        super(
                JigsawRegister.DEFERRED_JIGSAW_PIECE.get(),
                0,
                createDeferredBoundingBox(
                        provisionalStart,
                        minBuildHeight,
                        maxBuildHeight,
                        maxDistanceFromCenter
                )
        );

        this.provisionalStart = provisionalStart;
        this.startPoolId = startPoolId;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.layout = layout;
        this.requirements = List.copyOf(requirements);
        this.liquidSettings = liquidSettings;
    }

    /**
     * Deserialization constructor used by the registered StructurePieceType.
     */
    public BTEDeferredJigsawPiece(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        super(
                JigsawRegister.DEFERRED_JIGSAW_PIECE.get(),
                tag
        );

        this.provisionalStart = new BlockPos(
                tag.getInt(TAG_START_X),
                tag.getInt(TAG_START_Y),
                tag.getInt(TAG_START_Z)
        );

        ResourceLocation parsedPool = ResourceLocation.tryParse(
                tag.getString(TAG_START_POOL)
        );

        if (parsedPool == null) {
            throw new IllegalArgumentException(
                    "Invalid BTE deferred Jigsaw start-pool id: "
                            + tag.getString(TAG_START_POOL)
            );
        }

        this.startPoolId = parsedPool;

        if (tag.contains(TAG_HEIGHTMAP)) {
            Heightmap.Types type;

            try {
                type = Heightmap.Types.valueOf(
                        tag.getString(TAG_HEIGHTMAP)
                );
            } catch (IllegalArgumentException exception) {
                type = Heightmap.Types.WORLD_SURFACE_WG;
            }

            this.projectStartToHeightmap = Optional.of(type);
        } else {
            this.projectStartToHeightmap = Optional.empty();
        }

        this.maxDistanceFromCenter = Math.max(
                1,
                tag.getInt(TAG_MAX_DISTANCE)
        );

        this.layout = tag.contains(TAG_LAYOUT, Tag.TAG_COMPOUND)
                ? BTEJigsawRules.Layout.load(tag.getCompound(TAG_LAYOUT))
                : BTEJigsawRules.Layout.DEFAULT;

        List<BTEJigsawRules.Requirement> loadedRequirements =
                new ArrayList<>();

        ListTag requirementTags = tag.getList(
                TAG_REQUIREMENTS,
                Tag.TAG_COMPOUND
        );

        for (int i = 0; i < requirementTags.size(); i++) {
            loadedRequirements.add(
                    BTEJigsawRules.Requirement.load(
                            requirementTags.getCompound(i)
                    )
            );
        }

        this.requirements = List.copyOf(loadedRequirements);

        LiquidSettings loadedLiquid;

        try {
            loadedLiquid = LiquidSettings.valueOf(
                    tag.getString(TAG_LIQUID)
            );
        } catch (IllegalArgumentException exception) {
            loadedLiquid = net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.DEFAULT_LIQUID_SETTINGS;
        }

        this.liquidSettings = loadedLiquid;
    }

    private static BoundingBox createDeferredBoundingBox(
            BlockPos start,
            int minBuildHeight,
            int maxBuildHeight,
            int maxDistanceFromCenter
    ) {
        return new BoundingBox(
                start.getX() - maxDistanceFromCenter,
                minBuildHeight,
                start.getZ() - maxDistanceFromCenter,
                start.getX() + maxDistanceFromCenter,
                maxBuildHeight,
                start.getZ() + maxDistanceFromCenter
        );
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        tag.putInt(TAG_START_X, provisionalStart.getX());
        tag.putInt(TAG_START_Y, provisionalStart.getY());
        tag.putInt(TAG_START_Z, provisionalStart.getZ());
        tag.putString(TAG_START_POOL, startPoolId.toString());

        projectStartToHeightmap.ifPresent(
                type -> tag.putString(TAG_HEIGHTMAP, type.name())
        );

        tag.putInt(TAG_MAX_DISTANCE, maxDistanceFromCenter);
        tag.put(TAG_LAYOUT, layout.save());

        ListTag requirementTags = new ListTag();
        for (BTEJigsawRules.Requirement requirement : requirements) {
            requirementTags.add(requirement.save());
        }
        tag.put(TAG_REQUIREMENTS, requirementTags);

        tag.putString(TAG_LIQUID, liquidSettings.name());
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        Optional<BTEJigsawAssembler.AssemblyResult> assembly =
                getOrCreateAssembly(level, generator);

        if (assembly.isEmpty()) {
            return;
        }

        for (PoolElementStructurePiece piece : assembly.get().pieces()) {
            if (!piece.getBoundingBox().intersects(chunkBox)) {
                continue;
            }

            piece.postProcess(
                    level,
                    structureManager,
                    generator,
                    random,
                    chunkBox,
                    chunkPos,
                    pivot
            );
        }
    }

    private Optional<BTEJigsawAssembler.AssemblyResult> getOrCreateAssembly(
            WorldGenLevel level,
            ChunkGenerator generator
    ) {
        if (assemblyComputed) {
            return cachedAssembly;
        }

        synchronized (this) {
            if (assemblyComputed) {
                return cachedAssembly;
            }

            Registry<StructureTemplatePool> poolRegistry =
                    level.registryAccess().registryOrThrow(
                            Registries.TEMPLATE_POOL
                    );

            ResourceKey<StructureTemplatePool> startPoolKey =
                    ResourceKey.create(
                            Registries.TEMPLATE_POOL,
                            startPoolId
                    );

            Optional<Holder.Reference<StructureTemplatePool>> startPool =
                    poolRegistry.getHolder(startPoolKey);

            if (startPool.isEmpty()) {
                LOGGER.error(
                        "[Beyond The End] Missing deferred Jigsaw start pool: {}",
                        startPoolId
                );

                cachedAssembly = Optional.empty();
                assemblyComputed = true;
                return cachedAssembly;
            }

            cachedAssembly = BTEJigsawAssembler.assemble(
                    level,
                    generator,
                    startPool.get(),
                    provisionalStart,
                    projectStartToHeightmap,
                    maxDistanceFromCenter,
                    layout,
                    requirements,
                    liquidSettings
            );

            if (cachedAssembly.isEmpty()) {
                // One message per actual structure instance, not once per
                // locate candidate and not once per chunk.
                LOGGER.warn(
                        "[Beyond The End] Controlled Jigsaw at {} could not satisfy its layout/requirements. "
                                + "start_pool={}, requirements={}",
                        provisionalStart,
                        startPoolId,
                        requirements
                );
            }

            assemblyComputed = true;
            return cachedAssembly;
        }
    }
}
