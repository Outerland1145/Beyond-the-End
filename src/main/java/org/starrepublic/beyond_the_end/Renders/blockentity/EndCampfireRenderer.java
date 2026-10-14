package org.starrepublic.beyond_the_end.Renders.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EndCampfireBlockEntity;

public class EndCampfireRenderer
        implements BlockEntityRenderer<
                EndCampfireBlockEntity
                > {

    private final ItemRenderer itemRenderer;


    public EndCampfireRenderer(
            BlockEntityRendererProvider.Context context
    ) {

        this.itemRenderer =
                context.getItemRenderer();
    }


    @Override
    public void render(
            EndCampfireBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {

        Direction direction =
                blockEntity
                        .getBlockState()
                        .getValue(
                                CampfireBlock.FACING
                        );


        for (
                int slot = 0;
                slot < blockEntity
                        .getItems()
                        .size();
                slot++
        ) {

            ItemStack stack =
                    blockEntity
                            .getItems()
                            .get(slot);


            if (stack.isEmpty()) {
                continue;
            }


            poseStack.pushPose();


            /*
             * 放到營火表面。
             */
            poseStack.translate(
                    0.5F,
                    0.44921875F,
                    0.5F
            );


            Direction itemDirection =
                    Direction.from2DDataValue(
                            (
                                    slot
                                            +
                                            direction
                                                    .get2DDataValue()
                            )
                                    %
                                    4
                    );


            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            -itemDirection.toYRot()
                    )
            );


            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            90.0F
                    )
            );


            poseStack.translate(
                    -0.3125F,
                    -0.3125F,
                    0.0F
            );


            poseStack.scale(
                    0.375F,
                    0.375F,
                    0.375F
            );


            int seed =
                    (int)
                            blockEntity
                                    .getBlockPos()
                                    .asLong()
                            +
                            slot;


            this.itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffer,
                    blockEntity.getLevel(),
                    seed
            );


            poseStack.popPose();
        }
    }
}
