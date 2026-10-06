package org.starrepublic.beyond_the_end.Renders.blocks.blockitems;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class DecorativeEndGatewayItemExtensions implements IClientItemExtensions {

    /*
     * Renderer 只建立一次。
     *
     * 不要每幀 new 一個 Renderer。
     */
    private final DecorativeEndGatewayItemRenderer renderer =
            new DecorativeEndGatewayItemRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return renderer;
    }
}
