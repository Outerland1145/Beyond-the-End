package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.EndGatewayFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(EndGatewayFeature.class)
public abstract class End_Gateway_Mixin {
    int sizeX;
    int sizeZ;
    int sizeY;
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void injected(FeaturePlaceContext<EndGatewayConfiguration> p_159715_, CallbackInfoReturnable<Boolean> cir) {
        BlockPos blockpos = p_159715_.origin();
        WorldGenLevel worldgenlevel = p_159715_.level();
        EndGatewayConfiguration endgatewayconfiguration = p_159715_.config();
        StructureTemplateManager structuremanager = worldgenlevel.getServer().getStructureManager();
        ResourceLocation gatewaylocation = ResourceLocation.fromNamespaceAndPath("beyond_the_end", "endgateway/end_gateway");

        Optional<StructureTemplate> optional;
        try {
            optional = structuremanager.get(gatewaylocation);
            if (optional.isPresent()) {
                StructureTemplate template = optional.get();
                sizeX = template.getSize().getX();
                sizeZ = template.getSize().getZ();
                sizeY = template.getSize().getY();
                if (sizeX % 2 == 0 || sizeZ % 2 == 0 || sizeY%2 == 0 || sizeX != sizeZ) {

                    System.err.println(
                            "[Beyond The End] Can't Place Gateway: " +
                                    "End Gateway structure X/Y/Z size must be odd and X/Z size must equal ! " +
                                    "Current size: X=" + sizeX +
                                    ", Z=" + sizeZ +
                                    ", Y=" + sizeY
                    );

                    return;
                }


                blockpos = blockpos.offset(sizeX/ 2, sizeY/2, sizeZ / 2);
                StructurePlaceSettings structureplacesettings = (new StructurePlaceSettings()).setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false);
                optional.get().placeInWorld(worldgenlevel, blockpos, blockpos, structureplacesettings, worldgenlevel.getRandom(), 2);


                for (BlockPos blockpos1 : BlockPos.betweenClosed(
                        blockpos,
                        blockpos.offset(sizeX - 1, sizeY - 1, sizeZ - 1)
                )) {
                    if (worldgenlevel.getBlockState(blockpos1).is(Blocks.END_GATEWAY)) {
                        endgatewayconfiguration.getExit().ifPresent((p_65699_) -> {
                            BlockEntity blockentity = worldgenlevel.getBlockEntity(blockpos1);
                            if (blockentity instanceof TheEndGatewayBlockEntity) {
                                TheEndGatewayBlockEntity theendgatewayblockentity = (TheEndGatewayBlockEntity) blockentity;
                                theendgatewayblockentity.setExitPosition(p_65699_, endgatewayconfiguration.isExitExact());
                                blockentity.setChanged();
                            }

                        });
                    }


                }

                cir.setReturnValue(true);
                return;

            }
        } catch (ResourceLocationException resourcelocationexception) {
            System.out.println("Can't place Gateway");
        }

    }
}
