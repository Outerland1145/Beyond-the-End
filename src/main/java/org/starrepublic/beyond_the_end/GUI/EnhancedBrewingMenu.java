package org.starrepublic.beyond_the_end.GUI;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EnhancedBrewingStandBlockEntity;
import org.starrepublic.beyond_the_end.Register.MenuRegister;
import org.starrepublic.beyond_the_end.recipes.enhancedbrewing.EnhancedBrewingRecipes;

public class EnhancedBrewingMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 5;
    private static final int PLAYER_START = 5;
    private static final int PLAYER_END = 41;

    private final Container container;
    private final ContainerData data;

    /**
     * 客戶端建構器。
     *
     * 客戶端使用相同大小的空容器，
     * 實際物品與進度由伺服器同步。
     */
    public EnhancedBrewingMenu(
            int containerId,
            Inventory inventory
    ) {
        this(
                containerId,
                inventory,
                new SimpleContainer(MACHINE_SLOTS),
                new SimpleContainerData(3)
        );
    }

    /**
     * 伺服器端建構器。
     */
    public EnhancedBrewingMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data
    ) {
        super(
                MenuRegister.Enchanted_Brewing.get(),
                containerId
        );

        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, 3);

        this.container = container;
        this.data = data;

        container.startOpen(inventory.player);

        /*
         * 三個藥水瓶欄位。
         *
         * 原版藥水輸入：
         * PotionBrewing.isInput()
         *
         * 自訂資料包藥水輸入：
         * EnhancedBrewingRecipes.isInput()
         */
        int[][] bottlePositions = {
                {56, 51},
                {79, 58},
                {102, 51}
        };

        for (int i = 0; i < 3; i++) {

            addSlot(
                    new Slot(
                            container,
                            i,
                            bottlePositions[i][0],
                            bottlePositions[i][1]
                    ) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {

                            if (stack.isEmpty()) {
                                return false;
                            }

                            Level level = inventory.player.level();

                            return level.potionBrewing().isInput(stack)
                                    || EnhancedBrewingRecipes.isInput(
                                    level,
                                    stack
                            );
                        }

                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }

                        @Override
                        public int getMaxStackSize(ItemStack stack) {
                            return 1;
                        }
                    }
            );
        }

        /*
         * 釀造材料欄位：索引 3。
         *
         * 崩潰發生在這個欄位的 mayPlace()。
         *
         * 使用 inventory.player.level() 取得 Level，
         * 絕不將 registryAccess() 強制轉型。
         */
        addSlot(
                new Slot(
                        container,
                        3,
                        79,
                        17
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {

                        if (stack.isEmpty()) {
                            return false;
                        }

                        Level level = inventory.player.level();

                        return level.potionBrewing().isIngredient(stack)
                                || EnhancedBrewingRecipes.isIngredient(
                                level,
                                stack
                        );
                    }
                }
        );

        /*
         * 終界粉燃料欄位：索引 4。
         *
         * 不接受烈焰粉。
         */
        addSlot(
                new Slot(
                        container,
                        4,
                        17,
                        17
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {

                        return !stack.isEmpty()
                                && EnhancedBrewingStandBlockEntity
                                .isFuel(stack);
                    }
                }
        );

        /*
         * 玩家背包 27 格。
         * 選單索引 5～31。
         */
        for (int row = 0; row < 3; row++) {

            for (int column = 0; column < 9; column++) {

                addSlot(
                        new Slot(
                                inventory,
                                column + row * 9 + 9,
                                8 + column * 18,
                                84 + row * 18
                        )
                );
            }
        }

        /*
         * 玩家快捷欄 9 格。
         * 選單索引 32～40。
         */
        for (int column = 0; column < 9; column++) {

            addSlot(
                    new Slot(
                            inventory,
                            column,
                            8 + column * 18,
                            142
                    )
            );
        }

        addDataSlots(data);
    }

    public int getRemainingBrewTime() {
        return data.get(0);
    }

    public int getFuelUses() {
        return data.get(1);
    }

    public int getTotalBrewTime() {
        return data.get(2);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    /**
     * Shift 快捷搬運。
     *
     * 自動判定燃料、材料及藥水瓶。
     * 設備中的物品則移回玩家背包。
     */
    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {

        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot source = slots.get(index);

        if (!source.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();

        if (index < MACHINE_SLOTS) {

            // 設備 -> 玩家背包
            if (!moveItemStackTo(
                    stack,
                    PLAYER_START,
                    PLAYER_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }

        } else {

            /*
             * 玩家背包 -> 設備
             *
             * 優先順序：
             * 1. 終界粉燃料
             * 2. 釀造材料
             * 3. 藥水瓶
             */

            if (slots.get(4).mayPlace(stack)) {

                if (!moveItemStackTo(
                        stack,
                        4,
                        5,
                        false
                )) {
                    return ItemStack.EMPTY;
                }

            } else if (slots.get(3).mayPlace(stack)) {

                if (!moveItemStackTo(
                        stack,
                        3,
                        4,
                        false
                )) {
                    return ItemStack.EMPTY;
                }

            } else if (slots.get(0).mayPlace(stack)) {

                if (!moveItemStackTo(
                        stack,
                        0,
                        3,
                        false
                )) {
                    return ItemStack.EMPTY;
                }

            } else if (index < 32) {

                // 玩家主背包 -> 快捷欄
                if (!moveItemStackTo(
                        stack,
                        32,
                        PLAYER_END,
                        false
                )) {
                    return ItemStack.EMPTY;
                }

            } else {

                // 快捷欄 -> 玩家主背包
                if (!moveItemStackTo(
                        stack,
                        PLAYER_START,
                        32,
                        false
                )) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            source.set(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        source.onTake(player, stack);

        return original;
    }

    @Override
    public void removed(Player player) {

        super.removed(player);

        container.stopOpen(player);
    }
}

