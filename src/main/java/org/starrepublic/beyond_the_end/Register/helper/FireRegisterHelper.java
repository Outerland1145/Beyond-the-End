package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.starrepublic.beyond_the_end.Blocks.EndCampfireBlock;
import org.starrepublic.beyond_the_end.Blocks.EndFireBlock;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class FireRegisterHelper {
    public final DeferredBlock<? extends Block> fireblock;
    public final DeferredBlock<? extends Block> lantern;
    public final DeferredBlock<? extends Block> campfire;
    public final DeferredBlock<? extends Block> torch;
    public final DeferredBlock<? extends Block> walltorch;
    public final DeferredItem<? extends Item> torchitem;
    FireRegisterHelper
    (
            DeferredBlock<? extends Block> fireblock,
            DeferredBlock<? extends Block> lantern,
            DeferredBlock<? extends Block> campfire,
            DeferredBlock<? extends Block> torch,
            DeferredBlock<? extends Block> walltorch,
            DeferredItem<? extends Item> torchitem
    )
    {
        this.fireblock = fireblock;
        this.lantern = lantern;
        this.campfire = campfire;
        this.torch = torch;
        this.walltorch = walltorch;
        this.torchitem = torchitem;
    }
    public static FireRegisterHelper FireRegisterSet(String name) {
        DeferredBlock<EndFireBlock> fireblock =
                BlockRegisterHelper.RegisterNewBlock(
                        name+"_fire",
                        () -> new EndFireBlock(
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_PURPLE)
                                        .replaceable()
                                        .noCollission()
                                        .instabreak()
                                        .lightLevel(state -> 15)
                                        .sound(SoundType.WOOL)
                                        .pushReaction(PushReaction.DESTROY)
                        ),
                        false
                );
        DeferredBlock<LanternBlock> lantern =
                BlockRegisterHelper.RegisterNewBlock(
                        name+"_lantern",
                        () -> new LanternBlock(
                                BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .forceSolidOn()
                                        .requiresCorrectToolForDrops()
                                        .strength(3.5F)
                                        .sound(SoundType.LANTERN)
                                        .lightLevel(p_187433_ -> 15)
                                        .noOcclusion()
                                        .pushReaction(PushReaction.DESTROY)
                        ),
                        true
                );
        DeferredBlock<EndCampfireBlock> campfire =
                BlockRegisterHelper.RegisterNewBlock(
                        name+"_campfire",
                        () -> new EndCampfireBlock(
                                true,
                                1,
                                BlockBehaviour.Properties.ofFullCopy(Blocks.CAMPFIRE)
                        ),
                        true
                );
        DeferredBlock<Block> torch =
                BlockRegisterHelper.RegisterNewBlock(
                        name+"_torch",
                        () -> new TorchBlock(
                                ParticleTypes.DRAGON_BREATH,
                                BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH)
                        ),
                        false
                );

        DeferredBlock<Block> walltorch =
                BlockRegisterHelper.RegisterNewBlock(
                        name+"_wall_torch",
                        () -> new WallTorchBlock(
                                ParticleTypes.DRAGON_BREATH,
                                BlockBehaviour.Properties.ofFullCopy(Blocks.WALL_TORCH)
                        ),
                        false
                );
        DeferredItem<StandingAndWallBlockItem> torchitem =
                BlockRegister.ITEMS.register(
                        name+"_torch",
                        () -> new StandingAndWallBlockItem(
                                torch.get(),
                                walltorch.get(),
                                new Item.Properties(),
                                Direction.DOWN
                        )
                );
        return new FireRegisterHelper(
                fireblock,
                lantern,
                campfire,
                torch,
                walltorch,
                torchitem
        );
    }
}
