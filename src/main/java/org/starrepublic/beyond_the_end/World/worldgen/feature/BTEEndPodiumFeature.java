package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class BTEEndPodiumFeature
        extends Feature<NoneFeatureConfiguration> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BTEEndPodiumFeature.class);

    private final boolean active;

    public BTEEndPodiumFeature(boolean active) {
        super(NoneFeatureConfiguration.CODEC);
        this.active = active;
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        ResourceLocation location =
                ResourceLocation.fromNamespaceAndPath(
                        "beyond_the_end",
                        active
                                ? "endpodium/end_portal_active"
                                : "endpodium/end_portal_inactive"
                );

        StructureTemplateManager manager =
                level.getServer().getStructureManager();

        Optional<StructureTemplate> optional =
                manager.get(location);

        if (optional.isEmpty()) {
            LOGGER.error(
                    "Cannot place End Podium: missing structure {}",
                    location
            );
            return false;
        }

        StructureTemplate template = optional.get();

        int sizeX = template.getSize().getX();
        int sizeY = template.getSize().getY();
        int sizeZ = template.getSize().getZ();

        // 保留原本的結構尺寸限制
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0
                || sizeX % 2 == 0
                || sizeZ % 2 == 0
                || sizeX != sizeZ) {

            LOGGER.error(
                    "Invalid End Podium size: X={}, Y={}, Z={}",
                    sizeX, sizeY, sizeZ
            );
            return false;
        }

        // 祭壇中心對齊，結構底部向下偏移一格
        BlockPos start = origin.offset(
                -sizeX / 2,
                -1,
                -sizeZ / 2
        );

        StructurePlaceSettings settings =
                new StructurePlaceSettings()
                        .setMirror(Mirror.NONE)
                        .setRotation(Rotation.NONE)
                        .setIgnoreEntities(false);

        return template.placeInWorld(
                level,
                start,
                start,
                settings,
                context.random(),
                2
        );
    }
}


