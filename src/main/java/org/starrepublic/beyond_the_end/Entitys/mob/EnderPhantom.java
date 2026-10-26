package org.starrepublic.beyond_the_end.Entitys.mob;
import org.starrepublic.beyond_the_end.Register.helper.EntityHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class EnderPhantom extends Phantom {

    // =====================================================
    // 統一實體註冊
    // =====================================================

    public static final EntityHelper.MobSpec<EnderPhantom> SPEC =
            new EntityHelper.MobSpec<>(
                    EnderPhantom::new,
                    MobCategory.MONSTER,
                    0.9F,
                    0.5F,
                    EnderPhantom::createAttributes,
                    SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> false,
                    0x24163D,
                    0xB34AFF
            );

    // =====================================================
    // 基礎參數
    // =====================================================

    // 90 秒
    public static final int LIFE_TIME = 1800;

    // 最高目標速度：2 格/Tick
    public static final double FLIGHT_SPEED = 2.0D;

    // 無目標時的巡航速度比例
    private static final double IDLE_SPEED = 0.35D;

    private static final double TARGET_RANGE = 128.0D;

    private static final int ATTACK_INTERVAL = 20;
    private static final int TARGET_SEARCH_INTERVAL = 10;

    private static final String NBT_SUMMONER = "BTESummoner";
    private static final String NBT_EXPIRE_TIME = "BTEExpireTime";
    private static final String NBT_ATTACK_COOLDOWN =
            "BTEAttackCooldown";

    // =====================================================
    // 狀態
    // =====================================================

    @Nullable
    private UUID summonerUUID;

    private long expireGameTime = -1L;

    private int attackCooldown;

    // 待機飛行中心
    @Nullable
    private Vec3 idleCenter;

    // 待機飛行目標
    @Nullable
    private Vec3 idleDestination;

    private int idleRetargetTicks;

    // =====================================================
    // 建構
    // =====================================================

    public EnderPhantom(
            EntityType<? extends Phantom> type,
            Level level
    ) {
        super(type, level);

        // 使用新的飛行控制器
        this.moveControl = new EnderPhantomMoveControl(this);

        this.xpReward = 0;

        this.attackCooldown = 0;
        this.idleRetargetTicks = 0;
    }

    // =====================================================
    // Attributes
    // =====================================================

    public static AttributeSupplier.Builder createAttributes() {

        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.8D)
                .add(Attributes.FOLLOW_RANGE, TARGET_RANGE);
    }

    // =====================================================
    // AI
    // =====================================================

    @Override
    protected void registerGoals() {

        // 不使用原版 Phantom 的盤旋、等待及俯衝 AI
        this.goalSelector.addGoal(
                1,
                new EnderPhantomFlightGoal(this)
        );
    }

    // =====================================================
    // 飛行物理：重要修正
    // =====================================================

    /**
     * 直接處理由 MoveControl 設定的飛行速度。
     *
     * 不依賴原版 Phantom 的 moveTargetPoint。
     *
     * 使用 Entity.move() 執行真正的碰撞與位移，
     * 避免只有速度更新而沒有正常移動。
     */
    @Override
    public void travel(Vec3 travelVector) {

        if (this.isEffectiveAi()) {

            Vec3 velocity = this.getDeltaMovement();

            // 執行實際位移，保留方塊碰撞
            this.move(MoverType.SELF, velocity);

            // 輕微空氣阻力
            this.setDeltaMovement(
                    this.getDeltaMovement().scale(0.91D)
            );

        } else {

            // 客戶端沿用原版旅行邏輯
            super.travel(travelVector);
        }
    }

    // =====================================================
    // Tick
    // =====================================================

    @Override
    public void tick() {

        super.tick();

        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        if (this.isRemoved()) {
            return;
        }

        if (this.idleCenter == null) {
            this.idleCenter = this.position();
        }

        if (this.expireGameTime < 0L) {
            this.expireGameTime =
                    level.getGameTime() + LIFE_TIME;
        }

        // 90秒到期
        if (level.getGameTime() >= this.expireGameTime) {
            this.discard();
            return;
        }

        // 召喚者死亡時，清除夜魅
        if (this.summonerUUID != null) {

            Entity owner = level.getEntity(this.summonerUUID);

            if (owner instanceof EnderDragon dragon
                    && (!dragon.isAlive() || dragon.isRemoved())) {

                this.discard();
                return;
            }
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.tickCount % 6 == 0) {

            level.sendParticles(
                    ParticleTypes.PORTAL,
                    this.getX(),
                    this.getY() + 0.3D,
                    this.getZ(),
                    3,
                    0.4D,
                    0.2D,
                    0.4D,
                    0.025D
            );
        }
    }

    // =====================================================
    // 召喚者
    // =====================================================

    public void setSummoner(EnderDragon dragon) {

        this.summonerUUID = dragon.getUUID();

        if (this.level() instanceof ServerLevel level) {
            this.expireGameTime =
                    level.getGameTime() + LIFE_TIME;
        }
    }

    @Nullable
    public UUID getSummonerUUID() {
        return this.summonerUUID;
    }

    @Nullable
    public EnderDragon getSummoner() {

        if (this.summonerUUID == null) {
            return null;
        }

        if (!(this.level() instanceof ServerLevel level)) {
            return null;
        }

        Entity entity = level.getEntity(this.summonerUUID);

        if (entity instanceof EnderDragon dragon
                && dragon.isAlive()
                && !dragon.isRemoved()) {

            return dragon;
        }

        return null;
    }

    // =====================================================
    // 玩家搜尋
    // =====================================================

    private boolean isValidTarget(@Nullable Player player) {

        return player != null
                && player.isAlive()
                && !player.isRemoved()
                && !player.isCreative()
                && !player.isSpectator()
                && player.level() == this.level()
                && this.distanceToSqr(player)
                <= TARGET_RANGE * TARGET_RANGE;
    }

    @Nullable
    private Player findNearestPlayer(ServerLevel level) {

        Player nearest = null;
        double bestDistance = TARGET_RANGE * TARGET_RANGE;

        for (ServerPlayer player : level.players()) {

            if (!this.isValidTarget(player)) {
                continue;
            }

            double distance = this.distanceToSqr(player);

            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    // =====================================================
    // 待機巡航目標
    // =====================================================

    private Vec3 getIdleDestination() {

        if (this.idleCenter == null) {
            this.idleCenter = this.position();
        }

        this.idleRetargetTicks--;

        if (this.idleDestination != null
                && this.idleRetargetTicks > 0
                && this.distanceToSqr(this.idleDestination) > 9.0D) {

            return this.idleDestination;
        }

        double angle = this.getRandom().nextDouble()
                * Math.PI * 2.0D;

        double radius = 8.0D
                + this.getRandom().nextDouble() * 12.0D;

        double x = this.idleCenter.x
                + Math.cos(angle) * radius;

        double y = this.idleCenter.y
                + this.getRandom().nextDouble() * 12.0D
                - 4.0D;

        double z = this.idleCenter.z
                + Math.sin(angle) * radius;

        // 防止待機目標超出世界高度
        y = Mth.clamp(
                y,
                this.level().getMinBuildHeight() + 5.0D,
                this.level().getMaxBuildHeight() - 5.0D
        );

        this.idleDestination = new Vec3(x, y, z);

        this.idleRetargetTicks = 40
                + this.getRandom().nextInt(41);

        return this.idleDestination;
    }

    // =====================================================
    // 飛行 Goal
    // =====================================================

    private static final class EnderPhantomFlightGoal
            extends Goal {

        private final EnderPhantom phantom;

        private int searchCooldown = 0;

        private EnderPhantomFlightGoal(
                EnderPhantom phantom
        ) {
            this.phantom = phantom;

            this.setFlags(
                    EnumSet.of(
                            Flag.MOVE,
                            Flag.LOOK
                    )
            );
        }

        @Override
        public boolean canUse() {
            return this.phantom.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.phantom.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {

            if (!(this.phantom.level() instanceof ServerLevel level)) {
                return;
            }

            this.searchCooldown--;

            Player target =
                    this.phantom.getTarget() instanceof Player player
                            ? player
                            : null;

            if (this.searchCooldown <= 0
                    || !this.phantom.isValidTarget(target)) {

                this.searchCooldown = TARGET_SEARCH_INTERVAL;

                target = this.phantom.findNearestPlayer(level);

                this.phantom.setTarget(target);
            }

            // =============================================
            // 有玩家：追擊
            // =============================================

            if (this.phantom.isValidTarget(target)) {

                Vec3 center = target.getBoundingBox().getCenter();

                // 預測玩家移動方向
                Vec3 predicted = center.add(
                        target.getDeltaMovement().scale(2.0D)
                );

                this.phantom.getMoveControl()
                        .setWantedPosition(
                                predicted.x,
                                predicted.y,
                                predicted.z,
                                1.0D
                        );

                this.phantom.getLookControl()
                        .setLookAt(
                                target,
                                30.0F,
                                30.0F
                        );

                // 近戰
                if (this.phantom.attackCooldown <= 0
                        && this.phantom.hasLineOfSight(target)) {

                    boolean touching = this.phantom
                            .getBoundingBox()
                            .expandTowards(
                                    this.phantom.getDeltaMovement()
                            )
                            .inflate(0.35D)
                            .intersects(target.getBoundingBox());

                    if (touching
                            && this.phantom.doHurtTarget(target)) {

                        this.phantom.attackCooldown =
                                ATTACK_INTERVAL;
                    }
                }

                return;
            }

            // =============================================
            // 沒有玩家：待機飛行
            // =============================================

            Vec3 idleTarget = this.phantom.getIdleDestination();

            this.phantom.getMoveControl()
                    .setWantedPosition(
                            idleTarget.x,
                            idleTarget.y,
                            idleTarget.z,
                            IDLE_SPEED
                    );
        }
    }

    // =====================================================
    // 飛行 MoveControl
    // =====================================================

    private static final class EnderPhantomMoveControl
            extends MoveControl {

        private final EnderPhantom phantom;

        private EnderPhantomMoveControl(
                EnderPhantom phantom
        ) {
            super(phantom);
            this.phantom = phantom;
        }

        @Override
        public void tick() {

            Vec3 oldVelocity = this.phantom.getDeltaMovement();

            if (!this.hasWanted()
                    || this.speedModifier <= 0.0D) {

                this.phantom.setDeltaMovement(
                        oldVelocity.scale(0.8D)
                );

                return;
            }

            Vec3 difference = new Vec3(
                    this.wantedX - this.phantom.getX(),
                    this.wantedY - this.phantom.getY(),
                    this.wantedZ - this.phantom.getZ()
            );

            double distance = difference.length();

            if (distance < 0.35D) {

                this.phantom.setDeltaMovement(
                        oldVelocity.scale(0.65D)
                );

                return;
            }

            // 計算目標速度
            double speed = FLIGHT_SPEED * this.speedModifier;

            Vec3 desiredVelocity = difference
                    .scale(speed / distance);

            // 加速並保留少量慣性
            Vec3 newVelocity = oldVelocity
                    .scale(0.2D)
                    .add(
                            desiredVelocity.scale(0.8D)
                    );

            // 最高速度限制
            if (newVelocity.lengthSqr() > speed * speed) {
                newVelocity = newVelocity
                        .normalize()
                        .scale(speed);
            }

            this.phantom.setDeltaMovement(newVelocity);
            this.phantom.hasImpulse = true;

            // 跟隨飛行方向旋轉
            if (newVelocity.horizontalDistanceSqr() > 0.0001D) {

                float yaw = (float) (
                        Mth.atan2(
                                newVelocity.z,
                                newVelocity.x
                        ) * (180.0D / Math.PI)
                ) - 90.0F;

                this.phantom.setYRot(yaw);
                this.phantom.setYHeadRot(yaw);
                this.phantom.setYBodyRot(yaw);

                // 根據上升下降方向調整俯仰
                float pitch = (float) (
                        -Math.toDegrees(
                                Math.atan2(
                                        newVelocity.y,
                                        newVelocity.horizontalDistance()
                                )
                        )
                );

                this.phantom.setXRot(
                        Mth.clamp(pitch, -60.0F, 60.0F)
                );
            }
        }
    }

    // =====================================================
    // 其他規則
    // =====================================================

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    // =====================================================
    // NBT
    // =====================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {

        super.addAdditionalSaveData(tag);

        if (this.summonerUUID != null) {
            tag.putUUID(NBT_SUMMONER, this.summonerUUID);
        }

        tag.putLong(NBT_EXPIRE_TIME, this.expireGameTime);
        tag.putInt(NBT_ATTACK_COOLDOWN, this.attackCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {

        super.readAdditionalSaveData(tag);

        this.summonerUUID = tag.hasUUID(NBT_SUMMONER)
                ? tag.getUUID(NBT_SUMMONER)
                : null;

        this.expireGameTime = tag.contains(NBT_EXPIRE_TIME)
                ? tag.getLong(NBT_EXPIRE_TIME)
                : -1L;

        this.attackCooldown = Math.max(
                0,
                tag.getInt(NBT_ATTACK_COOLDOWN)
        );

        this.idleCenter = null;
        this.idleDestination = null;
        this.idleRetargetTicks = 0;
    }
}


