package org.starrepublic.beyond_the_end.Events;

import org.starrepublic.beyond_the_end.Beyond_the_end;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.starrepublic.beyond_the_end.Effect.EndermiteSpawnHelper;

public final class EnderInfestedCloudHandler {

    private EnderInfestedCloudHandler() {}

    // 每隔 20 Tick 生成一波
    private static final int SPAWN_INTERVAL = 20;

    // 每波生成三隻
    private static final int SPAWN_PER_WAVE = 3;

    // 總共六波
    private static final int MAX_WAVES = 6;

    private static final String POTION_ID =
            Beyond_the_end.MODID + ":ender_infested";

    public static void onEntityTick(EntityTickEvent.Post event) {

        if (!(event.getEntity() instanceof AreaEffectCloud cloud)) {
            return;
        }

        if (!(cloud.level() instanceof ServerLevel level)) {
            return;
        }

        if (cloud.isRemoved() || cloud.isWaiting()) {
            return;
        }

        int tick = cloud.tickCount;

        // 不是召喚時間就直接略過
        if (tick <= 0 || tick % SPAWN_INTERVAL != 0) {
            return;
        }

        // 超過六波後不再生成
        if (tick > SPAWN_INTERVAL * MAX_WAVES) {
            return;
        }

        float radius = cloud.getRadius();

        if (radius <= 0.5F) {
            return;
        }

        // 讀取藥水雲保存的原版資料
        CompoundTag data = cloud.saveWithoutId(new CompoundTag());

        if (!data.contains("potion_contents", Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag potionData =
                data.getCompound("potion_contents");

        // 確保只有終界蟲蝕藥水雲會生成終界蟎
        if (!POTION_ID.equals(potionData.getString("potion"))) {
            return;
        }

        EndermiteSpawnHelper.spawnAround(
                level,
                cloud.position(),
                SPAWN_PER_WAVE,
                radius,
                false
        );
    }
}

