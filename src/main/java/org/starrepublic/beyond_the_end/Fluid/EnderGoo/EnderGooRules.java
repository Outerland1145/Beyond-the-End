package org.starrepublic.beyond_the_end.Fluid.EnderGoo;

import net.minecraft.world.level.Level;
import org.starrepublic.beyond_the_end.Register.Tags;

public final class EnderGooRules {

    private EnderGooRules() {
    }

    public static boolean canExist(Level level) {
        return level.dimensionTypeRegistration()
                .is(Tags.ENDER_GOO_PLACEABLE);
    }
}
