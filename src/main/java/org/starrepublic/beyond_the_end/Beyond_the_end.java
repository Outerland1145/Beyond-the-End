package org.starrepublic.beyond_the_end;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.starrepublic.beyond_the_end.Blocks.AltarPedestalInteraction;
import org.starrepublic.beyond_the_end.Blocks.EssenceAltarBlock;
import org.starrepublic.beyond_the_end.Events.*;
import org.starrepublic.beyond_the_end.Items.rune.RuneClient;
import org.starrepublic.beyond_the_end.Register.*;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Beyond_the_end.MODID)
public class Beyond_the_end {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "beyond_the_end";
        public Beyond_the_end(IEventBus modEventBus,ModContainer modContainer) {
            modContainer.registerConfig(
                    ModConfig.Type.CLIENT,
                    BTEConfigs.CLIENT_SPEC
            );

            modContainer.registerConfig(
                    ModConfig.Type.SERVER,
                    BTEConfigs.SERVER_SPEC
            );
            TotalRegistries.register(modEventBus);
            NeoForge.EVENT_BUS.register(ServerConfigEvents.class);
            NeoForge.EVENT_BUS.register(EndBarrierEvents.class);
            NeoForge.EVENT_BUS.register(BTEDataReloadEvents.class);
            NeoForge.EVENT_BUS.register(CommandRegistry.class);
            NeoForge.EVENT_BUS.register(EndBarrierProgressEvents.class);
            NeoForge.EVENT_BUS.register(InteractionEvents.class);
            FeatureRegister.register( modEventBus );
            modEventBus.addListener(BlockEntityEvents::addValidBlocks);
            modEventBus.addListener(PayloadRegistry::registerPayloads);
            NeoForge.EVENT_BUS.addListener(EssenceAltarBlock::onFireAltarModeRightClick);
            NeoForge.EVENT_BUS.addListener(EssenceAltarBlock::onDragonAltarModeRightClick);
            NeoForge.EVENT_BUS.addListener(EndSkySyncEvents::onPlayerLoggedIn);
            NeoForge.EVENT_BUS.addListener(CrystalRemoverEvents::onCrystalInteract);
            NeoForge.EVENT_BUS.addListener(AltarPedestalInteraction::onRightClickBlock);
            NeoForge.EVENT_BUS.addListener(FireSmeltingEvents::onIncomingDamage);
            NeoForge.EVENT_BUS.addListener(CrystalRemoverClientEvents::onMouseScroll);
            NeoForge.EVENT_BUS.addListener(FireSmeltingEvents::onPlayerTickPre);
            NeoForge.EVENT_BUS.addListener(FireSmeltingEvents::onPlayerTickPost);

        }
        private static void onClientSetup(
                FMLClientSetupEvent event
        ) {
            RuneClient.setup(event);
        }

}
