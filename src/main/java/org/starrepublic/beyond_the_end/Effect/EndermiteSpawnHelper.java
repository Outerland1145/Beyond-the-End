package org.starrepublic.beyond_the_end.Effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class EndermiteSpawnHelper {

    private EndermiteSpawnHelper() {}

    public static int spawnAround(
            ServerLevel level,
            Vec3 center,
            int amount,
            double radius,
            boolean fallbackToCenter
    ) {
        RandomSource random = level.random;
        int spawned = 0;

        for (int i = 0; i < amount; i++) {

            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * radius;

            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;

            BlockPos pos = findSpawnPosition(level, center, x, z);

            // 主要用於瞬移成功後，確保至少能在目標旁嘗試生成。
            if (pos == null && fallbackToCenter) {
                pos = BlockPos.containing(center.x, center.y, center.z);
            }

            if (pos == null || !level.hasChunkAt(pos)) {
                continue;
            }

            Endermite mite = EntityType.ENDERMITE.create(level);

            if (mite == null) {
                continue;
            }

            mite.moveTo(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    random.nextFloat() * 360.0F,
                    0.0F
            );

            if (level.addFreshEntity(mite)) {
                spawned++;
            }
        }

        return spawned;
    }

    private static BlockPos findSpawnPosition(
            ServerLevel level,
            Vec3 center,
            double x,
            double z
    ) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int baseY = Mth.floor(center.y);

        // 僅搜尋附近高度，避免藥水落在洞穴時，
        // 終界蟎卻生成到地表。
        for (int offset = 3; offset >= -4; offset--) {

            BlockPos feet = new BlockPos(
                    blockX,
                    baseY + offset,
                    blockZ
            );

            BlockPos below = feet.below();
            BlockPos above = feet.above();

            if (!level.hasChunkAt(below)
                    || !level.hasChunkAt(above)) {
                continue;
            }

            BlockState floor = level.getBlockState(below);

            // 下方需要有可以站立的表面。
            if (!floor.isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }

            // 腳部與頭部空間不可被實體碰撞方塊堵住。
            if (!level.getBlockState(feet)
                    .getCollisionShape(level, feet).isEmpty()) {
                continue;
            }

            if (!level.getBlockState(above)
                    .getCollisionShape(level, above).isEmpty()) {
                continue;
            }

            return feet;
        }

        return null;
    }
}

