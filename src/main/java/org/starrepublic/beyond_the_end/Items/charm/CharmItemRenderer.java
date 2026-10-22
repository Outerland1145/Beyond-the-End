package org.starrepublic.beyond_the_end.Items.charm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.starrepublic.beyond_the_end.Beyond_the_end;

import java.util.Optional;

public class CharmItemRenderer
        extends BlockEntityWithoutLevelRenderer {

    /**
     * 護符灰階背景。
     *
     * 對應：
     * assets/beyond_the_end/textures/item/charm.png
     */
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "textures/item/charm/charm.png"
            );

    /**
     * 背景缺失時使用的原版備用材質。
     */
    private static final ResourceLocation FALLBACK_BACKGROUND =
            ResourceLocation.withDefaultNamespace(
                    "textures/block/white_concrete.png"
            );

    /**
     * 無效護符的備用前景。
     *
     * 如果此材質不存在，就不繪製前景。
     */
    private static final ResourceLocation UNKNOWN_ICON =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "textures/item/charm/unknown.png"
            );

    private static final int DEFAULT_COLOR = 0xAAAAAA;
    int color = DEFAULT_COLOR;
    ResourceLocation icon = null;
    public CharmItemRenderer() {
        super(
                Minecraft.getInstance()
                        .getBlockEntityRenderDispatcher(),

                Minecraft.getInstance()
                        .getEntityModels()
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {

        int color = DEFAULT_COLOR;

        // null 表示沒有可以繪製的前景圖示。
        ResourceLocation icon = null;

        Optional<CharmData> optionalData =
                CharmHelper.getData(stack);

        if (optionalData.isPresent()) {

            CharmData data = optionalData.get();

            HolderLookup.Provider registries =
                    getClientRegistries();

            Optional<CharmDefinition> definition =
                    CharmTypes.find(
                            data.type(),
                            registries
                    );

            if (definition.isPresent()) {

                CharmDefinition def = definition.get();

                if (data.level() >= 1
                        && data.level() <= def.maxLevel()) {

                    color = def.backgroundColor();

                    ResourceLocation requestedIcon =
                            toTextureLocation(def.icon());

                    // 只有材質存在時才使用。
                    if (textureExists(requestedIcon)) {
                        icon = requestedIcon;
                    }
                }
            }
        }

        /**
         * 沒有有效前景時：
         * 使用備用圖示；如果備用圖示也不存在，
         * 就只繪製背景。
         */
        if (icon == null && textureExists(UNKNOWN_ICON)) {
            icon = UNKNOWN_ICON;
        }

        /**
         * 檢查背景材質。
         */
        ResourceLocation background =
                textureExists(BACKGROUND)
                        ? BACKGROUND
                        : FALLBACK_BACKGROUND;

        poseStack.pushPose();

        /**
         * 第一層：RGB 背景。
         */
        renderLayer(
                poseStack,
                buffer,
                background,
                color,
                0.501F,
                packedLight,
                packedOverlay
        );

        /**
         * 第二層：前景圖示。
         *
         * 只有取得有效材質才繪製。
         */
        if (icon != null) {

            renderLayer(
                    poseStack,
                    buffer,
                    icon,
                    0xFFFFFF,
                    0.504F,
                    packedLight,
                    packedOverlay
            );
        }

        poseStack.popPose();
    }

    /**
     * 檢查客戶端資源管理器內是否有指定材質。
     *
     * 避免將不存在的 PNG 交給 RenderType，
     * 造成紫黑缺失材質。
     */
    private static boolean textureExists(
            ResourceLocation texture
    ) {

        return Minecraft.getInstance()
                .getResourceManager()
                .getResource(texture)
                .isPresent();
    }

    /**
     * 取得客戶端目前的資料包註冊表。
     *
     * 沒有世界時返回 null，
     * 仍可讀取 Java 內建護符。
     */
    private static HolderLookup.Provider getClientRegistries() {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }

        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }

        return null;
    }

    /**
     * 將材質 ID 轉換為完整 PNG 路徑。
     *
     * examplemod:item/charm/icons/time
     *
     * 轉成：
     *
     * examplemod:textures/item/charm/icons/time.png
     */
    private static ResourceLocation toTextureLocation(
            ResourceLocation id
    ) {

        return ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(),
                "textures/" + id.getPath() + ".png"
        );
    }

    /**
     * 繪製指定材質層。
     */
    private static void renderLayer(
            PoseStack poseStack,
            MultiBufferSource buffer,
            ResourceLocation texture,
            int rgb,
            float z,
            int packedLight,
            int packedOverlay
    ) {

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(texture)
        );

        PoseStack.Pose pose = poseStack.last();

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        // 左下
        vertex(
                consumer, pose,
                0F, 0F, z,
                0F, 1F,
                red, green, blue,
                packedLight, packedOverlay
        );

        // 右下
        vertex(
                consumer, pose,
                1F, 0F, z,
                1F, 1F,
                red, green, blue,
                packedLight, packedOverlay
        );

        // 右上
        vertex(
                consumer, pose,
                1F, 1F, z,
                1F, 0F,
                red, green, blue,
                packedLight, packedOverlay
        );

        // 左上
        vertex(
                consumer, pose,
                0F, 1F, z,
                0F, 0F,
                red, green, blue,
                packedLight, packedOverlay
        );
    }

    /**
     * 單一頂點繪製。
     */
    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int packedLight,
            int packedOverlay
    ) {

        consumer.addVertex(
                        pose.pose(),
                        x, y, z
                )
                .setColor(
                        red,
                        green,
                        blue,
                        255
                )
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(
                        pose,
                        0F, 0F, 1F
                );
    }
}
