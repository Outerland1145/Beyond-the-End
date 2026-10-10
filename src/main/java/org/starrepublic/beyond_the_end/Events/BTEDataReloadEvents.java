package org.starrepublic.beyond_the_end.Events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.starrepublic.beyond_the_end.World.worldgen.biomedefinition.EndBiomeDefinitionLoader;

public class BTEDataReloadEvents {
    private BTEDataReloadEvents() {
    }


    @SubscribeEvent
    public static void onAddReloadListener(
            AddReloadListenerEvent event
    ) {

        event.addListener(
                new EndBiomeDefinitionLoader(
                        event.getRegistryAccess()
                )
        );
    }
}
