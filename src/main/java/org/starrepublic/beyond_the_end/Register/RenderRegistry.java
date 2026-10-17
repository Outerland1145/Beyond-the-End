package org.starrepublic.beyond_the_end.Register;

import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.starrepublic.beyond_the_end.Renders.blockentity.DecorativeEndGatewayRenderer;
import org.starrepublic.beyond_the_end.Renders.blockentity.EndCampfireRenderer;
import org.starrepublic.beyond_the_end.Renders.blockentity.EndNetherPortalRenderer;
import org.starrepublic.beyond_the_end.Renders.blocks.blockitems.DecorativeEndGatewayItemExtensions;
import org.starrepublic.beyond_the_end.Renders.sky.BeyondEndDimensionEffects;

public class RenderRegistry {
    private RenderRegistry() {}

    public static void register(IEventBus eventBus) {
        eventBus.addListener(
                RenderRegistry::registerBlockEntityRenderers
        );
        eventBus.addListener(
                RenderRegistry::registerItemRenderers
        );
        eventBus.addListener(
                RenderRegistry::registerDimensionEffects
        );
    }

    private static void registerBlockEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                BlockEntityRegister.DECORATIVE_END_GATEWAY.get(),
                DecorativeEndGatewayRenderer::new
        );
        event.registerBlockEntityRenderer(
                BlockEntityRegister.END_NETHER_PORTAL.get(),
                EndNetherPortalRenderer::new
        );
        event.registerBlockEntityRenderer(
                BlockEntityRegister.END_CAMPFIRE.get(),
                EndCampfireRenderer::new
        );
    }
    private static void registerItemRenderers(
            RegisterClientExtensionsEvent event
    ) {

        event.registerItem(
                new DecorativeEndGatewayItemExtensions(),
                BlockRegister.DECORATIVE_END_GATEWAY.get().asItem()
        );
        event.registerItem(
                new DecorativeEndGatewayItemExtensions(),
                BlockItemRegistry.END_GATEWAY_ITEM.get()
        );
        event.registerItem(
                new DecorativeEndGatewayItemExtensions(),
                BlockItemRegistry.END_PORTAL_ITEM.get()
        );

    }
    public static void registerDimensionEffects(
            RegisterDimensionSpecialEffectsEvent event
    ) {
        event.register(
                BuiltinDimensionTypes.END_EFFECTS,
                new BeyondEndDimensionEffects()
        );
    }
}
