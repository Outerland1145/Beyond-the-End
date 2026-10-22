package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CharmTooltip {

    private static MutableComponent text(
            String key,
            Object... arguments
    ) {
        return Component.translatable(
                "tooltip.beyond_the_end.charm." + key,
                arguments
        );
    }

    private static String number(double value) {
        return String.format(
                Locale.ROOT,
                "%.2f",
                value
        );
    }

    private static String percent(double value) {
        return String.format(
                Locale.ROOT,
                "%.0f%%",
                value * 100.0D
        );
    }

    private static String amount(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }

        return number(value);
    }

    public static void append(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip
    ) {

        CharmData data =
                CharmHelper.getData(stack).orElse(null);

        if (data == null) {
            tooltip.add(
                    text("invalid")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        CharmDefinition definition = CharmTypes.find(
                data.type(),
                context.registries()
        ).orElse(null);

        if (definition == null) {
            tooltip.add(
                    text("unknown_type", data.type())
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        CharmDefinition.Variant variant = definition
                .getVariant(data.level())
                .orElse(null);

        if (variant == null) {
            tooltip.add(
                    text("invalid_variant", data.level())
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        // 護符變種編號。
        tooltip.add(
                text("variant", data.level())
                        .withStyle(ChatFormatting.DARK_GRAY)
        );

        // 作用說明。
        String descriptionKey =
                "type." + data.type().getPath()
                        .replace('/', '.');

        tooltip.add(
                text(descriptionKey)
                        .withStyle(ChatFormatting.GRAY)
        );

        // 根據效果類型顯示對應數值。
        for (CharmDefinition.EffectSpec effect :
                definition.effects()) {

            appendEffect(
                    tooltip,
                    effect.type().toString(),
                    variant.properties()
            );
        }

        if (!definition.stackable()) {
            tooltip.add(
                    text("cannot_stack")
                            .withStyle(ChatFormatting.DARK_RED)
            );
        }

        tooltip.add(
                text("activation")
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    private static void stat(
            List<Component> tooltip,
            String name,
            Object... args
    ) {

        tooltip.add(
                text("stat." + name, args)
                        .withStyle(ChatFormatting.GREEN)
        );
    }

    private static double get(
            Map<String, Double> properties,
            String key
    ) {
        return properties.getOrDefault(key, 0.0D);
    }

    private static void appendEffect(
            List<Component> tooltip,
            String effect,
            Map<String, Double> p
    ) {

        switch (effect) {

            case "beyond_the_end:attack_damage" ->
                    stat(
                            tooltip,
                            "attack",
                            percent(get(p, "dmg") - 1.0D)
                    );

            case "beyond_the_end:movement_speed" ->
                    stat(
                            tooltip,
                            "speed",
                            percent(get(p, "spd") - 1.0D)
                    );

            case "beyond_the_end:health_regeneration" ->
                    stat(
                            tooltip,
                            "regeneration",
                            percent(get(p, "regenspd"))
                    );

            case "beyond_the_end:damage_reduction" ->
                    stat(
                            tooltip,
                            "defense",
                            percent(get(p, "reducedmg"))
                    );

            case "beyond_the_end:experience_gain" ->
                    stat(
                            tooltip,
                            "experience",
                            percent(get(p, "exp") - 1.0D)
                    );

            case "beyond_the_end:blocking_reduction" ->
                    stat(
                            tooltip,
                            "blocking",
                            percent(get(p, "reducedmgblock"))
                    );

            case "beyond_the_end:blocking_reflection" -> {

                stat(
                        tooltip,
                        "blocking",
                        percent(get(p, "reducedmgblock"))
                );

                stat(
                        tooltip,
                        "reflection",
                        percent(get(p, "blockreflectdmg"))
                );
            }

            case "beyond_the_end:blocking_repulsion" -> {

                stat(
                        tooltip,
                        "blocking",
                        percent(get(p, "reducedmgblock"))
                );

                stat(
                        tooltip,
                        "repulsion",
                        amount(get(p, "blockrepulsepower"))
                );
            }

            case "beyond_the_end:digestive_recovery" ->
                    stat(
                            tooltip,
                            "digestive",
                            amount(get(p, "healthperhunger"))
                    );

            case "beyond_the_end:life_steal" ->
                    stat(
                            tooltip,
                            "lifesteal",
                            amount(get(p, "stealdealt")),
                            amount(get(p, "stealhealth"))
                    );

            case "beyond_the_end:damage_redirection" ->
                    stat(
                            tooltip,
                            "redirection",
                            percent(get(p, "rediramt")),
                            amount(get(p, "redirmobs"))
                    );

            case "beyond_the_end:magic_penetration" ->
                    stat(
                            tooltip,
                            "penetration",
                            percent(get(p, "dmgtomagic"))
                    );

            case "beyond_the_end:witchery_harm" ->
                    stat(
                            tooltip,
                            "witchery",
                            percent(get(p, "badeffchance")),
                            amount(get(p, "badefftime")),
                            amount(get(p, "badefflvl"))
                    );

            case "beyond_the_end:magic_defense" ->
                    stat(
                            tooltip,
                            "magic_defense",
                            percent(get(p, "reducemagicdmg"))
                    );

            case "beyond_the_end:falling_protection" ->
                    stat(
                            tooltip,
                            "falling",
                            amount(get(p, "fallblocks"))
                    );

            case "beyond_the_end:digging_speed" ->
                    stat(
                            tooltip,
                            "haste",
                            percent(get(p, "breakspd") - 1.0D)
                    );

            case "beyond_the_end:critical_strike" -> {

                stat(
                        tooltip,
                        "critical_chance",
                        percent(get(p, "critchance"))
                );

                stat(
                        tooltip,
                        "critical_damage",
                        percent(get(p, "critdmg"))
                );
            }

            case "beyond_the_end:second_durability" ->
                    stat(
                            tooltip,
                            "durability",
                            percent(get(p, "recdurabilitychance")),
                            percent(get(p, "recdurabilityamt"))
                    );

            case "beyond_the_end:slaughter_impact" ->
                    stat(
                            tooltip,
                            "slaughter",
                            percent(get(p, "impactamt")),
                            amount(get(p, "impactrad"))
                    );

            case "beyond_the_end:last_resort" ->
                    stat(
                            tooltip,
                            "last_resort",
                            amount(get(p, "lastresortblocks")),
                            amount(get(p, "lastresortcooldown"))
                    );

            default -> {

                // 資料包效果沒有專用 Tooltip 時，
                // 至少顯示 ID 與原始參數。
                tooltip.add(
                        Component.literal(effect)
                                .withStyle(ChatFormatting.YELLOW)
                );

                p.forEach((key, value) ->
                        tooltip.add(
                                Component.literal(
                                        "  " + key + ": " + number(value)
                                ).withStyle(ChatFormatting.GRAY)
                        )
                );
            }
        }
    }

    private CharmTooltip() {
    }
}
