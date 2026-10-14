package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class EndVault extends BaseEntityBlock {
    public static final MapCodec<EndVault> CODEC = simpleCodec(EndVault::new);
    public static final Property<VaultState> STATE = BlockStateProperties.VAULT_STATE;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    @Override
    public MapCodec<EndVault> codec() {
        return CODEC;
    }

    public EndVault(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STATE, VaultState.INACTIVE)
        );
    }

    @Override
    public ItemInteractionResult useItemOn(
            ItemStack itemStack,
            BlockState blockState,
            Level level,
            BlockPos blockpos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (itemStack.isEmpty() || blockState.getValue(STATE) != VaultState.ACTIVE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        } else if (level instanceof ServerLevel serverlevel) {
            if (serverlevel.getBlockEntity(blockpos) instanceof VaultBlockEntity vaultblockentity) {
                VaultBlockEntity.Server.tryInsertKey(
                        serverlevel,
                        blockpos,
                        blockState,
                        vaultblockentity.getConfig(),
                        vaultblockentity.getServerData(),
                        vaultblockentity.getSharedData(),
                        player,
                        itemStack
                );
                return ItemInteractionResult.SUCCESS;
            } else {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        } else {
            return ItemInteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VaultBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateManager) {
        stateManager.add(FACING, STATE);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return level instanceof ServerLevel serverlevel
                ? createTickerHelper(
                blockEntityType,
                BlockEntityType.VAULT,
                (p_323957_, p_324322_, p_323828_, p_323769_) -> VaultBlockEntity.Server.tick(
                        serverlevel, p_324322_, p_323828_, p_323769_.getConfig(), p_323769_.getServerData(), p_323769_.getSharedData()
                )
        )
                : createTickerHelper(
                blockEntityType,
                BlockEntityType.VAULT,
                (p_324290_, p_323926_, p_323941_, p_323489_) -> VaultBlockEntity.Client.tick(
                        p_324290_, p_323926_, p_323941_, p_323489_.getClientData(), p_323489_.getSharedData()
                )
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState().setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState blockState, Rotation rotation) {
        return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState blockState, Mirror mirror) {
        return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }
}
