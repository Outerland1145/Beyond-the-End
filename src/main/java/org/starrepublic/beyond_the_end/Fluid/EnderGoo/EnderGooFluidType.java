package org.starrepublic.beyond_the_end.Fluid.EnderGoo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class EnderGooFluidType extends FluidType {

    public EnderGooFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isVaporizedOnPlacement(
            Level level,
            BlockPos pos,
            FluidStack stack
    ) {
        return !EnderGooRules.canExist(level);
    }

    @Override
    public DripstoneDripInfo getDripInfo() {

        /*
         * 原版 Lava 的概率。
         *
         * 0.05859375F
         */
        return new DripstoneDripInfo(
                0.05859375F,

                /*
                 * 暫時使用紫色感比較接近的粒子。
                 *
                 * 日後可以換自己的 Goo drip particle。
                 */
                ParticleTypes.DRIPPING_OBSIDIAN_TEAR,

                /*
                 * 滴成功後：
                 *
                 * minecraft:cauldron
                 *
                 * ↓
                 *
                 * beyond_the_end:ender_goo_cauldron
                 */
                BlockRegister
                        .ENDER_GOO_CAULDRON
                        .get()
        );
    }
}
