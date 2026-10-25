package org.starrepublic.beyond_the_end.Commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.starrepublic.beyond_the_end.World.DFM.BTEDragonFightManager;

public final class BTEEndFightCommands {

    private BTEEndFightCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(

                Commands.literal("beyondtheend")

                        .then(
                                Commands.literal("dragonfight")

                                        .then(
                                                Commands.literal("unseal")

                                                        .requires(
                                                                source ->
                                                                        source.hasPermission(2)
                                                        )

                                                        .executes(context -> {

                                                            ServerLevel end =
                                                                    context.getSource()
                                                                            .getServer()
                                                                            .getLevel(Level.END);

                                                            if (end == null
                                                                    || !(end.getDragonFight()
                                                                    instanceof BTEDragonFightManager manager)) {

                                                                context.getSource().sendFailure(
                                                                        Component.literal(
                                                                                "Beyond The End Dragon Fight Manager unavailable."
                                                                        )
                                                                );

                                                                return 0;
                                                            }

                                                            if (!manager.unsealDragonFight()) {

                                                                context.getSource().sendFailure(
                                                                        Component.literal(
                                                                                "Dragon Fight is not sealed."
                                                                        )
                                                                );

                                                                return 0;
                                                            }

                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal(
                                                                            "Dragon Fight sealing removed."
                                                                    ),
                                                                    true
                                                            );

                                                            return 1;
                                                        })
                                        )
                        )
        );
    }
}

