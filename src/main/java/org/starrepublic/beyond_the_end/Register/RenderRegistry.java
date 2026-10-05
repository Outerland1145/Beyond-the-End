package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.starrepublic.beyond_the_end.Renders.DecorativeEndGatewayRenderer;

public class RenderRegistry {
    private RenderRegistry() {}

    public static void register(IEventBus eventBus) {
        eventBus.addListener(
                RenderRegistry::registerBlockEntityRenderers
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
}
