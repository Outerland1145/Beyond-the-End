package org.starrepublic.beyond_the_end.Effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class EnderInfestedEffect extends MobEffect {
    private static final float TELEPORT_DAMAGE = 2.0F;
    // 受到傷害時生成終界蟎的機率
    private static final float HURT_SPAWN_CHANCE = 0.50F;

    // 瞬移間隔：200 Tick = 10 秒
    private static final int TELEPORT_INTERVAL = 100;

    // 瞬移範圍
    private static final double TELEPORT_RANGE = 12.0D;
    private static final int TELEPORT_VERTICAL_RANGE = 4;

    // 每次瞬移最多嘗試的次數
    private static final int MAX_TELEPORT_ATTEMPTS = 12;

    public EnderInfestedEffect() {
        super(
                MobEffectCategory.HARMFUL,
                0x7135A3
        );
    }

    // 每 Tick 檢查效果
    @Override
    public boolean shouldApplyEffectTickThisTick(
            int duration,
            int amplifier
    ) {
        return true;
    }

    @Override
    public boolean applyEffectTick(
            LivingEntity entity,
            int amplifier
    ) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }

        if (!entity.isAlive() || entity.isPassenger()) {
            return true;
        }

        // 每隔指定時間執行一次
        if (entity.tickCount % TELEPORT_INTERVAL != 0) {
            return true;
        }

        // 瞬移成功才召喚
        if (tryTeleport(level, entity)) {

            int count = 1 + level.random.nextInt(3);

            EndermiteSpawnHelper.spawnAround(
                    level,
                    entity.position(),
                    count,
                    1.5D,
                    true
            );
        }

        return true;
    }

    // 受到傷害時觸發
    @Override
    public void onMobHurt(
            LivingEntity entity,
            int amplifier,
            DamageSource damageSource,
            float amount
    ) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (!entity.isAlive()) {
            return;
        }

        if (level.random.nextFloat() >= HURT_SPAWN_CHANCE) {
            return;
        }

        int count = 1 + level.random.nextInt(2);

        EndermiteSpawnHelper.spawnAround(
                level,
                entity.position(),
                count,
                1.5D,
                true
        );
    }

    private boolean tryTeleport(
            ServerLevel level,
            LivingEntity entity
    ) {
        double oldX = entity.getX();
        double oldY = entity.getY();
        double oldZ = entity.getZ();

        for (int i = 0; i < MAX_TELEPORT_ATTEMPTS; i++) {

            double x = oldX
                    + (level.random.nextDouble() * 2.0D - 1.0D)
                    * TELEPORT_RANGE;

            double y = oldY
                    + level.random.nextInt(
                    TELEPORT_VERTICAL_RANGE * 2 + 1
            ) - TELEPORT_VERTICAL_RANGE;

            double z = oldZ
                    + (level.random.nextDouble() * 2.0D - 1.0D)
                    * TELEPORT_RANGE;

            // 原版的安全隨機瞬移
            if (entity.randomTeleport(x, y, z, true)) {

                level.playSound(
                        null,
                        oldX, oldY, oldZ,
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                );

                level.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                );
                // 清除瞬移前累積的墜落距離
                entity.fallDistance = 0.0F;

                // 瞬移成功後造成類似終界珍珠的傷害
                entity.hurt(
                        level.damageSources().fall(),
                        TELEPORT_DAMAGE
                );
                return true;
            }
        }

        return false;
    }
}

