package org.starrepublic.beyond_the_end.Items.charm.rune;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.starrepublic.beyond_the_end.Register.DataComponentRegister;
import org.starrepublic.beyond_the_end.Register.ItemRegister;

import java.util.Optional;

public final class RuneHelper {

    /**
     * 建立指定種類的符文。
     */
    public static ItemStack create(ResourceLocation type) {

        ItemStack stack = new ItemStack(
                ItemRegister.RUNEITEM.get()
        );

        stack.set(
                DataComponentRegister.RUNE_TYPE.get(),
                new RuneData(type)
        );

        return stack;
    }

    /**
     * 取得 ItemStack 的符文種類。
     */
    public static Optional<ResourceLocation> getType(
            ItemStack stack
    ) {

        if (!stack.is(ItemRegister.RUNEITEM.get())) {
            return Optional.empty();
        }

        RuneData data = stack.get(
                DataComponentRegister.RUNE_TYPE.get()
        );

        return data == null
                ? Optional.empty()
                : Optional.of(data.id());
    }

    /**
     * 是否為指定種類的符文。
     */
    public static boolean isRune(
            ItemStack stack,
            ResourceLocation type
    ) {
        return getType(stack)
                .filter(type::equals)
                .isPresent();
    }

    /**
     * 是否為已註冊的有效符文。
     */
    public static boolean isValidRune(
            ItemStack stack,
            HolderLookup.Provider registries
    ) {

        return getType(stack)
                .filter(id ->
                        RuneTypes.isValid(id, registries)
                )
                .isPresent();
    }

    /**
     * 將全部符文加入創造模式物品欄。
     *
     * 先加入六種原始符文，
     * 再讀取目前世界的資料包符文。
     */
    public static void addCreativeStacks(
            CreativeModeTab.Output output,
            HolderLookup.Provider registries
    ) {

        for (ResourceLocation id :
                RuneTypes.BUILTIN_ORDER) {

            output.accept(create(id));
        }

        registries.lookup(RuneTypes.RUNE_REGISTRY_KEY)
                .ifPresent(lookup ->
                        lookup.listElements()
                                .map(holder ->
                                        holder.key().location()
                                )
                                .filter(id ->
                                        !RuneTypes.isBuiltin(id)
                                )
                                .sorted()
                                .forEach(id ->
                                        output.accept(create(id))
                                )
                );
    }

    private RuneHelper() {
    }
}
