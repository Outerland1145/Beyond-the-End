package org.starrepublic.beyond_the_end;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.starrepublic.beyond_the_end.Events.ClientEndEffectEvents;
import org.starrepublic.beyond_the_end.Events.EndSkyClientEvents;
import org.starrepublic.beyond_the_end.Events.FluidEvents;
import org.starrepublic.beyond_the_end.Items.charm.CharmClientExtensions;
import org.starrepublic.beyond_the_end.Items.charm.CharmItem;
import org.starrepublic.beyond_the_end.Items.rune.RuneClient;
import org.starrepublic.beyond_the_end.Register.ItemRegister;
import org.starrepublic.beyond_the_end.Register.RenderRegistry;

@Mod(value = Beyond_the_end.MODID, dist = Dist.CLIENT)
public class Beyond_the_end_Client {
    public Beyond_the_end_Client(IEventBus modEventBus) {
        RenderRegistry.register(modEventBus);
        FluidEvents.register(modEventBus);
        NeoForge.EVENT_BUS.register(ClientEndEffectEvents.class);
        NeoForge.EVENT_BUS.addListener(EndSkyClientEvents::onLoggingOut);
        modEventBus.addListener(RuneClient::setup);
        modEventBus.addListener(CharmClientExtensions::register);

    }

}
