package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.IEventBus;
import org.starrepublic.beyond_the_end.CreativeTabs.Blocks;

public final class TotalRegistries {
    private TotalRegistries() {}

    public static void register(IEventBus eventBus) {
        DataRegister.register(eventBus);
        //BTEStructureType.register(eventBus);
        //BTEStructureLimiters.bootstrap();
        JigsawRegister.register(eventBus);
        /*
         * 順序：
         *
         * Block
         * ↓
         * Item
         * ↓
         * BlockEntity
         */

        BlockRegister.register(eventBus);
        BlockEntityRegister.register(eventBus);
        BlockItemRegistry.register(eventBus);
        ItemRegister.register(eventBus);
        /*以下為創造模式面板*/
        Blocks.register(eventBus);

    }
}
