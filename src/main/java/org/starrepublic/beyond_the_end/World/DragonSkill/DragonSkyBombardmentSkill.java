package org.starrepublic.beyond_the_end.World.DragonSkill;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.starrepublic.beyond_the_end.Entitys.entity.DragonBombFireball;
import org.starrepublic.beyond_the_end.Register.EntityRegister;

public final class DragonSkyBombardmentSkill {

    private DragonSkyBombardmentSkill() {}

    // ===== 技能階段 =====

    private static final int IDLE = 0;
    private static final int ASCENDING = 1;
    private static final int TARGETING = 2;
    private static final int PLAYER_BARRAGE = 3;
    private static final int AREA_SWEEP = 4;
    private static final int DESCENDING = 5;

    // ===== 技能設定 =====

    private static final double HOVER_HEIGHT = 20.0D;
    private static final double ASCEND_SPEED = 1.15D;
    private static final double DESCEND_SPEED = 1.25D;

    private static final int TARGETING_TIME = 40;

    private static final int MIN_PLAYER_SHOTS = 20;
    private static final int MAX_PLAYER_SHOTS = 36;
    private static final int PLAYER_SHOT_INTERVAL = 8;

    private static final int SWEEP_SHOT_INTERVAL = 3;
    private static final int[] SWEEP_RADII = {
            10, 24, 38, 52, 66, 80, 94, 100
    };
    private static final double SWEEP_SPACING = 19.0D;

    private static final double TARGET_RANGE = 180.0D;
    private static final int MAX_NO_TARGET_TICKS = 160;

    // 技能完成後 900 Tick 才允許再次使用
    private static final int COOLDOWN = 900;

    // ===== NBT 狀態欄位 =====

    private static final String PREFIX = "bte_sky_bomb_";

    private static final String STAGE = PREFIX + "stage";
    private static final String TIMER = PREFIX + "timer";
    private static final String PORTAL_X = PREFIX + "portal_x";
    private static final String PORTAL_Y = PREFIX + "portal_y";
    private static final String PORTAL_Z = PREFIX + "portal_z";

    private static final String SHOTS_TOTAL = PREFIX + "shots_total";
    private static final String SHOTS_FIRED = PREFIX + "shots_fired";
    private static final String SWEEP_INDEX = PREFIX + "sweep_index";
    private static final String NO_TARGET = PREFIX + "no_target";
    private static final String NEXT_USE = PREFIX + "next_use";
    private static final String WAS_PERCHED = PREFIX + "was_perched";

    // ===== 事件註冊 =====

    public static void register() {
        NeoForge.EVENT_BUS.addListener(
                DragonSkyBombardmentSkill::onDragonTick
        );

        NeoForge.EVENT_BUS.addListener(
                DragonSkyBombardmentSkill::onInvulnerabilityCheck
        );

        NeoForge.EVENT_BUS.addListener(
                DragonSkyBombardmentSkill::onIncomingDamage
        );
    }

    /**
     * 其他技能亦可呼叫此方法，確認本技能是否正在執行。
     */
    public static boolean isActive(EnderDragon dragon) {
        return dragon.getPersistentData().getInt(STAGE) != IDLE;
    }

    // ===== 主控制器 =====

    private static void onDragonTick(EntityTickEvent.Post event) {

        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        if (!(dragon.level() instanceof ServerLevel level)) {
            return;
        }

        CompoundTag data = dragon.getPersistentData();

        if (!dragon.isAlive() || dragon.isRemoved()) {
            data.putInt(STAGE, IDLE);
            return;
        }

        int stage = data.getInt(STAGE);

        // 沒有執行技能：等待原版龍進入棲息觀察
        if (stage == IDLE) {
            checkActivation(level, dragon, data);
            return;
        }

        // 技能執行期間，持續保持在原版 HOVERING 階段
        if (dragon.getPhaseManager()
                .getCurrentPhase()
                .getPhase() != EnderDragonPhase.HOVERING) {

            dragon.getPhaseManager().setPhase(
                    EnderDragonPhase.HOVERING
            );
        }

        // 禁止原版飛行慣性干擾技能移動
        dragon.setDeltaMovement(Vec3.ZERO);

        int timer = data.getInt(TIMER) + 1;
        data.putInt(TIMER, timer);

        switch (stage) {
            case ASCENDING -> tickAscending(level, dragon, data);
            case TARGETING -> tickTargeting(level, dragon, data);
            case PLAYER_BARRAGE -> tickPlayerBarrage(
                    level, dragon, data
            );
            case AREA_SWEEP -> tickAreaSweep(
                    level, dragon, data
            );
            case DESCENDING -> tickDescending(
                    level, dragon, data
            );
            default -> {
                data.putInt(STAGE, DESCENDING);
                data.putInt(TIMER, 0);
            }
        }
    }

    // ===== 技能觸發 =====

    private static void checkActivation(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {
        DragonPhaseInstance phase =
                dragon.getPhaseManager().getCurrentPhase();

        EnderDragonPhase<?> phaseType = phase.getPhase();

        boolean perched =
                phaseType == EnderDragonPhase.SITTING_SCANNING
                        || phaseType == EnderDragonPhase.SITTING_ATTACKING
                        || phaseType == EnderDragonPhase.SITTING_FLAMING;

        if (!perched) {
            data.putBoolean(WAS_PERCHED, false);
            return;
        }

        // 每一次棲息只允許判定一次
        if (data.getBoolean(WAS_PERCHED)) {
            return;
        }

        data.putBoolean(WAS_PERCHED, true);

        if (phaseType != EnderDragonPhase.SITTING_SCANNING) {
            return;
        }

        if (level.getGameTime() < data.getLong(NEXT_USE)) {
            return;
        }

        // 沒有玩家時不啟動技能
        if (findTarget(level, dragon) == null) {
            return;
        }

        startSkill(level, dragon, data);
    }

    private static void startSkill(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {
        // 使用龍戰原點，而不是固定世界 (0,0)
        BlockPos portalCenter =
                EndPodiumFeature.getLocation(
                        dragon.getFightOrigin()
                );

        BlockPos surface = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                portalCenter
        );

        data.putInt(PORTAL_X, portalCenter.getX());
        data.putInt(PORTAL_Y, surface.getY());
        data.putInt(PORTAL_Z, portalCenter.getZ());

        int shotCount = MIN_PLAYER_SHOTS
                + level.getRandom().nextInt(
                MAX_PLAYER_SHOTS - MIN_PLAYER_SHOTS + 1
        );

        data.putInt(SHOTS_TOTAL, shotCount);
        data.putInt(SHOTS_FIRED, 0);
        data.putInt(SWEEP_INDEX, 0);
        data.putInt(NO_TARGET, 0);

        setStage(data, ASCENDING);

        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.getPhaseManager().setPhase(
                EnderDragonPhase.HOVERING
        );

        level.playSound(
                null,
                dragon.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE,
                5.0F,
                0.65F
        );
    }

    // ===== 第一階段：升空 =====

    private static void tickAscending(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {
        Vec3 target = getHoverPosition(data);

        Player player = findTarget(level, dragon);

        if (player != null) {
            faceTarget(dragon, player.getBoundingBox().getCenter());
        }

        boolean arrived = moveTowards(
                dragon,
                target,
                ASCEND_SPEED
        );

        if (arrived) {
            dragon.setDeltaMovement(Vec3.ZERO);
            setStage(data, TARGETING);
        }
    }

    // ===== 第二階段：空中鎖定 =====

    private static void tickTargeting(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {
        holdPosition(dragon, getHoverPosition(data));

        Player player = findTarget(level, dragon);

        if (player != null) {
            faceTarget(
                    dragon,
                    player.getBoundingBox().getCenter()
            );
        }

        // 蓄力粒子
        if (data.getInt(TIMER) % 4 == 0) {
            Vec3 pos = dragon.position();

            level.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    pos.x,
                    pos.y + 2.0D,
                    pos.z,
                    18,
                    3.0D,
                    1.5D,
                    3.0D,
                    0.04D
            );
        }

        if (data.getInt(TIMER) >= TARGETING_TIME) {
            setStage(data, PLAYER_BARRAGE);
        }
    }

    // ===== 第三階段：集中轟炸玩家 =====

    private static void tickPlayerBarrage(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {
        holdPosition(dragon, getHoverPosition(data));

        Player player = findTarget(level, dragon);

        if (player == null) {
            int missing = data.getInt(NO_TARGET) + 1;
            data.putInt(NO_TARGET, missing);

            // 玩家離開後不會永遠卡在這個階段
            if (missing >= MAX_NO_TARGET_TICKS) {
                setStage(data, AREA_SWEEP);
            }

            return;
        }

        data.putInt(NO_TARGET, 0);

        Vec3 playerCenter = player.getBoundingBox().getCenter();

        // 每 Tick 更新龍的朝向
        faceTarget(dragon, playerCenter);

        if (data.getInt(TIMER) % PLAYER_SHOT_INTERVAL != 0) {
            return;
        }

        Vec3 muzzle = getMuzzlePosition(dragon);

        double distance = muzzle.distanceTo(playerCenter);
        double leadTime = Math.min(
                distance / DragonBombFireball.FLIGHT_SPEED,
                10.0D
        );

        Vec3 predicted = playerCenter.add(
                player.getDeltaMovement().scale(
                        leadTime * 0.65D
                )
        );

        // 玩家周圍的轟炸散布
        Vec3 aim = predicted.add(
                level.getRandom().nextGaussian() * 1.6D,
                level.getRandom().nextGaussian() * 0.45D,
                level.getRandom().nextGaussian() * 1.6D
        );

        fireBomb(level, dragon, aim);

        int fired = data.getInt(SHOTS_FIRED) + 1;
        data.putInt(SHOTS_FIRED, fired);

        if (fired >= data.getInt(SHOTS_TOTAL)) {
            setStage(data, AREA_SWEEP);
        }
    }

    // ===== 第四階段：10～100 格環形掃射 =====

    private static void tickAreaSweep(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {
        holdPosition(dragon, getHoverPosition(data));

        if (data.getInt(TIMER) % SWEEP_SHOT_INTERVAL != 0) {
            return;
        }

        int globalIndex = data.getInt(SWEEP_INDEX);
        Vec3 aim = calculateSweepTarget(level, data, globalIndex);

        // 所有掃射位置都已經射擊完成
        if (aim == null) {
            setStage(data, DESCENDING);
            return;
        }

        faceTarget(dragon, aim);
        fireBomb(level, dragon, aim);

        data.putInt(SWEEP_INDEX, globalIndex + 1);
    }

    /**
     * 由內向外的同心圓掃射。
     *
     * 半徑：10、24、38、52、66、80、94、100。
     *
     * 每一圈依照圓周長自動計算火球數量。
     * 相鄰兩圈交錯角度，避免全部排成放射直線。
     */
    private static Vec3 calculateSweepTarget(
            ServerLevel level,
            CompoundTag data,
            int globalIndex
    ) {
        int index = globalIndex;

        for (int ring = 0; ring < SWEEP_RADII.length; ring++) {

            int radius = SWEEP_RADII[ring];

            int count = Math.max(
                    8,
                    Mth.ceil(
                            2.0D * Math.PI * radius
                                    / SWEEP_SPACING
                    )
            );

            if (index >= count) {
                index -= count;
                continue;
            }

            double offset = ring % 2 == 0
                    ? 0.0D
                    : Math.PI / count;

            double angle =
                    (2.0D * Math.PI * index / count) + offset;

            int x = Mth.floor(
                    data.getInt(PORTAL_X)
                            + Math.cos(angle) * radius
            );

            int z = Mth.floor(
                    data.getInt(PORTAL_Z)
                            + Math.sin(angle) * radius
            );

            BlockPos surface = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, 0, z)
            );

            return new Vec3(
                    x + 0.5D,
                    surface.getY() + 0.25D,
                    z + 0.5D
            );
        }

        return null;
    }

    // ===== 第五階段：返回棲息位置 =====

    private static void tickDescending(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {

        //dragon.setDeltaMovement(Vec3.ZERO);

        // 結束技能，解除無敵
        data.putInt(STAGE, IDLE);
        data.putInt(TIMER, 0);

        data.putLong(
                NEXT_USE,
                level.getGameTime() + COOLDOWN
        );

        data.putBoolean(WAS_PERCHED, true);

        // 返回原版正常棲息觀察階段
        dragon.getPhaseManager().setPhase(
                EnderDragonPhase.HOLDING_PATTERN
        );
    }

    // ===== 移動控制 =====

    private static Vec3 getHoverPosition(CompoundTag data) {
        return new Vec3(
                data.getInt(PORTAL_X),
                data.getInt(PORTAL_Y) + HOVER_HEIGHT,
                data.getInt(PORTAL_Z)
        );
    }

    private static Vec3 getPerchPosition(CompoundTag data) {
        return new Vec3(
                data.getInt(PORTAL_X),
                data.getInt(PORTAL_Y),
                data.getInt(PORTAL_Z)
        );
    }

    /**
     * 技能專用移動。
     *
     * 不修改原版 AI 的通用飛行公式。
     */
    private static boolean moveTowards(
            EnderDragon dragon,
            Vec3 target,
            double speed
    ) {
        Vec3 difference = target.subtract(dragon.position());

        double distance = difference.length();

        if (distance <= speed) {
            dragon.setPos(target.x, target.y, target.z);
            dragon.setDeltaMovement(Vec3.ZERO);
            return true;
        }

        Vec3 movement = difference.scale(speed / distance);

        dragon.setPos(
                dragon.getX() + movement.x,
                dragon.getY() + movement.y,
                dragon.getZ() + movement.z
        );

        dragon.setDeltaMovement(Vec3.ZERO);

        return false;
    }

    private static void holdPosition(
            EnderDragon dragon,
            Vec3 position
    ) {
        dragon.setPos(position.x, position.y, position.z);
        dragon.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * 持續讓龍的頭部正面朝向攻擊目標。
     *
     * 原版龍的頭部朝向跟隨身體 Y 軸旋轉，
     * 因此同時更新身體、頭部與實體角度。
     */
    private static void faceTarget(
            EnderDragon dragon,
            Vec3 target
    ) {
        double dx = target.x - dragon.getX();
        double dz = target.z - dragon.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            return;
        }

        float yaw = Mth.wrapDegrees(
                (float) Math.toDegrees(
                        Math.atan2(dx, -dz)
                )
        );

        dragon.setYRot(yaw);
        dragon.setYBodyRot(yaw);
        dragon.setYHeadRot(yaw);
    }

    // ===== 火球發射 =====

    /**
     * 取得高空轟炸的火球發射點。
     *
     * 根據龍目前的位置和旋轉計算，
     * 不使用可能尚未更新的頭部碰撞箱。
     */
    private static Vec3 getMuzzlePosition(EnderDragon dragon) {

        Vec3 headCenter = dragon.head.getBoundingBox().getCenter();

        // 直接使用龍目前朝向作為前方方向
        float yawRad = dragon.getYRot() * ((float) Math.PI / 180.0F);

        Vec3 forward = new Vec3(
                -Mth.sin(yawRad),
                0.0D,
                Mth.cos(yawRad)
        ).normalize();

        // 火球從龍頭前方一小段距離、略低的位置發射
        return headCenter
                .add(forward.scale(2.2D))
                .add(0.0D, -0.35D, 0.0D);
    }

    private static void fireBomb(
            ServerLevel level,
            EnderDragon dragon,
            Vec3 target
    ) {
        Vec3 muzzle = getMuzzlePosition(dragon);
        Vec3 direction = target.subtract(muzzle);

        if (direction.lengthSqr() < 0.0001D) {
            return;
        }

        DragonBombFireball fireball = new DragonBombFireball(
                EntityRegister.DRAGON_BOMB_FIREBALL.get(),
                level
        );

        fireball.launch(
                dragon,
                muzzle,
                direction
        );

        level.addFreshEntity(fireball);

        level.playSound(
                null,
                muzzle.x,
                muzzle.y,
                muzzle.z,
                SoundEvents.GHAST_SHOOT,
                SoundSource.HOSTILE,
                1.8F,
                0.65F + level.getRandom().nextFloat() * 0.2F
        );
    }

    // ===== 玩家搜尋 =====

    private static Player findTarget(
            ServerLevel level,
            EnderDragon dragon
    ) {
        Player nearest = null;
        double bestDistance = TARGET_RANGE * TARGET_RANGE;

        for (ServerPlayer player : level.players()) {

            if (!player.isAlive()
                    || player.isSpectator()) {
                continue;
            }

            double distance = dragon.distanceToSqr(player);

            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    // ===== 階段管理 =====

    private static void setStage(
            CompoundTag data,
            int stage
    ) {
        data.putInt(STAGE, stage);
        data.putInt(TIMER, 0);
    }

    // ===== 無敵處理 =====

    /**
     * 適用於龍本體與所有分段實體。
     */
    private static boolean isProtected(Entity entity) {

        if (entity instanceof EnderDragon dragon) {
            return isActive(dragon);
        }

        if (entity instanceof EnderDragonPart part) {
            EnderDragon parent = part.getParent();

            return parent != null && isActive(parent);
        }

        return false;
    }

    /**
     * 在原版傷害處理前，直接判定為無敵。
     */
    private static void onInvulnerabilityCheck(
            EntityInvulnerabilityCheckEvent event
    ) {
        if (isProtected(event.getEntity())) {
            event.setInvulnerable(true);
        }
    }

    /**
     * 額外攔截一般 LivingEntity 傷害流程。
     */
    private static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        if (isProtected(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}

