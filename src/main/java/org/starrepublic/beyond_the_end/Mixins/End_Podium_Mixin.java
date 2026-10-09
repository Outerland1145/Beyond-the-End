package org.starrepublic.beyond_the_end.Mixins;

import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mixin(EndPodiumFeature.class)
public abstract class End_Podium_Mixin {
    @Shadow
    @Final
    private boolean active;
    int sizeX;
    int sizeZ;
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void injected(FeaturePlaceContext<NoneFeatureConfiguration> p_159723_, CallbackInfoReturnable<Boolean> cir) {
        BlockPos blockpos = p_159723_.origin();
        WorldGenLevel worldgenlevel = p_159723_.level();
        StructureTemplateManager structuremanager = worldgenlevel.getServer().getStructureManager();
        ResourceLocation podiumlocation = ResourceLocation.fromNamespaceAndPath("beyond_the_end", active ? "endpodium/end_portal_active" : "endpodium/end_portal_inactive");

        Optional<StructureTemplate> optional;
        try {
            optional = structuremanager.get(podiumlocation);
            blockpos = blockpos.offset(-optional.get().getSize().getX() / 2, -1, -optional.get().getSize().getZ() / 2);
            if (optional.isPresent()) {
                StructureTemplate template = optional.get();
                sizeX = template.getSize().getX();
                sizeZ = template.getSize().getZ();
                if (sizeX % 2 == 0 || sizeZ % 2 == 0 || sizeX != sizeZ) {

                    System.err.println(
                            "[Beyond The End] Can't Place Podium: " +
                                    "End Portal structure must have odd and equal X/Z size! " +
                                    "Current size: X=" + sizeX +
                                    ", Z=" + sizeZ
                    );

                    return;
                }
                StructurePlaceSettings structureplacesettings = (new StructurePlaceSettings()).setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(false);
                optional.get().placeInWorld(worldgenlevel, blockpos, blockpos, structureplacesettings, worldgenlevel.getRandom(), 2);
                System.out.println("End Podium has been place");
                cir.setReturnValue(true);

            }

        } catch (ResourceLocationException resourcelocationexception) {
            System.out.println("Can't place portal");
        }

    }
}
