package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.starrepublic.beyond_the_end.Register.DataComponentRegister;
import org.starrepublic.beyond_the_end.Register.ItemRegister;

import java.util.Optional;

public final class CharmHelper {

    /**
     * 建立指定種類與等級的護符。
     */
    public static ItemStack create(
            ResourceLocation type,
            int level
    ) {

        ItemStack stack = new ItemStack(
                ItemRegister.CHARMITEM.get()
        );

        stack.set(
                DataComponentRegister.CHARM_DATA.get(),
                new CharmData(
                        type,
                        Math.clamp(level, 1, 64)
                )
        );

        return stack;
    }

    /**
     * 預設建立第一級護符。
     */
    public static ItemStack create(ResourceLocation type) {
        return create(type, 1);
    }

    public static Optional<CharmData> getData(
            ItemStack stack
    ) {

        if (!stack.is(ItemRegister.CHARMITEM.get())) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                stack.get(
                        DataComponentRegister.CHARM_DATA.get()
                )
        );
    }

    public static Optional<CharmDefinition> getDefinition(
            ItemStack stack,
            HolderLookup.Provider registries
    ) {

        return getData(stack).flatMap(data ->
                CharmTypes.find(
                        data.type(),
                        registries
                )
        );
    }

    public static boolean isValidCharm(
            ItemStack stack,
            HolderLookup.Provider registries
    ) {

        return getData(stack)
                .filter(data ->
                        CharmTypes.find(
                                        data.type(),
                                        registries
                                )
                                .filter(def ->
                                        data.level() <= def.maxLevel()
                                )
                                .isPresent()
                )
                .isPresent();
    }

    /**
     * 創造模式物品欄先顯示各種類的一級護符。
     *
     * 所有等級仍可透過 create() 或指令取得。
     */
    public static void addCreativeStacks(
            CreativeModeTab.Output output,
            HolderLookup.Provider registries
    ) {

        // 先加入所有 Java 內建護符
        for (ResourceLocation type : CharmTypes.BUILTIN_ORDER) {

            CharmTypes.find(type, registries)
                    .ifPresent(definition ->
                            addVariants(
                                    output,
                                    type,
                                    definition
                            )
                    );
        }

        // 再加入資料包新增的護符
        registries.lookup(
                CharmTypes.CHARM_REGISTRY_KEY
        ).ifPresent(lookup ->
                lookup.listElements()
                        .map(holder ->
                                holder.key().location()
                        )
                        .filter(type ->
                                !CharmTypes.isBuiltin(type)
                        )
                        .sorted()
                        .forEach(type ->
                                CharmTypes.find(type, registries)
                                        .ifPresent(definition ->
                                                addVariants(
                                                        output,
                                                        type,
                                                        definition
                                                )
                                        )
                        )
        );
    }
    public static Optional<CharmDefinition.Variant>
    getVariant(
            ItemStack stack,
            HolderLookup.Provider registries
    ) {

        return getData(stack)
                .flatMap(data ->
                        CharmTypes.getVariant(
                                data.type(),
                                data.level(),
                                registries
                        )
                );
    }

    /**
     * 將一個護符種類的所有變種加入創造模式。
     */
    private static void addVariants(
            CreativeModeTab.Output output,
            ResourceLocation type,
            CharmDefinition definition
    ) {

        for (int level = 1;
             level <= definition.maxLevel();
             level++) {

            output.accept(
                    create(type, level)
            );
        }
    }

    private CharmHelper() {
    }
}
