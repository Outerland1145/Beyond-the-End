package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

import net.minecraft.world.level.Level;

import java.util.List;

public class CharmPouch extends Item {

    public static final int SLOT_COUNT = 3;

    public CharmPouch(Properties properties) {
        super(properties);
    }

    /**
     * 主手右鍵開啟護符袋。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack stack = player.getItemInHand(hand);

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer) {

            int inventorySlot =
                    player.getInventory().selected;

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, menuPlayer) ->
                                    new CharmPouchMenu(
                                            containerId,
                                            inventory,
                                            menuPlayer.getInventory().selected
                                    ),
                            Component.translatable(
                                    "container.beyond_the_end.charm_pouch"
                            )
                    )
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }

    /**
     * 顯示護符袋的儲存內容。
     */
    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        super.appendHoverText(
                stack,
                context,
                tooltip,
                flag
        );

        NonNullList<ItemStack> contents =
                NonNullList.withSize(
                        SLOT_COUNT,
                        ItemStack.EMPTY
                );

        stack.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        ).copyInto(contents);

        int occupied = 0;

        for (ItemStack content : contents) {
            if (!content.isEmpty()) {
                occupied++;
            }
        }

        tooltip.add(
                Component.translatable(
                        "tooltip.beyond_the_end.charm_pouch.contents",
                        occupied,
                        SLOT_COUNT
                ).withStyle(ChatFormatting.GRAY)
        );

        for (ItemStack content : contents) {

            if (!content.isEmpty()) {

                tooltip.add(
                        Component.literal(" • ")
                                .append(content.getHoverName())
                                .withStyle(ChatFormatting.DARK_GRAY)
                );
            }
        }

        tooltip.add(
                Component.translatable(
                        "tooltip.beyond_the_end.charm_pouch.activation"
                ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
