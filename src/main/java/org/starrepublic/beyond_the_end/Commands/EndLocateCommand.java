package org.starrepublic.beyond_the_end.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;
import org.starrepublic.beyond_the_end.World.worldgen.feature.NestIslandTerrainGenerator;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;


public final class EndLocateCommand {
    private static final int DEFAULT_NEST_SEARCH_RADIUS = 1_000_000;

    private static final int MAX_NEST_SEARCH_RADIUS = 29_000_000;

    private EndLocateCommand() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher){

        dispatcher.register(

                Commands.literal("beyondtheend")

                        /*
                         * 權限等級 2：
                         *
                         * 和一般管理 / 測試指令一致。
                         */
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )

                        /*
                         * /bte locate_nest
                         */
                        .then(

                                Commands.literal(
                                                "locate_nest"
                                        )

                                        /*
                                         * 不帶參數。
                                         *
                                         * 預設搜尋 1,000,000 格。
                                         */
                                        .executes(
                                                context ->
                                                        locateNest(
                                                                context.getSource(),
                                                                DEFAULT_NEST_SEARCH_RADIUS
                                                        )
                                        )

                                        /*
                                         * /bte locate_nest <radius>
                                         */
                                        .then(

                                                Commands.argument(
                                                                "radius",
                                                                IntegerArgumentType.integer(
                                                                        1,
                                                                        MAX_NEST_SEARCH_RADIUS
                                                                )
                                                        )

                                                        .executes(
                                                                context -> {

                                                                    int radius =
                                                                            IntegerArgumentType
                                                                                    .getInteger(
                                                                                            context,
                                                                                            "radius"
                                                                                    );

                                                                    return locateNest(
                                                                            context.getSource(),
                                                                            radius
                                                                    );
                                                                }
                                                        )
                                        )
                        )
        );
    }


    /*
     * ============================================================
     * /bte locate_nest
     * ============================================================
     */

    private static int locateNest(
            CommandSourceStack source,
            int searchRadius
    ) {

        /*
         * ========================================================
         * 必須啟用自訂終界生成
         * ========================================================
         */

        if (
                !EndTerrainController
                        .isCustomTerrainEnabled()
        ) {

            source.sendFailure(
                    Component.literal(
                            "[Beyond The End] 自訂終界地形目前沒有啟用。"
                    )
            );

            return 0;
        }


        /*
         * ========================================================
         * 必須在終界使用
         * ========================================================
         *
         * 這樣搜尋起點的座標與距離才有明確意義。
         */
        if (
                source
                        .getLevel()
                        .dimension()
                        !=
                        Level.END
        ) {

            source.sendFailure(
                    Component.literal(
                            "[Beyond The End] locate_nest 只能在終界使用。"
                    )
            );

            return 0;
        }


        /*
         * ========================================================
         * 搜尋起點
         * ========================================================
         */

        int startX =
                Mth.floor(
                        source
                                .getPosition()
                                .x
                );

        int startZ =
                Mth.floor(
                        source
                                .getPosition()
                                .z
                );


        long worldSeed =
                source
                        .getLevel()
                        .getSeed();


        double voidRadius =
                EndTerrainController
                        .getVoidRadius();


        /*
         * ========================================================
         * Search
         * ========================================================
         */

        Optional<NestSearchResult> result =
                findNearestNest(
                        worldSeed,
                        startX,
                        startZ,
                        searchRadius,
                        voidRadius
                );


        /*
         * ========================================================
         * 沒找到
         * ========================================================
         */

        if (
                result.isEmpty()
        ) {

            source.sendFailure(
                    Component.literal(
                            "[Beyond The End] 在 "
                                    +
                                    searchRadius
                                    +
                                    " 格範圍內沒有找到巢穴遺跡。"
                    )
            );

            return 0;
        }


        /*
         * ========================================================
         * 找到
         * ========================================================
         */

        NestSearchResult searchResult =
                result.get();


        NestIslandTerrainGenerator.IslandInfo island =
                searchResult.island();


        int centerX =
                island.centerX();

        int centerZ =
                island.centerZ();


        int distance =
                (int) Math.round(
                        Math.sqrt(
                                searchResult.distanceSquared()
                        )
                );


        int radius =
                (int) Math.round(
                        island.radius()
                );


        /*
         * 第一行：
         * 位置
         */
        source.sendSuccess(
                () ->
                        Component.literal(
                                "[Beyond The End] 找到巢穴遺跡："
                                        +
                                        "["
                                        +
                                        centerX
                                        +
                                        ", ~, "
                                        +
                                        centerZ
                                        +
                                        "]"
                        ),
                false
        );


        /*
         * 第二行：
         * 詳細資訊
         */
        source.sendSuccess(
                () ->
                        Component.literal(
                                "距離：約 "
                                        +
                                        distance
                                        +
                                        " 格"
                                        +
                                        " | 島嶼半徑：約 "
                                        +
                                        radius
                                        +
                                        " 格"
                        ),
                false
        );


        /*
         * 第三行：
         * 測試用傳送指令。
         *
         * Y=120，
         * 避免直接塞進島內。
         */
        source.sendSuccess(
                () ->
                        Component.literal(
                                "測試傳送："
                                        +
                                        "/tp @s "
                                        +
                                        centerX
                                        +
                                        " 120 "
                                        +
                                        centerZ
                        ),
                false
        );


        return 1;
    }


    /*
     * ============================================================
     * Nearest Nest Search
     * ============================================================
     *
     * 這裡沒有：
     *
     * - getChunk()
     * - loadChunk()
     * - /locate
     * - StructureManager
     *
     * 完全只是數學搜尋。
     *
     * 因此搜尋幾百萬格也不會生成沿途 Chunk。
     */
    private static Optional<NestSearchResult> findNearestNest(
            long worldSeed,
            int startX,
            int startZ,
            int maxRadius,
            double voidRadius
    ) {

        int regionSize =
                NestIslandTerrainGenerator.REGION_SIZE;


        int startRegionX =
                Math.floorDiv(
                        startX,
                        regionSize
                );

        int startRegionZ =
                Math.floorDiv(
                        startZ,
                        regionSize
                );


        long maxDistanceSquared =
                (long) maxRadius
                        *
                        maxRadius;


        /*
         * ========================================================
         * Priority Queue
         * ========================================================
         *
         * 不是：
         *
         * ring 0
         * ring 1
         * ring 2
         * ...
         *
         * 而是永遠先檢查：
         *
         * 「理論上最可能離玩家最近的 Region」
         *
         * 因此找到結果後可以安全提前停止。
         */
        PriorityQueue<RegionSearchNode> open =
                new PriorityQueue<>(
                        Comparator.comparingLong(
                                RegionSearchNode::minimumDistanceSquared
                        )
                );


        Set<Long> visited =
                new HashSet<>();


        /*
         * 起始 Region。
         */
        addRegion(
                open,
                visited,
                startRegionX,
                startRegionZ,
                startX,
                startZ,
                maxDistanceSquared
        );


        NestIslandTerrainGenerator.IslandInfo bestIsland =
                null;


        long bestDistanceSquared =
                Long.MAX_VALUE;


        /*
         * ========================================================
         * Search loop
         * ========================================================
         */

        while (
                !open.isEmpty()
        ) {

            RegionSearchNode node =
                    open.poll();


            /*
             * 這個 Region 的任何位置
             * 都已經超出玩家指定的搜尋半徑。
             *
             * Queue 後面的只會更遠。
             */
            if (
                    node.minimumDistanceSquared()
                            >
                            maxDistanceSquared
            ) {

                break;
            }


            /*
             * 如果已經找到島，
             *
             * 而目前最靠近的「尚未檢查 Region」
             * 理論最短距離都比島心更遠，
             *
             * 就可以確定目前島嶼是最近的。
             */
            if (
                    bestIsland
                            !=
                            null

                            &&

                            node.minimumDistanceSquared()
                                    >
                                    bestDistanceSquared
            ) {

                break;
            }


            /*
             * ====================================================
             * 使用真正的島嶼生成算法
             * ====================================================
             *
             * 這非常重要。
             *
             * 指令不自己重新亂數一次。
             */
            NestIslandTerrainGenerator.IslandInfo island =
                    NestIslandTerrainGenerator
                            .getIslandForRegion(
                                    worldSeed,
                                    node.regionX(),
                                    node.regionZ()
                            );


            if (
                    island
                            !=
                            null
            ) {

                /*
                 * 和地形生成器使用相同的：
                 *
                 * 「整座島必須位於虛空區」
                 *
                 * 判定。
                 */
                if (
                        NestIslandTerrainGenerator.isValidIsland(
                                island,
                                voidRadius
                        )
                ) {

                    long dx =
                            (long) island.centerX()
                                    -
                                    startX;


                    long dz =
                            (long) island.centerZ()
                                    -
                                    startZ;


                    long distanceSquared =
                            dx * dx
                                    +
                                    dz * dz;


                    /*
                     * 必須在指定搜尋半徑內。
                     */
                    if (
                            distanceSquared
                                    <=
                                    maxDistanceSquared
                    ) {

                        /*
                         * 比目前最佳結果更近。
                         */
                        if (
                                distanceSquared
                                        <
                                        bestDistanceSquared
                        ) {

                            bestDistanceSquared =
                                    distanceSquared;

                            bestIsland =
                                    island;
                        }
                    }
                }
            }


            /*
             * ====================================================
             * 加入周圍 8 個 Region
             * ====================================================
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

                    if (
                            offsetX == 0
                                    &&
                                    offsetZ == 0
                    ) {

                        continue;
                    }


                    addRegion(
                            open,
                            visited,
                            node.regionX()
                                    +
                                    offsetX,
                            node.regionZ()
                                    +
                                    offsetZ,
                            startX,
                            startZ,
                            maxDistanceSquared
                    );
                }
            }
        }


        if (
                bestIsland
                        ==
                        null
        ) {

            return Optional.empty();
        }


        return Optional.of(
                new NestSearchResult(
                        bestIsland,
                        bestDistanceSquared
                )
        );
    }


    /*
     * ============================================================
     * Add search region
     * ============================================================
     */

    private static void addRegion(
            PriorityQueue<RegionSearchNode> open,
            Set<Long> visited,
            int regionX,
            int regionZ,
            int startX,
            int startZ,
            long maxDistanceSquared
    ) {

        long key =
                regionKey(
                        regionX,
                        regionZ
                );


        /*
         * 已經加入過 Queue。
         */
        if (
                !visited.add(key)
        ) {

            return;
        }


        long minimumDistanceSquared =
                minimumDistanceToRegionSquared(
                        startX,
                        startZ,
                        regionX,
                        regionZ
                );


        /*
         * 整個 Region 都已經在搜尋半徑外。
         *
         * 沒必要加入 Queue。
         */
        if (
                minimumDistanceSquared
                        >
                        maxDistanceSquared
        ) {

            return;
        }


        open.add(
                new RegionSearchNode(
                        regionX,
                        regionZ,
                        minimumDistanceSquared
                )
        );
    }


    /*
     * ============================================================
     * Region minimum distance
     * ============================================================
     *
     * 計算：
     *
     * 玩家座標
     *
     * ↓
     *
     * 到某個 8192×8192 Region 方框
     *
     * ↓
     *
     * 理論最短距離
     *
     *
     * 如果玩家本身就在 Region 裡：
     *
     * distance = 0
     */
    private static long minimumDistanceToRegionSquared(
            int startX,
            int startZ,
            int regionX,
            int regionZ
    ) {

        long regionSize =
                NestIslandTerrainGenerator.REGION_SIZE;


        long minX =
                (long) regionX
                        *
                        regionSize;


        long minZ =
                (long) regionZ
                        *
                        regionSize;


        long maxX =
                minX
                        +
                        regionSize
                        -
                        1L;


        long maxZ =
                minZ
                        +
                        regionSize
                        -
                        1L;


        long dx;


        if (
                startX
                        <
                        minX
        ) {

            dx =
                    minX
                            -
                            startX;

        } else if (
                startX
                        >
                        maxX
        ) {

            dx =
                    startX
                            -
                            maxX;

        } else {

            dx = 0L;
        }


        long dz;


        if (
                startZ
                        <
                        minZ
        ) {

            dz =
                    minZ
                            -
                            startZ;

        } else if (
                startZ
                        >
                        maxZ
        ) {

            dz =
                    startZ
                            -
                            maxZ;

        } else {

            dz = 0L;
        }


        return dx * dx
                +
                dz * dz;
    }


    /*
     * ============================================================
     * Region key
     * ============================================================
     */

    private static long regionKey(
            int regionX,
            int regionZ
    ) {

        return (
                (long) regionX
                        <<
                        32
        )
                ^
                (
                        regionZ
                                &
                                0xFFFFFFFFL
                );
    }


    /*
     * ============================================================
     * Internal records
     * ============================================================
     */

    /**
     * 搜尋 Queue 裡的一個 Region。
     */
    private record RegionSearchNode(
            int regionX,
            int regionZ,
            long minimumDistanceSquared
    ) {
    }


    /**
     * 最終搜尋結果。
     */
    private record NestSearchResult(
            NestIslandTerrainGenerator.IslandInfo island,
            long distanceSquared
    ) {
    }
}
