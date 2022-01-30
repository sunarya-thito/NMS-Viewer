package net.minecraft.server;

import java.util.Random;
import javax.annotation.Nullable;

public class PathfinderGoalFleeSun extends PathfinderGoal {

    private final EntityCreature a;
    private double b;
    private double c;
    private double d;
    private final double e;
    private final World f;

    public PathfinderGoalFleeSun(EntityCreature entitycreature, double d0) {
        this.a = entitycreature;
        this.e = d0;
        this.f = entitycreature.world;
        this.a(1);
    }

    public boolean a() {
        if (!this.f.L()) {
            return false;
        } else if (!this.a.isBurning()) {
            return false;
        } else if (!this.f.e(new BlockPosition(this.a.locX, this.a.getBoundingBox().minY, this.a.locZ))) {
            return false;
        } else if (!this.a.getEquipment(EnumItemSlot.HEAD).isEmpty()) {
            return false;
        } else {
            Vec3D vec3d = this.g();

            if (vec3d == null) {
                return false;
            } else {
                this.b = vec3d.x;
                this.c = vec3d.y;
                this.d = vec3d.z;
                return true;
            }
        }
    }

    public boolean b() {
        return !this.a.getNavigation().p();
    }

    public void c() {
        this.a.getNavigation().a(this.b, this.c, this.d, this.e);
    }

    @Nullable
    private Vec3D g() {
        Random random = this.a.getRandom();
        BlockPosition blockposition = new BlockPosition(this.a.locX, this.a.getBoundingBox().minY, this.a.locZ);

        for (int i = 0; i < 10; ++i) {
            BlockPosition blockposition1 = blockposition.a(random.nextInt(20) - 10, random.nextInt(6) - 3, random.nextInt(20) - 10);

            if (!this.f.e(blockposition1) && this.a.a(blockposition1) < 0.0F) {
                return new Vec3D((double) blockposition1.getX(), (double) blockposition1.getY(), (double) blockposition1.getZ());
            }
        }

        return null;
    }
}
