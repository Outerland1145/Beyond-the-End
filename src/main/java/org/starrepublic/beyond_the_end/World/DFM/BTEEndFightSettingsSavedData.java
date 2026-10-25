package org.starrepublic.beyond_the_end.World.DFM;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BTEEndFightSettingsSavedData extends SavedData {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BTEEndFightSettingsSavedData.class);

    private static final String DATA_NAME =
            "beyond_the_end_dragon_fight_settings";

    private static final String SPIKE_COUNT = "SpikeCount";
    private static final String SPIKE_RADIUS = "SpikeRadius";
    private static final String GATEWAY_COUNT = "GatewayCount";
    private static final String GATEWAY_RADIUS = "GatewayRadius";
    private static final String GUARDED_CRYSTAL_COUNT = "GuardedCrystalCount";
    private static final String CUSTOM_SPIKE_STRUCTURES = "CustomSpikeStructures";
    private boolean dragonFightSealed = false;
    private boolean dragonFightSealing = false;
    private int sealingNextSpike = 0;

    private final List<UUID> sealingCrystalIds = new ArrayList<>();



    private static final SavedData.Factory<BTEEndFightSettingsSavedData>
            FACTORY = new SavedData.Factory<>(
            BTEEndFightSettingsSavedData::create,
            BTEEndFightSettingsSavedData::load
    );
    private final boolean customSpikeStructures;
    private final int spikeCount;
    private final int spikeRadius;
    private final int gatewayCount;
    private final int gatewayRadius;
    private final int guardedCrystalCount;

    private BTEEndFightSettingsSavedData(
            int spikeCount,
            int spikeRadius,
            int gatewayCount,
            int gatewayRadius,
            int guardedCrystalCount,
            boolean customSpikeStructures
    ) {
        this.spikeCount = spikeCount;
        this.spikeRadius = spikeRadius;
        this.gatewayCount = gatewayCount;
        this.gatewayRadius = gatewayRadius;
        this.guardedCrystalCount = Math.clamp(guardedCrystalCount, 2, spikeCount);
        this.customSpikeStructures = customSpikeStructures;
    }

    /**
     * 只有沒有世界存檔資料時才呼叫。
     */
    private static BTEEndFightSettingsSavedData create() {

        BTEEndFightSettingsSavedData data =
                new BTEEndFightSettingsSavedData(
                        BTEConfigs.SERVER.SPIKE_COUNT.get(),
                        BTEConfigs.SERVER.SPIKE_RING_RADIUS.get(),
                        BTEConfigs.SERVER.GATEWAY_COUNT.get(),
                        BTEConfigs.SERVER.GATEWAY_RING_RADIUS.get(),
                        BTEConfigs.SERVER.GUARDED_CRYSTAL_COUNT.get(),
                        BTEConfigs.SERVER.CUSTOM_SPIKE_STRUCTURES.get()

                );

        // 新資料必須標記為需要保存
        data.setDirty();

        LOGGER.info(
                "Created End Fight settings from config: " +
                        "spikes={}, spikeRadius={}, gateways={}, gatewayRadius={}",
                data.spikeCount,
                data.spikeRadius,
                data.gatewayCount,
                data.gatewayRadius
        );

        return data;
    }

    /**
     * 讀取已有世界的保存資料。
     *
     * 有效存檔欄位優先。
     * 欄位缺失或數值無效時才讀取配置文件。
     */
    private static BTEEndFightSettingsSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        boolean hasSpikeCount =
                tag.contains(SPIKE_COUNT, Tag.TAG_INT)
                        && tag.getInt(SPIKE_COUNT) >= 1
                        && tag.getInt(SPIKE_COUNT) <= 64;

        boolean hasSpikeRadius =
                tag.contains(SPIKE_RADIUS, Tag.TAG_INT)
                        && tag.getInt(SPIKE_RADIUS) >= 16
                        && tag.getInt(SPIKE_RADIUS) <= 512;

        boolean hasGatewayCount =
                tag.contains(GATEWAY_COUNT, Tag.TAG_INT)
                        && tag.getInt(GATEWAY_COUNT) >= 0
                        && tag.getInt(GATEWAY_COUNT) <= 128;

        boolean hasGatewayRadius =
                tag.contains(GATEWAY_RADIUS, Tag.TAG_INT)
                        && tag.getInt(GATEWAY_RADIUS) >= 16
                        && tag.getInt(GATEWAY_RADIUS) <= 2048;
        // 確認柱子總數，供保護水晶數量判定使用
        int loadedSpikeCount = hasSpikeCount
                ? tag.getInt(SPIKE_COUNT)
                : BTEConfigs.SERVER.SPIKE_COUNT.get();

        // 舊存檔如果只有一根柱子，允許保護唯一水晶
        int minGuarded = loadedSpikeCount == 1 ? 1 : 2;

        boolean hasGuardedCount =
                tag.contains(GUARDED_CRYSTAL_COUNT, Tag.TAG_INT)
                        && tag.getInt(GUARDED_CRYSTAL_COUNT) >= minGuarded;
        boolean hasCustomSpikeStructures =
                tag.contains(CUSTOM_SPIKE_STRUCTURES, Tag.TAG_BYTE);
        BTEEndFightSettingsSavedData data =
                new BTEEndFightSettingsSavedData(
                        hasSpikeCount
                                ? tag.getInt(SPIKE_COUNT)
                                : BTEConfigs.SERVER.SPIKE_COUNT.get(),

                        hasSpikeRadius
                                ? tag.getInt(SPIKE_RADIUS)
                                : BTEConfigs.SERVER.SPIKE_RING_RADIUS.get(),

                        hasGatewayCount
                                ? tag.getInt(GATEWAY_COUNT)
                                : BTEConfigs.SERVER.GATEWAY_COUNT.get(),

                        hasGatewayRadius
                                ? tag.getInt(GATEWAY_RADIUS)
                                : BTEConfigs.SERVER.GATEWAY_RING_RADIUS.get(),
                        hasGuardedCount
                                ? tag.getInt(GUARDED_CRYSTAL_COUNT)
                                : BTEConfigs.SERVER.GUARDED_CRYSTAL_COUNT.get(),
                        hasCustomSpikeStructures
                                ? tag.getBoolean(CUSTOM_SPIKE_STRUCTURES)
                                : BTEConfigs.SERVER.CUSTOM_SPIKE_STRUCTURES.get()
                );


        // 有欄位需要補齊時，保存補齊後的資料。
        if (!hasSpikeCount
                || !hasSpikeRadius
                || !hasGatewayCount
                || !hasGatewayRadius
                || !hasGuardedCount
                || (hasGuardedCount
                && tag.getInt(GUARDED_CRYSTAL_COUNT)
                > loadedSpikeCount)|| !hasCustomSpikeStructures) {

            data.setDirty();

            LOGGER.warn(
                    "End Fight saved settings were incomplete or invalid. " +
                            "Missing values were loaded from config."
            );
        }

        LOGGER.info(
                "Loaded End Fight settings from world: " +
                        "spikes={}, spikeRadius={}, gateways={}, gatewayRadius={}",
                data.spikeCount,
                data.spikeRadius,
                data.gatewayCount,
                data.gatewayRadius
        );

        data.dragonFightSealed =
                tag.getBoolean("DragonFightSealed");

        data.dragonFightSealing =
                tag.getBoolean("DragonFightSealing");

        data.sealingNextSpike = Math.max(
                0,
                tag.getInt("SealingNextSpike")
        );

        // 已封禁時，不應再執行儀式
        if (data.dragonFightSealed) {
            data.dragonFightSealing = false;
        }

        int crystalCount = Math.min(
                4,
                Math.max(0, tag.getInt("SealingCrystalCount"))
        );

        for (int i = 0; i < crystalCount; i++) {

            String key = "SealingCrystal" + i;

            if (tag.hasUUID(key)) {
                data.sealingCrystalIds.add(tag.getUUID(key));
            }
        }


        return data;
    }

    /**
     * 保存至終界維度存檔。
     */
    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putInt(SPIKE_COUNT, this.spikeCount);
        tag.putInt(SPIKE_RADIUS, this.spikeRadius);
        tag.putInt(GATEWAY_COUNT, this.gatewayCount);
        tag.putInt(GATEWAY_RADIUS, this.gatewayRadius);
        tag.putInt(GUARDED_CRYSTAL_COUNT, this.guardedCrystalCount);
        tag.putBoolean( CUSTOM_SPIKE_STRUCTURES, this.customSpikeStructures);
        tag.putBoolean(
                "DragonFightSealed",
                this.dragonFightSealed
        );

        tag.putBoolean(
                "DragonFightSealing",
                this.dragonFightSealing
        );

        tag.putInt(
                "SealingNextSpike",
                this.sealingNextSpike
        );

        tag.putInt(
                "SealingCrystalCount",
                this.sealingCrystalIds.size()
        );

        for (int i = 0; i < this.sealingCrystalIds.size(); i++) {

            tag.putUUID(
                    "SealingCrystal" + i,
                    this.sealingCrystalIds.get(i)
            );
        }
        return tag;
    }

    /**
     * 取得指定終界世界的配置資料。
     *
     * computeIfAbsent：
     * 有存檔 -> 載入存檔
     * 無存檔 -> create() 從配置建立
     */
    public static BTEEndFightSettingsSavedData get(
            ServerLevel level
    ) {
        return level.getDataStorage().computeIfAbsent(
                FACTORY,
                DATA_NAME
        );
    }

    public int getSpikeCount() {
        return this.spikeCount;
    }

    public int getSpikeRadius() {
        return this.spikeRadius;
    }

    public int getGatewayCount() {
        return this.gatewayCount;
    }

    public int getGatewayRadius() {
        return this.gatewayRadius;
    }

    public int getGuardedCrystalCount() { return this.guardedCrystalCount; }

    public boolean isCustomSpikeStructuresEnabled() {return this.customSpikeStructures;}

    public boolean isDragonFightSealed() {
        return this.dragonFightSealed;
    }

    public boolean isDragonFightSealing() {
        return this.dragonFightSealing;
    }

    public int getSealingNextSpike() {
        return this.sealingNextSpike;
    }

    public List<UUID> getSealingCrystalIds() {
        return List.copyOf(this.sealingCrystalIds);
    }

    /**
     * 啟動永久封禁儀式。
     */
    public void startDragonFightSealing(List<UUID> crystals) {

        if (this.dragonFightSealed || this.dragonFightSealing) {
            return;
        }

        this.dragonFightSealing = true;
        this.sealingNextSpike = 0;

        this.sealingCrystalIds.clear();
        this.sealingCrystalIds.addAll(crystals);

        this.setDirty();
    }

    /**
     * 一根柱子已經摧毀，保存下一個索引。
     */
    public void advanceSealing(int nextSpike) {

        if (!this.dragonFightSealing) {
            return;
        }

        this.sealingNextSpike = Math.max(0, nextSpike);
        this.setDirty();
    }

    /**
     * 完成儀式，永久封禁龍戰。
     */
    public void finishDragonFightSealing() {

        this.dragonFightSealed = true;
        this.dragonFightSealing = false;

        this.sealingCrystalIds.clear();

        this.setDirty();
    }

    /**
     * 只能由管理員指令間接呼叫。
     */
    public void unsealDragonFight() {

        this.dragonFightSealed = false;
        this.dragonFightSealing = false;

        this.sealingNextSpike = 0;
        this.sealingCrystalIds.clear();

        this.setDirty();
    }

}

