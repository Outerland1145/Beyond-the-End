package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Entitys.blockentity.AltarPedestalBlockEntity;

public class AltarPedestalBlock extends BaseEntityBlock {

    public static final MapCodec<AltarPedestalBlock> CODEC =
            simpleCodec(AltarPedestalBlock::new);

    //private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public AltarPedestalBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    //@Override
    //protected VoxelShape getShape(
    //        BlockState state,
    //        BlockGetter level,
    //        BlockPos pos,
    //        CollisionContext context
    //) {
    //    return SHAPE;
    //}

    //@Override
    //protected VoxelShape getCollisionShape(
    //        BlockState state,
    //        BlockGetter level,
    //       BlockPos pos,
    //        CollisionContext context
    //) {
    //    return SHAPE;
    //}

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new AltarPedestalBlockEntity(pos, state);
    }

    // ========================================================
    // 放入物品
    // ========================================================

    // ========================================================
// 手持物品右鍵：放入一件
// ========================================================

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {

        if (!(level.getBlockEntity(pos)
                instanceof AltarPedestalBlockEntity pedestal)) {

            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (player.isSpectator()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 空手交給 useWithoutItem
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        AltarPedestalInteraction.interact(
                player,
                pedestal,
                stack,
                false
        );

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

// ========================================================
// 空手右鍵：取出一件
// ========================================================

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {

        if (!(level.getBlockEntity(pos)
                instanceof AltarPedestalBlockEntity pedestal)) {

            return InteractionResult.PASS;
        }

        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }

        AltarPedestalInteraction.interact(
                player,
                pedestal,
                ItemStack.EMPTY,
                false
        );

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ========================================================
    // 基座破壞
    // ========================================================

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {

        if (state.getBlock() != newState.getBlock()) {

            if (!level.isClientSide
                    && level.getBlockEntity(pos)
                    instanceof AltarPedestalBlockEntity pedestal) {

                ItemStack stack = pedestal.getItem();

                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }
}
