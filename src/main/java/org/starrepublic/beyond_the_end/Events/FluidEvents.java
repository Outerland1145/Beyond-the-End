package org.starrepublic.beyond_the_end.Events;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Register.FluidRegister;

public final class FluidEvents {
    private static final ResourceLocation ENDER_GOO_STILL =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "block/endergoo_still"
            );

    private static final ResourceLocation ENDER_GOO_FLOW =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "block/endergoo_flow"
            );

    private FluidEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FluidEvents::registerClientExtensions);
    }

    private static void registerClientExtensions(
            RegisterClientExtensionsEvent event
    ) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {

                    @Override
                    public ResourceLocation getStillTexture() {
                        return ENDER_GOO_STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return ENDER_GOO_FLOW;
                    }

                    @Override
                    public int getTintColor() {

                        /*
                         * 不額外染色，
                         * 完全使用 PNG 本身的顏色。
                         */
                        return 0xFFFFFFFF;
                    }


                    /*
                     * ========================================
                     * Fog color
                     * ========================================
                     *
                     * 注意：
                     *
                     * NeoForge 1.21.1 這裡回傳的是：
                     *
                     * Vector3f
                     *
                     * 不是：
                     *
                     * void
                     * Vector4f
                     *
                     * 後兩種是其他版本 API。
                     */
                    @Override
                    public Vector3f modifyFogColor(
                            Camera camera,
                            float partialTick,
                            ClientLevel level,
                            int renderDistance,
                            float darkenWorldAmount,
                            Vector3f fluidFogColor
                    ) {

                        /*
                         * 紫色 Ender Goo 濃霧。
                         *
                         * RGB 全部是 0~1。
                         */
                        return new Vector3f(
                                0.20F,
                                0.025F,
                                0.28F
                        );
                    }


                    /*
                     * ========================================
                     * Fog distance
                     * ========================================
                     */
                    @Override
                    public void modifyFogRender(
                            Camera camera,
                            FogRenderer.FogMode mode,
                            float renderDistance,
                            float partialTick,
                            float nearDistance,
                            float farDistance,
                            FogShape shape
                    ) {

                        /*
                         * 模仿岩漿內極短的視距。
                         *
                         * 玩家眼睛進入 Ender Goo 後，
                         * 大約只能清楚看到非常近的地方。
                         */
                        RenderSystem.setShaderFogStart(
                                0.25F
                        );

                        RenderSystem.setShaderFogEnd(
                                1.0F
                        );

                        RenderSystem.setShaderFogShape(
                                FogShape.SPHERE
                        );
                    }
                },

                FluidRegister.ENDER_GOO_TYPE.get()
        );
    }
}
