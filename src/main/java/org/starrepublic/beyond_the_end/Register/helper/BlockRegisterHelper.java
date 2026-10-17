package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.registries.DeferredBlock;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class BlockRegisterHelper {
    private BlockRegisterHelper() {}
    //public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Beyond_the_end.MODID);
    //public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Beyond_the_end.MODID);

    public static <T extends Block> DeferredBlock<T> RegisterNewBlock(
            String name,
            Supplier<T> blockSupplier,
            boolean needblockitem
    ) {
        if (needblockitem)
        {
            return registerblockwithblockitem(
                    name,
                    blockSupplier,
                    block -> new BlockItem(block, new Item.Properties())
            );
        }
        else
        {
            return registeronlyBlock(
                    name,
                    blockSupplier
            );
        }
    }
    public static <T extends Block> DeferredBlock<T> RegisterNewBlockWithSpecialItem(
            String name,
            Supplier<T> blockSupplier,
            Function<T, ? extends BlockItem> itemFactory
    ) {
        DeferredBlock<T> block =
                BlockRegister.BLOCKS.register(
                        name,
                        blockSupplier
                );

        BlockRegister.ITEMS.register(
                name,
                () -> itemFactory.apply(block.get())
        );

        return block;
    }
    private static <T extends Block> DeferredBlock<T> registeronlyBlock(
            String name,
            Supplier<T> blockSupplier
    ) {
        DeferredBlock<T> block = BlockRegister.BLOCKS.register(name, blockSupplier);
        return block;
    }
    public static <T extends Block, I extends BlockItem> DeferredBlock<T> registerblockwithblockitem(
            String name,
            Supplier<T> blockSupplier,
            Function<T, I> itemFactory
    ) {
        DeferredBlock<T> block = BlockRegister.BLOCKS.register(name, blockSupplier);

        BlockRegister.ITEMS.register(
                name,
                () -> itemFactory.apply(block.get())
        );

        return block;
    }
}
