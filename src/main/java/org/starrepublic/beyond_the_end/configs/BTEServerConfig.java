package org.starrepublic.beyond_the_end.configs;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class BTEServerConfig {
    public final ModConfigSpec.BooleanValue enableEndBarrier;
    public final ModConfigSpec.BooleanValue enableCustomEndTerrain;
    public final ModConfigSpec.BooleanValue enablevanillaendcity;
    public final ModConfigSpec.BooleanValue enableweakendportalactivateinend;

    public final ModConfigSpec.IntValue normalEndRadius;

    public final ModConfigSpec.IntValue barrierRadius;

    public final ModConfigSpec.BooleanValue useoriginalsetting;

    public final ModConfigSpec.ConfigValue<List<? extends String>>fracturedBiomeWhitelist;

    public final ModConfigSpec.IntValue EndSpawnPointX;
    public final ModConfigSpec.IntValue EndSpawnPointY;
    public final ModConfigSpec.IntValue EndSpawnPointZ;

    public final ModConfigSpec.IntValue EndExitPointX;
    public final ModConfigSpec.IntValue EndExitPointZ;

    public final ModConfigSpec.IntValue VoidCrystalGrowthHeight;

    public final ModConfigSpec.IntValue SPIKE_COUNT;
    public final ModConfigSpec.IntValue SPIKE_RING_RADIUS;



    public BTEServerConfig(ModConfigSpec.Builder builder) {

        builder.push("Beyond End Main Island Barrier");

        enableEndBarrier = builder
                .comment(
                        "Whether the End barrier is enabled.",
                        "When enabled, players cannot reach the End MainIsland before unlocking the barrier."
                )
                .define(
                        "enableEndBarrier",
                        true
                );

        barrierRadius = builder
                .comment(
                        "Radius of the End barrier measured from 0,0."
                )
                .defineInRange(
                        "barrierRadius",
                        300,
                        250,
                        850
                );

        builder.pop();


        builder.push("barrier_unlock");

        useoriginalsetting = builder
                .comment(
                        "Whether Used Original Setting to unlock the End barrier."
                )
                .define(
                        "useoriginalsetting",
                        true
                );
        builder.pop();
        builder.push("End Spawn Point");
        EndSpawnPointX = builder
                .comment(
                        "Setting the End Spawn Point Coordinate X"
                )
                .defineInRange(
                        "End Spawn Point Coordinate X",
                        1000,
                        -30000000,
                        30000000
                );
        EndSpawnPointY = builder
                .comment(
                        "Setting the End Spawn Point Coordinate Y"
                )
                .defineInRange(
                        "End Spawn Point Coordinate Y",
                        50,
                        1,
                        30000000
                );
        EndSpawnPointZ = builder
                .comment(
                        "Setting the End Spawn Point Coordinate Z"
                )
                .defineInRange(
                        "End Spawn Point Coordinate Z",
                        0,
                        -30000000,
                        30000000
                );
        builder.pop();
        builder.push("End Exit Point");
        EndExitPointX = builder
                .comment(
                        "Setting the End Exit Point Coordinate X in Overworld"
                )
                .defineInRange(
                        "End Exit Point Coordinate X",
                        1000,
                        -30000000,
                        30000000
                );
        EndExitPointZ = builder
                .comment(
                        "Setting the End Exit Point Coordinate Z in Overworld"
                )
                .defineInRange(
                        "End Exit Point Coordinate Z",
                        0,
                        -30000000,
                        30000000
                );
        builder.pop();
        builder.push("Beyond End Terrain Generation");
        enablevanillaendcity =
                builder
                        .comment(
                                "Enable Vanilla End City generation."
                        )
                        .worldRestart()
                        .define(
                                "enablevanillaendcity",
                                false
                        );
        enableCustomEndTerrain =
                builder
                        .comment(
                                "Enable Beyond The End custom End terrain generation.",
                                "",
                                "true:",
                                "The End is divided into normal, fractured and void regions.",
                                "",
                                "false:",
                                "Beyond The End will not modify vanilla End terrain generation.",
                                "Useful for compatibility with other End overhaul mods and modpacks.",
                                "",
                                "Requires new chunks to see terrain changes."
                        )
                        .worldRestart()
                        .define(
                                "enable_custom_end_terrain",
                                true
                        );

        /*
         * 正常外島區半徑。
         *
         * 最低 10000。
         * 最高 Integer.MAX_VALUE。
         */
        normalEndRadius =
                builder
                        .comment(
                                "Radius of the normal End terrain region, measured from X=0 Z=0.",
                                "",
                                "Minimum: 10000",
                                "Maximum: 2147483647",
                                "Default: 10000",
                                "",
                                "The fractured region always extends for another 10000 blocks.",
                                "",
                                "Example:",
                                "normal_end_radius = 10000",
                                "Normal:     0 - 10000",
                                "Fractured:  10000 - 20000",
                                "Void:       20000+",
                                "",
                                "normal_end_radius = 50000",
                                "Normal:     0 - 50000",
                                "Fractured:  50000 - 60000",
                                "Void:       60000+",
                                "",
                                "Requires new chunks to see terrain changes."
                        )
                        .worldRestart()
                        .defineInRange(
                                "normal_end_radius",
                                10_000,
                                10_000,
                                Integer.MAX_VALUE
                        );
        fracturedBiomeWhitelist =
                builder
                        .comment(
                                "Biomes allowed to generate inside the fractured End region.",
                                "",
                                "Biome IDs must use namespace:path format.",
                                "",
                                "Default:",
                                "- minecraft:end_barrens",
                                "- minecraft:small_end_islands",
                                "",
                                "Modded biomes can also be added, for example:",
                                "\"some_end_mod:crystal_wastes\"",
                                "",
                                "Biomes not listed here will be replaced while inside the fractured region."
                        )
                        .worldRestart()
                        .defineList(
                                "fractured_biome_whitelist",

                                List.of(
                                        "minecraft:end_barrens",
                                        "minecraft:small_end_islands"
                                ),

                                () ->
                                        "minecraft:end_barrens",

                                value -> {

                                    if (!(value instanceof String string)) {
                                        return false;
                                    }

                                    return ResourceLocation
                                            .tryParse(string)
                                            != null;
                                }
                        );
        VoidCrystalGrowthHeight =
                builder
                        .comment(
                                "Maximum distance above the dimension minimum build height " +
                                        "where Void Crystal Bedrock can grow crystals.",
                                "Default: 20"
                        )
                .defineInRange(
                        "VoidCrystalGrowthHeight",
                        20,
                        1,
                        512
                );
        enableweakendportalactivateinend =
                builder
                        .comment(
                                "Allow weak end portals to be activated in the End dimension.",
                                "Default: False"
                        )
                        .define(
                "enable_weak_end_portal_open_in_end",
                false
        );

        builder.pop();
        builder.push("end_dragon_fight");

        SPIKE_COUNT = builder
                .comment(
                        "Number of obsidian spikes around the End portal."
                )
                .defineInRange(
                        "spikeCount",
                        14,
                        10,
                        100
                );

        SPIKE_RING_RADIUS = builder
                .comment(
                        "Horizontal distance from the End portal center",
                        "to the center of each obsidian spike."
                )
                .defineInRange(
                        "spikeRingRadius",
                        45,
                        42,
                        500
                );

        builder.pop();

    }
}
