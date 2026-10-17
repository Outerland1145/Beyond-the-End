package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.Tags;

public class EndPlantBlockShort extends TallGrassBlock {
    private final TagKey<Block> plantableOn;
    private final Block block;
    public EndPlantBlockShort(
            String name,
            Block block,
            Properties properties
    ) {
        super(properties);
        this.plantableOn = createPlantableTag(name);
        this.block = block;
    }

    @Override
    protected boolean mayPlaceOn(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return state.is(plantableOn);
    }

    /*
     * 明確保證玩家、生物可以穿過。
     */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    /*
     * 不遮擋旁邊 / 下方方塊的面。
     */
    @Override
    protected VoxelShape getOcclusionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return Shapes.empty();
    }

    @Override
    public boolean isValidBonemealTarget(
            LevelReader level,
            BlockPos pos,
            BlockState state
    ) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(
            net.minecraft.world.level.Level level,
            RandomSource random,
            BlockPos pos,
            BlockState state
    ) {
        return true;
    }

    @Override
    public void performBonemeal(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state
    ) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }

        DoublePlantBlock.placeAt(
                level,
                block.defaultBlockState(),
                pos,
                Block.UPDATE_ALL
        );
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
                        "plant/can_" + name + "_plantable"
                )
        );
    }
}
