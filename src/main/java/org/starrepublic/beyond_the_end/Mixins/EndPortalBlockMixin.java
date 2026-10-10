package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

import javax.annotation.Nullable;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {

    @Inject(
            method = "getPortalDestination",
            at = @At("HEAD"),
            cancellable = true
    )
    private void beyondTheEnd$modifyEndPortalDestination(
            ServerLevel currentLevel,
            Entity entity,
            BlockPos portalPos,
            CallbackInfoReturnable<DimensionTransition> cir
    ) {

        /*
         * 只修改：
         *
         * END -> OVERWORLD
         *
         * 其他情況全部交回原版。
         */
        if (currentLevel.dimension() != Level.END) {
            return;
        }

        ServerLevel overworld =
                currentLevel.getServer().getLevel(Level.OVERWORLD);

        if (overworld == null) {
            cir.setReturnValue(null);
            return;
        }

        /*
         * 主世界指定出口座標。
         *
         * 建議之後放進 SERVER config。
         */
        int x = BTEConfigs.SERVER.EndExitPointX.get();
        int z = BTEConfigs.SERVER.EndExitPointZ.get();

        Vec3 target =
                findSafeOverworldExit(
                        overworld,
                        x,
                        z
                );
        cir.setReturnValue(
                new DimensionTransition(
                        overworld,
                        target,
                        Vec3.ZERO,
                        entity.getYRot(),
                        entity.getXRot(),
                        false,
                        DimensionTransition.PLACE_PORTAL_TICKET
                )
        );
    }
    private static Vec3 findSafeOverworldExit(
            ServerLevel level,
            int x,
            int z
    )
    {

        /*
         * 先強制取得目標位置的完整 chunk。
         *
         * 避免在傳送當下高度圖還不是完整世界狀態。
         */
        level.getChunk(
                x >> 4,
                z >> 4
        );

        /*
         * WORLD_SURFACE：
         *
         * 我們先取得這個 X/Z 柱狀區域最上層的非空氣位置附近。
         */
        int startY = level.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                x,
                z
        );

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 2;

        /*
         * 防止 Heightmap 回傳異常值。
         */
        startY = Math.max(
                minY + 1,
                Math.min(startY, maxY)
        );

        /*
         * 從上往下實際檢查。
         */
        for (int y = startY; y > minY; y--) {

            BlockPos feet = new BlockPos(x, y, z);
            BlockPos head = feet.above();
            BlockPos floor = feet.below();

            BlockState feetState =
                    level.getBlockState(feet);

            BlockState headState =
                    level.getBlockState(head);

            BlockState floorState =
                    level.getBlockState(floor);

            /*
             * 玩家腳與頭的位置必須沒有碰撞。
             */
            boolean feetFree =
                    feetState
                            .getCollisionShape(level, feet)
                            .isEmpty();

            boolean headFree =
                    headState
                            .getCollisionShape(level, head)
                            .isEmpty();

            /*
             * 腳下必須是真正可以站的方塊。
             */
            boolean hasFloor =
                    !floorState
                            .getCollisionShape(level, floor)
                            .isEmpty();

            /*
             * 不能把玩家放進液體。
             */
            boolean feetDry =
                    feetState.getFluidState().isEmpty();

            boolean headDry =
                    headState.getFluidState().isEmpty();

            /*
             * 腳下也不要是液體。
             */
            boolean floorDry =
                    floorState.getFluidState().isEmpty();

            if (
                    feetFree
                            && headFree
                            && hasFloor
                            && feetDry
                            && headDry
                            && floorDry
            ) {

                return new Vec3(
                        x + 0.5D,
                        y,
                        z + 0.5D
                );
            }
        }

        /*
         * 理論上不應走到這裡。
         *
         * 如果真的完全找不到安全地面，
         * 建立一個 fallback 平台。
         */
        int fallbackY = Math.max(
                level.getSeaLevel() + 2,
                64
        );

        BlockPos platform =
                new BlockPos(x, fallbackY - 1, z);

        level.setBlockAndUpdate(
                platform,
                Blocks.OBSIDIAN.defaultBlockState()
        );

        return new Vec3(
                x + 0.5D,
                fallbackY,
                z + 0.5D
        );
    }
    @Nullable
    private static Vec3 tryFindSafeLandPosition(
            ServerLevel level,
            int x,
            int z
    ) {

        level.getChunk(x >> 4, z >> 4);

        int y = level.getHeight(
                Heightmap.Types.WORLD_SURFACE,
                x,
                z
        );

        if (
                y <= level.getMinBuildHeight()
                        || y >= level.getMaxBuildHeight()
        ) {
            return null;
        }

        BlockPos feet =
                new BlockPos(x, y, z);

        BlockPos head =
                feet.above();

        BlockPos floor =
                feet.below();

        BlockState feetState =
                level.getBlockState(feet);

        BlockState headState =
                level.getBlockState(head);

        BlockState floorState =
                level.getBlockState(floor);

        /*
         * 最重要：
         *
         * WORLD_SURFACE 在海洋可以取得海面附近，
         * 所以我们明确拒绝液体。
         */
        if (!feetState.getFluidState().isEmpty()) {
            return null;
        }

        if (!headState.getFluidState().isEmpty()) {
            return null;
        }

        if (!floorState.getFluidState().isEmpty()) {
            return null;
        }

        /*
         * 两格生存空间。
         */
        if (
                !feetState
                        .getCollisionShape(level, feet)
                        .isEmpty()
        ) {
            return null;
        }

        if (
                !headState
                        .getCollisionShape(level, head)
                        .isEmpty()
        ) {
            return null;
        }

        /*
         * 必须有真正地面。
         */
        if (
                floorState
                        .getCollisionShape(level, floor)
                        .isEmpty()
        ) {
            return null;
        }

        return new Vec3(
                x + 0.5D,
                y,
                z + 0.5D
        );
    }
}
