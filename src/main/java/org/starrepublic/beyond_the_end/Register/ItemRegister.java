package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
    public static final DeferredItem<BucketItem> ENDER_GOO_BUCKET =
            ITEMS.register(
                    "ender_goo_bucket",
                    () -> new BucketItem(
                            FluidRegister.ENDER_GOO.get(),
                            new Item.Properties()
                                    .stacksTo(1)
                                    .craftRemainder(Items.BUCKET)
                    )
            );


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
