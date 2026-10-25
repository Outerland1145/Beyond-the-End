package org.starrepublic.beyond_the_end.Mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 將原版終界龍的垂直飛行控制還原至 Minecraft 1.12.2。
 *
 * 現代版本：
 *     verticalAcceleration = verticalError * 0.01D
 *
 * 1.12.2：
 *     verticalAcceleration = verticalError * 0.1D
 *
 * 不修改任何 Dragon Phase，不覆寫 aiStep()。
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonLegacyMovementMixin {

    /**
     * 只修改 aiStep() 中負責垂直加速度的 0.01D。
     *
     * 使用 MixinExtras 而非 @ModifyConstant，
     * 讓修改可以與其他 MixinExtras 注入串接。
     */
    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "CONSTANT",
                    args = "doubleValue=0.01"
            ),
            require = 1
    )
    private double beyondTheEnd$restoreLegacyVerticalAcceleration(
            double original
    ) {
        // 若其他模組已經修改此數值，保留其結果。
        if (Double.compare(original, 0.01D) != 0) {
            return original;
        }

        // Minecraft 1.12.2 的垂直修正係數。
        return 0.1D;
    }
}

