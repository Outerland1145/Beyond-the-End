package org.starrepublic.beyond_the_end.World.DFM;

import net.minecraft.util.Unit;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import net.minecraft.world.level.dimension.end.DragonRespawnAnimation;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.AABB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import org.starrepublic.beyond_the_end.World.worldgen.feature.BTEEndGatewayFeature;
import org.starrepublic.beyond_the_end.World.worldgen.feature.BTEEndPodiumFeature;

public class BTEDragonFightManager extends EndDragonFight {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BTEDragonFightManager.class);

    private static final ResourceLocation ACTIVE_PORTAL =
            ResourceLocation.fromNamespaceAndPath(
                    "beyond_the_end",
                    "endpodium/end_portal_active"
            );
    private static final BTEEndPodiumFeature ACTIVE_PODIUM =
            new BTEEndPodiumFeature(true);

    private static final BTEEndPodiumFeature INACTIVE_PODIUM =
            new BTEEndPodiumFeature(false);
    private static final BTEEndGatewayFeature gateway = new BTEEndGatewayFeature();
    private static final int GATEWAY_COUNT = 20;
    private static final int GATEWAY_RADIUS = 96;
    private static final int DRAGON_SCAN_INTERVAL = 1200;
    private static final int CRYSTAL_SCAN_INTERVAL = 100;
    private static final int PLAYER_SCAN_INTERVAL = 20;

    private final ServerLevel level;
    private final BlockPos origin;
    private final ServerBossEvent bossEvent;
    private final ArrayList<Integer> gateways;

    private boolean dragonKilled;
    private boolean previouslyKilled;
    private boolean needsStateScanning;
    private boolean pendingRespawnRecovery;

    @Nullable
    private UUID dragonUUID;

    @Nullable
    private BlockPos portalLocation;

    @Nullable
    private DragonRespawnAnimation respawnStage;

    private List<EndCrystal> respawnCrystals;

    private int respawnTime;
    private int ticksSinceDragonSeen;
    private int ticksSinceCrystalScan;
    private int ticksSincePlayerScan;
    private int crystalsAlive;

    private boolean arenaTicketActive;

    public BTEDragonFightManager(
            ServerLevel level,
            long seed,
            EndDragonFight.Data data,
            BlockPos origin
    ) {
        /*
         * 父類保留為 ServerLevel 所要求的 EndDragonFight 型別。
         * 不執行父類的 tick / 生成 / 復活流程。
         *
         * 父類不需要管理自訂的復活動畫，
         * 因此傳入 isRespawning=false 的資料副本。
         */
        super(
                level,
                seed,
                new EndDragonFight.Data(
                        data.needsStateScanning(),
                        data.dragonKilled(),
                        data.previouslyKilled(),
                        false,
                        data.dragonUUID(),
                        data.exitPortalLocation(),
                        data.gateways()
                ),
                origin
        );

        this.level = level;
        this.origin = origin.immutable();

        this.dragonKilled = data.dragonKilled();
        this.previouslyKilled = data.previouslyKilled();
        this.needsStateScanning = data.needsStateScanning();

        this.dragonUUID =
                data.dragonUUID().orElse(null);

        this.portalLocation =
                data.exitPortalLocation()
                        .map(BlockPos::immutable)
                        .orElse(null);

        this.pendingRespawnRecovery = data.isRespawning();

        this.respawnStage = null;
        this.respawnCrystals = List.of();

        this.bossEvent = new ServerBossEvent(
                Component.translatable("entity.minecraft.ender_dragon"),
                BossEvent.BossBarColor.PINK,
                BossEvent.BossBarOverlay.PROGRESS
        );

        this.bossEvent.setPlayBossMusic(true);
        this.bossEvent.setCreateWorldFog(true);
        this.bossEvent.setVisible(!this.dragonKilled);

        this.gateways = new ArrayList<>();

        if (data.gateways().isPresent()) {

            this.gateways.addAll(
                    data.gateways().get()
            );

        } else {

            for (int i = 0; i < GATEWAY_COUNT; i++) {
                this.gateways.add(i);
            }

            Util.shuffle(
                    this.gateways,
                    RandomSource.create(seed)
            );
        }

        LOGGER.info(
                "BTE Dragon Fight initialized. Killed={}, Previous={}, Respawning={}",
                this.dragonKilled,
                this.previouslyKilled,
                this.pendingRespawnRecovery
        );
    }

    // =====================================================
    // 原版存檔格式
    // =====================================================

    @Override
    public EndDragonFight.Data saveData() {

        return new EndDragonFight.Data(
                this.needsStateScanning,
                this.dragonKilled,
                this.previouslyKilled,
                this.respawnStage != null ||
                        this.pendingRespawnRecovery,
                Optional.ofNullable(this.dragonUUID),
                Optional.ofNullable(this.portalLocation),
                Optional.of(List.copyOf(this.gateways))
        );
    }

    // =====================================================
    // 主更新流程
    // =====================================================

    @Override
    public void tick() {

        if (++this.ticksSincePlayerScan >= PLAYER_SCAN_INTERVAL) {

            this.updatePlayers();
            this.ticksSincePlayerScan = 0;
        }

        // 沒有玩家參與龍戰時，不強制載入整個競技場。
        if (this.bossEvent.getPlayers().isEmpty()) {

            this.updateArenaTicket(false);
            return;
        }

        this.updateArenaTicket(true);

        if (!this.isArenaLoaded()) {
            return;
        }

        if (this.needsStateScanning) {
            this.scanInitialState();
        }

        if (this.pendingRespawnRecovery) {
            this.recoverRespawn();
        }

        if (this.respawnStage != null) {
            this.tickRespawn();
            return;
        }

        if (!this.dragonKilled) {

            if (this.dragonUUID == null ||
                    ++this.ticksSinceDragonSeen >= DRAGON_SCAN_INTERVAL) {

                this.findOrCreateDragon();
                this.ticksSinceDragonSeen = 0;
            }

            if (++this.ticksSinceCrystalScan >= CRYSTAL_SCAN_INTERVAL) {

                this.updateCrystalCount();
                this.ticksSinceCrystalScan = 0;
            }
        }
    }

    private void updatePlayers() {

        Set<ServerPlayer> valid = new HashSet<>();

        for (ServerPlayer player : this.level.players()) {

            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            double dx = player.getX() - this.origin.getX();
            double dz = player.getZ() - this.origin.getZ();

            if (dx * dx + dz * dz <= 192.0 * 192.0) {
                valid.add(player);
                this.bossEvent.addPlayer(player);
            }
        }

        for (ServerPlayer player :
                List.copyOf(this.bossEvent.getPlayers())) {

            if (!valid.contains(player)) {
                this.bossEvent.removePlayer(player);
            }
        }

        this.bossEvent.setVisible(!this.dragonKilled);
    }

    @Override
    public void addPlayer(ServerPlayer player) {
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void removePlayer(ServerPlayer player) {
        this.bossEvent.removePlayer(player);
    }

    private void updateArenaTicket(boolean required) {

        if (this.arenaTicketActive == required) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(this.origin);

        if (required) {

            this.level.getChunkSource().addRegionTicket(
                    TicketType.DRAGON,
                    chunkPos,
                    ARENA_TICKET_LEVEL,
                    Unit.INSTANCE
            );

        } else {

            this.level.getChunkSource().removeRegionTicket(
                    TicketType.DRAGON,
                    chunkPos,
                    ARENA_TICKET_LEVEL,
                    Unit.INSTANCE
            );
        }

        this.arenaTicketActive = required;
    }

    private boolean isArenaLoaded() {

        ChunkPos center = new ChunkPos(this.origin);

        for (int x = center.x - 8; x <= center.x + 8; x++) {
            for (int z = center.z - 8; z <= center.z + 8; z++) {

                if (!this.level.hasChunk(x, z)) {
                    return false;
                }
            }
        }

        return true;
    }

    // =====================================================
    // 既有世界與新世界初始化
    // =====================================================

    private void scanInitialState() {

        this.needsStateScanning = false;

        this.ensurePortalLocation();

        List<? extends EnderDragon> dragons =
                this.level.getDragons();

        boolean activePortal = this.hasActivePortal();

        if (!dragons.isEmpty()) {

            EnderDragon dragon = dragons.getFirst();

            this.bindDragon(dragon);
            this.dragonKilled = false;

            if (activePortal) {
                this.previouslyKilled = true;
            }

        } else if (activePortal) {

            this.dragonKilled = true;
            this.previouslyKilled = true;

        } else {

            this.dragonKilled = false;

            if (this.portalLocation != null) {
                this.placePortal(false);
            }

            this.findOrCreateDragon();
        }

        this.bossEvent.setVisible(!this.dragonKilled);

        LOGGER.info(
                "BTE Dragon Fight scan finished: killed={}, previouslyKilled={}",
                this.dragonKilled,
                this.previouslyKilled
        );
    }

    private void ensurePortalLocation() {

        if (this.portalLocation != null) {
            return;
        }

        BlockPos surface = this.level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING,
                this.origin
        );

        this.portalLocation = surface.below().immutable();
    }

    private boolean hasActivePortal() {

        if (this.portalLocation == null) {
            return false;
        }

        BlockPos center = this.portalLocation;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-8, -4, -8),
                center.offset(8, 5, 8)
        )) {
            if (this.level.getBlockState(pos).is(Blocks.END_PORTAL)) {
                return true;
            }
        }

        return false;
    }

    // =====================================================
    // 終界龍管理
    // =====================================================

    private void findOrCreateDragon() {

        List<? extends EnderDragon> dragons =
                this.level.getDragons();

        if (!dragons.isEmpty()) {

            EnderDragon selected = null;

            if (this.dragonUUID != null) {
                for (EnderDragon dragon : dragons) {
                    if (dragon.getUUID().equals(this.dragonUUID)) {
                        selected = dragon;
                        break;
                    }
                }
            }

            if (selected == null) {
                selected = dragons.getFirst();
            }

            this.bindDragon(selected);
            return;
        }

        this.createNewDragon();
    }

    @Nullable
    private EnderDragon createNewDragon() {

        EnderDragon dragon = EntityType.ENDER_DRAGON.create(
                this.level
        );

        if (dragon == null) {
            LOGGER.error("Failed to create Ender Dragon");
            return null;
        }

        dragon.getPhaseManager().setPhase(
                EnderDragonPhase.HOLDING_PATTERN
        );

        dragon.moveTo(
                this.origin.getX() + 0.5,
                this.origin.getY() + DRAGON_SPAWN_Y,
                this.origin.getZ() + 0.5,
                this.level.random.nextFloat() * 360.0F,
                0.0F
        );

        dragon.setDragonFight(this);
        dragon.setFightOrigin(this.origin);

        if (!this.level.addFreshEntity(dragon)) {
            LOGGER.error("Failed to add Ender Dragon to world");
            return null;
        }

        this.bindDragon(dragon);

        LOGGER.info(
                "Spawned Ender Dragon: {}",
                dragon.getUUID()
        );

        return dragon;
    }

    private void bindDragon(EnderDragon dragon) {

        dragon.setDragonFight(this);
        dragon.setFightOrigin(this.origin);

        this.dragonUUID = dragon.getUUID();
        this.ticksSinceDragonSeen = 0;

        this.bossEvent.setProgress(
                Mth.clamp(
                        dragon.getHealth() / dragon.getMaxHealth(),
                        0.0F,
                        1.0F
                )
        );
    }

    @Override
    public void updateDragon(EnderDragon dragon) {

        if (this.dragonUUID == null ||
                !this.dragonUUID.equals(dragon.getUUID())) {
            return;
        }

        this.ticksSinceDragonSeen = 0;

        this.bossEvent.setProgress(
                Mth.clamp(
                        dragon.getHealth() / dragon.getMaxHealth(),
                        0.0F,
                        1.0F
                )
        );

        this.bossEvent.setName(dragon.getDisplayName());
    }

    @Override
    public void setDragonKilled(EnderDragon dragon) {

        if (this.dragonUUID == null ||
                !this.dragonUUID.equals(dragon.getUUID()) ||
                this.dragonKilled) {
            return;
        }

        this.bossEvent.setProgress(0.0F);
        this.bossEvent.setVisible(false);

        this.dragonKilled = true;
        this.dragonUUID = null;

        this.placePortal(true);
        this.spawnNewGateway();

        // 只在首次擊殺時生成龍蛋。
        if (!this.previouslyKilled) {

            BlockPos eggPos = this.level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING,
                    this.origin
            );

            this.level.setBlockAndUpdate(
                    eggPos,
                    Blocks.DRAGON_EGG.defaultBlockState()
            );
        }

        this.previouslyKilled = true;

        LOGGER.info("Ender Dragon killed");
    }

    // =====================================================
    // 返回祭壇
    // =====================================================

    private boolean placePortal(boolean active) {

        this.ensurePortalLocation();

        if (this.portalLocation == null) {
            return false;
        }

        return (active ? ACTIVE_PODIUM : INACTIVE_PODIUM).place(
                NoneFeatureConfiguration.INSTANCE,
                this.level,
                this.level.getChunkSource().getGenerator(),
                this.level.getRandom(),
                this.portalLocation
        );
    }

    // =====================================================
    // 折躍門
    // =====================================================

    private void spawnNewGateway() {

        if (this.gateways.isEmpty()) {
            return;
        }

        int index = this.gateways.getLast();

        double angle = 2.0 * (
                -Math.PI +
                        (Math.PI / GATEWAY_COUNT) * index
        );

        int x = Mth.floor(
                GATEWAY_RADIUS * Math.cos(angle)
        );

        int z = Mth.floor(
                GATEWAY_RADIUS * Math.sin(angle)
        );

        BlockPos position = this.origin.offset(
                x,
                75 - this.origin.getY(),
                z
        );


        boolean success = gateway.place(
                EndGatewayConfiguration.delayedExitSearch(),
                this.level,
                this.level.getChunkSource().getGenerator(),
                this.level.getRandom(),
                position
        );


        if (!success) {
            LOGGER.error(
                    "Failed to generate End Gateway at {}",
                    position
            );
            return;
        }

        this.gateways.removeLast();

        this.level.levelEvent(
                3000,
                position,
                0
        );
    }

    // =====================================================
    // 水晶數量
    // =====================================================

    private void updateCrystalCount() {

        AABB area = new AABB(
                this.origin.getX() - 160,
                this.level.getMinBuildHeight(),
                this.origin.getZ() - 160,
                this.origin.getX() + 160,
                this.level.getMaxBuildHeight(),
                this.origin.getZ() + 160
        );

        this.crystalsAlive =
                this.level.getEntitiesOfClass(
                        EndCrystal.class,
                        area
                ).size();
    }

    @Override
    public int getCrystalsAlive() {
        return this.crystalsAlive;
    }

    @Override
    public void onCrystalDestroyed(
            EndCrystal crystal,
            DamageSource damageSource
    ) {

        this.updateCrystalCount();

        if (this.respawnStage != null) {

            for (EndCrystal used : this.respawnCrystals) {

                if (used.getUUID().equals(crystal.getUUID())) {
                    this.abortRespawn();
                    return;
                }
            }
        }

        if (this.dragonUUID != null) {

            Entity entity =
                    this.level.getEntity(this.dragonUUID);

            if (entity instanceof EnderDragon dragon) {

                dragon.onCrystalDestroyed(
                        crystal,
                        crystal.blockPosition(),
                        damageSource
                );
            }
        }
    }

    // =====================================================
    // 自訂復活條件
    // =====================================================

    @Override
    public void tryRespawn() {

        if (!this.dragonKilled ||
                this.respawnStage != null ||
                this.pendingRespawnRecovery) {
            return;
        }

        this.ensurePortalLocation();

        if (this.portalLocation == null) {
            return;
        }

        /*
         * 找不到任何祭壇石時，嘗試恢復啟用祭壇。
         * 不在這裡將部分損壞的祭壇自動補齊。
         */
        if (!this.hasAltarStones()) {

            if (!this.placePortal(true)) {
                return;
            }
        }

        List<EndCrystal> crystals =
                this.findRespawnCrystals();

        if (crystals == null) {
            return;
        }

        this.startRespawn(crystals);
    }

    private boolean hasAltarStones() {

        return this.getAltarStonePositions() != null
                && !this.getAltarStonePositions().isEmpty();
    }

    /**
     * 尋找啟用祭壇結構範圍內的所有祭壇石。
     */
    @Nullable
    private List<BlockPos> getAltarStonePositions() {

        if (this.portalLocation == null) {
            return null;
        }

        Optional<StructureTemplate> optional =
                this.level.getStructureManager().get(
                        ACTIVE_PORTAL
                );

        if (optional.isEmpty()) {

            LOGGER.error(
                    "Cannot find active End Portal template: {}",
                    ACTIVE_PORTAL
            );
            return null;
        }

        StructureTemplate template = optional.get();

        int sizeX = template.getSize().getX();
        int sizeY = template.getSize().getY();
        int sizeZ = template.getSize().getZ();

        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0
                || sizeX % 2 == 0
                || sizeZ % 2 == 0
                || sizeX != sizeZ) {

            LOGGER.error("Invalid active End Portal template size");
            return null;
        }

        BlockPos start = this.portalLocation.offset(
                -sizeX / 2,
                -1,
                -sizeZ / 2
        );

        List<BlockPos> result = new ArrayList<>();

        for (BlockPos mutable : BlockPos.betweenClosed(
                start,
                start.offset(
                        sizeX - 1,
                        sizeY - 1,
                        sizeZ - 1
                )
        )) {

            if (this.level.getBlockState(mutable)
                    .is(BlockRegister.AltarStone.get())) {

                result.add(mutable.immutable());
            }
        }

        return result;
    }

    /**
     * 每塊祭壇石正上方必須有一顆獨立的終界水晶。
     *
     * 保留舊 Mixin 的 BlockPos 精確判定，
     * 避免一顆水晶同時符合多個 AABB。
     */
    @Nullable
    private List<EndCrystal> findRespawnCrystals() {

        List<BlockPos> altarStones =
                this.getAltarStonePositions();

        if (altarStones == null || altarStones.isEmpty()) {
            return null;
        }

        List<EndCrystal> result = new ArrayList<>();
        Set<UUID> used = new HashSet<>();

        for (BlockPos altar : altarStones) {

            BlockPos expectedPos = altar.above();

            List<EndCrystal> nearby =
                    this.level.getEntitiesOfClass(
                            EndCrystal.class,
                            new AABB(expectedPos)
                    );

            EndCrystal matched = null;

            for (EndCrystal crystal : nearby) {

                if (!crystal.isRemoved()
                        && crystal.blockPosition().equals(expectedPos)
                        && used.add(crystal.getUUID())) {

                    matched = crystal;
                    break;
                }
            }

            if (matched == null) {
                return null;
            }

            result.add(matched);
        }

        return result.size() == altarStones.size()
                ? result
                : null;
    }

    // =====================================================
    // 復活動畫
    // =====================================================

    private void startRespawn(List<EndCrystal> crystals) {

        if (crystals.isEmpty() || this.respawnStage != null) {
            return;
        }

        this.respawnCrystals = List.copyOf(crystals);
        this.respawnStage = DragonRespawnAnimation.START;
        this.respawnTime = 0;

        this.pendingRespawnRecovery = false;

        // 復活過程先切換成未啟用祭壇。
        if (!this.placePortal(false)) {

            this.respawnStage = null;
            this.respawnCrystals = List.of();
            return;
        }

        LOGGER.info(
                "Started dragon respawn with {} crystals",
                crystals.size()
        );
    }

    private void recoverRespawn() {

        this.pendingRespawnRecovery = false;

        if (!this.dragonKilled) {
            return;
        }

        List<EndCrystal> crystals =
                this.findRespawnCrystals();

        if (crystals == null) {

            LOGGER.warn(
                    "Could not recover Dragon respawn; restoring active portal"
            );

            this.placePortal(true);
            return;
        }

        this.startRespawn(crystals);
    }

    private void tickRespawn() {

        if (this.respawnStage == null) {
            return;
        }

        for (EndCrystal crystal : this.respawnCrystals) {

            if (crystal.isRemoved()) {
                this.abortRespawn();
                return;
            }
        }

        DragonRespawnAnimation current = this.respawnStage;

        current.tick(
                this.level,
                this,
                this.respawnCrystals,
                this.respawnTime++,
                this.origin
        );
    }

    @Override
    protected void setRespawnStage(
            DragonRespawnAnimation stage
    ) {

        if (this.respawnStage == null) {
            throw new IllegalStateException(
                    "Cannot change respawn stage when no respawn is active"
            );
        }

        this.respawnTime = 0;

        if (stage == DragonRespawnAnimation.END) {

            this.finishRespawn();
            return;
        }

        this.respawnStage = stage;

        LOGGER.debug(
                "Dragon respawn stage changed to {}",
                stage
        );
    }

    private void finishRespawn() {

        // 清理復活使用的水晶。
        for (EndCrystal crystal : this.respawnCrystals) {

            if (!crystal.isRemoved()) {
                crystal.setBeamTarget(null);
                crystal.setInvulnerable(false);
                crystal.discard();
            }
        }

        this.respawnCrystals = List.of();
        this.respawnStage = null;
        this.respawnTime = 0;

        this.dragonKilled = false;
        this.pendingRespawnRecovery = false;

        if (!this.placePortal(false)) {
            LOGGER.error(
                    "Could not place inactive portal after dragon respawn"
            );
        }

        EnderDragon dragon = this.createNewDragon();

        if (dragon == null) {
            this.dragonKilled = true;
            this.placePortal(true);
            return;
        }

        this.bossEvent.setVisible(true);

        LOGGER.info("Ender Dragon respawn completed");
    }

    private void abortRespawn() {

        if (this.respawnStage == null &&
                !this.pendingRespawnRecovery) {
            return;
        }

        for (EndCrystal crystal : this.respawnCrystals) {

            if (!crystal.isRemoved()) {
                crystal.setInvulnerable(false);
                crystal.setBeamTarget(null);
            }
        }

        this.respawnCrystals = List.of();
        this.respawnStage = null;
        this.respawnTime = 0;

        this.pendingRespawnRecovery = false;
        this.dragonKilled = true;

        this.resetSpikeCrystals();
        this.placePortal(true);

        LOGGER.info("Ender Dragon respawn interrupted");
    }

    // =====================================================
    // 原版介面相容
    // =====================================================

    @Override
    public boolean hasPreviouslyKilledDragon() {
        return this.previouslyKilled;
    }

    @Override
    @Nullable
    public UUID getDragonUUID() {
        return this.dragonUUID;
    }

    @Override
    public void removeAllGateways() {
        this.gateways.clear();
    }

    public boolean isDragonKilled() {
        return this.dragonKilled;
    }

    public boolean isRespawning() {
        return this.respawnStage != null ||
                this.pendingRespawnRecovery;
    }

    @Nullable
    public BlockPos getPortalLocation() {
        return this.portalLocation;
    }

    public BlockPos getFightOrigin() {
        return this.origin;
    }

    public ServerLevel getFightLevel() {
        return this.level;
    }

    public int getRemainingGateways() {
        return this.gateways.size();
    }

    public EndDragonFight.Data getFightData() {
        return this.saveData();
    }
}


