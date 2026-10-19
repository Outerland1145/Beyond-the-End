package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipe;
import org.starrepublic.beyond_the_end.recipes.essence_altar.EssenceAltarRecipeSerializer;

import java.util.function.Supplier;


public class RecipeRegister {
    public static final DeferredRegister<RecipeType<?>>
            RECIPE_TYPES = DeferredRegister.create(
            Registries.RECIPE_TYPE,
            Beyond_the_end.MODID
    );

    public static final DeferredRegister<RecipeSerializer<?>>
            RECIPE_SERIALIZERS = DeferredRegister.create(
            Registries.RECIPE_SERIALIZER,
            Beyond_the_end.MODID
    );

    // ========================================================
    // Essence Altar
    // ========================================================

    public static final Supplier<RecipeType<EssenceAltarRecipe>>
            ESSENCE_ALTAR_TYPE = RECIPE_TYPES.register(
            "essence_altar",
            () -> RecipeType.<EssenceAltarRecipe>simple(
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "essence_altar"
                    )
            )
    );

    public static final Supplier<RecipeSerializer<EssenceAltarRecipe>>
            ESSENCE_ALTAR_SERIALIZER = RECIPE_SERIALIZERS.register(
            "essence_altar",
            EssenceAltarRecipeSerializer::new
    );

    // ========================================================
    // Register
    // ========================================================

    public static void register(IEventBus eventBus) {

        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
