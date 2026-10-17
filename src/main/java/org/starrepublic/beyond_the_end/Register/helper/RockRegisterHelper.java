package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

public class RockRegisterHelper {
    public final DeferredBlock<? extends Block> brick;
    public final DeferredBlock<? extends Block> stairs;
    public final DeferredBlock<? extends Block> slab;
    @Nullable
    public final DeferredBlock<? extends Block> crackedbrick;
    @Nullable
    public final DeferredBlock<? extends Block> chiseledbrick;
    public RockRegisterHelper(
            DeferredBlock<? extends Block> bricks,
            DeferredBlock<? extends Block> slab,
            DeferredBlock<? extends Block> stairs,
            DeferredBlock<? extends Block> wall,
            @Nullable DeferredBlock<? extends Block> chiseled,
            @Nullable DeferredBlock<? extends Block> cracked
    ) {
        this.brick = bricks;
        this.slab = slab;
        this.stairs = stairs;
        this.crackedbrick = cracked;
        this.chiseledbrick = chiseled;
    }
    public static RockRegisterHelper RegisterBrickSet(
            String name,
            boolean registerChiseled,
            boolean registerCracked
    ) {

        // =========================================================
        // 磚
        // =========================================================

        DeferredBlock<Block> bricks =
                BlockRegisterHelper.RegisterNewBlock(
                        name,
                        () -> new Block(
                                BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.DEEPSLATE_TILES)
                        ),
                        true
                );


        // =========================================================
        // 半磚
        // =========================================================

        DeferredBlock<SlabBlock> slab =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_slab",
                        () -> new SlabBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.DEEPSLATE_TILE_SLAB)
                        ),
                        true
                );


        // =========================================================
        // 階梯
        // =========================================================

        DeferredBlock<StairBlock> stairs =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_stairs",
                        () -> new StairBlock(
                                bricks.get().defaultBlockState(),
                                BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.DEEPSLATE_TILE_STAIRS)
                        ),
                        true
                );


        // =========================================================
        // 牆
        // =========================================================

        DeferredBlock<WallBlock> wall =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_wall",
                        () -> new WallBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.DEEPSLATE_TILE_WALL)
                        ),
                        true
                );


        // =========================================================
        // 鑿製磚
        // =========================================================

        DeferredBlock<Block> chiseled = null;

        if (registerChiseled) {

            chiseled =
                    BlockRegisterHelper.RegisterNewBlock(
                            "chiseled_" + name,
                            () -> new Block(
                                    BlockBehaviour.Properties
                                            .ofFullCopy(Blocks.DEEPSLATE_TILES)
                            ),
                            true
                    );
        }


        // =========================================================
        // 裂紋磚
        // =========================================================

        DeferredBlock<Block> cracked = null;

        if (registerCracked) {

            cracked =
                    BlockRegisterHelper.RegisterNewBlock(
                            "cracked_" + name,
                            () -> new Block(
                                    BlockBehaviour.Properties
                                            .ofFullCopy(Blocks.DEEPSLATE_TILES)
                            ),
                            true
                    );
        }


        return new RockRegisterHelper(
                bricks,
                slab,
                stairs,
                wall,
                chiseled,
                cracked
        );
    }

}
