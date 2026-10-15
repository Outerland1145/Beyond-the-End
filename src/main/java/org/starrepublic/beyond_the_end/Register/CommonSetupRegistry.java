package org.starrepublic.beyond_the_end.Register;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonSetupRegistry {
    private CommonSetupRegistry() {
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
                CommonSetupRegistry::onCommonSetup
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
        event.enqueueWork(() -> {

            FlowerPotBlock flowerPot =
                    (FlowerPotBlock) Blocks.FLOWER_POT;

            flowerPot.addPlant(
                    BlockRegister.MAGIC_FLOWER.getId(),
                    BlockRegister.POTTED_MAGIC_FLOWER
            );
        });
    }
}
