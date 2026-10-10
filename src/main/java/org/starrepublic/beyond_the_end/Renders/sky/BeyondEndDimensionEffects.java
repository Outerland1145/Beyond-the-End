package org.starrepublic.beyond_the_end.Renders.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.starrepublic.beyond_the_end.network.EndSkySeedState;

import java.util.Random;

public class BeyondEndDimensionEffects extends DimensionSpecialEffects {

    /*
     * =========================================================
     * 基本設定
     * =========================================================
     */

    private static final float SKY_RADIUS = 100.0F;

    /*
     * 普通星星數量。
     *
     * 1500~2500 通常已經非常密集。
     */
    private static final int STAR_COUNT = 2000;

    /*
     * 銀河星塵。
     */
    private static final int GALAXY_STAR_COUNT = 5000;

    /*
     * 銀河較大的模糊光點。
     */
    private static final int GALAXY_CLOUD_COUNT = 1800;

    /*
     * 銀心額外光暈 / 核心雲團
     */
    private static final int GALAXY_CORE_CLOUD_COUNT = 340;

    /*
     * =========================================================
     * 世界種子衍生的天空參數
     * =========================================================
     *
     * 不直接讓所有系統共用同一個 Random。
     * 每一種天空元素都從 EndSkySeed 派生自己的 seed，
     * 因此修改星星生成演算法不會連帶改變銀河與背景。
     */
    private static final long UNSET_SKY_SEED = Long.MIN_VALUE;

    private static final long FALLBACK_SKY_SEED =
            0x42B7E1237A91C5E4L;

    private static long currentSkySeed =
            FALLBACK_SKY_SEED;

    private static final long STAR_SALT = 0x17A42C93D128EAF1L;
    private static final long GALAXY_SALT = 0x4B2F891AE735C641L;
    private static final long GALAXY_CLOUD_SALT = 0x77C519AFE243BD29L;
    private static final long GALAXY_CORE_SALT = 0x2AF41BCD9173ECA5L;
    private static final long BACKGROUND_SALT = 0x61E04A7CD359B28FL;
    private static final long LAYOUT_SALT = 0x69C3137FE21B47A1L;
    private static final long STAR_COLOR_SALT = 0x3D5A7BC912E864F1L;
    private static long starSeed =
            mixSeed(
                    FALLBACK_SKY_SEED,
                    STAR_SALT
            );

    private static long galaxySeed =
            mixSeed(
                    FALLBACK_SKY_SEED,
                    GALAXY_SALT
            );

    private static long galaxyCloudSeed =
            mixSeed(
                    FALLBACK_SKY_SEED,
                    GALAXY_CLOUD_SALT
            );

    private static long galaxyCoreSeed =
            mixSeed(
                    FALLBACK_SKY_SEED,
                    GALAXY_CORE_SALT
            );

    private static long backgroundSeed =
            mixSeed(
                    FALLBACK_SKY_SEED,
                    BACKGROUND_SALT
            );

    // 每個世界都不同的銀河姿態與銀心位置。
    private static float galaxyRotationX;
    private static float galaxyRotationZ;
    private static double galaxyCoreLongitude;

    // 每個世界都有略微不同的恆星色彩比例。
    private static float starWhiteThreshold = 0.65F;
    private static float starBlueThreshold = 0.82F;
    private static float starWarmThreshold = 0.92F;

    // 每個世界不同的深空背景。
    private static int zenithColor = argb(255, 3, 8, 24);
    private static int upperColor = argb(255, 4, 10, 30);
    private static int horizonColor = argb(255, 7, 16, 42);
    private static int nadirColor = argb(255, 4, 8, 22);

    /*
     * =========================================================
     * GPU Buffer
     * =========================================================
     */

    /*
     * GPU Buffer
     */
    private static VertexBuffer backgroundBuffer;
    private static VertexBuffer starBuffer;
    private static VertexBuffer galaxyBuffer;
    private static VertexBuffer galaxyCloudBuffer;
    private static VertexBuffer galaxyCoreBuffer;

    private static boolean initialized = false;


    public BeyondEndDimensionEffects() {

        /*
         * cloudLevel
         * hasGround
         * skyType
         * forceBrightLightmap
         * constantAmbientLight
         *
         * End 本身沒有正常天空/地面系統，
         * 所以 SkyType.NONE 即可。
         */
        super(
                Float.NaN,      // cloud height
                false,          // has ground
                SkyType.NONE,   // sky type
                true,          // force bright lightmap
                true            // constant ambient light
        );
    }
    public static void resetSky() {

        currentSkySeed =
                UNSET_SKY_SEED;

        starSeed = 0L;
        galaxySeed = 0L;
        galaxyCloudSeed = 0L;
        galaxyCoreSeed = 0L;
        backgroundSeed = 0L;

        destroyBuffers();

        initialized = false;
    }

    /*
     * =========================================================
     * 原版 DimensionSpecialEffects 必須實作的方法
     * =========================================================
     */

    @Override
    public Vec3 getBrightnessDependentFogColor(
            Vec3 fogColor,
            float brightness
    ) {

        /*
         * 讓終界霧保持非常暗，
         * 稍微帶一點深藍紫。
         */
        return new Vec3(
                fogColor.x * 0.08D,
                fogColor.y * 0.09D,
                fogColor.z * 0.13D
        );
    }


    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }


    /*
     * =========================================================
     * 天空渲染
     * =========================================================
     */

    @Override
    public boolean renderSky(
            ClientLevel level,
            int ticks,
            float partialTick,
            Matrix4f modelViewMatrix,
            Camera camera,
            Matrix4f projectionMatrix,
            boolean isFoggy,
            Runnable setupFog
    ) {
        long syncedSeed =
                EndSkySeedState.getSkySeed();

        /*
         * 如果已經收到真正世界的天空 seed，
         * 就切換到該世界的天空。
         */
        if (
                syncedSeed != EndSkySeedState.UNSET
                        &&
                        syncedSeed != currentSkySeed
        ) {

            setSkySeed(
                    syncedSeed
            );
        }

        /*
         * 如果網路 seed 還沒有抵達，
         * 先使用 fallback 天空。
         *
         * 這樣不會因為封鎖 Vanilla sky
         * 而導致整片天空消失。
         */
        if (currentSkySeed == UNSET_SKY_SEED) {

            setSkySeed(
                    FALLBACK_SKY_SEED
            );
        }
        initializeBuffers();

        /*
         * 不寫入深度。天空永遠位於所有世界物件後方。
         */
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        /*
         * 1. 深藍宇宙背景
         */
        RenderSystem.disableBlend();
        drawBuffer(
                backgroundBuffer,
                modelViewMatrix,
                projectionMatrix
        );

        /*
         * 2. 銀河霧帶
         */
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawBuffer(
                galaxyCloudBuffer,
                modelViewMatrix,
                projectionMatrix
        );

        /*
         * 3. 銀心額外光暈
         */
        drawBuffer(
                galaxyCoreBuffer,
                modelViewMatrix,
                projectionMatrix
        );

        /*
         * 4. 銀河主星帶
         */
        drawBuffer(
                galaxyBuffer,
                modelViewMatrix,
                projectionMatrix
        );

        /*
         * 5. 普通星星
         */
        drawBuffer(
                starBuffer,
                modelViewMatrix,
                projectionMatrix
        );

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        return true;
    }


    /*
     * =========================================================
     * 初始化 GPU Buffer
     * =========================================================
     */

    /**
     * 由客戶端網路處理器呼叫。
     * 傳入的建議值是伺服器以世界 seed 派生出的 EndSkySeed，
     * 而不是直接把世界 seed 暴露給客戶端。
     */
    public static void setSkySeed(long skySeed) {

        if (currentSkySeed == skySeed) {
            return;
        }

        currentSkySeed = skySeed;

        starSeed = mixSeed(skySeed, STAR_SALT);
        galaxySeed = mixSeed(skySeed, GALAXY_SALT);
        galaxyCloudSeed = mixSeed(skySeed, GALAXY_CLOUD_SALT);
        galaxyCoreSeed = mixSeed(skySeed, GALAXY_CORE_SALT);
        backgroundSeed = mixSeed(skySeed, BACKGROUND_SALT);

        createWorldLayout(mixSeed(skySeed, LAYOUT_SALT));
        createStarColorProfile(mixSeed(skySeed, STAR_COLOR_SALT));
        createBackgroundPalette(backgroundSeed);

        destroyBuffers();
        initialized = false;
    }

    private static void initializeBuffers() {

        if (initialized || currentSkySeed == UNSET_SKY_SEED) {
            return;
        }

        initialized = true;

        createBackground();
        createStars();
        createGalaxyClouds();
        createGalaxyCore();
        createGalaxy();
    }


    /*
     * =========================================================
     * 深空背景
     * =========================================================
     */

    private static void createBackground() {

        Tesselator tesselator = Tesselator.getInstance();

        BufferBuilder builder = tesselator.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        float r = SKY_RADIUS;

        /*
         * 上
         */
        quad(
                builder,
                -r, r, -r,
                -r, r, r,
                r, r, r,
                r, r, -r,
                zenithColor
        );

        /*
         * 下
         */
        quad(
                builder,
                -r, -r, r,
                -r, -r, -r,
                r, -r, -r,
                r, -r, r,
                nadirColor
        );

        /*
         * 北
         */
        quadGradient(
                builder,
                -r, -r, -r,
                -r, r, -r,
                r, r, -r,
                r, -r, -r,
                horizonColor,
                upperColor,
                upperColor,
                horizonColor
        );

        /*
         * 南
         */
        quadGradient(
                builder,
                r, -r, r,
                r, r, r,
                -r, r, r,
                -r, -r, r,
                horizonColor,
                upperColor,
                upperColor,
                horizonColor
        );

        /*
         * 西
         */
        quadGradient(
                builder,
                -r, -r, r,
                -r, r, r,
                -r, r, -r,
                -r, -r, -r,
                horizonColor,
                upperColor,
                upperColor,
                horizonColor
        );

        /*
         * 東
         */
        quadGradient(
                builder,
                r, -r, -r,
                r, r, -r,
                r, r, r,
                r, -r, r,
                horizonColor,
                upperColor,
                upperColor,
                horizonColor
        );

        backgroundBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        backgroundBuffer.bind();
        backgroundBuffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
    }


    /*
     * =========================================================
     * 普通星星
     * =========================================================
     */

    private static void createStars() {

        Random random = new Random(starSeed);

        BufferBuilder builder =
                Tesselator.getInstance().begin(
                        VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR
                );


        for (int i = 0; i < STAR_COUNT; i++) {

            Vector3f direction = randomSphereDirection(random);

            /*
             * 大部分星星很小，
             * 偶爾出現比較亮的大星。
             */

            float chance = random.nextFloat();

            float size;

            if (chance < 0.008F) {

                // 極少數亮星
                size =
                        0.32F
                                + random.nextFloat() * 0.18F;

            } else if (chance < 0.08F) {

                // 中等亮星
                size =
                        0.18F
                                + random.nextFloat() * 0.12F;

            } else {

                // 一般星星
                size =
                        0.07F
                                + random.nextFloat() * 0.09F;
            }


            int color = randomStarColor(random, chance);

            addSkyQuad(
                    builder,
                    direction,
                    SKY_RADIUS - 2.0F,
                    size,
                    color
            );
        }


        starBuffer =
                new VertexBuffer(VertexBuffer.Usage.STATIC);

        starBuffer.bind();
        starBuffer.upload(builder.buildOrThrow());

        VertexBuffer.unbind();
    }


    /*
     * =========================================================
     * 銀河
     * =========================================================
     */

    private static void createGalaxy() {

        Random random = new Random(galaxySeed);

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        for (int i = 0; i < GALAXY_STAR_COUNT; i++) {

            double longitude = random.nextDouble() * Math.PI * 2.0D;

            /*
             * 與銀心的角距離
             */
            double coreDistance = angularDistance(longitude, galaxyCoreLongitude);

            /*
             * 越接近銀心，值越高
             */
            float coreFactor = (float) Math.exp(
                    -(coreDistance * coreDistance) / (2.0D * 0.36D * 0.36D)
            );

            /*
             * 銀河主盤面比較薄，
             * 靠近銀心時變厚，形成鼓起感。
             */
            double bandThickness = 0.045D + coreFactor * 0.11D;
            double latitude = random.nextGaussian() * bandThickness;

            Vector3f direction = sphericalDirection(longitude, latitude);

            /*
             * 銀河傾斜角
             */
            direction.rotateX(galaxyRotationX);
            direction.rotateZ(galaxyRotationZ);

            /*
             * 距離銀河盤面越遠越暗
             */
            float planeFactor = 1.0F - Math.min(1.0F, (float) Math.abs(latitude) / 0.22F);

            /*
             * 核心區額外加亮
             */
            float brightness = 0.45F + planeFactor * 0.45F + coreFactor * 0.85F;
            brightness = Math.min(brightness, 1.55F);

            int alpha = 40 + (int) (brightness * 125.0F);

            /*
             * 靠近銀心時，星點更大、更密、更偏暖白
             */
            float size = 0.05F
                    + random.nextFloat() * 0.12F
                    + coreFactor * (0.06F + random.nextFloat() * 0.16F);

            if (random.nextFloat() < (0.012F + coreFactor * 0.05F)) {
                size *= 1.35F + random.nextFloat() * 0.45F;
            }

            int color;
            float selector = random.nextFloat();

            if (coreFactor > 0.45F) {
                /*
                 * 銀心區域偏暖白、金白
                 */
                if (selector < 0.45F) {
                    color = argb(alpha, 255, 232, 190);
                } else if (selector < 0.80F) {
                    color = argb(alpha, 255, 245, 220);
                } else {
                    color = argb(alpha, 220, 225, 255);
                }
            } else {
                /*
                 * 一般銀河盤面偏藍白、紫白
                 */
                if (selector < 0.58F) {
                    color = argb(alpha, 200, 215, 255);
                } else if (selector < 0.84F) {
                    color = argb(alpha, 220, 200, 255);
                } else {
                    color = argb(alpha, 255, 228, 200);
                }
            }

            addSkyQuad(
                    builder,
                    direction,
                    SKY_RADIUS - 3.0F,
                    size,
                    color
            );
        }

        galaxyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        galaxyBuffer.bind();
        galaxyBuffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
    }


    /*
     * =========================================================
     * 銀河霧狀部分
     * =========================================================
     */

    private static void createGalaxyClouds() {

        Random random = new Random(galaxyCloudSeed);

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        for (int i = 0; i < GALAXY_CLOUD_COUNT; i++) {

            double longitude = random.nextDouble() * Math.PI * 2.0D;
            double coreDistance = angularDistance(longitude, galaxyCoreLongitude);

            float coreFactor = (float) Math.exp(
                    -(coreDistance * coreDistance) / (2.0D * 0.42D * 0.42D)
            );

            /*
             * 雲帶比星帶略寬
             */
            double latitude = random.nextGaussian() * (0.07D + coreFactor * 0.10D);

            Vector3f direction = sphericalDirection(longitude, latitude);

            direction.rotateX(galaxyRotationX);
            direction.rotateZ(galaxyRotationZ);

            /*
             * 靠近銀心時雲團更大
             */
            float size = 0.7F
                    + random.nextFloat() * 1.5F
                    + coreFactor * (0.8F + random.nextFloat() * 1.6F);

            int alpha = 8 + random.nextInt(8) + (int) (coreFactor * 18.0F);

            int color;
            float value = random.nextFloat();

            if (coreFactor > 0.45F) {
                if (value < 0.50F) {
                    color = argb(alpha, 210, 185, 130);
                } else if (value < 0.80F) {
                    color = argb(alpha, 180, 160, 210);
                } else {
                    color = argb(alpha, 145, 165, 235);
                }
            } else {
                if (value < 0.45F) {
                    color = argb(alpha, 90, 120, 220);
                } else if (value < 0.80F) {
                    color = argb(alpha, 125, 85, 200);
                } else {
                    color = argb(alpha, 160, 145, 215);
                }
            }

            addSkyQuad(
                    builder,
                    direction,
                    SKY_RADIUS - 4.0F,
                    size,
                    color
            );
        }

        galaxyCloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        galaxyCloudBuffer.bind();
        galaxyCloudBuffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
    }
    private static void createGalaxyCore() {

        Random random = new Random(galaxyCoreSeed);

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        for (int i = 0; i < GALAXY_CORE_CLOUD_COUNT; i++) {

            /*
             * 只在銀心附近生成
             */
            double longitude = galaxyCoreLongitude + random.nextGaussian() * 0.18D;
            double latitude = random.nextGaussian() * 0.10D;

            Vector3f direction = sphericalDirection(longitude, latitude);

            direction.rotateX(galaxyRotationX);
            direction.rotateZ(galaxyRotationZ);

            /*
             * 大型柔光團
             */
            float size = 1.4F + random.nextFloat() * 3.8F;

            int alpha = 12 + random.nextInt(22);

            int color;
            float selector = random.nextFloat();

            if (selector < 0.42F) {
                color = argb(alpha, 255, 225, 170);
            } else if (selector < 0.75F) {
                color = argb(alpha, 255, 243, 220);
            } else {
                color = argb(alpha, 175, 190, 255);
            }

            addSkyQuad(
                    builder,
                    direction,
                    SKY_RADIUS - 5.0F,
                    size,
                    color
            );
        }

        galaxyCoreBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        galaxyCoreBuffer.bind();
        galaxyCoreBuffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
    }
    /**
     * 建立這個世界專屬的銀河構圖。
     *
     * galaxyRotationX：決定銀河斜切天空的角度。
     * galaxyRotationZ：給不同世界額外的平面旋轉。
     * galaxyCoreLongitude：不是任意取值，而是挑選一個旋轉後
     * Y 足夠高的位置，確保銀心出現在天空而不是玩家腳下的虛空。
     */
    private static void createWorldLayout(long layoutSeed) {

        Random random = new Random(layoutSeed);

        galaxyRotationX = (float) Math.toRadians(
                38.0D + random.nextDouble() * 27.0D
        );

        galaxyRotationZ = (float) Math.toRadians(
                -18.0D + random.nextDouble() * 36.0D
        );

        galaxyCoreLongitude = chooseSkyCoreLongitude(random);
    }

    /**
     * 嘗試找出一個旋轉後位於上方天空的銀心位置。
     * y >= 0.48 代表銀心至少明顯高於地平線。
     */
    private static double chooseSkyCoreLongitude(Random random) {

        double bestLongitude = -Math.PI / 2.0D;
        float bestY = -1.0F;

        for (int i = 0; i < 48; i++) {
            double longitude = random.nextDouble() * Math.PI * 2.0D;

            Vector3f direction = sphericalDirection(longitude, 0.0D);
            direction.rotateX(galaxyRotationX);
            direction.rotateZ(galaxyRotationZ);

            if (direction.y > bestY) {
                bestY = direction.y;
                bestLongitude = longitude;
            }

            if (direction.y >= 0.48F) {
                return longitude;
            }
        }

        return bestLongitude;
    }

    /**
     * 不同世界的恆星光譜比例不同，但差異控制在合理範圍，
     * 避免某些世界幾乎只剩單一顏色。
     */
    private static void createStarColorProfile(long colorSeed) {

        Random random = new Random(colorSeed);

        float whiteWeight = 0.54F + random.nextFloat() * 0.16F;
        float blueWeight = 0.13F + random.nextFloat() * 0.09F;
        float warmWeight = 0.08F + random.nextFloat() * 0.07F;

        starWhiteThreshold = whiteWeight;
        starBlueThreshold = Math.min(0.90F, starWhiteThreshold + blueWeight);
        starWarmThreshold = Math.min(0.97F, starBlueThreshold + warmWeight);
    }

    /**
     * 產生世界專屬的深藍背景。
     * 所有結果仍維持「深藍宇宙」主題，只在藍度、青度與地平線亮度上變化。
     */
    private static void createBackgroundPalette(long seed) {

        Random random = new Random(seed);

        int blueShift = random.nextInt(9);
        int greenShift = random.nextInt(5);
        int redShift = random.nextInt(3);
        int horizonBoost = 8 + random.nextInt(11);

        zenithColor = argb(
                255,
                2 + redShift,
                7 + greenShift,
                22 + blueShift
        );

        upperColor = argb(
                255,
                3 + redShift,
                9 + greenShift,
                28 + blueShift
        );

        horizonColor = argb(
                255,
                5 + redShift,
                13 + greenShift,
                34 + blueShift + horizonBoost
        );

        nadirColor = argb(
                255,
                2 + redShift,
                6 + greenShift,
                18 + blueShift
        );
    }

    /**
     * SplitMix64 finalizer。相近的 world seed 也會得到完全不同的天空 seed。
     */
    public static long mixSeed(long seed, long salt) {

        long z = seed + salt;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * 世界切換時釋放上一個世界的 GPU buffer。
     */
    private static void destroyBuffers() {

        if (backgroundBuffer != null) {
            backgroundBuffer.close();
            backgroundBuffer = null;
        }

        if (starBuffer != null) {
            starBuffer.close();
            starBuffer = null;
        }

        if (galaxyBuffer != null) {
            galaxyBuffer.close();
            galaxyBuffer = null;
        }

        if (galaxyCloudBuffer != null) {
            galaxyCloudBuffer.close();
            galaxyCloudBuffer = null;
        }

        if (galaxyCoreBuffer != null) {
            galaxyCoreBuffer.close();
            galaxyCoreBuffer = null;
        }
    }

    private static double angularDistance(double a, double b) {

        double d = Math.abs(a - b) % (Math.PI * 2.0D);

        if (d > Math.PI) {
            d = Math.PI * 2.0D - d;
        }

        return d;
    }


    /*
     * =========================================================
     * 星空 Quad
     * =========================================================
     */

    private static void addSkyQuad(
            BufferBuilder builder,
            Vector3f direction,
            float radius,
            float size,
            int color
    ) {

        Vector3f center =
                new Vector3f(direction)
                        .normalize()
                        .mul(radius);


        /*
         * 建立與球面相切的兩條軸。
         */

        Vector3f reference;

        if (
                Math.abs(direction.y)
                        > 0.95F
        ) {

            reference =
                    new Vector3f(
                            1.0F,
                            0.0F,
                            0.0F
                    );

        } else {

            reference =
                    new Vector3f(
                            0.0F,
                            1.0F,
                            0.0F
                    );
        }


        Vector3f right =
                new Vector3f(reference)
                        .cross(direction)
                        .normalize()
                        .mul(size);


        Vector3f up =
                new Vector3f(direction)
                        .cross(right)
                        .normalize()
                        .mul(size);


        Vector3f p1 =
                new Vector3f(center)
                        .sub(right)
                        .sub(up);

        Vector3f p2 =
                new Vector3f(center)
                        .add(right)
                        .sub(up);

        Vector3f p3 =
                new Vector3f(center)
                        .add(right)
                        .add(up);

        Vector3f p4 =
                new Vector3f(center)
                        .sub(right)
                        .add(up);


        builder
                .addVertex(
                        p1.x,
                        p1.y,
                        p1.z
                )
                .setColor(color);


        builder
                .addVertex(
                        p2.x,
                        p2.y,
                        p2.z
                )
                .setColor(color);


        builder
                .addVertex(
                        p3.x,
                        p3.y,
                        p3.z
                )
                .setColor(color);


        builder
                .addVertex(
                        p4.x,
                        p4.y,
                        p4.z
                )
                .setColor(color);
    }


    /*
     * =========================================================
     * 星星方向
     * =========================================================
     */

    private static Vector3f randomSphereDirection(
            Random random
    ) {

        /*
         * 均勻球面取樣。
         */
        double y =
                random.nextDouble()
                        * 2.0D
                        - 1.0D;

        double angle =
                random.nextDouble()
                        * Math.PI
                        * 2.0D;

        double radius =
                Math.sqrt(
                        1.0D - y * y
                );


        return new Vector3f(
                (float) (
                        Math.cos(angle)
                                * radius
                ),

                (float) y,

                (float) (
                        Math.sin(angle)
                                * radius
                )
        );
    }


    private static Vector3f sphericalDirection(
            double longitude,
            double latitude
    ) {

        double cosLat =
                Math.cos(latitude);


        return new Vector3f(

                (float) (
                        Math.cos(longitude)
                                * cosLat
                ),

                (float)
                        Math.sin(latitude),

                (float) (
                        Math.sin(longitude)
                                * cosLat
                )
        );
    }


    /*
     * =========================================================
     * 星星顏色
     * =========================================================
     */

    private static int randomStarColor(
            Random random,
            float brightnessClass
    ) {

        int brightness;

        if (brightnessClass < 0.015F) {

            brightness =
                    245
                            + random.nextInt(11);

        } else {

            brightness =
                    185
                            + random.nextInt(71);
        }


        float selector =
                random.nextFloat();


        /*
         * 約：
         *
         * 65% 白色
         * 17% 藍白
         * 10% 暖白
         * 8% 紫白
         */

        if (selector < starWhiteThreshold) {

            return argb(
                    255,
                    brightness,
                    brightness,
                    brightness
            );

        } else if (selector < starBlueThreshold) {

            return argb(
                    255,
                    Math.max(
                            0,
                            brightness - 35
                    ),
                    Math.max(
                            0,
                            brightness - 15
                    ),
                    brightness
            );

        } else if (selector < starWarmThreshold) {

            return argb(
                    255,
                    brightness,
                    Math.max(
                            0,
                            brightness - 20
                    ),
                    Math.max(
                            0,
                            brightness - 45
                    )
            );

        } else {

            return argb(
                    255,
                    brightness,
                    Math.max(
                            0,
                            brightness - 40
                    ),
                    brightness
            );
        }
    }


    /*
     * =========================================================
     * Buffer Drawing
     * =========================================================
     */

    private static void drawBuffer(
            VertexBuffer buffer,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix
    ) {

        if (
                buffer == null
                        || buffer.isInvalid()
        ) {
            return;
        }


        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );


        ShaderInstance shader =
                RenderSystem.getShader();


        if (shader == null) {
            return;
        }


        buffer.bind();

        buffer.drawWithShader(
                modelViewMatrix,
                projectionMatrix,
                shader
        );

        VertexBuffer.unbind();
    }


    /*
     * =========================================================
     * Quad helpers
     * =========================================================
     */

    private static void quad(
            BufferBuilder builder,

            float x1,
            float y1,
            float z1,

            float x2,
            float y2,
            float z2,

            float x3,
            float y3,
            float z3,

            float x4,
            float y4,
            float z4,

            int color
    ) {

        builder
                .addVertex(
                        x1,
                        y1,
                        z1
                )
                .setColor(color);

        builder
                .addVertex(
                        x2,
                        y2,
                        z2
                )
                .setColor(color);

        builder
                .addVertex(
                        x3,
                        y3,
                        z3
                )
                .setColor(color);

        builder
                .addVertex(
                        x4,
                        y4,
                        z4
                )
                .setColor(color);
    }


    private static void quadGradient(
            BufferBuilder builder,

            float x1,
            float y1,
            float z1,

            float x2,
            float y2,
            float z2,

            float x3,
            float y3,
            float z3,

            float x4,
            float y4,
            float z4,

            int color1,
            int color2,
            int color3,
            int color4
    ) {

        builder
                .addVertex(
                        x1,
                        y1,
                        z1
                )
                .setColor(color1);

        builder
                .addVertex(
                        x2,
                        y2,
                        z2
                )
                .setColor(color2);

        builder
                .addVertex(
                        x3,
                        y3,
                        z3
                )
                .setColor(color3);

        builder
                .addVertex(
                        x4,
                        y4,
                        z4
                )
                .setColor(color4);
    }


    /*
     * =========================================================
     * ARGB
     * =========================================================
     */

    private static int argb(
            int alpha,
            int red,
            int green,
            int blue
    ) {

        return
                ((alpha & 0xFF) << 24)
                        |
                        ((red & 0xFF) << 16)
                        |
                        ((green & 0xFF) << 8)
                        |
                        (blue & 0xFF);
    }
}
