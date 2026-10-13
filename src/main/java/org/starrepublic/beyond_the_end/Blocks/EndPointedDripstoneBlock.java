package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class EndPointedDripstoneBlock  extends Block
        implements SimpleWaterloggedBlock {

    /*
     * 直接使用 Minecraft 已存在的 Property。
     *
     * JSON 中仍然是：
     *
     * vertical_direction=up/down
     * thickness=tip/frustum/middle/base/tip_merge
     * waterlogged=true/false
     */
    public static final DirectionProperty TIP_DIRECTION =
            BlockStateProperties.VERTICAL_DIRECTION;

    public static final EnumProperty<DripstoneThickness> THICKNESS =
            BlockStateProperties.DRIPSTONE_THICKNESS;

    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;


    /* -------------------------
       碰撞 / 選取形狀
       ------------------------- */

    private static final VoxelShape TIP_UP =
            Block.box(5.0D, 0.0D, 5.0D,
                    11.0D, 11.0D, 11.0D);

    private static final VoxelShape TIP_DOWN =
            Block.box(5.0D, 5.0D, 5.0D,
                    11.0D, 16.0D, 11.0D);

    private static final VoxelShape TIP_MERGE =
            Block.box(5.0D, 0.0D, 5.0D,
                    11.0D, 16.0D, 11.0D);

    private static final VoxelShape FRUSTUM =
            Block.box(4.0D, 0.0D, 4.0D,
                    12.0D, 16.0D, 12.0D);

    private static final VoxelShape MIDDLE =
            Block.box(3.0D, 0.0D, 3.0D,
                    13.0D, 16.0D, 13.0D);

    private static final VoxelShape BASE =
            Block.box(2.0D, 0.0D, 2.0D,
                    14.0D, 16.0D, 14.0D);


    public EndPointedDripstoneBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(TIP_DIRECTION, Direction.UP)
                        .setValue(THICKNESS, DripstoneThickness.TIP)
                        .setValue(WATERLOGGED, false)
        );
    }


    /* =========================================================
       放置
       ========================================================= */

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Direction direction = findPlacementDirection(
                level,
                pos,
                context.getClickedFace()
        );

        if (direction == null) {
            return null;
        }

        boolean waterlogged =
                context.getLevel()
                        .getFluidState(pos)
                        .getType() == Fluids.WATER;

        DripstoneThickness thickness =
                calculateThickness(
                        level,
                        pos,
                        direction,
                        true
                );

        return this.defaultBlockState()
                .setValue(TIP_DIRECTION, direction)
                .setValue(THICKNESS, thickness)
                .setValue(WATERLOGGED, waterlogged);
    }


    /**
     * 判斷這一格應該朝上還是朝下。
     */
    @Nullable
    private Direction findPlacementDirection(
            LevelReader level,
            BlockPos pos,
            Direction clickedFace
    ) {

        /*
         * 如果直接點上 / 下表面，
         * 優先使用玩家點擊的方向。
         */
        if (clickedFace.getAxis() == Direction.Axis.Y) {

            if (canAttach(level, pos, clickedFace)) {
                return clickedFace;
            }

            Direction opposite = clickedFace.getOpposite();

            if (canAttach(level, pos, opposite)) {
                return opposite;
            }
        }

        /*
         * 側面放置時自己尋找合法方向。
         */
        if (canAttach(level, pos, Direction.UP)) {
            return Direction.UP;
        }

        if (canAttach(level, pos, Direction.DOWN)) {
            return Direction.DOWN;
        }

        return null;
    }


    /* =========================================================
       支撐判定
       ========================================================= */

    private boolean canAttach(
            LevelReader level,
            BlockPos pos,
            Direction tipDirection
    ) {

        Direction supportDirection = tipDirection.getOpposite();

        BlockPos supportPos =
                pos.relative(supportDirection);

        BlockState supportState =
                level.getBlockState(supportPos);

        /*
         * 自己的鐘乳石可以成為下一段的支撐。
         */
        if (isSameDripstone(
                supportState,
                tipDirection
        )) {
            return true;
        }

        /*
         * 或普通實心方塊。
         */
        return supportState.isFaceSturdy(
                level,
                supportPos,
                tipDirection
        );
    }


    @Override
    protected boolean canSurvive(
            BlockState state,
            LevelReader level,
            BlockPos pos
    ) {

        Direction direction =
                state.getValue(TIP_DIRECTION);

        return canAttach(
                level,
                pos,
                direction
        );
    }


    /* =========================================================
       最重要：
       自訂鐘乳石的「同類」判斷
       ========================================================= */

    private boolean isSameDripstone(
            BlockState state,
            Direction direction
    ) {

        return state.is(this)
                && state.hasProperty(TIP_DIRECTION)
                && state.getValue(TIP_DIRECTION) == direction;
    }


    /* =========================================================
       計算 TIP / FRUSTUM / MIDDLE / BASE
       ========================================================= */

    private DripstoneThickness calculateThickness(
            LevelReader level,
            BlockPos pos,
            Direction direction,
            boolean allowMerge
    ) {

        /*
         * 朝尖端方向的鄰居。
         *
         * UP：
         *
         * current
         *   ↑
         * forward
         *
         * DOWN 則相反。
         */
        BlockState forward =
                level.getBlockState(
                        pos.relative(direction)
                );


        /*
         * ======================================
         * 對面是反方向鐘乳石
         *
         * ▲
         * ▼
         *
         * => TIP_MERGE
         * ======================================
         */

        if (isSameDripstone(
                forward,
                direction.getOpposite()
        )) {

            return allowMerge
                    ? DripstoneThickness.TIP_MERGE
                    : DripstoneThickness.TIP;
        }


        /*
         * 前面沒有自己的鐘乳石：
         *
         *     ▲
         *
         * => TIP
         */
        if (!isSameDripstone(
                forward,
                direction
        )) {
            return DripstoneThickness.TIP;
        }


        /*
         * 前面本身是 TIP，
         * 那自己就是 TIP 下面較粗的一節。
         */
        DripstoneThickness forwardThickness =
                forward.getValue(THICKNESS);

        if (forwardThickness == DripstoneThickness.TIP
                || forwardThickness
                == DripstoneThickness.TIP_MERGE) {

            return DripstoneThickness.FRUSTUM;
        }


        /*
         * 再看背後。
         */
        BlockState behind =
                level.getBlockState(
                        pos.relative(direction.getOpposite())
                );


        /*
         * 背後不是同方向鐘乳石：
         *
         * 自己就是最底端 / 最頂端根部。
         */
        if (!isSameDripstone(
                behind,
                direction
        )) {

            return DripstoneThickness.BASE;
        }


        /*
         * 兩邊都有鐘乳石：
         *
         * => MIDDLE
         */
        return DripstoneThickness.MIDDLE;
    }


    /* =========================================================
       鄰居更新
       ========================================================= */

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {

        if (state.getValue(WATERLOGGED)) {

            level.scheduleTick(
                    pos,
                    Fluids.WATER,
                    Fluids.WATER.getTickDelay(level)
            );
        }


        /*
         * 只有上下改變才可能影響鐘乳石結構。
         */
        if (direction.getAxis()
                != Direction.Axis.Y) {

            return state;
        }


        /*
         * 支撐消失。
         */
        if (!canSurvive(
                state,
                level,
                pos
        )) {

            return Blocks.AIR.defaultBlockState();
        }


        Direction tipDirection =
                state.getValue(TIP_DIRECTION);


        /*
         * 重新計算粗細。
         */
        DripstoneThickness newThickness =
                calculateThickness(
                        level,
                        pos,
                        tipDirection,
                        true
                );


        if (newThickness
                != state.getValue(THICKNESS)) {

            return state.setValue(
                    THICKNESS,
                    newThickness
            );
        }

        return state;
    }


    /* =========================================================
       水
       ========================================================= */

    @Override
    protected FluidState getFluidState(
            BlockState state
    ) {

        if (state.getValue(WATERLOGGED)) {
            return Fluids.WATER.getSource(false);
        }

        return super.getFluidState(state);
    }


    /* =========================================================
       Shape
       ========================================================= */

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {

        DripstoneThickness thickness =
                state.getValue(THICKNESS);

        Direction direction =
                state.getValue(TIP_DIRECTION);


        return switch (thickness) {

            case TIP ->
                    direction == Direction.UP
                            ? TIP_UP
                            : TIP_DOWN;

            case TIP_MERGE ->
                    TIP_MERGE;

            case FRUSTUM ->
                    FRUSTUM;

            case MIDDLE ->
                    MIDDLE;

            case BASE ->
                    BASE;
        };
    }


    @Override
    protected VoxelShape getOcclusionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return Shapes.empty();
    }


    /* =========================================================
       BlockState
       ========================================================= */

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {

        builder.add(
                TIP_DIRECTION,
                THICKNESS,
                WATERLOGGED
        );
    }
}
