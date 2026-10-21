package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.starrepublic.beyond_the_end.Register.Tags;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

import java.util.function.BooleanSupplier;

public class WeakEndPortalFrameBlock extends Block {
    public static final MapCodec<WeakEndPortalFrameBlock> CODEC = simpleCodec(WeakEndPortalFrameBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_EYE = BlockStateProperties.EYE;
    protected static final VoxelShape BASE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);
    protected static final VoxelShape EYE_SHAPE = Block.box(4.0, 13.0, 4.0, 12.0, 16.0, 12.0);
    protected static final VoxelShape FULL_SHAPE = Shapes.or(BASE_SHAPE, EYE_SHAPE);


    @Override
    public MapCodec<WeakEndPortalFrameBlock> codec() {
        return CODEC;
    }

    public WeakEndPortalFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_EYE, Boolean.valueOf(false)));
    }
    @Override
    protected boolean useShapeForLightOcclusion(BlockState blockstate) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState blockstate, BlockGetter blockgetter, BlockPos blockpos, CollisionContext context) {
        return blockstate.getValue(HAS_EYE) ? FULL_SHAPE : BASE_SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HAS_EYE, Boolean.valueOf(false));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState blockstate) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockstate, Level level, BlockPos pos) {
        return blockstate.getValue(HAS_EYE) ? 15 : 0;
    }

    @Override
    protected BlockState rotate(BlockState blockstate, Rotation rotation) {
        return blockstate.setValue(FACING, rotation.rotate(blockstate.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState blockstate, Mirror mirror) {
        return blockstate.rotate(mirror.getRotation(blockstate.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_EYE);
    }
    @Override
    protected boolean isPathfindable(BlockState blockState, PathComputationType type) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        // 只允許 TAG 內的物品執行框架激活互動
        if (!stack.is(Tags.ACTIVATOR_ITEMS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 已經插入激活物品
        if (state.getValue(HAS_EYE)) {
            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide) {

            // 寫入框架狀態
            BlockState newState = state.setValue(HAS_EYE, true);

            if (!level.setBlock(pos, newState, Block.UPDATE_ALL)) {
                return ItemInteractionResult.FAIL;
            }

            // 播放原版終界之眼插入效果
            level.levelEvent(1503, pos, 0);

            // 創造模式不消耗物品
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            // 插入完成後檢查傳送門結構
            WeakEndPortalStructure.tryActivate(level, pos);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide
                && !state.is(newState.getBlock())
                && state.getValue(HAS_EYE)) {
            WeakEndPortalStructure.removeNearbyPortals(level, pos);
            //WeakEndPortalStructure.removeConnectedPortal(level, pos,newState);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

}
