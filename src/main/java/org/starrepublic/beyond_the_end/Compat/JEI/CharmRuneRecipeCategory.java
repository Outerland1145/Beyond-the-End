package org.starrepublic.beyond_the_end.Compat.JEI;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.starrepublic.beyond_the_end.Register.ItemRegister;

import java.util.List;

public final class CharmRuneRecipeCategory
        extends AbstractRecipeCategory<
        CharmRuneJeiPlugin.DisplayRecipe
        > {

    private static final int WIDTH = 148;
    private static final int HEIGHT = 54;

    // 固定五個符文輸入槽
    private static final int INPUT_COUNT = 5;

    private static final int INPUT_X = 3;
    private static final int INPUT_Y = 18;
    private static final int INPUT_SPACING = 19;

    // 合成結果位置
    private static final int OUTPUT_X = 126;
    private static final int OUTPUT_Y = 18;

    // 靜態箭頭顏色
    private static final int ARROW_COLOR = 0xFF888888;

    public CharmRuneRecipeCategory(
            IGuiHelper guiHelper
    ) {

        super(
                CharmRuneJeiPlugin.CHARM_RUNE,

                Component.translatable(
                        "jei.beyond_the_end.charm_rune"
                ),

                guiHelper.createDrawableIngredient(
                        VanillaTypes.ITEM_STACK,
                        new ItemStack(
                                ItemRegister.CHARMPOUCH.get()
                        )
                ),

                WIDTH,
                HEIGHT
        );
    }

    /**
     * 設置配方。
     *
     * 永遠建立五個符文輸入槽。
     *
     * 材料不足五個時，
     * 剩餘的欄位會保持空白。
     */
    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            CharmRuneJeiPlugin.DisplayRecipe recipe,
            IFocusGroup focuses
    ) {

        List<ItemStack> inputs = recipe.inputs();

        /*
         * 固定建立五個槽位。
         *
         * 不能再使用：
         *
         * i < inputs.size()
         *
         * 否則槽位數量會隨配方變化。
         */
        for (int i = 0; i < INPUT_COUNT; i++) {

            var slot = builder.addInputSlot(
                            INPUT_X + i * INPUT_SPACING,
                            INPUT_Y
                    )
                    .setStandardSlotBackground();

            /*
             * 只有存在對應材料時才放入物品。
             *
             * 不存在材料的欄位仍然建立，
             * 但不添加 ItemStack。
             */
            if (i < inputs.size()) {

                ItemStack input = inputs.get(i);

                if (!input.isEmpty()) {
                    slot.addItemStack(
                            input.copy()
                    );
                }
            }
        }

        /*
         * 合成結果。
         *
         * 保留完整的 CharmData Component，
         * 以正確顯示護符類型與變種。
         */
        builder.addOutputSlot(
                        OUTPUT_X,
                        OUTPUT_Y
                )
                .setOutputSlotBackground()
                .addItemStack(
                        recipe.output().copy()
                );
    }

    /**
     * 繪製普通靜態箭頭。
     *
     * 使用 Minecraft 的 GuiGraphics，
     * 不依賴 JEI 已棄用的動畫箭頭 API。
     */
    @Override
    public void draw(
            CharmRuneJeiPlugin.DisplayRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {

        drawArrow(graphics, 101, 22);
    }

    /**
     * 繪製向右箭頭。
     *
     * 不需要額外 PNG，
     * 也不需要使用 JEI 動畫元件。
     */
    private static void drawArrow(
            GuiGraphics graphics,
            int x,
            int y
    ) {

        // 箭頭主幹
        graphics.fill(
                x,
                y + 4,
                x + 13,
                y + 7,
                ARROW_COLOR
        );

        // 箭頭後方較寬的部分
        graphics.fill(
                x + 10,
                y + 1,
                x + 13,
                y + 10,
                ARROW_COLOR
        );

        // 箭頭中段
        graphics.fill(
                x + 13,
                y + 3,
                x + 16,
                y + 8,
                ARROW_COLOR
        );

        // 箭頭尖端
        graphics.fill(
                x + 16,
                y + 4,
                x + 19,
                y + 7,
                ARROW_COLOR
        );
    }

    /**
     * 返回原始配方 ID。
     *
     * 例如：
     * beyond_the_end:charm_rune/hee_0
     */
    @Override
    public ResourceLocation getRegistryName(
            CharmRuneJeiPlugin.DisplayRecipe recipe
    ) {
        return recipe.id();
    }
}
