package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.starrepublic.beyond_the_end.Blocks.*;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class WoodRegisterHelper {
    public final DeferredBlock<? extends Block> log;
    public final DeferredBlock<? extends Block> wood;
    public final DeferredBlock<? extends Block> strippedlog;
    public final DeferredBlock<? extends Block> strippedwood;
    public final DeferredBlock<? extends Block> planks;
    public final DeferredBlock<? extends Block> stairs;
    public final DeferredBlock<? extends Block> slab;
    public final DeferredBlock<? extends Block> fence;
    public final DeferredBlock<? extends Block> fencegate;
    public final DeferredBlock<? extends Block> door;
    public final DeferredBlock<? extends Block> trapdoor;
    public final DeferredBlock<? extends Block> pressureplate;
    public final DeferredBlock<? extends Block> button;
    public final DeferredBlock<? extends Block> leaves;
    public final DeferredBlock<? extends Block> sapling;

    WoodRegisterHelper
    (
            DeferredBlock<? extends Block> log,
            DeferredBlock<? extends Block> wood,
            DeferredBlock<? extends Block> strippedlog,
            DeferredBlock<? extends Block> strippedwood,
            DeferredBlock<? extends Block> planks,
            DeferredBlock<? extends Block> stairs,
            DeferredBlock<? extends Block> slab,
            DeferredBlock<? extends Block> fence,
            DeferredBlock<? extends Block> fencegate,
            DeferredBlock<? extends Block> door,
            DeferredBlock<? extends Block> trapdoor,
            DeferredBlock<? extends Block> pressureplate,
            DeferredBlock<? extends Block> button,
            DeferredBlock<? extends Block> leaves,
            DeferredBlock<? extends Block> sapling
    )
    {
            this.log = log;
            this.wood = wood;
            this.strippedlog = strippedlog;
            this.strippedwood = strippedwood;
            this.planks = planks;
            this.stairs = stairs;
            this.slab = slab;
            this.fence = fence;
            this.fencegate = fencegate;
            this.door = door;
            this.trapdoor = trapdoor;
            this.pressureplate = pressureplate;
            this.button = button;
            this.leaves = leaves;
            this.sapling = sapling;
    }
    public static WoodRegisterHelper WoodRegisterSet(String name) {
        // =========================================================
        // Wood Type
        // =========================================================

        BlockSetType blockSetType =
                WoodTypes.CreateBlockSetType(name);

        WoodType woodType =
                WoodTypes.CreateWoodType(
                        name,
                        blockSetType
                );


        // =========================================================
        // Stripped Log
        // =========================================================

        DeferredBlock<RotatedPillarBlock> strippedlog =
                BlockRegisterHelper.RegisterNewBlock(
                        "stripped_" + name + "_log",

                        () -> new RotatedPillarBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.STRIPPED_WARPED_HYPHAE
                                        )
                        ),

                        true
                );


        // =========================================================
        // Stripped Wood
        // =========================================================

        DeferredBlock<RotatedPillarBlock> strippedwood =
                BlockRegisterHelper.RegisterNewBlock(
                        "stripped_" + name + "_wood",

                        () -> new RotatedPillarBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.STRIPPED_WARPED_HYPHAE
                                        )
                        ),

                        true
                );


        // =========================================================
        // Log
        // =========================================================

        DeferredBlock<WoodPillarBlock> log =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_log",

                        () -> new WoodPillarBlock(
                                strippedlog,
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.STRIPPED_WARPED_STEM
                                        )
                        ),

                        true
                );


        // =========================================================
        // Wood
        // =========================================================

        DeferredBlock<WoodPillarBlock> wood =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_wood",

                        () -> new WoodPillarBlock(
                                strippedwood,
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.STRIPPED_WARPED_STEM
                                        )
                        ),

                        true
                );


        // =========================================================
        // Planks
        // =========================================================

        DeferredBlock<Block> planks =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_planks",

                        () -> new Block(
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_PLANKS
                                        )
                        ),

                        true
                );


        // =========================================================
        // Stairs
        // =========================================================

        DeferredBlock<StairBlock> stairs =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_stairs",

                        () -> new StairBlock(
                                planks.get().defaultBlockState(),

                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_STAIRS
                                        )
                        ),

                        true
                );


        // =========================================================
        // Slab
        // =========================================================

        DeferredBlock<SlabBlock> slab =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_slab",

                        () -> new SlabBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_SLAB
                                        )
                        ),

                        true
                );


        // =========================================================
        // Fence
        // =========================================================

        DeferredBlock<FenceBlock> fence =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_fence",

                        () -> new FenceBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_FENCE
                                        )
                        ),

                        true
                );


        // =========================================================
        // Fence Gate
        // =========================================================

        DeferredBlock<WoodFenceGateBlock> fencegate =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_fence_gate",

                        () -> new WoodFenceGateBlock(
                                woodType,

                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_FENCE_GATE
                                        )
                        ),

                        true
                );


        // =========================================================
        // Door
        // =========================================================

        DeferredBlock<WoodDoorBlock> door =
                BlockRegisterHelper.RegisterNewBlockWithSpecialItem(
                        name + "_door",

                        () -> new WoodDoorBlock(
                                blockSetType,

                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_DOOR
                                        )
                        ),

                        block -> new DoubleHighBlockItem(
                                block,
                                new Item.Properties()
                        )
                );


        // =========================================================
        // Trapdoor
        // =========================================================

        DeferredBlock<WoodTrapDoorBlock> trapdoor =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_trapdoor",

                        () -> new WoodTrapDoorBlock(
                                blockSetType,

                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_TRAPDOOR
                                        )
                        ),

                        true
                );


        // =========================================================
        // Pressure Plate
        // =========================================================

        DeferredBlock<WoodPressurePlateBlock> pressureplate =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_pressure_plate",

                        () -> new WoodPressurePlateBlock(
                                blockSetType,

                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_PRESSURE_PLATE
                                        )
                        ),

                        true
                );


        // =========================================================
        // Button
        // =========================================================

        DeferredBlock<WoodButtonBlock> button =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_button",

                        () -> new WoodButtonBlock(
                                blockSetType,

                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.WARPED_BUTTON
                                        )
                        ),

                        true
                );


        // =========================================================
        // Leaves
        // =========================================================

        DeferredBlock<LeavesBlock> leaves =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_leaves",

                        () -> new LeavesBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.OAK_LEAVES
                                        )
                        ),

                        true
                );


        // =========================================================
        // Sapling
        // =========================================================

        DeferredBlock<WoodSaplingBlock> sapling =
                BlockRegisterHelper.RegisterNewBlock(
                        name + "_sapling",

                        () -> new WoodSaplingBlock(
                                name,

                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.OAK_SAPLING
                                        )
                        ),

                        true
                );


        // =========================================================
        // Result
        // =========================================================
        return new WoodRegisterHelper(
            log,
            wood,
            strippedlog,
            strippedwood,
            planks,
            stairs,
            slab,
            fence,
            fencegate,
            door,
            trapdoor,
            pressureplate,
            button,
            leaves,
            sapling
        );
    }
}
