package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyTrackPosition implements INamable {

    NORTH_SOUTH("north_south"), EAST_WEST("east_west"), ASCENDING_EAST("ascending_east"), ASCENDING_WEST("ascending_west"), ASCENDING_NORTH("ascending_north"), ASCENDING_SOUTH("ascending_south"), SOUTH_EAST("south_east"), SOUTH_WEST("south_west"), NORTH_WEST("north_west"), NORTH_EAST("north_east");

    private final String name;

    private BlockPropertyTrackPosition(String s) {
        this.name = s;
    }

    public String a() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

    public boolean b() {
        return this == BlockPropertyTrackPosition.ASCENDING_NORTH || this == BlockPropertyTrackPosition.ASCENDING_EAST || this == BlockPropertyTrackPosition.ASCENDING_SOUTH || this == BlockPropertyTrackPosition.ASCENDING_WEST;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
