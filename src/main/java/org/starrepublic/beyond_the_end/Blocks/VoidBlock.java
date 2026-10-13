package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VoidBlock extends Block {

    private static final int EFFECT_DURATION = 500;
    private static final int EFFECT_AMPLIFIER = 5;

    public VoidBlock(Properties properties) {
        super(properties);
    }

    /*
     * 完全沒有實體碰撞。
     */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    /*
     * 雖然模型是一整個 Cube，
     * 但不要讓它遮掉相鄰方塊的面。
     */
    @Override
    protected VoxelShape getOcclusionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity
    ) {
        super.entityInside(state, level, pos, entity);

        if (level.isClientSide()) {
            return;
        }

        if (entity instanceof LivingEntity livingEntity) {

            livingEntity.addEffect(
                    new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN,
                            EFFECT_DURATION,
                            EFFECT_AMPLIFIER,
                            false,
                            false,
                            true
                    )
            );
        }
    }
}
