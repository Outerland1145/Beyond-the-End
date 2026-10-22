package org.starrepublic.beyond_the_end.Items.charm;

import net.minecraft.core.component.DataComponents;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.items.ItemHandlerCopySlot;
import org.starrepublic.beyond_the_end.Items.rune.RuneHelper;
import org.starrepublic.beyond_the_end.Register.ItemRegister;
import org.starrepublic.beyond_the_end.Register.MenuRegister;

import net.minecraft.world.item.crafting.RecipeHolder;
import org.starrepublic.beyond_the_end.Register.RecipeRegister;
import org.starrepublic.beyond_the_end.recipes.CharmRecipe.CharmRuneInput;
import org.starrepublic.beyond_the_end.recipes.CharmRecipe.CharmRuneRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CharmPouchMenu extends AbstractContainerMenu {

    public static final int POUCH_SLOTS = 5;

    public static final int RUNE_SLOTS = 5;

    public static final int RESULT_SLOT =
            POUCH_SLOTS + RUNE_SLOTS;

    public static final int PLAYER_START =
            RESULT_SLOT + 1;

    private static final int PLAYER_SLOTS = 36;

    private final Inventory inventory;
    private final int pouchInventorySlot;
    private final ItemStack pouchStack;

    private final SimpleContainer runeInventory =
            new SimpleContainer(RUNE_SLOTS);

    private final SimpleContainer resultInventory =
            new SimpleContainer(1);

    private boolean updatingResult;

    /**
     * 客戶端建構器。
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
     * 伺服器建構器。
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

        this.pouchStack =
                inventory.player.level().isClientSide()
                        ? new ItemStack(
                        ItemRegister.CHARMPOUCH.get()
                )
                        : inventory.getItem(pouchInventorySlot);

        /*
         * 1. 五個護符儲存槽。
         *
         * 維持 Data Component 儲存。
         */
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

                        // 不能刪除，否則空槽位同步時會斷線。
                        if (stack.isEmpty()) {
                            return true;
                        }

                        return stack.is(
                                ItemRegister.CHARMITEM.get()
                        ) && CharmHelper.isValidCharm(
                                stack,
                                inventory.player.registryAccess()
                        );
                    }

                    @Override
                    public int getSlotLimit(int slot) {
                        return 1;
                    }
                };

        for (int i = 0; i < POUCH_SLOTS; i++) {

            addSlot(
                    new ItemHandlerCopySlot(
                            handler,
                            i,
                            44 + i * 18,
                            26
                    )
            );
        }

        /*
         * 2. 五個符文輸入槽。
         *
         * 使用暫時容器，不會寫入護符袋。
         */
        for (int i = 0; i < RUNE_SLOTS; i++) {

            addSlot(
                    new Slot(
                            runeInventory,
                            i,
                            26 + i * 18,
                            58
                    ) {

                        @Override
                        public boolean mayPlace(
                                ItemStack stack
                        ) {

                            return stack.isEmpty()
                                    || RuneHelper.isValidRune(
                                    stack,
                                    inventory.player.registryAccess()
                            );
                        }

                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }

                        @Override
                        public int getMaxStackSize(
                                ItemStack stack
                        ) {
                            return 1;
                        }
                    }
            );
        }

        /*
         * 3. 合成結果槽。
         */
        addSlot(
                new Slot(
                        resultInventory,
                        0,
                        139,
                        58
                ) {

                    @Override
                    public boolean mayPlace(
                            ItemStack stack
                    ) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {

                        if (player.level().isClientSide()) {
                            return this.hasItem();
                        }

                        Optional<RecipeHolder<CharmRuneRecipe>> match =
                                getMatchingRecipe();

                        if (match.isEmpty()) {
                            return false;
                        }

                        ItemStack expected =
                                match.get().value().getResultItem(
                                        player.registryAccess()
                                );

                        return !expected.isEmpty()
                                && ItemStack.isSameItemSameComponents(
                                this.getItem(),
                                expected
                        );
                    }

                    @Override
                    public void onTake(
                            Player player,
                            ItemStack taken
                    ) {

                        if (!player.level().isClientSide()) {
                            craftResult(taken);
                        }

                        super.onTake(player, taken);
                    }
                }
        );

        /*
         * 4. 玩家背包。
         */
        for (int row = 0; row < 3; row++) {

            for (int column = 0; column < 9; column++) {

                int slot = 9 + column + row * 9;

                addPlayerSlot(
                        slot,
                        8 + column * 18,
                        108 + row * 18
                );
            }
        }

        /*
         * 5. 快捷欄。
         */
        for (int column = 0; column < 9; column++) {

            addPlayerSlot(
                    column,
                    8 + column * 18,
                    166
            );
        }

        /*
         * 符文輸入變動後重新尋找配方。
         */
        runeInventory.addListener(
                container -> updateCraftResult()
        );

        updateCraftResult();
    }

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
     * 建立當前的配方輸入。
     */
    private CharmRuneInput getRuneInput() {

        List<ItemStack> items =
                new ArrayList<>(RUNE_SLOTS);

        for (int i = 0; i < RUNE_SLOTS; i++) {

            items.add(
                    runeInventory.getItem(i).copy()
            );
        }

        return new CharmRuneInput(items);
    }

    /**
     * 向 Minecraft RecipeManager 查詢合成結果。
     *
     * 不直接讀取 Java 內建清單，
     * 因此被魔改移除的配方不會匹配。
     */
    private Optional<RecipeHolder<CharmRuneRecipe>>
    getMatchingRecipe() {

        return inventory.player.level()
                .getRecipeManager()
                .getRecipeFor(
                        RecipeRegister.TYPE.get(),
                        getRuneInput(),
                        inventory.player.level()
                )
                .filter(holder ->
                        !holder.value()
                                .getResultItem(
                                        inventory.player.registryAccess()
                                ).isEmpty()
                );
    }

    /**
     * 更新合成結果預覽。
     *
     * 只由伺服器進行權威計算。
     */
    private void updateCraftResult() {

        if (updatingResult
                || inventory.player.level().isClientSide()) {
            return;
        }

        updatingResult = true;

        try {

            ItemStack result = getMatchingRecipe()
                    .map(holder ->
                            holder.value().getResultItem(
                                    inventory.player.registryAccess()
                            )
                    )
                    .orElse(ItemStack.EMPTY);

            resultInventory.setItem(
                    0,
                    result.copy()
            );

            broadcastChanges();

        } finally {
            updatingResult = false;
        }
    }

    /**
     * 玩家取得結果後消耗符文。
     *
     * 嚴格驗證當前輸入與結果一致。
     */
    private void craftResult(ItemStack taken) {

        Optional<RecipeHolder<CharmRuneRecipe>> match =
                getMatchingRecipe();

        if (match.isEmpty()) {
            updateCraftResult();
            return;
        }

        ItemStack expected =
                match.get().value().getResultItem(
                        inventory.player.registryAccess()
                );

        if (expected.isEmpty()
                || !ItemStack.isSameItemSameComponents(
                expected,
                taken
        )) {

            updateCraftResult();
            return;
        }

        updatingResult = true;

        try {

            for (int i = 0; i < RUNE_SLOTS; i++) {

                ItemStack stack =
                        runeInventory.getItem(i);

                if (stack.isEmpty()) {
                    continue;
                }

                ItemStack remaining = stack.copy();

                remaining.shrink(1);

                runeInventory.setItem(
                        i,
                        remaining
                );
            }

        } finally {
            updatingResult = false;
        }

        updateCraftResult();
    }

    /**
     * 防止操作正在打開的護符袋。
     */
    @Override
    public void clicked(
            int slotId,
            int button,
            ClickType clickType,
            Player player
    ) {

        int lockedSlot =
                PLAYER_START + 27 + pouchInventorySlot;

        if (slotId == lockedSlot) {
            return;
        }

        if (clickType == ClickType.SWAP
                && button == pouchInventorySlot) {
            return;
        }
        if (slotId == RESULT_SLOT
                && clickType != ClickType.PICKUP) {
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
     * Shift 快捷搬運。
     *
     * 合成結果暫時不允許 Shift 連續製作，
     * 避免在多次合成的狀態尚未完整驗證時
     * 造成物品複製。
     */
    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {

        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        if (index == RESULT_SLOT) {
            return ItemStack.EMPTY;
        }

        int lockedSlot =
                PLAYER_START + 27 + pouchInventorySlot;

        if (index == lockedSlot) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack current = slot.getItem();
        ItemStack original = current.copy();

        if (index < PLAYER_START) {

            // 護符袋或符文槽 -> 玩家背包
            if (!moveItemStackTo(
                    current,
                    PLAYER_START,
                    PLAYER_START + PLAYER_SLOTS,
                    true
            )) {
                return ItemStack.EMPTY;
            }

        } else {

            // 玩家背包 -> 護符袋
            if (CharmHelper.isValidCharm(
                    current,
                    player.registryAccess()
            )) {

                if (!moveItemStackTo(
                        current,
                        0,
                        POUCH_SLOTS,
                        false
                )) {
                    return ItemStack.EMPTY;
                }

            } else if (RuneHelper.isValidRune(
                    current,
                    player.registryAccess()
            )) {

                // 玩家背包 -> 符文槽
                if (!moveItemStackTo(
                        current,
                        POUCH_SLOTS,
                        RESULT_SLOT,
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

    @Override
    public boolean stillValid(Player player) {

        if (player.level().isClientSide()) {
            return true;
        }

        return player.isAlive()
                && inventory.getItem(pouchInventorySlot) == pouchStack
                && pouchStack.is(
                ItemRegister.CHARMPOUCH.get()
        );
    }

    /**
     * 關閉 GUI 時退還尚未使用的符文。
     */
    @Override
    public void removed(Player player) {

        super.removed(player);

        if (player.level().isClientSide()) {
            return;
        }

        for (int i = 0; i < RUNE_SLOTS; i++) {

            ItemStack stack =
                    runeInventory.removeItemNoUpdate(i);

            if (!stack.isEmpty()) {

                player.getInventory()
                        .placeItemBackInInventory(
                                stack
                        );
            }
        }
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
