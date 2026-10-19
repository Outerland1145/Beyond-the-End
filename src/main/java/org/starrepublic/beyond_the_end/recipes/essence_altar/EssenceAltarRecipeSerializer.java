package org.starrepublic.beyond_the_end.recipes.essence_altar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class EssenceAltarRecipeSerializer
        implements RecipeSerializer<EssenceAltarRecipe> {

    public static final MapCodec<EssenceAltarRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    instance.group(

                            Codec.STRING
                                    .fieldOf("essence")
                                    .forGetter(EssenceAltarRecipe::getEssence),

                            Ingredient.CODEC
                                    .fieldOf("ingredient")
                                    .forGetter(EssenceAltarRecipe::getInputIngredient),

                            ItemStack.CODEC
                                    .fieldOf("result")
                                    .forGetter(EssenceAltarRecipe::getResult),

                            Codec.intRange(1, 32767)
                                    .fieldOf("cost")
                                    .forGetter(EssenceAltarRecipe::getCost)

                    ).apply(instance, EssenceAltarRecipe::new)
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            EssenceAltarRecipe
            > STREAM_CODEC = StreamCodec.composite(

            ByteBufCodecs.STRING_UTF8,
            EssenceAltarRecipe::getEssence,

            Ingredient.CONTENTS_STREAM_CODEC,
            EssenceAltarRecipe::getInputIngredient,

            ItemStack.STREAM_CODEC,
            EssenceAltarRecipe::getResult,

            ByteBufCodecs.VAR_INT,
            EssenceAltarRecipe::getCost,

            EssenceAltarRecipe::new
    );

    @Override
    public MapCodec<EssenceAltarRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<
            RegistryFriendlyByteBuf,
            EssenceAltarRecipe
            > streamCodec() {
        return STREAM_CODEC;
    }
}


