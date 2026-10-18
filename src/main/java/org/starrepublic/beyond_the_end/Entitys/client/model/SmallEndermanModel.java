package org.starrepublic.beyond_the_end.Entitys.client.model;

import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.EnderMan;
import org.starrepublic.beyond_the_end.Entitys.mob.SmallEndermanEntity;

/**
 * 重用原版終界使者模型和所有原版動畫。
 * EndermanRenderer 會在 render() 中依照 getCarriedBlock() 設定 carrying，
 * 所以必須在 setupAnim() 裡、原版 EndermanModel 讀取 carrying 之前，
 * 將偷來的物品也視為「正在搬運」。
 */
public class SmallEndermanModel extends EndermanModel<EnderMan> {

    public SmallEndermanModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(
            EnderMan entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (entity instanceof SmallEndermanEntity smallEnderman
                && !smallEnderman.getDisplayHeldItem().isEmpty()) {
            // 這會啟用原版 EndermanModel 的雙手搬運姿勢。
            this.carrying = true;
        }

        super.setupAnim(entity, limbSwing, limbSwingAmount,
                ageInTicks, netHeadYaw, headPitch);
    }
}
