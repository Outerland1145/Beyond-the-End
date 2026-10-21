package org.starrepublic.beyond_the_end.Entitys.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.starrepublic.beyond_the_end.Entitys.entity.SafeEndCrystalEntity;

public class SafeEndCrystalRenderer
        extends EntityRenderer<SafeEndCrystalEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace(
                    "textures/entity/end_crystal/end_crystal.png"
            );

    private static final RenderType RENDER_TYPE =
            RenderType.entityCutoutNoCull(TEXTURE);

    private final ModelPart glass;
    private final ModelPart cube;
    private final ModelPart base;

    public SafeEndCrystalRenderer(
            EntityRendererProvider.Context context
    ) {
        super(context);

        ModelPart root =
                context.bakeLayer(ModelLayers.END_CRYSTAL);

        glass = root.getChild("glass");
        cube = root.getChild("cube");
        base = root.getChild("base");

        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(
            SafeEndCrystalEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();

        float time = entity.tickCount + partialTick;

        float bob = Mth.sin(time * 0.2F) / 2.0F + 0.5F;
        bob = (bob * bob + bob) * 0.4F;
        bob -= 1.4F;

        float rotation = time * 3.0F;

        VertexConsumer vertices =
                buffer.getBuffer(RENDER_TYPE);

        int light = LightTexture.FULL_BRIGHT;

        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.translate(0.0F, -0.5F, 0.0F);

        // 底座
        if (entity.showsBottom()) {

            base.render(
                    poseStack,
                    vertices,
                    light,
                    OverlayTexture.NO_OVERLAY
            );
        }

        // 外層水晶
        poseStack.mulPose(
                Axis.YP.rotationDegrees(rotation)
        );

        poseStack.translate(
                0.0F,
                1.5F + bob / 2.0F,
                0.0F
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(60.0F)
        );

        glass.render(
                poseStack,
                vertices,
                light,
                OverlayTexture.NO_OVERLAY
        );

        // 中層水晶
        poseStack.scale(
                0.875F,
                0.875F,
                0.875F
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(60.0F)
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(rotation)
        );

        glass.render(
                poseStack,
                vertices,
                light,
                OverlayTexture.NO_OVERLAY
        );

        // 核心
        poseStack.scale(
                0.875F,
                0.875F,
                0.875F
        );

        poseStack.mulPose(
                Axis.ZP.rotationDegrees(60.0F)
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(rotation)
        );

        cube.render(
                poseStack,
                vertices,
                light,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();

        super.render(
                entity,
                entityYaw,
                partialTick,
                poseStack,
                buffer,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            SafeEndCrystalEntity entity
    ) {
        return TEXTURE;
    }
}
