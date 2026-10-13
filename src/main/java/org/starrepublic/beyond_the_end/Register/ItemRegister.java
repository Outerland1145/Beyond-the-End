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
    public static final DeferredItem<BlockItem>
            AltarStone =  ITEMS.registerSimpleBlockItem(BlockRegister.AltarStone);
    public static final DeferredItem<BlockItem>
            BarrierNodeBlock = ITEMS.registerSimpleBlockItem(BlockRegister.BarrierNodeBlock);
    public static final DeferredItem<BlockItem>
            END_ANCHOR_BLOCK = ITEMS.registerSimpleBlockItem(BlockRegister.END_ANCHOR_BLOCK);
    public static final DeferredItem<BlockItem> END_Short_Grass_Block =
            ITEMS.registerSimpleBlockItem(BlockRegister.END_Short_Grass_Block);

    public static final DeferredItem<BlockItem> END_Tall_Grass_Block =
            ITEMS.registerSimpleBlockItem(BlockRegister.END_Tall_Grass_Block);

    public static final DeferredItem<BlockItem> END_SAND =
            ITEMS.registerSimpleBlockItem(BlockRegister.END_SAND);

    public static final DeferredItem<BlockItem> END_CRYSTAL =
            ITEMS.registerSimpleBlockItem(BlockRegister.END_CRYSTAL);

    public static final DeferredItem<BlockItem> END_DRIPSTONE =
            ITEMS.registerSimpleBlockItem(BlockRegister.END_DRIPSTONE);

    public static final DeferredItem<BlockItem> Void_Block =
            ITEMS.registerSimpleBlockItem(BlockRegister.Void_Block);


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
