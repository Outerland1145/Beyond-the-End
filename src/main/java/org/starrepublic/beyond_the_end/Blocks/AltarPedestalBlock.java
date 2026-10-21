package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Entitys.blockentity.AltarPedestalBlockEntity;

public class AltarPedestalBlock extends BaseEntityBlock {

    public static final MapCodec<AltarPedestalBlock> CODEC =
            simpleCodec(AltarPedestalBlock::new);
    public static final BooleanProperty OUTPUT =
            BooleanProperty.create("output");
    //private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);
    public AltarPedestalBlock(BlockBehaviour.Properties properties) {
        super(properties);

        // false = 輸入，true = 輸出
        registerDefaultState(
                stateDefinition.any().setValue(OUTPUT, false)
        );
    }
    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(OUTPUT);
    }
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

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
        // 輸出模式不允許玩家直接放入材料
        if (state.getValue(OUTPUT)) {

            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.beyond_the_end.pedestal_output_only"
                        ),
                        true
                );
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
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

        if (!level.isClientSide) {

            // 物品未取出前，不允許切換模式
            if (!pedestal.isEmpty()) {

                player.displayClientMessage(
                        Component.translatable(
                                "message.beyond_the_end.pedestal_not_empty"
                        ),
                        true
                );

                return InteractionResult.SUCCESS;
            }

            // 切換輸入 / 輸出
            boolean newOutput = !state.getValue(OUTPUT);
            BlockPos owner = pedestal.getOwnerAltar();

            if (owner != null) {
                pedestal.release(owner);
            }
            level.setBlock(
                    pos,
                    state.setValue(OUTPUT, newOutput),
                    Block.UPDATE_ALL
            );

            player.displayClientMessage(
                    Component.translatable(
                            newOutput
                                    ? "message.beyond_the_end.pedestal_output"
                                    : "message.beyond_the_end.pedestal_input"
                    ),
                    true
            );
        }

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
