package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.starrepublic.beyond_the_end.Renders.blockentity.DecorativeEndGatewayRenderer;
import org.starrepublic.beyond_the_end.Renders.blocks.blockitems.DecorativeEndGatewayItemExtensions;

public class RenderRegistry {
    private RenderRegistry() {}

    public static void register(IEventBus eventBus) {
        eventBus.addListener(
                RenderRegistry::registerBlockEntityRenderers
        );
        eventBus.addListener(
                RenderRegistry::registerItemRenderers
        );
    }

    private static void registerBlockEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                BlockEntityRegister.DECORATIVE_END_GATEWAY.get(),
                DecorativeEndGatewayRenderer::new
        );
    }
    private static void registerItemRenderers(
            RegisterClientExtensionsEvent event
    ) {

        event.registerItem(
                new DecorativeEndGatewayItemExtensions(),
                ItemRegister.DECORATIVE_END_GATEWAY.get()
        );
    }
}
