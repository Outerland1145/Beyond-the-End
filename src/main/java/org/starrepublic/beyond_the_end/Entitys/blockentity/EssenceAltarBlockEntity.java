package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
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
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        essence = Math.max(0, tag.getInt("Essence"));
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

