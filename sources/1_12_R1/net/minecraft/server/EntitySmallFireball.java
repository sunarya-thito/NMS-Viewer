package net.minecraft.server;

import org.bukkit.event.entity.EntityCombustByEntityEvent; // CraftBukkit

public class EntitySmallFireball extends EntityFireball {

    public EntitySmallFireball(World world) {
        super(world);
        this.setSize(0.3125F, 0.3125F);
    }

    public EntitySmallFireball(World world, EntityLiving entityliving, double d0, double d1, double d2) {
        super(world, entityliving, d0, d1, d2);
        this.setSize(0.3125F, 0.3125F);
        // CraftBukkit start
        if (this.shooter != null && this.shooter instanceof EntityInsentient) {
            isIncendiary = this.world.getGameRules().getBoolean("mobGriefing");
        }
        // CraftBukkit end
    }

    public EntitySmallFireball(World world, double d0, double d1, double d2, double d3, double d4, double d5) {
        super(world, d0, d1, d2, d3, d4, d5);
        this.setSize(0.3125F, 0.3125F);
    }

    public static void a(DataConverterManager dataconvertermanager) {
        EntityFireball.a(dataconvertermanager, "SmallFireball");
    }

    protected void a(MovingObjectPosition movingobjectposition) {
        if (!this.world.isClientSide) {
            boolean flag;

            if (movingobjectposition.entity != null) {
                if (!movingobjectposition.entity.isFireProof()) {
                    // CraftBukkit start - Entity damage by entity event + combust event
                    isIncendiary = movingobjectposition.entity.damageEntity(DamageSource.fireball(this, this.shooter), 5.0F);
                    if (isIncendiary) {
                        this.a(this.shooter, movingobjectposition.entity);
                        EntityCombustByEntityEvent event = new EntityCombustByEntityEvent((org.bukkit.entity.Projectile) this.getBukkitEntity(), movingobjectposition.entity.getBukkitEntity(), 5);
                        movingobjectposition.entity.world.getServer().getPluginManager().callEvent(event);

                        if (!event.isCancelled()) {
                            movingobjectposition.entity.setOnFire(event.getDuration());
                        }
                        // CraftBukkit end
                    }
                }
            } else {
                flag = true;
                if (this.shooter != null && this.shooter instanceof EntityInsentient) {
                    flag = this.world.getGameRules().getBoolean("mobGriefing");
                }

                // CraftBukkit start
                if (isIncendiary) {
                    BlockPosition blockposition = movingobjectposition.a().shift(movingobjectposition.direction);

                    if (this.world.isEmpty(blockposition)) {
                        if (!org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(world, blockposition.getX(), blockposition.getY(), blockposition.getZ(), this).isCancelled()) {
                            this.world.setTypeUpdate(blockposition, Blocks.FIRE.getBlockData());
                        }
                        // CraftBukkit end
                    }
                }
            }

            this.die();
        }

    }

    public boolean isInteractable() {
        return false;
    }

    public boolean damageEntity(DamageSource damagesource, float f) {
        return false;
    }
}
