package org.starrepublic.beyond_the_end.Entitys.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.Blocks;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public class SafeEndCrystalEntity extends Entity {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "safe_end_crystal"
            );
    private static final EntityDataAccessor<Boolean> DATA_SHOW_BOTTOM =
            SynchedEntityData.defineId(
                    SafeEndCrystalEntity.class,
                    EntityDataSerializers.BOOLEAN
            );
    public SafeEndCrystalEntity(
            EntityType<? extends SafeEndCrystalEntity> type,
            Level level
    ) {
        super(type, level);

        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(DATA_SHOW_BOTTOM, false);
    }
    public boolean showsBottom() {
        return this.entityData.get(DATA_SHOW_BOTTOM);
    }

    public void setShowBottom(boolean showBottom) {
        this.entityData.set(DATA_SHOW_BOTTOM, showBottom);
    }
    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        this.setShowBottom(
                !tag.contains("ShowBottom")
                        || tag.getBoolean("ShowBottom")
        );
    }
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        // 原版水晶只在終界產生火焰
        if (this.level().dimension() != Level.END) {
            return;
        }

        BlockPos pos = this.blockPosition();

        if (this.level().getBlockState(pos).isAir()) {
            this.level().setBlockAndUpdate(
                    pos,
                    Blocks.FIRE.defaultBlockState()
            );
        }
    }
    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putBoolean(
                "ShowBottom",
                this.showsBottom()
        );
    }

    /**
     * 免疫所有普通傷害。
     *
     * 普通武器、箭矢、爆炸等
     * 都無法破壞此實體。
     */
    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        return false;
    }

    /**
     * 必須能被玩家的準星選取，
     * 否則水晶拆除器可能無法鎖定目標。
     */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * 創造模式滑鼠中鍵取得安全水晶物品。
     */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(
                BuiltInRegistries.ITEM.get(ID)
        );
    }

    /**
     * 提供給已存在的水晶拆除器呼叫。
     *
     * 只有拆除器通過其原有的使用條件後，
     * 才應呼叫此方法。
     *
     * 伺服器執行，成功後返還安全水晶物品。
     */
    public boolean removeWithCrystalDisassembler(
            Player player
    ) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }

        ItemStack returnedItem = getPickResult();

        this.discard();

        if (!returnedItem.isEmpty()) {

            if (!player.addItem(returnedItem)) {
                player.drop(returnedItem, false);
            }
        }

        return true;
    }
}
