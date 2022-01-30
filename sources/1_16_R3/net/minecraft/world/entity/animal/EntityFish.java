package net.minecraft.world.entity.animal;

import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomSwim;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationGuardian;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

// CraftBukkit start
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerBucketFishEvent;
// CraftBukkit end

public abstract class EntityFish extends EntityWaterAnimal {

    private static final DataWatcherObject<Boolean> FROM_BUCKET = DataWatcher.a(EntityFish.class, DataWatcherRegistry.i);

    public EntityFish(EntityTypes<? extends EntityFish> entitytypes, World world) {
        super(entitytypes, world);
        this.moveController = new EntityFish.a(this);
    }

    @Override
    protected float b(EntityPose entitypose, EntitySize entitysize) {
        return entitysize.height * 0.65F;
    }

    public static AttributeProvider.Builder m() {
        return EntityInsentient.p().a(GenericAttributes.MAX_HEALTH, 3.0D);
    }

    @Override
    public boolean isSpecialPersistence() {
        return super.isSpecialPersistence() || this.isFromBucket();
    }

    public static boolean b(EntityTypes<? extends EntityFish> entitytypes, GeneratorAccess generatoraccess, EnumMobSpawn enummobspawn, BlockPosition blockposition, Random random) {
        return generatoraccess.getType(blockposition).a(Blocks.WATER) && generatoraccess.getType(blockposition.up()).a(Blocks.WATER);
    }

    @Override
    public boolean isTypeNotPersistent(double d0) {
        return true; // CraftBukkit
    }

    @Override
    public int getMaxSpawnGroup() {
        return 8;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.datawatcher.register(EntityFish.FROM_BUCKET, false);
    }

    public boolean isFromBucket() {
        return (Boolean) this.datawatcher.get(EntityFish.FROM_BUCKET);
    }

    public void setFromBucket(boolean flag) {
        this.datawatcher.set(EntityFish.FROM_BUCKET, flag);
        this.persistent = this.isPersistent(); // CraftBukkit - SPIGOT-4106 update persistence
    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        super.saveData(nbttagcompound);
        nbttagcompound.setBoolean("FromBucket", this.isFromBucket());
    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        super.loadData(nbttagcompound);
        this.setFromBucket(nbttagcompound.getBoolean("FromBucket"));
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.a(0, new PathfinderGoalPanic(this, 1.25D));
        PathfinderGoalSelector pathfindergoalselector = this.goalSelector;
        Predicate predicate = IEntitySelector.g;

        predicate.getClass();
        pathfindergoalselector.a(2, new PathfinderGoalAvoidTarget<>(this, EntityHuman.class, 8.0F, 1.6D, 1.4D, predicate::test));
        this.goalSelector.a(4, new EntityFish.b(this));
    }

    @Override
    protected NavigationAbstract b(World world) {
        return new NavigationGuardian(this, world);
    }

    @Override
    public void g(Vec3D vec3d) {
        if (this.doAITick() && this.isInWater()) {
            this.a(0.01F, vec3d);
            this.move(EnumMoveType.SELF, this.getMot());
            this.setMot(this.getMot().a(0.9D));
            if (this.getGoalTarget() == null) {
                this.setMot(this.getMot().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.g(vec3d);
        }

    }

    @Override
    public void movementTick() {
        if (!this.isInWater() && this.onGround && this.v) {
            this.setMot(this.getMot().add((double) ((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F), 0.4000000059604645D, (double) ((this.random.nextFloat() * 2.0F - 1.0F) * 0.05F)));
            this.onGround = false;
            this.impulse = true;
            this.playSound(this.getSoundFlop(), this.getSoundVolume(), this.dH());
        }

        super.movementTick();
    }

    @Override
    protected EnumInteractionResult b(EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);

        if (itemstack.getItem() == Items.WATER_BUCKET && this.isAlive()) {
            // CraftBukkit start
            ItemStack itemstack1 = this.eK();
            this.k(itemstack1);

            PlayerBucketFishEvent playerBucketFishEvent = CraftEventFactory.callPlayerFishBucketEvent(this, entityhuman, itemstack, itemstack1);
            itemstack1 = CraftItemStack.asNMSCopy(playerBucketFishEvent.getFishBucket());
            if (playerBucketFishEvent.isCancelled()) {
                ((EntityPlayer) entityhuman).updateInventory(((EntityPlayer) entityhuman).activeContainer); // We need to update inventory to resync client's bucket
                ((EntityPlayer) entityhuman).playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(this)); // We need to play out these packets as the client assumes the fish is gone
                ((EntityPlayer) entityhuman).playerConnection.sendPacket(new PacketPlayOutEntityMetadata(this.getId(), datawatcher, true)); // Need to send data such as the display name to client
                return EnumInteractionResult.FAIL;
            }
            // CraftBukkit end
            this.playSound(SoundEffects.ITEM_BUCKET_FILL_FISH, 1.0F, 1.0F);
            itemstack.subtract(1);
            // ItemStack itemstack1 = this.eK(); // CraftBukkit - moved up

            // this.k(itemstack1); // CraftBukkit - moved up
            if (!this.world.isClientSide) {
                CriterionTriggers.j.a((EntityPlayer) entityhuman, itemstack1);
            }

            if (itemstack.isEmpty()) {
                entityhuman.a(enumhand, itemstack1);
            } else if (!entityhuman.inventory.pickup(itemstack1)) {
                entityhuman.drop(itemstack1, false);
            }

            this.die();
            return EnumInteractionResult.a(this.world.isClientSide);
        } else {
            return super.b(entityhuman, enumhand);
        }
    }

    protected void k(ItemStack itemstack) {
        if (this.hasCustomName()) {
            itemstack.a(this.getCustomName());
        }

    }

    protected abstract ItemStack eK();

    protected boolean eL() {
        return true;
    }

    protected abstract SoundEffect getSoundFlop();

    @Override
    protected SoundEffect getSoundSwim() {
        return SoundEffects.ENTITY_FISH_SWIM;
    }

    @Override
    protected void b(BlockPosition blockposition, IBlockData iblockdata) {}

    static class a extends ControllerMove {

        private final EntityFish i;

        a(EntityFish entityfish) {
            super(entityfish);
            this.i = entityfish;
        }

        @Override
        public void a() {
            if (this.i.a((Tag) TagsFluid.WATER)) {
                this.i.setMot(this.i.getMot().add(0.0D, 0.005D, 0.0D));
            }

            if (this.h == ControllerMove.Operation.MOVE_TO && !this.i.getNavigation().m()) {
                float f = (float) (this.e * this.i.b(GenericAttributes.MOVEMENT_SPEED));

                this.i.q(MathHelper.g(0.125F, this.i.dN(), f));
                double d0 = this.b - this.i.locX();
                double d1 = this.c - this.i.locY();
                double d2 = this.d - this.i.locZ();

                if (d1 != 0.0D) {
                    double d3 = (double) MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

                    this.i.setMot(this.i.getMot().add(0.0D, (double) this.i.dN() * (d1 / d3) * 0.1D, 0.0D));
                }

                if (d0 != 0.0D || d2 != 0.0D) {
                    float f1 = (float) (MathHelper.d(d2, d0) * 57.2957763671875D) - 90.0F;

                    this.i.yaw = this.a(this.i.yaw, f1, 90.0F);
                    this.i.aA = this.i.yaw;
                }

            } else {
                this.i.q(0.0F);
            }
        }
    }

    static class b extends PathfinderGoalRandomSwim {

        private final EntityFish h;

        public b(EntityFish entityfish) {
            super(entityfish, 1.0D, 40);
            this.h = entityfish;
        }

        @Override
        public boolean a() {
            return this.h.eL() && super.a();
        }
    }
}
