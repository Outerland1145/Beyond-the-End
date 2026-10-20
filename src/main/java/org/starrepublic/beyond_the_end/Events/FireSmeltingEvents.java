package org.starrepublic.beyond_the_end.Events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.starrepublic.beyond_the_end.recipes.essence_altar.FireSmeltingAura;

public final class FireSmeltingEvents {

    private FireSmeltingEvents() {
    }

    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return;
        }

        if (FireSmeltingAura.find(
                level,
                player.getBoundingBox().getCenter()
        ) == null) {
            return;
        }

        player.clearFire();

        event.setCanceled(true);
    }

    // 在玩家更新開始時清除火焰
    public static void onPlayerTickPre(
            PlayerTickEvent.Pre event
    ) {
        clearFire(event.getEntity());
    }

    // 在玩家更新結束後再次清除，
    // 避免踩入火焰或岩漿後保留燃燒狀態
    public static void onPlayerTickPost(
            PlayerTickEvent.Post event
    ) {
        clearFire(event.getEntity());
    }

    private static void clearFire(Player player) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (FireSmeltingAura.find(
                level,
                player.getBoundingBox().getCenter()
        ) != null) {

            if (player.getRemainingFireTicks() > 0) {
                player.clearFire();
            }
        }
    }
}
