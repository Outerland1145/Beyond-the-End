package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.World.worldgen.structure.jigsaw.BTEComponentPoolElement;
import org.starrepublic.beyond_the_end.World.worldgen.structure.jigsaw.BTEDeferredJigsawPiece;
import org.starrepublic.beyond_the_end.World.worldgen.structure.jigsaw.BTEJigsawStructure;
import org.starrepublic.beyond_the_end.World.worldgen.structure.volcano.LargeVolcanoPiece;
import org.starrepublic.beyond_the_end.World.worldgen.structure.volcano.LargeVolcanoStructure;

public class JigsawRegister {
    private JigsawRegister() {}

    public static final DeferredRegister<StructureType<?>>
            STRUCTURE_TYPES =
            DeferredRegister.create(
                    BuiltInRegistries.STRUCTURE_TYPE,
                    Beyond_the_end.MODID
            );

    public static final DeferredRegister<
            StructurePoolElementType<?>
            >
            POOL_ELEMENT_TYPES =
            DeferredRegister.create(

                    BuiltInRegistries
                            .STRUCTURE_POOL_ELEMENT,

                    Beyond_the_end.MODID
            );


    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(
                    BuiltInRegistries.STRUCTURE_PIECE,
                    Beyond_the_end.MODID
            );

    public static final DeferredHolder<
            StructureType<?>,
            StructureType<BTEJigsawStructure>
            >
            CONTROLLED_JIGSAW =
            STRUCTURE_TYPES.register(

                    "controlled_jigsaw",

                    () ->
                            () ->
                                    BTEJigsawStructure.CODEC
            );

    public static final DeferredHolder<
            StructurePoolElementType<?>,
            StructurePoolElementType<BTEComponentPoolElement>
            >
            COMPONENT_POOL_ELEMENT =
            POOL_ELEMENT_TYPES.register(

                    "component_single_pool_element",

                    () ->
                            () ->
                                    BTEComponentPoolElement.CODEC
            );

    public static final DeferredHolder<StructurePieceType, StructurePieceType> DEFERRED_JIGSAW_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "deferred_jigsaw_piece",
                    () -> BTEDeferredJigsawPiece::new
            );

    public static final DeferredHolder<
            StructureType<?>,
            StructureType<LargeVolcanoStructure>
            >
            LARGE_VOLCANO =
            STRUCTURE_TYPES.register(
                    "large_volcano",
                    () -> () -> LargeVolcanoStructure.CODEC
            );
    public static final DeferredHolder<
            StructurePieceType,
            StructurePieceType.ContextlessType
            >
            LARGE_VOLCANO_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "large_volcano_piece",
                    () -> LargeVolcanoPiece::new
            );
    public static void register(
            IEventBus modBus
    ) {

        STRUCTURE_TYPES.register(modBus);
        POOL_ELEMENT_TYPES.register(modBus);
        STRUCTURE_PIECE_TYPES.register(modBus);
    }
}
