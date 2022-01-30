package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityEndGateway;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityProjectile extends IProjectile {

    protected EntityProjectile(EntityTypes<? extends EntityProjectile> entitytypes, World world) {
        super(entitytypes, world);
    }

    protected EntityProjectile(EntityTypes<? extends EntityProjectile> entitytypes, double d0, double d1, double d2, World world) {
        this(entitytypes, world);
        this.setPosition(d0, d1, d2);
    }

    protected EntityProjectile(EntityTypes<? extends EntityProjectile> entitytypes, EntityLiving entityliving, World world) {
        this(entitytypes, entityliving.locX(), entityliving.getHeadY() - 0.10000000149011612D, entityliving.locZ(), world);
        this.setShooter(entityliving);
    }

    @Override
    public boolean a(double d0) {
        double d1 = this.getBoundingBox().a() * 4.0D;

        if (Double.isNaN(d1)) {
            d1 = 4.0D;
        }

        d1 *= 64.0D;
        return d0 < d1 * d1;
    }

    @Override
    public void tick() {
        super.tick();
        MovingObjectPosition movingobjectposition = ProjectileHelper.a((Entity) this, this::a);
        boolean flag = false;

        if (movingobjectposition.getType() == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            BlockPosition blockposition = ((MovingObjectPositionBlock) movingobjectposition).getBlockPosition();
            IBlockData iblockdata = this.level.getType(blockposition);

            if (iblockdata.a(Blocks.NETHER_PORTAL)) {
                this.d(blockposition);
                flag = true;
            } else if (iblockdata.a(Blocks.END_GATEWAY)) {
                TileEntity tileentity = this.level.getTileEntity(blockposition);

                if (tileentity instanceof TileEntityEndGateway && TileEntityEndGateway.a((Entity) this)) {
                    TileEntityEndGateway.a(this.level, blockposition, iblockdata, this, (TileEntityEndGateway) tileentity);
                }

                flag = true;
            }
        }

        if (movingobjectposition.getType() != MovingObjectPosition.EnumMovingObjectType.MISS && !flag) {
            this.preOnHit(movingobjectposition); // CraftBukkit - projectile hit event
        }

        this.checkBlockCollisions();
        Vec3D vec3d = this.getMot();
        double d0 = this.locX() + vec3d.x;
        double d1 = this.locY() + vec3d.y;
        double d2 = this.locZ() + vec3d.z;

        this.z();
        float f;

        if (this.isInWater()) {
            for (int i = 0; i < 4; ++i) {
                float f1 = 0.25F;

                this.level.addParticle(Particles.BUBBLE, d0 - vec3d.x * 0.25D, d1 - vec3d.y * 0.25D, d2 - vec3d.z * 0.25D, vec3d.x, vec3d.y, vec3d.z);
            }

            f = 0.8F;
        } else {
            f = 0.99F;
        }

        this.setMot(vec3d.a((double) f));
        if (!this.isNoGravity()) {
            Vec3D vec3d1 = this.getMot();

            this.setMot(vec3d1.x, vec3d1.y - (double) this.l(), vec3d1.z);
        }

        this.setPosition(d0, d1, d2);
    }

    protected float l() {
        return 0.03F;
    }
}
