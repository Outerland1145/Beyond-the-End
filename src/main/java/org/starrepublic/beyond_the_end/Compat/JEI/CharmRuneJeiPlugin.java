package org.starrepublic.beyond_the_end.Compat.JEI;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;

import mezz.jei.api.recipe.RecipeType;

import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.starrepublic.beyond_the_end.Beyond_the_end;

import org.starrepublic.beyond_the_end.Items.charm.CharmData;
import org.starrepublic.beyond_the_end.Items.charm.CharmHelper;
import org.starrepublic.beyond_the_end.Items.charm.CharmPouchMenu;
import org.starrepublic.beyond_the_end.Items.charm.CharmPouchScreen;
import org.starrepublic.beyond_the_end.Items.rune.RuneHelper;
import org.starrepublic.beyond_the_end.Register.ItemRegister;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;
import org.starrepublic.beyond_the_end.recipes.CharmRecipe.CharmRuneRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@JeiPlugin
public final class CharmRuneJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "charm_rune_jei"
            );

    /**
     * JEI 專用 RecipeType。
     *
     * 與 Minecraft 註冊的 CharmRecipeRegister.TYPE
     * 是兩個不同的介面，但代表同一種配方。
     */
    public static final RecipeType<DisplayRecipe> CHARM_RUNE =
            RecipeType.create(
                    Beyond_the_end.MODID,
                    "charm_rune",
                    DisplayRecipe.class
            );

    /**
     * JEI 使用的配方顯示資料。
     *
     * id：
     *   原本 Minecraft 配方的 ID。
     *
     * inputs：
     *   含有 RuneData Component 的符文。
     *
     * output：
     *   含有 CharmData Component 的護符。
     */
    public record DisplayRecipe(
            ResourceLocation id,
            List<ItemStack> inputs,
            ItemStack output
    ) {

        public DisplayRecipe {
            inputs = inputs.stream()
                    .map(ItemStack::copy)
                    .toList();

            output = output.copy();
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    /**
     * JEI 物品變種識別。
     *
     * 護符使用 type + level。
     * 符文使用 RuneData 中的 type。
     *
     * 防止 JEI 將所有護符、符文視為同一個物品。
     */
    @Override
    public void registerItemSubtypes(
            ISubtypeRegistration registration
    ) {

        registration.registerSubtypeInterpreter(
                ItemRegister.CHARMITEM.get(),
                new CharmSubtypeInterpreter()
        );

        Item runeItem = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(
                        Beyond_the_end.MODID,
                        "rune"
                )
        );

        registration.registerSubtypeInterpreter(
                runeItem,
                new RuneSubtypeInterpreter()
        );
    }

    /**
     * 註冊護符合成分類。
     */
    @Override
    public void registerCategories(
            IRecipeCategoryRegistration registration
    ) {

        registration.addRecipeCategories(
                new CharmRuneRecipeCategory(
                        registration.getJeiHelpers()
                                .getGuiHelper()
                )
        );
    }

    /**
     * 從客戶端真正的 RecipeManager 讀取配方。
     *
     * 不呼叫 CharmBuiltinRecipes.createAll()。
     *
     * 因此 JEI 讀取的是已載入的配方，
     * 而不是重新生成一份原始 HEE 配方。
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

        List<DisplayRecipe> recipes =
                new ArrayList<>();

        List<RecipeHolder<CharmRuneRecipe>> holders =
                new ArrayList<>(
                        level.getRecipeManager()
                                .getAllRecipesFor(
                                        RecipeRegister.TYPE.get()
                                )
                );

        holders.sort(
                Comparator.comparing(
                        holder -> holder.id().toString()
                )
        );

        for (RecipeHolder<CharmRuneRecipe> holder : holders) {

            CharmRuneRecipe recipe = holder.value();

            /*
             * 現在的護符袋只有五個符文輸入槽。
             *
             * 六符文配方雖然可以在配方資料中存在，
             * 但目前無法使用護符袋製作，
             * 因此不顯示在 JEI 的可製作配方中。
             */
            if (recipe.runes().size() > CharmPouchMenu.RUNE_SLOTS) {
                continue;
            }

            List<ItemStack> inputs =
                    new ArrayList<>();

            boolean valid = true;

            for (ResourceLocation runeType : recipe.runes()) {

                /*
                 * 使用原有 RuneHelper 建立帶有正確
                 * 資料組件的符文，而非普通 Rune ItemStack。
                 */
                ItemStack rune = RuneHelper.create(runeType);

                if (rune.isEmpty()
                        || RuneHelper.getType(rune)
                        .filter(runeType::equals)
                        .isEmpty()) {

                    valid = false;
                    break;
                }

                inputs.add(rune);
            }

            if (!valid || inputs.isEmpty()) {
                continue;
            }

            /*
             * 護符結果直接由原配方組裝。
             *
             * 允許資料包新增護符類型和變種，
             * 也允許其他模組修改配方結果。
             */
            ItemStack result = recipe.getResultItem(
                    level.registryAccess()
            );

            if (result.isEmpty()) {
                continue;
            }

            recipes.add(
                    new DisplayRecipe(
                            holder.id(),
                            inputs,
                            result
                    )
            );
        }

        registration.addRecipes(
                CHARM_RUNE,
                recipes
        );
    }

    /**
     * 將護符袋設定為 JEI 配方製作入口。
     *
     * 在 JEI 中查看護符袋的用途時，
     * 可以開啟護符合成分類。
     */
    @Override
    public void registerRecipeCatalysts(
            IRecipeCatalystRegistration registration
    ) {

        registration.addRecipeCatalysts(
                CHARM_RUNE,
                new ItemStack(
                        ItemRegister.CHARMPOUCH.get()
                )
        );
    }

    /**
     * 點擊護符袋 GUI 中符文與結果槽之間的箭頭，
     * 可直接打開 JEI 的護符合成分類。
     *
     * 座標依照先前的 CharmPouchScreen：
     * 箭頭顯示於 x=121、y=62 附近。
     */
    @Override
    public void registerGuiHandlers(
            IGuiHandlerRegistration registration
    ) {

        registration.addRecipeClickArea(
                CharmPouchScreen.class,
                116,
                56,
                19,
                18,
                CHARM_RUNE
        );
    }

    /**
     * 護符 Data Component 的 JEI 唯一識別。
     *
     * 例：
     * beyond_the_end:criticalstrike#4
     */
    private static final class CharmSubtypeInterpreter
            implements ISubtypeInterpreter<ItemStack> {

        private static String identify(ItemStack stack) {

            return CharmHelper.getData(stack)
                    .map(data ->
                            data.type() + "#" + data.level()
                    )
                    .orElse("undefined");
        }

        @Override
        public Object getSubtypeData(
                ItemStack ingredient,
                UidContext context
        ) {
            return identify(ingredient);
        }

        @Override
        @SuppressWarnings("deprecation")
        public String getLegacyStringSubtypeInfo(
                ItemStack ingredient,
                UidContext context
        ) {
            return identify(ingredient);
        }
    }

    /**
     * 符文 Data Component 的 JEI 唯一識別。
     *
     * 例：
     * beyond_the_end:power
     */
    private static final class RuneSubtypeInterpreter
            implements ISubtypeInterpreter<ItemStack> {

        private static String identify(ItemStack stack) {

            return RuneHelper.getType(stack)
                    .map(ResourceLocation::toString)
                    .orElse("undefined");
        }

        @Override
        public Object getSubtypeData(
                ItemStack ingredient,
                UidContext context
        ) {
            return identify(ingredient);
        }

        @Override
        @SuppressWarnings("deprecation")
        public String getLegacyStringSubtypeInfo(
                ItemStack ingredient,
                UidContext context
        ) {
            return identify(ingredient);
        }
    }
}
