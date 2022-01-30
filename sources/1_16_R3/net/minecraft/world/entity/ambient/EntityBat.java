package net.minecraft.world.entity.ambient;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

import org.bukkit.craftbukkit.event.CraftEventFactory; // CraftBukkit

public class EntityBat extends EntityAmbient {

    private static final DataWatcherObject<Byte> b = DataWatcher.a(EntityBat.class, DataWatcherRegistry.a);
    private static final PathfinderTargetCondition c = (new PathfinderTargetCondition()).a(4.0D).b();
    private BlockPosition d;

    public EntityBat(EntityTypes<? extends EntityBat> entitytypes, World world) {
        super(entitytypes, world);
        this.setAsleep(true);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(EntityBat.b, (byte) 0);
    }

    @Override
    protected float getSoundVolume() {
        return 0.1F;
    }

    @Override
    protected float dH() {
        return super.dH() * 0.95F;
    }

    @Nullable
    @Override
    public SoundEffect getSoundAmbient() {
        return this.isAsleep() && this.random.nextInt(4) != 0 ? null : SoundEffects.ENTITY_BAT_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource damagesource) {
        return SoundEffects.ENTITY_BAT_HURT;
    }

    @Override
    protected SoundEffect getSoundDeath() {
        return SoundEffects.ENTITY_BAT_DEATH;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    protected void C(Entity entity) {}

    @Override
    protected void collideNearby() {}

    public static AttributeProvider.Builder m() {
        return EntityInsentient.p().a(GenericAttributes.MAX_HEALTH, 6.0D);
    }

    public boolean isAsleep() {
        return ((Byte) this.datawatcher.get(EntityBat.b) & 1) != 0;
    }

    public void setAsleep(boolean flag) {
        byte b0 = (Byte) this.datawatcher.get(EntityBat.b);

        if (flag) {
            this.datawatcher.set(EntityBat.b, (byte) (b0 | 1));
        } else {
            this.datawatcher.set(EntityBat.b, (byte) (b0 & -2));
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAsleep()) {
            this.setMot(Vec3D.ORIGIN);
            this.setPositionRaw(this.locX(), (double) MathHelper.floor(this.locY()) + 1.0D - (double) this.getHeight(), this.locZ());
        } else {
            this.setMot(this.getMot().d(1.0D, 0.6D, 1.0D));
        }

    }

    @Override
    protected void mobTick() {
        super.mobTick();
        BlockPosition blockposition = this.getChunkCoordinates();
        BlockPosition blockposition1 = blockposition.up();

        if (this.isAsleep()) {
            boolean flag = this.isSilent();

            if (this.world.getType(blockposition1).isOccluding(this.world, blockposition)) {
                if (this.random.nextInt(200) == 0) {
                    this.aC = (float) this.random.nextInt(360);
                }

                if (this.world.a(EntityBat.c, (EntityLiving) this) != null) {
                    // CraftBukkit Start - Call BatToggleSleepEvent
                    if (CraftEventFactory.handleBatToggleSleepEvent(this, true)) {
                        this.setAsleep(false);
                        if (!flag) {
                            this.world.a((EntityHuman) null, 1025, blockposition, 0);
                        }
                    }
                    // CraftBukkit End
                }
            } else {
                // CraftBukkit Start - Call BatToggleSleepEvent
                if (CraftEventFactory.handleBatToggleSleepEvent(this, true)) {
                    this.setAsleep(false);
                    if (!flag) {
                        this.world.a((EntityHuman) null, 1025, blockposition, 0);
                    }
                }
                // CraftBukkit End - Call BatToggleSleepEvent
            }
        } else {
            if (this.d != null && (!this.world.isEmpty(this.d) || this.d.getY() < 1)) {
                this.d = null;
            }

            if (this.d == null || this.random.nextInt(30) == 0 || this.d.a((IPosition) this.getPositionVector(), 2.0D)) {
                this.d = new BlockPosition(this.locX() + (double) this.random.nextInt(7) - (double) this.random.nextInt(7), this.locY() + (double) this.random.nextInt(6) - 2.0D, this.locZ() + (double) this.random.nextInt(7) - (double) this.random.nextInt(7));
            }

            double d0 = (double) this.d.getX() + 0.5D - this.locX();
            double d1 = (double) this.d.getY() + 0.1D - this.locY();
            double d2 = (double) this.d.getZ() + 0.5D - this.locZ();
            Vec3D vec3d = this.getMot();
            Vec3D vec3d1 = vec3d.add((Math.signum(d0) * 0.5D - vec3d.x) * 0.10000000149011612D, (Math.signum(d1) * 0.699999988079071D - vec3d.y) * 0.10000000149011612D, (Math.signum(d2) * 0.5D - vec3d.z) * 0.10000000149011612D);

            this.setMot(vec3d1);
            float f = (float) (MathHelper.d(vec3d1.z, vec3d1.x) * 57.2957763671875D) - 90.0F;
            float f1 = MathHelper.g(f - this.yaw);

            this.aT = 0.5F;
            this.yaw += f1;
            if (this.random.nextInt(100) == 0 && this.world.getType(blockposition1).isOccluding(this.world, blockposition1)) {
                // CraftBukkit Start - Call BatToggleSleepEvent
                if (CraftEventFactory.handleBatToggleSleepEvent(this, false)) {
                    this.setAsleep(true);
                }
                // CraftBukkit End
            }
        }

    }

    @Override
    protected boolean playStepSound() {
        return false;
    }

    @Override
    public boolean b(float f, float f1) {
        return false;
    }

    @Override
    protected void a(double d0, boolean flag, IBlockData iblockdata, BlockPosition blockposition) {}

    @Override
    public boolean isIgnoreBlockTrigger() {
        return true;
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (this.isInvulnerable(damagesource)) {
            return false;
        } else {
            if (!this.world.isClientSide && this.isAsleep()) {
                // CraftBukkit Start - Call BatToggleSleepEvent
                if (CraftEventFactory.handleBatToggleSleepEvent(this, true)) {
                    this.setAsleep(false);
                }
                // CraftBukkit End - Call BatToggleSleepEvent
            }

            return super.damageEntity(damagesource, f);
        }
    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        super.loadData(nbttagcompound);
        this.datawatcher.set(EntityBat.b, nbttagcompound.getByte("BatFlags"));
    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        super.saveData(nbttagcompound);
        nbttagcompound.setByte("BatFlags", (Byte) this.datawatcher.get(EntityBat.b));
    }

    public static boolean b(EntityTypes<EntityBat> entitytypes, GeneratorAccess generatoraccess, EnumMobSpawn enummobspawn, BlockPosition blockposition, Random random) {
        if (blockposition.getY() >= generatoraccess.getSeaLevel()) {
            return false;
        } else {
            int i = generatoraccess.getLightLevel(blockposition);
            byte b0 = 4;

            if (eJ()) {
                b0 = 7;
            } else if (random.nextBoolean()) {
                return false;
            }

            return i > random.nextInt(b0) ? false : a(entitytypes, generatoraccess, enummobspawn, blockposition, random);
        }
    }

    private static boolean eJ() {
        LocalDate localdate = LocalDate.now();
        int i = localdate.get(ChronoField.DAY_OF_MONTH);
        int j = localdate.get(ChronoField.MONTH_OF_YEAR);

        return j == 10 && i >= 20 || j == 11 && i <= 3;
    }

    @Override
    protected float b(EntityPose entitypose, EntitySize entitysize) {
        return entitysize.height / 2.0F;
    }
}
