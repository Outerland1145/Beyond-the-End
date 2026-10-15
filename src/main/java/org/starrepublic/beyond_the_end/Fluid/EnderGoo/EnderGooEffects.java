package org.starrepublic.beyond_the_end.Fluid.EnderGoo;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.starrepublic.beyond_the_end.Register.Tags;

public final class EnderGooEffects {

    private EnderGooEffects() {
    }


    public static boolean isImmune(
            Entity entity
    ) {

        return entity.getType()
                .is(
                        Tags.ENDER_GOO_IMMUNE
                );
    }


    public static void applyHazard(
            Level level,
            Entity entity
    ) {

        if (isImmune(entity)) {
            return;
        }


        if (
                level.isClientSide
                        || !(entity instanceof LivingEntity living)
        ) {
            return;
        }


        if (living.tickCount % 10 == 0) {

            living.addEffect(
                    new MobEffectInstance(
                            MobEffects.POISON,
                            40,
                            2
                    )
            );

            living.addEffect(
                    new MobEffectInstance(
                            MobEffects.WEAKNESS,
                            40,
                            2
                    )
            );

            living.addEffect(
                    new MobEffectInstance(
                            MobEffects.DIG_SLOWDOWN,
                            40,
                            2
                    )
            );
        }


        if (living.tickCount % 20 == 0) {

            living.hurt(
                    level.damageSources().magic(),
                    2.0F
            );
        }
    }
}
