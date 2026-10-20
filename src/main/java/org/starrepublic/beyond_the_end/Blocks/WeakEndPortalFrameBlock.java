package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
}
