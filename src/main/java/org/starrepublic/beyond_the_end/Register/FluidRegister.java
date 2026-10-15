package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Fluid.EnderGoo.EnderGooFluidType;

public class FluidRegister {
    private FluidRegister(){}
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys.FLUID_TYPES,
                    Beyond_the_end.MODID
            );
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(
                    Registries.FLUID,
                    Beyond_the_end.MODID
            );
    public static final DeferredHolder<FluidType, EnderGooFluidType>
            ENDER_GOO_TYPE =
            FLUID_TYPES.register(
                    "ender_goo",

                    () -> new EnderGooFluidType(

                            FluidType.Properties
                                    .create()

                                    .density(1200)

                                    .viscosity(1800)

                                    .lightLevel(12)

                                    .canSwim(true)

                                    .canDrown(true)

                                    .supportsBoating(false)

                                    .sound(
                                            SoundActions.BUCKET_FILL,
                                            SoundEvents.BUCKET_FILL_LAVA
                                    )

                                    .sound(
                                            SoundActions.BUCKET_EMPTY,
                                            SoundEvents.BUCKET_EMPTY_LAVA
                                    )

                                    /*
                                     * 新增這個。
                                     */
                                    .sound(
                                            SoundActions.CAULDRON_DRIP,
                                            SoundEvents
                                                    .POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON
                                    )
                    )
            );


    /*
     * ================================
     * Source Fluid
     * ================================
     *
     * 完整液體方塊。
     */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ENDER_GOO =
            FLUIDS.register(
                    "ender_goo",
                    () -> new BaseFlowingFluid.Source(
                            FluidRegister.ENDER_GOO_PROPERTIES
                    )
            );


    /*
     * ================================
     * Flowing Fluid
     * ================================
     *
     * 正在流動的 Ender Goo。
     */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_ENDER_GOO =
            FLUIDS.register(
                    "flowing_ender_goo",
                    () -> new BaseFlowingFluid.Flowing(
                            FluidRegister.ENDER_GOO_PROPERTIES
                    )
            );


    /*
     * ================================
     * Flow Properties
     * ================================
     */
    public static final BaseFlowingFluid.Properties ENDER_GOO_PROPERTIES =
            new BaseFlowingFluid.Properties(
                    ENDER_GOO_TYPE,
                    ENDER_GOO,
                    FLOWING_ENDER_GOO
            )
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(1)
                    .tickRate(18)
                    .explosionResistance(100.0F)
                    .block(BlockRegister.ENDER_GOO)
                    .bucket(ItemRegister.ENDER_GOO_BUCKET);
    public static void register(IEventBus eventBus) {

        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
