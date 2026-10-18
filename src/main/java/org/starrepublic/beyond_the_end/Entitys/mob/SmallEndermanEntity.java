package org.starrepublic.beyond_the_end.Entitys.mob;


import net.minecraft.resources.ResourceKey;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import org.starrepublic.beyond_the_end.Register.helper.EntityHelper;


public class SmallEndermanEntity extends EnderMan {

    // =========================================================
    // Entity Settings
    // =========================================================

    /**
     * 小終界使者縮放比例。
     *
     * 透過 Attributes.SCALE 生效，
     * 不需要在 Renderer 中再次縮放。
     */
    public static final double SCALE = 0.6D;


    /**
     * 該實體所有註冊需要的設定。
     *
     * AI、屬性等細節由本類自行決定，
     * EntityRegister 不需要知道。
     */
    public static final EntityHelper.MobSpec<SmallEndermanEntity> SPEC =
            new EntityHelper.MobSpec<>(

                    SmallEndermanEntity::new,

                    MobCategory.MONSTER,

                    // 基礎碰撞箱，實際會受 SCALE 影響。
                    0.6F,
                    2.9F,

                    // Attributes
                    SmallEndermanEntity::createAttributes,

                    // Spawn Placement
                    SpawnPlacementTypes.ON_GROUND,

                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,

                    Monster::checkMonsterSpawnRules,

                    // Spawn Egg Colors
                    0x17121F,
                    0xAF65E5
            );


    // =========================================================
    // Constructor
    // =========================================================

    public SmallEndermanEntity(
            EntityType<? extends SmallEndermanEntity> type,
            Level level
    ) {

        super(type, level);
    }


    // =========================================================
    // Attributes
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {

        return EnderMan.createAttributes()

                .add(
                        Attributes.SCALE,
                        SCALE
                );
    }


    // =========================================================
    // AI
    // =========================================================

    /**
     * 不覆寫 registerGoals()。
     *
     * 直接繼承 EnderMan：
     *
     * - 玩家注視判定
     * - 攻擊與追擊
     * - 瞬間移動
     * - 搬運方塊
     * - 隨機移動
     * - 仇恨管理
     *
     * 原版完整 AI。
     */


    // =========================================================
    // Loot
    // =========================================================

    /**
     * 沿用原版終界使者戰利品表。
     */
    @Override
    public ResourceKey<LootTable> getDefaultLootTable() {

        return EntityType.ENDERMAN.getDefaultLootTable();
    }
}
