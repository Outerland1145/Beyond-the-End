package org.starrepublic.beyond_the_end;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.starrepublic.beyond_the_end.Register.RenderRegistry;

@Mod(value = Beyond_the_end.MODID, dist = Dist.CLIENT)
public class Beyond_the_end_Client {
    public Beyond_the_end_Client(IEventBus modEventBus) {
        RenderRegistry.register(modEventBus);
    }
}
