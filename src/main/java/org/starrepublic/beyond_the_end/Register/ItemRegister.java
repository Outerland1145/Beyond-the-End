package org.starrepublic.beyond_the_end.Register;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Items.SafeEndCrystalItem;
import org.starrepublic.beyond_the_end.Items.charm.CharmItem;
import org.starrepublic.beyond_the_end.Items.rune.RuneItem;

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

    public static final DeferredItem<Item> VOID_CRYSTAL_SHARD = ITEMS.registerSimpleItem("void_crystal_shard");


    public static final DeferredItem<Item> DRAGON_ESSENCE = ITEMS.registerSimpleItem("dragon_essence");
    public static final DeferredItem<Item> SOUL_ESSENCE = ITEMS.registerSimpleItem("soul_essence");
    public static final DeferredItem<Item> FIRE_ESSENCE = ITEMS.registerSimpleItem("fire_essence");
    public static final DeferredItem<Item> END_POWDER = ITEMS.registerSimpleItem("end_powder");
    public static final DeferredItem<RuneItem>
            RUNEITEM = ITEMS.register(
            "rune",
            () -> new RuneItem(
                    new Item.Properties()
            )
    );
    public static final DeferredItem<Item>
            CHARMPOUCH = ITEMS.register(
            "charm_pouch",
            () -> new Item(
                    new Item.Properties()
            )
    );
    public static final DeferredItem<CharmItem>
            CHARMITEM = ITEMS.register(
            "charm",
            () -> new CharmItem(
                    new Item.Properties()
            )
    );
    public static final DeferredItem<Item>
            END_CRYSTAL_REMOVER = ITEMS.register(
            "crystal_remover",
            () -> new Item(
                    new Item.Properties().durability(1024)
            )
    );
            //registerSimpleItem("crystal_remover");
    public static final Supplier<SafeEndCrystalItem>
            SAFE_END_CRYSTAL = ITEMS.register(
            "safe_end_crystal",
            () -> new SafeEndCrystalItem(
                    new Item.Properties().stacksTo(64)
            )
    );


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
