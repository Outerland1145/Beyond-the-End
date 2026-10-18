package org.starrepublic.beyond_the_end.Entitys.client.renderer;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.starrepublic.beyond_the_end.Entitys.client.model.SmallEndermanModel;

/**
 * 延續原版 Renderer 的貼圖、發光眼睛、搬運方塊圖層。
 * 只把原版 Model 換成能偵測偷來物品的 Model，並加上一個渲染層。
 * 不需要重新註冊 ModelLayer，也不需要複製原版材質。
 */
public final class SmallEndermanRenderer extends EndermanRenderer {

    public SmallEndermanRenderer(EntityRendererProvider.Context context) {
        super(context);

        // LivingEntityRenderer.model 是 protected。
        // 保持同一個泛型 EndermanModel<EnderMan>，不需強制轉型。
        this.model = new SmallEndermanModel(context.bakeLayer(ModelLayers.ENDERMAN));
        this.addLayer(new SmallEndermanCarriedItemLayer(this, context.getItemRenderer()));
    }
}
