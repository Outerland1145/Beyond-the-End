package org.starrepublic.beyond_the_end.Entitys.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.starrepublic.beyond_the_end.Entitys.blockentity.AltarPedestalBlockEntity;

public class EssenceAltarPedestalRenderer
        implements BlockEntityRenderer<AltarPedestalBlockEntity> {

    private final ItemRenderer itemRenderer;
    private final Font font;

    public EssenceAltarPedestalRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }
    @Override
    public AABB getRenderBoundingBox(
            AltarPedestalBlockEntity pedestal
    ) {
        return new AABB(pedestal.getBlockPos())
                .inflate(0.3D, 0.8D, 0.3D);
    }
    @Override
    public void render(
            AltarPedestalBlockEntity pedestal,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {

        if (pedestal.getLevel() == null) {
            return;
        }

        ItemStack item = pedestal.getItem();

        if (item.isEmpty()) {
            return;
        }

        float time =
                pedestal.getLevel().getGameTime() + partialTick;

        double floating =
                Math.sin(time * 0.08D) * 0.045D;

        // ====================================================
        // 物品模型
        // ====================================================

        poseStack.pushPose();

        poseStack.translate(
                0.5D,
                0.95D + floating,
                0.5D
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(time * 1.5F)
        );

        poseStack.scale(
                0.65F,
                0.65F,
                0.65F
        );

        itemRenderer.renderStatic(
                item.copyWithCount(1),
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                pedestal.getLevel(),
                0
        );

        poseStack.popPose();

        // ====================================================
        // 數量顯示
        // ====================================================

        String count = "x" + item.getCount();

        poseStack.pushPose();

        poseStack.translate(
                0.5D,
                1.32D + floating,
                0.5D
        );

        poseStack.mulPose(
                Minecraft.getInstance()
                        .getEntityRenderDispatcher()
                        .cameraOrientation()
        );

        poseStack.scale(
                -0.025F,
                -0.025F,
                0.025F
        );

        float x = -font.width(count) / 2.0F;

        font.drawInBatch(
                count,
                x,
                0,
                0xFFFFFFFF,
                true,
                poseStack.last().pose(),
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                LightTexture.FULL_BRIGHT
        );

        poseStack.popPose();
    }
}
