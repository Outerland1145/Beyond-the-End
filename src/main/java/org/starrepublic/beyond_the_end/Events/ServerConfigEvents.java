package org.starrepublic.beyond_the_end.Events;

import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.starrepublic.beyond_the_end.Mixins.accessor.EndSpawnAccessor;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

public final class ServerConfigEvents {
    private ServerConfigEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(
            ServerAboutToStartEvent event
    ) {
        apply();
    }
    public static void apply() {
        int x =
                BTEConfigs.SERVER
                        .EndSpawnPointX
                        .get();

        int y =
                BTEConfigs.SERVER
                        .EndSpawnPointY
                        .get();

        int z =
                BTEConfigs.SERVER
                        .EndSpawnPointZ
                        .get();

        EndSpawnAccessor
                .beyondTheEnd$setEndSpawnPoint(
                        new BlockPos(
                                x,
                                y,
                                z
                        )
                );
    }

    }
