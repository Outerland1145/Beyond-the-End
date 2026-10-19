package org.starrepublic.beyond_the_end.Entitys.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import org.starrepublic.beyond_the_end.Register.helper.EntityHelper;

import java.util.List;

/**
 * 獨立遠古終界龍（Minecraft 1.21.1 / NeoForge）。
 * 絕不 extends EnderDragon；不連接 EndDragonFight。
 * 龍身部件為巢狀類，仍只需要一個實體源碼檔案。
 *
 * 飛行採用 1.8 式隨機巡航／俯衝目標選擇；不是 Mojang 1.8 移動公式逐行複製。
 */
public class AncientEnderDragonEntity extends Mob implements Enemy {

    private static final double ROAM_RADIUS = 60.0D;
    private static final double PLAYER_SCAN_RADIUS = 120.0D;
    private static final double DAMAGE_RANGE_SQR = 6.0D * 6.0D;

    public static final EntityHelper.MobSpec<AncientEnderDragonEntity> SPEC =
            new EntityHelper.MobSpec<>(
                    AncientEnderDragonEntity::new,
                    MobCategory.MONSTER,
                    16.0F, 8.0F,
                    AncientEnderDragonEntity::createAttributes,
                    SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> false,
                    0x170D20, 0xA14ECB
            );

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.beyond_the_end.ancient_ender_dragon"),
            BossEvent.BossBarColor.PINK,
            BossEvent.BossBarOverlay.PROGRESS
    );
    // 客戶端需要知道龍是否處於受擊停滯狀態
    private static final EntityDataAccessor<Boolean> DATA_HIT_HOVER =
            SynchedEntityData.defineId(
                    AncientEnderDragonEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    // 獨立的拍翼動畫時間
    private float flapTime = 0.0F;
    private float previousFlapTime = 0.0F;

    // 拍翼速度，用來平滑過渡
    private float flapSpeed = 0.04F;
    // 提供給客戶端模型動畫使用的飛行歷史
    private final float[] flightYawHistory = new float[64];
    private final double[] flightYHistory = new double[64];
    private int flightHistoryIndex = -1;
    private BlockPos home;
    private Vec3 flightTarget;

    // 目前追蹤的玩家；null 表示隨機巡航
    private Player flightPlayer;

    // 被玩家攻擊後停滯 2 秒
    private static final int HIT_HOVER_DURATION = 5;

    // 停滯結束後，優先逃離玩家 5 秒
    private static final int RETREAT_DURATION = 100;

    // 玩家重新鎖定冷卻：60～180 秒
    private static final int RETARGET_MIN_TICKS = 1200;
    private static final int RETARGET_MAX_TICKS = 3600;

    private int hitHoverTicks = 0;
    private int retreatTicks = 0;
    private int playerRetargetCooldownTicks = 0;

    // 紀錄最後一次攻擊者的位置，供逃離方向計算
    private Vec3 lastAttackerPos;

    // 舊版龍的轉向慣性
    private float yawVelocity = 0.0F;

    // 是否要求重新選擇飛行目標
    private boolean forceNewTarget = false;

    private int attackCooldown;
    private EndCrystal visualCrystal;
    private int nextVisualCrystalScan;

    private final DragonPart head = new DragonPart(this, "head", 3.0F, 3.0F);
    private final DragonPart neck = new DragonPart(this, "neck", 3.0F, 3.0F);
    private final DragonPart body = new DragonPart(this, "body", 5.0F, 3.0F);
    private final DragonPart tail1 = new DragonPart(this, "tail1", 2.0F, 2.0F);
    private final DragonPart tail2 = new DragonPart(this, "tail2", 2.0F, 2.0F);
    private final DragonPart tail3 = new DragonPart(this, "tail3", 2.0F, 2.0F);
    private final DragonPart wing1 = new DragonPart(this, "wing1", 4.0F, 2.0F);
    private final DragonPart wing2 = new DragonPart(this, "wing2", 4.0F, 2.0F);
    private final DragonPart[] parts = {head, neck, body, tail1, tail2, tail3, wing1, wing2};

    public AncientEnderDragonEntity(EntityType<? extends AncientEnderDragonEntity> type, Level level) {
        super(type, level);  // 重要：不可寫 EntityType.ENDER_DRAGON
        this.setNoGravity(true);
        this.noPhysics = true;
        this.noCulling = true;
        this.xpReward = 0;
        this.bossBar.setDarkenScreen(false);
        this.bossBar.setPlayBossMusic(true);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HIT_HOVER, false);
    }
    public static AttributeSupplier.Builder createAttributes() {
        // 生命值與原版相同，其他遊戲機制的移動由本類飛行程式控制。
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, PLAYER_SCAN_RADIUS);
    }
    @Override
    public void checkDespawn() {
        // BOSS 不執行普通 Mob 的自然消失判定
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        return false;
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected void registerGoals() {

        // 1.8 龍的飛行是獨立的方向控制，不使用 Mob 的地面 Goal。
    }
    @Override
    public void travel(Vec3 travelVector) {
        // 飛行完全由 tickOldFlight() 控制。
        // 不使用 LivingEntity 的普通移動流程。
    }
    @Override
    protected InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        if (player.getItemInHand(hand).is(Items.NAME_TAG)) {
            return InteractionResult.FAIL;
        }

        return super.mobInteract(player, hand);
    }
    @Override
    public boolean canBeLeashed() {
        return false;
    }
    private void updateFlapAnimation() {

        Vec3 motion = this.getDeltaMovement();

        double horizontalSpeed = Math.sqrt(
                motion.x * motion.x + motion.z * motion.z
        );

        // 參考 Minecraft 1.7.10 的 animTime 增量公式
        float targetSpeed = (float) (
                0.2D / (horizontalSpeed * 10.0D + 1.0D)
        );

        targetSpeed *= (float) Math.pow(
                2.0D,
                Mth.clamp(motion.y, -1.0D, 1.0D)
        );

        // 受擊停滯時，強制提高拍翼速度
        if (this.entityData.get(DATA_HIT_HOVER)) {
            targetSpeed = Math.max(targetSpeed, 0.18F);
        }

        targetSpeed = Mth.clamp(targetSpeed, 0.015F, 0.20F);

        // 逐漸加快或減慢，而非瞬間改變頻率
        flapSpeed += (targetSpeed - flapSpeed) * 0.25F;

        flapTime += flapSpeed;
    }

    public float getFlapPhase(float partialTick) {
        return Mth.lerp(
                partialTick,
                previousFlapTime,
                flapTime
        ) * (float) (Math.PI * 2.0D);
    }
    @Override
    public void tick() {
        // 保存上一 Tick 的動畫時間
        previousFlapTime = flapTime;

        super.tick();
        // 清除舊存檔中可能已經存在的載具關係
        if (!this.level().isClientSide() && this.isPassenger()) {
            this.stopRiding();
        }
        this.setNoGravity(true);
        this.noPhysics = true;

        // 死亡後不再更新任何拍翼動畫
        if (!this.isAlive()) {
            return;
        }

        // 活著時才推進動畫時間
        updateFlapAnimation();

        if (!this.level().isClientSide()) {
            if (this.home == null) this.home = this.blockPosition();
            if (!this.isNoAi()) tickOldFlight();
            // 將伺服器端的受擊停滯狀態同步給客戶端
            this.entityData.set(DATA_HIT_HOVER, hitHoverTicks > 0);
            tickCrystalHealing();
            this.bossBar.setName(this.getDisplayName());
            this.bossBar.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        }
        if (!this.isNoAi() && this.tickCount % 22 == 0) {
            this.level().playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.HOSTILE,
                    2.5F,
                    0.95F + this.random.nextFloat() * 0.1F
            );
        }
        // 更新全部龍身部件的位置
        updateParts();

        // 必須使用更新後的龍頭、翅膀、身體碰撞箱
        if (!this.level().isClientSide() && !this.isNoAi()) {

            // 龍頭傷害及翅膀擊退
            tickDragonCollision();

            // 龍頭及身體破壞可破壞方塊
            tickDragonBlockDestruction();
        }

        // 保留原有動畫歷史紀錄
        recordFlightHistory();

    }

    private void tickOldFlight() {

        // =====================================================
        // 1. 更新飛行目標
        // =====================================================

        // =====================================================
        // 1. 每 Tick 減少玩家重新鎖定冷卻
        // =====================================================

        if (playerRetargetCooldownTicks > 0) {
            --playerRetargetCooldownTicks;
        }

        // =====================================================
        // 2. 受擊後短暫懸停
        // =====================================================

        if (hitHoverTicks > 0) {

            --hitHoverTicks;

            // 停止位移，但保留既有的翅膀動畫
            this.setDeltaMovement(Vec3.ZERO);
            this.hasImpulse = true;

            // 停滯結束後選擇逃離路線
            if (hitHoverTicks == 0) {
                chooseRetreatTarget();
            }

            // 本 Tick 不執行任何飛行加速
            return;
        }

        // =====================================================
        // 3. 逃離階段
        // =====================================================

        if (retreatTicks > 0) {

            --retreatTicks;

            // 逃離期間絕不追蹤玩家
            flightPlayer = null;

            // 到達逃離目標後，繼續尋找更遠的位置
            if (flightTarget == null
                    || this.distanceToSqr(flightTarget) < 100.0D) {

                chooseRetreatTarget();
            }

            // 逃離結束，下一 Tick 恢復巡航目標選擇
            if (retreatTicks == 0) {
                forceNewTarget = true;
            }

        } else {

            // =================================================
            // 4. 原本的 1.7.10 風格目標追蹤
            // =================================================

            if (flightPlayer != null) {

                if (!flightPlayer.isAlive()
                        || flightPlayer.isSpectator()
                        || flightPlayer.level() != this.level()
                        || this.distanceToSqr(flightPlayer)
                        > PLAYER_SCAN_RADIUS * PLAYER_SCAN_RADIUS) {

                    flightPlayer = null;
                    forceNewTarget = true;

                } else {

                    double dx = flightPlayer.getX() - this.getX();
                    double dz = flightPlayer.getZ() - this.getZ();

                    double horizontalDistance =
                            Math.sqrt(dx * dx + dz * dz);

                    double heightOffset = Math.min(
                            10.0D,
                            horizontalDistance / 80.0D - 0.6D
                    );

                    flightTarget = new Vec3(
                            flightPlayer.getX(),
                            flightPlayer.getBoundingBox().minY
                                    + heightOffset,
                            flightPlayer.getZ()
                    );
                }

            } else if (flightTarget != null) {

                // 舊版龍的隨機巡航偏移
                flightTarget = flightTarget.add(
                        this.random.nextGaussian() * 2.0D,
                        0.0D,
                        this.random.nextGaussian() * 2.0D
                );
            }

            if (flightTarget == null
                    || forceNewTarget
                    || this.distanceToSqr(flightTarget) < 100.0D
                    || this.distanceToSqr(flightTarget) > 22500.0D) {

                chooseFlightTarget();
            }
        }

        // =====================================================
        // 從這裡開始保留原本的 1.7.10 飛行物理
        // =====================================================

        Vec3 to = flightTarget.subtract(this.position());

        if (to.lengthSqr() < 1.0E-6D) {
            forceNewTarget = true;
            return;
        }

        double dx = to.x;
        double dy = to.y;
        double dz = to.z;

        double horizontalDistance = Math.sqrt(
                dx * dx + dz * dz
        );

        // =====================================================
        // 3. 垂直飛行加速度
        // =====================================================

        double verticalDirection = Mth.clamp(
                dy / Math.max(horizontalDistance, 0.001D),
                -0.6D,
                0.6D
        );

        Vec3 velocity = this.getDeltaMovement().add(
                0.0D,
                verticalDirection * 0.1D,
                0.0D
        );

        // =====================================================
        // 4. 計算龍頭與目標的方向一致程度
        // =====================================================

        float currentYaw = this.getYRot();
        double yawRadians = Math.toRadians(currentYaw);

        // 注意：這裡使用 1.21.1 的 yaw 方向慣例
        Vec3 forward = new Vec3(
                -Math.sin(yawRadians),
                velocity.y,
                Math.cos(yawRadians)
        ).normalize();

        Vec3 targetDirection = to.normalize();

        float alignment = (float) (
                (forward.dot(targetDirection) + 0.5D) / 1.5D
        );

        alignment = Math.max(0.0F, alignment);

        // =====================================================
        // 5. 1.7.10 轉向慣性
        // =====================================================

        float wantedYaw = (float) Math.toDegrees(
                Mth.atan2(-dx, dz)
        );

        float yawDifference = Mth.clamp(
                Mth.wrapDegrees(wantedYaw - currentYaw),
                -50.0F,
                50.0F
        );

        double horizontalSpeed = Math.sqrt(
                velocity.x * velocity.x
                        + velocity.z * velocity.z
        );

        double turningResistance = Math.min(
                horizontalSpeed + 1.0D,
                40.0D
        );

        // 核心：先衰減舊角速度，再逐漸增加新的轉向
        yawVelocity *= 0.8F;

        yawVelocity += (float) (
                yawDifference * 0.7D
                        / turningResistance
                        / (horizontalSpeed + 1.0D)
        );

        float newYaw = Mth.wrapDegrees(
                currentYaw + yawVelocity * 0.1F
        );

        this.setYRot(newYaw);
        this.yBodyRot = newYaw;
        this.yHeadRot = newYaw;

        // =====================================================
        // 6. 按目前朝向推進，而不是直接飛向目標
        // =====================================================

        float speedFactor = (float) (
                2.0D / (turningResistance + 1.0D)
        );

        float thrust = 0.06F * (
                alignment * speedFactor
                        + (1.0F - speedFactor)
        );

        double newYawRadians = Math.toRadians(newYaw);

        Vec3 accelerated = velocity.add(
                -Math.sin(newYawRadians) * thrust,
                0.0D,
                Math.cos(newYawRadians) * thrust
        );

        // =====================================================
        // 7. 實際移動
        // =====================================================

        this.move(MoverType.SELF, accelerated);

        // =====================================================
        // 8. 依移動方向計算空氣阻力
        // =====================================================

        Vec3 movementDirection = accelerated.normalize();

        float drag = (float) (
                (movementDirection.dot(forward) + 1.0D) / 2.0D
        );

        drag = 0.8F + 0.15F * drag;

        this.setDeltaMovement(
                accelerated.x * drag,
                accelerated.y * 0.91D,
                accelerated.z * drag
        );

        this.hasImpulse = true;

        // =====================================================
        // 9. 平滑處理俯仰角
        // =====================================================

        float wantedPitch = (float) Mth.clamp(
                -Math.toDegrees(
                        Math.atan2(
                                accelerated.y,
                                Math.sqrt(
                                        accelerated.x * accelerated.x
                                                + accelerated.z * accelerated.z
                                )
                        )
                ),
                -35.0D,
                35.0D
        );

        this.setXRot(
                Mth.lerp(0.12F, this.getXRot(), wantedPitch)
        );
    }

    private void chooseFlightTarget() {

        forceNewTarget = false;
        flightPlayer = null;

        List<Player> players = this.level().getEntitiesOfClass(
                Player.class,
                AABB.ofSize(
                        this.position(),
                        PLAYER_SCAN_RADIUS * 2.0D,
                        160.0D,
                        PLAYER_SCAN_RADIUS * 2.0D
                ),
                player -> player.isAlive()
                        && !player.isSpectator()
                        && this.distanceToSqr(player)
                        <= PLAYER_SCAN_RADIUS * PLAYER_SCAN_RADIUS
        );

        // 1.7.10：約一半機率選擇玩家
        if (playerRetargetCooldownTicks == 0
                && !players.isEmpty()
                && this.random.nextBoolean()) {

            flightPlayer = players.get(
                    this.random.nextInt(players.size())
            );

            flightTarget = flightPlayer.position();
            return;
        }

        // 另外一半機率選擇隨機巡航座標
        // 保留 BTE 的巢穴中心，而不是使用世界原點。
        double baseY = Math.max(
                70.0D,
                home.getY() - 30.0D
        );

        Vec3 candidate = null;

        for (int attempt = 0; attempt < 32; attempt++) {

            double x = home.getX() + 0.5D
                    + (this.random.nextFloat() * 2.0D - 1.0D)
                    * ROAM_RADIUS;

            double y = Mth.clamp(
                    baseY + this.random.nextFloat() * 50.0D,
                    this.level().getMinBuildHeight() + 12.0D,
                    this.level().getMaxBuildHeight() - 12.0D
            );

            double z = home.getZ() + 0.5D
                    + (this.random.nextFloat() * 2.0D - 1.0D)
                    * ROAM_RADIUS;

            candidate = new Vec3(x, y, z);

            // 舊版龍不會故意選擇距離自己太近的巡航點。
            if (candidate.distanceToSqr(this.position()) > 100.0D) {
                flightTarget = candidate;
                return;
            }
        }

        // 極端情況下的保底目標
        flightTarget = this.position().add(
                0.0D, 15.0D, 30.0D
        );
    }

    private void chooseRetreatTarget() {

        // 取得遠離最後攻擊者的水平向量
        Vec3 away;

        if (lastAttackerPos != null) {
            away = new Vec3(
                    this.getX() - lastAttackerPos.x,
                    0.0D,
                    this.getZ() - lastAttackerPos.z
            );
        } else {
            away = Vec3.ZERO;
        }

        // 攻擊者與龍的位置太接近時，改用目前朝向
        if (away.lengthSqr() < 1.0E-4D) {

            double yaw = Math.toRadians(this.getYRot());

            away = new Vec3(
                    -Math.sin(yaw),
                    0.0D,
                    Math.cos(yaw)
            );
        }

        away = away.normalize();

        // 選擇距離龍 65～100 格的逃離目標
        double distance = 65.0D + this.random.nextInt(36);

        double targetY = Mth.clamp(
                this.getY() + 15.0D + this.random.nextInt(16),
                this.level().getMinBuildHeight() + 12.0D,
                this.level().getMaxBuildHeight() - 12.0D
        );

        this.flightTarget = new Vec3(
                this.getX() + away.x * distance,
                targetY,
                this.getZ() + away.z * distance
        );

        this.forceNewTarget = false;
    }

    private void tickCrystalHealing() {
        if (this.tickCount % 10 != 0 || this.getHealth() >= this.getMaxHealth()) return;
        List<EndCrystal> crystals = level().getEntitiesOfClass(
                EndCrystal.class, this.getBoundingBox().inflate(32.0D), Entity::isAlive);
        for (EndCrystal crystal : crystals) {
            if (crystal.distanceToSqr(this) < 32 * 32) {
                this.heal(1.0F);
                break;
            }
        }
    }
    public EndCrystal getVisualHealingCrystal() {

        if (this.tickCount < nextVisualCrystalScan) {
            return visualCrystal != null && visualCrystal.isAlive()
                    ? visualCrystal : null;
        }

        nextVisualCrystalScan = this.tickCount + 10;
        visualCrystal = null;

        double nearestDistance = 32.0D * 32.0D;

        for (EndCrystal crystal : this.level().getEntitiesOfClass(
                EndCrystal.class,
                this.getBoundingBox().inflate(32.0D),
                Entity::isAlive
        )) {

            double distance = this.distanceToSqr(crystal);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                visualCrystal = crystal;
            }
        }

        return visualCrystal;
    }

    private boolean canDragonHit(LivingEntity target) {

        if (target == this || !target.isAlive()) {
            return false;
        }

        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }

        return true;
    }
    private void knockBackWithWing(DragonPart wing) {

        AABB area = wing.getBoundingBox()
                .inflate(4.0D, 2.0D, 4.0D)
                .move(0.0D, -2.0D, 0.0D);

        Vec3 center = body.getBoundingBox().getCenter();

        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                this::canDragonHit
        );

        for (LivingEntity target : targets) {

            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;

            double distanceSqr = dx * dx + dz * dz;

            // 避免目標恰好在中心造成除以零，
            // 同時限制極端情況的擊退強度。
            if (distanceSqr < 0.01D) {
                double yaw = Math.toRadians(this.getYRot());
                dx = -Math.sin(yaw);
                dz = Math.cos(yaw);
                distanceSqr = 1.0D;
            }

            double strength = Math.min(
                    4.0D / Math.max(distanceSqr, 1.0D),
                    2.0D
            );

            target.push(
                    dx * strength,
                    0.2D,
                    dz * strength
            );
        }
    }
    private void tickDragonCollision() {

        // 參考舊版龍：受傷硬直期間不進行撞擊攻擊
        if (this.hurtTime > 0) {
            return;
        }

        // 左右翅膀擊退
        knockBackWithWing(wing1);
        knockBackWithWing(wing2);

        // 龍頭造成 10 點傷害
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                head.getBoundingBox().inflate(1.0D),
                this::canDragonHit
        );

        for (LivingEntity target : targets) {
            target.hurt(
                    this.damageSources().mobAttack(this),
                    10.0F
            );
        }
    }
    private boolean destroyBlocksInArea(AABB area) {

        int minX = Mth.floor(area.minX);
        int minY = Mth.floor(area.minY);
        int minZ = Mth.floor(area.minZ);

        int maxX = Mth.floor(area.maxX);
        int maxY = Mth.floor(area.maxY);
        int maxZ = Mth.floor(area.maxZ);

        boolean destroyedAny = false;

        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    cursor.set(x, y, z);

                    // 不因破壞地形而主動載入遠方區塊
                    if (!this.level().hasChunkAt(cursor)) {
                        continue;
                    }

                    BlockState state = this.level().getBlockState(cursor);

                    if (state.isAir()) {
                        continue;
                    }

                    // 原版龍免疫方塊，不可破壞
                    if (state.is(BlockTags.DRAGON_IMMUNE)) {
                        continue;
                    }

                    // 原版允許龍穿過、但不需要破壞的方塊
                    if (state.is(BlockTags.DRAGON_TRANSPARENT)) {
                        continue;
                    }

                    BlockPos pos = cursor.immutable();

                    // 尊重其他模組對生物破壞方塊的限制
                    if (!EventHooks.onEntityDestroyBlock(this, pos, state)) {
                        continue;
                    }

                    // 破壞方塊，但不直接掉落方塊物品
                    if (this.level().destroyBlock(pos, false, this)) {
                        destroyedAny = true;
                    }
                }
            }
        }

        return destroyedAny;
    }
    private void tickDragonBlockDestruction() {

        // 尊重 mobGriefing 與其他模組對生物破壞地形的設定
        if (!EventHooks.canEntityGrief(this.level(), this)) {
            return;
        }

        boolean destroyed = false;

        // 與 1.7.10 相同，使用龍頭和身體碰撞箱
        destroyed |= destroyBlocksInArea(head.getBoundingBox());
        destroyed |= destroyBlocksInArea(body.getBoundingBox());

        // 方塊破壞時產生爆炸粒子
        if (destroyed && this.level() instanceof ServerLevel serverLevel) {

            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION,
                    this.getX()
                            + (this.random.nextDouble() - 0.5D) * 6.0D,
                    this.getY() + 2.0D
                            + (this.random.nextDouble() - 0.5D) * 4.0D,
                    this.getZ()
                            + (this.random.nextDouble() - 0.5D) * 6.0D,
                    1,
                    0.0D, 0.0D, 0.0D,
                    0.0D
            );
        }
    }
    private void recordFlightHistory() {

        if (flightHistoryIndex == -1) {
            java.util.Arrays.fill(flightYawHistory, this.getYRot());
            java.util.Arrays.fill(flightYHistory, this.getY());

            flightHistoryIndex = 0;
            return;
        }

        flightHistoryIndex = (flightHistoryIndex + 1) & 63;

        flightYawHistory[flightHistoryIndex] = this.getYRot();
        flightYHistory[flightHistoryIndex] = this.getY();
    }

    public float getFlightYaw(int ticksAgo, float partialTick) {
        if (!this.isAlive()) {
            partialTick = 0.0F;
        }
        if (flightHistoryIndex == -1) {
            return this.getYRot();
        }

        int index = (flightHistoryIndex - Math.min(ticksAgo, 62)) & 63;
        int previous = (index - 1) & 63;

        return Mth.rotLerp(
                partialTick,
                flightYawHistory[previous],
                flightYawHistory[index]
        );
    }

    public double getFlightY(int ticksAgo, float partialTick) {
        if (!this.isAlive()) {
            partialTick = 0.0F;
        }
        if (flightHistoryIndex == -1) {
            return this.getY();
        }

        int index = (flightHistoryIndex - Math.min(ticksAgo, 62)) & 63;
        int previous = (index - 1) & 63;

        return Mth.lerp(
                partialTick,
                flightYHistory[previous],
                flightYHistory[index]
        );
    }
    private void updateParts() {
        // 相對於龍的航向放置碰撞部件，非 client-tracked 獨立實體。
        double a = Math.toRadians(this.getYRot());
        double fx = -Math.sin(a), fz = Math.cos(a);
        double rx = Math.cos(a), rz = Math.sin(a);
        put(head, fx * 6.0D, 2.0D, fz * 6.0D);
        put(neck, fx * 3.4D, 1.8D, fz * 3.4D);
        put(body, 0.0D, 1.0D, 0.0D);
        put(tail1, -fx * 3.0D, 1.5D, -fz * 3.0D);
        put(tail2, -fx * 5.5D, 1.4D, -fz * 5.5D);
        put(tail3, -fx * 8.0D, 1.3D, -fz * 8.0D);
        put(wing1, rx * 4.5D, 2.0D, rz * 4.5D);
        put(wing2, -rx * 4.5D, 2.0D, -rz * 4.5D);
    }

    private void put(DragonPart part, double x, double y, double z) {
        part.setPos(this.getX() + x, this.getY() + y, this.getZ() + z);
    }
    @Override
    public boolean hurt(DamageSource source, float amount) {

        boolean damaged = super.hurt(source, amount);

        if (!damaged || this.level().isClientSide() || !this.isAlive()) {
            return damaged;
        }

        if (source.getEntity() instanceof Player attacker) {

            // 每次受到有效玩家傷害，重新隨機決定鎖定冷卻
            playerRetargetCooldownTicks =
                    RETARGET_MIN_TICKS
                            + this.random.nextInt(
                            RETARGET_MAX_TICKS
                                    - RETARGET_MIN_TICKS + 1
                    );

            lastAttackerPos = attacker.position();

            // 立即解除玩家追蹤
            flightPlayer = null;

            // 沒有正在停滯或逃離，才重新觸發受傷停滯
            if (hitHoverTicks == 0 && retreatTicks == 0) {

                hitHoverTicks = HIT_HOVER_DURATION;
                retreatTicks = RETREAT_DURATION;

                flightTarget = null;
                forceNewTarget = false;

                this.setDeltaMovement(Vec3.ZERO);

            } else if (hitHoverTicks == 0 && retreatTicks > 0) {

                // 逃離途中再次受攻擊，只修正逃離方向
                // 不重新讓龍停滯
                chooseRetreatTarget();
            }
        }

        return damaged;
    }
    public boolean hurtPart(
            DragonPart part,
            DamageSource source,
            float amount
    ) {

        float actualDamage = part == head
                ? amount
                : amount * 0.25F + 1.0F;

        return this.hurt(source, actualDamage);
    }

    @Override
    public boolean isMultipartEntity() { return true; }

    @Override
    public PartEntity<?>[] getParts() { return parts; }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) {
            for (int i = 0; i < parts.length; ++i) parts[i].setId(id + 1 + i);
        }
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        for (int i = 0; i < parts.length; ++i) parts[i].setId(this.getId() + 1 + i);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void onRemovedFromLevel() {
        bossBar.removeAllPlayers();
        super.onRemovedFromLevel();
    }

    @Override
    protected void tickDeath() {
        // 獨立 200-tick 死亡階段；不使用主島龍戰鬥管理器，不生成返回門。
        ++this.deathTime;
        // =====================================================
// 遠古終界龍死亡經驗：6000 XP
// 依照 1.7.10 終界龍的死亡節奏分批釋放
// =====================================================

        if (this.level() instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {

            // 第 155～200 Tick，每 5 Tick 掉落 500 XP
            // 共 10 次，合計 5000 XP
            if (this.deathTime > 150
                    && this.deathTime <= 200
                    && this.deathTime % 5 == 0) {

                ExperienceOrb.award(
                        serverLevel,
                        this.position(),
                        500
                );
            }

            // 第 200 Tick 額外掉落 1000 XP
            if (this.deathTime == 200) {

                ExperienceOrb.award(
                        serverLevel,
                        this.position(),
                        1000
                );
            }
        }
        // 死亡後停止原本的飛行慣性
        this.setDeltaMovement(Vec3.ZERO);

        // 沿用 1.7.10 的死亡運動：每 Tick 上升 0.1 格
        this.move(
                MoverType.SELF,
                new Vec3(0.0D, 0.1D, 0.0D)
        );

        // 原本的 tick() 死亡後會提前 return，
        // 所以在這裡更新龍各部位的碰撞箱
                updateParts();
        if (this.level().isClientSide()
                && this.deathTime % 5 == 0) {

            double x = this.getX()
                    + (this.random.nextDouble() - 0.5D) * 12.0D;

            double y = this.getY()
                    + this.random.nextDouble() * 6.0D;

            double z = this.getZ()
                    + (this.random.nextDouble() - 0.5D) * 12.0D;

            this.level().addParticle(
                    ParticleTypes.EXPLOSION,
                    x, y, z,
                    0.0D, 0.0D, 0.0D
            );
        }
        if (this.deathTime >= 200 && !this.level().isClientSide()) {
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putInt("BTEHomeX", home.getX());
            tag.putInt("BTEHomeY", home.getY());
            tag.putInt("BTEHomeZ", home.getZ());
        }
        tag.putInt("BTEHitHoverTicks", hitHoverTicks);
        tag.putInt("BTERetreatTicks", retreatTicks);
        tag.putInt("BTEPlayerRetargetCooldown", playerRetargetCooldownTicks);
        if (lastAttackerPos != null) {
            tag.putDouble("BTELastAttackerX", lastAttackerPos.x);
            tag.putDouble("BTELastAttackerY", lastAttackerPos.y);
            tag.putDouble("BTELastAttackerZ", lastAttackerPos.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BTEHomeX") && tag.contains("BTEHomeY") && tag.contains("BTEHomeZ")) {
            home = new BlockPos(tag.getInt("BTEHomeX"), tag.getInt("BTEHomeY"), tag.getInt("BTEHomeZ"));
        }
        hitHoverTicks = Math.max(
                0, tag.getInt("BTEHitHoverTicks")
        );
        retreatTicks = Math.max(
                0, tag.getInt("BTERetreatTicks")
        );
        playerRetargetCooldownTicks = Math.max(
                0, tag.getInt("BTEPlayerRetargetCooldown")
        );
        if (tag.contains("BTELastAttackerX")
                && tag.contains("BTELastAttackerY")
                && tag.contains("BTELastAttackerZ")) {

            lastAttackerPos = new Vec3(
                    tag.getDouble("BTELastAttackerX"),
                    tag.getDouble("BTELastAttackerY"),
                    tag.getDouble("BTELastAttackerZ")
            );
        }
    }

    /**
     * 完全獨立的龍身碰撞部件。
     * 巢狀類不註冊 EntityType；NeoForge 視它為主實體的 multipart 子部分。
     */
    public static final class DragonPart extends PartEntity<AncientEnderDragonEntity> {
        private final EntityDimensions dimensions;
        private final String partName;

        public DragonPart(AncientEnderDragonEntity owner, String partName, float width, float height) {
            super(owner);
            this.partName = partName;
            this.dimensions = EntityDimensions.scalable(width, height);
            this.refreshDimensions();
        }

        public String getPartName() { return partName; }

        @Override
        public EntityDimensions getDimensions(Pose pose) { return dimensions; }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            return !getParent().level().isClientSide() && getParent().hurtPart(this, source, amount);
        }

        @Override
        public boolean isPickable() { return true; }

        @Override
        public boolean shouldBeSaved() { return false; }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) { }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) { }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) { }
    }
}
