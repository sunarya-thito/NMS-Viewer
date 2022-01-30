package net.minecraft.world.phys;

import net.minecraft.util.MathHelper;

public class Vec2F {

    public static final Vec2F ZERO = new Vec2F(0.0F, 0.0F);
    public static final Vec2F ONE = new Vec2F(1.0F, 1.0F);
    public static final Vec2F UNIT_X = new Vec2F(1.0F, 0.0F);
    public static final Vec2F NEG_UNIT_X = new Vec2F(-1.0F, 0.0F);
    public static final Vec2F UNIT_Y = new Vec2F(0.0F, 1.0F);
    public static final Vec2F NEG_UNIT_Y = new Vec2F(0.0F, -1.0F);
    public static final Vec2F MAX = new Vec2F(Float.MAX_VALUE, Float.MAX_VALUE);
    public static final Vec2F MIN = new Vec2F(Float.MIN_VALUE, Float.MIN_VALUE);
    public final float x;
    public final float y;

    public Vec2F(float f, float f1) {
        this.x = f;
        this.y = f1;
    }

    public Vec2F a(float f) {
        return new Vec2F(this.x * f, this.y * f);
    }

    public float a(Vec2F vec2f) {
        return this.x * vec2f.x + this.y * vec2f.y;
    }

    public Vec2F b(Vec2F vec2f) {
        return new Vec2F(this.x + vec2f.x, this.y + vec2f.y);
    }

    public Vec2F b(float f) {
        return new Vec2F(this.x + f, this.y + f);
    }

    public boolean c(Vec2F vec2f) {
        return this.x == vec2f.x && this.y == vec2f.y;
    }

    public Vec2F a() {
        float f = MathHelper.c(this.x * this.x + this.y * this.y);

        return f < 1.0E-4F ? Vec2F.ZERO : new Vec2F(this.x / f, this.y / f);
    }

    public float b() {
        return MathHelper.c(this.x * this.x + this.y * this.y);
    }

    public float c() {
        return this.x * this.x + this.y * this.y;
    }

    public float d(Vec2F vec2f) {
        float f = vec2f.x - this.x;
        float f1 = vec2f.y - this.y;

        return f * f + f1 * f1;
    }

    public Vec2F d() {
        return new Vec2F(-this.x, -this.y);
    }
}
