package org.starrepublic.beyond_the_end.Entitys.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.starrepublic.beyond_the_end.Entitys.mob.SmallEndermanEntity;

/**
 * 顯示偷來的物品：借用原版 CarriedBlockLayer 的初始座標及角度。
 * 不是一般 Humanoid 的單手拿物品動畫，而是雙手夾住物品。
 */
public final class SmallEndermanCarriedItemLayer
        extends RenderLayer<EnderMan, EndermanModel<EnderMan>> {

    private final ItemRenderer itemRenderer;

    public SmallEndermanCarriedItemLayer(
            RenderLayerParent<EnderMan, EndermanModel<EnderMan>> parent,
            ItemRenderer itemRenderer
    ) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            EnderMan entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!(entity instanceof SmallEndermanEntity smallEnderman)
                || entity.getCarriedBlock() != null) {
            return;
        }

        ItemStack stack = smallEnderman.getDisplayHeldItem();
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // 與原版 CarriedBlockLayer 相同的起始位移與方向。
        // 1.21.1 的 EndermanModel 面向 -Z，雙手向前抱物。
        poseStack.translate(0.0F, 0.6875F, -0.75F);
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));

        // ItemDisplayContext.FIXED 的模型以自身中心為原點。
        // 與原版方塊模型的 0..1 座標不同，因此不照搬負縮放。
        // 可調整這個縮放倍率以修改所有物品的視覺大小。
        poseStack.scale(0.6F, 0.6F, 0.6F);

        this.itemRenderer.renderStatic(
                entity,
                stack,
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                buffer,
                entity.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                entity.getId()
        );

        poseStack.popPose();
    }
}
