package org.starrepublic.beyond_the_end.Compat.JEI;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipe;

import java.util.List;

@JeiPlugin
public class FireEssenceAltarRecipeJEIPlugin implements IModPlugin {

    public static final RecipeType<EssenceAltarRecipe>
            FIRE_ALTAR = RecipeType.create(
            "beyond_the_end",
            "fire_essence_altar",
            EssenceAltarRecipe.class
    );

    @Override
    public ResourceLocation getPluginUid() {

        return ResourceLocation.fromNamespaceAndPath(
                "beyond_the_end",
                "fire_jei_plugin"
        );
    }

    @Override
    public void registerCategories(
            IRecipeCategoryRegistration registration
    ) {

        registration.addRecipeCategories(
                new FireEssenceAltarCategory(
                        registration.getJeiHelpers().getGuiHelper()
                )
        );
    }

    @Override
    public void registerRecipes(
            IRecipeRegistration registration
    ) {

        var level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        List<EssenceAltarRecipe> recipes =
                level.getRecipeManager()
                        .getAllRecipesFor(
                                RecipeRegister.ESSENCE_ALTAR_TYPE.get()
                        )
                        .stream()
                        .map(RecipeHolder::value)
                        .filter(recipe ->
                                "fire".equals(recipe.getEssence())
                        )
                        .toList();

        registration.addRecipes(
                FIRE_ALTAR,
                recipes
        );
    }

    @Override
    public void registerRecipeCatalysts(
            IRecipeCatalystRegistration registration
    ) {

        registration.addRecipeCatalysts(
                FIRE_ALTAR,
                BlockRegister.Essence_Altar.fires.get()
        );
    }
}
