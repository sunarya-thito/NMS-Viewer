package net.minecraft.world.entity.projectile;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

import org.bukkit.craftbukkit.event.CraftEventFactory; // CraftBukkit

public class EntityFireworks extends IProjectile implements ItemSupplier {

    public static final DataWatcherObject<ItemStack> DATA_ID_FIREWORKS_ITEM = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.ITEM_STACK);
    private static final DataWatcherObject<OptionalInt> DATA_ATTACHED_TO_TARGET = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.OPTIONAL_UNSIGNED_INT);
    public static final DataWatcherObject<Boolean> DATA_SHOT_AT_ANGLE = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.BOOLEAN);
    private int life;
    public int lifetime;
    @Nullable
    private EntityLiving attachedToEntity;

    public EntityFireworks(EntityTypes<? extends EntityFireworks> entitytypes, World world) {
        super(entitytypes, world);
    }

    public EntityFireworks(World world, double d0, double d1, double d2, ItemStack itemstack) {
        super(EntityTypes.FIREWORK_ROCKET, world);
        this.life = 0;
        this.setPos(d0, d1, d2);
        int i = 1;

        if (!itemstack.isEmpty() && itemstack.hasTag()) {
            this.entityData.set(EntityFireworks.DATA_ID_FIREWORKS_ITEM, itemstack.copy());
            i += itemstack.getOrCreateTagElement("Fireworks").getByte("Flight");
        }

        this.setDeltaMovement(this.random.nextGaussian() * 0.001D, 0.05D, this.random.nextGaussian() * 0.001D);
        this.lifetime = 10 * i + this.random.nextInt(6) + this.random.nextInt(7);
    }

    public EntityFireworks(World world, @Nullable Entity entity, double d0, double d1, double d2, ItemStack itemstack) {
        this(world, d0, d1, d2, itemstack);
        this.setOwner(entity);
    }

    public EntityFireworks(World world, ItemStack itemstack, EntityLiving entityliving) {
        this(world, entityliving, entityliving.getX(), entityliving.getY(), entityliving.getZ(), itemstack);
        this.entityData.set(EntityFireworks.DATA_ATTACHED_TO_TARGET, OptionalInt.of(entityliving.getId()));
        this.attachedToEntity = entityliving;
    }

    public EntityFireworks(World world, ItemStack itemstack, double d0, double d1, double d2, boolean flag) {
        this(world, d0, d1, d2, itemstack);
        this.entityData.set(EntityFireworks.DATA_SHOT_AT_ANGLE, flag);
    }

    public EntityFireworks(World world, ItemStack itemstack, Entity entity, double d0, double d1, double d2, boolean flag) {
        this(world, itemstack, d0, d1, d2, flag);
        this.setOwner(entity);
    }

    // Spigot Start - copied from tick
    @Override
    public void inactiveTick() {
        this.life += 1;

        if (!this.level.isClientSide && this.life > this.lifetime) {
            // CraftBukkit start
            if (!org.bukkit.craftbukkit.event.CraftEventFactory.callFireworkExplodeEvent(this).isCancelled()) {
                this.explode();
            }
            // CraftBukkit end
        }
        super.inactiveTick();
    }
    // Spigot End

    @Override
    protected void defineSynchedData() {
        this.entityData.define(EntityFireworks.DATA_ID_FIREWORKS_ITEM, ItemStack.EMPTY);
        this.entityData.define(EntityFireworks.DATA_ATTACHED_TO_TARGET, OptionalInt.empty());
        this.entityData.define(EntityFireworks.DATA_SHOT_AT_ANGLE, false);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d0) {
        return d0 < 4096.0D && !this.isAttachedToEntity();
    }

    @Override
    public boolean shouldRender(double d0, double d1, double d2) {
        return super.shouldRender(d0, d1, d2) && !this.isAttachedToEntity();
    }

    @Override
    public void tick() {
        super.tick();
        Vec3D vec3d;

        if (this.isAttachedToEntity()) {
            if (this.attachedToEntity == null) {
                ((OptionalInt) this.entityData.get(EntityFireworks.DATA_ATTACHED_TO_TARGET)).ifPresent((i) -> {
                    Entity entity = this.level.getEntity(i);

                    if (entity instanceof EntityLiving) {
                        this.attachedToEntity = (EntityLiving) entity;
                    }

                });
            }

            if (this.attachedToEntity != null) {
                if (this.attachedToEntity.isFallFlying()) {
                    vec3d = this.attachedToEntity.getLookAngle();
                    double d0 = 1.5D;
                    double d1 = 0.1D;
                    Vec3D vec3d1 = this.attachedToEntity.getDeltaMovement();

                    this.attachedToEntity.setDeltaMovement(vec3d1.add(vec3d.x * 0.1D + (vec3d.x * 1.5D - vec3d1.x) * 0.5D, vec3d.y * 0.1D + (vec3d.y * 1.5D - vec3d1.y) * 0.5D, vec3d.z * 0.1D + (vec3d.z * 1.5D - vec3d1.z) * 0.5D));
                }

                this.setPos(this.attachedToEntity.getX(), this.attachedToEntity.getY(), this.attachedToEntity.getZ());
                this.setDeltaMovement(this.attachedToEntity.getDeltaMovement());
            }
        } else {
            if (!this.isShotAtAngle()) {
                double d2 = this.horizontalCollision ? 1.0D : 1.15D;

                this.setDeltaMovement(this.getDeltaMovement().multiply(d2, 1.0D, d2).add(0.0D, 0.04D, 0.0D));
            }

            vec3d = this.getDeltaMovement();
            this.move(EnumMoveType.SELF, vec3d);
            this.setDeltaMovement(vec3d);
        }

        MovingObjectPosition movingobjectposition = ProjectileHelper.getHitResult(this, this::canHitEntity);

        if (!this.noPhysics) {
            this.preOnHit(movingobjectposition); // CraftBukkit - projectile hit event
            this.hasImpulse = true;
        }

        this.updateRotation();
        if (this.life == 0 && !this.isSilent()) {
            this.level.playSound((EntityHuman) null, this.getX(), this.getY(), this.getZ(), SoundEffects.FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT, 3.0F, 1.0F);
        }

        ++this.life;
        if (this.level.isClientSide && this.life % 2 < 2) {
            this.level.addParticle(Particles.FIREWORK, this.getX(), this.getY() - 0.3D, this.getZ(), this.random.nextGaussian() * 0.05D, -this.getDeltaMovement().y * 0.5D, this.random.nextGaussian() * 0.05D);
        }

        if (!this.level.isClientSide && this.life > this.lifetime) {
            // CraftBukkit start
            if (!org.bukkit.craftbukkit.event.CraftEventFactory.callFireworkExplodeEvent(this).isCancelled()) {
                this.explode();
            }
            // CraftBukkit end
        }

    }

    private void explode() {
        this.level.broadcastEntityEvent(this, (byte) 17);
        this.gameEvent(GameEvent.EXPLODE, this.getOwner());
        this.dealExplosionDamage();
        this.discard();
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity movingobjectpositionentity) {
        super.onHitEntity(movingobjectpositionentity);
        if (!this.level.isClientSide) {
            // CraftBukkit start
            if (!org.bukkit.craftbukkit.event.CraftEventFactory.callFireworkExplodeEvent(this).isCancelled()) {
                this.explode();
            }
            // CraftBukkit end
        }
    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock movingobjectpositionblock) {
        BlockPosition blockposition = new BlockPosition(movingobjectpositionblock.getBlockPos());

        this.level.getBlockState(blockposition).entityInside(this.level, blockposition, this);
        if (!this.level.isClientSide() && this.hasExplosion()) {
            // CraftBukkit start
            if (!org.bukkit.craftbukkit.event.CraftEventFactory.callFireworkExplodeEvent(this).isCancelled()) {
                this.explode();
            }
            // CraftBukkit end
        }

        super.onHitBlock(movingobjectpositionblock);
    }

    private boolean hasExplosion() {
        ItemStack itemstack = (ItemStack) this.entityData.get(EntityFireworks.DATA_ID_FIREWORKS_ITEM);
        NBTTagCompound nbttagcompound = itemstack.isEmpty() ? null : itemstack.getTagElement("Fireworks");
        NBTTagList nbttaglist = nbttagcompound != null ? nbttagcompound.getList("Explosions", 10) : null;

        return nbttaglist != null && !nbttaglist.isEmpty();
    }

    private void dealExplosionDamage() {
        float f = 0.0F;
        ItemStack itemstack = (ItemStack) this.entityData.get(EntityFireworks.DATA_ID_FIREWORKS_ITEM);
        NBTTagCompound nbttagcompound = itemstack.isEmpty() ? null : itemstack.getTagElement("Fireworks");
        NBTTagList nbttaglist = nbttagcompound != null ? nbttagcompound.getList("Explosions", 10) : null;

        if (nbttaglist != null && !nbttaglist.isEmpty()) {
            f = 5.0F + (float) (nbttaglist.size() * 2);
        }

        if (f > 0.0F) {
            if (this.attachedToEntity != null) {
                CraftEventFactory.entityDamage = this; // CraftBukkit
                this.attachedToEntity.hurt(DamageSource.fireworks(this, this.getOwner()), 5.0F + (float) (nbttaglist.size() * 2));
                CraftEventFactory.entityDamage = null; // CraftBukkit
            }

            double d0 = 5.0D;
            Vec3D vec3d = this.position();
            List<EntityLiving> list = this.level.getEntitiesOfClass(EntityLiving.class, this.getBoundingBox().inflate(5.0D));
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityLiving entityliving = (EntityLiving) iterator.next();

                if (entityliving != this.attachedToEntity && this.distanceToSqr((Entity) entityliving) <= 25.0D) {
                    boolean flag = false;

                    for (int i = 0; i < 2; ++i) {
                        Vec3D vec3d1 = new Vec3D(entityliving.getX(), entityliving.getY(0.5D * (double) i), entityliving.getZ());
                        MovingObjectPositionBlock movingobjectpositionblock = this.level.clip(new RayTrace(vec3d, vec3d1, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, this));

                        if (movingobjectpositionblock.getType() == MovingObjectPosition.EnumMovingObjectType.MISS) {
                            flag = true;
                            break;
                        }
                    }

                    if (flag) {
                        float f1 = f * (float) Math.sqrt((5.0D - (double) this.distanceTo(entityliving)) / 5.0D);

                        CraftEventFactory.entityDamage = this; // CraftBukkit
                        entityliving.hurt(DamageSource.fireworks(this, this.getOwner()), f1);
                        CraftEventFactory.entityDamage = null; // CraftBukkit
                    }
                }
            }
        }

    }

    private boolean isAttachedToEntity() {
        return ((OptionalInt) this.entityData.get(EntityFireworks.DATA_ATTACHED_TO_TARGET)).isPresent();
    }

    public boolean isShotAtAngle() {
        return (Boolean) this.entityData.get(EntityFireworks.DATA_SHOT_AT_ANGLE);
    }

    @Override
    public void handleEntityEvent(byte b0) {
        if (b0 == 17 && this.level.isClientSide) {
            if (!this.hasExplosion()) {
                for (int i = 0; i < this.random.nextInt(3) + 2; ++i) {
                    this.level.addParticle(Particles.POOF, this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.05D, 0.005D, this.random.nextGaussian() * 0.05D);
                }
            } else {
                ItemStack itemstack = (ItemStack) this.entityData.get(EntityFireworks.DATA_ID_FIREWORKS_ITEM);
                NBTTagCompound nbttagcompound = itemstack.isEmpty() ? null : itemstack.getTagElement("Fireworks");
                Vec3D vec3d = this.getDeltaMovement();

                this.level.createFireworks(this.getX(), this.getY(), this.getZ(), vec3d.x, vec3d.y, vec3d.z, nbttagcompound);
            }
        }

        super.handleEntityEvent(b0);
    }

    @Override
    public void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        super.addAdditionalSaveData(nbttagcompound);
        nbttagcompound.putInt("Life", this.life);
        nbttagcompound.putInt("LifeTime", this.lifetime);
        ItemStack itemstack = (ItemStack) this.entityData.get(EntityFireworks.DATA_ID_FIREWORKS_ITEM);

        if (!itemstack.isEmpty()) {
            nbttagcompound.put("FireworksItem", itemstack.save(new NBTTagCompound()));
        }

        nbttagcompound.putBoolean("ShotAtAngle", (Boolean) this.entityData.get(EntityFireworks.DATA_SHOT_AT_ANGLE));
    }

    @Override
    public void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        super.readAdditionalSaveData(nbttagcompound);
        this.life = nbttagcompound.getInt("Life");
        this.lifetime = nbttagcompound.getInt("LifeTime");
        ItemStack itemstack = ItemStack.of(nbttagcompound.getCompound("FireworksItem"));

        if (!itemstack.isEmpty()) {
            this.entityData.set(EntityFireworks.DATA_ID_FIREWORKS_ITEM, itemstack);
        }

        if (nbttagcompound.contains("ShotAtAngle")) {
            this.entityData.set(EntityFireworks.DATA_SHOT_AT_ANGLE, nbttagcompound.getBoolean("ShotAtAngle"));
        }

    }

    @Override
    public ItemStack getItem() {
        ItemStack itemstack = (ItemStack) this.entityData.get(EntityFireworks.DATA_ID_FIREWORKS_ITEM);

        return itemstack.isEmpty() ? new ItemStack(Items.FIREWORK_ROCKET) : itemstack;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
