package org.starrepublic.beyond_the_end.World.worldgen.structure.jigsaw;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import org.starrepublic.beyond_the_end.Register.JigsawRegister;

import java.util.Optional;

/**
 * A normal single-pool element with one extra logical component id.
 *
 * location  = actual NBT template
 * component = logical counter used by BTE requirements
 */
public final class BTEComponentPoolElement extends SinglePoolElement {

    public static final MapCodec<BTEComponentPoolElement> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(

                    ResourceLocation.CODEC
                            .fieldOf("location")
                            .forGetter(BTEComponentPoolElement::location),

                    StructureProcessorType.LIST_CODEC
                            .fieldOf("processors")
                            .forGetter(element -> element.processors),

                    projectionCodec(),

                    ResourceLocation.CODEC
                            .fieldOf("component")
                            .forGetter(BTEComponentPoolElement::component)

            ).apply(instance, BTEComponentPoolElement::new));

    private final ResourceLocation location;
    private final ResourceLocation component;

    public BTEComponentPoolElement(
            ResourceLocation location,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection,
            ResourceLocation component
    ) {
        super(
                Either.left(location),
                processors,
                projection,
                Optional.empty()
        );

        this.location = location;
        this.component = component;
    }

    public ResourceLocation location() {
        return location;
    }

    public ResourceLocation component() {
        return component;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return JigsawRegister.COMPONENT_POOL_ELEMENT.get();
    }

    @Override
    public String toString() {
        return "BTEComponentPoolElement[location="
                + location
                + ", component="
                + component
                + "]";
    }
}
