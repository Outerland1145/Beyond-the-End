package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.items.ItemHandlerCopySlot;

import org.starrepublic.beyond_the_end.Register.ItemRegister;
import org.starrepublic.beyond_the_end.Register.MenuRegister;

public class CharmPouchMenu extends AbstractContainerMenu {

    private static final int POUCH_SLOTS = 5;
    private static final int PLAYER_SLOTS = 36;

    private final Inventory inventory;
    private final int pouchInventorySlot;
    private final ItemStack pouchStack;

    /**
     * 客戶端建構器。
     *
     * 不從額外封包讀取資料。
     * 物品欄位內容由原版容器同步機制處理。
     */
    public CharmPouchMenu(
            int containerId,
            Inventory inventory
    ) {
        this(
                containerId,
                inventory,
                inventory.selected
        );
    }

    /**
     * 伺服器端建構器。
     */
    public CharmPouchMenu(
            int containerId,
            Inventory inventory,
            int inventorySlot
    ) {

        super(
                MenuRegister.CHARM_POUCH.get(),
                containerId
        );

        this.inventory = inventory;

        this.pouchInventorySlot =
                Math.clamp(inventorySlot, 0, 8);

        /*
         * 伺服器使用背包內真正的 ItemStack。
         *
         * 客戶端則使用臨時容器，
         * 欄位內容以伺服器的同步資料為準。
         */
        this.pouchStack = inventory.player.level().isClientSide()
                ? new ItemStack(ItemRegister.CHARMPOUCH.get())
                : inventory.getItem(pouchInventorySlot);

        ComponentItemHandler handler =
                new ComponentItemHandler(
                        pouchStack,
                        DataComponents.CONTAINER,
                        POUCH_SLOTS
                ) {

                    @Override
                    public boolean isItemValid(
                            int slot,
                            ItemStack stack
                    ) {

                        // 必須允許空物品。
                        // 客戶端初始化、取出物品、同步空欄位
                        // 都會使用 ItemStack.EMPTY。
                        if (stack.isEmpty()) {
                            return true;
                        }

                        // 非空物品只能是護符。
                        return stack.is(
                                ItemRegister.CHARMITEM.get()
                        );
                    }

                    @Override
                    public int getSlotLimit(int slot) {
                        return 1;
                    }
                };

        /*
         * 護符袋的三個欄位。
         *
         * 只能放入護符，不能放入其他物品。
         */
        for (int i = 0; i < POUCH_SLOTS; i++) {

            addSlot(
                    new ItemHandlerCopySlot(
                            handler,
                            i,
                            62 + i * 18,
                            26
                    )
            );
        }

        /*
         * 玩家背包 27 格。
         */
        for (int row = 0; row < 3; row++) {

            for (int column = 0; column < 9; column++) {

                int slot =
                        column + row * 9 + 9;

                addPlayerSlot(
                        slot,
                        8 + column * 18,
                        84 + row * 18
                );
            }
        }

        /*
         * 玩家快捷欄 9 格。
         */
        for (int column = 0; column < 9; column++) {

            addPlayerSlot(
                    column,
                    8 + column * 18,
                    142
            );
        }
    }

    /**
     * 背包欄位。
     *
     * 持有正在開啟的護符袋的那一格不可操作。
     */
    private void addPlayerSlot(
            int inventoryIndex,
            int x,
            int y
    ) {

        addSlot(
                new Slot(
                        inventory,
                        inventoryIndex,
                        x,
                        y
                ) {

                    @Override
                    public boolean mayPickup(Player player) {
                        return inventoryIndex != pouchInventorySlot;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return inventoryIndex != pouchInventorySlot;
                    }
                }
        );
    }

    /**
     * 防止用快捷鍵交換或丟棄正在編輯的護符袋。
     */
    @Override
    public void clicked(
            int slotId,
            int button,
            ClickType clickType,
            Player player
    ) {

        int lockedMenuSlot =
                POUCH_SLOTS + 27 + pouchInventorySlot;

        if (slotId == lockedMenuSlot) {
            return;
        }

        if (clickType == ClickType.SWAP
                && button == pouchInventorySlot) {
            return;
        }

        super.clicked(
                slotId,
                button,
                clickType,
                player
        );
    }

    /**
     * Shift + 左鍵快捷搬運。
     */
    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {

        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        int lockedMenuSlot =
                POUCH_SLOTS + 27 + pouchInventorySlot;

        if (index == lockedMenuSlot) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack current = slot.getItem();
        ItemStack original = current.copy();

        if (index < POUCH_SLOTS) {

            // 護符袋 -> 玩家背包
            if (!moveItemStackTo(
                    current,
                    POUCH_SLOTS,
                    POUCH_SLOTS + PLAYER_SLOTS,
                    true
            )) {
                return ItemStack.EMPTY;
            }

        } else {

            // 玩家背包 -> 護符袋
            if (current.is(ItemRegister.CHARMITEM.get())) {

                if (!moveItemStackTo(
                        current,
                        0,
                        POUCH_SLOTS,
                        false
                )) {
                    return ItemStack.EMPTY;
                }

            } else {
                return ItemStack.EMPTY;
            }
        }

        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, current);

        return original;
    }

    /**
     * 只有原本那個護符袋仍位於原本快捷欄時，
     * 才允許繼續編輯。
     */
    @Override
    public boolean stillValid(Player player) {

        if (player.level().isClientSide()) {
            return true;
        }

        return inventory.getItem(pouchInventorySlot) == pouchStack
                && pouchStack.is(ItemRegister.CHARMPOUCH.get())
                && player.isAlive();
    }

    public static void registerMenuScreens(
            RegisterMenuScreensEvent event
    ) {

        event.register(
                MenuRegister.CHARM_POUCH.get(),
                CharmPouchScreen::new
        );
    }
}
