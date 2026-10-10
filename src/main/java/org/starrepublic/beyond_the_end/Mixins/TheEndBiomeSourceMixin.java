package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;

import net.minecraft.core.registries.Registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.biome.Biome;
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
     * Biome control
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
         * 世界生成機制關閉時：
         *
         * 完全不碰原版 biome。
         */
        if (
                !EndTerrainController
                        .isCustomTerrainEnabled()
        ) {
            return;
        }

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
         * ========================================================
         * NORMAL REGION
         * ========================================================
         *
         * 正常區完全不碰。
         */
        if (
                distance
                        <
                        EndTerrainController
                                .getNormalRadius()
        ) {

            Holder<Biome> datapackBiome =
                    beyondTheEnd$pickDatapackBiome(
                            blockX,
                            blockZ,
                            false
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
         * 強制 Beyond The End 的 Void biome。
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
         */

        Holder<Biome> datapackBiome =
                beyondTheEnd$pickDatapackBiome(
                        blockX,
                        blockZ,
                        true
                );

        if (datapackBiome != null) {

            cir.setReturnValue(
                    datapackBiome
            );

            return;
        }


        Holder<Biome> originalBiome =
                cir.getReturnValue();

        /*
         * 如果原本的 biome 就在白名單：
         *
         * 保留。
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


        int index =
                beyondTheEnd$selectBiome(
                        blockX,
                        blockZ,
                        allowed.size()
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
     * Deterministic biome distribution
     * ============================================================
     */

    @Unique
    private static int beyondTheEnd$selectBiome(
            int x,
            int z,
            int count
    ) {

        /*
         * 不直接用 block 座標，
         * 否則 biome 可能每幾格換一次。
         *
         * 這裡先切成 256x256 的區域。
         */
        int regionX =
                Math.floorDiv(
                        x,
                        256
                );

        int regionZ =
                Math.floorDiv(
                        z,
                        256
                );

        long hash =
                (long) regionX
                        *
                        341873128712L;

        hash ^=
                (long) regionZ
                        *
                        132897987541L;

        hash ^= hash >>> 13;

        hash *=
                1274126177L;

        hash ^= hash >>> 16;

        return Math.floorMod(
                hash,
                count
        );
    }
    @Unique
    private Holder<Biome> beyondTheEnd$pickDatapackBiome(
            int x,
            int z,
            boolean fractured
    ) {

        List<EndBiomeDataCache.Entry> entries =
                EndBiomeDataCache.entries();

        if (entries.isEmpty()) {
            return null;
        }


        double roll =
                beyondTheEnd$biomeRoll(
                        x,
                        z
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
    @Unique
    private static double beyondTheEnd$biomeRoll(
            int x,
            int z
    ) {

        int regionX =
                Math.floorDiv(
                        x,
                        256
                );

        int regionZ =
                Math.floorDiv(
                        z,
                        256
                );


        long hash =
                (long) regionX
                        *
                        341873128712L;

        hash ^=
                (long) regionZ
                        *
                        132897987541L;


        /*
         * 跟原本 fallback 選擇使用不同 salt，
         * 避免兩個 random 完全相同。
         */
        hash ^=
                0x6A09E667F3BCC909L;


        hash ^= hash >>> 30;

        hash *=
                0xBF58476D1CE4E5B9L;

        hash ^= hash >>> 27;

        hash *=
                0x94D049BB133111EBL;

        hash ^= hash >>> 31;


        /*
         * 轉成：
         *
         * 0.0 <= result < 1.0
         */
        return (
                (hash >>> 11)
                        *
                        0x1.0p-53
        );
    }
}
