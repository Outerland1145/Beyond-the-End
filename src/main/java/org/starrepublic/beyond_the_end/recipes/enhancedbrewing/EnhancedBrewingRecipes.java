package org.starrepublic.beyond_the_end.recipes.enhancedbrewing;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class EnhancedBrewingRecipes {

    public static List<RecipeHolder<EnhancedBrewingRecipe>> all(
            RecipeManager manager
    ) {
        return manager.getAllRecipesFor(
                       RecipeRegister.Enchanted_brewing_types.get()
                )
                .stream()
                .sorted(Comparator.comparing(
                        holder -> holder.id().toString()
                ))
                .toList();
    }

    public static List<RecipeHolder<EnhancedBrewingRecipe>> all(
            Level level
    ) {
        return all(level.getRecipeManager());
    }

    private static Optional<RecipeHolder<EnhancedBrewingRecipe>> find(
            RecipeManager manager,
            ItemStack bottle,
            ItemStack ingredient,
            boolean shared
    ) {
        if (bottle.isEmpty() || ingredient.isEmpty()) {
            return Optional.empty();
        }

        return all(manager).stream()
                .filter(holder ->
                        holder.value().allowVanillaStand() == shared
                )
                .filter(holder ->
                        holder.value().matchesStacks(
                                bottle,
                                ingredient
                        )
                )
                .findFirst();
    }

    public static Optional<RecipeHolder<EnhancedBrewingRecipe>>
    findExclusive(
            Level level,
            ItemStack bottle,
            ItemStack ingredient
    ) {
        return find(
                level.getRecipeManager(),
                bottle,
                ingredient,
                false
        );
    }

    public static Optional<RecipeHolder<EnhancedBrewingRecipe>>
    findShared(
            RecipeManager manager,
            ItemStack bottle,
            ItemStack ingredient
    ) {
        return find(
                manager,
                bottle,
                ingredient,
                true
        );
    }

    public static boolean isIngredient(
            Level level,
            ItemStack ingredient
    ) {
        return !ingredient.isEmpty()
                && all(level).stream()
                .anyMatch(holder ->
                        holder.value()
                                .ingredient()
                                .test(ingredient)
                );
    }

    public static boolean isInput(
            Level level,
            ItemStack bottle
    ) {
        return !bottle.isEmpty()
                && all(level).stream()
                .anyMatch(holder ->
                        ItemStack.isSameItemSameComponents(
                                holder.value().input(),
                                bottle
                        )
                );
    }

    /**
     * 根據目前執行執行緒選擇配方表。
     *
     * 整合伺服器有客戶端與伺服器兩條主要執行緒，
     * 不能只因目前存在 MinecraftServer，
     * 就讓客戶端 GUI 一律讀取伺服器物件。
     */
    @Nullable
    private static RecipeManager resolveRecipeManager() {

        MinecraftServer server =
                ServerLifecycleHooks.getCurrentServer();

        // 真正的伺服器執行緒。
        if (server != null && server.isSameThread()) {
            return server.getRecipeManager();
        }

        // 實體客戶端：使用已同步的客戶端配方表。
        if (FMLEnvironment.dist == Dist.CLIENT) {

            RecipeManager clientManager =
                    EnhancedBrewingClientRecipeAccess.getManager();

            if (clientManager != null) {
                return clientManager;
            }
        }

        // 專用伺服器與其他沒有客戶端世界的情況。
        return server != null
                ? server.getRecipeManager()
                : null;
    }

    /**
     * 只註冊一個動態橋接器。
     *
     * 不預先複製配方，
     * /reload 後仍讀取目前的 RecipeManager。
     */
    public static void registerVanillaBrewing(
            RegisterBrewingRecipesEvent event
    ) {
        event.getBuilder().addRecipe(
                new SharedRecipeBridge()
        );
    }

    private static final class SharedRecipeBridge
            implements IBrewingRecipe {

        @Override
        public boolean isInput(ItemStack input) {

            if (input.isEmpty()) {
                return false;
            }

            RecipeManager manager = resolveRecipeManager();

            if (manager == null) {
                return false;
            }

            return all(manager).stream()
                    .anyMatch(holder ->
                            holder.value().allowVanillaStand()
                                    && ItemStack.isSameItemSameComponents(
                                    holder.value().input(),
                                    input
                            )
                    );
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {

            if (ingredient.isEmpty()) {
                return false;
            }

            RecipeManager manager = resolveRecipeManager();

            if (manager == null) {
                return false;
            }

            return all(manager).stream()
                    .anyMatch(holder ->
                            holder.value().allowVanillaStand()
                                    && holder.value()
                                    .ingredient()
                                    .test(ingredient)
                    );
        }

        @Override
        public ItemStack getOutput(
                ItemStack input,
                ItemStack ingredient
        ) {
            RecipeManager manager = resolveRecipeManager();

            if (manager == null) {
                return ItemStack.EMPTY;
            }

            return findShared(
                    manager,
                    input,
                    ingredient
            ).map(holder ->
                    holder.value().output().copy()
            ).orElse(ItemStack.EMPTY);
        }
    }

    private EnhancedBrewingRecipes() {
    }
}
