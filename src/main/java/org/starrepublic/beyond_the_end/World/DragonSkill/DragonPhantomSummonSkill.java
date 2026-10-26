package org.starrepublic.beyond_the_end.World.DragonSkill;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import org.starrepublic.beyond_the_end.Entitys.entity.DragonBombFireball;
import org.starrepublic.beyond_the_end.Entitys.mob.EnderPhantom;
import org.starrepublic.beyond_the_end.Register.EntityRegister;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public final class DragonPhantomSummonSkill {

    private DragonPhantomSummonSkill() {
    }

    // =====================================================
    // 技能參數
    // =====================================================

    private static final int SUMMON_COUNT = 6;
    private static final int CHECK_INTERVAL = 10;

    private static final float HEALTH_THRESHOLD = 0.5F;
    private static final float HEAL_MULTIPLIER = 1.0F;

    private static final double SUMMON_RADIUS = 16.0D;
    private static final double TARGET_RANGE = 128.0D;

    private static final String SUMMONED_TAG =
            "BTEDragonEnderPhantomSummoned";

    // =====================================================
    // 事件註冊
    // =====================================================

    public static void register() {

        NeoForge.EVENT_BUS.addListener(
                DragonPhantomSummonSkill::onDragonTick
        );

        NeoForge.EVENT_BUS.addListener(
                DragonPhantomSummonSkill::onLivingDamage
        );

        NeoForge.EVENT_BUS.addListener(
                DragonPhantomSummonSkill::onDragonDeath
        );

        NeoForge.EVENT_BUS.addListener(
                DragonPhantomSummonSkill::onPhantomInvulnerability
        );
    }

    // =====================================================
    // 召喚判定
    // =====================================================

    private static void onDragonTick(
            EntityTickEvent.Post event
    ) {

        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        if (!(dragon.level() instanceof ServerLevel level)) {
            return;
        }

        if (!dragon.isAlive() || dragon.isRemoved()) {
            return;
        }

        if (dragon.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        CompoundTag data = dragon.getPersistentData();

        // 一場龍戰只發動一次
        if (data.getBoolean(SUMMONED_TAG)) {
            return;
        }

        // 龍的生命必須低於50%
        if (dragon.getHealth()
                >= dragon.getMaxHealth() * HEALTH_THRESHOLD) {
            return;
        }

        EndDragonFight fight = dragon.getDragonFight();

        if (fight == null) {
            return;
        }

        // 所有參與龍戰的終界水晶必須已被摧毀
        if (fight.getCrystalsAlive() > 0) {
            return;
        }

        // 必須存在可攻擊玩家
        if (findNearestPlayer(level, dragon.position()) == null) {
            return;
        }

        int summoned = summonPhantoms(level, dragon);

        if (summoned <= 0) {
            return;
        }

        // 召喚成功後記錄，防止重複觸發
        data.putBoolean(SUMMONED_TAG, true);

        playSummonEffect(level, dragon);
    }

    // =====================================================
    // 召喚末影夜魅
    // =====================================================

    private static int summonPhantoms(
            ServerLevel level,
            EnderDragon dragon
    ) {

        int spawned = 0;

        for (int i = 0; i < SUMMON_COUNT; i++) {

            EnderPhantom phantom = new EnderPhantom(
                    EntityRegister.ENDER_PHANTOM.type().get(),
                    level
            );

            boolean validPosition = false;

            for (int attempt = 0; attempt < 10; attempt++) {

                double angle =
                        Math.PI * 2.0D
                                * (i + level.getRandom().nextDouble())
                                / SUMMON_COUNT;

                double radius =
                        SUMMON_RADIUS
                                + level.getRandom().nextDouble() * 6.0D;

                double x = dragon.getX()
                        + Math.cos(angle) * radius;

                double y = dragon.getY()
                        + 8.0D
                        + level.getRandom().nextDouble() * 8.0D;

                double z = dragon.getZ()
                        + Math.sin(angle) * radius;

                BlockPos pos = BlockPos.containing(x, y, z);

                if (!level.isInWorldBounds(pos)
                        || !level.isLoaded(pos)) {
                    continue;
                }

                phantom.setPos(x, y, z);

                if (!level.noCollision(phantom)) {
                    continue;
                }

                validPosition = true;
                break;
            }

            if (!validPosition) {
                continue;
            }

            // 綁定召喚者與90秒存活時間
            phantom.setSummoner(dragon);

            phantom.setYRot(
                    level.getRandom().nextFloat() * 360.0F
            );

            Player target = findNearestPlayer(
                    level,
                    phantom.position()
            );

            if (target != null) {
                phantom.setTarget(target);
            }

            if (level.addFreshEntity(phantom)) {
                spawned++;
            }
        }

        return spawned;
    }

    // =====================================================
    // 玩家搜尋
    // =====================================================

    @Nullable
    private static Player findNearestPlayer(
            ServerLevel level,
            Vec3 position
    ) {

        Player nearest = null;
        double nearestDistance = TARGET_RANGE * TARGET_RANGE;

        for (ServerPlayer player : level.players()) {

            if (!player.isAlive()
                    || player.isSpectator()) {
                continue;
            }

            double distance =
                    player.position().distanceToSqr(position);

            if (distance < nearestDistance) {

                nearestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    // =====================================================
    // 末影夜魅吸血
    // =====================================================

    private static void onLivingDamage(
            LivingDamageEvent.Post event
    ) {

        // 必須是玩家受到傷害
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }

        // 攻擊者必須是末影夜魅
        if (!(event.getSource().getEntity()
                instanceof EnderPhantom phantom)) {
            return;
        }

        if (!(phantom.level() instanceof ServerLevel level)) {
            return;
        }

        EnderDragon dragon = phantom.getSummoner();

        if (dragon == null || !dragon.isAlive()) {
            return;
        }

        /*
         * 取得玩家實際失去的生命值。
         *
         * 不使用 getOriginalDamage()
         * 不使用 getNewDamage()
         *
         * 避免將被護甲、抗性、吸收生命等抵消的
         * 傷害也計算為終界龍回血。
         */
        float actualDamage = event.getOriginalDamage()-event.getBlockedDamage()-event.getShieldDamage();

        if (actualDamage <= 0.0F) {
            return;
        }

        float healAmount = actualDamage * HEAL_MULTIPLIER;

        // 由原版 heal() 處理最大生命值上限
        dragon.heal(healAmount);

        level.sendParticles(
                ParticleTypes.DRAGON_BREATH,
                dragon.getX(),
                dragon.getY() + 2.0D,
                dragon.getZ(),
                12,
                2.0D,
                1.0D,
                2.0D,
                0.025D
        );
    }

    // =====================================================
    // 龍死亡時清理召喚物
    // =====================================================

    private static void onDragonDeath(
            LivingDeathEvent event
    ) {

        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }

        if (!(dragon.level() instanceof ServerLevel level)) {
            return;
        }

        UUID dragonUUID = dragon.getUUID();

        List<EnderPhantom> phantoms =
                level.getEntitiesOfClass(
                        EnderPhantom.class,
                        dragon.getBoundingBox().inflate(256.0D),
                        phantom -> dragonUUID.equals(
                                phantom.getSummonerUUID()
                        )
                );

        for (EnderPhantom phantom : phantoms) {
            phantom.discard();
        }
    }

    // =====================================================
    // 召喚效果
    // =====================================================

    private static void playSummonEffect(
            ServerLevel level,
            EnderDragon dragon
    ) {

        level.playSound(
                null,
                dragon.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE,
                5.0F,
                0.65F
        );

        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                dragon.getX(),
                dragon.getY() + 3.0D,
                dragon.getZ(),
                150,
                8.0D,
                4.0D,
                8.0D,
                0.12D
        );
    }

    /**
     * 末影夜魅免疫終界龍造成的傷害。
     */
    private static void onPhantomInvulnerability(
            EntityInvulnerabilityCheckEvent event
    ) {
        if (!(event.getEntity() instanceof EnderPhantom)) {
            return;
        }

        DamageSource source = event.getSource();

        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();

        // 1. 終界龍本體攻擊
        if (attacker instanceof EnderDragon
                || direct instanceof EnderDragon) {
            event.setInvulnerable(true);
            return;
        }

        // 2. 終界龍身體部位攻擊
        if (direct instanceof EnderDragonPart
                || attacker instanceof EnderDragonPart) {
            event.setInvulnerable(true);
            return;
        }

        // 3. 自訂轟炸火球
        if (direct instanceof DragonBombFireball) {
            event.setInvulnerable(true);
            return;
        }

        // 4. 其他由終界龍發射的投射物
        if (direct instanceof Projectile projectile
                && projectile.getOwner() instanceof EnderDragon) {
            event.setInvulnerable(true);
            return;
        }

        // 5. 終界龍產生的龍息效果雲
        if (direct instanceof AreaEffectCloud cloud
                && cloud.getOwner() instanceof EnderDragon) {
            event.setInvulnerable(true);
        }
    }

}

