package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Effect.EnderInfestedEffect;
import org.starrepublic.beyond_the_end.Events.EnderInfestedCloudHandler;

import java.util.function.Supplier;

public class EffectRegister {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(
                    Registries.MOB_EFFECT,
                    Beyond_the_end.MODID
            );

    public static final DeferredHolder<MobEffect, EnderInfestedEffect>
            ENDER_INFESTED = registerEffect(
            "ender_infested",
            EnderInfestedEffect::new
    );

    // 統一註冊方法
    public static <T extends MobEffect>
    DeferredHolder<MobEffect, T> registerEffect(
            String name,
            Supplier<T> supplier
    ) {
        return EFFECTS.register(name, supplier);
    }

    // 在模組主類中呼叫
    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
        PotionRegister.register(eventBus);
        NeoForge.EVENT_BUS.addListener(
                EnderInfestedCloudHandler::onEntityTick
        );

    }
}
