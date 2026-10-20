package org.starrepublic.beyond_the_end.CreativeTabs;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Register.*;

import java.util.function.Supplier;

public class Blocks {
    private Blocks() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    Beyond_the_end.MODID
            );

    public static final Supplier<CreativeModeTab> BTE_END_Building_TAB =
            CREATIVE_TABS.register(
                    "beyond_the_end_building_tab",
                    () -> CreativeModeTab.builder()

                            // 分頁名稱
                            .title(
                                    Component.translatable(
                                            "itemGroup.beyond_the_end.beyond_the_end_building_tab"
                                    )
                            )

                            // 分頁圖示
                            .icon(
                                    () -> new ItemStack(
                                            BlockRegister.DECORATIVE_END_GATEWAY.get()
                                    )
                            )

                            // 分頁中的所有物品
                            .displayItems((parameters, output) -> {
                                output.accept( BlockRegister.DECORATIVE_END_GATEWAY.get());
                                output.accept( BlockItemRegistry.END_PORTAL_ITEM.get());
                                output.accept( BlockItemRegistry.END_GATEWAY_ITEM.get());
                                output.accept( BlockRegister.END_FIRE.torchitem.get());
                                output.accept( BlockRegister.BarrierNodeBlock.get());
                                output.accept( BlockRegister.AltarStone.get());
                                output.accept( BlockRegister.END_ANCHOR_BLOCK.get());
                                output.accept( BlockRegister.Void_Block.get());
                                output.accept( BlockRegister.END_FIRE.campfire.get());
                                output.accept( BlockRegister.END_FIRE.lantern.get());
                                output.accept( BlockRegister.END_LARTERN_BLOCK.get());
                                output.accept( BlockRegister.SPOOKY.log.get());
                                output.accept( BlockRegister.SPOOKY.wood.get());
                                output.accept( BlockRegister.SPOOKY.strippedlog.get());
                                output.accept( BlockRegister.SPOOKY.strippedwood.get());
                                output.accept( BlockRegister.SPOOKY.planks.get());
                                output.accept( BlockRegister.SPOOKY.stairs.get());
                                output.accept( BlockRegister.SPOOKY.slab.get());
                                output.accept( BlockRegister.SPOOKY.fence.get());
                                output.accept( BlockRegister.SPOOKY.fencegate.get());
                                output.accept( BlockRegister.SPOOKY.door.get());
                                output.accept( BlockRegister.SPOOKY.trapdoor.get());
                                output.accept( BlockRegister.SPOOKY.pressureplate.get());
                                output.accept( BlockRegister.SPOOKY.button.get());
                                output.accept( BlockRegister.Essence_Altar.soul.get());
                                output.accept( BlockRegister.Essence_Altar.dragon.get());
                                output.accept( BlockRegister.Essence_Altar.fires.get());
                            })

                            .build()
    );
    public static final Supplier<CreativeModeTab> BTE_END_Nature_TAB =
            CREATIVE_TABS.register(
                    "beyond_the_end_nature_tab",
                    () -> CreativeModeTab.builder()

                            // 分頁名稱
                            .title(
                                    Component.translatable(
                                            "itemGroup.beyond_the_end.beyond_the_end_nature_tab"
                                    )
                            )

                            // 分頁圖示
                            .icon(
                                    () -> new ItemStack(
                                            BlockRegister.END_STONE_ENCHANTED.get()
                                    )
                            )

                            // 分頁中的所有物品
                            .displayItems((parameters, output) -> {
                                        output.accept( BlockRegister.END_STONE_ENCHANTED.get());
                                        output.accept( BlockRegister.END_STONE_INFESTED.get());
                                        output.accept( BlockRegister.END_STONE_BURNED.get());
                                        output.accept( BlockRegister.END_DRIPSTONE.get());
                                        output.accept( BlockRegister.END_SAND.get());
                                        output.accept( BlockRegister.VOID_CRYSTAL.smallcrystalbud.get());
                                        output.accept( BlockRegister.VOID_CRYSTAL.mediumcrystalbud.get());
                                        output.accept( BlockRegister.VOID_CRYSTAL.largecrystalbud.get());
                                        output.accept( BlockRegister.VOID_CRYSTAL.crystalcluster.get());
                                        output.accept( BlockRegister.VOID_CRYSTAL.crystalblock.get());
                                        output.accept( BlockRegister.VOID_CRYSTAL.crystalbuddingblock.get());
                                        output.accept( BlockRegister.END_GRASS.shortgrass.get());
                                        output.accept( BlockRegister.END_GRASS.tallgrass.get());
                                        output.accept( BlockRegister.Enchanted_Short_Grass_Block.shortgrass.get());
                                        output.accept( BlockRegister.Enchanted_Tall_Grass_Block.shortgrass.get());
                                        output.accept( BlockRegister.BURNINGGRASS.shortgrass.get());
                                        output.accept( BlockRegister.BURNINGGFERN.shortgrass.get());
                                        output.accept( BlockRegister.MAGIC_FLOWER.get());
                                        output.accept( BlockRegister.SPOOKY.sapling.get());
                                        output.accept( BlockRegister.SPOOKY.leaves.get());
                                        output.accept( BlockRegister.INFESTED_FERN.shortgrass.get() );
                                        output.accept( BlockRegister.INFESTED_FERN.tallgrass.get() );
                                        output.accept( BlockRegister.INFESTED_GRASS.shortgrass.get() );
                                        output.accept( BlockRegister.INFESTED_ROOT.shortgrass.get() );
                            }
                            ).build()
            );
    public static final Supplier<CreativeModeTab> BTE_END_ITEM_TAB =
            CREATIVE_TABS.register(
                    "beyond_the_end_item_tab",
                    () -> CreativeModeTab.builder()

                            // 分頁名稱
                            .title(
                                    Component.translatable(
                                            "itemGroup.beyond_the_end.beyond_the_end_item_tab"
                                    )
                            )

                            // 分頁圖示
                            .icon(
                                    () -> new ItemStack(
                                            ItemRegister.VOID_CRYSTAL_SHARD.get()
                                    )
                            )

                            // 分頁中的所有物品
                            .displayItems((parameters, output) -> {
                                        output.accept( ItemRegister.ENDER_GOO_BUCKET.get());
                                        output.accept( ItemRegister.VOID_CRYSTAL_SHARD.get());
                                        output.accept( ItemRegister.END_CRYSTAL_REMOVER.get());
                                        output.accept( ItemRegister.DRAGON_ESSENCE.get());
                                        output.accept( ItemRegister.SOUL_ESSENCE.get());
                                        output.accept( ItemRegister.FIRE_ESSENCE.get());
                                        output.accept(EntityRegister.SMALL_ENDERMAN.spawnEgg().get());
                                        output.accept(EntityRegister.ANGRY_ENDERMAN.spawnEgg().get());
                                        output.accept(EntityRegister.ANCIENT_ENDER_DRAGON.spawnEgg().get());
                                        output.accept(PotionContents.createItemStack(Items.POTION, PotionRegister.ENDER_INFESTED));
                                        output.accept(PotionContents.createItemStack(Items.SPLASH_POTION, PotionRegister.ENDER_INFESTED));
                                        output.accept(PotionContents.createItemStack(Items.LINGERING_POTION, PotionRegister.ENDER_INFESTED));
                                        output.accept(PotionContents.createItemStack(Items.TIPPED_ARROW, PotionRegister.ENDER_INFESTED));
                            }
                            ).build()
            );
    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
