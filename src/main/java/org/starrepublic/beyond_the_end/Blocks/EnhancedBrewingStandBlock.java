package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EnhancedBrewingStandBlockEntity;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;

public class EnhancedBrewingStandBlock
        extends BrewingStandBlock {

    public static final MapCodec<BrewingStandBlock> CODEC =
            simpleCodec(EnhancedBrewingStandBlock::new);

    public EnhancedBrewingStandBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }
    @Override
    public MapCodec<BrewingStandBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new EnhancedBrewingStandBlockEntity(
                pos,
                state
        );
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {

        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(
                type,
                BlockEntityRegister.ENCHANTEDBREWING.get(),
                EnhancedBrewingStandBlockEntity::serverTick
        );
    }

    /**
     * 必須自行開啟方塊實體的選單，
     * 因為原版釀造台使用自己的 BlockEntity 類型。
     */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {

        if (!level.isClientSide()) {

            if (level.getBlockEntity(pos)
                    instanceof EnhancedBrewingStandBlockEntity brewing) {

                player.openMenu(brewing);
            }
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }
}
