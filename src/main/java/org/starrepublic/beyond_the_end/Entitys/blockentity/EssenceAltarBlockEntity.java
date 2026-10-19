package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Register.Tags;

public abstract class EssenceAltarBlockEntity extends BlockEntity {

    // ========================================================
    // 共用精華儲存
    // ========================================================

    protected int essence = 0;
    private ItemStack displayEssenceItem = ItemStack.EMPTY;
    public ItemStack getDisplayEssenceItem() {
        return displayEssenceItem.copy();
    }

    // ========================================================
// 依照祭壇類型檢查精華物品
// ========================================================

    public boolean acceptsEssenceItem(ItemStack stack) {

        if (stack.isEmpty()) {
            return false;
        }

        TagKey<Item> tag;

        switch (getEssenceType()) {

            case "dragon":
                tag = Tags.DRAGON_ESSENCE;
                break;

            case "soul":
                tag = Tags.SOUL_ESSENCE;
                break;

            case "fire":
                tag = Tags.FIRE_ESSENCE;
                break;

            default:
                return false;
        }

        if (!stack.is(tag)) {
            return false;
        }

        ItemStack current = getDisplayEssenceItem();

        return essence <= 0
                || current.isEmpty()
                || ItemStack.isSameItemSameComponents(current, stack);
    }

    private final IItemHandler essenceAutomationHandler = new IItemHandler() {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {

            if (slot != 0 || getEssence() <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack template = getDisplayEssenceItem();

            if (template.isEmpty()) {
                return ItemStack.EMPTY;
            }

            return template.copyWithCount(
                    Math.min(
                            getEssence(),
                            template.getMaxStackSize()
                    )
            );
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 64 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && acceptsEssenceItem(stack);
        }

        // ====================================================
        // 自動輸入精華
        // ====================================================

        @Override
        public ItemStack insertItem(
                int slot,
                ItemStack stack,
                boolean simulate
        ) {

            if (slot != 0 || !acceptsEssenceItem(stack)) {
                return stack;
            }

            int accepted = Math.min(
                    stack.getCount(),
                    Integer.MAX_VALUE - getEssence()
            );

            if (accepted <= 0) {
                return stack;
            }

            if (!simulate) {

                accepted = addEssence(stack, accepted);

                if (accepted <= 0) {
                    return stack;
                }
            }

            int remainder = stack.getCount() - accepted;

            return remainder <= 0
                    ? ItemStack.EMPTY
                    : stack.copyWithCount(remainder);
        }

        // ====================================================
        // 自動提取精華
        // ====================================================

        @Override
        public ItemStack extractItem(
                int slot,
                int amount,
                boolean simulate
        ) {

            if (slot != 0 || amount <= 0 || getEssence() <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack template = getDisplayEssenceItem();

            if (template.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int extracted = Math.min(
                    amount,
                    getEssence()
            );

            extracted = Math.min(
                    extracted,
                    template.getMaxStackSize()
            );

            if (extracted <= 0) {
                return ItemStack.EMPTY;
            }

            if (!simulate && !consumeEssence(extracted)) {
                return ItemStack.EMPTY;
            }

            return template.copyWithCount(extracted);
        }
    };

    public IItemHandler getEssenceAutomationHandler() {
        return essenceAutomationHandler;
    }

    /**
     * 接受精華並記錄其顯示用物品。
     */
    public int addEssence(ItemStack source, int amount) {

        if (level == null || level.isClientSide
                || source.isEmpty() || amount <= 0) {
            return 0;
        }

        // 已有精華時，不允許混合不同物品
        if (essence > 0
                && !displayEssenceItem.isEmpty()
                && !ItemStack.isSameItemSameComponents(
                displayEssenceItem,
                source
        )) {
            return 0;
        }

        int accepted = Math.min(
                amount,
                Integer.MAX_VALUE - essence
        );

        if (accepted <= 0) {
            return 0;
        }

        // 記錄實際投入的精華物品
        if (displayEssenceItem.isEmpty() || essence == 0) {
            displayEssenceItem = source.copyWithCount(1);
        }

        essence += accepted;

        markUpdated();

        return accepted;
    }

    // ========================================================
    // 破壞祭壇時返還所有精華
    // ========================================================

    public void dropAllEssence(ServerLevel serverLevel) {

        if (essence <= 0) {
            return;
        }

        // 使用先前投入時記錄的物品
        ItemStack template = displayEssenceItem.copy();

        if (template.isEmpty()) {
            return;
        }

        int remaining = essence;

        // 先清空，避免移除流程重複執行造成複製
        essence = 0;
        displayEssenceItem = ItemStack.EMPTY;

        setChanged();

        if (!serverLevel.getGameRules().getBoolean(
                GameRules.RULE_DOBLOCKDROPS
        )) {
            return;
        }

        // 按照物品本身的堆疊上限分批生成
        while (remaining > 0) {

            int count = Math.min(
                    remaining,
                    template.getMaxStackSize()
            );

            ItemStack drop = template.copyWithCount(count);

            ItemEntity entity = new ItemEntity(
                    serverLevel,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 1.05D,
                    worldPosition.getZ() + 0.5D,
                    drop
            );

            // 隨機向上及四周噴出
            double velocityX =
                    (serverLevel.random.nextDouble() - 0.5D) * 0.40D;

            double velocityY =
                    0.15D + serverLevel.random.nextDouble() * 0.25D;

            double velocityZ =
                    (serverLevel.random.nextDouble() - 0.5D) * 0.40D;

            entity.setDeltaMovement(
                    velocityX,
                    velocityY,
                    velocityZ
            );

            entity.setDefaultPickUpDelay();

            serverLevel.addFreshEntity(entity);

            remaining -= count;
        }
    }
    protected EssenceAltarBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    // ========================================================
    // 祭壇識別
    // ========================================================

    public abstract String getEssenceType();

    public ResourceLocation getAltarId() {
        return BuiltInRegistries.BLOCK.getKey(
                getBlockState().getBlock()
        );
    }

    // ========================================================
    // 精華管理
    // ========================================================

    public int getEssence() {
        return essence;
    }

    public int addEssence(int amount) {

        if (level == null || level.isClientSide || amount <= 0) {
            return 0;
        }

        int accepted = Math.min(
                amount,
                Integer.MAX_VALUE - essence
        );

        if (accepted <= 0) {
            return 0;
        }

        essence += accepted;

        markUpdated();

        return accepted;
    }

    public boolean consumeEssence(int amount) {

        if (level == null || level.isClientSide) {
            return false;
        }

        if (amount < 0 || essence < amount) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        essence -= amount;

        markUpdated();

        return true;
    }

    public boolean hasEnoughEssence(int amount) {
        return amount >= 0 && essence >= amount;
    }

    // ========================================================
    // NBT
    // ========================================================

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.putInt("Essence", essence);
        if (!displayEssenceItem.isEmpty()) {
            tag.put(
                    "DisplayEssence",
                    displayEssenceItem.save(registries)
            );
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        essence = Math.max(0, tag.getInt("Essence"));
        displayEssenceItem =
                tag.contains("DisplayEssence", Tag.TAG_COMPOUND)
                        ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound("DisplayEssence")
                )
                        : ItemStack.EMPTY;
    }

    // ========================================================
    // 客戶端同步
    // ========================================================

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = new CompoundTag();

        saveAdditional(tag, registries);

        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ========================================================
    // 更新
    // ========================================================

    protected void markUpdated() {

        setChanged();

        if (level != null && !level.isClientSide) {

            BlockState state = getBlockState();

            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    Block.UPDATE_CLIENTS
            );
        }
    }
}

