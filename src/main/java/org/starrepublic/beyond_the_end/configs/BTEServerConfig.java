package org.starrepublic.beyond_the_end.configs;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BTEServerConfig {
    public final ModConfigSpec.BooleanValue enableEndBarrier;

    public final ModConfigSpec.IntValue barrierRadius;

    public final ModConfigSpec.BooleanValue useoriginalsetting;

    public final ModConfigSpec.IntValue EndSpawnPointX;
    public final ModConfigSpec.IntValue EndSpawnPointY;
    public final ModConfigSpec.IntValue EndSpawnPointZ;



    public BTEServerConfig(ModConfigSpec.Builder builder) {

        builder.push("end_barrier");

        enableEndBarrier = builder
                .comment(
                        "Whether the End barrier is enabled.",
                        "When enabled, players cannot reach the End MainIsland before unlocking the barrier."
                )
                .define(
                        "enableEndBarrier",
                        true
                );

        barrierRadius = builder
                .comment(
                        "Radius of the End barrier measured from 0,0."
                )
                .defineInRange(
                        "barrierRadius",
                        300,
                        250,
                        850
                );

        builder.pop();


        builder.push("barrier_unlock");

        useoriginalsetting = builder
                .comment(
                        "Whether Used Original Setting to unlock the End barrier."
                )
                .define(
                        "useoriginalsetting",
                        true
                );
        builder.pop();
        builder.push("End Spawn Point");
        EndSpawnPointX = builder
                .comment(
                        "Setting the End Spawn Point Coordinate X"
                )
                .defineInRange(
                        "End Spawn Point Coordinate X",
                        1000,
                        -30000000,
                        30000000
                );
        EndSpawnPointY = builder
                .comment(
                        "Setting the End Spawn Point Coordinate Y"
                )
                .defineInRange(
                        "End Spawn Point Coordinate Y",
                        50,
                        1,
                        30000000
                );
        EndSpawnPointZ = builder
                .comment(
                        "Setting the End Spawn Point Coordinate Z"
                )
                .defineInRange(
                        "End Spawn Point Coordinate Z",
                        0,
                        -30000000,
                        30000000
                );
        builder.pop();
    }
}
