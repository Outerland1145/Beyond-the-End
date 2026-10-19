package org.starrepublic.beyond_the_end.recipes.essence_altar;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;

public class EssenceAltarRecipe implements Recipe<EssenceAltarRecipeInput> {

    private final String essence;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int cost;

    public EssenceAltarRecipe(
            String essence,
            Ingredient ingredient,
            ItemStack result,
            int cost
    ) {
        this.essence = essence;
        this.ingredient = ingredient;
        this.result = result.copy();
        this.cost = cost;
    }

    public String getEssence() {
        return essence;
    }

    public Ingredient getInputIngredient() {
        return ingredient;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public boolean matches(
            EssenceAltarRecipeInput input,
            Level level
    ) {
        return essence.equals(input.essence())
                && input.size() == 1
                && ingredient.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(
            EssenceAltarRecipeInput input,
            HolderLookup.Provider registries
    ) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(
            HolderLookup.Provider registries
    ) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        return list;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegister.ESSENCE_ALTAR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegister.ESSENCE_ALTAR_TYPE.get();
    }
}


