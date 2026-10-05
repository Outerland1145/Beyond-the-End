package org.starrepublic.beyond_the_end.Register;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Items.DecorativeEndGatewayItem;

import java.util.function.Supplier;

public class ItemRegister {
    private ItemRegister() {}
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Beyond_the_end.MODID);
    public static final Supplier<DecorativeEndGatewayItem>
            DECORATIVE_END_GATEWAY =
            ITEMS.register(
                    "decorative_end_gateway",
                    () -> new DecorativeEndGatewayItem(
                            BlockRegister.DECORATIVE_END_GATEWAY.get(),
                            new Item.Properties()
                    )
            );
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
