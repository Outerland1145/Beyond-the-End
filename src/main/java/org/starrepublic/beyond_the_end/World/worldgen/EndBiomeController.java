package org.starrepublic.beyond_the_end.World.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Optional;

public final class EndBiomeController {

    private EndBiomeController() {
    }

    /**
     * 從目前 Server 的 dynamic biome registry
     * 取得 biome holder。
     *
     * Biome 不是 BuiltInRegistry，
     * 所以不要用 BuiltInRegistries 查。
     */
    public static Holder<Biome> getBiome(
            ResourceKey<Biome> biomeKey
    ) {

        var server =
                ServerLifecycleHooks.getCurrentServer();

        /*
         * 在遊戲還沒真正啟動 Server 時，
         * 可能是 null。
         */
        if (server == null) {
            return null;
        }

        Registry<Biome> biomeRegistry =
                server
                        .registryAccess()
                        .registryOrThrow(
                                Registries.BIOME
                        );

        Optional<Holder.Reference<Biome>> holder =
                biomeRegistry.getHolder(
                        biomeKey
                );

        return holder.orElse(null);
    }

    public static Holder<Biome> getBiome(
            ResourceLocation id
    ) {

        ResourceKey<Biome> key =
                ResourceKey.create(
                        Registries.BIOME,
                        id
                );

        return getBiome(key);
    }

    public static Holder<Biome> getBiome(
            String id
    ) {

        ResourceLocation location =
                ResourceLocation.tryParse(id);

        if (location == null) {
            return null;
        }

        return getBiome(location);
    }
}
