package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CharmPouchScreen
        extends AbstractContainerScreen<CharmPouchMenu> {

    public CharmPouchScreen(
            CharmPouchMenu menu,
            Inventory inventory,
            Component title
    ) {

        super(menu, inventory, title);

        this.imageWidth = 176;
        this.imageHeight = 174;

        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {

        int x = this.leftPos;
        int y = this.topPos;

        // 主背景
        graphics.fill(
                x,
                y,
                x + imageWidth,
                y + imageHeight,
                0xFF25252D
        );

        // 外框
        graphics.fill(
                x,
                y,
                x + imageWidth,
                y + 2,
                0xFF777780
        );

        graphics.fill(
                x,
                y + imageHeight - 2,
                x + imageWidth,
                y + imageHeight,
                0xFF777780
        );

        // 護符袋三個欄位
        for (int i = 0; i < 3; i++) {
            drawSlot(
                    graphics,
                    x + 62 + i * 18,
                    y + 26
            );
        }

        // 玩家背包
        for (int row = 0; row < 3; row++) {

            for (int column = 0; column < 9; column++) {

                drawSlot(
                        graphics,
                        x + 8 + column * 18,
                        y + 84 + row * 18
                );
            }
        }

        // 快捷欄
        for (int column = 0; column < 9; column++) {

            drawSlot(
                    graphics,
                    x + 8 + column * 18,
                    y + 142
            );
        }
    }

    private static void drawSlot(
            GuiGraphics graphics,
            int x,
            int y
    ) {

        graphics.fill(
                x - 1,
                y - 1,
                x + 17,
                y + 17,
                0xFF101017
        );

        graphics.fill(
                x,
                y,
                x + 16,
                y + 16,
                0xFF484854
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {

        graphics.drawString(
                this.font,
                this.title,
                8,
                8,
                0xFFFFFF,
                false
        );

        graphics.drawString(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                0xCCCCCC,
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
