package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Blocks.Blockitems.DecorativeEndGatewayItem;

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
    public static final DeferredItem<Item>
            AltarStone = ITEMS.register(
            "altar_stone",
            () -> new BlockItem(
                    BlockRegister.AltarStone.get(),
                    new Item.Properties()

            )
    );
    public static final DeferredItem<Item>
            BarrierNodeBlock = ITEMS.register(
            "barrier_node_block",
            () -> new BlockItem(
                    BlockRegister.BarrierNodeBlock.get(),
                    new Item.Properties()

            )
    );
    public static final DeferredItem<Item>
            END_ANCHOR_BLOCK = ITEMS.register(
            "end_anchor",
            () -> new BlockItem(
                    BlockRegister.END_ANCHOR_BLOCK.get(),
                    new Item.Properties()

            )
    );


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
