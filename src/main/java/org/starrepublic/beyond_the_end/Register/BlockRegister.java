package org.starrepublic.beyond_the_end.Register;

import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Blocks.*;

public class BlockRegister {
    private BlockRegister() {}
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Beyond_the_end.MODID);

    public static final DeferredBlock<DecorativeEndGatewayBlock>
            DECORATIVE_END_GATEWAY =
            BLOCKS.registerBlock(
                    "decorative_end_gateway",
                    DecorativeEndGatewayBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(3.0F, 1200.0F)
                            .sound(SoundType.GLASS)
                            .lightLevel(state -> 15)
            );
    public static final DeferredBlock<BarrierNodeBlock>
            BarrierNodeBlock =
            BLOCKS.registerBlock(
                    "barrier_node_block",
                    BarrierNodeBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(3.0F, 1200.0F)
                            .sound(SoundType.GLASS)
                            .lightLevel(state -> 15)
            );
    public static final DeferredBlock<Block> AltarStone =
            BLOCKS.registerSimpleBlock(
                    "altar_stone",
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(-1.0f, 5000.0F)
                            .requiresCorrectToolForDrops()
            );
    public static final DeferredBlock<EndTallGrassBlock> END_Tall_Grass_Block =
            BLOCKS.registerBlock(
                    "end_tall_grass",
                    EndTallGrassBlock::new,
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.TALL_GRASS)
                            .mapColor(MapColor.COLOR_PURPLE)
                            .noCollission()
                            .noOcclusion()
            );
    public static final DeferredBlock<EndShortGrassBlock> END_Short_Grass_Block =
            BLOCKS.registerBlock(
                    "end_short_grass",
                    EndShortGrassBlock::new,
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.SHORT_GRASS)
                            .mapColor(MapColor.COLOR_PURPLE)
                            .noCollission()
                            .noOcclusion()
            );

    public static final DeferredBlock<VoidBlock> Void_Block =
            BLOCKS.register(
                    "void_block",
                    () -> new VoidBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(3.0F, 9.0F)
                                    .sound(SoundType.DEEPSLATE)

                                    .noCollission()
                                    .noOcclusion()

                                    .isSuffocating(
                                            (state, level, pos) -> false
                                    )

                                    .isViewBlocking(
                                            (state, level, pos) -> false
                                    )

                                    .isRedstoneConductor(
                                            (state, level, pos) -> false
                                    )
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static final DeferredBlock<ColoredFallingBlock> END_SAND =
            BLOCKS.register(
                    "end_sand",
                    () -> new ColoredFallingBlock(
                            new ColorRGBA(0xD8D5A4),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.SAND)
                                    .strength(0.5F)
                                    .sound(SoundType.SAND)
                    )
            );
    public static final DeferredBlock<VoidCrystalBlock> END_CRYSTAL =
            BLOCKS.register(
                    "void_cluster",
                    () -> new VoidCrystalBlock(
                            7.0F,
                            3.0F,
                            BlockBehaviour.Properties
                                    .ofFullCopy(
                                            Blocks.AMETHYST_CLUSTER
                                    )
                                    .requiresCorrectToolForDrops()
                    )
            );
    public static final DeferredBlock<EndPointedDripstoneBlock> END_DRIPSTONE =
            BLOCKS.register(
                    "end_pointed_dripstone",
                    () -> new EndPointedDripstoneBlock(
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.POINTED_DRIPSTONE)
                                    .requiresCorrectToolForDrops()
                    )
            );

    public static final DeferredBlock<EndAnchorBlock> END_ANCHOR_BLOCK =
            BLOCKS.registerBlock(
                    "end_anchor",
                    EndAnchorBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(10.0f, 1200.0F)
                            .requiresCorrectToolForDrops()
            );
    public static final DeferredBlock<EndNetherPortalBlock> END_NETHER_PORTAL =
            BLOCKS.registerBlock(
                    "end_nether_portal",
                    properties -> new EndNetherPortalBlock(
                            properties
                                    .noCollission()
                                    .noOcclusion()
                                    .strength(-1.0F)
                                    .lightLevel(state -> 11)
                                    .noLootTable()
                    )
            );
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
