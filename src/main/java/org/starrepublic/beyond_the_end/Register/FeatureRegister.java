package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.World.worldgen.feature.*;

import java.util.function.Supplier;

public class FeatureRegister {
    private FeatureRegister() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(
                    BuiltInRegistries.FEATURE,
                    Beyond_the_end.MODID
            );

    public static final DeferredHolder<
                Feature<?>,
                VoidIslandFeature
                > VOID_ISLAND =
            FEATURES.register(
                    "void_island",
                    () -> new VoidIslandFeature(
                            NoneFeatureConfiguration.CODEC
                    )
            );

    public static final DeferredHolder<
            Feature<?>,
            VoidRuinFeature
            > VOID_RUIN =
            FEATURES.register(
                    "void_ruin",
                    () -> new VoidRuinFeature(
                            NoneFeatureConfiguration.CODEC
                    )
            );
    public static final DeferredHolder<
            Feature<?>,
            EndSurfaceVegetationFeature
            >
            END_SURFACE_VEGETATION =

            FEATURES.register(
                    "end_surface_vegetation",

                    () -> new EndSurfaceVegetationFeature(
                            net.minecraft.world.level.levelgen.feature
                                    .configurations
                                    .NoneFeatureConfiguration.CODEC
                    )
            );


    public static final DeferredHolder<
            Feature<?>,
            EndBarrensSandFieldFeature
            >
            END_BARRENS_SAND_FIELD =

            FEATURES.register(
                    "end_barrens_sand_field",

                    () -> new EndBarrensSandFieldFeature(
                            net.minecraft.world.level.levelgen.feature
                                    .configurations
                                    .NoneFeatureConfiguration.CODEC
                    )
            );
    public static final DeferredHolder<
            Feature<?>,
            EnderGooLakeFeature
            >
            TERRACED_ENDER_GOO_LAKE =
            FEATURES.register(
                    "ender_goo_lake",

                    () ->
                            new EnderGooLakeFeature(
                                    NoneFeatureConfiguration.CODEC
                            )
            );


    public static final DeferredHolder<
            Feature<?>,
            EnchantedGrassMeadowFeature
            >
            ENCHANTED_GRASS_MEADOW =
            FEATURES.register(
                    "enchanted_grass_meadow",

                    () ->
                            new EnchantedGrassMeadowFeature(
                                    NoneFeatureConfiguration.CODEC
                            )
            );


    public static final DeferredHolder<
            Feature<?>,
            ObsidianSpikeClusterFeature
            >
            OBSIDIAN_SPIKE_CLUSTER =
            FEATURES.register(
                    "obsidian_spike_cluster",

                    () ->
                            new ObsidianSpikeClusterFeature(
                                    NoneFeatureConfiguration.CODEC
                            )
            );
    public static final DeferredHolder<
            Feature<?>,
            VoidCrystalSpikeFeature
            >
            VOID_CRYSTAL_SPIKE =
            FEATURES.register(
                    "void_crystal_spike",

                    () -> new VoidCrystalSpikeFeature(
                            NoneFeatureConfiguration.CODEC
                    )
            );
    public static final Supplier<Feature<SpookyTreeConfiguration>>
            GHOST_TREE =
            FEATURES.register(
                    "spooky_tree",
                    () -> new SpookyTreeFeature(
                            SpookyTreeConfiguration.CODEC
                    )
            );
    public static final Supplier<Feature<SpookyTreeConfiguration>>
            HEE_SPOOKY_TREE =
            FEATURES.register(
                    "small_spooky_tree",
                    () -> new SmallSpookyTreeFeature(
                            SpookyTreeConfiguration.CODEC
                    )
            );


    public static void register(
            IEventBus modBus
    ) {

        FEATURES.register(modBus);
    }
}
