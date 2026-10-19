package org.starrepublic.beyond_the_end.Compat.Jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.starrepublic.beyond_the_end.Beyond_the_end;

import org.starrepublic.beyond_the_end.Blocks.AltarPedestalBlock;
import org.starrepublic.beyond_the_end.Blocks.EssenceAltarBlock;
import org.starrepublic.beyond_the_end.Entitys.blockentity.AltarPedestalBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EssenceAltarBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.WailaPlugin;
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

            tooltip.add(
                    Component.translatable(
                            "jade.beyond_the_end.essence",
                            altar.getEssence()
                    )
            );
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

            ItemStack stack = pedestal.getItem();

            if (stack.isEmpty()) {

                tooltip.add(
                        Component.translatable(
                                "jade.beyond_the_end.empty"
                        )
                );

                return;
            }

            tooltip.add(
                    Component.translatable(
                            "jade.beyond_the_end.item",
                            stack.getHoverName(),
                            stack.getCount()
                    )
            );

            if (!pedestal.getActiveRecipe().isEmpty()) {

                tooltip.add(
                        Component.translatable(
                                "jade.beyond_the_end.progress",
                                pedestal.getProgress()
                        )
                );
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
