package org.starrepublic.beyond_the_end.Compat.Jade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.starrepublic.beyond_the_end.Beyond_the_end;

import org.starrepublic.beyond_the_end.Blocks.AltarPedestalBlock;
import org.starrepublic.beyond_the_end.Blocks.EssenceAltarBlock;
import org.starrepublic.beyond_the_end.Entitys.blockentity.AltarPedestalBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.DragonEssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EssenceAltarBlockEntity;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 目前使用已同步到客戶端的 BlockEntity 資料
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {

        registration.registerBlockComponent(
                AltarProvider.INSTANCE,
                EssenceAltarBlock.class
        );

        registration.registerBlockComponent(
                PedestalProvider.INSTANCE,
                AltarPedestalBlock.class
        );
    }

    // ========================================================
    // Altar tooltip
    // ========================================================

    public static final class AltarProvider
            implements IBlockComponentProvider {

        public static final AltarProvider INSTANCE =
                new AltarProvider();

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        Beyond_the_end.MODID,
                        "altar_essence"
                );

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config
        ) {

            if (!(accessor.getBlockEntity()
                    instanceof EssenceAltarBlockEntity altar)) {
                return;
            }

            // 移除 Jade 內建的物品儲存顯示
            Component fullEssence = Component.translatable(
                    "jade.beyond_the_end.essence",
                    altar.getEssence()
            );

            if (!tooltip.replace(
                    JadeIds.UNIVERSAL_ITEM_STORAGE,
                    fullEssence
            )) {
                tooltip.add(fullEssence);
            }
            if (altar instanceof DragonEssenceAltarBlockEntity dragon) {

                String failure = dragon.getRecipeFailure();

                if (!failure.isEmpty()) {

                    String reasonKey = switch (failure) {

                        case "no_output" ->
                                "jade.beyond_the_end.no_output_pedestal";

                        case "output_full" ->
                                "jade.beyond_the_end.output_pedestal_full";

                        default ->
                                "jade.beyond_the_end.unknown_failure";
                    };

                    tooltip.add(
                            Component.translatable(
                                    "jade.beyond_the_end.recipe_failed"
                            ).withStyle(ChatFormatting.RED)
                    );

                    tooltip.add(
                            Component.translatable(reasonKey)
                                    .withStyle(ChatFormatting.RED)
                    );
                }
            }
        }

        @Override
        public int getDefaultPriority() {
            // 在 Jade 內建物品儲存 Provider 之後執行
            return TooltipPosition.BODY + 2000;
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    // ========================================================
    // Pedestal tooltip
    // ========================================================

    public static final class PedestalProvider
            implements IBlockComponentProvider {

        public static final PedestalProvider INSTANCE =
                new PedestalProvider();

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        Beyond_the_end.MODID,
                        "pedestal_contents"
                );

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config
        ) {

            if (!(accessor.getBlockEntity()
                    instanceof AltarPedestalBlockEntity pedestal)) {
                return;
            }

            if (!pedestal.getActiveRecipe().isEmpty()) {

                tooltip.add(
                        Component.translatable(
                                "jade.beyond_the_end.progress",
                                pedestal.getProgress()
                        )
                );
            }
            tooltip.add(
                    Component.translatable(
                            pedestal.isOutputMode()
                                    ? "jade.beyond_the_end.pedestal_output"
                                    : "jade.beyond_the_end.pedestal_input"
                    )
            );
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
