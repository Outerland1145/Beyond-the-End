package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.IEventBus;

public final class TotalRegistries {
    private TotalRegistries() {}

    public static void register(IEventBus eventBus) {

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
        ItemRegister.register(eventBus);
    }
}
