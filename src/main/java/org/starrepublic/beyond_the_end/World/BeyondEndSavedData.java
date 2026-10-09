package org.starrepublic.beyond_the_end.World;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class BeyondEndSavedData extends SavedData {

    private static final String DATA_NAME = "beyond_the_end";
    private static final String TAG_MAIN_ISLAND_BARRIER_UNLOCKED =
            "MainIslandBarrierUnlocked";

    private boolean mainIslandBarrierUnlocked = false;

    public BeyondEndSavedData() {
    }

    public static BeyondEndSavedData create() {
        return new BeyondEndSavedData();
    }

    public static BeyondEndSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        BeyondEndSavedData data = create();

        data.mainIslandBarrierUnlocked =
                tag.getBoolean(TAG_MAIN_ISLAND_BARRIER_UNLOCKED);

        return data;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        tag.putBoolean(
                TAG_MAIN_ISLAND_BARRIER_UNLOCKED,
                mainIslandBarrierUnlocked
        );

        return tag;
    }

    public boolean isMainIslandBarrierUnlocked() {
        return mainIslandBarrierUnlocked;
    }

    public void setMainIslandBarrierUnlocked(boolean unlocked) {

        if (this.mainIslandBarrierUnlocked == unlocked) {
            return;
        }

        this.mainIslandBarrierUnlocked = unlocked;

        // 非常重要
        this.setDirty();
    }

    public void unlockMainIslandBarrier() {
        setMainIslandBarrierUnlocked(true);
    }

    public void lockMainIslandBarrier() {
        setMainIslandBarrierUnlocked(false);
    }

    public static BeyondEndSavedData get(ServerLevel level) {

        // 無論從哪個維度呼叫，
        // 都統一取 Overworld 的資料
        ServerLevel overworld =
                level.getServer().overworld();

        return overworld
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                BeyondEndSavedData::create,
                                BeyondEndSavedData::load
                        ),
                        DATA_NAME
                );
    }
}
