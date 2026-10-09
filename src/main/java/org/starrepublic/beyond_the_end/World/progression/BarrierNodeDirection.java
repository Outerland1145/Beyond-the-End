package org.starrepublic.beyond_the_end.World.progression;
import net.minecraft.util.StringRepresentable;

public enum BarrierNodeDirection implements StringRepresentable {

    NORTH("north"),
    SOUTH("south"),
    EAST("east"),
    WEST("west");

    private final String name;

    BarrierNodeDirection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
