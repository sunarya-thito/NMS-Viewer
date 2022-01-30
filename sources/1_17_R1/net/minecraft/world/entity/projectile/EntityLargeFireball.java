package net.minecraft.world.entity.projectile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;

import org.bukkit.event.entity.ExplosionPrimeEvent; // CraftBukkit

public class EntityLargeFireball extends EntityFireballFireball {

    public int explosionPower = 1;

    public EntityLargeFireball(EntityTypes<? extends EntityLargeFireball> entitytypes, World world) {
        super(entitytypes, world);
        isIncendiary = this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING); // CraftBukkit
    }

    public EntityLargeFireball(World world, EntityLiving entityliving, double d0, double d1, double d2, int i) {
        super(EntityTypes.FIREBALL, entityliving, d0, d1, d2, world);
        this.explosionPower = i;
        isIncendiary = this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING); // CraftBukkit
    }

    @Override
    protected void a(MovingObjectPosition movingobjectposition) {
        super.a(movingobjectposition);
        if (!this.level.isClientSide) {
            boolean flag = this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);

            // CraftBukkit start - fire ExplosionPrimeEvent
            ExplosionPrimeEvent event = new ExplosionPrimeEvent((org.bukkit.entity.Explosive) this.getBukkitEntity());
            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                // give 'this' instead of (Entity) null so we know what causes the damage
                this.level.createExplosion(this, this.locX(), this.locY(), this.locZ(), event.getRadius(), event.getFire(), flag ? Explosion.Effect.DESTROY : Explosion.Effect.NONE);
            }
            // CraftBukkit end
            this.die();
        }

    }

    @Override
    protected void a(MovingObjectPositionEntity movingobjectpositionentity) {
        super.a(movingobjectpositionentity);
        if (!this.level.isClientSide) {
            Entity entity = movingobjectpositionentity.getEntity();
            Entity entity1 = this.getShooter();

            entity.damageEntity(DamageSource.fireball(this, entity1), 6.0F);
            if (entity1 instanceof EntityLiving) {
                this.a((EntityLiving) entity1, entity);
            }

        }
    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        super.saveData(nbttagcompound);
        nbttagcompound.setByte("ExplosionPower", (byte) this.explosionPower);
    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        super.loadData(nbttagcompound);
        if (nbttagcompound.hasKeyOfType("ExplosionPower", 99)) {
            // CraftBukkit - set bukkitYield when setting explosionpower
            bukkitYield = this.explosionPower = nbttagcompound.getByte("ExplosionPower");
        }

    }
}
