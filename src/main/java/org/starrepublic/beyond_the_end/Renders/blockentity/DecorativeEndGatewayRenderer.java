package org.starrepublic.beyond_the_end.Renders.blockentity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import org.starrepublic.beyond_the_end.Entitys.DecorativeEndGatewayBlockEntity;

public class DecorativeEndGatewayRenderer extends TheEndPortalRenderer<DecorativeEndGatewayBlockEntity> {

    public DecorativeEndGatewayRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);
    }

    /*
     * 讓星空佔滿整個方塊高度。
     */
    @Override
    protected float getOffsetUp() {
        return 1.0F;
    }

    @Override
    protected float getOffsetDown() {
        return 0.0F;
    }

    /*
     * 使用終界折躍門而不是普通終界傳送門的 RenderType。
     */
    @Override
    protected RenderType renderType() {
        return RenderType.endGateway();
    }
}
