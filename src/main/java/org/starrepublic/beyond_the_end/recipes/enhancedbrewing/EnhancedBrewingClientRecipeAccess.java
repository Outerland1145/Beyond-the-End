package org.starrepublic.beyond_the_end.recipes.enhancedbrewing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.crafting.RecipeManager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class EnhancedBrewingClientRecipeAccess {

    @Nullable
    public static RecipeManager getManager() {

        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) {
            return null;
        }

        return level.getRecipeManager();
    }

    private EnhancedBrewingClientRecipeAccess() {
    }
}
