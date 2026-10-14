package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;

import net.minecraft.resources.ResourceKey;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.starrepublic.beyond_the_end.World.worldgen.EndBiomeController;
import org.starrepublic.beyond_the_end.World.worldgen.EndTerrainController;
import org.starrepublic.beyond_the_end.World.worldgen.biome.VoidBiome;
import org.starrepublic.beyond_the_end.World.worldgen.biomedefinition.EndBiomeDataCache;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin {

    /*
     * ============================================================
     * World-seed derived salt
     * ============================================================
     *
     * TheEndBiomeSource 本身不保存 world seed。
     *
     * 但 getNoiseBiome() 會收到 Climate.Sampler，而 Sampler 內的
     * DensityFunction 是由目前世界的 RandomState / seed 建立的。
     *
     * 因此這裡從幾個固定座標的 climate sample 導出一個
     * 每個世界穩定、不同種子通常不同的 salt。
     *
     * 之後所有：
     *
     * - Domain Warp
     * - Voronoi site jitter
     * - datapack biome roll
     * - fractured fallback biome
     *
     * 都會混入這個 salt。
     *
     * 結果：
     *
     * 同一個世界 seed -> 群系分布保持一致。
     * 不同世界 seed   -> 群系版圖、邊界、cell 位置都不同。
     */
    @Unique
    private volatile boolean
            beyondTheEnd$worldSeedSaltInitialized = false;

    @Unique
    private volatile long
            beyondTheEnd$worldSeedSalt = 0L;

    /*
     * ============================================================
     * Organic biome distribution settings
     * ============================================================
     *
     * 這裡不再把世界切成固定 256x256 方格。
     *
     * 新算法：
     *
     * 1. 用平滑 Value Noise 扭曲 X/Z 座標。
     * 2. 在扭曲後的座標上建立帶抖動的 Voronoi cell。
     * 3. 每個 cell 再使用穩定 hash 決定 biome。
     *
     * 因此：
     *
     * - 同一片 biome 仍然會保持連續。
     * - generation_chance 仍然可以使用。
     * - 不再出現 256x256 的直角方格邊界。
     */

    /*
     * 群系區域的基本尺度。
     *
     * 數值越大：
     * - biome 平均面積越大。
     *
     * 數值越小：
     * - biome 分布越碎。
     */
    @Unique
    private static final int
            beyondTheEnd$BIOME_CELL_SIZE = 384;

    /*
     * Cell 中心可以偏離自身中心多少。
     *
     * 0.0 = 規則格點。
     * 1.0 = 可以在整個 cell 內大幅偏移。
     *
     * 不建議超過 0.85。
     */
    @Unique
    private static final double
            beyondTheEnd$BIOME_SITE_JITTER = 0.72D;

    /*
     * 大尺度座標扭曲。
     *
     * SCALE 越大，扭曲越平緩。
     * STRENGTH 越大，邊界彎曲越明顯。
     */
    @Unique
    private static final double
            beyondTheEnd$BIOME_WARP_SCALE = 640.0D;

    @Unique
    private static final double
            beyondTheEnd$BIOME_WARP_STRENGTH = 120.0D;

    /*
     * 不同用途使用不同 salt，避免：
     *
     * - X/Z 抖動完全相同。
     * - biome roll 和 fallback index 完全同步。
     */
    @Unique
    private static final long
            beyondTheEnd$SALT_WARP_X = 0x243F6A8885A308D3L;

    @Unique
    private static final long
            beyondTheEnd$SALT_WARP_Z = 0x13198A2E03707344L;

    @Unique
    private static final long
            beyondTheEnd$SALT_SITE_X = 0xA4093822299F31D0L;

    @Unique
    private static final long
            beyondTheEnd$SALT_SITE_Z = 0x082EFA98EC4E6C89L;

    @Unique
    private static final long
            beyondTheEnd$SALT_DATAPACK_ROLL = 0x452821E638D01377L;

    @Unique
    private static final long
            beyondTheEnd$SALT_FALLBACK = 0xBE5466CF34E90C6CL;


    /*
     * ============================================================
     * Possible biomes
     * ============================================================
     */

    @Inject(
            method = "collectPossibleBiomes",
            at = @At("RETURN"),
            cancellable = true
    )
    private void beyondTheEnd$addDatapackPossibleBiomes(
            CallbackInfoReturnable<Stream<Holder<Biome>>> cir
    ) {

        Stream<Holder<Biome>> original =
                cir.getReturnValue();

        Stream<Holder<Biome>> custom =
                EndBiomeDataCache.entries()
                        .stream()
                        .map(
                                EndBiomeDataCache.Entry::biome
                        );

        cir.setReturnValue(
                Stream.concat(
                        original,
                        custom
                ).distinct()
        );
    }


    /*
     * ============================================================
     * Biome control
     * ============================================================
     */

    @Inject(
            method = "getNoiseBiome",
            at = @At("RETURN"),
            cancellable = true
    )
    private void beyondTheEnd$controlBiome(
            int quartX,
            int quartY,
            int quartZ,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        /*
         * 先保存原版 TheEndBiomeSource 的結果。
         *
         * 這非常重要：
         * 此時 cir.getReturnValue() 還是原版判定的 biome。
         */
        Holder<Biome> originalBiome =
                cir.getReturnValue();
        /*
         * getNoiseBiome 收到的是 quart 座標。
         *
         * 1 quart = 4 blocks。
         */

        int blockX =
                QuartPos.toBlock(
                        quartX
                );

        int blockZ =
                QuartPos.toBlock(
                        quartZ
                );

        double distance =
                EndTerrainController
                        .distanceFromCenter(
                                blockX,
                                blockZ
                        );
        /*
         * 從目前世界的 Climate.Sampler 取得一個穩定的
         * seed-derived salt。
         *
         * 只會在這個 TheEndBiomeSource instance 第一次使用時建立一次，
         * 之後直接使用 cache。
         */
        long worldSeedSalt =
                beyondTheEnd$getWorldSeedSalt(
                        sampler
                );

        if (
                !EndTerrainController
                        .isCustomTerrainEnabled()
        ) {

            if (
                    originalBiome.is(
                            Biomes.THE_END
                    )
            ) {

                return;
            }

            Holder<Biome> datapackBiome =
                    beyondTheEnd$pickDatapackBiome(
                            blockX,
                            blockZ,
                            false,
                            worldSeedSalt
                    );

            if (datapackBiome != null) {

                cir.setReturnValue(
                        datapackBiome
                );
            }

            return;
        }


        /*
         * ========================================================
         * NORMAL REGION
         * ========================================================
         *
         * Datapack biome 可以正常參與生成。
         *
         * 沒有命中 datapack biome 時，保留原版 End biome。
         */
        if (
                distance
                        <
                        EndTerrainController
                                .getNormalRadius()
        ) {
            if (
                    originalBiome.is(
                            Biomes.THE_END
                    )
            ) {

                return;
            }

            Holder<Biome> datapackBiome =
                    beyondTheEnd$pickDatapackBiome(
                            blockX,
                            blockZ,
                            false,
                            worldSeedSalt
                    );

            if (datapackBiome != null) {

                cir.setReturnValue(
                        datapackBiome
                );
            }

            return;
        }


        /*
         * ========================================================
         * VOID REGION
         * ========================================================
         *
         * Void 區永遠強制 Beyond The End 的 Void biome。
         *
         * Datapack 不得覆蓋。
         */
        if (
                distance
                        >=
                        EndTerrainController
                                .getVoidRadius()
        ) {

            Holder<Biome> voidBiome =
                    EndBiomeController.getBiome(
                            VoidBiome.VOID
                    );

            if (voidBiome != null) {

                cir.setReturnValue(
                        voidBiome
                );
            }

            return;
        }


        /*
         * ========================================================
         * FRACTURED REGION
         * ========================================================
         *
         * Datapack biome 必須：
         *
         * 1. 存在於 End biome definition。
         * 2. 同時存在於 fracturedBiomeWhitelist。
         */

        Holder<Biome> datapackBiome =
                beyondTheEnd$pickDatapackBiome(
                        blockX,
                        blockZ,
                        true,
                        worldSeedSalt
                );

        if (datapackBiome != null) {

            cir.setReturnValue(
                    datapackBiome
            );

            return;
        }

        /*
         * 原版計算出的 biome 本身就在白名單：
         *
         * 保留原結果。
         */
        if (
                beyondTheEnd$isWhitelisted(
                        originalBiome
                )
        ) {

            return;
        }


        List<Holder<Biome>> allowed =
                beyondTheEnd$getAllowedBiomes();

        /*
         * 白名單全部無效時使用固定 fallback。
         */
        if (allowed.isEmpty()) {

            Holder<Biome> fallback =
                    EndBiomeController.getBiome(
                            "minecraft:end_barrens"
                    );

            if (fallback != null) {

                cir.setReturnValue(
                        fallback
                );
            }

            return;
        }


        /*
         * 原本這裡是 256x256 方格 hash。
         *
         * 現在改成和 datapack biome 一樣使用
         * warped Voronoi cell，避免破碎區 fallback biome
         * 再次出現方格邊界。
         */
        int index =
                beyondTheEnd$selectBiome(
                        blockX,
                        blockZ,
                        allowed.size(),
                        worldSeedSalt
                );

        cir.setReturnValue(
                allowed.get(index)
        );
    }


    /*
     * ============================================================
     * Whitelist check
     * ============================================================
     */

    @Unique
    private boolean beyondTheEnd$isWhitelisted(
            Holder<Biome> biome
    ) {

        Optional<ResourceKey<Biome>> key =
                biome.unwrapKey();

        if (key.isEmpty()) {
            return false;
        }

        String id =
                key.get()
                        .location()
                        .toString();

        for (
                String allowed :
                BTEConfigs.SERVER
                        .fracturedBiomeWhitelist
                        .get()
        ) {

            if (
                    id.equals(
                            allowed
                    )
            ) {

                return true;
            }
        }

        return false;
    }


    /*
     * ============================================================
     * Resolve config biome IDs
     * ============================================================
     */

    @Unique
    private List<Holder<Biome>>
    beyondTheEnd$getAllowedBiomes() {

        List<Holder<Biome>> result =
                new ArrayList<>();

        for (
                String id :
                BTEConfigs.SERVER
                        .fracturedBiomeWhitelist
                        .get()
        ) {

            Holder<Biome> biome =
                    EndBiomeController.getBiome(
                            id
                    );

            if (biome != null) {

                result.add(
                        biome
                );
            }
        }

        return result;
    }


    /*
     * ============================================================
     * Datapack biome selection
     * ============================================================
     */

    @Unique
    private Holder<Biome> beyondTheEnd$pickDatapackBiome(
            int x,
            int z,
            boolean fractured,
            long worldSeedSalt
    ) {

        List<EndBiomeDataCache.Entry> entries =
                EndBiomeDataCache.entries();

        if (entries.isEmpty()) {
            return null;
        }


        /*
         * 找到目前座標所屬的「自然群系區域」。
         *
         * 回傳值把 cellX / cellZ 打包進一個 long。
         */
        long cell =
                beyondTheEnd$findBiomeCell(
                        x,
                        z,
                        worldSeedSalt
                );

        int cellX =
                (int) (cell >> 32);

        int cellZ =
                (int) cell;


        /*
         * 每一個自然 cell 只有一個穩定 roll。
         *
         * 因此同一塊區域不會每幾格切一次 biome。
         */
        double roll =
                beyondTheEnd$hashToUnit(
                        cellX,
                        cellZ,
                        beyondTheEnd$SALT_DATAPACK_ROLL,
                        worldSeedSalt
                );

        double accumulated =
                0.0D;


        for (
                EndBiomeDataCache.Entry entry :
                entries
        ) {

            /*
             * Fractured：
             *
             * JSON 裡有還不夠，
             * config whitelist 也必須允許。
             */
            if (
                    fractured
                            &&
                            !beyondTheEnd$isWhitelisted(
                                    entry.biome()
                            )
            ) {

                continue;
            }


            accumulated +=
                    entry.chance();


            if (
                    roll
                            <
                            accumulated
            ) {

                return entry.biome();
            }
        }


        /*
         * 沒有任何 datapack biome 命中。
         *
         * 回到原本 biome 生成。
         */
        return null;
    }


    /*
     * ============================================================
     * Fractured fallback biome selection
     * ============================================================
     *
     * 原本使用：
     *
     * floorDiv(x, 256)
     * floorDiv(z, 256)
     *
     * 因此天然產生正方形群系。
     *
     * 現在直接使用自然 cell。
     */

    @Unique
    private static int beyondTheEnd$selectBiome(
            int x,
            int z,
            int count,
            long worldSeedSalt
    ) {

        long cell =
                beyondTheEnd$findBiomeCell(
                        x,
                        z,
                        worldSeedSalt
                );

        int cellX =
                (int) (cell >> 32);

        int cellZ =
                (int) cell;

        long hash =
                beyondTheEnd$hash(
                        cellX,
                        cellZ,
                        beyondTheEnd$SALT_FALLBACK,
                        worldSeedSalt
                );

        return Math.floorMod(
                hash,
                count
        );
    }


    /*
     * ============================================================
     * Organic biome cell
     * ============================================================
     *
     * 核心流程：
     *
     * 原始世界座標
     *      ↓
     * 平滑 Noise 扭曲座標
     *      ↓
     * 找附近 3x3 cell 的抖動中心
     *      ↓
     * 取最近中心
     *      ↓
     * 得到一個不規則、連續的 biome region
     */

    @Unique
    private static long beyondTheEnd$findBiomeCell(
            int x,
            int z,
            long worldSeedSalt
    ) {

        /*
         * ----------------------------------------
         * Domain Warp
         * ----------------------------------------
         *
         * 使用平滑 noise 彎曲整個 Voronoi 網格。
         *
         * 這一步是消除「筆直多邊形邊界」的關鍵。
         */
        double warpX =
                (
                        beyondTheEnd$smoothNoise(
                                x / beyondTheEnd$BIOME_WARP_SCALE,
                                z / beyondTheEnd$BIOME_WARP_SCALE,
                                beyondTheEnd$SALT_WARP_X,
                                worldSeedSalt
                        )
                                -
                                0.5D
                )
                        *
                        2.0D
                        *
                        beyondTheEnd$BIOME_WARP_STRENGTH;

        double warpZ =
                (
                        beyondTheEnd$smoothNoise(
                                x / beyondTheEnd$BIOME_WARP_SCALE,
                                z / beyondTheEnd$BIOME_WARP_SCALE,
                                beyondTheEnd$SALT_WARP_Z,
                                worldSeedSalt
                        )
                                -
                                0.5D
                )
                        *
                        2.0D
                        *
                        beyondTheEnd$BIOME_WARP_STRENGTH;


        double warpedX =
                x + warpX;

        double warpedZ =
                z + warpZ;


        int baseCellX =
                beyondTheEnd$fastFloor(
                        warpedX
                                /
                                beyondTheEnd$BIOME_CELL_SIZE
                );

        int baseCellZ =
                beyondTheEnd$fastFloor(
                        warpedZ
                                /
                                beyondTheEnd$BIOME_CELL_SIZE
                );


        int bestCellX =
                baseCellX;

        int bestCellZ =
                baseCellZ;

        double bestDistance =
                Double.POSITIVE_INFINITY;


        /*
         * 只需要檢查附近 3x3 cell。
         *
         * Site jitter 被限制在自身 cell 附近，
         * 所以最近中心不會跳到很遠的 cell。
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
                        baseCellX + offsetX;

                int currentCellZ =
                        baseCellZ + offsetZ;


                double jitterX =
                        (
                                beyondTheEnd$hashToUnit(
                                        currentCellX,
                                        currentCellZ,
                                        beyondTheEnd$SALT_SITE_X,
                                        worldSeedSalt
                                )
                                        -
                                        0.5D
                        )
                                *
                                beyondTheEnd$BIOME_CELL_SIZE
                                *
                                beyondTheEnd$BIOME_SITE_JITTER;

                double jitterZ =
                        (
                                beyondTheEnd$hashToUnit(
                                        currentCellX,
                                        currentCellZ,
                                        beyondTheEnd$SALT_SITE_Z,
                                        worldSeedSalt
                                )
                                        -
                                        0.5D
                        )
                                *
                                beyondTheEnd$BIOME_CELL_SIZE
                                *
                                beyondTheEnd$BIOME_SITE_JITTER;


                double siteX =
                        (
                                currentCellX
                                        +
                                        0.5D
                        )
                                *
                                beyondTheEnd$BIOME_CELL_SIZE
                                +
                                jitterX;

                double siteZ =
                        (
                                currentCellZ
                                        +
                                        0.5D
                        )
                                *
                                beyondTheEnd$BIOME_CELL_SIZE
                                +
                                jitterZ;


                double deltaX =
                        warpedX - siteX;

                double deltaZ =
                        warpedZ - siteZ;

                double distance =
                        deltaX * deltaX
                                +
                                deltaZ * deltaZ;


                if (
                        distance
                                <
                                bestDistance
                ) {

                    bestDistance =
                            distance;

                    bestCellX =
                            currentCellX;

                    bestCellZ =
                            currentCellZ;
                }
            }
        }


        /*
         * 高 32 bits = cellX
         * 低 32 bits = cellZ
         */
        return (
                (long) bestCellX
                        <<
                        32
        )
                |
                (
                        bestCellZ
                                &
                                0xFFFFFFFFL
                );
    }


    /*
     * ============================================================
     * Smooth value noise
     * ============================================================
     *
     * Hash 本身是不連續的。
     *
     * 這裡取四個格點的 hash 值，再使用 quintic fade
     * 做雙線性插值，得到連續平滑的 2D noise。
     */

    @Unique
    private static double beyondTheEnd$smoothNoise(
            double x,
            double z,
            long salt,
            long worldSeedSalt
    ) {

        int x0 =
                beyondTheEnd$fastFloor(
                        x
                );

        int z0 =
                beyondTheEnd$fastFloor(
                        z
                );

        int x1 =
                x0 + 1;

        int z1 =
                z0 + 1;


        double localX =
                x - x0;

        double localZ =
                z - z0;


        double fadeX =
                beyondTheEnd$fade(
                        localX
                );

        double fadeZ =
                beyondTheEnd$fade(
                        localZ
                );


        double value00 =
                beyondTheEnd$hashToUnit(
                        x0,
                        z0,
                        salt,
                        worldSeedSalt
                );

        double value10 =
                beyondTheEnd$hashToUnit(
                        x1,
                        z0,
                        salt,
                        worldSeedSalt
                );

        double value01 =
                beyondTheEnd$hashToUnit(
                        x0,
                        z1,
                        salt,
                        worldSeedSalt
                );

        double value11 =
                beyondTheEnd$hashToUnit(
                        x1,
                        z1,
                        salt,
                        worldSeedSalt
                );


        double top =
                beyondTheEnd$lerp(
                        fadeX,
                        value00,
                        value10
                );

        double bottom =
                beyondTheEnd$lerp(
                        fadeX,
                        value01,
                        value11
                );


        return beyondTheEnd$lerp(
                fadeZ,
                top,
                bottom
        );
    }


    /*
     * Quintic fade：
     *
     * 6t^5 - 15t^4 + 10t^3
     *
     * 比普通 smoothstep 更適合 noise interpolation，
     * 一階、二階導數在邊界都會平滑。
     */
    @Unique
    private static double beyondTheEnd$fade(
            double value
    ) {

        return value
                *
                value
                *
                value
                *
                (
                        value
                                *
                                (
                                        value
                                                *
                                                6.0D
                                                -
                                                15.0D
                                )
                                +
                                10.0D
                );
    }


    @Unique
    private static double beyondTheEnd$lerp(
            double delta,
            double start,
            double end
    ) {

        return start
                +
                delta
                        *
                        (
                                end
                                        -
                                        start
                        );
    }


    @Unique
    private static int beyondTheEnd$fastFloor(
            double value
    ) {

        int integer =
                (int) value;

        return value
                <
                integer
                ? integer - 1
                : integer;
    }


    /*
     * ============================================================
     * World seed salt
     * ============================================================
     *
     * TheEndBiomeSource 沒有直接保存 world seed，
     * 所以使用 seed-dependent Climate.Sampler 建立一個 fingerprint。
     *
     * 這裡故意取多個固定座標，並混合 TargetPoint 的六個 climate 值。
     * 即使其中某些 End climate channel 接近常數，
     * 其他 seed-dependent channel 仍然可以參與 fingerprint。
     *
     * 注意：
     *
     * - 固定座標 -> 同一世界每次結果完全一致。
     * - Sampler 由 RandomState / world seed 建立。
     * - 不依賴目前玩家或 chunk 座標，所以 salt 是全世界統一的。
     */
    @Unique
    private long beyondTheEnd$getWorldSeedSalt(
            Climate.Sampler sampler
    ) {

        if (
                beyondTheEnd$worldSeedSaltInitialized
        ) {

            return beyondTheEnd$worldSeedSalt;
        }

        synchronized (this) {

            if (
                    beyondTheEnd$worldSeedSaltInitialized
            ) {

                return beyondTheEnd$worldSeedSalt;
            }


            Climate.TargetPoint pointA =
                    sampler.sample(
                            0,
                            0,
                            0
                    );

            Climate.TargetPoint pointB =
                    sampler.sample(
                            1024,
                            0,
                            -1024
                    );

            Climate.TargetPoint pointC =
                    sampler.sample(
                            -2048,
                            0,
                            1536
                    );

            Climate.TargetPoint pointD =
                    sampler.sample(
                            3072,
                            0,
                            2560
                    );


            long value =
                    0x9E3779B97F4A7C15L;

            value =
                    beyondTheEnd$mixClimatePoint(
                            value,
                            pointA
                    );

            value =
                    beyondTheEnd$mixClimatePoint(
                            value,
                            pointB
                    );

            value =
                    beyondTheEnd$mixClimatePoint(
                            value,
                            pointC
                    );

            value =
                    beyondTheEnd$mixClimatePoint(
                            value,
                            pointD
                    );


            /*
             * 再做一次 avalanche，
             * 避免 climate 值彼此相關時留下規律。
             */
            value =
                    beyondTheEnd$mix64(
                            value
                                    ^
                                    0xD1B54A32D192ED03L
                    );


            beyondTheEnd$worldSeedSalt =
                    value;

            /*
             * volatile write 放最後，
             * 確保其他 worldgen thread 看到 initialized=true 時，
             * worldSeedSalt 已經完整寫入。
             */
            beyondTheEnd$worldSeedSaltInitialized =
                    true;


            return value;
        }
    }


    @Unique
    private static long beyondTheEnd$mixClimatePoint(
            long seed,
            Climate.TargetPoint point
    ) {

        long value =
                seed;

        value =
                beyondTheEnd$mix64(
                        value
                                ^
                                point.temperature()
                                ^
                                0x243F6A8885A308D3L
                );

        value =
                beyondTheEnd$mix64(
                        value
                                ^
                                Long.rotateLeft(
                                        point.humidity(),
                                        7
                                )
                                ^
                                0x13198A2E03707344L
                );

        value =
                beyondTheEnd$mix64(
                        value
                                ^
                                Long.rotateLeft(
                                        point.continentalness(),
                                        13
                                )
                                ^
                                0xA4093822299F31D0L
                );

        value =
                beyondTheEnd$mix64(
                        value
                                ^
                                Long.rotateLeft(
                                        point.erosion(),
                                        21
                                )
                                ^
                                0x082EFA98EC4E6C89L
                );

        value =
                beyondTheEnd$mix64(
                        value
                                ^
                                Long.rotateLeft(
                                        point.depth(),
                                        29
                                )
                                ^
                                0x452821E638D01377L
                );

        value =
                beyondTheEnd$mix64(
                        value
                                ^
                                Long.rotateLeft(
                                        point.weirdness(),
                                        37
                                )
                                ^
                                0xBE5466CF34E90C6CL
                );

        return value;
    }


    /*
     * ============================================================
     * Stable seed-aware hash
     * ============================================================
     */

    @Unique
    private static long beyondTheEnd$hash(
            int x,
            int z,
            long salt,
            long worldSeedSalt
    ) {

        long value =
                worldSeedSalt;

        value ^=
                (long) x
                        *
                        341873128712L;

        value ^=
                (long) z
                        *
                        132897987541L;

        value ^=
                salt;


        return beyondTheEnd$mix64(
                value
        );
    }


    /*
     * SplitMix64 finalizer。
     *
     * 將輸入的 bit pattern 做 avalanche，
     * 讓 world seed、cell 座標與 salt 都能充分影響輸出。
     */
    @Unique
    private static long beyondTheEnd$mix64(
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


    @Unique
    private static double beyondTheEnd$hashToUnit(
            int x,
            int z,
            long salt,
            long worldSeedSalt
    ) {

        long hash =
                beyondTheEnd$hash(
                        x,
                        z,
                        salt,
                        worldSeedSalt
                );

        /*
         * 轉成：
         *
         * 0.0 <= result < 1.0
         */
        return (
                hash >>> 11
        )
                *
                0x1.0p-53;
    }
}


