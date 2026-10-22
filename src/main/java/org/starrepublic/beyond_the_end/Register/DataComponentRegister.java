package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Items.charm.CharmData;
import org.starrepublic.beyond_the_end.Items.rune.RuneData;

public final class DataComponentRegister {

    public static final DeferredRegister.DataComponents
            DATA_COMPONENTS =
            DeferredRegister.createDataComponents(
                    Registries.DATA_COMPONENT_TYPE,
                    Beyond_the_end.MODID
            );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<RuneData>
            > RUNE_TYPE =
            DATA_COMPONENTS.registerComponentType(
                    "rune_type",
                    builder -> builder
                            .persistent(RuneData.CODEC)
                            .networkSynchronized(RuneData.STREAM_CODEC)
            );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<CharmData>
            > CHARM_DATA =
            DATA_COMPONENTS.registerComponentType(
                    "charm_data",
                    builder -> builder
                            .persistent(CharmData.CODEC)
                            .networkSynchronized(
                                    CharmData.STREAM_CODEC
                            )
            );

    public static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }

    private DataComponentRegister() {
    }
}
