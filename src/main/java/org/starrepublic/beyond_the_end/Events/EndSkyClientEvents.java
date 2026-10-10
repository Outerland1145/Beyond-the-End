package org.starrepublic.beyond_the_end.Events;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.starrepublic.beyond_the_end.Renders.sky.BeyondEndDimensionEffects;
import org.starrepublic.beyond_the_end.network.EndSkySeedState;

public final class EndSkyClientEvents {

    private EndSkyClientEvents() {
    }

    public static void onLoggingOut(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {

        EndSkySeedState.clear();
        BeyondEndDimensionEffects.resetSky();
    }
}
