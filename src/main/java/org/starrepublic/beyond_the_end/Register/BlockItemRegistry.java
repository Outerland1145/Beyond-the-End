package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Blocks.Blockitems.DecorativeEndGatewayItem;

import java.util.function.Supplier;

public class BlockItemRegistry {
    private BlockItemRegistry() {}
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Beyond_the_end.MODID);
    public static final DeferredItem<BlockItem> END_PORTAL_ITEM =
            ITEMS.register(
                    "end_portal",
                    registryName -> new BlockItem(
                            Blocks.END_PORTAL,
                            new Item.Properties()
                    )
            );

    public static final DeferredItem<BlockItem> END_GATEWAY_ITEM =
            ITEMS.register(
                    "end_gateway",
                    registryName -> new BlockItem(
                            Blocks.END_GATEWAY,
                            new Item.Properties()
                    )
            );
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
