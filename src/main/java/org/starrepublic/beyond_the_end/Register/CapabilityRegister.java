package org.starrepublic.beyond_the_end.Register;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;

public final class CapabilityRegister {

    private CapabilityRegister() {
    }

    public static void register(
            RegisterCapabilitiesEvent event
    ) {

        // ====================================================
        // 祭壇專用基座
        // ====================================================

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityRegister.ALTAR_PEDESTAL.get(),
                (pedestal, side) ->
                        pedestal.getAutomationHandler()
        );

        // ====================================================
        // 龍之精華祭壇
        // ====================================================

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityRegister.DRAGON_ESSENCE_ALTAR.get(),
                (altar, side) ->
                        altar.getEssenceAutomationHandler()
        );

        // ====================================================
        // 魂之精華祭壇
        // ====================================================

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityRegister.SOUL_ESSENCE_ALTAR.get(),
                (altar, side) ->
                        altar.getEssenceAutomationHandler()
        );

        // ====================================================
        // 炎之精華祭壇
        // ====================================================

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityRegister.FIRE_ESSENCE_ALTAR.get(),
                (altar, side) ->
                        altar.getEssenceAutomationHandler()
        );
    }
}
