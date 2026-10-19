package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Blocks.EssenceAltarBlock;

public class AltarBlockHelper {
    public final DeferredBlock<? extends Block> dragon;
    public final DeferredBlock<? extends Block> fires;
    public final DeferredBlock<? extends Block> soul;
    AltarBlockHelper
            (
                    DeferredBlock<? extends Block> dragon,
                    DeferredBlock<? extends Block> fires,
                    DeferredBlock<? extends Block> soul
            )
    {
        this.dragon = dragon;
        this.fires = fires;
        this.soul = soul;
    }
    public static AltarBlockHelper AltarBlockSet(
            String name
    )
    {
        DeferredBlock<Block> dragon =
                BlockRegisterHelper.RegisterNewBlock(
                        "dragon_"+name+"_altar",
                        () -> new EssenceAltarBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.ENCHANTING_TABLE)
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        DeferredBlock<Block> fires =
                BlockRegisterHelper.RegisterNewBlock(
                        "fire_"+name+"_altar",
                        () -> new EssenceAltarBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.ENCHANTING_TABLE)
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        DeferredBlock<Block> soul =
                BlockRegisterHelper.RegisterNewBlock(
                        "soul_"+name+"_altar",
                        () -> new EssenceAltarBlock(
                                BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.ENCHANTING_TABLE)
                                        .requiresCorrectToolForDrops()
                        ),
                        true
                );
        return new AltarBlockHelper(
                dragon,
                fires,
                soul
        );
    }
}
