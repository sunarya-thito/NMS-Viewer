package net.minecraft.world.effect;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;

public class MobEffectAbsorption extends MobEffectList {

    protected MobEffectAbsorption(MobEffectInfo mobeffectinfo, int i) {
        super(mobeffectinfo, i);
    }

    @Override
    public void a(EntityLiving entityliving, AttributeMapBase attributemapbase, int i) {
        entityliving.setAbsorptionHearts(entityliving.getAbsorptionHearts() - (float) (4 * (i + 1)));
        super.a(entityliving, attributemapbase, i);
    }

    @Override
    public void b(EntityLiving entityliving, AttributeMapBase attributemapbase, int i) {
        entityliving.setAbsorptionHearts(entityliving.getAbsorptionHearts() + (float) (4 * (i + 1)));
        super.b(entityliving, attributemapbase, i);
    }
}
