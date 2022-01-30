package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBowShoot;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.ProjectileHelper;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityIllagerIllusioner extends EntityIllagerWizard implements IRangedEntity {

    private static final int NUM_ILLUSIONS = 4;
    private static final int ILLUSION_TRANSITION_TICKS = 3;
    private static final int ILLUSION_SPREAD = 3;
    private int clientSideIllusionTicks;
    private final Vec3D[][] clientSideIllusionOffsets;

    public EntityIllagerIllusioner(EntityTypes<? extends EntityIllagerIllusioner> entitytypes, World world) {
        super(entitytypes, world);
        this.xpReward = 5;
        this.clientSideIllusionOffsets = new Vec3D[2][4];

        for (int i = 0; i < 4; ++i) {
            this.clientSideIllusionOffsets[0][i] = Vec3D.ZERO;
            this.clientSideIllusionOffsets[1][i] = Vec3D.ZERO;
        }

    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(1, new EntityIllagerWizard.b());
        this.goalSelector.a(4, new EntityIllagerIllusioner.b());
        this.goalSelector.a(5, new EntityIllagerIllusioner.a());
        this.goalSelector.a(6, new PathfinderGoalBowShoot<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.a(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 3.0F, 1.0F));
        this.goalSelector.a(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 8.0F));
        this.targetSelector.a(1, (new PathfinderGoalHurtByTarget(this, new Class[]{EntityRaider.class})).a(new Class[0])); // CraftBukkit - decompile error
        this.targetSelector.a(2, (new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true)).a(300));
        this.targetSelector.a(3, (new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, false)).a(300));
        this.targetSelector.a(3, (new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, false)).a(300));
    }

    public static AttributeProvider.Builder p() {
        return EntityMonster.fB().a(GenericAttributes.MOVEMENT_SPEED, 0.5D).a(GenericAttributes.FOLLOW_RANGE, 18.0D).a(GenericAttributes.MAX_HEALTH, 32.0D);
    }

    @Override
    public GroupDataEntity prepare(WorldAccess worldaccess, DifficultyDamageScaler difficultydamagescaler, EnumMobSpawn enummobspawn, @Nullable GroupDataEntity groupdataentity, @Nullable NBTTagCompound nbttagcompound) {
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.BOW));
        return super.prepare(worldaccess, difficultydamagescaler, enummobspawn, groupdataentity, nbttagcompound);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
    }

    @Override
    public AxisAlignedBB cs() {
        return this.getBoundingBox().grow(3.0D, 0.0D, 3.0D);
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.level.isClientSide && this.isInvisible()) {
            --this.clientSideIllusionTicks;
            if (this.clientSideIllusionTicks < 0) {
                this.clientSideIllusionTicks = 0;
            }

            if (this.hurtTime != 1 && this.tickCount % 1200 != 0) {
                if (this.hurtTime == this.hurtDuration - 1) {
                    this.clientSideIllusionTicks = 3;

                    for (int i = 0; i < 4; ++i) {
                        this.clientSideIllusionOffsets[0][i] = this.clientSideIllusionOffsets[1][i];
                        this.clientSideIllusionOffsets[1][i] = new Vec3D(0.0D, 0.0D, 0.0D);
                    }
                }
            } else {
                this.clientSideIllusionTicks = 3;
                float f = -6.0F;
                boolean flag = true;

                int j;

                for (j = 0; j < 4; ++j) {
                    this.clientSideIllusionOffsets[0][j] = this.clientSideIllusionOffsets[1][j];
                    this.clientSideIllusionOffsets[1][j] = new Vec3D((double) (-6.0F + (float) this.random.nextInt(13)) * 0.5D, (double) Math.max(0, this.random.nextInt(6) - 4), (double) (-6.0F + (float) this.random.nextInt(13)) * 0.5D);
                }

                for (j = 0; j < 16; ++j) {
                    this.level.addParticle(Particles.CLOUD, this.d(0.5D), this.da(), this.f(0.5D), 0.0D, 0.0D, 0.0D);
                }

                this.level.a(this.locX(), this.locY(), this.locZ(), SoundEffects.ILLUSIONER_MIRROR_MOVE, this.getSoundCategory(), 1.0F, 1.0F, false);
            }
        }

    }

    @Override
    public SoundEffect t() {
        return SoundEffects.ILLUSIONER_AMBIENT;
    }

    public Vec3D[] z(float f) {
        if (this.clientSideIllusionTicks <= 0) {
            return this.clientSideIllusionOffsets[1];
        } else {
            double d0 = (double) (((float) this.clientSideIllusionTicks - f) / 3.0F);

            d0 = Math.pow(d0, 0.25D);
            Vec3D[] avec3d = new Vec3D[4];

            for (int i = 0; i < 4; ++i) {
                avec3d[i] = this.clientSideIllusionOffsets[1][i].a(1.0D - d0).e(this.clientSideIllusionOffsets[0][i].a(d0));
            }

            return avec3d;
        }
    }

    @Override
    public boolean p(Entity entity) {
        return super.p(entity) ? true : (entity instanceof EntityLiving && ((EntityLiving) entity).getMonsterType() == EnumMonsterType.ILLAGER ? this.getScoreboardTeam() == null && entity.getScoreboardTeam() == null : false);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.ILLUSIONER_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundDeath() {
        return SoundEffects.ILLUSIONER_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource damagesource) {
        return SoundEffects.ILLUSIONER_HURT;
    }

    @Override
    protected SoundEffect getSoundCastSpell() {
        return SoundEffects.ILLUSIONER_CAST_SPELL;
    }

    @Override
    public void a(int i, boolean flag) {}

    @Override
    public void a(EntityLiving entityliving, float f) {
        ItemStack itemstack = this.h(this.b(ProjectileHelper.a((EntityLiving) this, Items.BOW)));
        EntityArrow entityarrow = ProjectileHelper.a(this, itemstack, f);
        double d0 = entityliving.locX() - this.locX();
        double d1 = entityliving.e(0.3333333333333333D) - entityarrow.locY();
        double d2 = entityliving.locZ() - this.locZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        entityarrow.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, 1.6F, (float) (14 - this.level.getDifficulty().a() * 4));
        this.playSound(SoundEffects.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addEntity(entityarrow);
    }

    @Override
    public EntityIllagerAbstract.a n() {
        return this.fG() ? EntityIllagerAbstract.a.SPELLCASTING : (this.isAggressive() ? EntityIllagerAbstract.a.BOW_AND_ARROW : EntityIllagerAbstract.a.CROSSED);
    }

    private class b extends EntityIllagerWizard.PathfinderGoalCastSpell {

        b() {
            super();
        }

        @Override
        public boolean a() {
            return !super.a() ? false : !EntityIllagerIllusioner.this.hasEffect(MobEffects.INVISIBILITY);
        }

        @Override
        protected int g() {
            return 20;
        }

        @Override
        protected int h() {
            return 340;
        }

        @Override
        protected void j() {
            EntityIllagerIllusioner.this.addEffect(new MobEffect(MobEffects.INVISIBILITY, 1200), org.bukkit.event.entity.EntityPotionEffectEvent.Cause.ILLUSION); // CraftBukkit
        }

        @Nullable
        @Override
        protected SoundEffect k() {
            return SoundEffects.ILLUSIONER_PREPARE_MIRROR;
        }

        @Override
        protected EntityIllagerWizard.Spell getCastSpell() {
            return EntityIllagerWizard.Spell.DISAPPEAR;
        }
    }

    private class a extends EntityIllagerWizard.PathfinderGoalCastSpell {

        private int lastTargetId;

        a() {
            super();
        }

        @Override
        public boolean a() {
            return !super.a() ? false : (EntityIllagerIllusioner.this.getGoalTarget() == null ? false : (EntityIllagerIllusioner.this.getGoalTarget().getId() == this.lastTargetId ? false : EntityIllagerIllusioner.this.level.getDamageScaler(EntityIllagerIllusioner.this.getChunkCoordinates()).a((float) EnumDifficulty.NORMAL.ordinal())));
        }

        @Override
        public void c() {
            super.c();
            this.lastTargetId = EntityIllagerIllusioner.this.getGoalTarget().getId();
        }

        @Override
        protected int g() {
            return 20;
        }

        @Override
        protected int h() {
            return 180;
        }

        @Override
        protected void j() {
            EntityIllagerIllusioner.this.getGoalTarget().addEffect(new MobEffect(MobEffects.BLINDNESS, 400), EntityIllagerIllusioner.this, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.ATTACK); // CraftBukkit
        }

        @Override
        protected SoundEffect k() {
            return SoundEffects.ILLUSIONER_PREPARE_BLINDNESS;
        }

        @Override
        protected EntityIllagerWizard.Spell getCastSpell() {
            return EntityIllagerWizard.Spell.BLINDNESS;
        }
    }
}
