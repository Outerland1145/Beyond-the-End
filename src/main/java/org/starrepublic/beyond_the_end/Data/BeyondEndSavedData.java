package org.starrepublic.beyond_the_end.Data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.starrepublic.beyond_the_end.World.progression.BarrierNodeDirection;

public class BeyondEndSavedData extends SavedData {

    private static final String DATA_NAME = "beyond_the_end";
    private static final String TAG_MAIN_ISLAND_BARRIER_UNLOCKED =
            "MainIslandBarrierUnlocked";

    private boolean mainIslandBarrierUnlocked = false;

    private boolean elytraObtained = false;

    private boolean northNodeDestroyed = false;
    private boolean southNodeDestroyed = false;
    private boolean eastNodeDestroyed = false;
    private boolean westNodeDestroyed = false;

    public BeyondEndSavedData() {
    }
    public boolean hasObtainedElytra() {
        return elytraObtained;
    }

    public void setElytraObtained() {

        if (elytraObtained) {
            return;
        }

        elytraObtained = true;
        setDirty();
    }
    public boolean isNodeDestroyed(
            BarrierNodeDirection direction
    ) {

        return switch (direction) {

            case NORTH ->
                    northNodeDestroyed;

            case SOUTH ->
                    southNodeDestroyed;

            case EAST ->
                    eastNodeDestroyed;

            case WEST ->
                    westNodeDestroyed;
        };
    }
    public void markNodeDestroyed(
            BarrierNodeDirection direction
    ) {

        boolean changed = false;

        switch (direction) {

            case NORTH -> {
                if (!northNodeDestroyed) {
                    northNodeDestroyed = true;
                    changed = true;
                }
            }

            case SOUTH -> {
                if (!southNodeDestroyed) {
                    southNodeDestroyed = true;
                    changed = true;
                }
            }

            case EAST -> {
                if (!eastNodeDestroyed) {
                    eastNodeDestroyed = true;
                    changed = true;
                }
            }

            case WEST -> {
                if (!westNodeDestroyed) {
                    westNodeDestroyed = true;
                    changed = true;
                }
            }
        }

        if (changed) {
            setDirty();
        }
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
        data.elytraObtained =
                tag.getBoolean(
                        "ElytraObtained"
                );

        data.northNodeDestroyed =
                tag.getBoolean(
                        "NorthNodeDestroyed"
                );

        data.southNodeDestroyed =
                tag.getBoolean(
                        "SouthNodeDestroyed"
                );

        data.eastNodeDestroyed =
                tag.getBoolean(
                        "EastNodeDestroyed"
                );

        data.westNodeDestroyed =
                tag.getBoolean(
                        "WestNodeDestroyed"
                );

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
        tag.putBoolean(
                "ElytraObtained",
                elytraObtained
        );

        tag.putBoolean(
                "NorthNodeDestroyed",
                northNodeDestroyed
        );

        tag.putBoolean(
                "SouthNodeDestroyed",
                southNodeDestroyed
        );

        tag.putBoolean(
                "EastNodeDestroyed",
                eastNodeDestroyed
        );

        tag.putBoolean(
                "WestNodeDestroyed",
                westNodeDestroyed
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
    public void resetBarrierProgress() {

        this.mainIslandBarrierUnlocked = false;

        this.elytraObtained = false;

        this.northNodeDestroyed = false;
        this.southNodeDestroyed = false;
        this.eastNodeDestroyed = false;
        this.westNodeDestroyed = false;

        setDirty();
    }
}
