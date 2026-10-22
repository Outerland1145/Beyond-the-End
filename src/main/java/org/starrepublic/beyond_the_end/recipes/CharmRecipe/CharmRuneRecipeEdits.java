package org.starrepublic.beyond_the_end.recipes.CharmRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CharmRuneRecipeEdits {

    private static final Set<ResourceLocation> REMOVED =
            new HashSet<>();

    private static final Map<
            ResourceLocation,
            RecipeHolder<CharmRuneRecipe>
            > REPLACEMENTS = new HashMap<>();

    /**
     * 每次開始資料包重新載入時重置修改內容。
     *
     * 之後 KubeJS 的配方事件可重新提交修改。
     */
    public static void beginReload() {
        REMOVED.clear();
        REPLACEMENTS.clear();
    }

    /**
     * 刪除指定配方。
     */
    public static void remove(String recipeId) {

        ResourceLocation id =
                ResourceLocation.parse(recipeId);

        REMOVED.add(id);
        REPLACEMENTS.remove(id);
    }

    /**
     * 修改指定配方。
     *
     * recipeId：要修改的配方
     * charmId：輸出護符種類
     * variant：輸出護符變種
     * runes：符文組合字串
     */
    public static void replace(
            String recipeId,
            String charmId,
            int variant,
            String runes
    ) {

        ResourceLocation id =
                ResourceLocation.parse(recipeId);

        CharmRuneRecipe recipe =
                new CharmRuneRecipe(
                        ResourceLocation.parse(charmId),
                        variant,
                        CharmBuiltinRecipes.parseRunes(runes)
                );

        REPLACEMENTS.put(
                id,
                new RecipeHolder<>(id, recipe)
        );

        REMOVED.remove(id);
    }

    /**
     * 新增自訂配方，格式與 replace 相同。
     */
    public static void add(
            String recipeId,
            String charmId,
            int variant,
            String runes
    ) {
        replace(recipeId, charmId, variant, runes);
    }

    public static boolean isRemoved(ResourceLocation id) {
        return REMOVED.contains(id);
    }

    public static Map<
            ResourceLocation,
            RecipeHolder<CharmRuneRecipe>
            > replacements() {
        return Map.copyOf(REPLACEMENTS);
    }

    private CharmRuneRecipeEdits() {
    }
}
