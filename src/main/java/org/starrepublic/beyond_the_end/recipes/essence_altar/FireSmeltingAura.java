package org.starrepublic.beyond_the_end.recipes.essence_altar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Entitys.blockentity.FireEssenceAltarBlockEntity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class FireSmeltingAura {

    public static final double RADIUS = 20.0D;
    public static final double RADIUS_SQUARED = 400.0D;

    private static final Map<
            ServerLevel,
            Map<Long, WeakReference<FireEssenceAltarBlockEntity>>
            > ALTARS = new WeakHashMap<>();

    private FireSmeltingAura() {
    }

    // 每次炎祭壇執行伺服器 Tick 時呼叫
    public static void track(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar
    ) {

        ALTARS.computeIfAbsent(
                level,
                ignored -> new HashMap<>()
        ).put(
                altar.getBlockPos().asLong(),
                new WeakReference<>(altar)
        );
    }

    // ========================================================
    // 查詢指定位置的有效加速效果
    // ========================================================

    @Nullable
    public static Aura find(
            ServerLevel level,
            Vec3 target
    ) {

        Map<Long, WeakReference<FireEssenceAltarBlockEntity>> map =
                ALTARS.get(level);

        if (map == null || map.isEmpty()) {
            return null;
        }

        int highestSpeed = 0;

        Iterator<
                Map.Entry<Long, WeakReference<FireEssenceAltarBlockEntity>>
                > iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {

            FireEssenceAltarBlockEntity altar =
                    iterator.next().getValue().get();

            if (altar == null
                    || altar.isRemoved()
                    || altar.getLevel() != level) {

                iterator.remove();
                continue;
            }

            BlockPos pos = altar.getBlockPos();

            if (!level.hasChunkAt(pos)) {
                iterator.remove();
                continue;
            }

            // 防止已被替換的方塊實體繼續生效
            if (level.getBlockEntity(pos) != altar) {
                iterator.remove();
                continue;
            }

            if (!altar.isSmeltingActive()) {
                continue;
            }

            Vec3 center = Vec3.atCenterOf(pos);

            // 真正的三維球體距離判斷
            if (center.distanceToSqr(target)
                    > RADIUS_SQUARED) {
                continue;
            }

            highestSpeed = Math.max(
                    highestSpeed,
                    altar.getSmeltingSpeedPercent()
            );
        }

        if (highestSpeed <= 0) {
            return null;
        }

        return new Aura(
                highestSpeed,
                highestSpeed > 200
        );
    }

    @Nullable
    public static Aura find(
            ServerLevel level,
            BlockPos target
    ) {

        return find(
                level,
                Vec3.atCenterOf(target)
        );
    }

    // 不同祭壇的效果不疊加，只取最高速度
    public record Aura(
            int speedPercent,
            boolean allowFuelFree
    ) {
    }
}
