package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.starrepublic.beyond_the_end.Fluid.EnderGoo.EnderGooEffects;
import org.starrepublic.beyond_the_end.Register.Tags;

public class EnderGooCauldronBlock extends AbstractCauldronBlock {

    public static final CauldronInteraction.InteractionMap INTERACTIONS =
            CauldronInteraction.newInteractionMap(
                    "beyond_the_end:ender_goo"
            );

    public static final MapCodec<EnderGooCauldronBlock> CODEC =
            simpleCodec(
                    EnderGooCauldronBlock::new
            );

    public EnderGooCauldronBlock(Properties properties) {
        super(
                properties,
                INTERACTIONS
        );
    }
    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity
    ) {

        super.entityInside(
                state,
                level,
                pos,
                entity
        );


        if (
                !isEntityInsideContent(
                        state,
                        pos,
                        entity
                )
        ) {
            return;
        }


        EnderGooEffects.applyHazard(
                level,
                entity
        );
    }
    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return true;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return 0.9375D;
    }
}
