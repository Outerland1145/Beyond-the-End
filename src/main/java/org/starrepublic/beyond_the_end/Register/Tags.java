package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.starrepublic.beyond_the_end.Beyond_the_end;

public class Tags {
    public static final TagKey<Block> END_CRYSTAL_PLACEABLE = blockTag("end_crystal_placeable");
    public static final TagKey<Block> END_PLANT_PLANTABLE = blockTag("end_plant_plantable");
    public static final TagKey<Block> CHORUS_PLANTABLE = blockTag("chorus_plantable");

    private static TagKey<Block> blockTag(String name) {
        return blockTag(Beyond_the_end.MODID, name);
    }
    public static TagKey<Block> blockTag(String modid, String name) {
        return TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath(modid, name));
    }
}
