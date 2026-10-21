package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.starrepublic.beyond_the_end.Beyond_the_end;

import static org.starrepublic.beyond_the_end.Register.FluidRegister.FLUIDS;

public class Tags {
    public static final TagKey<Block> END_CRYSTAL_PLACEABLE = blockTag("end_crystal_placeable");
    public static final TagKey<Block> END_FIRE_BASE_BLOCKS = blockTag("end_fire_base_blocks");
    public static final TagKey<Block> CHORUS_PLANTABLE = blockTag("chorus_plantable");
    public static final TagKey<Block> ENDER_GOO_INFECTABLE = blockTag("ender_goo_infectable");
    public static final TagKey<Block> ENCHANTED_FEATURE_GROUND = blockTag("enchanted_feature_ground");

    public static final TagKey<Item> DRAGON_ESSENCE =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "dragon_essence"
                    )
            );
    public static final TagKey<Item> SOUL_ESSENCE =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "soul_essence"
                    )
            );

    public static final TagKey<Item> FIRE_ESSENCE =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "fire_essence"
                    )
            );
    public static final TagKey<Item> END_POWDER =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "altar_end_powder"
                    )
            );
    public static final TagKey<Item> ACTIVATOR_ITEMS =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "weak_end_portal_activators"
                    )
            );

    public static final TagKey<Fluid> ENDER_GOO = FLUIDS.createTagKey("ender_goo");
    public static final TagKey<Biome> CRYSTAL_REMOVER_BLACKLIST =
            TagKey.create(
                    Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "crystal_remover_blacklist"
                    )
            );
    public static final TagKey<Biome> IGNORE_MONSTER_SPAWN_LIGHT =
            TagKey.create(
                    Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "ignore_monster_spawn_light"
                    )
            );
    public static final TagKey<EntityType<?>> ENDER_GOO_IMMUNE =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "ender_goo_immune"
                    )
            );
    public static final TagKey<DimensionType> ENDER_GOO_PLACEABLE =
            TagKey.create(
                    Registries.DIMENSION_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "ender_goo_placeable"
                    )
            );
    private static TagKey<Block> blockTag(String name) {
        return blockTag(Beyond_the_end.MODID, name);
    }
    public static TagKey<Block> blockTag(String modid, String name) {
        return TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath(modid, name));
    }

}
