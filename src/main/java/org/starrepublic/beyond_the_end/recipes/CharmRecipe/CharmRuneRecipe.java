package org.starrepublic.beyond_the_end.recipes.CharmRecipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.starrepublic.beyond_the_end.Items.charm.CharmHelper;
import org.starrepublic.beyond_the_end.Items.charm.CharmTypes;
import org.starrepublic.beyond_the_end.Items.rune.RuneHelper;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record CharmRuneRecipe(
        ResourceLocation charmType,
        int variant,
        List<ResourceLocation> runes
) implements Recipe<CharmRuneInput> {

    public CharmRuneRecipe {
        runes = List.copyOf(runes);

        if (runes.size() < 3 || runes.size() > 6) {
            throw new IllegalArgumentException(
                    "Rune count must be between 3 and 6"
            );
        }

        if (variant < 1 || variant > 64) {
            throw new IllegalArgumentException(
                    "Invalid charm variant: " + variant
            );
        }
    }

    @Override
    public boolean matches(
            CharmRuneInput input,
            Level level
    ) {

        Map<ResourceLocation, Integer> remaining =
                new HashMap<>();

        for (ResourceLocation rune : runes) {
            remaining.merge(rune, 1, Integer::sum);
        }

        int used = 0;

        for (int i = 0; i < input.size(); i++) {

            ItemStack stack = input.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            // 每個符文槽只允許放一個符文。
            if (stack.getCount() != 1) {
                return false;
            }

            ResourceLocation type =
                    RuneHelper.getType(stack).orElse(null);

            if (type == null) {
                return false;
            }

            int count = remaining.getOrDefault(type, 0);

            if (count <= 0) {
                return false;
            }

            if (count == 1) {
                remaining.remove(type);
            } else {
                remaining.put(type, count - 1);
            }

            used++;
        }

        return used == runes.size()
                && remaining.isEmpty();
    }

    @Override
    public ItemStack assemble(
            CharmRuneInput input,
            HolderLookup.Provider registries
    ) {
        return getResultItem(registries);
    }

    @Override
    public ItemStack getResultItem(
            HolderLookup.Provider registries
    ) {

        // 只允許產生目前存在的護符類型和變種。
        if (CharmTypes.getVariant(
                charmType,
                variant,
                registries
        ).isEmpty()) {
            return ItemStack.EMPTY;
        }

        return CharmHelper.create(
                charmType,
                variant
        );
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return width * height >= runes.size();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegister.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegister.TYPE.get();
    }

    public static final class Serializer
            implements RecipeSerializer<CharmRuneRecipe> {

        private static final MapCodec<CharmRuneRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance ->
                        instance.group(

                                ResourceLocation.CODEC
                                        .fieldOf("charm")
                                        .forGetter(
                                                CharmRuneRecipe::charmType
                                        ),

                                com.mojang.serialization.Codec.INT
                                        .fieldOf("variant")
                                        .forGetter(
                                                CharmRuneRecipe::variant
                                        ),

                                ResourceLocation.CODEC.listOf()
                                        .fieldOf("runes")
                                        .forGetter(
                                                CharmRuneRecipe::runes
                                        )

                        ).apply(
                                instance,
                                CharmRuneRecipe::new
                        )
                );

        private static final StreamCodec<
                RegistryFriendlyByteBuf,
                CharmRuneRecipe
                > STREAM_CODEC = new StreamCodec<>() {

            @Override
            public CharmRuneRecipe decode(
                    RegistryFriendlyByteBuf buffer
            ) {

                ResourceLocation charm =
                        buffer.readResourceLocation();

                int variant = buffer.readVarInt();
                int size = buffer.readVarInt();

                if (size < 3 || size > 6) {
                    throw new IllegalArgumentException(
                            "Invalid rune recipe length: " + size
                    );
                }

                java.util.ArrayList<ResourceLocation> runes =
                        new java.util.ArrayList<>();

                for (int i = 0; i < size; i++) {
                    runes.add(
                            buffer.readResourceLocation()
                    );
                }

                return new CharmRuneRecipe(
                        charm,
                        variant,
                        runes
                );
            }

            @Override
            public void encode(
                    RegistryFriendlyByteBuf buffer,
                    CharmRuneRecipe recipe
            ) {

                buffer.writeResourceLocation(
                        recipe.charmType()
                );

                buffer.writeVarInt(
                        recipe.variant()
                );

                buffer.writeVarInt(
                        recipe.runes().size()
                );

                for (ResourceLocation rune : recipe.runes()) {
                    buffer.writeResourceLocation(rune);
                }
            }
        };

        @Override
        public MapCodec<CharmRuneRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<
                RegistryFriendlyByteBuf,
                CharmRuneRecipe
                > streamCodec() {
            return STREAM_CODEC;
        }
    }
}
