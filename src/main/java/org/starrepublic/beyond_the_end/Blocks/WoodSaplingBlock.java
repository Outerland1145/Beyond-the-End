package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.starrepublic.beyond_the_end.Beyond_the_end;

import java.util.Optional;

public class WoodSaplingBlock extends SaplingBlock {
    private final TagKey<Block> plantableOn;
    /**
     * 一般木材：
     *
     * name = "xxx"
     *
     * 自动使用：
     *
     * beyond_the_end:xxx_tree
     */
    public WoodSaplingBlock(
            String name,
            Properties properties
    ) {
        super(
                createDefaultTreeGrower(name),
                properties
        );
        this.plantableOn = createPlantableTag(name);
    }


    private static TreeGrower createDefaultTreeGrower(
            String name
    ) {

        ResourceKey<ConfiguredFeature<?, ?>> tree =
                ResourceKey.create(
                        Registries.CONFIGURED_FEATURE,
                        ResourceLocation.fromNamespaceAndPath(
                                Beyond_the_end.MODID,
                                name + "_tree"
                        )
                );

        return new TreeGrower(
                Beyond_the_end.MODID + ":" + name,

                // Mega tree
                Optional.empty(),

                // Normal tree
                Optional.of(tree),

                // Flower variant
                Optional.empty()
        );
    }

    @Override
    protected boolean mayPlaceOn(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return state.is(plantableOn);
    }

    public TagKey<Block> getPlantableOnTag() {
        return plantableOn;
    }

    private static TagKey<Block> createPlantableTag(
            String name
    ) {
        return TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(
                        Beyond_the_end.MODID,
                        "can_" + name + "_plantable"
                )
        );
    }
}
