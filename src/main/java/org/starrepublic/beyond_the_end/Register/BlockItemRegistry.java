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
    public static final DeferredItem<Item> END_TORCH =
            ITEMS.register(
                    "end_torch",
                    () -> new StandingAndWallBlockItem(
                            BlockRegister.END_TORCH.get(),
                            BlockRegister.END_WALL_TORCH.get(),
                            new Item.Properties(),
                            Direction.DOWN
                    )
            );
    public static final DeferredItem<BlockItem>
            END_LARTERN =  ITEMS.registerSimpleBlockItem(BlockRegister.END_LARTERN);
    public static final DeferredItem<BlockItem>
            END_CAMPFIRE =  ITEMS.registerSimpleBlockItem(BlockRegister.END_CAMPFIRE);
    public static final DeferredItem<BlockItem>
            END_LARTERN_BLOCK =  ITEMS.registerSimpleBlockItem(BlockRegister.END_LARTERN_BLOCK);
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
    public static final DeferredItem<BlockItem> END_STONE_ENCHANTED =
            ITEMS.registerSimpleBlockItem(BlockRegister.END_STONE_ENCHANTED);
    public static final DeferredItem<BlockItem> END_STONE_INFESTED =
            ITEMS.registerSimpleBlockItem(BlockRegister.END_STONE_INFESTED);
    public static final DeferredItem<BlockItem> END_STONE_BURNED =
            ITEMS.registerSimpleBlockItem(BlockRegister.END_STONE_BURNED);

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
