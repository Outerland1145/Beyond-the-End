package org.starrepublic.beyond_the_end.World.worldgen.biomedefinition;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.*;

import static com.mojang.text2speech.Narrator.LOGGER;

public final class EndBiomeDataCache {

    private static volatile List<Entry> ENTRIES =
            List.of();

    private static volatile SurfaceRules.RuleSource
            DATAPACK_SURFACE_RULE;


    private EndBiomeDataCache() {}

    private static volatile SurfaceRules.RuleSource
            SURFACE_RULE = null;


    public record Entry(
            ResourceKey<Biome> biomeKey,
            Holder<Biome> biome,
            double chance,
            SurfaceRules.RuleSource surfaceRule
    ) {}
    public static void rebuild(
            RegistryAccess registryAccess,
            Collection<EndBiomeDefinition> definitions
    ) {

        Registry<Biome> biomeRegistry =
                registryAccess.registryOrThrow(
                        Registries.BIOME
                );

        List<Entry> entries =
                new ArrayList<>();

        List<SurfaceRules.RuleSource> rules =
                new ArrayList<>();


        for (EndBiomeDefinition definition : definitions) {

            Optional<Holder.Reference<Biome>> optional =
                    biomeRegistry.getHolder(
                            definition.biome()
                    );

            if (optional.isEmpty()) {
                continue;
            }


            Holder.Reference<Biome> biome =
                    optional.get();


            entries.add(
                    new Entry(
                            definition.biome(),
                            biome,
                            definition.generationChance(),
                            definition.surfaceRule()
                    )
            );


            /*
             * 最重要的部分。
             *
             * JSON 只提供「這個 biome 的 surface rule」。
             *
             * 模組自動包成：
             *
             * if biome == xxx
             *     apply surface rule
             */
            rules.add(
                    SurfaceRules.ifTrue(
                            SurfaceRules.isBiome(
                                    definition.biome()
                            ),
                            definition.surfaceRule()
                    )
            );
        }


        ENTRIES =
                List.copyOf(entries);


        SURFACE_RULE =
                rules.isEmpty()
                        ? null
                        : SurfaceRules.sequence(
                        rules.toArray(
                                SurfaceRules.RuleSource[]::new
                        )
                );


        LOGGER.info(
                "[Beyond The End] Built {} biome entries and {} surface rules",
                ENTRIES.size(),
                rules.size()
        );
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }


    public static SurfaceRules.RuleSource surfaceRule() {

        return SURFACE_RULE;
    }
}
