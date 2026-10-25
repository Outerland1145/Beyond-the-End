package org.starrepublic.beyond_the_end.World.DFM;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BTEDragonFightRegistry {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BTEDragonFightRegistry.class);

    private BTEDragonFightRegistry() {
    }

    /**
     * 由模組主類統一呼叫一次。
     */
    public static void register() {

        NeoForge.EVENT_BUS.addListener(
                BTEDragonFightRegistry::onLevelLoad
        );
    }

    /**
     * 伺服器維度載入完成時執行。
     */
    private static void onLevelLoad(LevelEvent.Load event) {

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // 只處理原版終界維度
        if (!level.dimension().equals(Level.END)) {
            return;
        }
        // 初始化或讀取該終界世界的龍戰配置
        BTEEndFightSettingsSavedData.get(level);
        EndDragonFight original = level.getDragonFight();

        // 不接管不存在的龍戰管理器
        if (original == null) {
            return;
        }

        // 避免重複替換
        if (original instanceof BTEDragonFightManager) {
            return;
        }

        // 保留原版目前記錄的龍戰狀態
        EndDragonFight.Data data = original.saveData();

        // 第一版沿用原版主島龍戰中心
        BlockPos origin = BlockPos.ZERO;

        BTEDragonFightManager manager =
                new BTEDragonFightManager(
                        level,
                        level.getSeed(),
                        data,
                        origin
                );

        // 交由 ServerLevel 的原有更新與保存流程管理
        install(level, manager);

        LOGGER.info(
                "Beyond The End Dragon Fight Manager installed in {}",
                level.dimension().location()
        );
    }

    /**
     * 安裝新的龍戰管理器。
     */
    @SuppressWarnings("deprecation")
    private static void install(
            ServerLevel level,
            BTEDragonFightManager manager
    ) {
        level.setDragonFight(manager);
    }

    /**
     * 統一取得 Beyond The End 的龍戰管理器。
     *
     * 尚未安裝時回傳 null。
     */
    public static BTEDragonFightManager get(
            ServerLevel level
    ) {

        if (!level.dimension().equals(Level.END)) {
            return null;
        }

        EndDragonFight fight = level.getDragonFight();

        if (fight instanceof BTEDragonFightManager manager) {
            return manager;
        }

        return null;
    }
}

