package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;

public class AltarPedestalBlockEntity extends BlockEntity {

    private ItemStack item = ItemStack.EMPTY;

    private int progress = 0;
    private String activeRecipe = "";

    @Nullable
    private BlockPos ownerAltar = null;

    public AltarPedestalBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                BlockEntityRegister.ALTAR_PEDESTAL.get(),
                pos,
                state
        );
    }

    // ========================================================
    // Item
    // ========================================================

    public ItemStack getItem() {
        return item.copy();
    }

    public boolean isEmpty() {
        return item.isEmpty();
    }

    /**
     * 嘗試插入物品。
     * 回傳實際插入數量。
     */
    public int insert(ItemStack source, int requested) {

        if (level == null || level.isClientSide
                || source.isEmpty() || requested <= 0) {
            return 0;
        }

        if (!item.isEmpty()
                && !ItemStack.isSameItemSameComponents(item, source)) {
            return 0;
        }

        int current = item.isEmpty() ? 0 : item.getCount();
        int capacity = source.getMaxStackSize() - current;

        int inserted = Math.min(
                Math.min(requested, source.getCount()),
                capacity
        );

        if (inserted <= 0) {
            return 0;
        }

        if (item.isEmpty()) {
            item = source.copyWithCount(inserted);
        } else {
            item.grow(inserted);
        }

        resetProgress();
        markUpdated();

        return inserted;
    }

    /**
     * 取出指定數量。
     */
    public ItemStack extract(int amount) {

        if (level == null || level.isClientSide
                || amount <= 0 || item.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = item.split(amount);

        if (item.isEmpty()) {
            item = ItemStack.EMPTY;
        }

        resetProgress();
        markUpdated();

        return extracted;
    }

    /**
     * 由祭壇呼叫，每次完成一份配方。
     *
     * 若基座只有一份輸入，直接替換成產物。
     * 若有多份輸入，消耗一份，產物交給呼叫方生成。
     */
    public ItemStack completeOne(ItemStack result) {

        if (level == null || level.isClientSide
                || item.isEmpty() || result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack output = result.copy();

        if (item.getCount() == 1) {

            item = output;
            output = ItemStack.EMPTY;

        } else {

            item.shrink(1);
        }

        resetProgress();
        markUpdated();

        return output;
    }

    // ========================================================
    // Altar ownership
    // ========================================================

    @Nullable
    public BlockPos getOwnerAltar() {
        return ownerAltar;
    }

    public boolean claim(BlockPos altarPos) {

        if (level == null || level.isClientSide) {
            return false;
        }

        if (ownerAltar != null && !ownerAltar.equals(altarPos)) {
            return false;
        }

        if (ownerAltar == null) {
            ownerAltar = altarPos.immutable();
            resetProgress();
            markUpdated();
        }

        return true;
    }

    public void release(BlockPos altarPos) {

        if (level == null || level.isClientSide) {
            return;
        }

        if (ownerAltar != null && ownerAltar.equals(altarPos)) {

            ownerAltar = null;
            resetProgress();
            markUpdated();
        }
    }

    // ========================================================
    // Recipe progress
    // ========================================================

    public int getProgress() {
        return progress;
    }

    public String getActiveRecipe() {
        return activeRecipe;
    }

    public void setActiveRecipe(String recipeId) {

        if (recipeId.equals(activeRecipe)) {
            return;
        }

        activeRecipe = recipeId;
        progress = 0;

        markUpdated();
    }

    public void advanceProgress() {

        if (level == null || level.isClientSide) {
            return;
        }

        progress++;
        markUpdated();
    }

    private void resetProgress() {
        progress = 0;
        activeRecipe = "";
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

        if (!item.isEmpty()) {
            tag.put("Item", item.save(registries));
        }

        tag.putInt("Progress", progress);
        tag.putString("ActiveRecipe", activeRecipe);

        if (ownerAltar != null) {
            tag.putLong("OwnerAltar", ownerAltar.asLong());
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        item = tag.contains("Item", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(
                registries,
                tag.getCompound("Item")
        )
                : ItemStack.EMPTY;

        progress = Math.max(0, tag.getInt("Progress"));
        activeRecipe = tag.getString("ActiveRecipe");

        ownerAltar = tag.contains("OwnerAltar", Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong("OwnerAltar"))
                : null;
    }

    // ========================================================
    // Client synchronization
    // ========================================================

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdated() {

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
