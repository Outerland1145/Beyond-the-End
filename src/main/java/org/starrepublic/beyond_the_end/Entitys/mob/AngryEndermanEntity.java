package org.starrepublic.beyond_the_end.Entitys.mob;

import net.minecraft.resources.ResourceKey;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

import org.starrepublic.beyond_the_end.Register.helper.EntityHelper;


public class AngryEndermanEntity extends EnderMan {

    // =========================================================
    // Entity Specification
    // =========================================================

    public static final EntityHelper.MobSpec<AngryEndermanEntity> SPEC =
            new EntityHelper.MobSpec<>(

                    AngryEndermanEntity::new,

                    MobCategory.MONSTER,

                    // 原版終界使者的尺寸
                    0.6F,
                    2.9F,

                    // 原版終界使者的屬性
                    AngryEndermanEntity::createAttributes,

                    // 原版敵對生物生成規則
                    SpawnPlacementTypes.ON_GROUND,

                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,

                    Monster::checkMonsterSpawnRules,

                    // 生成蛋顏色
                    0x151019,
                    0xB02349
            );


    // =========================================================
    // Constructor
    // =========================================================

    public AngryEndermanEntity(
            EntityType<? extends AngryEndermanEntity> type,
            Level level
    ) {
        super(type, level);
    }


    // =========================================================
    // Attributes
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {

        // 完全繼承原版屬性
        return EnderMan.createAttributes();
    }


    // =========================================================
    // AI
    // =========================================================

    @Override
    protected void registerGoals() {

        // 保留原版所有 AI
        super.registerGoals();

        /*
         * 新增最高優先級的玩家仇恨目標。
         *
         * 不需要玩家注視。
         * 不需要受到玩家攻擊。
         *
         * 只要符合目標搜尋條件，就會主動攻擊。
         */
        this.targetSelector.addGoal(
                0,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );
    }


    // =========================================================
    // Permanent Anger
    // =========================================================

    /**
     * 永遠處於憤怒外觀。
     *
     * EndermanRenderer 會使用此方法決定
     * 是否顯示原版憤怒姿態與抖動效果。
     */
    @Override
    public boolean isCreepy() {
        return true;
    }


    // =========================================================
    // Loot
    // =========================================================

    /**
     * 使用原版終界使者戰利品表。
     */
    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return EntityType.ENDERMAN.getDefaultLootTable();
    }
}
