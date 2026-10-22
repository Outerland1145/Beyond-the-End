package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.starrepublic.beyond_the_end.Register.ItemRegister;

public final class CharmClientExtensions
        implements IClientItemExtensions {

    private final CharmItemRenderer renderer =
            new CharmItemRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return renderer;
    }

    public static void register(
            RegisterClientExtensionsEvent event
    ) {
        event.registerItem(
                new CharmClientExtensions(),
                ItemRegister.CHARMITEM.get()
        );
    }
}
