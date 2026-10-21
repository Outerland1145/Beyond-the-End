package org.starrepublic.beyond_the_end.Events;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.dimension.end.EndDragonFight;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import org.starrepublic.beyond_the_end.Entitys.entity.SafeEndCrystalEntity;
import org.starrepublic.beyond_the_end.Register.ItemRegister;
import org.starrepublic.beyond_the_end.Register.Tags;

public class CrystalRemoverEvents {

    private CrystalRemoverEvents() {}

    /*
     * ======================================================
     * 工具模式
     * ======================================================
     */

    public enum Mode {
        REMOVER(
                "mode.beyond_the_end.crystal_remover.remove"
        ),

        ADJUST(
                "mode.beyond_the_end.crystal_remover.adjust"
        );

        private final String translationKey;

        Mode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        public static Mode byId(int id) {
            Mode[] modes = values();
            return modes[Math.floorMod(id, modes.length)];
        }
    }

    private static final String MODE_KEY =
            "CrystalRemoverMode";

    /*
     * 取得單件工具目前的模式。
     *
     * 沒有資料的舊版工具預設為模式 0，
     * 即原版終界水晶拆除模式。
     */
    public static Mode getMode(ItemStack stack) {

        CompoundTag tag = stack.getOrDefault(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY
        ).copyTag();

        return Mode.byId(tag.getInt(MODE_KEY));
    }

    /*
     * 儲存模式，不覆蓋其他 Custom Data。
     */
    private static void setMode(
            ItemStack stack,
            Mode mode
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putInt(
                        MODE_KEY,
                        mode.ordinal()
                )
        );
    }

    /*
     * 滾輪封包到達伺服器後呼叫。
     *
     * direction:
     *  1 = 下一個模式
     * -1 = 上一個模式
     */
    public static void changeMode(
            ServerPlayer player,
            int direction
    ) {
        if (direction != 1 && direction != -1) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        if (!stack.is(ItemRegister.END_CRYSTAL_REMOVER.get())) {
            return;
        }

        Mode current = getMode(stack);

        Mode next = Mode.byId(
                current.ordinal() + direction
        );

        setMode(stack, next);

        player.displayClientMessage(
                Component.translatable(
                        "message.beyond_the_end.crystal_remover_mode",
                        Component.translatable(next.translationKey())
                ),
                true
        );
    }

    /*
     * ======================================================
     * 原本的右鍵互動事件
     * ======================================================
     */

    public static void onCrystalInteract(
            PlayerInteractEvent.EntityInteractSpecific event
    ) {
        Entity target = event.getTarget();

        // 原版水晶或安全水晶
        if (!(target instanceof EndCrystal)
                && !(target instanceof SafeEndCrystalEntity)) {
            return;
        }

        // 保留原本 ItemRegister 的判斷
        ItemStack tool = event.getItemStack();

        if (!tool.is(ItemRegister.END_CRYSTAL_REMOVER.get())) {
            return;
        }

        /*
         * 保留原本的事件取消機制。
         *
         * 避免原版或其他正常右鍵互動流程
         * 繼續處理水晶。
         */
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        // 所有實際操作由伺服器負責
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (target.isRemoved()) {
            return;
        }

        Player player = event.getEntity();
        Mode mode = getMode(tool);

        /*
         * ==================================================
         * 原有群系 TAG 黑名單
         * ==================================================
         *
         * 拆除模式：
         * - 禁止拆除
         * - 不返還物品
         * - 不消耗耐久
         *
         * 調整模式：
         * - 不會摧毀水晶
         * - 因此不套用拆除黑名單
         */


        /*
         * ==================================================
         * 按照模式執行操作
         * ==================================================
         */

        boolean success = false;

        switch (mode) {

            case REMOVER -> {

                if (target instanceof EndCrystal crystal) {
                    if (mode != Mode.ADJUST
                            && level.getBiome(target.blockPosition())
                            .is(Tags.CRYSTAL_REMOVER_BLACKLIST)) {

                        player.displayClientMessage(
                                Component.translatable(
                                        "message.beyond_the_end.crystal_remover_blocked"
                                ),
                                true
                        );

                        return;
                    }
                    else {
                        success = removeVanillaCrystal(
                                level,
                                player,
                                crystal
                        );
                    }

                }
                else if (target instanceof SafeEndCrystalEntity crystal) {
                    success = removeSafeCrystal(
                            level,
                            player,
                            crystal
                    );
                }
            }

            case ADJUST -> {

                success = adjustCrystal(
                        level,
                        target
                );
            }
        }

        if (!success) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.beyond_the_end.crystal_remover_invalid"
                    ),
                    true
            );

            return;
        }

        /*
         * ==================================================
         * 工具耐久度
         * ==================================================
         *
         * 成功操作才消耗 1 點耐久。
         * 依原版耐久系統處理損耗及附魔。
         */
        if (!player.getAbilities().instabuild) {
            switch (mode) {

                case REMOVER -> {
                    EquipmentSlot slot =
                            event.getHand() == InteractionHand.MAIN_HAND
                                    ? EquipmentSlot.MAINHAND
                                    : EquipmentSlot.OFFHAND;

                    tool.hurtAndBreak(
                            1,
                            player,
                            slot
                    );
                }
                //調整水晶基座不消耗耐久
                case ADJUST -> {}
            }

        }

        /*
         * ==================================================
         * 操作音效
         * ==================================================
         *
         * 拆除仍使用原有 GLASS_BREAK。
         * 調整時使用 LEVER_CLICK。
         */
        level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                mode == Mode.ADJUST
                        ? SoundEvents.LEVER_CLICK
                        : SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS,
                1.0F,
                mode == Mode.ADJUST ? 1.0F : 0.9F
        );
    }

    /*
     * ======================================================
     * 模式 0：原版水晶安全拆除
     * ======================================================
     *
     * 保留原本完整的拆除方式。
     */
    private static boolean removeVanillaCrystal(
            ServerLevel level,
            Player player,
            EndCrystal crystal
    ) {
        if (crystal.isRemoved()) {
            return false;
        }

        /*
         * 只建立傷害來源。
         * 不對水晶造成實際傷害。
         */
        DamageSource source =
                player.damageSources().playerAttack(player);

        /*
         * 直接移除，避免 EndCrystal#hurt
         * 觸發爆炸。
         */
        crystal.discard();

        /*
         * 保留你原本的終界龍戰鬥狀態同步。
         */
        EndDragonFight fight = level.getDragonFight();

        if (fight != null) {

            fight.onCrystalDestroyed(
                    crystal,
                    source
            );
        }

        /*
         * 保留原本的水晶返還方式。
         * 創造模式不返還物品。
         */
        if (!player.getAbilities().instabuild) {

            ItemStack result =
                    new ItemStack(Items.END_CRYSTAL);

            player.getInventory().add(result);

            if (!result.isEmpty()) {
                player.drop(result, false);
            }
        }

        return true;
    }

    /*
     * ======================================================
     * 模式 1：安全水晶拆除
     * ======================================================
     */
    private static boolean removeSafeCrystal(
            ServerLevel level,
            Player player,
            SafeEndCrystalEntity crystal
    ) {
        if (crystal.isRemoved()) {
            return false;
        }

        /*
         * 安全水晶不是原版 EndCrystal。
         *
         * 直接移除，不進行 EndDragonFight 通知，
         * 也不會觸發爆炸。
         */
        crystal.discard();

        if (!player.getAbilities().instabuild) {

            ItemStack result = new ItemStack(
                    ItemRegister.SAFE_END_CRYSTAL.get()
            );

            player.getInventory().add(result);

            if (!result.isEmpty()) {
                player.drop(result, false);
            }
        }

        return true;
    }

    /*
     * ======================================================
     * 模式 2：調整水晶基座
     * ======================================================
     */
    private static boolean adjustCrystal(
            ServerLevel level,
            Entity target
    ) {
        // 原版水晶
        if (target instanceof EndCrystal crystal) {

            crystal.setShowBottom(
                    !crystal.showsBottom()
            );

            return true;
        }

        // 安全水晶
        if (target instanceof SafeEndCrystalEntity crystal) {

            crystal.setShowBottom(
                    !crystal.showsBottom()
            );

            return true;
        }

        return false;
    }

    /*
     * ======================================================
     * 物品提示
     * ======================================================
     *
     * 無須另外建立 Item 子類別。
     */
    public static void onItemTooltip(
            ItemTooltipEvent event
    ) {
        ItemStack stack = event.getItemStack();

        if (!stack.is(ItemRegister.END_CRYSTAL_REMOVER.get())) {
            return;
        }

        Mode mode = getMode(stack);

        event.getToolTip().add(
                Component.translatable(
                        "tooltip.beyond_the_end.crystal_remover_mode",
                        Component.translatable(mode.translationKey())
                ).withStyle(ChatFormatting.AQUA)
        );

        event.getToolTip().add(
                Component.translatable(
                        "tooltip.beyond_the_end.crystal_remover_switch"
                ).withStyle(ChatFormatting.GRAY)
        );
    }
}
