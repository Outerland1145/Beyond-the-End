package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;

@Mixin(
        targets =
                "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction"
)
public abstract class EndIslandDensityFunctionMixin {

    /*
     * ============================================================
     * Cell 設定
     * ============================================================
     *
     * 每一格最多生成一座主要碎島。
     *
     * Cell 越大：
     * 島與島平均距離越遠。
     */
    @Unique
    private static final int
            beyondTheEnd$CELL_SIZE = 112;

    /*
     * 島中心最多可以偏離 Cell 中心多少格。
     *
     * 注意不要設太大。
     *
     * 我們故意限制它，
     * 讓兩個相鄰 Cell 的島不容易連在一起。
     */
    @Unique
    private static final double
            beyondTheEnd$CENTER_JITTER = 22.0D;

    /*
     * 自訂 Noise。
     *
     * 這些 Noise 不再決定大陸。
     *
     * 只負責：
     *
     * - 島邊緣
     * - 不規則輪廓
     * - 小型凹凸
     */
    @Unique
    private SimplexNoise
            beyondTheEnd$shapeNoise;

    @Unique
    private SimplexNoise
            beyondTheEnd$detailNoise;

    @Unique
    private SimplexNoise
            beyondTheEnd$warpNoiseX;

    @Unique
    private SimplexNoise
            beyondTheEnd$warpNoiseZ;

    /*
     * 保存世界 seed。
     *
     * Cell 的位置、尺寸、旋轉都由它決定。
     */
    @Unique
    private long beyondTheEnd$seed;

    /*
     * ============================================================
     * 初始化
     * ============================================================
     */

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void beyondTheEnd$init(
            long seed,
            CallbackInfo ci
    ) {

        this.beyondTheEnd$seed =
                seed;

        this.beyondTheEnd$shapeNoise =
                new SimplexNoise(
                        new LegacyRandomSource(
                                seed
                                        ^
                                        0x74FA28C9135B610DL
                        )
                );

        this.beyondTheEnd$detailNoise =
                new SimplexNoise(
                        new LegacyRandomSource(
                                seed
                                        ^
                                        0x31C6ABF728954DE1L
                        )
                );

        this.beyondTheEnd$warpNoiseX =
                new SimplexNoise(
                        new LegacyRandomSource(
                                seed
                                        ^
                                        0x17D84F3C92AB5601L
                        )
                );

        this.beyondTheEnd$warpNoiseZ =
                new SimplexNoise(
                        new LegacyRandomSource(
                                seed
                                        ^
                                        0x69B21D83ACF74015L
                        )
                );
    }

    /*
     * ============================================================
     * Density 主入口
     * ============================================================
     */

    @Inject(
            method = "compute",
            at = @At("RETURN"),
            cancellable = true
    )
    private void beyondTheEnd$modifyEndDensity(
            DensityFunction.FunctionContext context,
            CallbackInfoReturnable<Double> cir
    ) {

        int x =
                context.blockX();

        int z =
                context.blockZ();

        long distanceSq =
                (long) x * x
                        +
                        (long) z * z;

        /*
         * ========================================
         * 0 ~ 10000
         *
         * 完全原版。
         * ========================================
         */

        if (
                distanceSq
                        <=
                        EndTerrainController.NORMAL_RADIUS_SQR
        ) {

            return;
        }

        double distance =
                Math.sqrt(
                        (double) distanceSq
                );

        /*
         * ========================================
         * 20000+
         *
         * 完全虛空。
         * ========================================
         */

        if (
                distance
                        >=
                        EndTerrainController.VOID_RADIUS
        ) {

            cir.setReturnValue(
                    EndTerrainController.VOID_DENSITY
            );

            return;
        }

        double vanillaDensity =
                cir.getReturnValue();

        /*
         * 產生真正獨立的碎島。
         */
        double fracturedDensity =
                beyondTheEnd$generateCellularIslands(
                        x,
                        z,
                        distance
                );

        /*
         * ========================================
         * 10000 ~ 13000
         *
         * 原版逐步退出。
         * ========================================
         */

        if (
                distance
                        <
                        EndTerrainController
                                .FULL_FRACTURED_RADIUS
        ) {

            double blend =
                    EndTerrainController
                            .getFracturedTransition(
                                    distance
                            );

            cir.setReturnValue(
                    EndTerrainController.lerp(
                            blend,
                            vanillaDensity,
                            fracturedDensity
                    )
            );

            return;
        }

        /*
         * ========================================
         * 13000+
         *
         * 完全禁止使用 vanillaDensity。
         * ========================================
         */

        cir.setReturnValue(
                fracturedDensity
        );
    }

    /*
     * ============================================================
     * Cellular Fractured Islands
     * ============================================================
     */

    @Unique
    private double beyondTheEnd$generateCellularIslands(
            int x,
            int z,
            double distance
    ) {

        /*
         * 13000 → 20000
         *
         * 0 → 1
         */
        double progress =
                EndTerrainController
                        .getDeepFracturedProgress(
                                distance
                        );

        /*
         * ========================================
         * 非常小幅的 Domain Warp
         * ========================================
         *
         * 注意：
         *
         * 這次 Warp 不可以像上一版 200 多格。
         *
         * 否則島中心會互相拉扯成長條。
         *
         * 現在只用約 7 格來破壞規則感。
         */

        double warpX =
                beyondTheEnd$warpNoiseX
                        .getValue(
                                x * 0.007D,
                                z * 0.007D
                        )
                        *
                        7.0D;

        double warpZ =
                beyondTheEnd$warpNoiseZ
                        .getValue(
                                x * 0.007D,
                                z * 0.007D
                        )
                        *
                        7.0D;

        double sampleX =
                x + warpX;

        double sampleZ =
                z + warpZ;

        /*
         * 找到目前所處 Cell。
         */
        int cellX =
                floorDiv(
                        (int) Math.floor(sampleX),
                        beyondTheEnd$CELL_SIZE
                );

        int cellZ =
                floorDiv(
                        (int) Math.floor(sampleZ),
                        beyondTheEnd$CELL_SIZE
                );

        /*
         * 預設完全虛空。
         *
         * 之後尋找附近 Cell 的島。
         */
        double bestDensity =
                EndTerrainController.VOID_DENSITY;

        /*
         * 因為島半徑明顯小於 CELL_SIZE，
         * 搜尋周圍 3x3 Cells 已足夠。
         */
        for (
                int offsetX = -1;
                offsetX <= 1;
                offsetX++
        ) {

            for (
                    int offsetZ = -1;
                    offsetZ <= 1;
                    offsetZ++
            ) {

                int currentCellX =
                        cellX + offsetX;

                int currentCellZ =
                        cellZ + offsetZ;

                /*
                 * 決定這個 Cell 是否生成島。
                 */
                if (
                        !beyondTheEnd$cellHasIsland(
                                currentCellX,
                                currentCellZ,
                                progress
                        )
                ) {

                    continue;
                }

                double islandDensity =
                        beyondTheEnd$sampleIsland(
                                sampleX,
                                sampleZ,
                                currentCellX,
                                currentCellZ,
                                progress
                        );

                /*
                 * 多座島的 Density 用 max。
                 *
                 * 不相加。
                 *
                 * 非常重要：
                 *
                 * 相加會讓靠近的島重新融合成大陸。
                 */
                bestDensity =
                        Math.max(
                                bestDensity,
                                islandDensity
                        );
            }
        }

        /*
         * ========================================
         * 19000 ~ 20000
         *
         * 整套 Cellular Islands 消失。
         * ========================================
         */

        if (
                distance
                        >
                        19_000.0D
        ) {

            double fade =
                    EndTerrainController
                            .getVoidTransition(
                                    distance
                            );

            bestDensity =
                    EndTerrainController.lerp(
                            fade,
                            bestDensity,
                            EndTerrainController.VOID_DENSITY
                    );
        }

        return bestDensity;
    }

    /*
     * ============================================================
     * 一個 Cell 是否有島
     * ============================================================
     */

    @Unique
    private boolean beyondTheEnd$cellHasIsland(
            int cellX,
            int cellZ,
            double progress
    ) {

        long hash =
                beyondTheEnd$hash(
                        cellX,
                        cellZ,
                        0x41B72A91L
                );

        double random =
                beyondTheEnd$toUnitDouble(
                        hash
                );

        /*
         * 13000：
         *
         * 約 78% Cell 有島。
         *
         * 20000：
         *
         * 約 30% Cell 有島。
         *
         * 但 19000 後還有整體 Fade，
         * 所以實際會更少。
         */

        double chance =
                EndTerrainController.lerp(
                        progress,
                        0.78D,
                        0.30D
                );

        return random
                <
                chance;
    }

    /*
     * ============================================================
     * 生成單一有限尺寸島
     * ============================================================
     */

    @Unique
    private double beyondTheEnd$sampleIsland(
            double worldX,
            double worldZ,
            int cellX,
            int cellZ,
            double progress
    ) {

        /*
         * ----------------------------------------
         * Island Center
         * ----------------------------------------
         */

        double randomX =
                beyondTheEnd$random(
                        cellX,
                        cellZ,
                        11
                );

        double randomZ =
                beyondTheEnd$random(
                        cellX,
                        cellZ,
                        17
                );

        double centerX =
                cellX
                        *
                        beyondTheEnd$CELL_SIZE

                        +

                        beyondTheEnd$CELL_SIZE
                                *
                                0.5D

                        +

                        (
                                randomX
                                        *
                                        2.0D
                                        -
                                        1.0D
                        )
                                *
                                beyondTheEnd$CENTER_JITTER;

        double centerZ =
                cellZ
                        *
                        beyondTheEnd$CELL_SIZE

                        +

                        beyondTheEnd$CELL_SIZE
                                *
                                0.5D

                        +

                        (
                                randomZ
                                        *
                                        2.0D
                                        -
                                        1.0D
                        )
                                *
                                beyondTheEnd$CENTER_JITTER;

        /*
         * ----------------------------------------
         * 島尺寸
         * ----------------------------------------
         *
         * 13000：
         *
         * 約 16 ~ 34 格半徑。
         *
         * 越靠近 20000：
         *
         * 半徑逐漸縮小。
         */

        double sizeRandom =
                beyondTheEnd$random(
                        cellX,
                        cellZ,
                        23
                );

        double baseRadius =
                16.0D
                        +
                        sizeRandom
                                *
                                18.0D;

        double sizeScale =
                EndTerrainController.lerp(
                        progress,
                        1.0D,
                        0.55D
                );

        baseRadius *=
                sizeScale;

        /*
         * X/Z 不同半徑。
         *
         * 形成自然橢圓，
         * 但比例限制在合理範圍。
         *
         * 不允許產生：
         *
         * 10 × 100
         *
         * 那種超長島。
         */

        double aspect =
                EndTerrainController.lerp(
                        beyondTheEnd$random(
                                cellX,
                                cellZ,
                                29
                        ),
                        0.78D,
                        1.22D
                );

        double radiusX =
                baseRadius
                        *
                        aspect;

        double radiusZ =
                baseRadius
                        /
                        aspect;

        /*
         * ----------------------------------------
         * 隨機旋轉
         * ----------------------------------------
         */

        double angle =
                beyondTheEnd$random(
                        cellX,
                        cellZ,
                        31
                )
                        *
                        Math.PI
                        *
                        2.0D;

        double cos =
                Math.cos(angle);

        double sin =
                Math.sin(angle);

        double dx =
                worldX - centerX;

        double dz =
                worldZ - centerZ;

        double rotatedX =
                dx * cos
                        -
                        dz * sin;

        double rotatedZ =
                dx * sin
                        +
                        dz * cos;

        /*
         * ----------------------------------------
         * 基礎橢圓距離
         * ----------------------------------------
         */

        double normalizedX =
                rotatedX
                        /
                        radiusX;

        double normalizedZ =
                rotatedZ
                        /
                        radiusZ;

        double normalizedDistance =
                Math.sqrt(
                        normalizedX
                                *
                                normalizedX
                                +
                                normalizedZ
                                        *
                                        normalizedZ
                );

        /*
         * ----------------------------------------
         * Noise 只負責島邊輪廓
         * ----------------------------------------
         */

        double shapeNoise =
                beyondTheEnd$shapeNoise
                        .getValue(
                                worldX
                                        *
                                        0.025D,

                                worldZ
                                        *
                                        0.025D
                        );

        double detailNoise =
                beyondTheEnd$detailNoise
                        .getValue(
                                worldX
                                        *
                                        0.065D,

                                worldZ
                                        *
                                        0.065D
                        );

        /*
         * 邊界扭曲。
         *
         * 不能太強。
         *
         * 否則有限島又會重新長出長觸手。
         */
        double distortion =
                shapeNoise
                        *
                        0.14D

                        +

                        detailNoise
                                *
                                0.055D;

        double effectiveDistance =
                normalizedDistance
                        +
                        distortion;

        /*
         * ----------------------------------------
         * 連續 Density
         * ----------------------------------------
         *
         * 中心：
         *
         * effectiveDistance ≈ 0
         *
         * 邊緣：
         *
         * ≈ 1
         *
         * 外部：
         *
         * > 1
         */

        double islandStrength =
                1.0D
                        -
                        effectiveDistance;

        /*
         * 強度映射。
         *
         * 中心 Density 明顯較高，
         * 邊緣自然下降。
         */
        return islandStrength
                *
                0.95D

                -

                0.37D;
    }

    /*
     * ============================================================
     * Deterministic Random
     * ============================================================
     */

    @Unique
    private double beyondTheEnd$random(
            int cellX,
            int cellZ,
            long salt
    ) {

        return beyondTheEnd$toUnitDouble(
                beyondTheEnd$hash(
                        cellX,
                        cellZ,
                        salt
                )
        );
    }

    @Unique
    private long beyondTheEnd$hash(
            int x,
            int z,
            long salt
    ) {

        long value =
                beyondTheEnd$seed;

        value ^=
                (long) x
                        *
                        341873128712L;

        value ^=
                (long) z
                        *
                        132897987541L;

        value ^=
                salt
                        *
                        0x9E3779B97F4A7C15L;

        /*
         * SplitMix64 finalizer。
         */
        value ^=
                value >>> 30;

        value *=
                0xBF58476D1CE4E5B9L;

        value ^=
                value >>> 27;

        value *=
                0x94D049BB133111EBL;

        value ^=
                value >>> 31;

        return value;
    }

    @Unique
    private double beyondTheEnd$toUnitDouble(
            long value
    ) {

        /*
         * 取高 53 bits，
         * 映射到 0~1。
         */
        return (
                (value >>> 11)
                        *
                        0x1.0p-53
        );
    }

    @Unique
    private static int floorDiv(
            int x,
            int y
    ) {

        return Math.floorDiv(
                x,
                y
        );
    }
}
