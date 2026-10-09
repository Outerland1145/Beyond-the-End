package org.starrepublic.beyond_the_end;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.starrepublic.beyond_the_end.Events.EndBarrierEvents;
import org.starrepublic.beyond_the_end.Events.EndBarrierProgressEvents;
import org.starrepublic.beyond_the_end.Events.ServerConfigEvents;
import org.starrepublic.beyond_the_end.Register.CommandRegistry;
import org.starrepublic.beyond_the_end.Register.PayloadRegistry;
import org.starrepublic.beyond_the_end.Register.TotalRegistries;
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
            NeoForge.EVENT_BUS.register(CommandRegistry.class);
            NeoForge.EVENT_BUS.register(EndBarrierProgressEvents.class);
            modEventBus.register(PayloadRegistry.class);

        }

}
