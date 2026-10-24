package org.starrepublic.beyond_the_end.recipes.enhancedbrewing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record EnhancedBrewingInput(
        ItemStack bottle,
        ItemStack ingredient
) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> bottle;
            case 1 -> ingredient;
            default -> throw new IllegalArgumentException(
                    "Invalid brewing input slot: " + index
            );
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
