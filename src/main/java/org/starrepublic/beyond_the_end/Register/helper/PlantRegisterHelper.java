package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Blocks.EndPlantBlockShort;
import org.starrepublic.beyond_the_end.Blocks.EndPlantBlockShortWithoutTall;
import org.starrepublic.beyond_the_end.Blocks.EndPlantTall;

public class PlantRegisterHelper {
    public final DeferredBlock<? extends Block> shortgrass;
    @Nullable
    public final DeferredBlock<? extends Block> tallgrass;

    PlantRegisterHelper
            (
                    DeferredBlock<? extends Block> shortgrass,
                    @Nullable DeferredBlock<? extends Block> tallgrass
            )
    {
        this.shortgrass = shortgrass;
        this.tallgrass = tallgrass;
    }
    public static PlantRegisterHelper PlantRegisterWithTallSet(
            String name
    )
    {
        DeferredBlock<EndPlantTall> tallgrass = BlockRegisterHelper.RegisterNewBlock(
                "tall_" + name,
                () -> new EndPlantTall(name,BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)),
                true
        );
        DeferredBlock<? extends Block> shortgrass = BlockRegisterHelper.RegisterNewBlock(
                "short_" + name,
                () -> new EndPlantBlockShort(
                        name,
                        tallgrass.get(),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)
                ),
                true
        );

        return new PlantRegisterHelper(
                shortgrass,
                tallgrass
        );
    }
    public static PlantRegisterHelper PlantRegisterWithoutTallSet(
            String name
    )
    {
        DeferredBlock<? extends Block> grass = BlockRegisterHelper.RegisterNewBlock(
                name,
                () -> new EndPlantBlockShortWithoutTall(
                        name,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)
                ),
                true
        );

        return new PlantRegisterHelper(
                grass,
                null
        );
    }
}
