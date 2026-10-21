package org.starrepublic.beyond_the_end.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.AABB;
import org.starrepublic.beyond_the_end.Entitys.entity.SafeEndCrystalEntity;
import org.starrepublic.beyond_the_end.Register.Tags;

public class SafeEndCrystalItem extends Item {

    public SafeEndCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();

        BlockPos basePos = context.getClickedPos();
        BlockPos spawnPos = basePos.above();

        Player player = context.getPlayer();

        BlockState baseState = level.getBlockState(basePos);

        // 只能放在特定TAG的方塊上
        if (!baseState.is(Tags.END_CRYSTAL_PLACEABLE)) {

            return InteractionResult.FAIL;
        }

        // 群系限制
        /*
        if (level.getBiome(spawnPos).is(Tags.CRYSTAL_REMOVER_BLACKLIST)) {
            return InteractionResult.FAIL;
        }
        */

        // 世界高度及邊界
        /*
        if (!level.isInWorldBounds(spawnPos)
                || !level.isInWorldBounds(spawnPos.above())
                || !level.getWorldBorder().isWithinBounds(spawnPos)) {

            return InteractionResult.FAIL;
        }
        */

        // 水晶上方必須有足夠的空間
        if (!level.isEmptyBlock(spawnPos)
                || !level.isEmptyBlock(spawnPos.above())) {

            return InteractionResult.FAIL;
        }

        // 防止與其他實體重疊生成
        if (!level.getEntities(
                (Entity) null,
                new AABB(
                        spawnPos.getX(),
                        spawnPos.getY(),
                        spawnPos.getZ(),
                        spawnPos.getX() + 1,
                        spawnPos.getY() + 2,
                        spawnPos.getZ() + 1
                )
        ).isEmpty()) {

            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {

            EntityType<?> type =
                    BuiltInRegistries.ENTITY_TYPE.get(
                            SafeEndCrystalEntity.ID
                    );

            Entity created = type.create(level);

            if (!(created instanceof SafeEndCrystalEntity crystal)) {
                return InteractionResult.FAIL;
            }

            crystal.setPos(
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5D
            );

            if (!level.addFreshEntity(crystal)) {
                return InteractionResult.FAIL;
            }

            // 生存模式消耗物品
            if (player == null
                    || !player.getAbilities().instabuild) {

                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide
        );
    }
}
