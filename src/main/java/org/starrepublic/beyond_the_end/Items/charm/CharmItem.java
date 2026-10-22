package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.starrepublic.beyond_the_end.Register.DataComponentRegister;
import org.starrepublic.beyond_the_end.Register.ItemRegister;

import java.util.List;
import java.util.function.Consumer;

public class CharmItem extends Item {

    public CharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {

        CharmData data = stack.get(
                DataComponentRegister.CHARM_DATA.get()
        );

        if (data == null) {
            return super.getName(stack);
        }

        String translationKey =
                "charm."
                        + data.type().getNamespace()
                        + "."
                        + data.type().getPath()
                        .replace('/', '.');

        return Component.translatable(translationKey);
    }

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

        CharmTooltip.append(
                stack,
                context,
                tooltip
        );
    }
}
