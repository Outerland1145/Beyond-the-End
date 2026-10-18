package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Register.Tags;

public class EnchantedFlowerBlock extends FlowerBlock {
    private final TagKey<Block> plantableOn;
    public static final MapCodec<EnchantedFlowerBlock> CODEC =
            simpleCodec(EnchantedFlowerBlock::new);


    public EnchantedFlowerBlock(
            BlockBehaviour.Properties properties
    ) {

        /*
         * FlowerBlock 需要一個可疑燉菜效果。
         *
         * 這裡先使用 Night Vision 5 秒，
         * 不影響花本身平時的任何行為。
         */
        super(
                MobEffects.NIGHT_VISION,
                5.0F,
                properties
        );
        this.plantableOn = createPlantableTag();
    }


    @Override
    public MapCodec<? extends FlowerBlock> codec() {
        return CODEC;
    }


    /*
     * 除了原版花允許的 Dirt 類方塊，
     * 魔力花額外允許附魔之地地表方塊。
     */
    @Override
    protected boolean mayPlaceOn(
            BlockState floor,
            BlockGetter level,
            BlockPos pos
    ) {

        return floor.is(
                plantableOn
        ) || super.mayPlaceOn(
                floor,
                level,
                pos
        );
    }
    public TagKey<Block> getPlantableOnTag() {
        return plantableOn;
    }

    private static TagKey<Block> createPlantableTag()
    {
        return TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(
                        Beyond_the_end.MODID,
                        "plant/can_enchanted_flower_plantable"
                )
        );
    }
}
