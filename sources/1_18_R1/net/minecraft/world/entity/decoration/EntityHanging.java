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
    protected void defineSynchedData() {}

    public void setDirection(EnumDirection enumdirection) {
        Validate.notNull(enumdirection);
        Validate.isTrue(enumdirection.getAxis().isHorizontal());
        this.direction = enumdirection;
        this.setYRot((float) (this.direction.get2DDataValue() * 90));
        this.yRotO = this.getYRot();
        this.recalculateBoundingBox();
    }

    protected void recalculateBoundingBox() {
        if (this.direction != null) {
            // CraftBukkit start code moved in to calculateBoundingBox
            this.setBoundingBox(calculateBoundingBox(this, this.pos, this.direction, this.getWidth(), this.getHeight()));
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
            double d4 = offs(width);
            double d5 = offs(height);

            d0 -= (double) direction.getStepX() * 0.46875D;
            d2 -= (double) direction.getStepZ() * 0.46875D;
            d1 += d5;
            EnumDirection enumdirection = direction.getCounterClockWise();

            d0 += d4 * (double) enumdirection.getStepX();
            d2 += d4 * (double) enumdirection.getStepZ();
            if (entity != null) {
                entity.setPosRaw(d0, d1, d2);
            }
            double d6 = (double) width;
            double d7 = (double) height;
            double d8 = (double) width;

            if (direction.getAxis() == EnumDirection.EnumAxis.Z) {
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

    private static double offs(int i) { // CraftBukkit - static
        return i % 32 == 0 ? 0.5D : 0.0D;
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            this.checkOutOfWorld();
            if (this.checkInterval++ == this.level.spigotConfig.hangingTickFrequency) { // Spigot
                this.checkInterval = 0;
                if (!this.isRemoved() && !this.survives()) {
                    // CraftBukkit start - fire break events
                    Material material = this.level.getBlockState(this.blockPosition()).getMaterial();
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
                    this.discard();
                    this.dropItem((Entity) null);
                }
            }
        }

    }

    public boolean survives() {
        if (!this.level.noCollision((Entity) this)) {
            return false;
        } else {
            int i = Math.max(1, this.getWidth() / 16);
            int j = Math.max(1, this.getHeight() / 16);
            BlockPosition blockposition = this.pos.relative(this.direction.getOpposite());
            EnumDirection enumdirection = this.direction.getCounterClockWise();
            BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

            for (int k = 0; k < i; ++k) {
                for (int l = 0; l < j; ++l) {
                    int i1 = (i - 1) / -2;
                    int j1 = (j - 1) / -2;

                    blockposition_mutableblockposition.set(blockposition).move(enumdirection, k + i1).move(EnumDirection.UP, l + j1);
                    IBlockData iblockdata = this.level.getBlockState(blockposition_mutableblockposition);

                    if (!iblockdata.getMaterial().isSolid() && !BlockDiodeAbstract.isDiode(iblockdata)) {
                        return false;
                    }
                }
            }

            return this.level.getEntities((Entity) this, this.getBoundingBox(), EntityHanging.HANGING_ENTITY).isEmpty();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        if (entity instanceof EntityHuman) {
            EntityHuman entityhuman = (EntityHuman) entity;

            return !this.level.mayInteract(entityhuman, this.pos) ? true : this.hurt(DamageSource.playerAttack(entityhuman), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public EnumDirection getDirection() {
        return this.direction;
    }

    @Override
    public boolean hurt(DamageSource damagesource, float f) {
        if (this.isInvulnerableTo(damagesource)) {
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

                this.kill();
                this.markHurt();
                this.dropItem(damagesource.getEntity());
            }

            return true;
        }
    }

    @Override
    public void move(EnumMoveType enummovetype, Vec3D vec3d) {
        if (!this.level.isClientSide && !this.isRemoved() && vec3d.lengthSqr() > 0.0D) {
            if (this.isRemoved()) return; // CraftBukkit

            // CraftBukkit start - fire break events
            // TODO - Does this need its own cause? Seems to only be triggered by pistons
            HangingBreakEvent event = new HangingBreakEvent((Hanging) this.getBukkitEntity(), HangingBreakEvent.RemoveCause.PHYSICS);
            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (this.isRemoved() || event.isCancelled()) {
                return;
            }
            // CraftBukkit end

            this.kill();
            this.dropItem((Entity) null);
        }

    }

    @Override
    public void push(double d0, double d1, double d2) {
        if (false && !this.level.isClientSide && !this.isRemoved() && d0 * d0 + d1 * d1 + d2 * d2 > 0.0D) { // CraftBukkit - not needed
            this.kill();
            this.dropItem((Entity) null);
        }

    }

    @Override
    public void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        BlockPosition blockposition = this.getPos();

        nbttagcompound.putInt("TileX", blockposition.getX());
        nbttagcompound.putInt("TileY", blockposition.getY());
        nbttagcompound.putInt("TileZ", blockposition.getZ());
    }

    @Override
    public void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        this.pos = new BlockPosition(nbttagcompound.getInt("TileX"), nbttagcompound.getInt("TileY"), nbttagcompound.getInt("TileZ"));
    }

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract void dropItem(@Nullable Entity entity);

    public abstract void playPlacementSound();

    @Override
    public EntityItem spawnAtLocation(ItemStack itemstack, float f) {
        EntityItem entityitem = new EntityItem(this.level, this.getX() + (double) ((float) this.direction.getStepX() * 0.15F), this.getY() + (double) f, this.getZ() + (double) ((float) this.direction.getStepZ() * 0.15F), itemstack);

        entityitem.setDefaultPickUpDelay();
        this.level.addFreshEntity(entityitem);
        return entityitem;
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public void setPos(double d0, double d1, double d2) {
        this.pos = new BlockPosition(d0, d1, d2);
        this.recalculateBoundingBox();
        this.hasImpulse = true;
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    @Override
    public float rotate(EnumBlockRotation enumblockrotation) {
        if (this.direction.getAxis() != EnumDirection.EnumAxis.Y) {
            switch (enumblockrotation) {
                case CLOCKWISE_180:
                    this.direction = this.direction.getOpposite();
                    break;
                case COUNTERCLOCKWISE_90:
                    this.direction = this.direction.getCounterClockWise();
                    break;
                case CLOCKWISE_90:
                    this.direction = this.direction.getClockWise();
            }
        }

        float f = MathHelper.wrapDegrees(this.getYRot());

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
    public float mirror(EnumBlockMirror enumblockmirror) {
        return this.rotate(enumblockmirror.getRotation(this.direction));
    }

    @Override
    public void thunderHit(WorldServer worldserver, EntityLightning entitylightning) {}

    @Override
    public void refreshDimensions() {}
}
