package org.starrepublic.beyond_the_end.Compat.JEI;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipe;

import java.util.List;

@JeiPlugin
public class DragonEssenceAltarRecipeJEIPlugin implements IModPlugin {

    // ========================================================
    // JEI Recipe Type
    // ========================================================

    public static final RecipeType<EssenceAltarRecipe>
            DRAGON_ALTAR = RecipeType.create(
            Beyond_the_end.MODID,
            "dragon_essence_altar",
            EssenceAltarRecipe.class
    );

    // ========================================================
    // Plugin UID
    // ========================================================

    @Override
    public ResourceLocation getPluginUid() {

        return ResourceLocation.fromNamespaceAndPath(
                Beyond_the_end.MODID,
                "dragon_jei_plugin"
        );
    }

    // ========================================================
    // 註冊配方分類
    // ========================================================

    @Override
    public void registerCategories(
            IRecipeCategoryRegistration registration
    ) {

        registration.addRecipeCategories(
                new DragonEssenceAltarCategory(
                        registration.getJeiHelpers().getGuiHelper()
                )
        );
    }

    // ========================================================
    // 註冊配方
    // ========================================================

    @Override
    public void registerRecipes(
            IRecipeRegistration registration
    ) {

        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        // 取得已經載入的所有 Essence Altar 配方
        // 但只把 dragon 配方交給這個 JEI 分類
        List<EssenceAltarRecipe> recipes =
                level.getRecipeManager()
                        .getAllRecipesFor(
                                RecipeRegister.ESSENCE_ALTAR_TYPE.get()
                        )
                        .stream()
                        .map(RecipeHolder::value)
                        .filter(recipe ->
                                "dragon".equals(recipe.getEssence())
                        )
                        .toList();

        registration.addRecipes(
                DRAGON_ALTAR,
                recipes
        );
    }

    // ========================================================
    // 註冊工作方塊
    // ========================================================

    @Override
    public void registerRecipeCatalysts(
            IRecipeCatalystRegistration registration
    ) {

        registration.addRecipeCatalysts(
                DRAGON_ALTAR,
                BlockRegister.Essence_Altar.dragon.get()
        );
    }
}

