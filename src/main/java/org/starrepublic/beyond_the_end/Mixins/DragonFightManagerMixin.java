package org.starrepublic.beyond_the_end.Mixins;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.dimension.end.DragonRespawnAnimation;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

import javax.annotation.Nullable;
import java.util.*;

@Mixin(EndDragonFight.class)
public class DragonFightManagerMixin {
    @Shadow
    private boolean dragonKilled;

    @Shadow
    @Nullable
    private DragonRespawnAnimation respawnStage;

    @Shadow
    @Nullable
    private BlockPos portalLocation;

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    private BlockPattern.BlockPatternMatch findExitPortal() {return null;}

    @Shadow
    private void spawnExitPortal(boolean unused) {}

    @Shadow
    private void respawnDragon(List<EndCrystal> crystals) {}

    @Inject(method = "tryRespawn", at = @At("HEAD"), cancellable = true)
    public void TryRespawnInject(CallbackInfo ci) {
        ci.cancel();

        if (this.dragonKilled && this.respawnStage == null) {

            BlockPos blockpos = this.portalLocation;
            if (blockpos == null) {

                BlockPattern.BlockPatternMatch match =
                        this.findExitPortal();

                /*
                 * 找不到則重新生成。
                 */
                if (match == null) {
                    this.spawnExitPortal(true);
                }

                blockpos = this.portalLocation;
            }
            if (blockpos == null) {

                System.err.println(
                        "[Beyond The End] Can't respawn dragon: " +
                                "Exit Portal location is null!"
                );

                return;
            }
            StructureTemplateManager structuremanager =
                    this.level.getStructureManager();

            ResourceLocation portallocation =
                    ResourceLocation.fromNamespaceAndPath(
                            "beyond_the_end",
                            "endpodium/end_portal_active"
                    );

            Optional<StructureTemplate> optional =
                    structuremanager.get(portallocation);

            if (optional.isEmpty()) {

                System.err.println(
                        "[Beyond The End] Can't respawn dragon: " +
                                "Can't find End Portal structure!"
                );

                return;
            }
            StructureTemplate template = optional.get();

            int sizeX = template.getSize().getX();
            int sizeY = template.getSize().getY();
            int sizeZ = template.getSize().getZ();
            BlockPos structureStart =
                    blockpos.offset(
                            -sizeX / 2,
                            -1,
                            -sizeZ / 2
                    );
            List<BlockPos> altarStones =
                    Lists.newArrayList();
            for (BlockPos pos : BlockPos.betweenClosed(
                    structureStart,
                    structureStart.offset(
                            sizeX - 1,
                            sizeY - 1,
                            sizeZ - 1
                    )
            )) {

                if (this.level
                        .getBlockState(pos)
                        .is(BlockRegister.AltarStone.get())) {

                    /*
                     * immutable() 很重要：
                     *
                     * betweenClosed 迭代時可能重複使用 MutableBlockPos。
                     */
                    altarStones.add(pos.immutable());
                }
            }
            if (altarStones.isEmpty()) {

                System.err.println(
                        "[Beyond The End] Can't respawn dragon: " +
                                "No Altar Stone found!"
                );

                return;
            }
            Set<EndCrystal> usedCrystals =
                    new HashSet<>();
            for (BlockPos altarPos : altarStones) {

                BlockPos crystalPos =
                        altarPos.above();

                List<EndCrystal> crystals =
                        this.level.getEntitiesOfClass(
                                EndCrystal.class,
                                new AABB(crystalPos)
                        );


                /*
                 * 額外篩選：
                 *
                 * 水晶的實際 BlockPos 必須就是
                 * 祭壇石正上方。
                 *
                 * 這可以防止 End Crystal 較大的碰撞箱
                 * 同時與相鄰 AABB 相交。
                 */
                EndCrystal validCrystal = null;

                for (EndCrystal crystal : crystals) {

                    if (crystal.blockPosition()
                            .equals(crystalPos)
                            && !usedCrystals.contains(crystal)) {

                        validCrystal = crystal;
                        break;
                    }
                }


                /*
                 * 任意一塊祭壇石沒有水晶：
                 * 儀式失敗。
                 */
                if (validCrystal == null) {
                    return;
                }


                usedCrystals.add(validCrystal);
            }
            if (usedCrystals.size()
                    != altarStones.size()) {

                return;
            }


            /*
             * 全部條件滿足。
             */
            this.respawnDragon(
                    new ArrayList<>(usedCrystals)
            );
        }
    }
}
