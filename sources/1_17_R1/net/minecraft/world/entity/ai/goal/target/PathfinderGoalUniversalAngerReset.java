package net.minecraft.world.entity.ai.goal.target;

import java.util.List;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AxisAlignedBB;

public class PathfinderGoalUniversalAngerReset<T extends EntityInsentient & IEntityAngerable> extends PathfinderGoal {

    private static final int ALERT_RANGE_Y = 10;
    private final T mob;
    private final boolean alertOthersOfSameType;
    private int lastHurtByPlayerTimestamp;

    public PathfinderGoalUniversalAngerReset(T t0, boolean flag) {
        this.mob = t0;
        this.alertOthersOfSameType = flag;
    }

    @Override
    public boolean a() {
        return this.mob.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.g();
    }

    private boolean g() {
        return this.mob.getLastDamager() != null && this.mob.getLastDamager().getEntityType() == EntityTypes.PLAYER && this.mob.dH() > this.lastHurtByPlayerTimestamp;
    }

    @Override
    public void c() {
        this.lastHurtByPlayerTimestamp = this.mob.dH();
        ((IEntityAngerable) this.mob).H_();
        if (this.alertOthersOfSameType) {
            this.h().stream().filter((entityinsentient) -> {
                return entityinsentient != this.mob;
            }).map((entityinsentient) -> {
                return (IEntityAngerable) entityinsentient;
            }).forEach(IEntityAngerable::H_);
        }

        super.c();
    }

    private List<? extends EntityInsentient> h() {
        double d0 = this.mob.b(GenericAttributes.FOLLOW_RANGE);
        AxisAlignedBB axisalignedbb = AxisAlignedBB.a(this.mob.getPositionVector()).grow(d0, 10.0D, d0);

        return this.mob.level.a(this.mob.getClass(), axisalignedbb, IEntitySelector.NO_SPECTATORS);
    }
}
