package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class EndSurfaceCoverBlock
        extends Block
        implements BonemealableBlock {

    /*
     * 被完全覆蓋後退化成什麼。
     *
     * Enchanted End Stone
     * → End Stone
     *
     * 其他群系可以指定自己的底層方塊。
     */
    private final Supplier<? extends Block> baseBlock;


    /*
     * 對地表使用骨粉後生成哪一套植被。
     */
    @Nullable
    private final ResourceKey<ConfiguredFeature<?, ?>>
            bonemealFeature;


    public EndSurfaceCoverBlock(
            BlockBehaviour.Properties properties,

            Supplier<? extends Block> baseBlock,

            @Nullable
            ResourceKey<ConfiguredFeature<?, ?>> bonemealFeature
    ) {

        super(properties);

        this.baseBlock =
                baseBlock;

        this.bonemealFeature =
                bonemealFeature;
    }


    /*
     * ==========================================
     * 地表存活判定
     * ==========================================
     *
     * 思路參考 Nylium：
     *
     * 上面如果是完全阻光的實心方塊，
     * 地表覆蓋會死亡，退化成底層方塊。
     *
     * Air / Grass / Flower / Snow Layer
     * 等不會造成退化。
     */
    private static boolean canRemainSurface(
            LevelReader level,
            BlockPos pos
    ) {

        BlockPos abovePos =
                pos.above();

        BlockState aboveState =
                level.getBlockState(
                        abovePos
                );


        return aboveState.getLightBlock(
                level,
                abovePos
        ) < level.getMaxLightLevel();
    }


    /*
     * ==========================================
     * 被覆蓋後退化
     * ==========================================
     */
    @Override
    protected void randomTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {

        if (!canRemainSurface(
                level,
                pos
        )) {

            level.setBlockAndUpdate(
                    pos,

                    baseBlock
                            .get()
                            .defaultBlockState()
            );
        }
    }


    /*
     * ==========================================
     * 是否可以使用骨粉
     * ==========================================
     */
    @Override
    public boolean isValidBonemealTarget(
            LevelReader level,
            BlockPos pos,
            BlockState state
    ) {

        return bonemealFeature != null
                && level.getBlockState(
                pos.above()
        ).isAir();
    }


    @Override
    public boolean isBonemealSuccess(
            Level level,
            RandomSource random,
            BlockPos pos,
            BlockState state
    ) {

        return true;
    }


    /*
     * ==========================================
     * 骨粉生成群系植被
     * ==========================================
     */
    @Override
    public void performBonemeal(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state
    ) {

        if (bonemealFeature == null) {
            return;
        }


        Registry<ConfiguredFeature<?, ?>> registry =
                level.registryAccess()
                        .registryOrThrow(
                                Registries.CONFIGURED_FEATURE
                        );


        registry.getHolder(
                bonemealFeature
        ).ifPresent(holder -> {

            holder.value().place(
                    level,

                    level.getChunkSource()
                            .getGenerator(),

                    random,

                    pos.above()
            );
        });
    }


    /*
     * 和 Nylium 類型一致：
     * 骨粉效果主要作用於周圍區域。
     */
    @Override
    public Type getType() {

        return Type.NEIGHBOR_SPREADER;
    }
}
