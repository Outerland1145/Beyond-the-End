package org.starrepublic.beyond_the_end.configs;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BTEClientConfig {
    public final ModConfigSpec.BooleanValue enableBarrierDistortion;

    public final ModConfigSpec.DoubleValue distortionStrength;

    public final ModConfigSpec.IntValue distortionStartDistance;

    public final ModConfigSpec.BooleanValue enableVoidParticles;

    public final ModConfigSpec.IntValue voidParticleCount;

    public final ModConfigSpec.DoubleValue voidParticleRadius;


    public BTEClientConfig(ModConfigSpec.Builder builder) {

        builder.push("barrier_visual");

        enableBarrierDistortion = builder
                .comment("Whether the End barrier screen distortion effect is enabled.")
                .define("enableBarrierDistortion", true);

        distortionStrength = builder
                .comment("Maximum strength of the End barrier distortion effect.")
                .defineInRange(
                        "distortionStrength",
                        1.0,
                        0.0,
                        5.0
                );

        distortionStartDistance = builder
                .comment("Distance from the End barrier where screen distortion begins.")
                .defineInRange(
                        "distortionStartDistance",
                        128,
                        16,
                        1024
                );

        builder.pop();


        builder.push("void_particles");

        enableVoidParticles = builder
                .comment("Whether void particles near the End barrier are enabled.")
                .define("enableVoidParticles", true);

        voidParticleCount = builder
                .comment("Number of void particles spawned around the player.")
                .defineInRange(
                        "voidParticleCount",
                        80,
                        0,
                        1000
                );

        voidParticleRadius = builder
                .comment("Radius around the player in which void particles spawn.")
                .defineInRange(
                        "voidParticleRadius",
                        16.0,
                        1.0,
                        64.0
                );

        builder.pop();
    }
}
