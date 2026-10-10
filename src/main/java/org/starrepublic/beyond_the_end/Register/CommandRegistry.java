package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.starrepublic.beyond_the_end.Commands.EndBarrierCommands;
import org.starrepublic.beyond_the_end.Commands.EndLocateCommand;

public final class CommandRegistry {
    private CommandRegistry() {}

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event) {

        EndBarrierCommands.register(
                event.getDispatcher()
        );
        EndLocateCommand.register(
                event.getDispatcher()
        );
    }
}
