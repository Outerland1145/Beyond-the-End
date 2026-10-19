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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public abstract class EssenceAltarBlockEntity extends BlockEntity {

    // ========================================================
    // 共用精華儲存
    // ========================================================

    protected int essence = 0;
    private ItemStack displayEssenceItem = ItemStack.EMPTY;
    public ItemStack getDisplayEssenceItem() {
        return displayEssenceItem.copy();
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

