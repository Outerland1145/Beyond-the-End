package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import org.starrepublic.beyond_the_end.Register.ItemRegister;
import top.theillusivec4.curios.api.CuriosApi;

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

        public double get(
                String name,
                double fallback
        ) {
            return variant.getProperty(name, fallback);
        }
    }

    /**
     * 只取得 Curios charm 槽中的護符袋。
     *
     * 手持護符、背包內的護符與未裝備的護符袋
     * 全部不會被視為啟用。
     */
    public static List<ActiveCharm> getActive(
            Player player
    ) {

        List<ActiveCharm> result = new ArrayList<>();

        Set<ResourceLocation> nonStackable =
                new HashSet<>();

        CuriosApi.getCuriosInventory(player)
                .ifPresent(curios -> {

                    for (var slot :
                            curios.findCurios("charm")) {

                        ItemStack pouch = slot.stack();

                        if (!pouch.is(
                                ItemRegister.CHARMPOUCH.get()
                        )) {
                            continue;
                        }

                        collectPouch(
                                player,
                                pouch,
                                result,
                                nonStackable
                        );
                    }
                });

        return List.copyOf(result);
    }

    /**
     * 從護符袋讀取內部三個物品。
     *
     * ItemContainerContents 本身是不可變資料，
     * 使用 copyInto() 複製內容以供讀取。
     */
    private static void collectPouch(
            Player player,
            ItemStack pouch,
            List<ActiveCharm> result,
            Set<ResourceLocation> nonStackable
    ) {

        NonNullList<ItemStack> contents =
                NonNullList.withSize(
                        CharmPouch.SLOT_COUNT,
                        ItemStack.EMPTY
                );

        pouch.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        ).copyInto(contents);

        for (ItemStack stack : contents) {

            if (stack.isEmpty()) {
                continue;
            }

            collect(
                    player,
                    stack,
                    result,
                    nonStackable
            );
        }
    }

    /**
     * 驗證單一護符。
     *
     * 保留原本的疊加規則與效果變種查詢。
     */
    private static void collect(
            Player player,
            ItemStack stack,
            List<ActiveCharm> result,
            Set<ResourceLocation> nonStackable
    ) {

        CharmData data =
                CharmHelper.getData(stack)
                        .orElse(null);

        if (data == null) {
            return;
        }

        CharmDefinition definition =
                CharmTypes.find(
                        data.type(),
                        player.registryAccess()
                ).orElse(null);

        if (definition == null) {
            return;
        }

        CharmDefinition.Variant variant =
                definition.getVariant(
                        data.level()
                ).orElse(null);

        if (variant == null) {
            return;
        }

        /*
         * HEE 部分特殊護符不允許疊加。
         */
        if (!definition.stackable()
                && !nonStackable.add(data.type())) {
            return;
        }

        result.add(
                new ActiveCharm(
                        data.type(),
                        definition,
                        variant
                )
        );
    }

    /**
     * 統一分派護符效果。
     *
     * 你的 CharmEffectEvents 原本就使用這個方法，
     * 因此不需要把全部效果事件重寫。
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
