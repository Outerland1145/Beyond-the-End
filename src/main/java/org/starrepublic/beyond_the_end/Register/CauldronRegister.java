package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;

import org.starrepublic.beyond_the_end.Blocks.EnderGooCauldronBlock;

public final class CauldronRegister {

    private CauldronRegister() {
    }

    /*
     * ==========================================
     * 統一註冊入口
     * ==========================================
     *
     * 主 Mod 類只需要呼叫這一個。
     */
    public static void register(IEventBus modEventBus) {

        modEventBus.addListener(
                CauldronRegister::onCommonSetup
        );

        modEventBus.addListener(
                CauldronRegister::onRegisterCauldronFluidContent
        );
    }


    /*
     * ==========================================
     * Common Setup
     * ==========================================
     */

    private static void onCommonSetup(
            FMLCommonSetupEvent event
    ) {

        /*
         * CauldronInteraction 的 Map
         * 不要在 static 初始化期間直接修改。
         */
        event.enqueueWork(
                CauldronRegister::registerInteractions
        );
    }


    /*
     * ==========================================
     * 玩家桶互動
     * ==========================================
     */

    private static void registerInteractions() {

        /*
         * ------------------------------------------
         * 原版空鍋釜
         * +
         * Ender Goo Bucket
         *
         * ↓
         *
         * Ender Goo Cauldron
         * ------------------------------------------
         */
        CauldronInteraction.EMPTY
                .map()
                .put(
                        ItemRegister.ENDER_GOO_BUCKET.get(),

                        (
                                state,
                                level,
                                pos,
                                player,
                                hand,
                                stack
                        ) -> CauldronInteraction.emptyBucket(
                                level,
                                pos,
                                player,
                                hand,
                                stack,

                                BlockRegister
                                        .ENDER_GOO_CAULDRON
                                        .get()
                                        .defaultBlockState(),

                                SoundEvents.BUCKET_EMPTY_LAVA
                        )
                );


        /*
         * ------------------------------------------
         * Ender Goo Cauldron
         * +
         * Empty Bucket
         *
         * ↓
         *
         * Ender Goo Bucket
         * +
         * Vanilla Empty Cauldron
         * ------------------------------------------
         */
        EnderGooCauldronBlock.INTERACTIONS
                .map()
                .put(
                        Items.BUCKET,

                        (
                                state,
                                level,
                                pos,
                                player,
                                hand,
                                stack
                        ) -> CauldronInteraction.fillBucket(
                                state,
                                level,
                                pos,
                                player,
                                hand,
                                stack,

                                new ItemStack(
                                        ItemRegister
                                                .ENDER_GOO_BUCKET
                                                .get()
                                ),

                                /*
                                 * 這張 interaction map
                                 * 本身就只屬於 Goo Cauldron。
                                 */
                                cauldronState -> true,

                                SoundEvents.BUCKET_FILL_LAVA
                        )
                );
    }


    /*
     * ==========================================
     * NeoForge Fluid Capability
     * ==========================================
     *
     * 告訴 NeoForge：
     *
     * beyond_the_end:ender_goo_cauldron
     *
     * 裡面真正存的是一桶 Ender Goo。
     *
     * 其他模組的 Pipe / Tank / Fluid Handler
     * 才能識別它。
     */
    private static void onRegisterCauldronFluidContent(
            RegisterCauldronFluidContentEvent event
    ) {

        event.register(
                BlockRegister
                        .ENDER_GOO_CAULDRON
                        .get(),

                FluidRegister
                        .ENDER_GOO
                        .get(),

                FluidType.BUCKET_VOLUME,

                /*
                 * 沒有 LEVEL property。
                 *
                 * Goo Cauldron 只有：
                 *
                 * 空
                 * 滿
                 */
                null
        );
    }
}
