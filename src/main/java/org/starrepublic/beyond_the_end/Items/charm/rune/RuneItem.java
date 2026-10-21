package org.starrepublic.beyond_the_end.Items.charm.rune;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.starrepublic.beyond_the_end.Register.DataComponentRegister;

import java.util.List;

public class RuneItem extends Item {

    public RuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {

        RuneData data = stack.get(
                DataComponentRegister.RUNE_TYPE.get()
        );

        if (data == null) {
            return super.getName(stack);
        }

        ResourceLocation id = data.id();

        String translationKey =
                "rune."
                        + id.getNamespace()
                        + "."
                        + id.getPath().replace('/', '.');

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

        RuneData data = stack.get(
                DataComponentRegister.RUNE_TYPE.get()
        );

        if (data == null) {
            tooltip.add(
                    Component.translatable(
                            "tooltip.beyond_the_end.rune_unassigned"
                    ).withStyle(ChatFormatting.GRAY)
            );
            return;
        }

        if (flag.isAdvanced()) {
            tooltip.add(
                    Component.literal(data.id().toString())
                            .withStyle(ChatFormatting.DARK_GRAY)
            );
        }
    }
}
