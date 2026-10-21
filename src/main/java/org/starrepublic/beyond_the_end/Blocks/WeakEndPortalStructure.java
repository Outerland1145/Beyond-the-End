package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

public final class WeakEndPortalStructure {

    private WeakEndPortalStructure() {}

    /**
     * 判斷座標是否為 5x5 傳送門的 12 個框架位置。
     * 四個角落不算框架。
     */
    private static boolean isFramePosition(int x, int z) {
        return ((z == 0 || z == 4) && x >= 1 && x <= 3)
                || ((x == 0 || x == 4) && z >= 1 && z <= 3);
    }

    /**
     * 取得框架應該面向的方向。
     * 所有框架都必須指向傳送門中央。
     */
    private static Direction expectedFacing(int x, int z) {
        if (x == 0) return Direction.EAST;
        if (x == 4) return Direction.WEST;
        if (z == 0) return Direction.SOUTH;
        return Direction.NORTH;
    }

    /**
     * 檢查完整的 12 框架結構。
     *
     * ignoredPos 為 null：檢查全部 12 個框架。
     * ignoredPos 不為 null：忽略該位置，用於框架遭破壞後的判定。
     */
    private static boolean matchesRing(
            Level level,
            BlockPos origin,
            BlockPos ignoredPos
    ) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {

                if (!isFramePosition(x, z)) {
                    continue;
                }

                BlockPos checkPos = origin.offset(x, 0, z);

                if (ignoredPos != null && checkPos.equals(ignoredPos)) {
                    continue;
                }

                BlockState state = level.getBlockState(checkPos);

                if (!(state.getBlock() instanceof WeakEndPortalFrameBlock)) {
                    return false;
                }

                if (!state.getValue(WeakEndPortalFrameBlock.HAS_EYE)) {
                    return false;
                }

                if (state.getValue(WeakEndPortalFrameBlock.FACING)
                        != expectedFacing(x, z)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 中央 3x3 必須全部是空氣。
     * 同時確認生成位置沒有超出世界高度與世界邊界。
     */
    private static boolean isPortalAreaClear(
            Level level,
            BlockPos origin
    ) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {

                BlockPos pos = origin.offset(x, 0, z);

                if (!level.isInWorldBounds(pos)
                        || !level.getWorldBorder().isWithinBounds(pos)) {
                    return false;
                }

                if (!level.getBlockState(pos).isAir()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 插入激活物品後，嘗試啟動傳送門。
     */
    public static boolean tryActivate(
            Level level,
            BlockPos changedFrame
    ) {
        if (level.isClientSide) {
            return false;
        }
        if (level.dimension() == Level.END
                && !BTEConfigs.SERVER.enableweakendportalactivateinend.get()) {
            return false;
        }
        // 嘗試將被互動的框架視為 12 個框架中的任意一個
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {

                if (!isFramePosition(x, z)) {
                    continue;
                }

                // 推算 5x5 結構的西北角
                BlockPos origin = changedFrame.offset(-x, 0, -z);

                if (!matchesRing(level, origin, null)) {
                    continue;
                }

                if (!isPortalAreaClear(level, origin)) {
                    continue;
                }

                // 結構完全合法，生成中央 3x3 傳送門
                for (int px = 1; px <= 3; px++) {
                    for (int pz = 1; pz <= 3; pz++) {

                        BlockPos portalPos = origin.offset(px, 0, pz);

                        level.setBlock(
                                portalPos,
                                Blocks.END_PORTAL.defaultBlockState(),
                                Block.UPDATE_ALL
                        );
                    }
                }

                // 原版終界傳送門激活音效
                level.globalLevelEvent(
                        LevelEvent.SOUND_END_PORTAL_SPAWN,
                        origin.offset(2, 0, 2),
                        0
                );

                return true;
            }
        }

        return false;
    }
    /**
     * 當帶有眼睛的虛弱終界傳送門框架遭到破壞時，
     * 清除以該框架為中心，7x7 水平區域內的所有終界傳送門方塊。
     *
     * 搜尋範圍：
     * X = -3 ~ +3
     * Y =  0
     * Z = -3 ~ +3
     *
     * 不檢查傳送門結構是否完整。
     */
    public static void removeNearbyPortals(
            Level level,
            BlockPos center
    ) {
        if (level.isClientSide) {
            return;
        }

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {

                BlockPos checkPos = center.offset(dx, 0, dz);

                // 只清除原版終界傳送門方塊
                if (!level.getBlockState(checkPos).is(Blocks.END_PORTAL)) {
                    continue;
                }

                level.setBlock(
                        checkPos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL
                );
            }
        }
    }
    /**
     * 框架遭到破壞時，清除它所屬的傳送門。
     */
    public static void removeConnectedPortal(
            Level level,
            BlockPos removedPos,
            BlockState oldState
    ) {
        if (level.isClientSide) {
            return;
        }

        // 已啟動的傳送門，其框架必定有 HAS_EYE
        if (!oldState.getValue(WeakEndPortalFrameBlock.HAS_EYE)) {
            return;
        }

        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {

                if (!isFramePosition(x, z)) {
                    continue;
                }

                // 被移除的框架方向必須符合這個位置
                if (oldState.getValue(WeakEndPortalFrameBlock.FACING)
                        != expectedFacing(x, z)) {
                    continue;
                }

                BlockPos origin = removedPos.offset(-x, 0, -z);

                // 檢查剩下 11 個框架是否仍然符合結構
                if (!matchesRing(level, origin, removedPos)) {
                    continue;
                }

                boolean hasPortal = false;

                for (int px = 1; px <= 3 && !hasPortal; px++) {
                    for (int pz = 1; pz <= 3; pz++) {

                        BlockPos portalPos = origin.offset(px, 0, pz);

                        if (level.getBlockState(portalPos).is(Blocks.END_PORTAL)) {
                            hasPortal = true;
                            break;
                        }
                    }
                }

                if (!hasPortal) {
                    continue;
                }

                // 僅移除該結構中央的傳送門方塊
                for (int px = 1; px <= 3; px++) {
                    for (int pz = 1; pz <= 3; pz++) {

                        BlockPos portalPos = origin.offset(px, 0, pz);

                        if (level.getBlockState(portalPos).is(Blocks.END_PORTAL)) {
                            level.setBlock(
                                    portalPos,
                                    Blocks.AIR.defaultBlockState(),
                                    Block.UPDATE_ALL
                            );
                        }
                    }
                }

                return;
            }
        }
    }
}
