package org.starrepublic.beyond_the_end.World.DragonSkill;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

/**
 * 統一計算終界龍火球的發射位置。
 *
 * 普通飛行：
 * 直接使用已經更新的原版龍頭碰撞箱。
 *
 * 高空轟炸：
 * 根據龍的當前位置、角度與姿態，
 * 重新計算龍頭位置，避免 EntityTickEvent.Post
 * 中移動龍後，龍頭碰撞箱仍然停留在舊位置。
 */
public final class DragonFireballMuzzle {

    private DragonFireballMuzzle() {
    }

    // 火球生成在龍頭前方的距離
    private static final double MOUTH_FORWARD_OFFSET = 1.8D;

    // 稍微下移，使火球接近嘴部而不是頭頂
    private static final double MOUTH_VERTICAL_OFFSET = -0.25D;

    // 原版龍頭相對龍本體的水平距離
    private static final float HEAD_DISTANCE = 6.5F;

    /**
     * 普通飛行時使用。
     *
     * 原版 AI 已經更新龍頭碰撞箱，
     * 因此直接讀取 dragon.head 的當前位置。
     */
    public static Vec3 getFlyingMuzzle(EnderDragon dragon) {

        Vec3 headCenter = dragon.head
                .getBoundingBox()
                .getCenter();

        Vec3 forward = headCenter
                .subtract(dragon.position())
                .multiply(1.0D, 0.0D, 1.0D);

        if (forward.lengthSqr() < 0.0001D) {
            forward = getHorizontalFacing(dragon);
        } else {
            forward = forward.normalize();
        }

        return headCenter
                .add(forward.scale(MOUTH_FORWARD_OFFSET))
                .add(0.0D, MOUTH_VERTICAL_OFFSET, 0.0D);
    }

    /**
     * 高空轟炸時使用。
     *
     * 不依賴 dragon.head 已緩存的碰撞箱座標。
     *
     * 依照 EnderDragon.aiStep() 中的龍頭位置計算，
     * 使用龍本體目前的座標與旋轉角度重新計算。
     */
    public static Vec3 getSkyBombardmentMuzzle(
            EnderDragon dragon
    ) {
        double[] latency5 = dragon.getLatencyPos(5, 1.0F);
        double[] latency10 = dragon.getLatencyPos(10, 1.0F);

        // 原版龍身的上下傾斜角度
        float tilt = (float) (
                latency5[1] - latency10[1]
        ) * 10.0F * ((float) Math.PI / 180.0F);

        float cosTilt = Mth.cos(tilt);
        float sinTilt = Mth.sin(tilt);

        // 與原版龍頭分段使用相同的水平角度修正
        float yaw = dragon.getYRot()
                * ((float) Math.PI / 180.0F)
                - dragon.yRotA * 0.01F;

        float sinYaw = Mth.sin(yaw);
        float cosYaw = Mth.cos(yaw);

        // 原版 HOVERING 為 sitting 狀態，
        // 龍頭的基本垂直偏移為 -1.0 格。
        double headYOffset;

        if (dragon.getPhaseManager()
                .getCurrentPhase()
                .isSitting()) {

            headYOffset = -1.0D;

        } else {

            double[] latency0 = dragon.getLatencyPos(0, 1.0F);

            headYOffset = latency5[1] - latency0[1];
        }

        // 根據目前龍本體的位置重算龍頭中心
        Vec3 headCenter = dragon.position().add(
                sinYaw * HEAD_DISTANCE * cosTilt,
                headYOffset + sinTilt * HEAD_DISTANCE,
                -cosYaw * HEAD_DISTANCE * cosTilt
        );

        // 沿龍頭的水平正前方延伸至嘴部
        Vec3 forward = new Vec3(
                sinYaw,
                0.0D,
                -cosYaw
        );

        return headCenter
                .add(forward.scale(MOUTH_FORWARD_OFFSET))
                .add(0.0D, MOUTH_VERTICAL_OFFSET, 0.0D);
    }

    /**
     * 原版終界龍的水平正前方。
     */
    private static Vec3 getHorizontalFacing(
            EnderDragon dragon
    ) {
        float radians = dragon.getYRot()
                * ((float) Math.PI / 180.0F);

        return new Vec3(
                Mth.sin(radians),
                0.0D,
                -Mth.cos(radians)
        );
    }
}

