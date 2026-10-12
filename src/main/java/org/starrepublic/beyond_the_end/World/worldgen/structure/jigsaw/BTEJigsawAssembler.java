package org.starrepublic.beyond_the_end.World.worldgen.structure.jigsaw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Beyond The End's actual Jigsaw assembler.
 *
 * This class is intentionally NOT called from Structure.findGenerationPoint().
 * It only runs from BTEDeferredJigsawPiece.postProcess(), when the structure is
 * really being placed into world-generation chunks. Therefore /locate can scan
 * structure starts without repeatedly assembling complete End Cities.
 */
public final class BTEJigsawAssembler {

    private static final int MAX_POOL_GRAPH_SCAN = 4096;

    private BTEJigsawAssembler() {}

    public record AssemblyResult(
            BlockPos anchor,
            List<PoolElementStructurePiece> pieces
    ) {}

    private record AssemblyAttempt(
            BlockPos anchor,
            List<PoolElementStructurePiece> pieces,
            RequirementTracker tracker
    ) {}

    private record QueueEntry(
            PoolElementStructurePiece piece,
            int depth
    ) {}

    /**
     * Assemble one deterministic structure layout.
     *
     * The same world seed + provisional start + JSON configuration always
     * produces the same result, including after a save/reload.
     */
    public static Optional<AssemblyResult> assemble(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            Holder<StructureTemplatePool> startPool,
            BlockPos provisionalStart,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            BTEJigsawRules.Layout layout,
            List<BTEJigsawRules.Requirement> requirements,
            LiquidSettings liquidSettings
    ) {
        Registry<StructureTemplatePool> poolRegistry =
                level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);

        StructureTemplateManager templateManager =
                level.getLevel().getStructureManager();

        RandomState randomState =
                level.getLevel().getChunkSource().randomState();

        // Fail fast if a mandatory logical component does not exist anywhere
        // in the reachable template-pool graph. This prevents dozens of
        // pointless rerolls when a JSON component id is mistyped.
        Set<ResourceLocation> reachableComponents = collectReachableComponents(
                startPool,
                poolRegistry,
                templateManager
        );

        for (BTEJigsawRules.Requirement requirement : requirements) {
            if (requirement.minCount() > 0
                    && !reachableComponents.contains(requirement.component())) {
                return Optional.empty();
            }
        }

        for (int attempt = 0; attempt < layout.attempts(); attempt++) {
            RandomSource random = RandomSource.create(
                    createAttemptSeed(
                            level.getSeed(),
                            provisionalStart,
                            attempt
                    )
            );

            Optional<AssemblyAttempt> attemptResult = assembleAttempt(
                    level,
                    chunkGenerator,
                    randomState,
                    templateManager,
                    poolRegistry,
                    startPool,
                    provisionalStart,
                    projectStartToHeightmap,
                    maxDistanceFromCenter,
                    layout,
                    requirements,
                    liquidSettings,
                    random
            );

            if (attemptResult.isEmpty()) {
                continue;
            }

            AssemblyAttempt result = attemptResult.get();

            if (result.pieces().size() < layout.minPieces()) {
                continue;
            }

            if (!result.tracker().allRequirementsSatisfied()) {
                continue;
            }

            return Optional.of(
                    new AssemblyResult(
                            result.anchor(),
                            List.copyOf(result.pieces())
                    )
            );
        }

        return Optional.empty();
    }

    private static Optional<AssemblyAttempt> assembleAttempt(
            WorldGenLevel level,
            ChunkGenerator chunkGenerator,
            RandomState randomState,
            StructureTemplateManager templateManager,
            Registry<StructureTemplatePool> poolRegistry,
            Holder<StructureTemplatePool> startPoolHolder,
            BlockPos provisionalStart,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            BTEJigsawRules.Layout layout,
            List<BTEJigsawRules.Requirement> requirements,
            LiquidSettings liquidSettings,
            RandomSource random
    ) {
        StructureTemplatePool startPool = startPoolHolder.value();

        if (startPool.size() == 0) {
            return Optional.empty();
        }

        StructurePoolElement startElement =
                startPool.getRandomTemplate(random);

        if (startElement == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        }

        if (startElement.getProjection()
                != StructureTemplatePool.Projection.RIGID) {
            return Optional.empty();
        }

        Rotation startRotation = Rotation.getRandom(random);

        BoundingBox initialBox = startElement.getBoundingBox(
                templateManager,
                provisionalStart,
                startRotation
        );

        PoolElementStructurePiece startPiece =
                new PoolElementStructurePiece(
                        templateManager,
                        startElement,
                        provisionalStart,
                        startElement.getGroundLevelDelta(),
                        startRotation,
                        initialBox,
                        liquidSettings
                );

        BoundingBox startBox = startPiece.getBoundingBox();

        int centerX = (startBox.minX() + startBox.maxX()) / 2;
        int centerZ = (startBox.minZ() + startBox.maxZ()) / 2;

        int targetY = provisionalStart.getY();

        if (projectStartToHeightmap.isPresent()) {
            targetY += chunkGenerator.getFirstFreeHeight(
                    centerX,
                    centerZ,
                    projectStartToHeightmap.get(),
                    level,
                    randomState
            );
        }

        int currentGroundY =
                startBox.minY() + startPiece.getGroundLevelDelta();

        startPiece.move(
                0,
                targetY - currentGroundY,
                0
        );

        startBox = startPiece.getBoundingBox();

        if (!insideWorldHeight(level, startBox)) {
            return Optional.empty();
        }

        BlockPos anchor = new BlockPos(
                (startBox.minX() + startBox.maxX()) / 2,
                targetY,
                (startBox.minZ() + startBox.maxZ()) / 2
        );

        RequirementTracker tracker =
                new RequirementTracker(requirements);

        List<PoolElementStructurePiece> pieces =
                new ArrayList<>();

        pieces.add(startPiece);
        tracker.onPlaced(startElement);

        Deque<QueueEntry> queue =
                new ArrayDeque<>();

        queue.addLast(new QueueEntry(startPiece, 0));

        List<AABB> occupied =
                new ArrayList<>();

        occupied.add(AABB.of(startBox));

        Assembler assembler = new Assembler(
                level,
                templateManager,
                poolRegistry,
                maxDistanceFromCenter,
                layout,
                liquidSettings,
                anchor,
                pieces,
                occupied,
                queue,
                tracker,
                random
        );

        while (!queue.isEmpty()
                && pieces.size() < layout.maxPieces()) {
            assembler.expand(queue.removeFirst());
        }

        return Optional.of(
                new AssemblyAttempt(
                        anchor,
                        pieces,
                        tracker
                )
        );
    }

    /**
     * Scan the pool graph without placing anything. Used only to detect an
     * impossible/mistyped mandatory component before doing expensive rerolls.
     */
    private static Set<ResourceLocation> collectReachableComponents(
            Holder<StructureTemplatePool> startPool,
            Registry<StructureTemplatePool> poolRegistry,
            StructureTemplateManager templateManager
    ) {
        Set<ResourceLocation> components = new HashSet<>();
        Set<ResourceLocation> visitedPools = new HashSet<>();
        Deque<Holder<StructureTemplatePool>> queue = new ArrayDeque<>();
        queue.add(startPool);

        RandomSource scanRandom = RandomSource.create(0x4254454AL);
        int scanned = 0;

        while (!queue.isEmpty() && scanned++ < MAX_POOL_GRAPH_SCAN) {
            Holder<StructureTemplatePool> poolHolder = queue.removeFirst();

            Optional<ResourceLocation> poolId = poolHolder
                    .unwrapKey()
                    .map(key -> key.location());

            if (poolId.isPresent() && !visitedPools.add(poolId.get())) {
                continue;
            }

            StructureTemplatePool pool = poolHolder.value();

            // LinkedHashSet removes weight-produced duplicates while keeping a
            // stable iteration order for the graph scan.
            Set<StructurePoolElement> uniqueElements =
                    new LinkedHashSet<>(pool.getShuffledTemplates(scanRandom));

            for (StructurePoolElement element : uniqueElements) {
                if (element == EmptyPoolElement.INSTANCE) {
                    continue;
                }

                ResourceLocation component = getComponent(element);
                if (component != null) {
                    components.add(component);
                }

                List<StructureTemplate.StructureBlockInfo> jigsaws =
                        element.getShuffledJigsawBlocks(
                                templateManager,
                                BlockPos.ZERO,
                                Rotation.NONE,
                                scanRandom
                        );

                for (StructureTemplate.StructureBlockInfo jigsaw : jigsaws) {
                    if (jigsaw.nbt() == null) {
                        continue;
                    }

                    ResourceLocation childPoolId = ResourceLocation.tryParse(
                            jigsaw.nbt().getString("pool")
                    );

                    if (childPoolId == null) {
                        continue;
                    }

                    poolRegistry.getHolder(
                                    net.minecraft.resources.ResourceKey.create(
                                            Registries.TEMPLATE_POOL,
                                            childPoolId
                                    )
                            )
                            .ifPresent(queue::addLast);
                }
            }

            Holder<StructureTemplatePool> fallback = pool.getFallback();
            queue.addLast(fallback);
        }

        return components;
    }

    private static final class Assembler {

        private final WorldGenLevel level;
        private final StructureTemplateManager templateManager;
        private final Registry<StructureTemplatePool> poolRegistry;
        private final int maxDistanceFromCenter;
        private final BTEJigsawRules.Layout layout;
        private final LiquidSettings liquidSettings;
        private final BlockPos structureCenter;
        private final List<PoolElementStructurePiece> pieces;
        private final List<AABB> occupied;
        private final Deque<QueueEntry> queue;
        private final RequirementTracker tracker;
        private final RandomSource random;

        private Assembler(
                WorldGenLevel level,
                StructureTemplateManager templateManager,
                Registry<StructureTemplatePool> poolRegistry,
                int maxDistanceFromCenter,
                BTEJigsawRules.Layout layout,
                LiquidSettings liquidSettings,
                BlockPos structureCenter,
                List<PoolElementStructurePiece> pieces,
                List<AABB> occupied,
                Deque<QueueEntry> queue,
                RequirementTracker tracker,
                RandomSource random
        ) {
            this.level = level;
            this.templateManager = templateManager;
            this.poolRegistry = poolRegistry;
            this.maxDistanceFromCenter = maxDistanceFromCenter;
            this.layout = layout;
            this.liquidSettings = liquidSettings;
            this.structureCenter = structureCenter;
            this.pieces = pieces;
            this.occupied = occupied;
            this.queue = queue;
            this.tracker = tracker;
            this.random = random;
        }

        private void expand(QueueEntry entry) {
            if (pieces.size() >= layout.maxPieces()) {
                return;
            }

            PoolElementStructurePiece parent = entry.piece();
            StructurePoolElement parentElement = parent.getElement();

            if (parentElement.getProjection()
                    != StructureTemplatePool.Projection.RIGID) {
                return;
            }

            List<StructureTemplate.StructureBlockInfo> parentJigsaws =
                    parentElement.getShuffledJigsawBlocks(
                            templateManager,
                            parent.getPosition(),
                            parent.getRotation(),
                            random
                    );

            for (StructureTemplate.StructureBlockInfo parentJigsaw : parentJigsaws) {
                if (pieces.size() >= layout.maxPieces()) {
                    return;
                }

                if (parentJigsaw.nbt() == null) {
                    continue;
                }

                ResourceLocation poolId = ResourceLocation.tryParse(
                        parentJigsaw.nbt().getString("pool")
                );

                if (poolId == null) {
                    continue;
                }

                Optional<Holder.Reference<StructureTemplatePool>> poolHolder =
                        poolRegistry.getHolder(
                                net.minecraft.resources.ResourceKey.create(
                                        Registries.TEMPLATE_POOL,
                                        poolId
                                )
                        );

                if (poolHolder.isEmpty()) {
                    continue;
                }

                StructureTemplatePool pool = poolHolder.get().value();
                int childDepth = entry.depth() + 1;

                boolean generated = false;

                if (childDepth <= layout.maxDepth()) {
                    generated = tryPool(
                            pool,
                            parent,
                            parentJigsaw,
                            childDepth
                    );
                }

                if (!generated) {
                    tryPool(
                            pool.getFallback().value(),
                            parent,
                            parentJigsaw,
                            childDepth
                    );
                }
            }
        }

        private boolean tryPool(
                StructureTemplatePool pool,
                PoolElementStructurePiece parent,
                StructureTemplate.StructureBlockInfo parentJigsaw,
                int childDepth
        ) {
            if (pool.size() == 0) {
                return false;
            }

            List<StructurePoolElement> shuffled =
                    new ArrayList<>(pool.getShuffledTemplates(random));

            List<StructurePoolElement> requiredCandidates =
                    new ArrayList<>();

            List<StructurePoolElement> normalCandidates =
                    new ArrayList<>();

            for (StructurePoolElement candidate : shuffled) {
                if (candidate == EmptyPoolElement.INSTANCE) {
                    // Keep Empty in the normal weighted order. If it is
                    // reached, this Jigsaw branch intentionally terminates.
                    normalCandidates.add(candidate);
                    continue;
                }

                if (candidate.getProjection()
                        != StructureTemplatePool.Projection.RIGID) {
                    continue;
                }

                if (!tracker.canPlace(candidate, childDepth)) {
                    continue;
                }

                if (tracker.isRequiredNow(candidate, childDepth)) {
                    requiredCandidates.add(candidate);
                } else {
                    normalCandidates.add(candidate);
                }
            }

            // Required components are grouped first, and higher-priority
            // requirements go before lower-priority ones. Inside equal scores
            // the original weighted shuffled order is preserved (TimSort is
            // stable), so Template Pool weights still matter.
            requiredCandidates.sort(
                    Comparator.comparingInt(
                            (StructurePoolElement element) ->
                                    tracker.requiredPriority(element)
                    ).reversed()
            );

            for (StructurePoolElement candidate : requiredCandidates) {
                if (tryCandidate(
                        parent,
                        parentJigsaw,
                        candidate,
                        childDepth
                )) {
                    return true;
                }
            }

            for (StructurePoolElement candidate : normalCandidates) {
                if (candidate == EmptyPoolElement.INSTANCE) {
                    return false;
                }

                if (tryCandidate(
                        parent,
                        parentJigsaw,
                        candidate,
                        childDepth
                )) {
                    return true;
                }
            }

            return false;
        }

        private boolean tryCandidate(
                PoolElementStructurePiece parent,
                StructureTemplate.StructureBlockInfo parentJigsaw,
                StructurePoolElement candidate,
                int childDepth
        ) {
            Direction parentDirection =
                    JigsawBlock.getFrontFacing(parentJigsaw.state());

            BlockPos targetPosition =
                    parentJigsaw.pos().relative(parentDirection);

            for (Rotation rotation : Rotation.getShuffled(random)) {
                List<StructureTemplate.StructureBlockInfo> candidateJigsaws =
                        candidate.getShuffledJigsawBlocks(
                                templateManager,
                                BlockPos.ZERO,
                                rotation,
                                random
                        );

                for (StructureTemplate.StructureBlockInfo candidateJigsaw : candidateJigsaws) {
                    if (!JigsawBlock.canAttach(
                            parentJigsaw,
                            candidateJigsaw
                    )) {
                        continue;
                    }

                    BlockPos localJigsawPos = candidateJigsaw.pos();

                    BlockPos candidateOrigin = new BlockPos(
                            targetPosition.getX() - localJigsawPos.getX(),
                            targetPosition.getY() - localJigsawPos.getY(),
                            targetPosition.getZ() - localJigsawPos.getZ()
                    );

                    BoundingBox candidateBox = candidate.getBoundingBox(
                            templateManager,
                            candidateOrigin,
                            rotation
                    );

                    if (!insideWorldHeight(level, candidateBox)) {
                        continue;
                    }

                    if (!insideStructureRadius(candidateBox)) {
                        continue;
                    }

                    if (collides(candidateBox)) {
                        continue;
                    }

                    PoolElementStructurePiece newPiece =
                            new PoolElementStructurePiece(
                                    templateManager,
                                    candidate,
                                    candidateOrigin,
                                    candidate.getGroundLevelDelta(),
                                    rotation,
                                    candidateBox,
                                    liquidSettings
                            );

                    pieces.add(newPiece);
                    occupied.add(AABB.of(candidateBox));
                    tracker.onPlaced(candidate);

                    if (childDepth < layout.maxDepth()
                            && pieces.size() < layout.maxPieces()) {
                        queue.addLast(
                                new QueueEntry(
                                        newPiece,
                                        childDepth
                                )
                        );
                    }

                    return true;
                }
            }

            return false;
        }

        private boolean insideStructureRadius(BoundingBox box) {
            int minX = structureCenter.getX() - maxDistanceFromCenter;
            int maxX = structureCenter.getX() + maxDistanceFromCenter;
            int minZ = structureCenter.getZ() - maxDistanceFromCenter;
            int maxZ = structureCenter.getZ() + maxDistanceFromCenter;

            return box.minX() >= minX
                    && box.maxX() <= maxX
                    && box.minZ() >= minZ
                    && box.maxZ() <= maxZ;
        }

        private boolean collides(BoundingBox candidateBox) {
            AABB candidateAABB = AABB.of(candidateBox).deflate(0.25D);

            for (AABB existing : occupied) {
                if (existing.intersects(candidateAABB)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static final class RequirementTracker {

        private final Map<ResourceLocation, BTEJigsawRules.Requirement> requirements;
        private final Map<ResourceLocation, Integer> counts;

        private RequirementTracker(
                List<BTEJigsawRules.Requirement> requirementList
        ) {
            this.requirements = new HashMap<>();
            this.counts = new HashMap<>();

            for (BTEJigsawRules.Requirement requirement : requirementList) {
                requirements.put(
                        requirement.component(),
                        requirement
                );

                counts.put(
                        requirement.component(),
                        0
                );
            }
        }

        private void onPlaced(StructurePoolElement element) {
            ResourceLocation component = getComponent(element);

            if (component == null
                    || !counts.containsKey(component)) {
                return;
            }

            counts.merge(
                    component,
                    1,
                    Integer::sum
            );
        }

        private boolean canPlace(
                StructurePoolElement element,
                int depth
        ) {
            ResourceLocation component = getComponent(element);

            if (component == null) {
                return true;
            }

            BTEJigsawRules.Requirement requirement =
                    requirements.get(component);

            if (requirement == null) {
                return true;
            }

            if (depth < requirement.minDepth()
                    || depth > requirement.maxDepth()) {
                return false;
            }

            return counts.getOrDefault(component, 0)
                    < requirement.maxCount();
        }

        private boolean isRequiredNow(
                StructurePoolElement element,
                int depth
        ) {
            ResourceLocation component = getComponent(element);

            if (component == null) {
                return false;
            }

            BTEJigsawRules.Requirement requirement =
                    requirements.get(component);

            if (requirement == null) {
                return false;
            }

            if (depth < requirement.minDepth()
                    || depth > requirement.maxDepth()) {
                return false;
            }

            return counts.getOrDefault(component, 0)
                    < requirement.minCount();
        }

        private int requiredPriority(
                StructurePoolElement element
        ) {
            ResourceLocation component = getComponent(element);

            if (component == null) {
                return Integer.MIN_VALUE;
            }

            BTEJigsawRules.Requirement requirement =
                    requirements.get(component);

            return requirement == null
                    ? Integer.MIN_VALUE
                    : requirement.priority();
        }

        private boolean allRequirementsSatisfied() {
            for (BTEJigsawRules.Requirement requirement : requirements.values()) {
                int count = counts.getOrDefault(
                        requirement.component(),
                        0
                );

                if (count < requirement.minCount()
                        || count > requirement.maxCount()) {
                    return false;
                }
            }

            return true;
        }
    }

    private static ResourceLocation getComponent(
            StructurePoolElement element
    ) {
        if (element instanceof BTEComponentPoolElement componentElement) {
            return componentElement.component();
        }

        return null;
    }

    private static boolean insideWorldHeight(
            WorldGenLevel level,
            BoundingBox box
    ) {
        return box.minY() >= level.getMinBuildHeight()
                && box.maxY() < level.getMaxBuildHeight();
    }

    private static long createAttemptSeed(
            long worldSeed,
            BlockPos start,
            int attempt
    ) {
        ChunkPos chunkPos = new ChunkPos(start);

        long seed = worldSeed;
        seed ^= (long) chunkPos.x * 341873128712L;
        seed ^= (long) chunkPos.z * 132897987541L;
        seed ^= (long) attempt * 42317861L;
        return seed;
    }
}
