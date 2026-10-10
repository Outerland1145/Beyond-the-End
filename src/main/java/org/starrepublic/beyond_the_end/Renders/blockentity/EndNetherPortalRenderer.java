package org.starrepublic.beyond_the_end.Renders.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import org.starrepublic.beyond_the_end.Entitys.EndNetherPortalBlockEntity;

public class EndNetherPortalRenderer
        extends TheEndPortalRenderer<EndNetherPortalBlockEntity> {

    /*
     * Portal 的實際厚度。
     *
     * Collision shape:
     *
     * 0.375 -> 0.625
     *
     * 厚度 = 0.25
     */
    private static final float PORTAL_MIN = 0.375F;
    private static final float PORTAL_MAX = 0.625F;

    /*
     * 紅色背景強度。
     *
     * 可以自行調整。
     */
    private static final int RED = 150;
    private static final int GREEN = 8;
    private static final int BLUE = 8;
    private static final int ALPHA = 135;

    public EndNetherPortalRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(context);
    }

    @Override
    public void render(
            EndNetherPortalBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Direction.Axis axis =
                blockEntity.getBlockState()
                        .getValue(BlockStateProperties.HORIZONTAL_AXIS);

        /*
         * ======================================
         * 第一層：
         * 原版 End Portal
         * ======================================
         *
         * 我們仍然保留 Minecraft 原版的：
         *
         * - 星空
         * - End Portal shader
         * - 空間深度
         *
         * 只把它壓縮到真正 Portal 的厚度。
         */

        poseStack.pushPose();

        poseStack.translate(
                0.5D,
                0.5D,
                0.5D
        );

        if (axis == Direction.Axis.X) {

            /*
             * X 軸 Portal：
             *
             * NORTH / SOUTH
             */
            poseStack.scale(
                    1.0F,
                    1.0F,
                    0.25F
            );

        } else {

            /*
             * Z 軸 Portal：
             *
             * EAST / WEST
             */
            poseStack.scale(
                    0.25F,
                    1.0F,
                    1.0F
            );
        }

        poseStack.translate(
                -0.5D,
                -0.5D,
                -0.5D
        );

        super.render(
                blockEntity,
                partialTick,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();

        /*
         * ======================================
         * 第二層：
         * 紅色能量背景
         * ======================================
         */

        renderRedPortalLayer(
                blockEntity,
                poseStack,
                bufferSource,
                axis
        );
    }

    private void renderRedPortalLayer(
            EndNetherPortalBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Direction.Axis axis
    ) {
        /*
         * lightning RenderType 使用 additive 類型效果，
         *
         * 很適合給 Portal 增加紅色能量背景，
         * 同時不會完全遮掉原本的星空。
         */
        VertexConsumer consumer =
                bufferSource.getBuffer(RenderType.lightning());

        Matrix4f matrix =
                poseStack.last().pose();

        /*
         * 稍微向外偏移。
         *
         * 避免跟 End Portal 本身 Z-fighting。
         */
        final float EPSILON = 0.001F;

        if (axis == Direction.Axis.X) {

            /*
             * Portal 沿 X 延伸：
             *
             * NORTH / SOUTH
             */

            float northZ = PORTAL_MIN - EPSILON;
            float southZ = PORTAL_MAX + EPSILON;

            renderZFace(
                    matrix,
                    consumer,
                    northZ
            );

            renderZFaceReverse(
                    matrix,
                    consumer,
                    southZ
            );

        } else {

            /*
             * Portal 沿 Z 延伸：
             *
             * WEST / EAST
             */

            float westX = PORTAL_MIN - EPSILON;
            float eastX = PORTAL_MAX + EPSILON;

            renderXFace(
                    matrix,
                    consumer,
                    westX
            );

            renderXFaceReverse(
                    matrix,
                    consumer,
                    eastX
            );
        }
    }

    /*
     * ==========================
     * NORTH / SOUTH
     * ==========================
     */

    private void renderZFace(
            Matrix4f matrix,
            VertexConsumer consumer,
            float z
    ) {
        vertex(consumer, matrix, 0F, 0F, z);
        vertex(consumer, matrix, 1F, 0F, z);
        vertex(consumer, matrix, 1F, 1F, z);
        vertex(consumer, matrix, 0F, 1F, z);
    }

    private void renderZFaceReverse(
            Matrix4f matrix,
            VertexConsumer consumer,
            float z
    ) {
        vertex(consumer, matrix, 0F, 1F, z);
        vertex(consumer, matrix, 1F, 1F, z);
        vertex(consumer, matrix, 1F, 0F, z);
        vertex(consumer, matrix, 0F, 0F, z);
    }

    /*
     * ==========================
     * WEST / EAST
     * ==========================
     */

    private void renderXFace(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x
    ) {
        vertex(consumer, matrix, x, 0F, 0F);
        vertex(consumer, matrix, x, 0F, 1F);
        vertex(consumer, matrix, x, 1F, 1F);
        vertex(consumer, matrix, x, 1F, 0F);
    }

    private void renderXFaceReverse(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x
    ) {
        vertex(consumer, matrix, x, 1F, 0F);
        vertex(consumer, matrix, x, 1F, 1F);
        vertex(consumer, matrix, x, 0F, 1F);
        vertex(consumer, matrix, x, 0F, 0F);
    }

    private void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z
    ) {
        consumer
                .addVertex(
                        matrix,
                        x,
                        y,
                        z
                )
                .setColor(
                        RED,
                        GREEN,
                        BLUE,
                        ALPHA
                );
    }
}
