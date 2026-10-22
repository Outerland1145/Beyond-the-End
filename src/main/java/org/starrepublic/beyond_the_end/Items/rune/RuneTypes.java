package org.starrepublic.beyond_the_end.Items.rune;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import org.starrepublic.beyond_the_end.Beyond_the_end;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RuneTypes {

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                Beyond_the_end.MODID,
                name
        );
    }

    // HEE 原始六種符文
    public static final ResourceLocation POWER =
            id("power");

    public static final ResourceLocation AGILITY =
            id("agility");

    public static final ResourceLocation VITALITY =
            id("vitality");

    public static final ResourceLocation DEFENSE =
            id("defense");

    public static final ResourceLocation MAGIC =
            id("magic");

    public static final ResourceLocation VOID =
            id("void");

    // 固定順序，用於創造模式物品欄
    public static final List<ResourceLocation> BUILTIN_ORDER =
            List.of(
                    POWER,
                    AGILITY,
                    VITALITY,
                    DEFENSE,
                    MAGIC,
                    VOID
            );

    // 六種原始符文直接硬編碼
    private static final Map<
            ResourceLocation,
            RuneDefinition
            > BUILTIN_RUNES = Map.of(
            POWER, new RuneDefinition(1),
            AGILITY, new RuneDefinition(2),
            VITALITY, new RuneDefinition(3),
            DEFENSE, new RuneDefinition(4),
            MAGIC, new RuneDefinition(5),
            VOID, new RuneDefinition(6)
    );

    // 資料包符文註冊表
    public static final ResourceKey<Registry<RuneDefinition>>
            RUNE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    id("rune_type")
            );

    public static void register(IEventBus modBus) {
        modBus.addListener(
                RuneTypes::registerDatapackRegistries
        );
    }

    private static void registerDatapackRegistries(
            DataPackRegistryEvent.NewRegistry event
    ) {
        event.dataPackRegistry(
                RUNE_REGISTRY_KEY,
                RuneDefinition.CODEC,
                RuneDefinition.CODEC
        );
    }

    /**
     * 檢查是不是原始六種符文。
     */
    public static boolean isBuiltin(ResourceLocation id) {
        return BUILTIN_RUNES.containsKey(id);
    }

    /**
     * 查詢符文定義。
     *
     * 優先使用硬編碼的原始符文。
     * 其他符文從目前世界的資料包註冊表取得。
     *
     * registries 可以為 null：
     * 此時僅能查詢原始六種符文。
     */
    public static Optional<RuneDefinition> find(
            ResourceLocation id,
            HolderLookup.Provider registries
    ) {

        RuneDefinition builtin = BUILTIN_RUNES.get(id);

        if (builtin != null) {
            return Optional.of(builtin);
        }

        if (registries == null) {
            return Optional.empty();
        }

        ResourceKey<RuneDefinition> key =
                ResourceKey.create(
                        RUNE_REGISTRY_KEY,
                        id
                );

        return registries.lookup(RUNE_REGISTRY_KEY)
                .flatMap(lookup -> lookup.get(key))
                .map(Holder::value);
    }

    /**
     * 判斷某個 ID 是否為合法符文。
     */
    public static boolean isValid(
            ResourceLocation id,
            HolderLookup.Provider registries
    ) {
        return find(id, registries).isPresent();
    }

    /**
     * 取得符文模型編號。
     *
     * 無效符文返回 0，顯示預設備用材質。
     */
    public static int getModelIndex(
            ResourceLocation id,
            HolderLookup.Provider registries
    ) {
        return find(id, registries)
                .map(RuneDefinition::modelIndex)
                .orElse(0);
    }

    private RuneTypes() {
    }
}