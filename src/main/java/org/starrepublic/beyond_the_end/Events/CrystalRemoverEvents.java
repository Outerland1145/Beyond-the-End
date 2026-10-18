package org.starrepublic.beyond_the_end.Events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.dimension.end.EndDragonFight;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.starrepublic.beyond_the_end.Register.ItemRegister;
import org.starrepublic.beyond_the_end.Register.Tags;

public class CrystalRemoverEvents {

    private CrystalRemoverEvents() {}

    public static void onCrystalInteract(
            PlayerInteractEvent.EntityInteractSpecific event
    ) {

        // 必須是終界水晶
        if (!(event.getTarget() instanceof EndCrystal crystal)) {
            return;
        }

        // 必須使用水晶拆除器
        if (!event.getItemStack().is(ItemRegister.END_CRYSTAL_REMOVER.get())) {
            return;
        }

        /*
         * 取消原版實體互動。
         *
         * 使用 SUCCESS 避免客戶端繼續嘗試其他
         * 右鍵互動流程。
         */
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        // 所有實際修改只在伺服器執行
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (crystal.isRemoved()) {
            return;
        }

        Player player = event.getEntity();

        BlockPos crystalPos = crystal.blockPosition();

        /*
         * ======================================================
         * 群系 TAG 黑名單
         * ======================================================
         *
         * 水晶所在群系具有指定 TAG 時：
         *
         * - 不移除
         * - 不返還物品
         * - 不消耗拆除器
         * - 不觸發爆炸
         */
        if (level.getBiome(crystalPos)
                .is(Tags.CRYSTAL_REMOVER_BLACKLIST)) {

            player.displayClientMessage(
                    Component.translatable(
                            "message.beyond_the_end.crystal_remover_blocked"
                    ),
                    true
            );

            return;
        }

        /*
         * ======================================================
         * 安全拆除
         * ======================================================
         */

        // 記錄位置
        double x = crystal.getX();
        double y = crystal.getY();
        double z = crystal.getZ();

        // 只建立傷害來源，不對水晶造成傷害
        DamageSource source =
                player.damageSources().playerAttack(player);

        /*
         * 直接移除實體，不經過 EndCrystal#hurt。
         * 因此不會執行水晶的爆炸邏輯。
         */
        crystal.discard();

        /*
         * ======================================================
         * 通知原版終界龍戰鬥管理器
         * ======================================================
         *
         * 正常情況下主島群系已被禁止拆除，
         * 但如果資料包允許其他相關群系，
         * 仍須保持龍戰鬥狀態同步。
         */
        EndDragonFight fight = level.getDragonFight();

        if (fight != null) {
            fight.onCrystalDestroyed(crystal, source);
        }

        /*
         * ======================================================
         * 返還終界水晶
         * ======================================================
         */

        if (!player.getAbilities().instabuild) {

            ItemStack result =
                    new ItemStack(Items.END_CRYSTAL);

            // 優先放入背包
            player.getInventory().add(result);

            // 若無法完全放入，就丟在玩家位置
            if (!result.isEmpty()) {
                player.drop(result, false);
            }
        }

        /*
         * ======================================================
         * 拆除音效
         * ======================================================
         */
        level.playSound(
                null,
                x,
                y,
                z,
                SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS,
                1.0F,
                0.9F
        );
    }
}
