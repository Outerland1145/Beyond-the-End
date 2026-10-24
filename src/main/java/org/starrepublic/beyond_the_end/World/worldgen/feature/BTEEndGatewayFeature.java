package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
public class BTEEndGatewayFeature
        extends Feature<EndGatewayConfiguration> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BTEEndGatewayFeature.class);

    public BTEEndGatewayFeature() {
        super(EndGatewayConfiguration.CODEC);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<EndGatewayConfiguration> context
    ) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        EndGatewayConfiguration config = context.config();

        ResourceLocation location =
                ResourceLocation.fromNamespaceAndPath(
                        "beyond_the_end",
                        "endgateway/end_gateway"
                );

        StructureTemplateManager manager =
                level.getServer().getStructureManager();

        Optional<StructureTemplate> optional =
                manager.get(location);

        if (optional.isEmpty()) {
            LOGGER.error(
                    "Cannot place End Gateway: missing structure {}",
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
                || sizeY % 2 == 0
                || sizeZ % 2 == 0
                || sizeX != sizeZ) {

            LOGGER.error(
                    "Invalid End Gateway size: X={}, Y={}, Z={}",
                    sizeX, sizeY, sizeZ
            );
            return false;
        }

        // 暫時保留原始 Mixin 的位移公式
        BlockPos start = origin.offset(
                sizeX / 2,
                sizeY / 2,
                sizeZ / 2
        );

        StructurePlaceSettings settings =
                new StructurePlaceSettings()
                        .setMirror(Mirror.NONE)
                        .setRotation(Rotation.NONE)
                        .setIgnoreEntities(false);

        if (!template.placeInWorld(
                level,
                start,
                start,
                settings,
                context.random(),
                2
        )) {
            return false;
        }

        // 為模板內所有折躍門方塊設定出口
        for (BlockPos pos : BlockPos.betweenClosed(
                start,
                start.offset(
                        sizeX - 1,
                        sizeY - 1,
                        sizeZ - 1
                )
        )) {
            if (!level.getBlockState(pos).is(Blocks.END_GATEWAY)) {
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity instanceof TheEndGatewayBlockEntity gateway) {

                config.getExit().ifPresent(exit -> {

                    gateway.setExitPosition(
                            exit,
                            config.isExitExact()
                    );

                    gateway.setChanged();
                });
            }
        }

        return true;
    }
}


