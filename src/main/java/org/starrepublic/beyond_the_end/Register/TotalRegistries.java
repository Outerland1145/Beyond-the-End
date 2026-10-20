package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.IEventBus;
import org.starrepublic.beyond_the_end.Blocks.EssenceAltarBlock;
import org.starrepublic.beyond_the_end.CreativeTabs.Blocks;
import org.starrepublic.beyond_the_end.Events.SpawnRules;

public final class TotalRegistries {
    private TotalRegistries() {}

    public static void register(IEventBus eventBus) {
        DataRegister.register(eventBus);
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
        FluidRegister.register(eventBus);
        BlockRegister.register(eventBus);
        BlockEntityRegister.register(eventBus);
        EntityRegister.register(eventBus);
        BlockItemRegistry.register(eventBus);
        ItemRegister.register(eventBus);
        CommonSetupRegistry.register(eventBus);
        EffectRegister.register(eventBus);
        RecipeRegister.register(eventBus);
        Blocks.register(eventBus);
        eventBus.addListener(SpawnRules::registerSpawnPlacements);
        eventBus.addListener(CapabilityRegister::register);

    }
}
