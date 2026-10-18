package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public class PotionRegister {
    public static final DeferredRegister<Potion> POTION =
            DeferredRegister.create(
                    Registries.POTION,
                    Beyond_the_end.MODID
            );
    public static final DeferredHolder<Potion, Potion>
            ENDER_INFESTED = POTION.register(
            "ender_infested",
            () -> new Potion(
                    "ender_infested",
                    new MobEffectInstance(
                            EffectRegister.ENDER_INFESTED,
                            3600,
                            0
                    )
            )
    );
    public static void register(IEventBus eventBus) {
        POTION.register(eventBus);
    }
}
