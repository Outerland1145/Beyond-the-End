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
    public static PlantRegisterHelper PlantRegisterSet(
            String name,
            boolean needtallgrass
    )
    {
        DeferredBlock<? extends Block> shortgrass = null;
        DeferredBlock<EndPlantTall> tallgrass;
        if (needtallgrass) {

            tallgrass = BlockRegisterHelper.RegisterNewBlock(
                "tall_" + name + "grass",
                () -> new EndPlantTall(name,BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)),
                true
            );
            shortgrass = BlockRegisterHelper.RegisterNewBlock(
                    "short_" + name + "grass",
                    () -> new EndPlantBlockShort(
                            name,
                            tallgrass.get(),
                            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    ),
                    true
            );
        }
        else
        {
            tallgrass = null;
            shortgrass = BlockRegisterHelper.RegisterNewBlock(
                    "short_" + name + "grass",
                    () -> new EndPlantBlockShortWithoutTall(
                            name,
                            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    ),
                    true
            );
        }
        return new PlantRegisterHelper(
                shortgrass,
                tallgrass
        );
    }
}
