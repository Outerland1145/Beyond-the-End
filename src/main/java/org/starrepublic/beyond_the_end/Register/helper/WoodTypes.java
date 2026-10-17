package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public final class WoodTypes {

    private WoodTypes() {}


    public static BlockSetType CreateBlockSetType(
            String name
    ) {

        return BlockSetType.register(
                new BlockSetType(
                        Beyond_the_end.MODID + ":" + name
                )
        );
    }


    public static WoodType CreateWoodType(
            String name,
            BlockSetType blockSetType
    ) {

        return WoodType.register(
                new WoodType(
                        Beyond_the_end.MODID + ":" + name,
                        blockSetType
                )
        );
    }
}
