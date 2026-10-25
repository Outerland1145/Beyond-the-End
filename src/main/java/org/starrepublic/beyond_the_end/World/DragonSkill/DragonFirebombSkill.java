package org.starrepublic.beyond_the_end.World.DragonSkill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.starrepublic.beyond_the_end.Entitys.entity.DragonBombFireball;
import org.starrepublic.beyond_the_end.Register.EntityRegister;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class DragonFirebombSkill {

    private DragonFirebombSkill() {
    }

    // 每輪發射數量
    private static final int MIN_SHOTS = 4;
    private static final int MAX_SHOTS = 8;

    // 每顆火球之間的間隔
    private static final int SHOT_INTERVAL = 7;

    // 技能冷卻
    private static final int MIN_COOLDOWN = 100;
    private static final int MAX_COOLDOWN = 200;

    // 攻擊距離
    private static final double ATTACK_RANGE = 120.0D;

    // 已經開始轟炸時的最大追蹤距離
    private static final double MAX_TRACK_RANGE = 180.0D;

    /*
     * 每隻原版終界龍都擁有自己的技能狀態。
     *
     * 使用 WeakHashMap，避免實體被移除後
     * 仍被靜態 Map 永久持有。
     */
    private static final Map<EnderDragon, AttackState> STATES =
            new WeakHashMap<>();

    /**
     * 在模組初始化時呼叫一次。
     */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(
                DragonFirebombSkill::onDragonTick
        );
    }

    /**
     * 終界龍每 Tick 執行一次技能控制。
     */
    private static void onDragonTick(EntityTickEvent.Post event) {

        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        if (!(dragon.level() instanceof ServerLevel level)) {
            return;
        }

        if (!dragon.isAlive() || dragon.isRemoved()) {
            STATES.remove(dragon);
            return;
        }

        AttackState state = STATES.computeIfAbsent(
                dragon,
                ignored -> new AttackState(
                        100 + level.random.nextInt(101)
                )
        );

        // 已經開始進行一輪轟炸
        if (state.remainingShots > 0) {

            if (!canBombard(dragon)) {
                stopBombardment(state, level, 100);
                return;
            }

            state.shotTimer--;

            if (state.shotTimer > 0) {
                return;
            }

            Player target = state.targetId == null
                    ? null
                    : level.getPlayerByUUID(state.targetId);

            if (!isValidTarget(dragon, target, MAX_TRACK_RANGE)) {
                stopBombardment(state, level, 100);
                return;
            }

            fireProjectile(level, dragon, target);

            state.remainingShots--;
            state.shotTimer = SHOT_INTERVAL;

            // 本輪轟炸完成
            if (state.remainingShots <= 0) {
                state.remainingShots = 0;
                state.targetId = null;
                state.cooldown = randomCooldown(level);
            }

            return;
        }

        // 正常冷卻
        if (state.cooldown > 0) {
            state.cooldown--;
            return;
        }

        // 只有正常飛行巡航階段可以開始轟炸
        if (!canBombard(dragon)) {
            state.cooldown = 20;
            return;
        }

        Player target = findTarget(level, dragon);

        if (target == null) {
            state.cooldown = 40;
            return;
        }

        // 開始一輪 3～8 顆火球的轟炸
        state.remainingShots = MIN_SHOTS
                + level.random.nextInt(MAX_SHOTS - MIN_SHOTS + 1);

        state.targetId = target.getUUID();

        // 下一 Tick 發射第一顆
        state.shotTimer = 1;
    }

    /**
     * 不覆蓋原版 AI，只有正常巡航才能使用。
     */
    private static boolean canBombard(EnderDragon dragon) {

        return dragon.getPhaseManager()
                .getCurrentPhase()
                .getPhase() == EnderDragonPhase.HOLDING_PATTERN;
    }

    /**
     * 尋找最近的可攻擊玩家。
     */
    private static Player findTarget(
            ServerLevel level,
            EnderDragon dragon
    ) {
        return level.getNearestPlayer(
                dragon.getX(),
                dragon.getY(),
                dragon.getZ(),
                ATTACK_RANGE,
                entity -> entity instanceof Player player
                        && player.isAlive()
                        && !player.isSpectator()
        );
    }

    /**
     * 驗證轟炸期間的鎖定目標。
     */
    private static boolean isValidTarget(
            EnderDragon dragon,
            Player player,
            double range
    ) {
        if (player == null) {
            return false;
        }

        if (!player.isAlive()
                || player.isRemoved()
                || player.isSpectator()
                ) {
            return false;
        }

        return dragon.distanceToSqr(player) <= range * range;
    }

    /**
     * 實際發射火球。
     *
     * 每次射擊重新計算玩家位置，
     * 不會整輪都朝第一次鎖定的位置射擊。
     */
    private static void fireProjectile(
            ServerLevel level,
            EnderDragon dragon,
            Player player
    ) {

        Vec3 muzzle = DragonFireballMuzzle.getFlyingMuzzle(dragon);

        /*
         * 預測玩家的移動。
         *
         * 因為火球速度很快，只需要短時間預測。
         */
        double distance = muzzle.distanceTo(player.position());

        double leadTime = Math.min(
                distance / DragonBombFireball.FLIGHT_SPEED,
                8.0D
        );

        Vec3 predictedPosition = player
                .getBoundingBox()
                .getCenter()
                .add(
                        player.getDeltaMovement()
                                .scale(leadTime * 0.6D)
                );

        /*
         * 添加少量隨機散布。
         *
         * 火球不是全部落在完全相同的位置，
         * 而是在玩家附近形成連續轟炸。
         */
        double spreadX = level.random.nextGaussian() * 0.9D;
        double spreadY = level.random.nextGaussian() * 0.35D;
        double spreadZ = level.random.nextGaussian() * 0.9D;

        Vec3 aimPosition = predictedPosition.add(
                spreadX,
                spreadY,
                spreadZ
        );

        Vec3 direction = aimPosition.subtract(muzzle);

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

        // 發射音效
        level.playSound(
                null,
                muzzle.x,
                muzzle.y,
                muzzle.z,
                SoundEvents.GHAST_SHOOT,
                SoundSource.HOSTILE,
                2.0F,
                0.75F + level.random.nextFloat() * 0.25F
        );
    }

    /**
     * 中斷技能並開始冷卻。
     */
    private static void stopBombardment(
            AttackState state,
            ServerLevel level,
            int minimumCooldown
    ) {
        state.remainingShots = 0;
        state.targetId = null;
        state.shotTimer = 0;

        state.cooldown = Math.max(
                minimumCooldown,
                randomCooldown(level)
        );
    }

    private static int randomCooldown(ServerLevel level) {
        return MIN_COOLDOWN
                + level.random.nextInt(
                MAX_COOLDOWN - MIN_COOLDOWN + 1
        );
    }

    /**
     * 每隻終界龍各自保存的技能狀態。
     */
    private static final class AttackState {

        private int cooldown;
        private int remainingShots;
        private int shotTimer;

        private UUID targetId;

        private AttackState(int initialCooldown) {
            this.cooldown = initialCooldown;
            this.remainingShots = 0;
            this.shotTimer = 0;
            this.targetId = null;
        }
    }
}

