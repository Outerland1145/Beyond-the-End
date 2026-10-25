package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.IEventBus;
import org.starrepublic.beyond_the_end.CreativeTabs.Blocks;
import org.starrepublic.beyond_the_end.Events.SpawnRules;
import org.starrepublic.beyond_the_end.Items.charm.CharmTypes;
import org.starrepublic.beyond_the_end.World.DFM.BTEDragonFightRegistry;
import org.starrepublic.beyond_the_end.World.DragonSkill.DragonFirebombSkill;
import org.starrepublic.beyond_the_end.World.DragonSkill.DragonSkyBombardmentSkill;

public final class TotalRegistries {
    private TotalRegistries() {}

    public static void register(IEventBus eventBus) {
        DataRegister.register(eventBus);
        JigsawRegister.register(eventBus);
        DataComponentRegister.register(eventBus);
        CharmTypes.register(eventBus);
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
        MenuRegister.register(eventBus);
        BTEDragonFightRegistry.register();
        eventBus.addListener(SpawnRules::registerSpawnPlacements);
        eventBus.addListener(CapabilityRegister::register);
        DragonFirebombSkill.register();
        DragonSkyBombardmentSkill.register();

    }
}
