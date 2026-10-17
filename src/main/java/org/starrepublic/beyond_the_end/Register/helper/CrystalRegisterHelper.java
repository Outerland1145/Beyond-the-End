package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.starrepublic.beyond_the_end.Blocks.VoidCrystalBlock;
import org.starrepublic.beyond_the_end.Blocks.VoidCrystalBuddingBlock;

public class CrystalRegisterHelper {
    public final DeferredBlock<? extends Block> smallcrystalbud;
    public final DeferredBlock<? extends Block> mediumcrystalbud;
    public final DeferredBlock<? extends Block> largecrystalbud;
    public final DeferredBlock<? extends Block> crystalcluster;
    public final DeferredBlock<? extends Block> crystalblock;
    public final DeferredBlock<? extends Block> crystalbuddingblock;
    CrystalRegisterHelper
    (
        DeferredBlock<? extends Block> smallcrystalbud,
        DeferredBlock<? extends Block> mediumcrystalbud,
        DeferredBlock<? extends Block> largecrystalbud,
        DeferredBlock<? extends Block> crystalcluster,
        DeferredBlock<? extends Block> crystalblock,
        DeferredBlock<? extends Block> crystalbuddingblock
    )
    {
        this.smallcrystalbud = smallcrystalbud;
        this.mediumcrystalbud = mediumcrystalbud;
        this.largecrystalbud = largecrystalbud;
        this.crystalcluster = crystalcluster;
        this.crystalblock = crystalblock;
        this.crystalbuddingblock = crystalbuddingblock;
    }
    public static CrystalRegisterHelper CrystalRegisterSet(String name) {
        DeferredBlock<VoidCrystalBlock> smallcrystalbud =
                BlockRegisterHelper.RegisterNewBlock(
                        "small_"+name+"_bud",
                        () -> new VoidCrystalBlock(
                                7.0F,
                                3.0F,
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.AMETHYST_CLUSTER
                                        )
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        DeferredBlock<VoidCrystalBlock> mediumcrystalbud =
                BlockRegisterHelper.RegisterNewBlock(
                        "medium_"+name+"_bud",
                        () -> new VoidCrystalBlock(
                                7.0F,
                                3.0F,
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.AMETHYST_CLUSTER
                                        )
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        DeferredBlock<VoidCrystalBlock> largecrystalbud =
                BlockRegisterHelper.RegisterNewBlock(
                        "large_"+name+"_bud",
                        () -> new VoidCrystalBlock(
                                7.0F,
                                3.0F,
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.AMETHYST_CLUSTER
                                        )
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        DeferredBlock<VoidCrystalBlock> crystalcluster =
                BlockRegisterHelper.RegisterNewBlock(
                        name+"_cluster",
                        () -> new VoidCrystalBlock(
                                7.0F,
                                3.0F,
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.AMETHYST_CLUSTER
                                        )
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        DeferredBlock<Block> crystalblock =
                BlockRegisterHelper.RegisterNewBlock(
                        name+"_block",
                        () -> new Block(
                                BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.AMETHYST_BLOCK)
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        DeferredBlock<VoidCrystalBuddingBlock> crystalbuddingblock =
                BlockRegisterHelper.RegisterNewBlock(
                        name+"_budding_block",
                        () -> new VoidCrystalBuddingBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(
                                                Blocks.BUDDING_AMETHYST
                                        )
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        return new CrystalRegisterHelper(
                smallcrystalbud,
                mediumcrystalbud,
                largecrystalbud,
                crystalcluster,
                crystalblock,
                crystalbuddingblock
        );
    }
}
