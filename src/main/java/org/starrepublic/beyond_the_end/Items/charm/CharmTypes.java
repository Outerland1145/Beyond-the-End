package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import org.starrepublic.beyond_the_end.Beyond_the_end;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CharmTypes {

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                Beyond_the_end.MODID,
                name
        );
    }

    public static final ResourceKey<Registry<CharmDefinition>>
            CHARM_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    id("charm_type")
            );

    private static final Map<ResourceLocation, CharmDefinition>
            BUILTIN = Collections.unmodifiableMap(
            createBuiltin()
    );

    public static final List<ResourceLocation> BUILTIN_ORDER =
            List.copyOf(BUILTIN.keySet());

    public record CharmReference(
            ResourceLocation type,
            int level
    ) {}

    private static final Map<Integer, CharmReference>
            HEE_VARIANTS = buildLegacyIndex();

    private static Map<ResourceLocation, CharmDefinition>
    createBuiltin() {

        Map<ResourceLocation, CharmDefinition> map =
                new LinkedHashMap<>();

        // 基礎護符

        add(map, "basicpower", 0xD65A54, 0, List.of(
                v(0, "dmg", 1.25),
                v(1, "dmg", 1.45),
                v(2, "dmg", 1.60)
        ), "attack_damage");

        add(map, "basicagility", 0x68C9A8, 1, List.of(
                v(3, "spd", 1.15),
                v(4, "spd", 1.28),
                v(5, "spd", 1.40)
        ), "movement_speed");

        add(map, "basicvigor", 0xE67A95, 2, List.of(
                v(6, "regenspd", 0.20),
                v(7, "regenspd", 0.40),
                v(8, "regenspd", 0.55)
        ), "health_regeneration");

        add(map, "basicdefense", 0x679ACD, 3, List.of(
                v(9, "reducedmg", 0.10),
                v(10, "reducedmg", 0.20),
                v(11, "reducedmg", 0.30)
        ), "damage_reduction");

        add(map, "basicmagic", 0xB183D7, 7, List.of(
                v(12, "exp", 1.10),
                v(13, "exp", 1.20),
                v(14, "exp", 1.30)
        ), "experience_gain");

        // 均衡護符

        add(map, "equality", 0xD7D0AC, 9, List.of(
                        v(18,
                                "dmg", 1.08,
                                "spd", 1.05,
                                "regenspd", 0.06,
                                "reducedmg", 0.03,
                                "exp", 1.03)
                ),
                "attack_damage",
                "movement_speed",
                "health_regeneration",
                "damage_reduction",
                "experience_gain"
        );

        // 格擋護符：三種共用前景 10

        add(map, "blocking", 0x668CBE, 10, List.of(
                v(19, "reducedmgblock", 0.20),
                v(20, "reducedmgblock", 0.32),
                v(21, "reducedmgblock", 0.45)
        ), "blocking_reduction");

        add(map, "blockingreflection", 0x7D80CE, 10, List.of(
                v(22,
                        "reducedmgblock", 0.20,
                        "blockreflectdmg", 0.10),
                v(23,
                        "reducedmgblock", 0.32,
                        "blockreflectdmg", 0.10),
                v(24,
                        "reducedmgblock", 0.20,
                        "blockreflectdmg", 0.20)
        ), "blocking_reflection");

        add(map, "blockingrepulsion", 0x5EA4C0, 10, List.of(
                v(71,
                        "reducedmgblock", 0.20,
                        "blockrepulsepower", 1),
                v(72,
                        "reducedmgblock", 0.32,
                        "blockrepulsepower", 1),
                v(73,
                        "reducedmgblock", 0.20,
                        "blockrepulsepower", 2)
        ), "blocking_repulsion");

        // 生命恢復與傷害轉移

        add(map, "digestiverecover", 0x93BB64, 5, List.of(
                v(25, "healthperhunger", 0.50),
                v(26, "healthperhunger", 1.00),
                v(27, "healthperhunger", 2.00)
        ), "digestive_recovery");

        add(map, "lifesteal", 0xC45575, 11, List.of(
                v(28,
                        "stealhealth", 2,
                        "stealdealt", 16),
                v(30,
                        "stealhealth", 2,
                        "stealdealt", 10)
        ), "life_steal");

        add(map, "damageredirection", 0x8D648D, 3, List.of(
                v(31, "rediramt", 0.15, "redirmobs", 1),
                v(32, "rediramt", 0.12, "redirmobs", 2),
                v(33, "rediramt", 0.14, "redirmobs", 3),
                v(34, "rediramt", 0.20, "redirmobs", 1),
                v(35, "rediramt", 0.18, "redirmobs", 2),
                v(36, "rediramt", 0.30, "redirmobs", 1)
        ), "damage_redirection");

        // 魔法系護符

        add(map, "magicpenetration", 0xA05ACB, 4, List.of(
                v(37, "dmgtomagic", 0.10),
                v(38, "dmgtomagic", 0.15),
                v(39, "dmgtomagic", 0.20),
                v(40, "dmgtomagic", 0.25),
                v(41, "dmgtomagic", 0.15),
                v(42, "dmgtomagic", 0.25),
                v(43, "dmgtomagic", 0.32)
        ), "magic_penetration");

        add(map, "witcheryharm", 0x9C55AC, 4, List.of(
                v(45,
                        "badeffchance", 0.09,
                        "badefflvl", 2,
                        "badefftime", 5),
                v(46,
                        "badeffchance", 0.14,
                        "badefflvl", 1,
                        "badefftime", 5),
                v(65,
                        "badeffchance", 0.09,
                        "badefflvl", 1,
                        "badefftime", 10)
        ), "witchery_harm");

        add(map, "magicdefense", 0x7669C9, 3, List.of(
                v(74, "reducemagicdmg", 0.30),
                v(75, "reducemagicdmg", 0.50),
                v(76, "reducemagicdmg", 0.70)
        ), "magic_defense");

        // 特殊護符

        add(map, "fallingprotection", 0xBA955C, 6, List.of(
                v(49, "fallblocks", 3),
                v(50, "fallblocks", 6)
        ), "falling_protection");

        add(map, "haste", 0xD9AD56, 8, List.of(
                v(51, "breakspd", 1.10),
                v(52, "breakspd", 1.18),
                v(53, "breakspd", 1.25)
        ), "digging_speed");

        add(map, "criticalstrike", 0xCF6258, 12, List.of(
                v(54,
                        "critchance", 0.10,
                        "critdmg", 1.50),
                v(55,
                        "critchance", 0.10,
                        "critdmg", 2.00),
                v(56,
                        "critchance", 0.10,
                        "critdmg", 2.60),
                v(57,
                        "critchance", 0.15,
                        "critdmg", 1.50),
                v(58,
                        "critchance", 0.22,
                        "critdmg", 1.50),
                v(59,
                        "critchance", 0.15,
                        "critdmg", 2.00)
        ), "critical_strike");

        add(map, "seconddurability", 0x80A8A5, 8, List.of(
                v(60,
                        "recdurabilitychance", 0.28,
                        "recdurabilityamt", 0.10),
                v(61,
                        "recdurabilitychance", 0.25,
                        "recdurabilityamt", 0.18),
                v(62,
                        "recdurabilitychance", 0.22,
                        "recdurabilityamt", 0.25),
                v(63,
                        "recdurabilitychance", 0.32,
                        "recdurabilityamt", 0.16),
                v(64,
                        "recdurabilitychance", 0.44,
                        "recdurabilityamt", 0.12)
        ), "second_durability");

        add(map, "slaughterimpact", 0xB64B60, 12, List.of(
                v(17,
                        "impactamt", 0.60,
                        "impactrad", 4),
                v(66,
                        "impactamt", 0.90,
                        "impactrad", 4),
                v(67,
                        "impactamt", 1.40,
                        "impactrad", 4),
                v(68,
                        "impactamt", 0.70,
                        "impactrad", 6),
                v(69,
                        "impactamt", 0.70,
                        "impactrad", 8),
                v(70,
                        "impactamt", 1.00,
                        "impactrad", 6)
        ), "slaughter_impact");

        add(map, "lastresort", 0x554B8B, 7, List.of(
                v(77,
                        "lastresortblocks", 6,
                        "lastresortcooldown", 10),
                v(78,
                        "lastresortblocks", 6,
                        "lastresortcooldown", 6),
                v(79,
                        "lastresortblocks", 12,
                        "lastresortcooldown", 10)
        ), "last_resort");

        return map;
    }

    /**
     * 建立 HEE 護符變種。
     */
    private static CharmDefinition.Variant v(
            int heeId,
            Object... arguments
    ) {

        if (arguments.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Invalid charm properties: " + heeId
            );
        }

        Map<String, Double> properties =
                new LinkedHashMap<>();

        for (int i = 0; i < arguments.length; i += 2) {

            String key = (String) arguments[i];
            Number value = (Number) arguments[i + 1];

            properties.put(
                    key,
                    value.doubleValue()
            );
        }

        return new CharmDefinition.Variant(
                heeId,
                properties
        );
    }

    /**
     * 建立內建護符。
     *
     * 所有類型共用 fore_0 ~ fore_12。
     */
    private static void add(
            Map<ResourceLocation, CharmDefinition> map,
            String name,
            int backgroundColor,
            int foregroundIndex,
            List<CharmDefinition.Variant> variants,
            String... effectNames
    ) {

        if (foregroundIndex < 0 || foregroundIndex > 12) {
            throw new IllegalArgumentException(
                    "Invalid foreground index: "
                            + foregroundIndex
            );
        }

        List<CharmDefinition.EffectSpec> effects =
                new ArrayList<>();

        for (String effectName : effectNames) {

            effects.add(
                    new CharmDefinition.EffectSpec(
                            id(effectName),
                            Map.of()
                    )
            );
        }

        ResourceLocation icon = id(
                "item/charm/charm_fore_"
                        + foregroundIndex
        );

        // 原版只有生命竊取與最後防線不可疊加
        boolean stackable =
                !name.equals("lifesteal")
                        && !name.equals("lastresort");

        map.put(
                id(name),
                new CharmDefinition(
                        backgroundColor,
                        icon,
                        variants.size(),
                        stackable,
                        effects,
                        variants
                )
        );
    }

    /**
     * 建立原 HEE Metadata ID 到新護符變種的對照。
     */
    private static Map<Integer, CharmReference>
    buildLegacyIndex() {

        Map<Integer, CharmReference> index =
                new LinkedHashMap<>();

        for (var entry : BUILTIN.entrySet()) {

            List<CharmDefinition.Variant> variants =
                    entry.getValue().variants();

            for (int i = 0; i < variants.size(); i++) {

                int oldId = variants.get(i).heeId();

                CharmReference previous = index.putIfAbsent(
                        oldId,
                        new CharmReference(
                                entry.getKey(),
                                i + 1
                        )
                );

                if (previous != null) {
                    throw new IllegalStateException(
                            "Duplicate HEE charm ID: " + oldId
                    );
                }
            }
        }

        if (BUILTIN.size() != 21 || index.size() != 74) {
            throw new IllegalStateException(
                    "Invalid built-in HEE charms: types="
                            + BUILTIN.size()
                            + ", variants="
                            + index.size()
            );
        }

        return Map.copyOf(index);
    }

    /**
     * 用原 HEE 變種 ID 找到新的物品資料。
     *
     * 例如：
     * 57 -> criticalstrike, level 4
     */
    public static Optional<CharmReference> fromHeeId(
            int oldId
    ) {
        return Optional.ofNullable(
                HEE_VARIANTS.get(oldId)
        );
    }

    /**
     * 動態資料包註冊。
     */
    public static void register(IEventBus modBus) {
        modBus.addListener(
                CharmTypes::registerDatapackRegistries
        );
    }

    private static void registerDatapackRegistries(
            DataPackRegistryEvent.NewRegistry event
    ) {

        event.dataPackRegistry(
                CHARM_REGISTRY_KEY,
                CharmDefinition.CODEC,
                CharmDefinition.CODEC
        );
    }

    public static boolean isBuiltin(
            ResourceLocation type
    ) {
        return BUILTIN.containsKey(type);
    }

    /**
     * 優先讀取 Java 內建護符。
     */
    public static Optional<CharmDefinition> find(
            ResourceLocation type,
            HolderLookup.Provider registries
    ) {

        CharmDefinition builtin =
                BUILTIN.get(type);

        if (builtin != null) {
            return Optional.of(builtin);
        }

        if (registries == null) {
            return Optional.empty();
        }

        ResourceKey<CharmDefinition> key =
                ResourceKey.create(
                        CHARM_REGISTRY_KEY,
                        type
                );

        return registries.lookup(
                CHARM_REGISTRY_KEY
        ).flatMap(lookup ->
                lookup.get(key)
        ).map(Holder::value);
    }

    public static boolean isValid(
            ResourceLocation type,
            HolderLookup.Provider registries
    ) {
        return find(type, registries).isPresent();
    }

    /**
     * 取得實際變種的能力資料。
     */
    public static Optional<CharmDefinition.Variant>
    getVariant(
            ResourceLocation type,
            int level,
            HolderLookup.Provider registries
    ) {

        return find(type, registries)
                .flatMap(def ->
                        def.getVariant(level)
                );
    }

    private CharmTypes() {
    }
}
