package org.starrepublic.beyond_the_end.Compat.JEI;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipe;

public class FireEssenceAltarCategory
        implements IRecipeCategory<EssenceAltarRecipe> {

    private final IDrawable icon;

    public FireEssenceAltarCategory(IGuiHelper helper) {

        icon = helper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(
                        BlockRegister.Essence_Altar.fires.get()
                )
        );
    }

    @Override
    public RecipeType<EssenceAltarRecipe> getRecipeType() {
        return FireEssenceAltarRecipeJEIPlugin.FIRE_ALTAR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(
                "jei.beyond_the_end.fire_altar"
        );
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 176;
    }

    @Override
    public int getHeight() {
        return 86;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            EssenceAltarRecipe recipe,
            IFocusGroup focuses
    ) {

        builder.addInputSlot(18, 10)
                .setStandardSlotBackground()
                .addIngredients(recipe.getInputIngredient());

        builder.addOutputSlot(140, 10)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResult().copy());
    }

    @Override
    public void draw(
            EssenceAltarRecipe recipe,
            IRecipeSlotsView slots,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {

        var font = Minecraft.getInstance().font;

        int cost = recipe.getCost();

        graphics.drawString(
                font,
                "→",
                86,
                14,
                0xFF606060,
                false
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "jei.beyond_the_end.normal",
                        cost,
                        String.format(
                                java.util.Locale.ROOT,
                                "%.1f",
                                (double) cost
                        )
                ),
                4, 42, 0xFF404040, false
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "jei.beyond_the_end.speed",
                        cost * 2,
                        String.format(
                                java.util.Locale.ROOT,
                                "%.1f",
                                cost / 2.0D
                        )
                ),
                4, 56, 0xFF404040, false
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "jei.beyond_the_end.efficiency",
                        (cost + 1) / 2,
                        String.format(
                                java.util.Locale.ROOT,
                                "%.1f",
                                cost * 2.0D
                        )
                ),
                4, 70, 0xFF404040, false
        );
    }
}
