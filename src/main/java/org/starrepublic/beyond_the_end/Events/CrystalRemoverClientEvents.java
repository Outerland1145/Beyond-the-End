package org.starrepublic.beyond_the_end.Events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.starrepublic.beyond_the_end.Register.ItemRegister;
import org.starrepublic.beyond_the_end.network.payload.CrystalRemoverModePayload;

public final class CrystalRemoverClientEvents {

    private static double scrollAccumulator = 0.0D;

    private CrystalRemoverClientEvents() {}

    public static void onMouseScroll(
            InputEvent.MouseScrollingEvent event
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        /*
         * 只有潛行時可以切換模式。
         */
        if (!minecraft.player.isShiftKeyDown()) {
            scrollAccumulator = 0.0D;
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();

        /*
         * 必須是原本的水晶拆除器。
         */
        if (!stack.is(ItemRegister.END_CRYSTAL_REMOVER.get())) {
            scrollAccumulator = 0.0D;
            return;
        }

        double scroll = event.getScrollDeltaY();

        if (scroll == 0.0D) {
            return;
        }

        /*
         * 阻止原版滾輪切換快捷列。
         */
        event.setCanceled(true);

        /*
         * 累積滾動量，支援高精度滑鼠。
         */
        scrollAccumulator += scroll;

        // 限制極端輸入
        scrollAccumulator = Math.clamp(scrollAccumulator,
                -5.0D, 5.0D);

        while (scrollAccumulator >= 1.0D) {

            PacketDistributor.sendToServer(
                    new CrystalRemoverModePayload(1)
            );

            scrollAccumulator -= 1.0D;
        }

        while (scrollAccumulator <= -1.0D) {

            PacketDistributor.sendToServer(
                    new CrystalRemoverModePayload(-1)
            );

            scrollAccumulator += 1.0D;
        }
    }
}
