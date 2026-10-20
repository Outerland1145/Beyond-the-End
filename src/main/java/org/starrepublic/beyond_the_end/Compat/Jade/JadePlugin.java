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
import org.starrepublic.beyond_the_end.Entitys.blockentity.FireEssenceAltarBlockEntity;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 目前使用已同步到客戶端的 BlockEntity 資料
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {

        registration.registerBlockComponent(
                DragonAltarProvider.INSTANCE,
                EssenceAltarBlock.class
        );

        registration.registerBlockComponent(
                PedestalProvider.INSTANCE,
                AltarPedestalBlock.class
        );

        registration.registerBlockComponent(
                FireModeProvider.INSTANCE,
                EssenceAltarBlock.class
        );
    }

    // ========================================================
    // Altar tooltip
    // ========================================================

    public static final class DragonAltarProvider
            implements IBlockComponentProvider {

        public static final DragonAltarProvider INSTANCE =
                new DragonAltarProvider();

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

            // ========================================================
            // 龍之精華祭壇模式
            // ========================================================

            if (altar instanceof DragonEssenceAltarBlockEntity dragon) {

                String mode = dragon.getPowderMode();

                String modeKey;

                if ("speed".equals(mode)) {

                    modeKey = "jade.beyond_the_end.mode_speed";

                } else if ("efficiency".equals(mode)) {

                    modeKey = "jade.beyond_the_end.mode_efficiency";

                } else {

                    modeKey = "jade.beyond_the_end.mode_normal";
                }

                tooltip.add(
                        Component.translatable(
                                "jade.beyond_the_end.mode",
                                Component.translatable(modeKey)
                        )
                );

                // ====================================================
                // 模式效果剩餘時間
                // ====================================================

                if (!"normal".equals(mode)) {

                    int remainingTicks = dragon.getPowderTicks();

                    if (remainingTicks > 0) {

                        int seconds = (remainingTicks + 19) / 20;

                        tooltip.add(
                                Component.translatable(
                                        "jade.beyond_the_end.powder_time",
                                        seconds
                                )
                        );

                    } else {

                        tooltip.add(
                                Component.translatable(
                                        "jade.beyond_the_end.powder_inactive"
                                )
                        );
                    }
                }
            }

            tooltip.replace(JadeIds.UNIVERSAL_ITEM_STORAGE,fullEssence);
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
    public static final class FireModeProvider
            implements IBlockComponentProvider {

        public static final FireModeProvider INSTANCE =
                new FireModeProvider();

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(
                        "beyond_the_end",
                        "fire_altar_mode"
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

            // ========================================================
            // 炎之精華祭壇模式
            // ========================================================

            if (altar instanceof FireEssenceAltarBlockEntity fire) {

                String mode = fire.getPowderMode();

                String modeKey;

                if ("speed".equals(mode)) {

                    modeKey = "jade.beyond_the_end.mode_speed";

                } else if ("efficiency".equals(mode)) {

                    modeKey = "jade.beyond_the_end.mode_efficiency";

                } else if("normal".equals(mode)){

                    modeKey = "jade.beyond_the_end.mode_normal";
                }
                else {
                    modeKey = "jade.beyond_the_end.mode_smelting";
                }

                tooltip.add(
                        Component.translatable(
                                "jade.beyond_the_end.mode",
                                Component.translatable(modeKey)
                        )
                );

                // ====================================================
                // 模式效果剩餘時間
                // ====================================================

                if (!"normal".equals(mode)&&!"smelting".equals(mode)) {

                    int remainingTicks = fire.getPowderTicks();

                    if (remainingTicks > 0) {

                        int seconds = (remainingTicks + 19) / 20;

                        tooltip.add(
                                Component.translatable(
                                        "jade.beyond_the_end.powder_time",
                                        seconds
                                )
                        );

                    } else {

                        tooltip.add(
                                Component.translatable(
                                        "jade.beyond_the_end.powder_inactive"
                                )
                        );
                    }
                }
            }

            tooltip.replace(JadeIds.UNIVERSAL_ITEM_STORAGE,fullEssence);
            if (altar instanceof FireEssenceAltarBlockEntity fire) {

                String failure = fire.getRecipeFailure();

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

            // ========================================================
            // 祭壇注魔進度條
            // ========================================================

            if (!pedestal.getActiveRecipe().isEmpty()
                    && pedestal.getRequiredTicks() > 0) {

                int current = pedestal.getProcessingTicks();
                int total = pedestal.getRequiredTicks();

                // 避免進度超出合法範圍
                float progress = Math.clamp(
                        (float) current / (float) total,
                        0.0F,
                        1.0F
                );

                int percent = Math.round(progress * 100.0F);

                tooltip.add(
                        Component.translatable(
                                "jade.beyond_the_end.progress",
                                percent
                        )
                );

                // Jade 原生進度條
                tooltip.add(
                        IElementHelper.get().progress(progress)
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
