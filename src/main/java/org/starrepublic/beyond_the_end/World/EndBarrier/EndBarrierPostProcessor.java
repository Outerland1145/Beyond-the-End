package org.starrepublic.beyond_the_end.World.EndBarrier;

import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.starrepublic.beyond_the_end.Beyond_the_end;

import java.io.IOException;

public final class EndBarrierPostProcessor {
    private EndBarrierPostProcessor() {
    }

    private static final ResourceLocation EFFECT =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "shaders/post/end_barrier.json"
            );

    private static PostChain postChain;

    private static int width = -1;
    private static int height = -1;

    public static void render(
            float partialTick
    ) {

        float distortion =
                ClientBarrierState
                        .getDistortion();

        float whiteout =
                ClientBarrierState
                        .getWhiteout();

        /*
         * 完全沒有效果時連 PostChain 都不跑。
         */
        if (distortion <= 0.001F
                && whiteout <= 0.001F) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        ensureLoaded(minecraft);

        if (postChain == null) {
            return;
        }

        resizeIfNecessary(
                minecraft
        );

        postChain.setUniform(
                "Distortion",
                distortion
        );

        postChain.setUniform(
                "Whiteout",
                whiteout
        );

        postChain.process(
                partialTick
        );
    }

    private static void ensureLoaded(
            Minecraft minecraft
    ) {

        if (postChain != null) {
            return;
        }

        try {

            postChain =
                    new PostChain(
                            minecraft.getTextureManager(),
                            minecraft.getResourceManager(),
                            minecraft.getMainRenderTarget(),
                            EFFECT
                    );

            width =
                    minecraft
                            .getWindow()
                            .getWidth();

            height =
                    minecraft
                            .getWindow()
                            .getHeight();

            postChain.resize(
                    width,
                    height
            );

        } catch (
                IOException |
                JsonSyntaxException exception
        ) {

            exception.printStackTrace();

            postChain = null;
        }
    }

    private static void resizeIfNecessary(
            Minecraft minecraft
    ) {

        int newWidth =
                minecraft
                        .getWindow()
                        .getWidth();

        int newHeight =
                minecraft
                        .getWindow()
                        .getHeight();

        if (newWidth == width
                && newHeight == height) {

            return;
        }

        width = newWidth;
        height = newHeight;

        postChain.resize(
                width,
                height
        );
    }

    public static void reload() {

        if (postChain != null) {

            postChain.close();
            postChain = null;
        }

        width = -1;
        height = -1;
    }
}
