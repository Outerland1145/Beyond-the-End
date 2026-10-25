package org.starrepublic.beyond_the_end.World.DFM;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.Vec3;
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
import org.starrepublic.beyond_the_end.Register.FeatureRegister;
import org.starrepublic.beyond_the_end.World.worldgen.feature.BTEEndGatewayFeature;
import org.starrepublic.beyond_the_end.World.worldgen.feature.BTEEndPodiumFeature;
import org.starrepublic.beyond_the_end.World.worldgen.feature.BTEEndSpikeLayout;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

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
    private final int gatewayCount;
    private final int gatewayRadius;
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
    private BTEDragonRespawnStage respawnStage;

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
        BTEEndFightSettingsSavedData settings = BTEEndFightSettingsSavedData.get(level);
        this.gatewayCount = settings.getGatewayCount();
        this.gatewayRadius = settings.getGatewayRadius();

        this.gateways = new ArrayList<>();

        /*
         * 尚未首次擊殺終界龍：
         * 使用配置重新建立完整折躍門列表。
         *
         * 即使原版管理器預先建立了 20 個索引，
         * 這裡也會以 Beyond The End 的配置為準。
         */
        if (!data.previouslyKilled()
                || data.gateways().isEmpty()) {

            for (int i = 0; i < this.gatewayCount; i++) {
                this.gateways.add(i);
            }

            // 保留原版依世界種子決定生成順序的方式
            Util.shuffle(
                    this.gateways,
                    RandomSource.create(seed)
            );

        } else {

            /*
             * 已經擊殺過終界龍：
             * 保留世界存檔中尚未使用的索引。
             *
             * 不重新洗牌，也不補回已使用的折躍門。
             */
            this.gateways.addAll(
                    data.gateways().get()
            );
        }

        LOGGER.info(
                "End Gateway settings: count={}, radius={}, remaining={}",
                this.gatewayCount,
                this.gatewayRadius,
                this.gateways.size()
        );


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

            /*
             * 龍戰期間的哭泣黑曜石粒子雨。
             *
             * 每 5 Tick 生成一次。
             * 僅在終界龍仍然存活、玩家正在參與龍戰時生效。
             */
            if (this.level.getGameTime() % 5L == 0L
                    && this.dragonUUID != null
                    && this.level.getEntity(this.dragonUUID)
                    instanceof EnderDragon dragon
                    && dragon.isAlive()) {

                for (ServerPlayer player : this.bossEvent.getPlayers()) {

                    this.level.sendParticles(
                            player,
                            ParticleTypes.FALLING_OBSIDIAN_TEAR,
                            false,
                            player.getX(),
                            player.getY() + 11.0D,
                            player.getZ(),
                            22,
                            10.0D,
                            2.5D,
                            10.0D,
                            0.0D
                    );
                }
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

    @Override
    public void resetSpikeCrystals() {

        for (SpikeFeature.EndSpike spike :
                BTEEndSpikeLayout.getSpikes(this.level)) {

            List<EndCrystal> crystals =
                    this.level.getEntitiesOfClass(
                            EndCrystal.class,
                            spike.getTopBoundingBox()
                    );

            for (EndCrystal crystal : crystals) {

                crystal.setInvulnerable(false);
                crystal.setBeamTarget(null);
            }
        }
    }

    // =====================================================
    // 折躍門
    // =====================================================


    private void spawnNewGateway() {

        // 所有折躍門生成完畢，直接返回
        if (this.gateways.isEmpty()) {
            return;
        }

        // 取得下一個尚未使用的索引
        int index = this.gateways.getLast();

        // 防止無效索引被用於目前的環形配置
        if (index < 0 || index >= this.gatewayCount) {
            LOGGER.error(
                    "Invalid End Gateway index {} for configured count {}",
                    index,
                    this.gatewayCount
            );
            return;
        }

        /*
         * 依照折躍門總數等角度排列。
         *
         * 保留原版角度計算方式。
         */
        double angle = 2.0D * (
                -Math.PI
                        + (Math.PI / this.gatewayCount) * index
        );

        int x = Mth.floor(
                this.gatewayRadius * Math.cos(angle)
        );

        int z = Mth.floor(
                this.gatewayRadius * Math.sin(angle)
        );

        // 保留原版 Y=75 的生成高度
        BlockPos position = new BlockPos(
                this.origin.getX() + x,
                75,
                this.origin.getZ() + z
        );

        boolean success =
                gateway.place(
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

        // 只有成功生成，才消耗折躍門索引
        this.gateways.removeLast();

        this.level.levelEvent(
                3000,
                position,
                0
        );

        LOGGER.info(
                "End Gateway generated: index={}, position={}, remaining={}",
                index,
                position,
                this.gateways.size()
        );
    }


    // =====================================================
    // 水晶數量
    // =====================================================


    private void updateCrystalCount() {

        this.crystalsAlive = 0;

        for (SpikeFeature.EndSpike spike :
                BTEEndSpikeLayout.getSpikes(this.level)) {

            this.crystalsAlive +=
                    this.level.getEntitiesOfClass(
                            EndCrystal.class,
                            spike.getTopBoundingBox()
                    ).size();
        }
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
        this.respawnStage = BTEDragonRespawnStage.START;
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

        // 若儀式使用的水晶被破壞，立即中止。
        for (EndCrystal crystal : this.respawnCrystals) {

            if (crystal.isRemoved()) {
                this.abortRespawn();
                return;
            }
        }

        BTEDragonRespawnStage current =
                this.respawnStage;

        BTEDragonRespawnStage next = current.tick(
                this.level,
                this,
                this.respawnCrystals,
                this.respawnTime
        );

        if (next == BTEDragonRespawnStage.END) {
            this.finishRespawn();
            return;
        }

        if (next != current) {

            this.respawnStage = next;
            this.respawnTime = 0;

        } else {

            this.respawnTime++;
        }
    }
    /*
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
     */

    /**
     * 龍復活時重建指定的黑曜石柱。
     *
     * 使用 BTEEndSpikeFeature，而不是
     * 原版 Feature.END_SPIKE。
     */
    public boolean regenerateSpike(
            SpikeFeature.EndSpike spike,
            BlockPos beamTarget
    ) {
        /*
         * 先移除柱頂舊的水晶。
         * 避免重新生成時疊出兩顆水晶。
         */
        List<EndCrystal> oldCrystals =
                this.level.getEntitiesOfClass(
                        EndCrystal.class,
                        spike.getTopBoundingBox()
                );

        for (EndCrystal crystal : oldCrystals) {
            crystal.discard();
        }

        SpikeConfiguration configuration =
                new SpikeConfiguration(
                        true,
                        List.of(spike),
                        beamTarget
                );

        BlockPos position = new BlockPos(
                spike.getCenterX(),
                0,
                spike.getCenterZ()
        );

        boolean success =
                FeatureRegister.END_SPIKE.get().place(
                        configuration,
                        this.level,
                        this.level.getChunkSource().getGenerator(),
                        this.level.getRandom(),
                        position
                );

        if (!success) {

            LOGGER.error(
                    "Failed to regenerate End Spike at X={}, Z={}",
                    spike.getCenterX(),
                    spike.getCenterZ()
            );
        }

        return success;
    }

    private void finishRespawn() {


        /*
         * 記錄所有祭壇水晶的爆炸位置。
         *
         * 必須先移除全部水晶，再執行爆炸，
         * 避免相鄰水晶被爆炸連鎖引爆。
         */
        List<Vec3> explosionPositions = new ArrayList<>();

        for (EndCrystal crystal : this.respawnCrystals) {

            if (crystal.isRemoved()) {
                continue;
            }

            explosionPositions.add(crystal.position());

            crystal.setBeamTarget(null);
            crystal.setInvulnerable(false);
            crystal.discard();
        }

        /*
         * 清除黑曜石柱水晶的光束與無敵狀態。
         * 保留你之前修正光束問題的程式碼。
         */
        this.resetSpikeCrystals();

        /*
         * 祭壇水晶爆炸。
         */
        for (Vec3 pos : explosionPositions) {

            // 真正的爆炸
            this.level.explode(
                    null,
                    pos.x,
                    pos.y,
                    pos.z,
                    4.0F,
                    Level.ExplosionInteraction.NONE
            );

            // 紫色龍息能量
            this.level.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    pos.x, pos.y, pos.z,
                    65,
                    1.5, 1.5, 1.5,
                    0.15
            );

            // 向外擴散的終界傳送門粒子
            this.level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    pos.x, pos.y, pos.z,
                    45,
                    1.8, 1.8, 1.8,
                    0.15
            );

            // 爆炸瞬間的白色閃光
            this.level.sendParticles(
                    ParticleTypes.FLASH,
                    pos.x, pos.y, pos.z,
                    1,
                    0, 0, 0,
                    0
            );

            // 額外的發光粒子
            this.level.sendParticles(
                    ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z,
                    30,
                    1.5, 1.5, 1.5,
                    0.12
            );
        }

        this.resetSpikeCrystals();
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


        /*
         * 恢復原版「終界……再臨」進度。
         *
         * 只有復活成功後才觸發，
         * 不會因為首次生成終界龍而誤觸發。
         */
        double centerX = this.origin.getX() + 0.5;
        double centerY = this.origin.getY() + 128.0;
        double centerZ = this.origin.getZ() + 0.5;

        double advancementRadius = 192.0;

        for (ServerPlayer player : this.level.players()) {

            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            if (player.distanceToSqr(
                    centerX,
                    centerY,
                    centerZ
            ) > advancementRadius * advancementRadius) {
                continue;
            }

            CriteriaTriggers.SUMMONED_ENTITY.trigger(
                    player,
                    dragon
            );
        }


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


