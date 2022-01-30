package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.monster.IRangedEntity;

public class PathfinderGoalArrowAttack extends PathfinderGoal {

    private final EntityInsentient mob;
    private final IRangedEntity rangedAttackMob;
    private EntityLiving target;
    private int attackTime;
    private final double speedModifier;
    private int seeTime;
    private final int attackIntervalMin;
    private final int attackIntervalMax;
    private final float attackRadius;
    private final float attackRadiusSqr;

    public PathfinderGoalArrowAttack(IRangedEntity irangedentity, double d0, int i, float f) {
        this(irangedentity, d0, i, i, f);
    }

    public PathfinderGoalArrowAttack(IRangedEntity irangedentity, double d0, int i, int j, float f) {
        this.attackTime = -1;
        if (!(irangedentity instanceof EntityLiving)) {
            throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
        } else {
            this.rangedAttackMob = irangedentity;
            this.mob = (EntityInsentient) irangedentity;
            this.speedModifier = d0;
            this.attackIntervalMin = i;
            this.attackIntervalMax = j;
            this.attackRadius = f;
            this.attackRadiusSqr = f * f;
            this.a(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }
    }

    @Override
    public boolean a() {
        EntityLiving entityliving = this.mob.getGoalTarget();

        if (entityliving != null && entityliving.isAlive()) {
            this.target = entityliving;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean b() {
        return this.a() || !this.mob.getNavigation().m();
    }

    @Override
    public void d() {
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
    }

    @Override
    public void e() {
        double d0 = this.mob.h(this.target.locX(), this.target.locY(), this.target.locZ());
        boolean flag = this.mob.getEntitySenses().a(this.target);

        if (flag) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }

        if (d0 <= (double) this.attackRadiusSqr && this.seeTime >= 5) {
            this.mob.getNavigation().o();
        } else {
            this.mob.getNavigation().a((Entity) this.target, this.speedModifier);
        }

        this.mob.getControllerLook().a(this.target, 30.0F, 30.0F);
        if (--this.attackTime == 0) {
            if (!flag) {
                return;
            }

            float f = (float) Math.sqrt(d0) / this.attackRadius;
            float f1 = MathHelper.a(f, 0.1F, 1.0F);

            this.rangedAttackMob.a(this.target, f1);
            this.attackTime = MathHelper.d(f * (float) (this.attackIntervalMax - this.attackIntervalMin) + (float) this.attackIntervalMin);
        } else if (this.attackTime < 0) {
            this.attackTime = MathHelper.floor(MathHelper.d(Math.sqrt(d0) / (double) this.attackRadius, (double) this.attackIntervalMin, (double) this.attackIntervalMax));
        }

    }
}
