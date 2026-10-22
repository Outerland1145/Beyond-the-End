package org.starrepublic.beyond_the_end.recipes.CharmRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record CharmRuneInput(
        List<ItemStack> stacks
) implements RecipeInput {

    public CharmRuneInput {
        stacks = List.copyOf(stacks);
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
