package org.starrepublic.beyond_the_end.Commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.starrepublic.beyond_the_end.Data.BeyondEndSavedData;
import org.starrepublic.beyond_the_end.World.EndBarrier.EndBarrierManager;
import org.starrepublic.beyond_the_end.World.progression.BarrierNodeDirection;

public final class EndBarrierCommands {

    private EndBarrierCommands() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("beyondtheend")

                        .requires(source ->
                                source.hasPermission(2))

                        .then(
                                Commands.literal("barrier")

                                        // 開啟結界
                                        .then(
                                                Commands.literal("on")
                                                        .executes(context -> {

                                                            ServerLevel level =
                                                                    context
                                                                            .getSource()
                                                                            .getLevel();

                                                            EndBarrierManager
                                                                    .lockBarrier(level);

                                                            context
                                                                    .getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "主島結界已開啟"
                                                                            ),
                                                                            true
                                                                    );

                                                            return 1;
                                                        })
                                        )

                                        // 解除結界
                                        .then(
                                                Commands.literal("off")
                                                        .executes(context -> {

                                                            ServerLevel level =
                                                                    context
                                                                            .getSource()
                                                                            .getLevel();

                                                            EndBarrierManager
                                                                    .unlockBarrier(level);

                                                            context
                                                                    .getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "主島結界已解除"
                                                                            ),
                                                                            true
                                                                    );

                                                            return 1;
                                                        })
                                        )

                                        // 查詢
                                        .then(
                                                Commands.literal("status")
                                                        .executes(context -> {

                                                            ServerLevel level =
                                                                    context
                                                                            .getSource()
                                                                            .getLevel();

                                                            boolean enabled =
                                                                    EndBarrierManager
                                                                            .isBarrierEnabled(level);

                                                            context
                                                                    .getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "主島結界狀態: "
                                                                                            + (
                                                                                            enabled
                                                                                                    ? "開啟"
                                                                                                    : "已解除"
                                                                                    )
                                                                            ),
                                                                            false
                                                                    );

                                                            return 1;
                                                        })
                                        )
                                        .then(
                                                Commands.literal("progress")
                                                        .executes(context -> {

                                                            ServerLevel level =
                                                                    context
                                                                            .getSource()
                                                                            .getLevel();

                                                            BeyondEndSavedData data =
                                                                    BeyondEndSavedData
                                                                            .get(level);

                                                            context
                                                                    .getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "主島結界進度：\n"
                                                                                            + "鞘翅: "
                                                                                            + yesNo(
                                                                                            data.hasObtainedElytra()
                                                                                    )
                                                                                            + "\n北方節點: "
                                                                                            + yesNo(
                                                                                            data.isNodeDestroyed(
                                                                                                    BarrierNodeDirection.NORTH
                                                                                            )
                                                                                    )
                                                                                            + "\n南方節點: "
                                                                                            + yesNo(
                                                                                            data.isNodeDestroyed(
                                                                                                    BarrierNodeDirection.SOUTH
                                                                                            )
                                                                                    )
                                                                                            + "\n東方節點: "
                                                                                            + yesNo(
                                                                                            data.isNodeDestroyed(
                                                                                                    BarrierNodeDirection.EAST
                                                                                            )
                                                                                    )
                                                                                            + "\n西方節點: "
                                                                                            + yesNo(
                                                                                            data.isNodeDestroyed(
                                                                                                    BarrierNodeDirection.WEST
                                                                                            )
                                                                                    )
                                                                            ),
                                                                            false
                                                                    );

                                                            return 1;
                                                        })
                                        )
                                        .then(
                                                Commands.literal("resetprogress")
                                                        .executes(context -> {

                                                            ServerLevel level =
                                                                    context
                                                                            .getSource()
                                                                            .getLevel();

                                                            BeyondEndSavedData
                                                                    .get(level)
                                                                    .resetBarrierProgress();

                                                            EndBarrierManager
                                                                    .lockBarrier(level);

                                                            context
                                                                    .getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "主島結界進度已完全重置"
                                                                            ),
                                                                            true
                                                                    );

                                                            return 1;
                                                        })
                                        )
                        )
        );
    }
    private static String yesNo(
            boolean value
    ) {

        return value
                ? "完成"
                : "未完成";
    }
}
