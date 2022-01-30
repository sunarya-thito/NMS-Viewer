package net.minecraft.server;

import javax.annotation.Nullable;

public class PathfinderGoalRandomStrollLand extends PathfinderGoalRandomStroll {

    private final float h;

    public PathfinderGoalRandomStrollLand(EntityCreature entitycreature, double d0) {
        this(entitycreature, d0, 0.001F);
    }

    public PathfinderGoalRandomStrollLand(EntityCreature entitycreature, double d0, float f) {
        super(entitycreature, d0);
        this.h = f;
    }

    @Nullable
    protected Vec3D f() {
        if (this.a.isInWater()) {
            Vec3D vec3d = RandomPositionGenerator.b(this.a, 15, 7);

            return vec3d == null ? super.f() : vec3d;
        } else {
            return this.a.getRandom().nextFloat() >= this.h ? RandomPositionGenerator.b(this.a, 10, 7) : super.f();
        }
    }
}
