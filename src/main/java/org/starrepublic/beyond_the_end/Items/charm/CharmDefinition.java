package org.starrepublic.beyond_the_end.Items.charm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record CharmDefinition(
        int backgroundColor,
        ResourceLocation icon,
        int maxLevel,
        boolean stackable,
        List<EffectSpec> effects,
        List<Variant> variants
) {

    private static final ResourceLocation DEFAULT_ICON =
            ResourceLocation.fromNamespaceAndPath(
                    "beyond_the_end",
                    "item/charm/icons/fore_0"
            );

    public static final Codec<Integer> RGB_CODEC =
            Codec.STRING.comapFlatMap(
                    value -> {
                        if (!value.matches("#[0-9a-fA-F]{6}")) {
                            return DataResult.error(
                                    () -> "Invalid RGB: " + value
                            );
                        }

                        return DataResult.success(
                                Integer.parseInt(
                                        value.substring(1),
                                        16
                                )
                        );
                    },
                    value -> String.format(
                            Locale.ROOT,
                            "#%06X",
                            value & 0xFFFFFF
                    )
            );

    public static final Codec<CharmDefinition> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(

                            RGB_CODEC
                                    .fieldOf("background_color")
                                    .forGetter(
                                            CharmDefinition::backgroundColor
                                    ),

                            ResourceLocation.CODEC
                                    .optionalFieldOf(
                                            "icon",
                                            DEFAULT_ICON
                                    )
                                    .forGetter(
                                            CharmDefinition::icon
                                    ),

                            Codec.intRange(1, 64)
                                    .optionalFieldOf("max_level", 1)
                                    .forGetter(
                                            CharmDefinition::maxLevel
                                    ),

                            Codec.BOOL
                                    .optionalFieldOf("stackable", true)
                                    .forGetter(
                                            CharmDefinition::stackable
                                    ),

                            EffectSpec.CODEC.listOf()
                                    .optionalFieldOf(
                                            "effects",
                                            List.of()
                                    )
                                    .forGetter(
                                            CharmDefinition::effects
                                    ),

                            Variant.CODEC.listOf()
                                    .optionalFieldOf(
                                            "variants",
                                            List.of()
                                    )
                                    .forGetter(
                                            CharmDefinition::variants
                                    )

                    ).apply(instance, CharmDefinition::new)
            );

    public CharmDefinition {
        effects = List.copyOf(effects);
        variants = List.copyOf(variants);

        if (!variants.isEmpty()
                && variants.size() != maxLevel) {

            throw new IllegalArgumentException(
                    "Charm max_level must match variants size"
            );
        }
    }

    /**
     * 取得指定的護符變種。
     *
     * level 為 1-based。
     */
    public Optional<Variant> getVariant(int level) {

        if (level < 1 || level > maxLevel) {
            return Optional.empty();
        }

        if (variants.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                variants.get(level - 1)
        );
    }

    /**
     * 原本的效果種類定義。
     *
     * 保留，供後續效果處理器使用。
     */
    public record EffectSpec(
            ResourceLocation type,
            Map<String, Double> parameters
    ) {

        public static final Codec<EffectSpec> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(

                                ResourceLocation.CODEC
                                        .fieldOf("type")
                                        .forGetter(EffectSpec::type),

                                Codec.unboundedMap(
                                                Codec.STRING,
                                                Codec.DOUBLE
                                        )
                                        .optionalFieldOf(
                                                "parameters",
                                                Map.of()
                                        )
                                        .forGetter(
                                                EffectSpec::parameters
                                        )

                        ).apply(instance, EffectSpec::new)
                );

        public EffectSpec {
            parameters = Map.copyOf(parameters);
        }
    }

    /**
     * 護符變種。
     *
     * heeId：
     *   原版 HEE 的配方 / Item Metadata ID。
     *
     *   -1 代表資料包新增的非 HEE 變種。
     *
     * properties：
     *   該變種專屬的效果參數。
     */
    public record Variant(
            int heeId,
            Map<String, Double> properties
    ) {

        public static final Codec<Variant> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(

                                Codec.INT
                                        .optionalFieldOf(
                                                "hee_id",
                                                -1
                                        )
                                        .forGetter(
                                                Variant::heeId
                                        ),

                                Codec.unboundedMap(
                                                Codec.STRING,
                                                Codec.DOUBLE
                                        )
                                        .optionalFieldOf(
                                                "properties",
                                                Map.of()
                                        )
                                        .forGetter(
                                                Variant::properties
                                        )

                        ).apply(instance, Variant::new)
                );

        public Variant {
            properties = Map.copyOf(properties);
        }

        public double getProperty(
                String name,
                double defaultValue
        ) {
            return properties.getOrDefault(
                    name,
                    defaultValue
            );
        }
    }
}
