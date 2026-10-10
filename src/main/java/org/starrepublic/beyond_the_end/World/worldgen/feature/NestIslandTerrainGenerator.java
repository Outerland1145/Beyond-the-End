package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.util.Mth;
import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;

public final class NestIslandTerrainGenerator {

    /*
     * 一個 Placement Region 的大小。
     *
     * 巢穴遺跡非常稀有，
     * 所以不用像破碎區一樣每 100 格就算一次。
     */
    public static final int REGION_SIZE = 8192;

    /*
     * 島心相對 Region 中心的最大偏移。
     */
    private static final int CENTER_JITTER = 2048;

    /*
     * 每個 Region 1/4 機率存在巢穴。
     */
    private static final int REGION_CHANCE = 4;

    /*
     * 島嶼半徑。
     *
     * 這不是碎島。
     * 是真正主島級尺寸。
     */
    private static final double MIN_RADIUS = 120.0D;
    private static final double MAX_RADIUS = 280.0D;
    public static final double MIN_CENTER_DISTANCE = EndTerrainController.getVoidRadius()+30_000D;
    public static final double VOID_MARGIN = 32.0D;
    public static final long IegalRadius = 20_000L;

    /*
     * 原版 EndIslandDensityFunction 的合理下限。
     *
     * 原版輸出：
     *
     * (height - 8) / 128
     *
     * height clamp = [-100, 80]
     *
     * 所以：
     * min = -108 / 128 = -0.84375
     * max =  72 / 128 =  0.5625
     */
    private static final double MIN_DENSITY = -0.84375D;


    private NestIslandTerrainGenerator() {
    }
    public static double getMinCenterDistance() {

        return EndTerrainController.getVoidRadius()
                + 30_000D;
    }

    /*
     * ============================================================
     * Island data
     * ============================================================
     */

    public record IslandInfo(
            int centerX,
            int centerZ,
            double radius,
            long seed
    ) {
    }
    public static long getlegalRadius() {

        return EndTerrainController.getVoidRadius()+IegalRadius;
    }
    public static boolean isValidIsland(
            IslandInfo island,
            double voidRadius
    ) {
        double centerX =
                island.centerX();

        double centerZ =
                island.centerZ();


        double centerDistance =
                Math.sqrt(
                        centerX * centerX
                                +
                                centerZ * centerZ
                );


        /*
         * ========================================================
         * 世界中心 50,000 格禁生成區
         * ========================================================
         */

        if (
                centerDistance
                        <
                        getMinCenterDistance()
        ) {

            return false;
        }


        /*
         * ========================================================
         * 整座島必須位於虛空區
         * ========================================================
         *
         * 不只是島心在虛空區。
         *
         * 島嶼最靠近世界中心的一端，
         * 也必須完全超過 voidRadius。
         */
        if (
                centerDistance
                        -
                        island.radius()
                        -
                        VOID_MARGIN

                        <
                        voidRadius
        ) {

            return false;
        }


        return true;
    }
    /*
     * ============================================================
     * Density
     * ============================================================
     */

    public static double sampleDensity(
            int worldX,
            int worldZ,
            long worldSeed,
            double voidRadius
    ) {

        int regionX =
                Math.floorDiv(
                        worldX,
                        REGION_SIZE
                );

        int regionZ =
                Math.floorDiv(
                        worldZ,
                        REGION_SIZE
                );


        double bestDensity =
                MIN_DENSITY;


        /*
         * 島可能跨 Region 邊界，
         * 所以一定要查看周圍 3×3 Region。
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

                IslandInfo island =
                        getIslandForRegion(
                                worldSeed,
                                regionX + offsetX,
                                regionZ + offsetZ
                        );


                if (island == null) {
                    continue;
                }


                /*
                 * =================================================
                 * 整座島必須真正位於虛空區
                 * =================================================
                 *
                 * 不只是中心超過 voidRadius，
                 * 而是連靠近原點的島邊都不能跨出去。
                 */
                if (
                        !isValidIsland(
                                island,
                                voidRadius
                        )
                ) {

                    continue;
                }


                double density =
                        sampleIsland(
                                worldX,
                                worldZ,
                                island
                        );


                bestDensity =
                        Math.max(
                                bestDensity,
                                density
                        );
            }
        }


        return bestDensity;
    }


    /*
     * ============================================================
     * 1.7.10-style main island
     * ============================================================
     */

    private static double sampleIsland(
            int worldX,
            int worldZ,
            IslandInfo island
    ) {

        double dx =
                worldX
                        - island.centerX();

        double dz =
                worldZ
                        - island.centerZ();


        double distance =
                Math.sqrt(
                        dx * dx
                                +
                                dz * dz
                );


        /*
         * 很遠的地方直接退出。
         *
         * 原版 height 已經會在 radius 外快速跌入負值，
         * 不需要每次都繼續算。
         */
        if (
                distance
                        >
                        island.radius()
                                * 2.1D
        ) {

            return MIN_DENSITY;
        }


        /*
         * ========================================================
         * Legacy End main-island falloff
         * ========================================================
         *
         * 舊版核心：
         *
         * 100 - sqrt(x*x + z*z) * 8
         *
         * 現代 EndIslandDensityFunction
         * 實際也是這套邏輯的延續。
         *
         * 這裡只是把固定尺度正規化成每座巢穴自己的 radius。
         *
         * distance == 0
         *     → height ≈ 100 → clamp 80
         *
         * distance == radius
         *     → height ≈ 0
         *
         * distance == radius * 2
         *     → height ≈ -100
         */
        double normalizedDistance =
                distance
                        /
                        island.radius();


        double height =
                100.0D
                        -
                        normalizedDistance
                                *
                                100.0D;


        /*
         * 稍微加入非常低頻的輪廓變化。
         *
         * 不做碎裂，
         * 只讓島邊不像完美圓。
         */
        double edgeNoise =
                pseudoNoise(
                        worldX,
                        worldZ,
                        island.seed()
                );


        /*
         * 越靠外圍 Noise 影響越強。
         *
         * 中央維持完整大陸，
         * 不讓島心被打洞。
         */
        double edgeFactor =
                Mth.clamp(
                        (normalizedDistance - 0.45D)
                                /
                                0.65D,
                        0.0D,
                        1.0D
                );


        height +=
                edgeNoise
                        *
                        18.0D
                        *
                        edgeFactor;


        height =
                Mth.clamp(
                        height,
                        -100.0D,
                        80.0D
                );


        /*
         * 和 Vanilla EndIslandDensityFunction
         * 使用相同輸出區間。
         */
        return (
                height
                        -
                        8.0D
        )
                /
                128.0D;
    }


    /*
     * ============================================================
     * Placement
     * ============================================================
     */

    public static IslandInfo getIslandForRegion(
            long worldSeed,
            int regionX,
            int regionZ
    ) {

        long baseHash =
                hash(
                        worldSeed,
                        regionX,
                        regionZ,
                        0x4E455354L
                );


        /*
         * Region 是否有島。
         */
        if (
                Math.floorMod(
                        baseHash,
                        REGION_CHANCE
                )
                        !=
                        0
        ) {

            return null;
        }


        double randomX =
                unit(
                        hash(
                                worldSeed,
                                regionX,
                                regionZ,
                                11L
                        )
                );

        double randomZ =
                unit(
                        hash(
                                worldSeed,
                                regionX,
                                regionZ,
                                17L
                        )
                );


        long regionCenterX =
                (long) regionX
                        *
                        REGION_SIZE

                        +

                        REGION_SIZE
                                /
                                2L;

        long regionCenterZ =
                (long) regionZ
                        *
                        REGION_SIZE

                        +

                        REGION_SIZE
                                /
                                2L;


        int centerX =
                (int) (
                        regionCenterX

                                +

                                (
                                        randomX
                                                *
                                                2.0D
                                                -
                                                1.0D
                                )
                                        *
                                        CENTER_JITTER
                );


        int centerZ =
                (int) (
                        regionCenterZ

                                +

                                (
                                        randomZ
                                                *
                                                2.0D
                                                -
                                                1.0D
                                )
                                        *
                                        CENTER_JITTER
                );


        /*
         * ========================================================
         * Island size
         * ========================================================
         */

        double sizeRandom =
                unit(
                        hash(
                                worldSeed,
                                regionX,
                                regionZ,
                                23L
                        )
                );


        double radius;


        /*
         * 約 70%：
         * 普通大型主島。
         */
        if (
                sizeRandom
                        <
                        0.70D
        ) {

            radius =
                    lerp(
                            sizeRandom
                                    /
                                    0.70D,
                            120.0D,
                            175.0D
                    );

        }

        /*
         * 約 25%：
         * 大型。
         */
        else if (
                sizeRandom
                        <
                        0.95D
        ) {

            radius =
                    lerp(
                            (
                                    sizeRandom
                                            -
                                            0.70D
                            )
                                    /
                                    0.25D,
                            175.0D,
                            225.0D
                    );

        }

        /*
         * 約 5%：
         * 巨型。
         */
        else {

            radius =
                    lerp(
                            (
                                    sizeRandom
                                            -
                                            0.95D
                            )
                                    /
                                    0.05D,
                            225.0D,
                            MAX_RADIUS
                    );
        }


        return new IslandInfo(
                centerX,
                centerZ,
                radius,
                hash(
                        worldSeed,
                        regionX,
                        regionZ,
                        97L
                )
        );
    }


    /*
     * ============================================================
     * Edge Noise
     * ============================================================
     *
     * 這裡暫時不用建立另一組 SimplexNoise instance。
     *
     * 它只是破壞完美圓形，
     * 真正的 3D 地形細節仍由現代 End noise router 處理。
     */
    private static double pseudoNoise(
            int x,
            int z,
            long seed
    ) {

        int cellSize = 32;

        int cellX =
                Math.floorDiv(
                        x,
                        cellSize
                );

        int cellZ =
                Math.floorDiv(
                        z,
                        cellSize
                );


        double localX =
                Math.floorMod(
                        x,
                        cellSize
                )
                        /
                        (double) cellSize;

        double localZ =
                Math.floorMod(
                        z,
                        cellSize
                )
                        /
                        (double) cellSize;


        localX =
                smoothstep(
                        localX
                );

        localZ =
                smoothstep(
                        localZ
                );


        double v00 =
                signedUnit(
                        hash(
                                seed,
                                cellX,
                                cellZ,
                                1
                        )
                );

        double v10 =
                signedUnit(
                        hash(
                                seed,
                                cellX + 1,
                                cellZ,
                                1
                        )
                );

        double v01 =
                signedUnit(
                        hash(
                                seed,
                                cellX,
                                cellZ + 1,
                                1
                        )
                );

        double v11 =
                signedUnit(
                        hash(
                                seed,
                                cellX + 1,
                                cellZ + 1,
                                1
                        )
                );


        double a =
                lerp(
                        localX,
                        v00,
                        v10
                );

        double b =
                lerp(
                        localX,
                        v01,
                        v11
                );


        return lerp(
                localZ,
                a,
                b
        );
    }


    /*
     * ============================================================
     * Deterministic hashing
     * ============================================================
     */

    private static long hash(
            long seed,
            int x,
            int z,
            long salt
    ) {

        long value =
                seed;

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


    private static double unit(
            long value
    ) {

        return (
                value >>> 11
        )
                *
                0x1.0p-53;
    }


    private static double signedUnit(
            long value
    ) {

        return unit(value)
                *
                2.0D
                -
                1.0D;
    }


    private static double smoothstep(
            double value
    ) {

        return value
                *
                value
                *
                (
                        3.0D
                                -
                                2.0D
                                        *
                                        value
                );
    }


    private static double lerp(
            double value,
            double min,
            double max
    ) {

        return min
                +
                (
                        max
                                -
                                min
                )
                        *
                        value;
    }
}
