package org.starrepublic.beyond_the_end.World.DragonSkill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class DragonDiveBombardmentSkill {

    private DragonDiveBombardmentSkill() {
    }

    // =====================================================
    // 技能參數
    // =====================================================

    private static final double CHARGE_SPEED = 3.2D;

    private static final float IMPACT_DAMAGE = 16.0F;

    private static final double HORIZONTAL_KNOCKBACK = 2.6D;
    private static final double VERTICAL_KNOCKBACK = 0.85D;

    private static final double MIN_TARGET_DISTANCE = 24.0D;
    private static final double MAX_TARGET_DISTANCE = 100.0D;

    private static final double MIN_HEIGHT_ADVANTAGE = 6.0D;

    private static final int TRACKING_TICKS = 8;
    private static final int MAX_CHARGE_TICKS = 35;

    private static final int MIN_COOLDOWN = 260;
    private static final int MAX_COOLDOWN = 420;

    // 冷卻完成後，每 Tick 約 1/60 機率發動
    private static final int ACTIVATION_CHANCE = 60;

    // 接近目標後結束衝鋒的距離
    private static final double ARRIVAL_DISTANCE = 6.0D;

    // =====================================================
    // Persistent NBT
    // =====================================================

    private static final String PREFIX = "BTE_DragonDive_";

    private static final String ACTIVE = PREFIX + "Active";
    private static final String AGE = PREFIX + "Age";
    private static final String NEXT_USE = PREFIX + "NextUse";

    private static final String TARGET_UUID = PREFIX + "TargetUUID";

    private static final String TARGET_X = PREFIX + "TargetX";
    private static final String TARGET_Y = PREFIX + "TargetY";
    private static final String TARGET_Z = PREFIX + "TargetZ";

    /*
     * 每 Tick 前暫存龍身碰撞箱。
     *
     * 用於高速移動時的掃掠碰撞判定。
     * 不需要儲存至 NBT。
     */
    private static final Map<EnderDragon, AABB> PREVIOUS_BODY_BOXES =
            new WeakHashMap<>();

    // =====================================================
    // 事件註冊
    // =====================================================

    public static void register() {

        NeoForge.EVENT_BUS.addListener(
                DragonDiveBombardmentSkill::onDragonTickPre
        );

        NeoForge.EVENT_BUS.addListener(
                DragonDiveBombardmentSkill::onDragonTickPost
        );
    }

    public static boolean isActive(EnderDragon dragon) {
        return dragon.getPersistentData().getBoolean(ACTIVE);
    }

    // =====================================================
    // Tick Pre：保留衝鋒目標及上一 Tick 碰撞箱
    // =====================================================

    private static void onDragonTickPre(
            EntityTickEvent.Pre event
    ) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        if (!(dragon.level() instanceof ServerLevel)) {
            return;
        }

        if (!isActive(dragon) || !dragon.isAlive()) {
            PREVIOUS_BODY_BOXES.remove(dragon);
            return;
        }

        if (!isChargingPhase(dragon)) {
            PREVIOUS_BODY_BOXES.remove(dragon);
            return;
        }

        CompoundTag data = dragon.getPersistentData();

        /*
         * 每 Tick 重新設定原版衝鋒階段的目標。
         *
         * 這也能處理存檔重新載入後，
         * DragonChargePlayerPhase 內部目標遺失的情況。
         */
        getChargePhase(dragon).setTarget(
                readTarget(data)
        );

        PREVIOUS_BODY_BOXES.put(
                dragon,
                getBodyBox(dragon)
        );
    }

    // =====================================================
    // Tick Post：技能狀態機
    // =====================================================

    private static void onDragonTickPost(
            EntityTickEvent.Post event
    ) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        if (!(dragon.level() instanceof ServerLevel level)) {
            return;
        }

        if (!dragon.isAlive() || dragon.isRemoved()) {
            PREVIOUS_BODY_BOXES.remove(dragon);
            dragon.getPersistentData().putBoolean(ACTIVE, false);
            return;
        }

        CompoundTag data = dragon.getPersistentData();

        if (data.getBoolean(ACTIVE)) {
            tickCharging(level, dragon, data);
        } else {
            tryStart(level, dragon, data);
        }
    }

    // =====================================================
    // 發動條件
    // =====================================================

    private static void tryStart(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {
        // 只能從原版巡航階段啟動
        if (dragon.getPhaseManager()
                .getCurrentPhase()
                .getPhase() != EnderDragonPhase.HOLDING_PATTERN) {
            return;
        }

        // 不打斷之前的高空毀滅轟炸
        if (DragonSkyBombardmentSkill.isActive(dragon)) {
            return;
        }

        // 不打斷正在進行的火球轟炸
        if (DragonFirebombSkill.isActive(dragon)) {
            return;
        }

        if (level.getGameTime() < data.getLong(NEXT_USE)) {
            return;
        }

        if (level.getRandom().nextInt(ACTIVATION_CHANCE) != 0) {
            return;
        }

        ServerPlayer target = findTarget(level, dragon);

        if (target == null) {
            return;
        }

        beginCharge(level, dragon, target, data);
    }

    private static ServerPlayer findTarget(
            ServerLevel level,
            EnderDragon dragon
    ) {
        ServerPlayer nearest = null;

        double minDistanceSqr =
                MIN_TARGET_DISTANCE * MIN_TARGET_DISTANCE;

        double maxDistanceSqr =
                MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE;

        double nearestDistance = maxDistanceSqr;

        for (ServerPlayer player : level.players()) {

            if (!isValidPlayer(player)) {
                continue;
            }

            double distance = dragon.distanceToSqr(player);

            if (distance < minDistanceSqr
                    || distance > nearestDistance) {
                continue;
            }

            // 必須能夠形成向下俯衝的動作
            if (dragon.getY()
                    < player.getY() + MIN_HEIGHT_ADVANTAGE) {
                continue;
            }

            nearestDistance = distance;
            nearest = player;
        }

        return nearest;
    }

    private static boolean isValidPlayer(ServerPlayer player) {

        return player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator();
    }

    // =====================================================
    // 開始衝鋒
    // =====================================================

    private static void beginCharge(
            ServerLevel level,
            EnderDragon dragon,
            ServerPlayer target,
            CompoundTag data
    ) {
        Vec3 aim = predictTarget(dragon, target);

        data.putBoolean(ACTIVE, true);
        data.putInt(AGE, 0);

        data.putUUID(
                TARGET_UUID,
                target.getUUID()
        );

        writeTarget(data, aim);

        /*
         * 使用原版 CHARGING_PLAYER。
         *
         * setPhase() 必須在 setTarget() 前執行，
         * 因為原版 Phase.begin() 會重設階段內部狀態。
         */
        dragon.getPhaseManager().setPhase(
                EnderDragonPhase.CHARGING_PLAYER
        );

        getChargePhase(dragon).setTarget(aim);

        Vec3 direction = aim.subtract(dragon.position());

        if (direction.lengthSqr() > 0.0001D) {

            direction = direction.normalize();

            faceDirection(dragon, direction);

            // 立即給予高速初速度
            dragon.setDeltaMovement(
                    direction.scale(CHARGE_SPEED)
            );

            dragon.hasImpulse = true;
        }

        level.playSound(
                null,
                dragon.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE,
                4.0F,
                1.25F
        );
    }

    // =====================================================
    // 執行衝鋒
    // =====================================================

    private static void tickCharging(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data
    ) {
        /*
         * 如果其他戰鬥機制已經切換 Phase，
         * 不強行把龍改回衝鋒。
         */
        if (!isChargingPhase(dragon)) {
            finishCharge(level, dragon, data, false);
            return;
        }

        int age = data.getInt(AGE) + 1;
        data.putInt(AGE, age);

        // 先進行碰撞檢查，避免高速掠過時漏判
        if (checkBodyImpact(level, dragon)) {
            finishCharge(level, dragon, data, true);
            return;
        }

        // 衝鋒超時
        if (age >= MAX_CHARGE_TICKS) {
            finishCharge(level, dragon, data, true);
            return;
        }

        /*
         * 前8 Tick 持續重新瞄準玩家。
         *
         * 之後不再改變瞄準點，
         * 讓玩家能夠透過移動躲避俯衝。
         */
        if (age <= TRACKING_TICKS
                && data.hasUUID(TARGET_UUID)) {

            UUID uuid = data.getUUID(TARGET_UUID);

            Player foundPlayer = level.getPlayerByUUID(uuid);

            if (foundPlayer instanceof ServerPlayer player
                    && isValidPlayer(player)) {

                Vec3 aim = predictTarget(dragon, player);

                writeTarget(data, aim);

                getChargePhase(dragon).setTarget(aim);
            }
        }

        Vec3 aim = readTarget(data);

        Vec3 difference = aim.subtract(dragon.position());

        double distance = difference.length();

        if (distance <= ARRIVAL_DISTANCE) {
            finishCharge(level, dragon, data, true);
            return;
        }

        /*
         * 使用原版 Phase 管理衝鋒狀態，
         * 但在 Tick 結束時補足高速移動速度。
         *
         * 不直接 setPos() 傳送龍，
         * 以保留原版移動、碰撞及身體部位更新。
         */
        Vec3 direction = difference.normalize();

        faceDirection(dragon, direction);

        dragon.setDeltaMovement(
                direction.scale(CHARGE_SPEED)
        );

        dragon.hasImpulse = true;
    }

    // =====================================================
    // 玩家位置預測
    // =====================================================

    private static Vec3 predictTarget(
            EnderDragon dragon,
            ServerPlayer player
    ) {
        Vec3 center = player.getBoundingBox().getCenter();

        double distance = dragon.position().distanceTo(center);

        double leadTicks = Math.min(
                distance / CHARGE_SPEED,
                6.0D
        );

        return center.add(
                player.getDeltaMovement().scale(
                        leadTicks * 0.65D
                )
        );
    }

    // =====================================================
    // 龍身撞擊
    // =====================================================

    private static boolean checkBodyImpact(
            ServerLevel level,
            EnderDragon dragon
    ) {
        AABB currentBody = getBodyBox(dragon);

        AABB previousBody = PREVIOUS_BODY_BOXES.remove(dragon);

        if (previousBody == null) {
            previousBody = currentBody;
        }

        /*
         * 將上一 Tick 與目前龍身的碰撞箱合併。
         *
         * 高速移動時即使龍身跳過玩家位置，
         * 仍可檢測移動路徑中經過的玩家。
         */
        AABB sweptBox = previousBody
                .minmax(currentBody)
                .inflate(0.35D);

        Vec3 from = previousBody.getCenter();
        Vec3 to = currentBody.getCenter();

        for (ServerPlayer player : level.players()) {

            if (!isValidPlayer(player)) {
                continue;
            }

            AABB playerBox = player.getBoundingBox();

            if (!sweptBox.intersects(playerBox)) {
                continue;
            }

            /*
             * 第二層碰撞判定：
             * 檢查龍身中心的移動軌跡，
             * 避免 sweptBox 在斜向移動時誤判。
             */
            AABB expandedPlayerBox =
                    playerBox.inflate(
                            2.7D,
                            2.0D,
                            2.7D
                    );

            boolean intersects =
                    expandedPlayerBox.contains(from)
                            || expandedPlayerBox.contains(to)
                            || expandedPlayerBox.clip(from, to).isPresent();

            if (!intersects) {
                continue;
            }

            damagePlayer(level, dragon, player);

            // 本輪衝鋒命中實體後結束
            return true;
        }

        return false;
    }

    /**
     * 原版 EnderDragon 的 subEntities 順序：
     *
     * 0 = head
     * 1 = neck
     * 2 = body
     * 3～5 = tail
     * 6～7 = wings
     */
    private static AABB getBodyBox(EnderDragon dragon) {
        return dragon.getSubEntities()[2].getBoundingBox();
    }

    private static void damagePlayer(
            ServerLevel level,
            EnderDragon dragon,
            ServerPlayer player
    ) {
        boolean damaged = player.hurt(
                level.damageSources().mobAttack(dragon),
                IMPACT_DAMAGE
        );

        if (!damaged) {
            return;
        }

        // 擊退方向：遠離終界龍
        double dx = player.getX() - dragon.getX();
        double dz = player.getZ() - dragon.getZ();

        double length = Math.sqrt(dx * dx + dz * dz);

        if (length < 0.001D) {

            Vec3 velocity = dragon.getDeltaMovement();

            dx = velocity.x;
            dz = velocity.z;

            length = Math.sqrt(dx * dx + dz * dz);
        }

        if (length < 0.001D) {
            dx = 1.0D;
            dz = 0.0D;
            length = 1.0D;
        }

        dx /= length;
        dz /= length;

        player.push(
                dx * HORIZONTAL_KNOCKBACK,
                VERTICAL_KNOCKBACK,
                dz * HORIZONTAL_KNOCKBACK
        );

        player.hurtMarked = true;

        level.playSound(
                null,
                dragon.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE,
                4.0F,
                1.25F
        );
    }

    // =====================================================
    // 結束技能
    // =====================================================

    private static void finishCharge(
            ServerLevel level,
            EnderDragon dragon,
            CompoundTag data,
            boolean restoreHolding
    ) {
        data.putBoolean(ACTIVE, false);
        data.remove(TARGET_UUID);

        PREVIOUS_BODY_BOXES.remove(dragon);

        data.putLong(
                NEXT_USE,
                level.getGameTime()
                        + MIN_COOLDOWN
                        + level.getRandom().nextInt(
                        MAX_COOLDOWN - MIN_COOLDOWN + 1
                )
        );

        // 避免龍保留完整衝鋒速度
        dragon.setDeltaMovement(
                dragon.getDeltaMovement().scale(0.45D)
        );

        /*
         * 只在本技能正常結束且仍屬於衝鋒階段時
         * 恢復巡航。
         */
        if (restoreHolding && isChargingPhase(dragon)) {

            dragon.getPhaseManager().setPhase(
                    EnderDragonPhase.HOLDING_PATTERN
            );
        }
    }

    // =====================================================
    // Phase / 姿態
    // =====================================================

    private static boolean isChargingPhase(
            EnderDragon dragon
    ) {
        return dragon.getPhaseManager()
                .getCurrentPhase()
                .getPhase() == EnderDragonPhase.CHARGING_PLAYER;
    }

    private static DragonChargePlayerPhase getChargePhase(
            EnderDragon dragon
    ) {
        return dragon.getPhaseManager().getPhase(
                EnderDragonPhase.CHARGING_PLAYER
        );
    }

    private static void faceDirection(
            EnderDragon dragon,
            Vec3 direction
    ) {
        if (direction.horizontalDistanceSqr() < 0.0001D) {
            return;
        }

        float yaw = (float) (
                Mth.atan2(direction.z, direction.x)
                        * (180.0D / Math.PI)
        ) - 90.0F;

        dragon.setYRot(yaw);
        dragon.setYBodyRot(yaw);
        dragon.setYHeadRot(yaw);
    }

    // =====================================================
    // NBT 目標座標
    // =====================================================

    private static void writeTarget(
            CompoundTag data,
            Vec3 target
    ) {
        data.putDouble(TARGET_X, target.x);
        data.putDouble(TARGET_Y, target.y);
        data.putDouble(TARGET_Z, target.z);
    }

    private static Vec3 readTarget(CompoundTag data) {
        return new Vec3(
                data.getDouble(TARGET_X),
                data.getDouble(TARGET_Y),
                data.getDouble(TARGET_Z)
        );
    }
}

