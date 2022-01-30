package net.minecraft.world.effect;

import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.AttributeMapBase;

public class MobEffectHealthBoost extends MobEffectList {

    public MobEffectHealthBoost(MobEffectInfo mobeffectinfo, int i) {
        super(mobeffectinfo, i);
    }

    @Override
    public void a(EntityLiving entityliving, AttributeMapBase attributemapbase, int i) {
        super.a(entityliving, attributemapbase, i);
        if (entityliving.getHealth() > entityliving.getMaxHealth()) {
            entityliving.setHealth(entityliving.getMaxHealth());
        }

    }
}
