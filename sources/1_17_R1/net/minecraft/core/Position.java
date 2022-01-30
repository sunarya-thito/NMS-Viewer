package net.minecraft.core;

public class Position implements IPosition {

    protected final double x;
    protected final double y;
    protected final double z;

    public Position(double d0, double d1, double d2) {
        this.x = d0;
        this.y = d1;
        this.z = d2;
    }

    @Override
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public double getZ() {
        return this.z;
    }
}
