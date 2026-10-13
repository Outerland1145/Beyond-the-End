package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.entity.item.FallingBlockEntity;
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

import java.util.ArrayList;
import java.util.List;

public class EndPointedDripstoneBlock  extends Block
        implements SimpleWaterloggedBlock, Fallable {
    private static final int DELAY_BEFORE_FALLING = 2;

    private static final int MAX_STALACTITE_HEIGHT_FOR_DAMAGE_CALCULATION = 6;

    private static final float STALACTITE_DAMAGE_PER_FALL_DISTANCE_AND_SIZE = 1.0F;

    private static final int STALACTITE_MAX_DAMAGE = 40;

    private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.0F;

    private static final float STALAGMITE_FALL_DAMAGE_MODIFIER = 2.0F;
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

        Direction tipDirection =
                state.getValue(TIP_DIRECTION);


        /*
         * 尖端方向 UP：
         * 支撐在 DOWN。
         *
         * 尖端方向 DOWN：
         * 支撐在 UP。
         */
        Direction supportDirection =
                tipDirection.getOpposite();


        BlockPos supportPos =
                pos.relative(supportDirection);


        BlockState supportState =
                level.getBlockState(supportPos);


        /*
         * 情況 1：
         * 普通完整支撐面。
         */
        if (supportState.isFaceSturdy(
                level,
                supportPos,
                tipDirection
        )) {
            return true;
        }


        /*
         * 情況 2：
         * 同方向的 End Dripstone。
         */
        return isSameDripstone(
                supportState,
                tipDirection
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

        /*
         * Waterlogged
         */
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(
                    pos,
                    Fluids.WATER,
                    Fluids.WATER.getTickDelay(level)
            );
        }


        /*
         * 原版只處理上下方向的結構變化。
         */
        if (direction != Direction.UP
                && direction != Direction.DOWN) {

            return state;
        }


        Direction tipDirection =
                state.getValue(TIP_DIRECTION);


        /*
         * ==========================================
         * 原版：
         *
         * 向下鐘乳石如果已經有 scheduled tick，
         * 不再重複處理。
         * ==========================================
         */
        if (tipDirection == Direction.DOWN
                && level.getBlockTicks()
                .hasScheduledTick(pos, this)) {

            return state;
        }


        /*
         * ==========================================
         * 支撐方向的方塊發生改變，而且自己已經
         * 無法繼續存在。
         *
         * DOWN：2 tick 後墜落
         * UP：  1 tick 後正常破壞
         *
         * 重要：
         * 這裡絕對不能 return AIR。
         * ==========================================
         */
        if (direction == tipDirection.getOpposite()
                && !canSurvive(state, level, pos)) {

            level.scheduleTick(
                    pos,
                    this,
                    tipDirection == Direction.DOWN
                            ? 2
                            : 1
            );

            return state;
        }


        /*
         * 仍然有支撐：
         * 重新計算粗細。
         */
        boolean wasMerged =
                state.getValue(THICKNESS)
                        == DripstoneThickness.TIP_MERGE;


        DripstoneThickness newThickness =
                calculateThickness(
                        level,
                        pos,
                        tipDirection,
                        wasMerged
                );


        return state.setValue(
                THICKNESS,
                newThickness
        );
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
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {

        /*
         * ==========================================
         * 原版邏輯：
         *
         * 向上的石筍：
         * 如果 scheduled tick 發生時仍然沒有支撐，
         * 只破壞「目前這一格」。
         *
         * true = 正常掉落
         * ==========================================
         */
        if (isPointingUp(state)
                && !canSurvive(state, level, pos)) {

            level.destroyBlock(
                    pos,
                    true
            );
        }
        else
        {
            spawnFallingStalactite(
                    state,
                    level,
                    pos
            );
        }

        /*
         * ==========================================
         * 其他情況就是向下鐘乳石：
         *
         * 轉成 FallingBlockEntity。
         * ==========================================
         */

    }
    private boolean isPointingUp(BlockState state) {

        return isSameDripstone(
                state,
                Direction.UP
        );
    }


    private boolean isPointingDown(BlockState state) {

        return isSameDripstone(
                state,
                Direction.DOWN
        );
    }
    private void spawnFallingStalactite(
            BlockState state,
            ServerLevel level,
            BlockPos pos
    ) {

        BlockPos.MutableBlockPos currentPos =
                pos.mutable();

        BlockState currentState = state;

        int stalactiteLength = 0;


        /*
         * 從失去支撐的根部一路往下搜尋。
         */
        while (isSameDripstone(
                currentState,
                Direction.DOWN
        )) {

            stalactiteLength++;


            /*
             * 把目前這節方塊轉換成 FallingBlockEntity。
             */
            FallingBlockEntity fallingBlock =
                    FallingBlockEntity.fall(
                            level,
                            currentPos,
                            currentState
                    );


            /*
             * 尖端才負責造成「落下鐘乳石」傷害。
             *
             * 長度越大，危險性越高；
             * 但原版最多只計算 6 格。
             */
            if (isTip(currentState, true)) {

                int effectiveSize =
                        Math.min(
                                stalactiteLength,
                                MAX_STALACTITE_HEIGHT_FOR_DAMAGE_CALCULATION
                        );

                fallingBlock.setHurtsEntities(
                        STALACTITE_DAMAGE_PER_FALL_DISTANCE_AND_SIZE
                                * effectiveSize,

                        STALACTITE_MAX_DAMAGE
                );

                break;
            }


            /*
             * 繼續往下一格。
             */
            currentPos.move(Direction.DOWN);

            currentState =
                    level.getBlockState(currentPos);
        }
    }
    private boolean isTip(
            BlockState state,
            boolean includeMergedTip
    ) {

        if (!state.is(this)) {
            return false;
        }

        DripstoneThickness thickness =
                state.getValue(THICKNESS);

        if (thickness == DripstoneThickness.TIP) {
            return true;
        }

        return includeMergedTip
                && thickness == DripstoneThickness.TIP_MERGE;
    }
    @Override
    public DamageSource getFallDamageSource(Entity entity) {

        return entity.damageSources()
                .fallingStalactite(entity);
    }
    @Override
    public void onBrokenAfterFall(
            Level level,
            BlockPos pos,
            FallingBlockEntity fallingBlock
    ) {

        level.playSound(
                null,
                pos,
                SoundEvents.POINTED_DRIPSTONE_LAND,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
    }
    @Override
    public void fallOn(
            Level level,
            BlockState state,
            BlockPos pos,
            Entity entity,
            float fallDistance
    ) {

        /*
         * 只有朝上的尖端造成石筍傷害。
         */
        if (state.getValue(TIP_DIRECTION) == Direction.UP
                && state.getValue(THICKNESS)
                == DripstoneThickness.TIP) {

            entity.causeFallDamage(
                    fallDistance
                            + STALAGMITE_FALL_DISTANCE_OFFSET,

                    STALAGMITE_FALL_DAMAGE_MODIFIER,

                    level.damageSources().stalagmite()
            );

            return;
        }

        super.fallOn(
                level,
                state,
                pos,
                entity,
                fallDistance
        );
    }
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
