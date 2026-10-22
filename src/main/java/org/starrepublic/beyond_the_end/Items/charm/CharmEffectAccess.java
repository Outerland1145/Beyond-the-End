package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public final class CharmEffectAccess {

    public record ActiveCharm(
            ResourceLocation type,
            CharmDefinition definition,
            CharmDefinition.Variant variant
    ) {

        public double get(String name) {
            return variant.getProperty(name, 0.0D);
        }

        public double get(String name, double fallback) {
            return variant.getProperty(name, fallback);
        }
    }

    /**
     * 臨時測試接口：
     * 主手與副手中的護符視為啟用。
     *
     * 後續製作護符袋時，只需要修改這個方法。
     */
    public static List<ActiveCharm> getActive(Player player) {

        List<ActiveCharm> result = new ArrayList<>();
        Set<ResourceLocation> nonStackable = new HashSet<>();

        collect(player, player.getMainHandItem(), result, nonStackable);
        collect(player, player.getOffhandItem(), result, nonStackable);

        return List.copyOf(result);
    }

    private static void collect(
            Player player,
            ItemStack stack,
            List<ActiveCharm> result,
            Set<ResourceLocation> nonStackable
    ) {

        CharmData data = CharmHelper.getData(stack).orElse(null);

        if (data == null) {
            return;
        }

        CharmDefinition definition = CharmTypes.find(
                data.type(),
                player.registryAccess()
        ).orElse(null);

        if (definition == null) {
            return;
        }

        CharmDefinition.Variant variant = definition
                .getVariant(data.level())
                .orElse(null);

        if (variant == null) {
            return;
        }

        if (!definition.stackable()
                && !nonStackable.add(data.type())) {
            return;
        }

        result.add(new ActiveCharm(
                data.type(),
                definition,
                variant
        ));
    }

    /**
     * 依護符資料所宣告的效果類型分派。
     *
     * 資料包護符可使用相同的效果處理器。
     */
    public static void forEachEffect(
            Player player,
            BiConsumer<ActiveCharm, String> consumer
    ) {

        for (ActiveCharm charm : getActive(player)) {
            for (CharmDefinition.EffectSpec effect :
                    charm.definition().effects()) {

                consumer.accept(
                        charm,
                        effect.type().toString()
                );
            }
        }
    }

    private CharmEffectAccess() {
    }
}
