package org.starrepublic.beyond_the_end.GUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EnhancedBrewingStandBlockEntity;

public class EnhancedBrewingScreen
        extends AbstractContainerScreen<EnhancedBrewingMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace(
                    "textures/gui/container/brewing_stand.png"
            );

    private static final ResourceLocation FUEL_SPRITE =
            ResourceLocation.withDefaultNamespace(
                    "container/brewing_stand/fuel_length"
            );

    private static final ResourceLocation PROGRESS_SPRITE =
            ResourceLocation.withDefaultNamespace(
                    "container/brewing_stand/brew_progress"
            );

    private static final ResourceLocation BUBBLES_SPRITE =
            ResourceLocation.withDefaultNamespace(
                    "container/brewing_stand/bubbles"
            );

    private static final int[] BUBBLE_HEIGHTS = {
            29, 24, 20, 16, 11, 6, 0
    };

    public EnhancedBrewingScreen(
            EnhancedBrewingMenu menu,
            Inventory inventory,
            Component title
    ) {

        super(menu, inventory, title);

        imageWidth = 176;
        imageHeight = 166;

        titleLabelX = 8;
        titleLabelY = 6;

        inventoryLabelX = 8;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {

        int x = leftPos;
        int y = topPos;

        // 原版完整背景。
        graphics.blit(
                BACKGROUND,
                x,
                y,
                0,
                0,
                imageWidth,
                imageHeight
        );

        renderFuel(graphics, x, y);
        renderBrewingProgress(graphics, x, y);
    }

    private void renderFuel(
            GuiGraphics graphics,
            int x,
            int y
    ) {

        int fuel = menu.getFuelUses();

        if (fuel <= 0) {
            return;
        }

        int width = Math.clamp(
                (int) Math.ceil(
                        18.0D * fuel
                                / EnhancedBrewingStandBlockEntity.FUEL_USES
                ),
                0,
                18
        );

        if (width <= 0) {
            return;
        }

        // 原版燃料圖示位於 GUI (60,44)，尺寸 18×4。
        graphics.enableScissor(
                x + 60,
                y + 44,
                x + 60 + width,
                y + 48
        );

        graphics.blitSprite(
                FUEL_SPRITE,
                x + 60,
                y + 44,
                18,
                4
        );

        graphics.disableScissor();
    }

    private void renderBrewingProgress(
            GuiGraphics graphics,
            int x,
            int y
    ) {

        int remaining = menu.getRemainingBrewTime();
        int duration = menu.getTotalBrewTime();

        if (remaining <= 0 || duration <= 0) {
            return;
        }

        float progress = Math.clamp(
                (duration - remaining) / (float) duration,
                0.0F,
                1.0F
        );

        int height = Math.clamp(
                (int) (28.0F * progress),
                0,
                28
        );

        // 原版釀造進度圖示：GUI (97,16)，尺寸 9×28。
        if (height > 0) {

            graphics.enableScissor(
                    x + 97,
                    y + 16,
                    x + 106,
                    y + 16 + height
            );

            graphics.blitSprite(
                    PROGRESS_SPRITE,
                    x + 97,
                    y + 16,
                    9,
                    28
            );

            graphics.disableScissor();
        }

        // 氣泡：保持原版動畫節奏。
        int bubbleIndex = Math.floorMod(
                remaining / 2,
                BUBBLE_HEIGHTS.length
        );

        int bubbleHeight = BUBBLE_HEIGHTS[bubbleIndex];

        if (bubbleHeight > 0) {

            int bubbleX = x + 63;
            int bubbleY = y + 43 - bubbleHeight;

            graphics.enableScissor(
                    bubbleX,
                    bubbleY,
                    bubbleX + 12,
                    bubbleY + bubbleHeight
            );

            graphics.blitSprite(
                    BUBBLES_SPRITE,
                    x + 63,
                    y + 14,
                    12,
                    29
            );

            graphics.disableScissor();
        }
    }

    /**
     * 標題與物品欄名稱。
     *
     * 當其他語言的標題過長時，自動縮小文字，
     * 避免覆蓋到 JEI。
     */
    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {

        Component displayTitle =
                Component.translatableWithFallback(
                        "container.beyond_the_end.end_brewing_stand",
                        "Enhanced Brewing Stand"
                );

        int textWidth = font.width(displayTitle);

        float scale = textWidth > 160
                ? 160.0F / textWidth
                : 1.0F;

        graphics.pose().pushPose();

        graphics.pose().translate(
                titleLabelX,
                titleLabelY,
                0
        );

        graphics.pose().scale(
                scale,
                scale,
                1.0F
        );

        graphics.drawString(
                font,
                displayTitle,
                0,
                0,
                0x404040,
                false
        );

        graphics.pose().popPose();

        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                0x404040,
                false
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }
}
