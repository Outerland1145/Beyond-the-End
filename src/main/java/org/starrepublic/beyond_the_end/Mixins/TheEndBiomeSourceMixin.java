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
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin {

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

        /*
         * 否則取得配置中的合法 biome。
         */
        List<Holder<Biome>> allowed =
                beyondTheEnd$getAllowedBiomes();

        /*
         * 如果配置全部無效：
         *
         * 強制 fallback。
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
         * 穩定地根據座標選 biome。
         */
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
}
