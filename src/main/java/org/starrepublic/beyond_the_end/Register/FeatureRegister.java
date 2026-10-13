package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.World.worldgen.feature.EndBarrensSandFieldFeature;
import org.starrepublic.beyond_the_end.World.worldgen.feature.EndSurfaceVegetationFeature;
import org.starrepublic.beyond_the_end.World.worldgen.feature.VoidIslandFeature;
import org.starrepublic.beyond_the_end.World.worldgen.feature.VoidRuinFeature;

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


    public static void register(
            IEventBus modBus
    ) {

        FEATURES.register(modBus);
    }
}
