package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class NestIslandDecorationGenerator {

    /*
     * ============================================================
     * Settings
     * ============================================================
     */
    /*
     * ============================================================
     * Chorus Plant
     * ============================================================
     */

    /**
     * 每個巢穴島 Chunk：
     *
     * 1 / 3 機率嘗試生成歌萊果。
     *
     * 屬於中等密度，
     * 不會像 Chorus Forest 一樣變成密林。
     */
    private static final int
            CHORUS_CHUNK_CHANCE = 3;


    /**
     * 一次成功抽中後，
     * 嘗試 1 ~ 3 株。
     */
    private static final int
            CHORUS_MIN_ATTEMPTS = 1;

    private static final int
            CHORUS_MAX_ATTEMPTS = 3;


    /**
     * 不在島最外圈生成歌萊果。
     */
    private static final double
            CHORUS_AREA_SCALE = 0.90D;


    /*
     * ============================================================
     * Fallen spikes
     * ============================================================
     */

    /**
     * 只有「沒有水晶」的柱子才進行此判定。
     *
     * 12% 的無水晶柱會倒塌。
     *
     * 假設水晶殘留率 45%，
     * 那麼整體約：
     *
     * 55% × 12%
     * ≈ 6.6%
     *
     * 的柱子是倒塌狀態。
     */
    private static final double
            FALLEN_SPIKE_CHANCE = 0.12D;
    /**
     * 返回傳送門：
     * 距離島心 <= 50。
     */
    private static final double PORTAL_MAX_DISTANCE = 50.0D;

    /**
     * 黑曜石平台：
     * 距離島心 <= 100。
     */
    private static final double PLATFORM_MAX_DISTANCE = 100.0D;

    /**
     * 返回傳送門固定 Y。
     */
    private static final int PORTAL_Y = 64;

    /**
     * 終界水晶殘留機率。
     */
    private static final double CRYSTAL_CHANCE = 0.15D;

    /**
     * 哭泣黑曜石在柱子外殼上的比例。
     */
    private static final double MIN_CRYING_CHANCE = 0.06D;
    private static final double MAX_CRYING_CHANCE = 0.16D;

    /**
     * 為避免每個 Chunk 都重新計算一整座島的配置，
     * 使用 island seed 快取。
     */
    private static final ConcurrentHashMap<Long, DecorationLayout>
            LAYOUT_CACHE = new ConcurrentHashMap<>();

    private NestIslandDecorationGenerator() {
    }

    private static boolean fallenSpikeIntersectsChunk(
            SpikeInfo spike,
            ChunkPos chunk
    ) {

        double directionX =
                Math.cos(
                        spike.fallenAngle()
                );

        double directionZ =
                Math.sin(
                        spike.fallenAngle()
                );


        double endX =
                spike.x()

                        +

                        directionX
                                *
                                spike.height();


        double endZ =
                spike.z()

                        +

                        directionZ
                                *
                                spike.height();


        double minX =
                Math.min(
                        spike.x(),
                        endX
                )
                        -
                        spike.radius()
                        -
                        1.0D;


        double maxX =
                Math.max(
                        spike.x(),
                        endX
                )
                        +
                        spike.radius()
                        +
                        1.0D;


        double minZ =
                Math.min(
                        spike.z(),
                        endZ
                )
                        -
                        spike.radius()
                        -
                        1.0D;


        double maxZ =
                Math.max(
                        spike.z(),
                        endZ
                )
                        +
                        spike.radius()
                        +
                        1.0D;


        return maxX
                >=
                chunk.getMinBlockX()

                &&

                minX
                        <=
                        chunk.getMaxBlockX()

                &&

                maxZ
                        >=
                        chunk.getMinBlockZ()

                &&

                minZ
                        <=
                        chunk.getMaxBlockZ();
    }
    private static void placeFallenSpikeSection(
            WorldGenLevel level,
            ChunkPos chunk,
            SpikeInfo spike
    ) {

        double directionX =
                Math.cos(
                        spike.fallenAngle()
                );

        double directionZ =
                Math.sin(
                        spike.fallenAngle()
                );


        /*
         * 原本柱高直接成為倒塌後的長度。
         */
        double length =
                spike.height();


        /*
         * 只遍歷目前 Chunk。
         */
        for (
                int x = chunk.getMinBlockX();
                x <= chunk.getMaxBlockX();
                x++
        ) {

            for (
                    int z = chunk.getMinBlockZ();
                    z <= chunk.getMaxBlockZ();
                    z++
            ) {

                /*
                 * 使用 Block center。
                 */
                double relativeX =
                        x
                                +
                                0.5D

                                -

                                spike.x();


                double relativeZ =
                        z
                                +
                                0.5D

                                -

                                spike.z();


                /*
                 * ====================================================
                 * 投影到柱子的中心線
                 * ====================================================
                 */

                double along =
                        relativeX
                                *
                                directionX

                                +

                                relativeZ
                                        *
                                        directionZ;


                /*
                 * 不在柱子的長度範圍。
                 */
                if (
                        along
                                <
                                0.0D

                                ||

                                along
                                        >
                                        length
                ) {

                    continue;
                }


                /*
                 * 找出投影點。
                 */
                double projectedX =
                        directionX
                                *
                                along;

                double projectedZ =
                        directionZ
                                *
                                along;


                double perpendicularX =
                        relativeX
                                -
                                projectedX;

                double perpendicularZ =
                        relativeZ
                                -
                                projectedZ;


                double horizontalDistanceSquared =
                        perpendicularX
                                *
                                perpendicularX

                                +

                                perpendicularZ
                                        *
                                        perpendicularZ;


                double radiusSquared =
                        spike.radius()
                                *
                                spike.radius();


                if (
                        horizontalDistanceSquared
                                >
                                radiusSquared
                                        +
                                        1.0D
                ) {

                    continue;
                }


                /*
                 * ====================================================
                 * 找這一格自己的地面
                 * ====================================================
                 *
                 * 只讀目前 Chunk，
                 * 所以不會再次造成 worldgen 越界。
                 */
                int surfaceY =
                        findEndStoneSurface(
                                level,
                                x,
                                z
                        );


                if (
                        surfaceY
                                ==
                                Integer.MIN_VALUE
                ) {

                    continue;
                }


                /*
                 * 橫向圓柱的中心。
                 *
                 * radius 高，
                 * 所以圓柱底部剛好貼著地面。
                 */
                int centerY =
                        surfaceY
                                +
                                spike.radius();


                /*
                 * ====================================================
                 * Vertical cross section
                 * ====================================================
                 */

                for (
                        int y = surfaceY;
                        y <= surfaceY + spike.radius() * 2;
                        y++
                ) {

                    double verticalDistance =
                            y
                                    -
                                    centerY;


                    double crossSectionDistance =
                            horizontalDistanceSquared

                                    +

                                    verticalDistance
                                            *
                                            verticalDistance;


                    if (
                            crossSectionDistance
                                    >
                                    radiusSquared
                                            +
                                            1.0D
                    ) {

                        continue;
                    }


                    BlockPos pos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );


                    /*
                     * =================================================
                     * 外殼判定
                     * =================================================
                     */

                    double innerRadius =
                            Math.max(
                                    0.0D,
                                    spike.radius()
                                            -
                                            1.0D
                            );


                    boolean outerShell =
                            crossSectionDistance

                                    >=

                                    innerRadius
                                            *
                                            innerRadius;


                    if (
                            outerShell

                                    &&

                                    shouldUseCryingObsidian(
                                            spike,
                                            x,
                                            y,
                                            z
                                    )
                    ) {

                        setBlock(
                                level,
                                pos,
                                Blocks.CRYING_OBSIDIAN
                                        .defaultBlockState()
                        );

                    } else {

                        setBlock(
                                level,
                                pos,
                                Blocks.OBSIDIAN
                                        .defaultBlockState()
                        );
                    }
                }
            }
        }


        /*
         * ============================================================
         * Bedrock cap
         * ============================================================
         *
         * 原本沒有水晶，
         * 但柱頂留下的基岩仍然跟著柱子一起倒下。
         */

        int tipX =
                (int) Math.round(
                        spike.x()

                                +

                                directionX
                                        *
                                        length
                );


        int tipZ =
                (int) Math.round(
                        spike.z()

                                +

                                directionZ
                                        *
                                        length
                );


        /*
         * 只有包含柱頂的 Chunk 負責。
         */
        if (
                ownsPosition(
                        chunk,
                        tipX,
                        tipZ
                )
        ) {

            int surfaceY =
                    findEndStoneSurface(
                            level,
                            tipX,
                            tipZ
                    );


            if (
                    surfaceY
                            !=
                            Integer.MIN_VALUE
            ) {

                setBlock(
                        level,

                        new BlockPos(
                                tipX,
                                surfaceY + spike.radius(),
                                tipZ
                        ),

                        Blocks.BEDROCK
                                .defaultBlockState()
                );
            }
        }
    }
    private static void placeChorusPlants(
            WorldGenLevel level,
            ChunkPos chunk,
            NestIslandTerrainGenerator.IslandInfo island
    ) {

        /*
         * ============================================================
         * Deterministic random
         * ============================================================
         */

        long seed =
                island.seed()

                        ^

                        (
                                (long) chunk.x
                                        *
                                        341873128712L
                        )

                        ^

                        (
                                (long) chunk.z
                                        *
                                        132897987541L
                        )

                        ^

                        0x43484F525553L;


        seed =
                mix64(
                        seed
                );


        RandomSource random =
                RandomSource.create(
                        seed
                );


        /*
         * 每三個 Chunk 約一個 Chunk有植被。
         */
        if (
                random.nextInt(
                        CHORUS_CHUNK_CHANCE
                )
                        !=
                        0
        ) {

            return;
        }


        int attempts =
                CHORUS_MIN_ATTEMPTS

                        +

                        random.nextInt(
                                CHORUS_MAX_ATTEMPTS
                                        -
                                        CHORUS_MIN_ATTEMPTS
                                        +
                                        1
                        );


        double maximumRadius =
                island.radius()
                        *
                        CHORUS_AREA_SCALE;


        double maximumRadiusSquared =
                maximumRadius
                        *
                        maximumRadius;


        for (
                int attempt = 0;
                attempt < attempts;
                attempt++
        ) {

            /*
             * 直接在目前 Chunk 選位置。
             */
            int x =
                    chunk.getMinBlockX()

                            +

                            random.nextInt(
                                    16
                            );


            int z =
                    chunk.getMinBlockZ()

                            +

                            random.nextInt(
                                    16
                            );


            /*
             * ========================================================
             * 必須仍然位於巢穴島內部
             * ========================================================
             */

            double dx =
                    x
                            -
                            island.centerX();

            double dz =
                    z
                            -
                            island.centerZ();


            if (
                    dx * dx
                            +
                            dz * dz

                            >

                            maximumRadiusSquared
            ) {

                continue;
            }


            /*
             * ========================================================
             * 找 End Stone surface
             * ========================================================
             */

            int surfaceY =
                    findEndStoneSurface(
                            level,
                            x,
                            z
                    );


            if (
                    surfaceY
                            ==
                            Integer.MIN_VALUE
            ) {

                continue;
            }


            BlockPos plantPos =
                    new BlockPos(
                            x,
                            surfaceY,
                            z
                    );


            /*
             * 上面必須是空氣。
             */
            if (
                    !level
                            .getBlockState(
                                    plantPos
                            )
                            .isAir()
            ) {

                continue;
            }


            /*
             * 底下必須是真正 End Stone。
             *
             * 因此：
             *
             * - 黑曜石平台
             * - 黑曜石柱
             * - 基岩
             *
             * 上面都不會長。
             */
            if (
                    !level
                            .getBlockState(
                                    plantPos.below()
                            )
                            .is(
                                    Blocks.END_STONE
                            )
            ) {

                continue;
            }


            /*
             * ========================================================
             * Vanilla chorus plant generation
             * ========================================================
             *
             * 8 是原版風格的水平最大擴張距離。
             */
            ChorusFlowerBlock.generatePlant(
                    level,
                    plantPos,
                    random,
                    8
            );
        }
    }
    /*
     * ============================================================
     * Public entry
     * ============================================================
     */
    private static boolean ownsPosition(
            ChunkPos chunk,
            int blockX,
            int blockZ
    ) {

        return blockX
                >=
                chunk.getMinBlockX()

                &&

                blockX
                        <=
                        chunk.getMaxBlockX()

                &&

                blockZ
                        >=
                        chunk.getMinBlockZ()

                &&

                blockZ
                        <=
                        chunk.getMaxBlockZ();
    }
    public static void decorateChunk(
            WorldGenLevel level,
            ChunkAccess chunk
    ) {

        ChunkPos chunkPos =
                chunk.getPos();

        int blockX =
                chunkPos.getMiddleBlockX();

        int blockZ =
                chunkPos.getMiddleBlockZ();


        int regionX =
                Math.floorDiv(
                        blockX,
                        NestIslandTerrainGenerator.REGION_SIZE
                );

        int regionZ =
                Math.floorDiv(
                        blockZ,
                        NestIslandTerrainGenerator.REGION_SIZE
                );


        long worldSeed =
                level.getSeed();


        /*
         * 島可能靠近 Region 邊界，
         * 所以仍然檢查周圍 3x3 Region。
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

                NestIslandTerrainGenerator.IslandInfo island =
                        NestIslandTerrainGenerator
                                .getIslandForRegion(
                                        worldSeed,
                                        regionX + offsetX,
                                        regionZ + offsetZ
                                );


                if (island == null) {

                    continue;
                }


                /*
                 * 如果你已經改成：
                 *
                 * isValidIsland(island)
                 *
                 * 就使用你自己的新版。
                 */
                if (
                        !NestIslandTerrainGenerator
                                .isValidIsland(
                                        island,
                                        EndTerrainController
                                                .getVoidRadius()
                                )
                ) {

                    continue;
                }


                DecorationLayout layout =
                        LAYOUT_CACHE
                                .computeIfAbsent(
                                        island.seed(),
                                        ignored ->
                                                createLayout(
                                                        island
                                                )
                                );


                /*
                 * ========================================================
                 * Spikes
                 * ========================================================
                 */

                for (
                        SpikeInfo spike
                        :
                        layout.spikes()
                ) {

                    if (
                            spike.fallen()
                    ) {

                        if (
                                fallenSpikeIntersectsChunk(
                                        spike,
                                        chunkPos
                                )
                        ) {

                            placeFallenSpikeSection(
                                    level,
                                    chunkPos,
                                    spike
                            );
                        }

                        continue;
                    }


                    if (
                            ownsPosition(
                                    chunkPos,
                                    spike.x(),
                                    spike.z()
                            )
                    ) {

                        placeSpike(
                                level,
                                spike
                        );
                    }
                }


                /*
                 * ========================================================
                 * Return portal
                 * ========================================================
                 */

                if (
                        ownsPosition(
                                chunkPos,
                                layout.portal().x(),
                                layout.portal().z()
                        )
                ) {

                    placePortal(
                            level,
                            layout.portal()
                    );
                }


                /*
                 * ========================================================
                 * Arrival platform
                 * ========================================================
                 */

                if (
                        ownsPosition(
                                chunkPos,
                                layout.platform().x(),
                                layout.platform().z()
                        )
                ) {

                    placePlatform(
                            level,
                            layout.platform()
                    );
                }


                /*
                 * ========================================================
                 * Chorus vegetation
                 * ========================================================
                 */

                placeChorusPlants(
                        level,
                        chunkPos,
                        island
                );
            }
        }
    }


    /*
     * ============================================================
     * Layout generation
     * ============================================================
     */

    private static DecorationLayout createLayout(
            NestIslandTerrainGenerator.IslandInfo island
    ) {

        Random random =
                new Random(
                        island.seed()
                                ^
                                0x4E4553544445434FL
                );

        /*
         * ========================================================
         * Portal
         * ========================================================
         */

        Point portalPoint =
                randomPointInDisk(
                        random,
                        island.centerX(),
                        island.centerZ(),
                        PORTAL_MAX_DISTANCE
                );

        PortalInfo portal =
                new PortalInfo(
                        portalPoint.x(),
                        PORTAL_Y,
                        portalPoint.z()
                );


        /*
         * ========================================================
         * Platform
         * ========================================================
         *
         * 不直接放到 100 格邊緣，
         * 否則最小型主島可能讓平台落到虛空。
         *
         * 半徑永遠 <= 100。
         */
        double platformRadius =
                Math.min(
                        PLATFORM_MAX_DISTANCE,
                        island.radius() * 0.70D
                );

        Point platformPoint = null;

        for (int attempt = 0; attempt < 64; attempt++) {

            Point candidate =
                    randomPointInAnnulus(
                            random,
                            island.centerX(),
                            island.centerZ(),
                            45.0D,
                            platformRadius
                    );

            /*
             * 不要和返回傳送門疊在一起。
             */
            if (
                    distanceSquared(
                            candidate.x(),
                            candidate.z(),
                            portal.x(),
                            portal.z()
                    )
                            >=
                            20.0D * 20.0D
            ) {

                platformPoint = candidate;

                break;
            }
        }

        if (platformPoint == null) {

            platformPoint =
                    new Point(
                            island.centerX() + 60,
                            island.centerZ()
                    );
        }


        PlatformInfo platform =
                new PlatformInfo(
                        platformPoint.x(),
                        platformPoint.z()
                );


        /*
         * ========================================================
         * Spikes
         * ========================================================
         *
         * 島越大，柱子越多。
         */
        /*
         * ========================================================
         * Legacy-style Obsidian Spikes
         * ========================================================
         *
         * 舊版終界不是先規定：
         *
         * 「這座島只能有 20 根柱子」
         *
         * 而是島上的 Chunk 不斷有機率嘗試生成柱子。
         *
         * 因此：
         *
         * 島越大
         *     ↓
         * Chunk 越多
         *     ↓
         * 柱子自然越多
         *
         * 這就是大型舊終界主島充滿柱子的原因。
         */

        List<SpikeInfo> spikes =
                new ArrayList<>();


        /*
         * 柱子只在島嶼比較可靠的區域生成。
         *
         * 0.90 代表外圍只留下很薄的一圈沒有柱子，
         * 比之前的 0.82 明顯更密。
         */
        double spikeAreaRadius =
                island.radius();


        /*
         * 找出整座島覆蓋的 Chunk 範圍。
         */
        int minChunkX =
                Math.floorDiv(
                        (int) Math.floor(
                                island.centerX()
                                        -
                                        spikeAreaRadius
                        ),
                        16
                );

        int maxChunkX =
                Math.floorDiv(
                        (int) Math.ceil(
                                island.centerX()
                                        +
                                        spikeAreaRadius
                        ),
                        16
                );

        int minChunkZ =
                Math.floorDiv(
                        (int) Math.floor(
                                island.centerZ()
                                        -
                                        spikeAreaRadius
                        ),
                        16
                );

        int maxChunkZ =
                Math.floorDiv(
                        (int) Math.ceil(
                                island.centerZ()
                                        +
                                        spikeAreaRadius
                        ),
                        16
                );


        for (
                int chunkX = minChunkX;
                chunkX <= maxChunkX;
                chunkX++
        ) {

            for (
                    int chunkZ = minChunkZ;
                    chunkZ <= maxChunkZ;
                    chunkZ++
            ) {

                /*
                 * ====================================================
                 * 每個 Chunk 獨立取得 deterministic random
                 * ====================================================
                 *
                 * 因此無論 Chunk 生成順序如何，
                 * 同一世界永遠產生同一組柱子。
                 */
                long spikeSeed =
                        mix64(
                                island.seed()

                                        ^

                                        (
                                                (long) chunkX
                                                        *
                                                        341873128712L
                                        )

                                        ^

                                        (
                                                (long) chunkZ
                                                        *
                                                        132897987541L
                                        )
                        );


                Random spikeRandom =
                        new Random(
                                spikeSeed
                        );


                /*
                 * ====================================================
                 * 舊版風格：
                 *
                 * 約 1/5 Chunk 嘗試生成柱子。
                 * ====================================================
                 */
                if (
                        spikeRandom.nextInt(4)
                                !=
                                0
                ) {

                    continue;
                }


                /*
                 * Chunk 內隨機位置。
                 */
                int x =
                        chunkX
                                *
                                16

                                +

                                spikeRandom.nextInt(16);


                int z =
                        chunkZ
                                *
                                16

                                +

                                spikeRandom.nextInt(16);


                /*
                 * ====================================================
                 * 確認仍在島的柱群範圍內
                 * ====================================================
                 */
                double dx =
                        x
                                -
                                island.centerX();

                double dz =
                        z
                                -
                                island.centerZ();


                double distanceSquared =
                        dx * dx
                                +
                                dz * dz;


                if (
                        distanceSquared
                                >
                                spikeAreaRadius
                                        *
                                        spikeAreaRadius
                ) {

                    continue;
                }


                /*
                 * ====================================================
                 * Legacy dimensions
                 * ====================================================
                 */

                int radius =
                        spikeRandom.nextInt(4)
                                +
                                1;


                int height =
                        spikeRandom.nextInt(32)
                                +
                                6;


                /*
                 * ====================================================
                 * Portal exclusion
                 * ====================================================
                 */
                if (
                        distanceSquared(
                                x,
                                z,
                                portal.x(),
                                portal.z()
                        )
                                <
                                square(
                                        radius
                                                +
                                                8.0D
                                )
                ) {

                    continue;
                }


                /*
                 * ====================================================
                 * Platform exclusion
                 * ====================================================
                 */
                if (
                        distanceSquared(
                                x,
                                z,
                                platform.x(),
                                platform.z()
                        )
                                <
                                square(
                                        radius
                                                +
                                                6.0D
                                )
                ) {

                    continue;
                }


                /*
                 * ====================================================
                 * 柱子之間只防止真正重疊
                 * ====================================================
                 *
                 * 之前是：
                 *
                 * radius + otherRadius + 5
                 *
                 * 那會讓柱子看起來像人工排好的。
                 *
                 * 舊終界應允許柱子靠得相當近。
                 */
                boolean overlaps =
                        false;


                for (
                        SpikeInfo existing
                        :
                        spikes
                ) {

                    double minimumDistance =
                            radius
                                    +
                                    existing.radius()
                                    +
                                    1.0D;


                    if (
                            distanceSquared(
                                    x,
                                    z,
                                    existing.x(),
                                    existing.z()
                            )
                                    <
                                    minimumDistance
                                            *
                                            minimumDistance
                    ) {

                        overlaps = true;

                        break;
                    }
                }


                if (overlaps) {

                    continue;
                }


                /*
                 * ====================================================
                 * Crystal
                 * ====================================================
                 */

                boolean hasCrystal =
                        spikeRandom.nextDouble()
                                <
                                CRYSTAL_CHANCE;

                boolean fallen =
                        !hasCrystal

                                &&

                                spikeRandom.nextDouble()
                                        <
                                        FALLEN_SPIKE_CHANCE;


                /*
                 * 倒塌方向完全隨機。
                 */
                double fallenAngle =
                        spikeRandom.nextDouble()
                                *
                                Math.PI
                                *
                                2.0D;
                /*
                 * ====================================================
                 * Crying Obsidian
                 * ====================================================
                 */

                double cryingChance =
                        MIN_CRYING_CHANCE

                                +

                                spikeRandom.nextDouble()

                                        *

                                        (
                                                MAX_CRYING_CHANCE
                                                        -
                                                        MIN_CRYING_CHANCE
                                        );


                spikes.add(
                        new SpikeInfo(
                                x,
                                z,
                                radius,
                                height,

                                hasCrystal,

                                fallen,

                                fallenAngle,

                                spikeRandom.nextFloat()
                                        *
                                        360.0F,

                                cryingChance,

                                spikeRandom.nextLong()
                        )
                );
            }
        }


        return new DecorationLayout(
                portal,
                platform,
                List.copyOf(spikes)
        );
    }


    /*
     * ============================================================
     * Portal
     * ============================================================
     *
     * 造型直接沿用 inactive End Podium 的概念：
     *
     * - 外圍基岩
     * - 下方基岩 + End Stone
     * - 中央空心
     * - 基岩柱
     * - 四支火把
     *
     * 絕不生成 END_PORTAL。
     *
     * 1.21.1 原版 EndPodiumFeature(false)
     * 就是在中央使用 AIR 而非 END_PORTAL。
     */
    private static void placePortal(
            WorldGenLevel level,
            PortalInfo portal
    ) {

        BlockPos origin =
                new BlockPos(
                        portal.x(),
                        portal.y(),
                        portal.z()
                );


        for (
                int x = portal.x() - 4;
                x <= portal.x() + 4;
                x++
        ) {

            for (
                    int y = portal.y() - 1;
                    y <= portal.y() + 32;
                    y++
            ) {

                for (
                        int z = portal.z() - 4;
                        z <= portal.z() + 4;
                        z++
                ) {

                    double dx =
                            x - portal.x();

                    double dy =
                            y - portal.y();

                    double dz =
                            z - portal.z();


                    double distanceSquared =
                            dx * dx
                                    +
                                    dy * dy
                                    +
                                    dz * dz;


                    boolean inner =
                            distanceSquared
                                    <
                                    2.5D
                                            *
                                            2.5D;

                    boolean rim =
                            distanceSquared
                                    <
                                    3.5D
                                            *
                                            3.5D;


                    if (!rim) {
                        continue;
                    }


                    BlockPos pos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );


                    if (
                            y
                                    <
                                    portal.y()
                    ) {

                        if (inner) {

                            setBlock(
                                    level,
                                    pos,
                                    Blocks.BEDROCK
                                            .defaultBlockState()
                            );

                        } else {

                            setBlock(
                                    level,
                                    pos,
                                    Blocks.AIR
                                            .defaultBlockState()
                            );
                        }

                    } else if (
                            y
                                    >
                                    portal.y()
                    ) {

                        /*
                         * 清空祭壇上方。
                         */
                        setBlock(
                                level,
                                pos,
                                Blocks.AIR
                                        .defaultBlockState()
                        );

                    } else if (!inner) {

                        /*
                         * 外圍基岩。
                         */
                        setBlock(
                                level,
                                pos,
                                Blocks.BEDROCK
                                        .defaultBlockState()
                        );

                    } else {

                        /*
                         * 關鍵：
                         *
                         * 沒有任何 End Portal block。
                         */
                        setBlock(
                                level,
                                pos,
                                Blocks.AIR
                                        .defaultBlockState()
                        );
                    }
                }
            }
        }


        /*
         * 中央基岩柱。
         */
        BlockPos BedrockPiller = origin;
        for (int y = 0; y < 5; y++) {

            setBlock(
                    level,
                    BedrockPiller,
                    Blocks.BEDROCK
                            .defaultBlockState()
            );
            BedrockPiller = origin.above(y);
        }


        /*
         * 四支火把。
         */
        BlockPos torchCenter =
                origin.above(2);


        for (
                Direction direction
                :
                Direction.Plane.HORIZONTAL
        ) {

            BlockPos torchPos =
                    torchCenter.relative(
                            direction
                    );


            setBlock(
                    level,
                    torchPos,

                    Blocks.WALL_TORCH
                            .defaultBlockState()
                            .setValue(
                                    WallTorchBlock.FACING,
                                    direction
                            )
            );
        }
    }


    /*
     * ============================================================
     * Platform
     * ============================================================
     *
     * 仿原版 End platform：
     *
     * 5x5 obsidian floor
     * 上方 3 格清空。
     */
    private static void placePlatform(
            WorldGenLevel level,
            PlatformInfo platform
    ) {

        /*
         * 現在只有平台中心所在的 Chunk
         * 才會呼叫這個函式。
         *
         * 因此這次查詢一定是在目前 WorldGenRegion
         * 的附近，而不是島另一端。
         */
        int surfaceY = 50;


        for (
                int dx = -2;
                dx <= 2;
                dx++
        ) {

            for (
                    int dz = -2;
                    dz <= 2;
                    dz++
            ) {

                int x =
                        platform.x()
                                +
                                dx;

                int z =
                        platform.z()
                                +
                                dz;


                /*
                 * 黑曜石平台。
                 */
                setBlock(
                        level,
                        new BlockPos(
                                x,
                                surfaceY - 1,
                                z
                        ),
                        Blocks.OBSIDIAN
                                .defaultBlockState()
                );


                /*
                 * 平台上方清空。
                 */
                for (
                        int dy = 0;
                        dy < 3;
                        dy++
                ) {

                    setBlock(
                            level,
                            new BlockPos(
                                    x,
                                    surfaceY + dy,
                                    z
                            ),
                            Blocks.AIR
                                    .defaultBlockState()
                    );
                }
            }
        }
    }


    /*
     * ============================================================
     * Spike
     * ============================================================
     */

    private static void placeSpike(
            WorldGenLevel level,
            SpikeInfo spike
    ) {

        int baseY =
                findEndStoneSurface(
                        level,
                        spike.x(),
                        spike.z()
                );


        if (
                baseY
                        ==
                        Integer.MIN_VALUE
        ) {

            return;
        }


        /*
         * 仿舊 WorldGenSpikes：
         *
         * 整個柱底圓盤下面都必須是 End Stone。
         *
         * 避免柱子懸空在島邊。
         */
        if (
                !hasValidSpikeBase(
                        level,
                        spike,
                        baseY
                )
        ) {

            return;
        }


        int topY =
                baseY
                        +
                        spike.height();


        /*
         * 柱體。
         */
        for (
                int x =
                spike.x()
                        -
                        spike.radius();
                x <=
                        spike.x()
                                +
                                spike.radius();
                x++
        ) {

            for (
                    int z =
                    spike.z()
                            -
                            spike.radius();
                    z <=
                            spike.z()
                                    +
                                    spike.radius();
                    z++
            ) {

                int dx =
                        x - spike.x();

                int dz =
                        z - spike.z();


                if (
                        dx * dx
                                +
                                dz * dz

                                >

                                spike.radius()
                                        *
                                        spike.radius()
                                        +
                                        1
                ) {

                    continue;
                }


                /*
                 * 這個 Chunk 只寫自己的部分。
                 *
                 * 防止 worldgen 跨 Chunk 寫入問題。
                 */



                for (
                        int y = baseY;
                        y < topY;
                        y++
                ) {

                    BlockPos pos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );


                    /*
                     * 只有柱體「外殼」有哭泣黑曜石。
                     */
                    if (
                            isOuterShell(
                                    dx,
                                    dz,
                                    spike.radius()
                            )

                                    &&

                                    shouldUseCryingObsidian(
                                            spike,
                                            x,
                                            y,
                                            z
                                    )
                    ) {

                        setBlock(
                                level,
                                pos,
                                Blocks.CRYING_OBSIDIAN
                                        .defaultBlockState()
                        );

                    } else {

                        setBlock(
                                level,
                                pos,
                                Blocks.OBSIDIAN
                                        .defaultBlockState()
                        );
                    }
                }
            }
        }


        /*
         * 柱頂中央基岩。
         */
        BlockPos bedrockPos =
                new BlockPos(
                        spike.x(),
                        topY,
                        spike.z()
                );


        setBlock(
                level,
                bedrockPos,
                Blocks.BEDROCK
                        .defaultBlockState()
        );


        if (
                spike.hasCrystal()
        ) {

            EndCrystal crystal =
                    new EndCrystal(
                            level.getLevel(),
                            spike.x() + 0.5D,
                            topY + 1.0D,
                            spike.z() + 0.5D
                    );


            crystal.setYRot(
                    spike.crystalYaw()
            );


            level.addFreshEntity(
                    crystal
            );
        }
    }


    /*
     * ============================================================
     * Spike base
     * ============================================================
     */

    private static boolean hasValidSpikeBase(
            WorldGenLevel level,
            SpikeInfo spike,
            int baseY
    ) {

        for (
                int dx =
                -spike.radius();
                dx <= spike.radius();
                dx++
        ) {

            for (
                    int dz =
                    -spike.radius();
                    dz <= spike.radius();
                    dz++
            ) {

                if (
                        dx * dx
                                +
                                dz * dz

                                >

                                spike.radius()
                                        *
                                        spike.radius()
                                        +
                                        1
                ) {

                    continue;
                }


                BlockPos below =
                        new BlockPos(
                                spike.x() + dx,
                                baseY - 1,
                                spike.z() + dz
                        );


                if (
                        !level
                                .getBlockState(
                                        below
                                )
                                .is(
                                        Blocks.END_STONE
                                )
                ) {

                    return false;
                }
            }
        }


        return true;
    }


    /*
     * ============================================================
     * End Stone surface
     * ============================================================
     *
     * 不直接用 WORLD_SURFACE，
     * 因為未來島上有 Chorus Plant 時，
     * Heightmap 可能把植物也算進去。
     *
     * 我們真正需要的是最高一層 End Stone。
     */
    private static int findEndStoneSurface(
            WorldGenLevel level,
            int x,
            int z
    ) {

        int maxY =
                Math.min(
                        level.getMaxBuildHeight() - 1,
                        180
                );

        int minY =
                Math.max(
                        level.getMinBuildHeight(),
                        0
                );


        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();


        for (
                int y = maxY;
                y >= minY;
                y--
        ) {

            pos.set(
                    x,
                    y,
                    z
            );


            if (
                    level
                            .getBlockState(
                                    pos
                            )
                            .is(
                                    Blocks.END_STONE
                            )
            ) {

                return y + 1;
            }
        }


        return Integer.MIN_VALUE;
    }


    /*
     * ============================================================
     * Crying Obsidian
     * ============================================================
     */

    private static boolean isOuterShell(
            int dx,
            int dz,
            int radius
    ) {

        if (radius <= 1) {
            return true;
        }


        int inner =
                radius - 1;


        return dx * dx
                +
                dz * dz

                >=

                inner
                        *
                        inner;
    }


    private static boolean shouldUseCryingObsidian(
            SpikeInfo spike,
            int x,
            int y,
            int z
    ) {

        long hash =
                spike.blockSeed();

        hash ^=
                (long) x
                        *
                        341873128712L;

        hash ^=
                (long) y
                        *
                        132897987541L;

        hash ^=
                (long) z
                        *
                        42317861L;


        hash =
                mix64(
                        hash
                );


        double value =
                (
                        hash >>> 11
                )
                        *
                        0x1.0p-53;


        return value
                <
                spike.cryingChance();
    }


    /*
     * ============================================================
     * Random positions
     * ============================================================
     */

    private static Point randomPointInDisk(
            Random random,
            int centerX,
            int centerZ,
            double radius
    ) {

        double angle =
                random.nextDouble()
                        *
                        Math.PI
                        *
                        2.0D;


        /*
         * sqrt 保證在面積上均勻分布，
         * 而不是大量擠在中心。
         */
        double distance =
                Math.sqrt(
                        random.nextDouble()
                )
                        *
                        radius;


        return new Point(
                centerX
                        +
                        (int) Math.round(
                                Math.cos(angle)
                                        *
                                        distance
                        ),

                centerZ
                        +
                        (int) Math.round(
                                Math.sin(angle)
                                        *
                                        distance
                        )
        );
    }


    private static Point randomPointInAnnulus(
            Random random,
            int centerX,
            int centerZ,
            double minRadius,
            double maxRadius
    ) {

        double angle =
                random.nextDouble()
                        *
                        Math.PI
                        *
                        2.0D;


        double minSquared =
                minRadius
                        *
                        minRadius;

        double maxSquared =
                maxRadius
                        *
                        maxRadius;


        double distance =
                Math.sqrt(
                        minSquared
                                +
                                random.nextDouble()
                                        *
                                        (
                                                maxSquared
                                                        -
                                                        minSquared
                                        )
                );


        return new Point(
                centerX
                        +
                        (int) Math.round(
                                Math.cos(angle)
                                        *
                                        distance
                        ),

                centerZ
                        +
                        (int) Math.round(
                                Math.sin(angle)
                                        *
                                        distance
                        )
        );
    }


    /*
     * ============================================================
     * Chunk checks
     * ============================================================
     */

    private static boolean couldAffectChunk(
            NestIslandTerrainGenerator.IslandInfo island,
            ChunkPos chunk
    ) {

        double radius =
                island.radius()
                        +
                        16.0D;


        int nearestX =
                Math.max(
                        chunk.getMinBlockX(),
                        Math.min(
                                island.centerX(),
                                chunk.getMaxBlockX()
                        )
                );

        int nearestZ =
                Math.max(
                        chunk.getMinBlockZ(),
                        Math.min(
                                island.centerZ(),
                                chunk.getMaxBlockZ()
                        )
                );


        return distanceSquared(
                island.centerX(),
                island.centerZ(),
                nearestX,
                nearestZ
        )
                <=
                radius
                        *
                        radius;
    }


    private static boolean spikeIntersectsChunk(
            SpikeInfo spike,
            ChunkPos chunk
    ) {

        return spike.x()
                +
                spike.radius()

                >=

                chunk.getMinBlockX()

                &&

                spike.x()
                        -
                        spike.radius()

                        <=

                        chunk.getMaxBlockX()

                &&

                spike.z()
                        +
                        spike.radius()

                        >=

                        chunk.getMinBlockZ()

                &&

                spike.z()
                        -
                        spike.radius()

                        <=

                        chunk.getMaxBlockZ();
    }


    private static boolean isInsideChunk(
            int x,
            int z,
            ChunkPos chunk
    ) {

        return x >= chunk.getMinBlockX()
                &&
                x <= chunk.getMaxBlockX()
                &&
                z >= chunk.getMinBlockZ()
                &&
                z <= chunk.getMaxBlockZ();
    }


    /*
     * ============================================================
     * Utility
     * ============================================================
     */

    private static void setBlock(
            WorldGenLevel level,
            BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state
    ) {

        /*
         * 2：
         * 不做大量鄰居更新，
         * 適合 worldgen。
         */
        level.setBlock(
                pos,
                state,
                2
        );
    }


    private static double distanceSquared(
            int x1,
            int z1,
            int x2,
            int z2
    ) {

        double dx =
                x1 - x2;

        double dz =
                z1 - z2;


        return dx * dx
                +
                dz * dz;
    }


    private static double square(
            double value
    ) {

        return value
                *
                value;
    }


    private static int clamp(
            int value,
            int min,
            int max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }


    private static long mix64(
            long value
    ) {

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


    /*
     * ============================================================
     * Layout records
     * ============================================================
     */

    private record DecorationLayout(
            PortalInfo portal,
            PlatformInfo platform,
            List<SpikeInfo> spikes
    ) {
    }


    private record PortalInfo(
            int x,
            int y,
            int z
    ) {
    }


    private record PlatformInfo(
            int x,
            int z
    ) {
    }


    private record SpikeInfo(
            int x,
            int z,
            int radius,
            int height,
            boolean hasCrystal,
            boolean fallen,
            double fallenAngle,
            float crystalYaw,
            double cryingChance,
            long blockSeed
    ) {
    }


    private record Point(
            int x,
            int z
    ) {
    }
}
