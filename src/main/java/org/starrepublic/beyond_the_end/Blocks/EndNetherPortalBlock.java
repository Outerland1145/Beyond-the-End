package org.starrepublic.beyond_the_end.Blocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Entitys.EndNetherPortalBlockEntity;

public class EndNetherPortalBlock extends BaseEntityBlock implements Portal {

    public static final EnumProperty<Direction.Axis> AXIS =
            BlockStateProperties.HORIZONTAL_AXIS;

    public static final MapCodec<EndNetherPortalBlock> CODEC =
            simpleCodec(EndNetherPortalBlock::new);

    public EndNetherPortalBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(AXIS, Direction.Axis.X)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {

        builder.add(AXIS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // 不走普通 Block Model
        // 完全交給 BlockEntityRenderer
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context) {

        Direction.Axis axis = state.getValue(AXIS);

        // 和地獄門類似，非常薄
        return axis == Direction.Axis.X
                ? Shapes.box(0.0, 0.0, 0.375, 1.0, 1.0, 0.625)
                : Shapes.box(0.375, 0.0, 0.0, 0.625, 1.0, 1.0);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context) {

        return Shapes.empty();
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity) {

        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(
            ServerLevel level,
            Entity entity) {

        if (entity instanceof Player player) {

            return Math.max(
                    0,
                    level.getGameRules().getInt(
                            player.getAbilities().invulnerable
                                    ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY
                                    : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY
                    )
            );
        }

        return 0;
    }

    @Override
    @Nullable
    public DimensionTransition getPortalDestination(
            ServerLevel currentLevel,
            Entity entity,
            BlockPos portalPos
    ) {
        ServerLevel targetLevel;
        double targetX;
        double targetZ;

        // ==========================================
        // END -> NETHER
        //
        // 終界 1 格 = 地獄 8 格
        //
        // 所以：
        // End X100 -> Nether X800
        // ==========================================
        if (currentLevel.dimension() == Level.END) {

            targetLevel = currentLevel
                    .getServer()
                    .getLevel(Level.NETHER);

            if (targetLevel == null) {
                return null;
            }

            targetX = entity.getX() / 8.0D;
            targetZ = entity.getZ() / 8.0D;
        }

        // ==========================================
        // NETHER -> END
        //
        // Nether X800 -> End X100
        // ==========================================
        else if (currentLevel.dimension() == Level.NETHER) {

            targetLevel = currentLevel
                    .getServer()
                    .getLevel(Level.END);

            if (targetLevel == null) {
                return null;
            }

            targetX = entity.getX() * 8.0D;
            targetZ = entity.getZ() * 8.0D;
        }

        // 主世界以及其他維度都不能使用
        else {
            return null;
        }

        /*
         * 目前先保留 Y。
         *
         * 之後正式加入出口 Portal 搜尋後，
         * Y 會由找到的 / 新生成的 Portal 決定。
         */
        Vec3 rawTargetPos = new Vec3(
                targetX,
                entity.getY(),
                targetZ
        );

        /*
         * 防止 ×8 之後跑出世界邊界。
         *
         * 1.21.1 WorldBorder 有 clampToBounds(Vec3)。
         */
        Vec3 targetPos = Vec3.atLowerCornerOf(targetLevel
                .getWorldBorder()
                .clampToBounds(rawTargetPos));

        return new DimensionTransition(
                targetLevel,
                targetPos,
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                false,

                /*
                 * Portal 傳送完成後：
                 *
                 * 1. 播放 Portal 音效
                 * 2. 加入 Portal chunk ticket
                 */
                DimensionTransition.PLAY_PORTAL_SOUND
                        .then(DimensionTransition.PLACE_PORTAL_TICKET)
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state) {

        return new EndNetherPortalBlockEntity(pos, state);
    }
}
