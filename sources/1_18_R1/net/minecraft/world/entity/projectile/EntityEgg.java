package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.EntityChicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;

// CraftBukkit start
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEggThrowEvent;
// CraftBukkit end

public class EntityEgg extends EntityProjectileThrowable {

    public EntityEgg(EntityTypes<? extends EntityEgg> entitytypes, World world) {
        super(entitytypes, world);
    }

    public EntityEgg(World world, EntityLiving entityliving) {
        super(EntityTypes.EGG, entityliving, world);
    }

    public EntityEgg(World world, double d0, double d1, double d2) {
        super(EntityTypes.EGG, d0, d1, d2, world);
    }

    @Override
    public void handleEntityEvent(byte b0) {
        if (b0 == 3) {
            double d0 = 0.08D;

            for (int i = 0; i < 8; ++i) {
                this.level.addParticle(new ParticleParamItem(Particles.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }

    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity movingobjectpositionentity) {
        super.onHitEntity(movingobjectpositionentity);
        movingobjectpositionentity.getEntity().hurt(DamageSource.thrown(this, this.getOwner()), 0.0F);
    }

    @Override
    protected void onHit(MovingObjectPosition movingobjectposition) {
        super.onHit(movingobjectposition);
        if (!this.level.isClientSide) {
            boolean hatching = this.random.nextInt(8) == 0; // CraftBukkit
            if (true) {
                byte b0 = 1;

                if (this.random.nextInt(32) == 0) {
                    b0 = 4;
                }

                // CraftBukkit start
                if (!hatching) {
                    b0 = 0;
                }
                EntityType hatchingType = EntityType.CHICKEN;

                Entity shooter = this.getOwner();
                if (shooter instanceof EntityPlayer) {
                    PlayerEggThrowEvent event = new PlayerEggThrowEvent((Player) shooter.getBukkitEntity(), (org.bukkit.entity.Egg) this.getBukkitEntity(), hatching, b0, hatchingType);
                    this.level.getCraftServer().getPluginManager().callEvent(event);

                    b0 = event.getNumHatches();
                    hatching = event.isHatching();
                    hatchingType = event.getHatchingType();
                }

                if (hatching) {
                    for (int i = 0; i < b0; ++i) {
                        Entity entity = level.getWorld().createEntity(new org.bukkit.Location(level.getWorld(), this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F), hatchingType.getEntityClass());
                        if (entity.getBukkitEntity() instanceof Ageable) {
                            ((Ageable) entity.getBukkitEntity()).setBaby();
                        }
                        level.getWorld().addEntity(entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.EGG);
                    }
                }
                // CraftBukkit end
            }

            this.level.broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }

    }

    @Override
    protected Item getDefaultItem() {
        return Items.EGG;
    }
}
