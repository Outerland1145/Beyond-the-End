package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.tags.DamageTypeTags;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.entity.player.Player;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CharmEffectEvents {

    private static final ResourceLocation SPEED_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    "beyond_the_end",
                    "charm_agility"
            );

    private static final Map<UUID, Integer> LAST_FOOD =
            new HashMap<>();

    private static final Map<UUID, Double> LIFE_STEAL_PROGRESS =
            new HashMap<>();

    private static final Map<UUID, Long> LAST_RESORT_COOLDOWN =
            new HashMap<>();

    /*
     * 避免護符產生的額外傷害，
     * 再次觸發同一個護符而無限遞迴。
     */
    private static final ThreadLocal<Boolean> INTERNAL_DAMAGE =
            ThreadLocal.withInitial(() -> false);

    private static boolean internal() {
        return INTERNAL_DAMAGE.get();
    }

    private static boolean has(
            String actual,
            String effectName
    ) {
        return actual.equals(
                "beyond_the_end:" + effectName
        );
    }

    private static double clampFraction(double value) {
        return Math.clamp(value, 0.0D, 1.0D);
    }

    /**
     * 每個玩家 Tick：
     *
     * 敏捷、活力、消化恢復。
     */
    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        double[] values = new double[3];

        // 0 = 移動速度加成
        // 1 = 自然生命恢復速度加成
        // 2 = 每消耗一點飢餓值恢復生命量

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (has(effect, "movement_speed")) {
                        values[0] += charm.get("spd", 1.0D) - 1.0D;
                    }

                    if (has(effect, "health_regeneration")) {
                        values[1] += charm.get("regenspd");
                    }

                    if (has(effect, "digestive_recovery")) {
                        values[2] += charm.get("healthperhunger");
                    }
                }
        );

        // 敏捷：使用瞬時屬性修飾器。
        AttributeInstance movement =
                player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (movement != null) {

            movement.removeModifier(SPEED_MODIFIER);

            if (values[0] != 0.0D) {

                movement.addTransientModifier(
                        new AttributeModifier(
                                SPEED_MODIFIER,
                                values[0],
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        )
                );
            }
        }

        // 活力：在原本的自然恢復條件下提供額外治療。
        //
        // 這是現代版適配：保留提升恢復效率的效果，
        // 不直接套用再生藥水，也不覆蓋其他藥水效果。
        if (values[1] > 0.0D
                && player.tickCount % 80 == 0
                && player.level().getGameRules().getBoolean(
                net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION
        )
                && player.getFoodData().getFoodLevel() >= 18
                && player.getHealth() < player.getMaxHealth()) {

            player.heal(
                    (float) (values[1])
            );
        }

        // 消化恢復：監測飢餓值減少。
        int food = player.getFoodData().getFoodLevel();

        Integer previous = LAST_FOOD.put(
                player.getUUID(),
                food
        );

        if (previous != null
                && previous > food
                && values[2] > 0.0D
                && player.getHealth() < player.getMaxHealth()) {

            player.heal(
                    (float) (
                            (previous - food) * values[2]
                    )
            );
        }
    }

    /**
     * 採掘速度：包含普通急速護符。
     */
    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {

        Player player = event.getEntity();

        double[] bonus = {0.0D};

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (has(effect, "digging_speed")) {
                        bonus[0] += charm.get("breakspd", 1.0D) - 1.0D;
                    }
                }
        );

        if (bonus[0] != 0.0D) {
            event.setNewSpeed(
                    (float) (
                            event.getNewSpeed() * Math.max(0, 1 + bonus[0])
                    )
            );
        }
    }

    /**
     * 經驗增益。
     *
     * 使用 XpChange 而不僅限於拾取經驗球，
     * 因此其他經由 giveExperiencePoints 給予的經驗
     * 也能取得增益。
     */
    @SubscribeEvent
    public static void onExperience(
            PlayerXpEvent.XpChange event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getAmount() <= 0) {
            return;
        }

        double[] bonus = {0.0D};

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (has(effect, "experience_gain")) {
                        bonus[0] += charm.get("exp", 1.0D) - 1.0D;
                    }
                }
        );

        if (bonus[0] <= 0.0D) {
            return;
        }

        double result = event.getAmount() * (1.0D + bonus[0]);

        int integer = (int) Math.floor(result);
        double fraction = result - integer;

        // 保留小數機率，避免小經驗值永遠得不到加成。
        if (player.getRandom().nextDouble() < fraction) {
            integer++;
        }

        event.setAmount(Math.max(0, integer));
    }

    /**
     * 攻擊加成與暴擊。
     *
     * 只計算真正來自玩家的傷害來源，
     * 並允許不同暴擊護符獨立觸發、加算額外倍率。
     */
    @SubscribeEvent
    public static void onOutgoingDamage(
            LivingIncomingDamageEvent event
    ) {

        if (internal()) {
            return;
        }

        DamageSource source = event.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getEntity() == player) {
            return;
        }

        double[] bonus = {0.0D};

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (has(effect, "attack_damage")) {

                        bonus[0] += charm.get("dmg", 1.0D) - 1.0D;

                    } else if (has(effect, "critical_strike")) {

                        double chance = clampFraction(
                                charm.get("critchance")
                        );

                        if (player.getRandom().nextDouble() < chance) {

                            bonus[0] += Math.max(
                                    0.0D,
                                    charm.get("critdmg", 1.0D) - 1.0D
                            );
                        }
                    }
                }
        );

        if (bonus[0] != 0.0D) {

            event.setAmount(
                    (float) (
                            event.getAmount() * Math.max(0, 1 + bonus[0])
                    )
            );
        }

        /*
         * 魔法穿透：
         *
         * 使用 DamageContainer 修改盔甲減免，
         * 而不是再次呼叫 hurt() 產生第二次傷害。
         *
         * 此處實現「指定比例的物理傷害不受盔甲減免」。
         * 真正拆分兩種 DamageType 需要獨立處理。
         */
        double[] penetration = {0.0D};

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (has(effect, "magic_penetration")) {
                        penetration[0] += charm.get("dmgtomagic");
                    }
                }
        );

        float fraction = (float) clampFraction(penetration[0]);

        if (fraction > 0.0F) {

            event.addReductionModifier(
                    DamageContainer.Reduction.ARMOR,
                    (container, reduction) ->
                            reduction * (1.0F - fraction)
            );
        }
    }

    /**
     * 受傷前減免：
     *
     * 防禦、魔法防禦、墜落防護、格擋。
     *
     * 所有減免作用於已計算盔甲與防護效果的傷害。
     */
    @SubscribeEvent
    public static void onIncomingDamage(
            LivingDamageEvent.Pre event
    ) {

        if (internal()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DamageSource source = event.getSource();

        float damage = event.getNewDamage();

        if (damage <= 0.0F) {
            return;
        }

        double[] reduction = {0.0D};
        double[] fallBlocks = {0.0D};

        boolean magic =
                source.is(DamageTypes.MAGIC)
                        || source.is(DamageTypes.INDIRECT_MAGIC);

        boolean fall =
                source.is(DamageTypes.FALL);

        boolean excludedPhysical =
                magic
                        || fall
                        || source.is(DamageTypes.IN_WALL)
                        || source.is(DamageTypes.FELL_OUT_OF_WORLD);

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (has(effect, "damage_reduction")
                            && !excludedPhysical) {

                        reduction[0] += charm.get("reducedmg");

                    } else if (has(effect, "magic_defense")
                            && magic) {

                        reduction[0] += charm.get("reducemagicdmg");

                    } else if (has(effect, "falling_protection")
                            && fall) {

                        fallBlocks[0] += charm.get("fallblocks");

                    } else if (
                            (has(effect, "blocking_reduction")
                                    || has(effect, "blocking_reflection")
                                    || has(effect, "blocking_repulsion"))
                                    && player.isBlocking()) {

                        reduction[0] += charm.get("reducedmgblock");
                    }
                }
        );

        damage *= (float) (
                1.0D - clampFraction(reduction[0])
        );

        // 保留 HEE 墜落保護描述的實際抵消差值：
        // 標示 6 格的護符，約額外抵消 4 點墜落傷害。
        if (fall && fallBlocks[0] > 0.0D) {

            damage = Math.max(
                    0.0F,
                    damage - (float) Math.max(0, fallBlocks[0] - 2)
            );
        }

        /*
         * 最後手段：
         *
         * 傷害致命時嘗試傳送。
         * 傳送成功才免除此次傷害。
         */
        if (damage >= player.getHealth() + player.getAbsorptionAmount()) {

            if (tryLastResort(player)) {
                damage = 0.0F;
            }
        }

        event.setNewDamage(damage);
    }

    /**
     * 最後手段：嘗試安全傳送。
     */
    private static boolean tryLastResort(ServerPlayer player) {

        ServerLevel level = player.serverLevel();

        long now = level.getGameTime();

        long nextAvailable = LAST_RESORT_COOLDOWN.getOrDefault(
                player.getUUID(),
                0L
        );

        if (now < nextAvailable) {
            return false;
        }

        List<CharmEffectAccess.ActiveCharm> charms =
                CharmEffectAccess.getActive(player);

        for (CharmEffectAccess.ActiveCharm charm : charms) {

            boolean correctEffect =
                    charm.definition().effects().stream()
                            .anyMatch(effect ->
                                    has(
                                            effect.type().toString(),
                                            "last_resort"
                                    )
                            );

            if (!correctEffect) {
                continue;
            }

            int distance = Math.max(
                    1,
                    (int) charm.get("lastresortblocks", 6)
            );

            int cooldown = Math.max(
                    1,
                    (int) charm.get("lastresortcooldown", 10)
            );

            BlockPos origin = player.blockPosition();

            for (int attempt = 0; attempt < 24; attempt++) {

                double angle =
                        player.getRandom().nextDouble() * Math.PI * 2;

                int dx = (int) Math.round(Math.cos(angle) * distance);
                int dz = (int) Math.round(Math.sin(angle) * distance);

                for (int dy = 2; dy >= -2; dy--) {

                    BlockPos destination = origin.offset(dx, dy, dz);

                    BlockPos below = destination.below();

                    boolean safe =
                            level.getBlockState(destination).isAir()
                                    && level.getBlockState(
                                    destination.above()
                            ).isAir()
                                    && level.getBlockState(below)
                                    .isFaceSturdy(
                                            level,
                                            below,
                                            Direction.UP
                                    );

                    if (!safe) {
                        continue;
                    }

                    player.teleportTo(
                            destination.getX() + 0.5D,
                            destination.getY(),
                            destination.getZ() + 0.5D
                    );

                    player.resetFallDistance();

                    LAST_RESORT_COOLDOWN.put(
                            player.getUUID(),
                            now + cooldown * 20L
                    );

                    return true;
                }
            }
        }

        return false;
    }
    /**
     * 格擋反射及格擋排斥。
     *
     * 只有原版認定成功格擋時才執行。
     */
    @SubscribeEvent
    public static void onShieldBlock(
            LivingShieldBlockEvent event
    ) {

        if (internal() || !event.getOriginalBlock()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity attacker = event.getDamageSource().getEntity();

        if (!(attacker instanceof LivingEntity target)
                || target == player) {
            return;
        }

        float blocked = event.getBlockedDamage();

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (has(effect, "blocking_reflection")) {

                        float reflected =
                                blocked * (float) charm.get(
                                        "blockreflectdmg"
                                );

                        if (reflected > 0.0F) {

                            INTERNAL_DAMAGE.set(true);

                            try {
                                target.hurt(
                                        player.damageSources().thorns(player),
                                        reflected
                                );
                            } finally {
                                INTERNAL_DAMAGE.set(false);
                            }
                        }
                    }

                    if (has(effect, "blocking_repulsion")) {

                        double strength =
                                charm.get("blockrepulsepower");

                        if (strength > 0.0D) {

                            target.knockback(
                                    strength * 0.5D,
                                    player.getX() - target.getX(),
                                    player.getZ() - target.getZ()
                            );
                        }
                    }
                }
        );
    }

    /**
     * 實際傷害結算後：
     *
     * 生命汲取、巫術打擊。
     */
    @SubscribeEvent
    public static void onDamagePost(
            LivingDamageEvent.Post event
    ) {

        if (internal()) {
            return;
        }

        DamageSource source = event.getSource();

        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        if (event.getEntity() == attacker) {
            return;
        }

        float dealt = event.getNewDamage();

        if (dealt <= 0.0F) {
            return;
        }

        /*
         * 原版生命汲取與巫術效果都要求直接攻擊。
         *
         * 箭矢等間接傷害不觸發此段效果。
         */
        if (source.getDirectEntity() != attacker) {
            return;
        }

        double[] stolenHealth = {0.0D};
        double[] requiredDamage = {0.0D};

        CharmEffectAccess.forEachEffect(
                attacker,
                (charm, effect) -> {

                    if (has(effect, "life_steal")) {

                        stolenHealth[0] = charm.get("stealhealth");
                        requiredDamage[0] = charm.get("stealdealt");

                    } else if (has(effect, "witchery_harm")) {

                        double chance = clampFraction(
                                charm.get("badeffchance")
                        );

                        if (attacker.getRandom().nextDouble() < chance) {

                            int amplifier = Math.max(
                                    0,
                                    (int) charm.get("badefflvl", 1) - 1
                            );

                            int duration = Math.max(
                                    1,
                                    (int) charm.get("badefftime", 5) * 20
                            );

                            applyRandomNegativeEffect(
                                    attacker,
                                    event.getEntity(),
                                    duration,
                                    amplifier
                            );
                        }
                    }
                }
        );

        /*
         * 生命汲取：累積實際造成的傷害。
         *
         * 達到指定門檻才治療，並保留超出門檻的傷害值。
         */
        if (stolenHealth[0] > 0.0D
                && requiredDamage[0] > 0.0D) {

            UUID uuid = attacker.getUUID();

            double accumulated =
                    LIFE_STEAL_PROGRESS.getOrDefault(uuid, 0.0D)
                            + dealt;

            int triggers = (int) Math.floor(
                    accumulated / requiredDamage[0]
            );

            if (triggers > 0) {

                attacker.heal(
                        (float) (triggers * stolenHealth[0])
                );

                accumulated -= triggers * requiredDamage[0];
            }

            LIFE_STEAL_PROGRESS.put(uuid, accumulated);
        }
    }

    /**
     * 巫術打擊的負面效果池。
     *
     * 現代版使用原版負面藥水效果與燃燒。
     */
    private static void applyRandomNegativeEffect(
            ServerPlayer attacker,
            LivingEntity target,
            int duration,
            int amplifier
    ) {

        int choice = attacker.getRandom().nextInt(6);

        switch (choice) {

            case 0 -> target.addEffect(
                    new MobEffectInstance(
                            MobEffects.POISON,
                            duration,
                            amplifier
                    )
            );

            case 1 -> target.addEffect(
                    new MobEffectInstance(
                            MobEffects.WEAKNESS,
                            duration,
                            amplifier
                    )
            );

            case 2 -> target.addEffect(
                    new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN,
                            duration,
                            amplifier
                    )
            );

            case 3 -> target.addEffect(
                    new MobEffectInstance(
                            MobEffects.DIG_SLOWDOWN,
                            duration,
                            amplifier
                    )
            );

            case 4 -> target.addEffect(
                    new MobEffectInstance(
                            MobEffects.WITHER,
                            duration,
                            amplifier
                    )
            );

            case 5 -> target.setRemainingFireTicks(
                    Math.max(
                            target.getRemainingFireTicks(),
                            duration
                    )
            );
        }
    }

    /**
     * 傷害轉移。
     *
     * 僅在成功將傷害轉移到附近生物時，
     * 才減少玩家所受到的傷害。
     */
    @SubscribeEvent
    public static void onDamageRedirection(
            LivingDamageEvent.Pre event
    ) {

        if (internal()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        float original = event.getNewDamage();

        if (original <= 0.0F) {
            return;
        }

        float[] remaining = {original};

        Entity attacker = event.getSource().getEntity();

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (!has(effect, "damage_redirection")) {
                        return;
                    }

                    double fraction = clampFraction(
                            charm.get("rediramt")
                    );

                    int maxTargets = Math.max(
                            1,
                            (int) charm.get("redirmobs", 1)
                    );

                    float toRedirect =
                            remaining[0] * (float) fraction;

                    if (toRedirect <= 0.0F) {
                        return;
                    }

                    List<LivingEntity> targets = player.level()
                            .getEntitiesOfClass(
                                    LivingEntity.class,
                                    player.getBoundingBox().inflate(6.0D),
                                    entity ->
                                            entity.isAlive()
                                                    && entity != player
                                                    && entity != attacker
                                                    && !(entity instanceof Player)
                            );

                    targets.sort(
                            Comparator.comparingDouble(
                                    entity ->
                                            entity.distanceToSqr(player)
                            )
                    );

                    if (targets.isEmpty()) {
                        return;
                    }

                    int count = Math.min(
                            maxTargets,
                            targets.size()
                    );

                    float damageEach = toRedirect / count;

                    float successfullyRedirected = 0.0F;

                    INTERNAL_DAMAGE.set(true);

                    try {
                        for (int i = 0; i < count; i++) {

                            LivingEntity target = targets.get(i);

                            if (target.hurt(
                                    player.damageSources().magic(),
                                    damageEach
                            )) {
                                successfullyRedirected += damageEach;
                            }
                        }
                    } finally {
                        INTERNAL_DAMAGE.set(false);
                    }

                    remaining[0] = Math.max(
                            0.0F,
                            remaining[0] - successfullyRedirected
                    );
                }
        );

        event.setNewDamage(remaining[0]);
    }

    /**
     * 第二耐久。
     *
     * 物品因耐久耗盡而被破壞時，
     * 根據護符機率重建物品，恢復部分耐久，
     * 並以掉落物形式返還玩家。
     */
    @SubscribeEvent
    public static void onItemDestroyed(
            PlayerDestroyItemEvent event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack original = event.getOriginal();

        if (original.isEmpty()
                || !original.isDamageableItem()) {
            return;
        }

        boolean[] recovered = {false};

        CharmEffectAccess.forEachEffect(
                player,
                (charm, effect) -> {

                    if (recovered[0]
                            || !has(effect, "second_durability")) {
                        return;
                    }

                    double chance = clampFraction(
                            charm.get("recdurabilitychance")
                    );

                    double recovery = clampFraction(
                            charm.get("recdurabilityamt")
                    );

                    if (player.getRandom().nextDouble() >= chance) {
                        return;
                    }

                    ItemStack restored = original.copy();

                    int maxDamage = restored.getMaxDamage();

                    if (maxDamage <= 0) {
                        return;
                    }

                    int durabilityRecovered = Math.max(
                            1,
                            (int) Math.ceil(maxDamage * recovery)
                    );

                    restored.setDamageValue(
                            Math.max(0, maxDamage - durabilityRecovered)
                    );

                    recovered[0] = true;

                    player.drop(restored, false);
                }
        );
    }

    /**
     * 殺戮衝擊。
     *
     * 擊殺生物後對附近敵對／非玩家生物造成範圍傷害。
     *
     * 注意：此處的傷害基數仍屬暫定適配。
     */
    @SubscribeEvent
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {

        if (internal()) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        LivingEntity dead = event.getEntity();

        if (dead == killer) {
            return;
        }

        CharmEffectAccess.forEachEffect(
                killer,
                (charm, effect) -> {

                    if (!has(effect, "slaughter_impact")) {
                        return;
                    }

                    double radius = Math.max(
                            1.0D,
                            charm.get("impactrad", 4)
                    );

                    double power = Math.max(
                            0.0D,
                            charm.get("impactamt")
                    );

                    float impactDamage = (float) Math.min(
                            40.0D,
                            dead.getMaxHealth() * power
                    );

                    if (impactDamage <= 0.0F) {
                        return;
                    }

                    List<LivingEntity> targets =
                            dead.level().getEntitiesOfClass(
                                    LivingEntity.class,
                                    dead.getBoundingBox().inflate(radius),
                                    entity ->
                                            entity != dead
                                                    && entity != killer
                                                    && entity.isAlive()
                                                    && !(entity instanceof Player)
                                                    && entity.distanceToSqr(dead)
                                                    <= radius * radius
                            );

                    INTERNAL_DAMAGE.set(true);

                    try {
                        for (LivingEntity target : targets) {

                            target.hurt(
                                    killer.damageSources().magic(),
                                    impactDamage
                            );
                        }
                    } finally {
                        INTERNAL_DAMAGE.set(false);
                    }
                }
        );
    }

    /**
     * 玩家登出時清理暫存資料。
     */
    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {

        UUID uuid = event.getEntity().getUUID();

        LAST_FOOD.remove(uuid);
        LIFE_STEAL_PROGRESS.remove(uuid);
        LAST_RESORT_COOLDOWN.remove(uuid);
    }
}