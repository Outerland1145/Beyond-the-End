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
        this.imageHeight = 198;

        this.inventoryLabelX = 8;
        this.inventoryLabelY = 96;
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

        // 背景
        graphics.fill(
                x,
                y,
                x + imageWidth,
                y + imageHeight,
                0xFF25252D
        );

        // 外框
        graphics.renderOutline(
                x,
                y,
                imageWidth,
                imageHeight,
                0xFF888897
        );

        // 五個護符裝備欄
        for (int i = 0; i < CharmPouchMenu.POUCH_SLOTS; i++) {

            drawSlot(
                    graphics,
                    x + 44 + i * 18,
                    y + 26,
                    0xFF65518B
            );
        }

        // 五個符文輸入欄
        for (int i = 0; i < CharmPouchMenu.RUNE_SLOTS; i++) {

            drawSlot(
                    graphics,
                    x + 26 + i * 18,
                    y + 58,
                    0xFF4C5268
            );
        }

        // 合成箭頭
        graphics.drawString(
                font,
                "→",
                x + 121,
                y + 62,
                0xFFDDC98A,
                false
        );

        // 結果槽
        drawSlot(
                graphics,
                x + 139,
                y + 58,
                0xFF7C6845
        );

        // 玩家背包 27 格
        for (int row = 0; row < 3; row++) {

            for (int column = 0; column < 9; column++) {

                drawSlot(
                        graphics,
                        x + 8 + column * 18,
                        y + 108 + row * 18,
                        0xFF454550
                );
            }
        }

        // 快捷欄
        for (int column = 0; column < 9; column++) {

            drawSlot(
                    graphics,
                    x + 8 + column * 18,
                    y + 166,
                    0xFF454550
            );
        }
    }

    private static void drawSlot(
            GuiGraphics graphics,
            int x,
            int y,
            int backgroundColor
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
                backgroundColor
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {

        graphics.drawString(
                font,
                this.title,
                8,
                8,
                0xFFFFFFFF,
                false
        );

        graphics.drawString(
                font,
                Component.translatable(
                        "container.beyond_the_end.charm_pouch.runes"
                ),
                8,
                47,
                0xFFCCCCCC,
                false
        );

        graphics.drawString(
                font,
                this.playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                0xFFCCCCCC,
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
