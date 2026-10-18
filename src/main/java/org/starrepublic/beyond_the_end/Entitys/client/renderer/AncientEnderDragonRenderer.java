package org.starrepublic.beyond_the_end.Entitys.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.starrepublic.beyond_the_end.Entitys.mob.AncientEnderDragonEntity;

/**
 * 獨立 Renderer：只重用原版 ENDER_DRAGON 模型圖層與貼圖，
 * 不繼承或呼叫 EnderDragonRenderer (其泛型限定為 EnderDragon)。
 *
 * 注意：這是模型重用版。要 1:1 原版骨骼運動仍需移植
 * EnderDragonRenderer.DragonModel 的龍頸／龍尾逐節動畫算法。
 */
public class AncientEnderDragonRenderer extends EntityRenderer<AncientEnderDragonEntity> {

    private static final ResourceLocation SKIN =
            ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
    private static final ResourceLocation EYES =
            ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
    private static final ResourceLocation DYING_TEXTURE =
            ResourceLocation.withDefaultNamespace(
                    "textures/entity/enderdragon/dragon_exploding.png"
            );
    private static final float HALF_SQRT_3 =
            (float) (Math.sqrt(3.0D) / 2.0D);
    private final ModelPart root;

    public AncientEnderDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.root = context.bakeLayer(ModelLayers.ENDER_DRAGON);
        this.shadowRadius = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(AncientEnderDragonEntity dragon) {
        return SKIN;
    }

    @Override
    public void render(AncientEnderDragonEntity dragon, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int light) {
        stack.pushPose();

        // 原版龍的模型採用其專用的反向座標系。
        stack.mulPose(new Quaternionf().rotationY((float)Math.toRadians(180.0F - yaw)));
        stack.mulPose(new Quaternionf().rotationX((float)Math.toRadians(
                Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot()))));
        // 原版龍的外層模型轉換
        stack.translate(0.0F, 0.0F, 1.0F);
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.501F, 0.0F);

        root.getAllParts().forEach(ModelPart::resetPose);
        float t = dragon.tickCount + partialTick;
        stack.pushPose();

        float phase = t * 0.28F;
        float bob = Mth.sin(phase - 1.0F) + 1.0F;
        bob = (bob * bob + bob * 2.0F) * 0.05F;

        stack.translate(0.0F, bob - 2.0F, -3.0F);
        stack.mulPose(Axis.XP.rotationDegrees(bob * 2.0F));
        animateDragonBody(t);

        int hurtOverlay = OverlayTexture.pack(
                0.0F,
                dragon.hurtTime > 0
        );

        if (dragon.deathTime > 0) {

            // 死亡進度：0.0 → 1.0
            float progress = Mth.clamp(
                    (dragon.deathTime + partialTick) / 200.0F,
                    0.0F,
                    1.0F
            );

            int deathColor = FastColor.ARGB32.color(
                    Mth.floor(progress * 255.0F),
                    -1
            );

            // 第一層：逐漸出現的解體材質
            VertexConsumer exploding = buffers.getBuffer(
                    RenderType.dragonExplosionAlpha(DYING_TEXTURE)
            );

            renderAnimatedMesh(
                    dragon, partialTick,
                    stack, exploding,
                    light, OverlayTexture.NO_OVERLAY,
                    deathColor
            );

            // 第二層：原版死亡時的龍身貼圖
            VertexConsumer decal = buffers.getBuffer(
                    RenderType.entityDecal(SKIN)
            );

            renderAnimatedMesh(
                    dragon, partialTick,
                    stack, decal,
                    light, hurtOverlay,
                    -1
            );

        } else {

            // 正常狀態的龍身
            VertexConsumer body = buffers.getBuffer(
                    RenderType.entityCutoutNoCull(SKIN)
            );

            renderAnimatedMesh(
                    dragon, partialTick,
                    stack, body,
                    light, hurtOverlay,
                    -1
            );
        }

        // 發光眼睛保持獨立繪製
        VertexConsumer eyes = buffers.getBuffer(
                RenderType.eyes(EYES)
        );

        renderAnimatedMesh(
                dragon, partialTick,
                stack, eyes,
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                -1
        );
        stack.popPose(); // 結束模型內部變換
        if (dragon.deathTime > 0) {

            float progress = Mth.clamp(
                    (dragon.deathTime + partialTick) / 200.0F,
                    0.0F,
                    1.0F
            );

            stack.pushPose();
            stack.translate(0.0F, -1.0F, -2.0F);

            renderRays(
                    stack,
                    progress,
                    buffers.getBuffer(RenderType.dragonRays())
            );

            renderRays(
                    stack,
                    progress,
                    buffers.getBuffer(RenderType.dragonRaysDepth())
            );

            stack.popPose();
        }
        stack.popPose(); // 結束整隻龍的外部變換

        if (dragon.isAlive()) {

            EndCrystal crystal = dragon.getVisualHealingCrystal();

            if (crystal != null) {

                Vec3 offset = crystal.getPosition(partialTick)
                        .subtract(dragon.getPosition(partialTick));

                stack.pushPose();

                EnderDragonRenderer.renderCrystalBeams(
                        (float) offset.x,
                        (float) offset.y,
                        (float) offset.z,
                        partialTick,
                        dragon.tickCount,
                        stack,
                        buffers,
                        light
                );

                stack.popPose();
            }
        }
        super.render(dragon, yaw, partialTick, stack, buffers, light);
    }
    private void animateDragonBody(float time) {

        ModelPart body = root.getChild("body");
        ModelPart head = root.getChild("head");

        float phase = time * 0.28F;

        float sin = Mth.sin(phase);
        float cos = Mth.cos(phase);

        float bob = Mth.sin(phase - 1.0F) + 1.0F;
        bob = (bob * bob + bob * 2.0F) * 0.05F;

        // 下顎開合
        head.getChild("jaw").xRot =
                (sin + 1.0F) * 0.2F;

        // 左右翅膀
        ModelPart leftWing = root.getChild("left_wing");
        ModelPart rightWing = root.getChild("right_wing");

        leftWing.xRot = 0.125F - cos * 0.2F;
        leftWing.yRot = -0.25F;
        leftWing.zRot = -(sin + 0.125F) * 0.8F;

        rightWing.xRot = leftWing.xRot;
        rightWing.yRot = -leftWing.yRot;
        rightWing.zRot = -leftWing.zRot;

        // 翅尖延遲彎曲
        float wingTipAngle =
                (Mth.sin(phase + 2.0F) + 0.5F) * 0.75F;

        leftWing.getChild("left_wing_tip").zRot =
                wingTipAngle;

        rightWing.getChild("right_wing_tip").zRot =
                -wingTipAngle;

        // 四肢隨飛行節奏輕微擺動
        animateLeg(root, "left_front_leg",
                "left_front_leg_tip", "left_front_foot",
                false, bob);

        animateLeg(root, "right_front_leg",
                "right_front_leg_tip", "right_front_foot",
                false, bob);

        animateLeg(root, "left_hind_leg",
                "left_hind_leg_tip", "left_hind_foot",
                true, bob);

        animateLeg(root, "right_hind_leg",
                "right_hind_leg_tip", "right_hind_foot",
                true, bob);
    }
    private void animateLeg(
            ModelPart parent,
            String legName,
            String tipName,
            String footName,
            boolean hindLeg,
            float bob
    ) {
        ModelPart leg = parent.getChild(legName);
        ModelPart tip = leg.getChild(tipName);
        ModelPart foot = tip.getChild(footName);

        if (hindLeg) {
            leg.xRot = 1.0F + bob * 0.1F;
            tip.xRot = 0.5F + bob * 0.1F;
            foot.xRot = 0.75F + bob * 0.1F;
        } else {
            leg.xRot = 1.3F + bob * 0.1F;
            tip.xRot = -0.5F - bob * 0.1F;
            foot.xRot = 0.75F + bob * 0.1F;
        }
    }
    private float[] renderSpine(
            AncientEnderDragonEntity dragon,
            float partialTick,
            boolean neck,
            PoseStack stack,
            VertexConsumer buffer,
            int light,
            int overlay,
            int color
    ) {

        ModelPart part = root.getChild("neck");

        float x = 0.0F;
        float y = neck ? 20.0F : 10.0F;
        float z = neck ? -12.0F : 60.0F;

        float anchorYaw = dragon.getFlightYaw(6, partialTick);
        double anchorY = dragon.getFlightY(6, partialTick);

        float phase = (dragon.tickCount + partialTick) * 0.28F;
        int count = neck ? 5 : 12;

        for (int i = 0; i < count; i++) {

            int delay = neck ? 5 - i : 11 + i;

            float yawOffset = Mth.wrapDegrees(
                    dragon.getFlightYaw(delay, partialTick) - anchorYaw
            );

            float heightOffset = (float) (
                    dragon.getFlightY(delay, partialTick) - anchorY
            );

            float yawBend = Mth.clamp(
                    yawOffset * 0.015F,
                    -0.5F,
                    0.5F
            ) + Mth.sin(phase + i * 0.42F) * 0.04F;

            float pitchBend = Mth.clamp(
                    heightOffset * 0.035F,
                    -0.45F,
                    0.45F
            ) + Mth.sin(phase + i * 0.35F) * 0.035F;

            part.x = x;
            part.y = y;
            part.z = z;

            part.xRot = pitchBend;
            part.yRot = neck ? yawBend : (float) Math.PI + yawBend;
            part.zRot = -yawBend * 0.45F;

            if (buffer != null) {
                part.render(stack, buffer, light, overlay, color);
            }

            float direction = neck ? -1.0F : 1.0F;

            x += direction * Mth.sin(yawBend) * 10.0F;
            y += Mth.sin(pitchBend) * 10.0F;
            z += direction
                    * Mth.cos(yawBend)
                    * Mth.cos(pitchBend)
                    * 10.0F;
        }

        // 頸部最後的位置就是頭部應在的位置
        return new float[]{x, y, z};
    }
    private void renderAnimatedMesh(
            AncientEnderDragonEntity dragon,
            float partialTick,
            PoseStack stack,
            VertexConsumer buffer,
            int light,
            int overlay,
            int color
    ) {

        ModelPart neck = root.getChild("neck");
        ModelPart head = root.getChild("head");

        // 先計算頸部末端，作為頭部的位置
        float[] headPosition = renderSpine(
                dragon, partialTick, true,
                stack, null, light, overlay, color
        );

        head.x = headPosition[0];
        head.y = headPosition[1];
        head.z = headPosition[2];

        head.yRot = Mth.clamp(
                Mth.wrapDegrees(
                        dragon.getFlightYaw(0, partialTick)
                                - dragon.getFlightYaw(6, partialTick)
                ) * Mth.DEG_TO_RAD,
                -0.7F,
                0.7F
        );

        // 先隱藏原本的單段頸尾，避免與分節模型重疊
        neck.visible = false;

        // 繪製身體、頭部、翅膀及四肢
        root.render(stack, buffer, light, overlay, color);

        neck.visible = true;

        // 分別繪製完整頸部與尾部
        renderSpine(
                dragon, partialTick, true,
                stack, buffer, light, overlay, color
        );

        renderSpine(
                dragon, partialTick, false,
                stack, buffer, light, overlay, color
        );
    }
    private static void renderRays(
            PoseStack stack,
            float progress,
            VertexConsumer consumer
    ) {
        stack.pushPose();

        float fade = Math.min(
                progress > 0.8F
                        ? (progress - 0.8F) / 0.2F
                        : 0.0F,
                1.0F
        );

        int white = FastColor.ARGB32.colorFromFloat(
                1.0F - fade,
                1.0F, 1.0F, 1.0F
        );

        RandomSource random = RandomSource.create(432L);

        Vector3f origin = new Vector3f();
        Vector3f v1 = new Vector3f();
        Vector3f v2 = new Vector3f();
        Vector3f v3 = new Vector3f();

        Quaternionf rotation = new Quaternionf();

        int count = Mth.floor(
                (progress + progress * progress)
                        / 2.0F * 60.0F
        );

        for (int i = 0; i < count; i++) {

            rotation.rotationXYZ(
                    random.nextFloat() * Mth.TWO_PI,
                    random.nextFloat() * Mth.TWO_PI,
                    random.nextFloat() * Mth.TWO_PI
            ).rotateXYZ(
                    random.nextFloat() * Mth.TWO_PI,
                    random.nextFloat() * Mth.TWO_PI,
                    random.nextFloat() * Mth.TWO_PI
                            + progress * Mth.HALF_PI
            );

            stack.mulPose(rotation);

            float length =
                    random.nextFloat() * 20.0F
                            + 5.0F + fade * 10.0F;

            float width =
                    random.nextFloat() * 2.0F
                            + 1.0F + fade * 2.0F;

            v1.set(-HALF_SQRT_3 * width, length, -0.5F * width);
            v2.set(HALF_SQRT_3 * width, length, -0.5F * width);
            v3.set(0.0F, length, width);

            PoseStack.Pose pose = stack.last();

            consumer.addVertex(pose, origin).setColor(white);
            consumer.addVertex(pose, v1).setColor(0xFF00FF);
            consumer.addVertex(pose, v2).setColor(0xFF00FF);

            consumer.addVertex(pose, origin).setColor(white);
            consumer.addVertex(pose, v2).setColor(0xFF00FF);
            consumer.addVertex(pose, v3).setColor(0xFF00FF);

            consumer.addVertex(pose, origin).setColor(white);
            consumer.addVertex(pose, v3).setColor(0xFF00FF);
            consumer.addVertex(pose, v1).setColor(0xFF00FF);
        }

        stack.popPose();
    }
    private void bend(String childName, float x, float y, float z) {
        if (root.hasChild(childName)) {
            ModelPart part = root.getChild(childName);
            part.xRot += x;
            part.yRot += y;
            part.zRot += z;
        }
    }
}
