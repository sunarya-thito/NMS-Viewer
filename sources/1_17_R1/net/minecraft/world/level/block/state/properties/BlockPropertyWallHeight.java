package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyWallHeight implements INamable {

    NONE("none"), LOW("low"), TALL("tall");

    private final String name;

    private BlockPropertyWallHeight(String s) {
        this.name = s;
    }

    public String toString() {
        return this.getName();
    }

    @Override
    public String getName() {
        return this.name;
    }
}
