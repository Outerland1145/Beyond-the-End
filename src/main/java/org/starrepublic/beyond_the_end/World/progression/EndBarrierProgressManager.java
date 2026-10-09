package org.starrepublic.beyond_the_end.World.progression;

import net.minecraft.server.level.ServerLevel;
import org.starrepublic.beyond_the_end.API.EndBarrierApi;
import org.starrepublic.beyond_the_end.Data.BeyondEndSavedData;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

public class EndBarrierProgressManager {
    private EndBarrierProgressManager() {
    }
    private static boolean ProgressManagerEnable = BTEConfigs.SERVER.useoriginalsetting.get();
    public static boolean CheckProgressManagerEnable() {
        if (ProgressManagerEnable) {
            return true;
        }
        else {
            return false;
        }
    }
    public static void markElytraObtained(
            ServerLevel level
    ) {

        BeyondEndSavedData data =
                BeyondEndSavedData.get(level);

        data.setElytraObtained();

        checkDefaultUnlockConditions(level);
    }

    public static void markNodeDestroyed(
            ServerLevel level,
            BarrierNodeDirection direction
    ) {

        BeyondEndSavedData data =
                BeyondEndSavedData.get(level);

        data.markNodeDestroyed(direction);

        checkDefaultUnlockConditions(level);
    }

    public static void checkDefaultUnlockConditions(
            ServerLevel level
    ) {

        BeyondEndSavedData data =
                BeyondEndSavedData.get(level);

        if (data.isMainIslandBarrierUnlocked()) {
            return;
        }

        if (!data.hasObtainedElytra()) {
            return;
        }

        for (
                BarrierNodeDirection direction :
                BarrierNodeDirection.values()
        ) {

            if (!data.isNodeDestroyed(direction)) {
                return;
            }
        }

       EndBarrierApi.unlockBarrier(level);
    }
}
