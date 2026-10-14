package org.starrepublic.beyond_the_end.Renders.blocks.blockitems;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.starrepublic.beyond_the_end.Entitys.blockentity.DecorativeEndGatewayBlockEntity;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class DecorativeEndGatewayItemRenderer
        extends BlockEntityWithoutLevelRenderer {

    private final BlockEntityRenderDispatcher dispatcher;

    /*
     * 這個 BlockEntity 不會真正加入世界。
     *
     * 它只存在於 Client，
     * 讓現有 DecorativeEndGatewayRenderer
     * 有一個可以渲染的目標。
     */
    private final DecorativeEndGatewayBlockEntity blockEntity;

    public DecorativeEndGatewayItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );

        this.dispatcher =
                Minecraft.getInstance().getBlockEntityRenderDispatcher();

        this.blockEntity =
                new DecorativeEndGatewayBlockEntity(
                        BlockPos.ZERO,
                        BlockRegister.DECORATIVE_END_GATEWAY
                                .get()
                                .defaultBlockState()
                );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {

        poseStack.pushPose();

        /*
         * 直接要求 Minecraft：
         *
         * 「把這個 DecorativeEndGatewayBlockEntity
         * 當成物品畫出來。」
         *
         * Dispatcher 會找到我們之前註冊的
         * DecorativeEndGatewayRenderer。
         */
        dispatcher.renderItem(
                blockEntity,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }
}
