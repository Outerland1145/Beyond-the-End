package org.starrepublic.beyond_the_end.Register;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Blocks.BarrierNodeBlock;
import org.starrepublic.beyond_the_end.Blocks.DecorativeEndGatewayBlock;

public class BlockRegister {
    private BlockRegister() {}
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Beyond_the_end.MODID);

    public static final DeferredBlock<DecorativeEndGatewayBlock>
            DECORATIVE_END_GATEWAY =
            BLOCKS.registerBlock(
                    "decorative_end_gateway",
                    DecorativeEndGatewayBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(3.0F, 1200.0F)
                            .sound(SoundType.GLASS)
                            .lightLevel(state -> 15)
            );
    public static final DeferredBlock<BarrierNodeBlock>
            BarrierNodeBlock =
            BLOCKS.registerBlock(
                    "barrier_node_block",
                    BarrierNodeBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(3.0F, 1200.0F)
                            .sound(SoundType.GLASS)
                            .lightLevel(state -> 15)
            );
    public static final DeferredBlock<Block> AltarStone =
            BLOCKS.registerSimpleBlock(
                    "altar_stone",
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(-1.0f, 5000.0F)
                            .requiresCorrectToolForDrops()
            );
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
