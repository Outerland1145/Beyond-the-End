package org.starrepublic.beyond_the_end.recipes.CharmRecipe;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;

import net.minecraft.util.profiling.ProfilerFiller;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mojang.realmsclient.client.RealmsError.LOGGER;

public final class CharmRecipeReloadEvents {

    @SubscribeEvent
    public static void onAddReloadListeners(
            AddReloadListenerEvent event
    ) {

        CharmRuneRecipeEdits.beginReload();

        RecipeManager manager =
                event.getServerResources().getRecipeManager();

        event.addListener(
                new SimplePreparableReloadListener<Void>() {

                    @Override
                    protected Void prepare(
                            ResourceManager resources,
                            ProfilerFiller profiler
                    ) {
                        return null;
                    }

                    @Override
                    protected void apply(
                            Void ignored,
                            ResourceManager resources,
                            ProfilerFiller profiler
                    ) {

                        installRecipes(manager);
                    }
                }
        );
    }

    private static void installRecipes(
            RecipeManager manager
    ) {

        Map<ResourceLocation, RecipeHolder<?>>
                recipes = new HashMap<>();

        // 先保留全部正常載入的原版、模組和資料包配方。
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            recipes.put(holder.id(), holder);
        }

        // Java 內建配方。
        for (RecipeHolder<CharmRuneRecipe> holder :
                CharmBuiltinRecipes.createAll()) {

            if (CharmRuneRecipeEdits.isRemoved(holder.id())) {
                continue;
            }

            // 已有同 ID 的資料包配方優先。
            recipes.putIfAbsent(
                    holder.id(),
                    holder
            );
        }

        // 套用魔改模組新增或替換的配方。
        for (var entry :
                CharmRuneRecipeEdits.replacements().entrySet()) {

            if (CharmRuneRecipeEdits.isRemoved(entry.getKey())) {
                continue;
            }

            recipes.put(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        // 最後再確保明確移除的配方不存在。
        recipes.keySet().removeIf(
                CharmRuneRecipeEdits::isRemoved
        );

        manager.replaceRecipes(
                new ArrayList<>(recipes.values())
        );

        LOGGER.info(
                "[Beyond the End] Charm rune recipes installed: {}",
                manager.getAllRecipesFor(
                        RecipeRegister.TYPE.get()
                ).size()
        );
    }

    private CharmRecipeReloadEvents() {
    }
}
