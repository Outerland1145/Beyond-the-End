package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Blocks.*;
import org.starrepublic.beyond_the_end.Blocks.Blockitems.DecorativeEndGatewayItem;
import org.starrepublic.beyond_the_end.Fluid.EnderGoo.EnderGooFluidBlock;
import org.starrepublic.beyond_the_end.Register.helper.BlockRegisterHelper;
import org.starrepublic.beyond_the_end.Register.helper.CrystalRegisterHelper;
import org.starrepublic.beyond_the_end.Register.helper.FireRegisterHelper;
import org.starrepublic.beyond_the_end.Register.helper.WoodRegisterHelper;
import org.starrepublic.beyond_the_end.World.worldgen.feature.ConfiguredFeatureKeys;

public class BlockRegister {
    private BlockRegister() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Beyond_the_end.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Beyond_the_end.MODID);

    public static final CrystalRegisterHelper VOID_CRYSTAL = CrystalRegisterHelper.CrystalRegisterSet("void_crystal");
    public static final FireRegisterHelper END_FIRE = FireRegisterHelper.FireRegisterSet("end");
    public static final WoodRegisterHelper SPOOKY = WoodRegisterHelper.WoodRegisterSet("spooky");


    public static final DeferredBlock<DecorativeEndGatewayBlock> DECORATIVE_END_GATEWAY =
            BlockRegisterHelper.RegisterNewBlockWithSpecialItem(
                    "decorative_end_gateway",
                    () -> new DecorativeEndGatewayBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(10.0F, 20.0F)
                                    .sound(SoundType.GLASS)
                                    .lightLevel(state -> 15)
                                    .requiresCorrectToolForDrops()
                    ),
                    block -> new DecorativeEndGatewayItem(
                            block,
                            new Item.Properties()
                    )
            );


    public static final DeferredBlock<BarrierNodeBlock> BarrierNodeBlock =
            BlockRegisterHelper.RegisterNewBlock(
                    "barrier_node_block",
                    () -> new BarrierNodeBlock(
                            BlockBehaviour.Properties.of()
                            .strength(3.0F, 1200.0F)
                            .sound(SoundType.GLASS)
                            .lightLevel(state -> 15)
                    ),
                    true
            );


    public static final DeferredBlock<Block> END_LARTERN_BLOCK =
            BlockRegisterHelper.RegisterNewBlock(
                    "end_lantern_block",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .strength(10.0F, 10.0F)
                                    .sound(SoundType.GLASS)
                                    .lightLevel(state -> 15)
                                    .requiresCorrectToolForDrops()
                    ),
                    true
            );
    public static final DeferredBlock<EndSurfaceCoverBlock> END_STONE_ENCHANTED =
            BlockRegisterHelper.RegisterNewBlock(
                    "enchanted_end_stone",
                    () -> new EndSurfaceCoverBlock(

                            BlockBehaviour.Properties
                                    .ofFullCopy(
                                            Blocks.END_STONE
                                    )
                                    .randomTicks(),
                            () -> Blocks.END_STONE,
                            ConfiguredFeatureKeys.ENCHANTED_LANDS_BONEMEAL
                    ),
                    true
            );
    public static final DeferredBlock<Block> END_STONE_INFESTED =
            BlockRegisterHelper.RegisterNewBlock(
                    "infested_end_stone",
                    () -> new Block(

                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BROWN)
                                    .strength(3.0f, 9.0F)
                                    .requiresCorrectToolForDrops()
                    ),
                    true
            );

    public static final DeferredBlock<Block> END_STONE_BURNED =
            BlockRegisterHelper.RegisterNewBlock(
                    "burned_end_stone",
                    () -> new Block(

                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_YELLOW)
                                    .strength(3.0f, 9.0F)
                                    .requiresCorrectToolForDrops()
                    ),
                    true
            );
    public static final DeferredBlock<Block> AltarStone =
            BlockRegisterHelper.RegisterNewBlock(
                    "altar_stone",
                    () -> new Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .strength(-1.0f, 5000.0F)
                    ),
                    true
            );
    public static final DeferredBlock<EndTallGrassBlock> END_Tall_Grass_Block =
            BlockRegisterHelper.RegisterNewBlock(
                    "end_tall_grass",
                    () -> new EndTallGrassBlock(
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.TALL_GRASS)
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .noCollission()
                                    .noOcclusion()
                    ),
                    true
            );
    public static final DeferredBlock<EndShortGrassBlock> END_Short_Grass_Block =
            BlockRegisterHelper.RegisterNewBlock(
                    "end_short_grass",
                    () -> new EndShortGrassBlock(
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.SHORT_GRASS)
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .noCollission()
                                    .noOcclusion()
                    ),
                    true
            );
    public static final DeferredBlock<EnchantedPlants> Enchanted_Short_Grass_Block =
            BlockRegisterHelper.RegisterNewBlock(
                    "enchanted_short_grass",
                    () -> new EnchantedPlants(
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.SHORT_GRASS)
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .noCollission()
                                    .noOcclusion()
                    ),
                    true
            );
    public static final DeferredBlock<EnchantedPlants> Enchanted_Tall_Grass_Block =
            BlockRegisterHelper.RegisterNewBlock(
                    "enchanted_tall_grass",
                    () -> new EnchantedPlants(
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.SHORT_GRASS)
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .noCollission()
                                    .noOcclusion()
                    ),
                    true
            );
    public static final DeferredBlock<EnchantedFlowerBlock> MAGIC_FLOWER =
            BlockRegisterHelper.RegisterNewBlock(
                    "enchanted_flower",
                    () -> new EnchantedFlowerBlock(
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.POPPY)
                                    .lightLevel(
                                            state -> 10
                                    )
                    ),
                    true
            );
    public static final DeferredBlock<FlowerPotBlock> POTTED_MAGIC_FLOWER =
            BlockRegisterHelper.RegisterNewBlock(
                    "potted_magic_flower",
                    () -> new FlowerPotBlock(
                            () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                            MAGIC_FLOWER,
                            BlockBehaviour.Properties
                                    .ofFullCopy(
                                            Blocks.POTTED_POPPY
                                    )
                                    .lightLevel(
                                            state -> 10
                                    )
                    ),
                    false
            );

    public static final DeferredBlock<VoidBlock> Void_Block =
            BlockRegisterHelper.RegisterNewBlock(
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
                    ),
                    true
            );

    public static final DeferredBlock<ColoredFallingBlock> END_SAND =
            BlockRegisterHelper.RegisterNewBlock(
                    "end_sand",
                    () -> new ColoredFallingBlock(
                            new ColorRGBA(0xD8D5A4),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.SAND)
                                    .strength(0.5F)
                                    .sound(SoundType.SAND)
                    ),
                    true
            );

    public static final DeferredBlock<EndPointedDripstoneBlock> END_DRIPSTONE =
            BlockRegisterHelper.RegisterNewBlock(
                    "end_pointed_dripstone",
                    () -> new EndPointedDripstoneBlock(
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.POINTED_DRIPSTONE)
                                    .requiresCorrectToolForDrops()
                    ),
                    true
            );

    public static final DeferredBlock<EndAnchorBlock> END_ANCHOR_BLOCK =
            BlockRegisterHelper.RegisterNewBlock(
                    "end_anchor",
                    () -> new EndAnchorBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .strength(10.0f, 1200.0F)
                                    .requiresCorrectToolForDrops()
                    ),
                    true
            );
    public static final DeferredBlock<EndNetherPortalBlock> END_NETHER_PORTAL =
            BlockRegisterHelper.RegisterNewBlock(
                    "end_nether_portal",
                    () -> new EndNetherPortalBlock(
                            BlockBehaviour.Properties.of()
                                    .noCollission()
                                    .noOcclusion()
                                    .strength(-1.0F)
                                    .lightLevel(state -> 11)
                                    .noLootTable()
                    ),
                    false
            );
    public static final DeferredBlock<EnderGooFluidBlock> ENDER_GOO =
            BlockRegisterHelper.RegisterNewBlock(
                    "ender_goo",
                    () -> new EnderGooFluidBlock(
                            FluidRegister.ENDER_GOO.get(),
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.WATER)
                                    .lightLevel(state -> 12)
                    ),
                    false
            );
    public static final DeferredBlock<EnderGooCauldronBlock> ENDER_GOO_CAULDRON =
            BlockRegisterHelper.RegisterNewBlock(
                    "ender_goo_cauldron",
                    () -> new EnderGooCauldronBlock(
                            BlockBehaviour.Properties
                                    .ofFullCopy(Blocks.CAULDRON)
                                    .lightLevel(state -> 12)
                                    .requiresCorrectToolForDrops()
                    ),
                    false
            );
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
