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
import org.starrepublic.beyond_the_end.Entitys.blockentity.EssenceAltarBlockEntity;

public class EssenceAltarRenderer<T extends EssenceAltarBlockEntity>
        implements BlockEntityRenderer<T> {

    private final ItemRenderer itemRenderer;
    private final Font font;

    public EssenceAltarRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }
    @Override
    public AABB getRenderBoundingBox(T altar) {
        return new AABB(altar.getBlockPos())
                .inflate(0.5D, 1.5D, 0.5D);
    }
    @Override
    public void render(
            T altar,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {

        if (altar.getLevel() == null || altar.getEssence() <= 0) {
            return;
        }

        ItemStack essenceItem = altar.getDisplayEssenceItem();

        float time = altar.getLevel().getGameTime() + partialTick;

        double floating = Math.sin(time * 0.10D) * 0.07D;

        // ====================================================
        // 旋轉的精華物品
        // ====================================================

        if (!essenceItem.isEmpty()) {

            poseStack.pushPose();

            poseStack.translate(
                    0.5D,
                    1.25D + floating,
                    0.5D
            );

            poseStack.mulPose(
                    Axis.YP.rotationDegrees(time * 2.0F)
            );

            poseStack.scale(
                    0.65F,
                    0.65F,
                    0.65F
            );

            itemRenderer.renderStatic(
                    essenceItem,
                    ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    altar.getLevel(),
                    0
            );

            poseStack.popPose();
        }

        // ====================================================
        // 浮空精華數量
        // ====================================================

        String amount = Integer.toString(altar.getEssence());

        poseStack.pushPose();

        poseStack.translate(
                0.5D,
                1.70D + floating,
                0.5D
        );

        // 文字永遠朝向玩家視角
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

        float x = -font.width(amount) / 2.0F;

        font.drawInBatch(
                amount,
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
