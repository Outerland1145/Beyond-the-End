package org.starrepublic.beyond_the_end.CreativeTabs;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Register.BlockItemRegistry;
import org.starrepublic.beyond_the_end.Register.ItemRegister;

import java.util.function.Supplier;

public class Blocks {
    private Blocks() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    Beyond_the_end.MODID
            );

    public static final Supplier<CreativeModeTab> FADING_END_TAB =
            CREATIVE_TABS.register(
                    "beyond_the_end",
                    () -> CreativeModeTab.builder()

                            // 分頁名稱
                            .title(
                                    Component.translatable(
                                            "itemGroup.beyond_the_end.beyond_the_end_block"
                                    )
                            )

                            // 分頁圖示
                            .icon(
                                    () -> new ItemStack(
                                            BlockItemRegistry.DECORATIVE_END_GATEWAY.get()
                                    )
                            )

                            // 分頁中的所有物品
                            .displayItems((parameters, output) -> {
                                output.accept( BlockItemRegistry.DECORATIVE_END_GATEWAY.get());
                                output.accept( BlockItemRegistry.END_PORTAL_ITEM.get());
                                output.accept( BlockItemRegistry.END_GATEWAY_ITEM.get());
                                output.accept( BlockItemRegistry.AltarStone.get());
                                output.accept( BlockItemRegistry.BarrierNodeBlock.get());
                                output.accept( BlockItemRegistry.END_ANCHOR_BLOCK.get());
                                output.accept( BlockItemRegistry.Void_Block.get());
                                output.accept( BlockItemRegistry.END_Short_Grass_Block.get());
                                output.accept( BlockItemRegistry.END_Tall_Grass_Block.get());
                                output.accept( BlockItemRegistry.END_STONE_ENCHANTED.get());
                                output.accept( BlockItemRegistry.END_STONE_INFESTED.get());
                                output.accept( BlockItemRegistry.END_STONE_BURNED.get());
                                output.accept( BlockItemRegistry.END_DRIPSTONE.get());
                                output.accept( BlockItemRegistry.END_CRYSTAL.get());
                                output.accept( BlockItemRegistry.END_SAND.get());
                                output.accept( BlockItemRegistry.END_TORCH.get());
                                output.accept( BlockItemRegistry.END_CAMPFIRE.get());
                                output.accept( BlockItemRegistry.END_LARTERN.get());
                                output.accept( BlockItemRegistry.END_LARTERN_BLOCK.get());
                                output.accept( ItemRegister.ENDER_GOO_BUCKET);


                            })

                            .build()
            );

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
