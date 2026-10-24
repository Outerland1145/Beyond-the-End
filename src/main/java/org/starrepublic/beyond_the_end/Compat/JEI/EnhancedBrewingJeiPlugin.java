package org.starrepublic.beyond_the_end.Compat.JEI;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;

import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;

import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.starrepublic.beyond_the_end.Beyond_the_end;

import org.starrepublic.beyond_the_end.GUI.EnhancedBrewingScreen;
import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.recipes.enhancedbrewing.EnhancedBrewingRecipe;
import org.starrepublic.beyond_the_end.recipes.enhancedbrewing.EnhancedBrewingRecipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
import net.minecraft.client.gui.GuiGraphics;

@JeiPlugin
public final class EnhancedBrewingJeiPlugin
        implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "enhanced_brewing_jei"
            );

    /**
     * JEI 專屬顯示資料。
     *
     * 僅用於強化釀造台限定配方。
     */
    public record DisplayRecipe(
            ResourceLocation id,
            ItemStack input,
            List<ItemStack> ingredients,
            ItemStack output
    ) {

        public DisplayRecipe {

            input = input.copy();
            output = output.copy();

            ingredients = ingredients.stream()
                    .map(ItemStack::copy)
                    .toList();
        }
    }

    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            RecipeType.create(
                    Beyond_the_end.MODID,
                    "enhanced_brewing",
                    DisplayRecipe.class
            );

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(
            IRecipeCategoryRegistration registration
    ) {

        registration.addRecipeCategories(
                new EnhancedCategory(
                        registration.getJeiHelpers()
                                .getGuiHelper()
                )
        );
    }

    /**
     * 讀取實際 RecipeManager 配方。
     *
     * 共同配方交給 JEI 原版釀造分類。
     * 限定配方交給強化釀造分類。
     */
    @Override
    public void registerRecipes(
            IRecipeRegistration registration
    ) {

        ClientLevel level =
                Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        List<IJeiBrewingRecipe> sharedRecipes =
                new ArrayList<>();

        List<DisplayRecipe> exclusiveRecipes =
                new ArrayList<>();

        for (var holder : EnhancedBrewingRecipes.all(level)) {

            EnhancedBrewingRecipe recipe =
                    holder.value();

            ItemStack input =
                    recipe.input().copy();

            ItemStack output =
                    recipe.output().copy();

            List<ItemStack> ingredients =
                    Arrays.stream(
                                    recipe.ingredient().getItems()
                            )
                            .filter(stack -> !stack.isEmpty())
                            .map(ItemStack::copy)
                            .toList();

            if (input.isEmpty()
                    || output.isEmpty()
                    || ingredients.isEmpty()) {
                continue;
            }

            if (recipe.allowVanillaStand()) {

                /*
                 * 使用 JEI 官方提供的原版釀造配方工廠。
                 *
                 * 產生 IJeiBrewingRecipe，
                 * 直接加入原版 RecipeTypes.BREWING。
                 */
                IJeiBrewingRecipe shared =
                        registration.getVanillaRecipeFactory()
                                .createBrewingRecipe(
                                        ingredients,
                                        input,
                                        output,
                                        holder.id()
                                );

                sharedRecipes.add(shared);

            } else {

                exclusiveRecipes.add(
                        new DisplayRecipe(
                                holder.id(),
                                input,
                                ingredients,
                                output
                        )
                );
            }
        }

        // 原版 JEI 釀造分類：只加入共用資料包配方。
        registration.addRecipes(
                RecipeTypes.BREWING,
                sharedRecipes
        );

        // 強化台專屬分類：只加入限定配方。
        registration.addRecipes(
                RECIPE_TYPE,
                exclusiveRecipes
        );
    }

    /**
     * 強化釀造台同時屬於兩個 JEI 分類的製作設備。
     *
     * 這讓普通釀造配方頁面也出現強化釀造台圖示。
     */
    @Override
    public void registerRecipeCatalysts(
            IRecipeCatalystRegistration registration
    ) {

        registration.addRecipeCatalysts(
                RecipeTypes.BREWING,
                BlockRegister.ENHANCED_BREWING_STAND_BLOCK.get()
        );

        registration.addRecipeCatalysts(
                RECIPE_TYPE,
                BlockRegister.ENHANCED_BREWING_STAND_BLOCK.get()
        );
    }

    /**
     * 點擊強化釀造台 GUI 的釀造進度區域，
     * 可以查看普通與限定兩種釀造配方。
     */
    @Override
    public void registerGuiHandlers(
            IGuiHandlerRegistration registration
    ) {

        registration.addRecipeClickArea(
                EnhancedBrewingScreen.class,
                97,
                16,
                12,
                28,
                RecipeTypes.BREWING,
                RECIPE_TYPE
        );
    }

    /**
     * 強化釀造台限定配方分類。
     *
     * 尺寸與材料欄位排列參照
     * JEI 19.51 原版 BrewingRecipeCategory。
     */
    public static final class EnhancedCategory
            extends AbstractRecipeCategory<DisplayRecipe> {

        private static final ResourceLocation BREW_PROGRESS =
                ResourceLocation.withDefaultNamespace(
                        "container/brewing_stand/brew_progress"
                );

        private static final ResourceLocation BUBBLES =
                ResourceLocation.withDefaultNamespace(
                        "container/brewing_stand/bubbles"
                );

        private static final ResourceLocation FUEL =
                ResourceLocation.withDefaultNamespace(
                        "container/brewing_stand/fuel_length"
                );
        private static final ResourceLocation BREWING_BACKGROUND =
                ResourceLocation.withDefaultNamespace(
                        "textures/gui/container/brewing_stand.png"
                );

        public EnhancedCategory(
                IGuiHelper guiHelper
        ) {

            super(
                    RECIPE_TYPE,

                    Component.translatable(
                            "jei.beyond_the_end.enhanced_brewing"
                    ),

                    guiHelper.createDrawableIngredient(
                            VanillaTypes.ITEM_STACK,
                            new ItemStack(
                                    BlockRegister.ENHANCED_BREWING_STAND_BLOCK.get()
                            )
                    ),

                    // 與 JEI 原版釀造分類相同的寬高
                    114,
                    61
            );
        }

        @Override
        public void setRecipe(
                IRecipeLayoutBuilder builder,
                DisplayRecipe recipe,
                IFocusGroup focuses
        ) {

            /*
             * 三個藥水輸入欄。
             *
             * 同一份輸入藥水分別展示在三個位置，
             * 與原版 BrewingRecipeCategory 相同。
             */
            builder.addInputSlot(1, 37)
                    .addItemStack(
                            recipe.input().copy()
                    );

            builder.addInputSlot(24, 44)
                    .addItemStack(
                            recipe.input().copy()
                    );

            builder.addInputSlot(47, 37)
                    .addItemStack(
                            recipe.input().copy()
                    );

            /*
             * 上方釀造材料欄位。
             */
            builder.addInputSlot(24, 3)
                    .addItemStacks(
                            recipe.ingredients()
                    );

            /*
             * 右側輸出欄。
             */
            builder.addOutputSlot(81, 3)
                    .addItemStack(
                            recipe.output().copy()
                    )
                    .setStandardSlotBackground();
        }

        /**
         * 以 Minecraft 1.21.1 的原版 GUI Sprite
         * 繪製釀造進度、氣泡及燃料裝飾。
         *
         * 不使用 JEI 已棄用的
         * addAnimatedRecipeArrow()。
         */
        @Override
        public void draw(
                DisplayRecipe recipe,
                IRecipeSlotsView recipeSlotsView,
                GuiGraphics graphics,
                double mouseX,
                double mouseY
        ) {
            graphics.blit(
                    BREWING_BACKGROUND,
                    0, 0,
                    55, 14,
                    114, 61,
                    256, 256
            );
            // 釀造進度圖示
            graphics.blitSprite(
                    BREW_PROGRESS,
                    43,
                    3,
                    9,
                    28
            );

            // 氣泡圖示
            graphics.blitSprite(
                    BUBBLES,
                    9,
                    1,
                    12,
                    29
            );

            // 燃料圖示
            graphics.blitSprite(
                    FUEL,
                    5,
                    30,
                    18,
                    4
            );
        }

        @Override
        public ResourceLocation getRegistryName(
                DisplayRecipe recipe
        ) {
            return recipe.id();
        }
    }
}
