package org.starrepublic.beyond_the_end.API;

import net.minecraft.server.level.ServerLevel;
import org.starrepublic.beyond_the_end.World.EndBarrier.EndBarrierManager;

public final class EndBarrierApi {
    private EndBarrierApi() {
    }

    /**
     * 查詢主島結界是否仍然存在。
     */
    public static boolean isBarrierEnabled(
            ServerLevel level
    ) {
        return EndBarrierManager
                .isBarrierEnabled(level);
    }

    /**
     * 查詢主島結界是否已解除。
     */
    public static boolean isBarrierUnlocked(
            ServerLevel level
    ) {
        return !EndBarrierManager
                .isBarrierEnabled(level);
    }

    /**
     * 永久解除這個世界的主島結界。
     *
     * 可供其他 MOD 呼叫。
     */
    public static void unlockBarrier(
            ServerLevel level
    ) {
        EndBarrierManager
                .unlockBarrier(level);
    }

    /**
     * 重新啟用結界。
     *
     * 主要給開發、整合 MOD 或特殊劇情使用。
     */
    public static void lockBarrier(
            ServerLevel level
    ) {
        EndBarrierManager
                .lockBarrier(level);
    }
    public static boolean tryUnlockBarrier(
            ServerLevel level
    ) {

        if (isBarrierUnlocked(level)) {
            return false;
        }

        unlockBarrier(level);

        return true;
    }
}
