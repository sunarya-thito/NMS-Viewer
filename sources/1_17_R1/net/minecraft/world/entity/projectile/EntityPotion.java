package net.minecraft.world.entity.projectile;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAreaEffectCloud;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;

// CraftBukkit start
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.LivingEntity;
// CraftBukkit end

public class EntityPotion extends EntityProjectileThrowable implements ItemSupplier {

    public static final double SPLASH_RANGE = 4.0D;
    private static final double SPLASH_RANGE_SQ = 16.0D;
    public static final Predicate<EntityLiving> WATER_SENSITIVE = EntityLiving::ex;

    public EntityPotion(EntityTypes<? extends EntityPotion> entitytypes, World world) {
        super(entitytypes, world);
    }

    public EntityPotion(World world, EntityLiving entityliving) {
        super(EntityTypes.POTION, entityliving, world);
    }

    public EntityPotion(World world, double d0, double d1, double d2) {
        super(EntityTypes.POTION, d0, d1, d2, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SPLASH_POTION;
    }

    @Override
    protected float l() {
        return 0.05F;
    }

    @Override
    protected void a(MovingObjectPositionBlock movingobjectpositionblock) {
        super.a(movingobjectpositionblock);
        if (!this.level.isClientSide) {
            ItemStack itemstack = this.getSuppliedItem();
            PotionRegistry potionregistry = PotionUtil.d(itemstack);
            List<MobEffect> list = PotionUtil.getEffects(itemstack);
            boolean flag = potionregistry == Potions.WATER && list.isEmpty();
            EnumDirection enumdirection = movingobjectpositionblock.getDirection();
            BlockPosition blockposition = movingobjectpositionblock.getBlockPosition();
            BlockPosition blockposition1 = blockposition.shift(enumdirection);

            if (flag) {
                this.a(blockposition1);
                this.a(blockposition1.shift(enumdirection.opposite()));
                Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

                while (iterator.hasNext()) {
                    EnumDirection enumdirection1 = (EnumDirection) iterator.next();

                    this.a(blockposition1.shift(enumdirection1));
                }
            }

        }
    }

    @Override
    protected void a(MovingObjectPosition movingobjectposition) {
        super.a(movingobjectposition);
        if (!this.level.isClientSide) {
            ItemStack itemstack = this.getSuppliedItem();
            PotionRegistry potionregistry = PotionUtil.d(itemstack);
            List<MobEffect> list = PotionUtil.getEffects(itemstack);
            boolean flag = potionregistry == Potions.WATER && list.isEmpty();

            if (flag) {
                this.splash();
            } else if (true || !list.isEmpty()) { // CraftBukkit - Call event even if no effects to apply
                if (this.isLingering()) {
                    this.a(itemstack, potionregistry);
                } else {
                    this.a(list, movingobjectposition.getType() == MovingObjectPosition.EnumMovingObjectType.ENTITY ? ((MovingObjectPositionEntity) movingobjectposition).getEntity() : null);
                }
            }

            int i = potionregistry.b() ? 2007 : 2002;

            this.level.triggerEffect(i, this.getChunkCoordinates(), PotionUtil.c(itemstack));
            this.die();
        }
    }

    private void splash() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox().grow(4.0D, 2.0D, 4.0D);
        List<EntityLiving> list = this.level.a(EntityLiving.class, axisalignedbb, EntityPotion.WATER_SENSITIVE);

        if (!list.isEmpty()) {
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityLiving entityliving = (EntityLiving) iterator.next();
                double d0 = this.f(entityliving);

                if (d0 < 16.0D && entityliving.ex()) {
                    entityliving.damageEntity(DamageSource.c(entityliving, this.getShooter()), 1.0F);
                }
            }
        }

        List<Axolotl> list1 = this.level.a(Axolotl.class, axisalignedbb);
        Iterator iterator1 = list1.iterator();

        while (iterator1.hasNext()) {
            Axolotl axolotl = (Axolotl) iterator1.next();

            axolotl.fw();
        }

    }

    private void a(List<MobEffect> list, @Nullable Entity entity) {
        AxisAlignedBB axisalignedbb = this.getBoundingBox().grow(4.0D, 2.0D, 4.0D);
        List<EntityLiving> list1 = this.level.a(EntityLiving.class, axisalignedbb);
        Map<LivingEntity, Double> affected = new HashMap<LivingEntity, Double>(); // CraftBukkit

        if (!list1.isEmpty()) {
            Entity entity1 = this.x();
            Iterator iterator = list1.iterator();

            while (iterator.hasNext()) {
                EntityLiving entityliving = (EntityLiving) iterator.next();

                if (entityliving.eQ()) {
                    double d0 = this.f(entityliving);

                    if (d0 < 16.0D) {
                        double d1 = 1.0D - Math.sqrt(d0) / 4.0D;

                        if (entityliving == entity) {
                            d1 = 1.0D;
                        }

                        // CraftBukkit start
                        affected.put((LivingEntity) entityliving.getBukkitEntity(), d1);
                    }
                }
            }
        }

        org.bukkit.event.entity.PotionSplashEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callPotionSplashEvent(this, affected);
        if (!event.isCancelled() && list != null && !list.isEmpty()) { // do not process effects if there are no effects to process
            Entity entity1 = this.x();
            for (LivingEntity victim : event.getAffectedEntities()) {
                if (!(victim instanceof CraftLivingEntity)) {
                    continue;
                }

                EntityLiving entityliving = ((CraftLivingEntity) victim).getHandle();
                double d1 = event.getIntensity(victim);
                // CraftBukkit end

                Iterator iterator1 = list.iterator();

                while (iterator1.hasNext()) {
                    MobEffect mobeffect = (MobEffect) iterator1.next();
                    MobEffectList mobeffectlist = mobeffect.getMobEffect();
                    // CraftBukkit start - Abide by PVP settings - for players only!
                    if (!this.level.pvpMode && this.getShooter() instanceof EntityPlayer && entityliving instanceof EntityPlayer && entityliving != this.getShooter()) {
                        int i = MobEffectList.getId(mobeffectlist);
                        // Block SLOWER_MOVEMENT, SLOWER_DIG, HARM, BLINDNESS, HUNGER, WEAKNESS and POISON potions
                        if (i == 2 || i == 4 || i == 7 || i == 15 || i == 17 || i == 18 || i == 19) {
                            continue;
                        }
                    }
                    // CraftBukkit end

                    if (mobeffectlist.isInstant()) {
                        mobeffectlist.applyInstantEffect(this, this.getShooter(), entityliving, mobeffect.getAmplifier(), d1);
                    } else {
                        int i = (int) (d1 * (double) mobeffect.getDuration() + 0.5D);

                        if (i > 20) {
                            entityliving.addEffect(new MobEffect(mobeffectlist, i, mobeffect.getAmplifier(), mobeffect.isAmbient(), mobeffect.isShowParticles()), entity1, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.POTION_SPLASH); // CraftBukkit
                        }
                    }
                }
            }
        }

    }

    private void a(ItemStack itemstack, PotionRegistry potionregistry) {
        EntityAreaEffectCloud entityareaeffectcloud = new EntityAreaEffectCloud(this.level, this.locX(), this.locY(), this.locZ());
        Entity entity = this.getShooter();

        if (entity instanceof EntityLiving) {
            entityareaeffectcloud.setSource((EntityLiving) entity);
        }

        entityareaeffectcloud.setRadius(3.0F);
        entityareaeffectcloud.setRadiusOnUse(-0.5F);
        entityareaeffectcloud.setWaitTime(10);
        entityareaeffectcloud.setRadiusPerTick(-entityareaeffectcloud.getRadius() / (float) entityareaeffectcloud.getDuration());
        entityareaeffectcloud.a(potionregistry);
        Iterator iterator = PotionUtil.b(itemstack).iterator();

        while (iterator.hasNext()) {
            MobEffect mobeffect = (MobEffect) iterator.next();

            entityareaeffectcloud.addEffect(new MobEffect(mobeffect));
        }

        NBTTagCompound nbttagcompound = itemstack.getTag();

        if (nbttagcompound != null && nbttagcompound.hasKeyOfType("CustomPotionColor", 99)) {
            entityareaeffectcloud.setColor(nbttagcompound.getInt("CustomPotionColor"));
        }

        // CraftBukkit start
        org.bukkit.event.entity.LingeringPotionSplashEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callLingeringPotionSplashEvent(this, entityareaeffectcloud);
        if (!(event.isCancelled() || entityareaeffectcloud.isRemoved())) {
            this.level.addEntity(entityareaeffectcloud);
        } else {
            entityareaeffectcloud.die();
        }
        // CraftBukkit end
    }

    public boolean isLingering() {
        return this.getSuppliedItem().a(Items.LINGERING_POTION);
    }

    private void a(BlockPosition blockposition) {
        IBlockData iblockdata = this.level.getType(blockposition);

        if (iblockdata.a((Tag) TagsBlock.FIRE)) {
            // CraftBukkit start
            if (!CraftEventFactory.callEntityChangeBlockEvent(this, blockposition, Blocks.AIR.getBlockData()).isCancelled()) {
                this.level.a(blockposition, false);
            }
            // CraftBukkit end
        } else if (AbstractCandleBlock.b(iblockdata)) {
            // CraftBukkit start
            if (!CraftEventFactory.callEntityChangeBlockEvent(this, blockposition, iblockdata.set(AbstractCandleBlock.LIT, false)).isCancelled()) {
                AbstractCandleBlock.a((EntityHuman) null, iblockdata, (GeneratorAccess) this.level, blockposition);
            }
            // CraftBukkit end
        } else if (BlockCampfire.g(iblockdata)) {
            // CraftBukkit start
            if (!CraftEventFactory.callEntityChangeBlockEvent(this, blockposition, iblockdata.set(BlockCampfire.LIT, false)).isCancelled()) {
                this.level.a((EntityHuman) null, 1009, blockposition, 0);
                BlockCampfire.a(this.getShooter(), (GeneratorAccess) this.level, blockposition, iblockdata);
                this.level.setTypeUpdate(blockposition, (IBlockData) iblockdata.set(BlockCampfire.LIT, false));
            }
            // CraftBukkit end
        }

    }
}
