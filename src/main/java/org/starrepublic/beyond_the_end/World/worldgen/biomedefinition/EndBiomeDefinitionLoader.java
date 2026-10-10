package org.starrepublic.beyond_the_end.World.worldgen.biomedefinition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;


public final class EndBiomeDefinitionLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    /*
     * 實際已經從 JSON 讀進來的定義。
     */
    private static volatile Map<
            ResourceLocation,
            EndBiomeDefinition
            > DEFINITIONS = Map.of();

    private final RegistryAccess registryAccess;


    public EndBiomeDefinitionLoader(
            RegistryAccess registryAccess
    ) {

        /*
         * 實際掃描：
         *
         * data/<namespace>/
         *     beyond_the_end/
         *         end_biome_definition/
         *             *.json
         */
        super(
                GSON,
                "end_biome_definition"
        );

        this.registryAccess =
                registryAccess;
    }


    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonMap,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {

        LOGGER.info(
                "[Beyond The End] Found {} raw end biome JSON files",
                jsonMap.size()
        );


        /*
         * 非常重要：
         *
         * 不直接使用 JsonOps.INSTANCE。
         *
         * SurfaceRules 等 Codec 可能需要 Registry context。
         */
        DynamicOps<JsonElement> ops =
                registryAccess.createSerializationContext(
                        JsonOps.INSTANCE
                );


        Map<ResourceLocation, EndBiomeDefinition> loaded =
                new LinkedHashMap<>();


        jsonMap.forEach(
                (id, json) -> {

                    LOGGER.info(
                            "[Beyond The End] Parsing End biome definition: {}",
                            id
                    );


                    EndBiomeDefinition.CODEC
                            .parse(
                                    ops,
                                    json
                            )
                            .resultOrPartial(
                                    error ->
                                            LOGGER.error(
                                                    "[Beyond The End] Failed to parse {}: {}",
                                                    id,
                                                    error
                                            )
                            )
                            .ifPresent(
                                    definition -> {

                                        loaded.put(
                                                id,
                                                definition
                                        );

                                        LOGGER.info(
                                                "[Beyond The End] Loaded definition {} -> biome={}, chance={}",
                                                id,
                                                definition.biome().location(),
                                                definition.generationChance()
                                        );
                                    }
                            );
                }
        );


        DEFINITIONS =
                Map.copyOf(
                        loaded
                );


        LOGGER.info(
                "[Beyond The End] Successfully loaded {} End biome definitions",
                DEFINITIONS.size()
        );


        /*
         * JSON 全部解析完之後，
         * 再重建世界生成使用的 cache。
         */
        EndBiomeDataCache.rebuild(
                registryAccess,
                DEFINITIONS.values()
        );
    }


    public static Map<
            ResourceLocation,
            EndBiomeDefinition
            > definitions() {

        return DEFINITIONS;
    }
}
