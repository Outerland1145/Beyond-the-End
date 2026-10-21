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

import java.util.Locale;

public class DragonEssenceAltarCategory
        implements IRecipeCategory<EssenceAltarRecipe> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 86;

    private final IDrawable icon;

    public DragonEssenceAltarCategory(
            IGuiHelper guiHelper
    ) {

        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(
                        BlockRegister.Essence_Altar.dragon.get()
                )
        );
    }

    // ========================================================
    // Category
    // ========================================================

    @Override
    public RecipeType<EssenceAltarRecipe> getRecipeType() {
        return DragonEssenceAltarRecipeJEIPlugin.DRAGON_ALTAR;
    }

    @Override
    public Component getTitle() {

        return Component.translatable(
                "jei.beyond_the_end.dragon_altar"
        );
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    // ========================================================
    // 配方物品欄位
    // ========================================================

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            EssenceAltarRecipe recipe,
            IFocusGroup focuses
    ) {

        // 基座輸入物品
        builder.addInputSlot(18, 10)
                .setStandardSlotBackground()
                .addIngredients(
                        recipe.getInputIngredient()
                );

        // 基座輸出物品
        builder.addOutputSlot(140, 10)
                .setOutputSlotBackground()
                .addItemStack(
                        recipe.getResult().copy()
                );
    }

    // ========================================================
    // 繪製配方成本與時間
    // ========================================================

    @Override
    public void draw(
            EssenceAltarRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {

        var font = Minecraft.getInstance().font;

        int baseCost = recipe.getCost();

        // 普通模式
        int normalCost = baseCost;
        double normalSeconds = baseCost;

        // 加速模式
        int speedCost = baseCost * 2;
        double speedSeconds = baseCost * 0.5D;

        // 效率模式，向上取整
        int efficiencyCost = (baseCost + 1) / 2;
        double efficiencySeconds = baseCost * 2.0D;

        // 中間箭頭
        guiGraphics.drawString(
                font,
                "→",
                86,
                14,
                0xFF606060,
                false
        );

        // ====================================================
        // 普通模式
        // ====================================================

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "jei.beyond_the_end.normal",
                        normalCost,
                        formatSeconds(normalSeconds)
                ),
                4,
                42,
                0xFF404040,
                false
        );

        // ====================================================
        // 加速模式
        // ====================================================

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "jei.beyond_the_end.speed",
                        speedCost,
                        formatSeconds(speedSeconds)
                ),
                4,
                56,
                0xFF404040,
                false
        );

        // ====================================================
        // 效率模式
        // ====================================================

        guiGraphics.drawString(
                font,
                Component.translatable(
                        "jei.beyond_the_end.efficiency",
                        efficiencyCost,
                        formatSeconds(efficiencySeconds)
                ),
                4,
                70,
                0xFF404040,
                false
        );
    }

    private static String formatSeconds(double seconds) {

        return String.format(
                Locale.ROOT,
                "%.1f",
                seconds
        );
    }
}

