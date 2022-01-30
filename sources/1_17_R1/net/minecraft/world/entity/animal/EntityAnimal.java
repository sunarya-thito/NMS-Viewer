package net.minecraft.world.entity.animal;

import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;

// CraftBukkit start
import net.minecraft.world.entity.EntityTameableAnimal;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
// CraftBukkit end

public abstract class EntityAnimal extends EntityAgeable {

    static final int PARENT_AGE_AFTER_BREEDING = 6000;
    public int inLove;
    public UUID loveCause;
    public ItemStack breedItem; // CraftBukkit - Add breedItem variable

    protected EntityAnimal(EntityTypes<? extends EntityAnimal> entitytypes, World world) {
        super(entitytypes, world);
        this.a(PathType.DANGER_FIRE, 16.0F);
        this.a(PathType.DAMAGE_FIRE, -1.0F);
    }

    @Override
    protected void mobTick() {
        if (this.getAge() != 0) {
            this.inLove = 0;
        }

        super.mobTick();
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.getAge() != 0) {
            this.inLove = 0;
        }

        if (this.inLove > 0) {
            --this.inLove;
            if (this.inLove % 10 == 0) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;

                this.level.addParticle(Particles.HEART, this.d(1.0D), this.da() + 0.5D, this.g(1.0D), d0, d1, d2);
            }
        }

    }

    /* CraftBukkit start
    // Function disabled as it has no special function anymore after
    // setSitting is disabled.
    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (this.isInvulnerable(damagesource)) {
            return false;
        } else {
            this.inLove = 0;
            return super.damageEntity(damagesource, f);
        }
    }
    // CraftBukkit end */

    @Override
    public float a(BlockPosition blockposition, IWorldReader iworldreader) {
        return iworldreader.getType(blockposition.down()).a(Blocks.GRASS_BLOCK) ? 10.0F : iworldreader.z(blockposition) - 0.5F;
    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        super.saveData(nbttagcompound);
        nbttagcompound.setInt("InLove", this.inLove);
        if (this.loveCause != null) {
            nbttagcompound.a("LoveCause", this.loveCause);
        }

    }

    @Override
    public double bk() {
        return 0.14D;
    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        super.loadData(nbttagcompound);
        this.inLove = nbttagcompound.getInt("InLove");
        this.loveCause = nbttagcompound.b("LoveCause") ? nbttagcompound.a("LoveCause") : null;
    }

    public static boolean b(EntityTypes<? extends EntityAnimal> entitytypes, GeneratorAccess generatoraccess, EnumMobSpawn enummobspawn, BlockPosition blockposition, Random random) {
        return generatoraccess.getType(blockposition.down()).a(Blocks.GRASS_BLOCK) && generatoraccess.getLightLevel(blockposition, 0) > 8;
    }

    @Override
    public int J() {
        return 120;
    }

    @Override
    public boolean isTypeNotPersistent(double d0) {
        return false;
    }

    @Override
    protected int getExpValue(EntityHuman entityhuman) {
        return 1 + this.level.random.nextInt(3);
    }

    public boolean isBreedItem(ItemStack itemstack) {
        return itemstack.a(Items.WHEAT);
    }

    @Override
    public EnumInteractionResult b(EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);

        if (this.isBreedItem(itemstack)) {
            int i = this.getAge();

            if (!this.level.isClientSide && i == 0 && this.fz()) {
                this.a(entityhuman, enumhand, itemstack);
                this.g(entityhuman);
                this.a(GameEvent.MOB_INTERACT, this.cT());
                return EnumInteractionResult.SUCCESS;
            }

            if (this.isBaby()) {
                this.a(entityhuman, enumhand, itemstack);
                this.setAge((int) ((float) (-i / 20) * 0.1F), true);
                this.a(GameEvent.MOB_INTERACT, this.cT());
                return EnumInteractionResult.a(this.level.isClientSide);
            }

            if (this.level.isClientSide) {
                return EnumInteractionResult.CONSUME;
            }
        }

        return super.b(entityhuman, enumhand);
    }

    protected void a(EntityHuman entityhuman, EnumHand enumhand, ItemStack itemstack) {
        if (!entityhuman.getAbilities().instabuild) {
            itemstack.subtract(1);
        }

    }

    public boolean fz() {
        return this.inLove <= 0;
    }

    public void g(@Nullable EntityHuman entityhuman) {
        // CraftBukkit start
        EntityEnterLoveModeEvent entityEnterLoveModeEvent = CraftEventFactory.callEntityEnterLoveModeEvent(entityhuman, this, 600);
        if (entityEnterLoveModeEvent.isCancelled()) {
            return;
        }
        this.inLove = entityEnterLoveModeEvent.getTicksInLove();
        // CraftBukkit end
        if (entityhuman != null) {
            this.loveCause = entityhuman.getUniqueID();
        }
        this.breedItem = entityhuman.getInventory().getItemInHand(); // CraftBukkit

        this.level.broadcastEntityEffect(this, (byte) 18);
    }

    public void setLoveTicks(int i) {
        this.inLove = i;
    }

    public int fA() {
        return this.inLove;
    }

    @Nullable
    public EntityPlayer getBreedCause() {
        if (this.loveCause == null) {
            return null;
        } else {
            EntityHuman entityhuman = this.level.b(this.loveCause);

            return entityhuman instanceof EntityPlayer ? (EntityPlayer) entityhuman : null;
        }
    }

    public boolean isInLove() {
        return this.inLove > 0;
    }

    public void resetLove() {
        this.inLove = 0;
    }

    public boolean mate(EntityAnimal entityanimal) {
        return entityanimal == this ? false : (entityanimal.getClass() != this.getClass() ? false : this.isInLove() && entityanimal.isInLove());
    }

    public void a(WorldServer worldserver, EntityAnimal entityanimal) {
        EntityAgeable entityageable = this.createChild(worldserver, entityanimal);

        if (entityageable != null) {
            // CraftBukkit start - set persistence for tame animals
            if (entityageable instanceof EntityTameableAnimal && ((EntityTameableAnimal) entityageable).isTamed()) {
                entityageable.setPersistenceRequired(true);
            }
            // CraftBukkit end
            EntityPlayer entityplayer = this.getBreedCause();

            if (entityplayer == null && entityanimal.getBreedCause() != null) {
                entityplayer = entityanimal.getBreedCause();
            }
            // CraftBukkit start - call EntityBreedEvent
            entityageable.setBaby(true);
            entityageable.setPositionRotation(this.locX(), this.locY(), this.locZ(), 0.0F, 0.0F);
            int experience = this.getRandom().nextInt(7) + 1;
            org.bukkit.event.entity.EntityBreedEvent entityBreedEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityBreedEvent(entityageable, this, entityanimal, entityplayer, this.breedItem, experience);
            if (entityBreedEvent.isCancelled()) {
                return;
            }
            experience = entityBreedEvent.getExperience();
            // CraftBukkit end

            if (entityplayer != null) {
                entityplayer.a(StatisticList.ANIMALS_BRED);
                CriterionTriggers.BRED_ANIMALS.a(entityplayer, this, entityanimal, entityageable);
            }

            this.setAgeRaw(6000);
            entityanimal.setAgeRaw(6000);
            this.resetLove();
            entityanimal.resetLove();
            worldserver.addAllEntities(entityageable, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.BREEDING); // CraftBukkit - added SpawnReason
            worldserver.broadcastEntityEffect(this, (byte) 18);
            if (worldserver.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                // CraftBukkit start - use event experience
                if (experience > 0) {
                    worldserver.addEntity(new EntityExperienceOrb(worldserver, this.locX(), this.locY(), this.locZ(), experience));
                }
                // CraftBukkit end
            }

        }
    }

    @Override
    public void a(byte b0) {
        if (b0 == 18) {
            for (int i = 0; i < 7; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;

                this.level.addParticle(Particles.HEART, this.d(1.0D), this.da() + 0.5D, this.g(1.0D), d0, d1, d2);
            }
        } else {
            super.a(b0);
        }

    }
}
