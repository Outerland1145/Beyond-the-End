package org.starrepublic.beyond_the_end.recipes.essence_altar;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;
import java.util.Objects;

public record EssenceAltarRecipeInput(
        String essence,
        List<ItemStack> stacks
) implements RecipeInput {

    public EssenceAltarRecipeInput {

        Objects.requireNonNull(essence, "essence");
        Objects.requireNonNull(stacks, "stacks");

        // 移除空物品，並建立輸入快照
        stacks = stacks.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    @Override
    public ItemStack getItem(int index) {
        return stacks.get(index);
    }

    @Override
    public int size() {
        return stacks.size();
    }
}

