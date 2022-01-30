package net.minecraft.world.entity.decoration;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDiodeAbstract;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.lang3.Validate;

// CraftBukkit start
import net.minecraft.world.damagesource.EntityDamageSourceIndirect;
import net.minecraft.world.level.material.Material;
import org.bukkit.entity.Hanging;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
// CraftBukkit end

public abstract class EntityHanging extends Entity {

    protected static final Predicate<Entity> HANGING_ENTITY = (entity) -> {
        return entity instanceof EntityHanging;
    };
    private int checkInterval;
    public BlockPosition pos;
    protected EnumDirection direction;

    protected EntityHanging(EntityTypes<? extends EntityHanging> entitytypes, World world) {
        super(entitytypes, world);
        this.direction = EnumDirection.SOUTH;
    }

    protected EntityHanging(EntityTypes<? extends EntityHanging> entitytypes, World world, BlockPosition blockposition) {
        this(entitytypes, world);
        this.pos = blockposition;
    }

    @Override
    protected void initDatawatcher() {}

    public void setDirection(EnumDirection enumdirection) {
        Validate.notNull(enumdirection);
        Validate.isTrue(enumdirection.n().d());
        this.direction = enumdirection;
        this.setYRot((float) (this.direction.get2DRotationValue() * 90));
        this.yRotO = this.getYRot();
        this.updateBoundingBox();
    }

    protected void updateBoundingBox() {
        if (this.direction != null) {
            // CraftBukkit start code moved in to calculateBoundingBox
            this.a(calculateBoundingBox(this, this.pos, this.direction, this.getHangingWidth(), this.getHangingHeight()));
            // CraftBukkit end
        }
    }

    // CraftBukkit start - break out BB calc into own method
    public static AxisAlignedBB calculateBoundingBox(@Nullable Entity entity, BlockPosition blockPosition, EnumDirection direction, int width, int height) {
        {
            double d0 = (double) blockPosition.getX() + 0.5D;
            double d1 = (double) blockPosition.getY() + 0.5D;
            double d2 = (double) blockPosition.getZ() + 0.5D;
            double d3 = 0.46875D;
            double d4 = a(width);
            double d5 = a(height);

            d0 -= (double) direction.getAdjacentX() * 0.46875D;
            d2 -= (double) direction.getAdjacentZ() * 0.46875D;
            d1 += d5;
            EnumDirection enumdirection = direction.h();

            d0 += d4 * (double) enumdirection.getAdjacentX();
            d2 += d4 * (double) enumdirection.getAdjacentZ();
            if (entity != null) {
                entity.setPositionRaw(d0, d1, d2);
            }
            double d6 = (double) width;
            double d7 = (double) height;
            double d8 = (double) width;

            if (direction.n() == EnumDirection.EnumAxis.Z) {
                d8 = 1.0D;
            } else {
                d6 = 1.0D;
            }

            d6 /= 32.0D;
            d7 /= 32.0D;
            d8 /= 32.0D;
            return new AxisAlignedBB(d0 - d6, d1 - d7, d2 - d8, d0 + d6, d1 + d7, d2 + d8);
        }
    }
    // CraftBukkit end

    private static double a(int i) { // CraftBukkit - static
        return i % 32 == 0 ? 0.5D : 0.0D;
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            this.aj();
            if (this.checkInterval++ == this.level.spigotConfig.hangingTickFrequency) { // Spigot
                this.checkInterval = 0;
                if (!this.isRemoved() && !this.survives()) {
                    // CraftBukkit start - fire break events
                    Material material = this.level.getType(this.getChunkCoordinates()).getMaterial();
                    HangingBreakEvent.RemoveCause cause;

                    if (!material.equals(Material.AIR)) {
                        // TODO: This feels insufficient to catch 100% of suffocation cases
                        cause = HangingBreakEvent.RemoveCause.OBSTRUCTION;
                    } else {
                        cause = HangingBreakEvent.RemoveCause.PHYSICS;
                    }

                    HangingBreakEvent event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), cause);
                    this.level.getCraftServer().getPluginManager().callEvent(event);

                    if (this.isRemoved() || event.isCancelled()) {
                        return;
                    }
                    // CraftBukkit end
                    this.die();
                    this.a((Entity) null);
                }
            }
        }

    }

    public boolean survives() {
        if (!this.level.getCubes(this)) {
            return false;
        } else {
            int i = Math.max(1, this.getHangingWidth() / 16);
            int j = Math.max(1, this.getHangingHeight() / 16);
            BlockPosition blockposition = this.pos.shift(this.direction.opposite());
            EnumDirection enumdirection = this.direction.h();
            BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

            for (int k = 0; k < i; ++k) {
                for (int l = 0; l < j; ++l) {
                    int i1 = (i - 1) / -2;
                    int j1 = (j - 1) / -2;

                    blockposition_mutableblockposition.g(blockposition).c(enumdirection, k + i1).c(EnumDirection.UP, l + j1);
                    IBlockData iblockdata = this.level.getType(blockposition_mutableblockposition);

                    if (!iblockdata.getMaterial().isBuildable() && !BlockDiodeAbstract.isDiode(iblockdata)) {
                        return false;
                    }
                }
            }

            return this.level.getEntities(this, this.getBoundingBox(), EntityHanging.HANGING_ENTITY).isEmpty();
        }
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean r(Entity entity) {
        if (entity instanceof EntityHuman) {
            EntityHuman entityhuman = (EntityHuman) entity;

            return !this.level.a(entityhuman, this.pos) ? true : this.damageEntity(DamageSource.playerAttack(entityhuman), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public EnumDirection getDirection() {
        return this.direction;
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (this.isInvulnerable(damagesource)) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level.isClientSide) {
                // CraftBukkit start - fire break events
                Entity damager = (damagesource instanceof EntityDamageSourceIndirect) ? ((EntityDamageSourceIndirect) damagesource).getProximateDamageSource() : damagesource.getEntity();
                HangingBreakEvent event;
                if (damager != null) {
                    event = new HangingBreakByEntityEvent((Hanging) this.getBukkitEntity(), damager.getBukkitEntity(), damagesource.isExplosion() ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.ENTITY);
                } else {
                    event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), damagesource.isExplosion() ? HangingBreakEvent.RemoveCause.EXPLOSION : HangingBreakEvent.RemoveCause.DEFAULT);
                }

                this.level.getCraftServer().getPluginManager().callEvent(event);

                if (this.isRemoved() || event.isCancelled()) {
                    return true;
                }
                // CraftBukkit end

                this.killEntity();
                this.velocityChanged();
                this.a(damagesource.getEntity());
            }

            return true;
        }
    }

    @Override
    public void move(EnumMoveType enummovetype, Vec3D vec3d) {
        if (!this.level.isClientSide && !this.isRemoved() && vec3d.g() > 0.0D) {
            if (this.isRemoved()) return; // CraftBukkit

            // CraftBukkit start - fire break events
            // TODO - Does this need its own cause? Seems to only be triggered by pistons
            HangingBreakEvent event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), HangingBreakEvent.RemoveCause.PHYSICS);
            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (this.isRemoved() || event.isCancelled()) {
                return;
            }
            // CraftBukkit end

            this.killEntity();
            this.a((Entity) null);
        }

    }

    @Override
    public void i(double d0, double d1, double d2) {
        if (false && !this.level.isClientSide && !this.isRemoved() && d0 * d0 + d1 * d1 + d2 * d2 > 0.0D) { // CraftBukkit - not needed
            this.killEntity();
            this.a((Entity) null);
        }

    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        BlockPosition blockposition = this.getBlockPosition();

        nbttagcompound.setInt("TileX", blockposition.getX());
        nbttagcompound.setInt("TileY", blockposition.getY());
        nbttagcompound.setInt("TileZ", blockposition.getZ());
    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        this.pos = new BlockPosition(nbttagcompound.getInt("TileX"), nbttagcompound.getInt("TileY"), nbttagcompound.getInt("TileZ"));
    }

    public abstract int getHangingWidth();

    public abstract int getHangingHeight();

    public abstract void a(@Nullable Entity entity);

    public abstract void playPlaceSound();

    @Override
    public EntityItem a(ItemStack itemstack, float f) {
        EntityItem entityitem = new EntityItem(this.level, this.locX() + (double) ((float) this.direction.getAdjacentX() * 0.15F), this.locY() + (double) f, this.locZ() + (double) ((float) this.direction.getAdjacentZ() * 0.15F), itemstack);

        entityitem.defaultPickupDelay();
        this.level.addEntity(entityitem);
        return entityitem;
    }

    @Override
    protected boolean be() {
        return false;
    }

    @Override
    public void setPosition(double d0, double d1, double d2) {
        this.pos = new BlockPosition(d0, d1, d2);
        this.updateBoundingBox();
        this.hasImpulse = true;
    }

    public BlockPosition getBlockPosition() {
        return this.pos;
    }

    @Override
    public float a(EnumBlockRotation enumblockrotation) {
        if (this.direction.n() != EnumDirection.EnumAxis.Y) {
            switch (enumblockrotation) {
                case CLOCKWISE_180:
                    this.direction = this.direction.opposite();
                    break;
                case COUNTERCLOCKWISE_90:
                    this.direction = this.direction.h();
                    break;
                case CLOCKWISE_90:
                    this.direction = this.direction.g();
            }
        }

        float f = MathHelper.g(this.getYRot());

        switch (enumblockrotation) {
            case CLOCKWISE_180:
                return f + 180.0F;
            case COUNTERCLOCKWISE_90:
                return f + 90.0F;
            case CLOCKWISE_90:
                return f + 270.0F;
            default:
                return f;
        }
    }

    @Override
    public float a(EnumBlockMirror enumblockmirror) {
        return this.a(enumblockmirror.a(this.direction));
    }

    @Override
    public void onLightningStrike(WorldServer worldserver, EntityLightning entitylightning) {}

    @Override
    public void updateSize() {}
}
