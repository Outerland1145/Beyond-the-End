package org.starrepublic.beyond_the_end.recipes.enhancedbrewing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;

public record EnhancedBrewingRecipe(
        ItemStack input,
        Ingredient ingredient,
        ItemStack output,
        boolean allowVanillaStand
) implements Recipe<EnhancedBrewingInput> {

    public EnhancedBrewingRecipe {
        if (input.isEmpty() || output.isEmpty()) {
            throw new IllegalArgumentException(
                    "Brewing input/output cannot be empty"
            );
        }

        if (input.getCount() != 1 || output.getCount() != 1) {
            throw new IllegalArgumentException(
                    "Brewing input/output count must be one"
            );
        }

        input = input.copy();
        output = output.copy();
    }

    public boolean matchesStacks(
            ItemStack bottle,
            ItemStack reagent
    ) {
        return !bottle.isEmpty()
                && !reagent.isEmpty()
                && ItemStack.isSameItemSameComponents(input, bottle)
                && ingredient.test(reagent);
    }

    @Override
    public boolean matches(
            EnhancedBrewingInput input,
            Level level
    ) {
        return matchesStacks(
                input.bottle(),
                input.ingredient()
        );
    }

    @Override
    public ItemStack assemble(
            EnhancedBrewingInput input,
            HolderLookup.Provider registries
    ) {
        return output.copy();
    }

    @Override
    public ItemStack getResultItem(
            HolderLookup.Provider registries
    ) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegister.Enchanted_brewing_serializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegister.Enchanted_brewing_types.get();
    }

    public static final class Serializer
            implements RecipeSerializer<EnhancedBrewingRecipe> {

        public static final MapCodec<EnhancedBrewingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(
                                ItemStack.CODEC
                                        .fieldOf("input")
                                        .forGetter(EnhancedBrewingRecipe::input),

                                Ingredient.CODEC_NONEMPTY
                                        .fieldOf("ingredient")
                                        .forGetter(EnhancedBrewingRecipe::ingredient),

                                ItemStack.CODEC
                                        .fieldOf("output")
                                        .forGetter(EnhancedBrewingRecipe::output),

                                com.mojang.serialization.Codec.BOOL
                                        .optionalFieldOf(
                                                "allow_vanilla_stand",
                                                false
                                        )
                                        .forGetter(
                                                EnhancedBrewingRecipe::allowVanillaStand
                                        )
                        ).apply(instance, EnhancedBrewingRecipe::new)
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                EnhancedBrewingRecipe
                > STREAM_CODEC = StreamCodec.composite(

                ItemStack.STREAM_CODEC,
                EnhancedBrewingRecipe::input,

                Ingredient.CONTENTS_STREAM_CODEC,
                EnhancedBrewingRecipe::ingredient,

                ItemStack.STREAM_CODEC,
                EnhancedBrewingRecipe::output,

                ByteBufCodecs.BOOL,
                EnhancedBrewingRecipe::allowVanillaStand,

                EnhancedBrewingRecipe::new
        );

        @Override
        public MapCodec<EnhancedBrewingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<
                RegistryFriendlyByteBuf,
                EnhancedBrewingRecipe
                > streamCodec() {
            return STREAM_CODEC;
        }
    }
}
