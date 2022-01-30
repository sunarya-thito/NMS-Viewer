package net.minecraft.world.entity.item;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsFluid;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3D;

// CraftBukkit start
import net.minecraft.server.MinecraftServer;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
// CraftBukkit end

public class EntityItem extends Entity {

    private static final DataWatcherObject<ItemStack> DATA_ITEM = DataWatcher.defineId(EntityItem.class, DataWatcherRegistry.ITEM_STACK);
    private static final int LIFETIME = 6000;
    private static final int INFINITE_PICKUP_DELAY = 32767;
    private static final int INFINITE_LIFETIME = -32768;
    public int age;
    public int pickupDelay;
    private int health;
    @Nullable
    private UUID thrower;
    @Nullable
    private UUID owner;
    public final float bobOffs;
    private int lastTick = MinecraftServer.currentTick - 1; // CraftBukkit

    public EntityItem(EntityTypes<? extends EntityItem> entitytypes, World world) {
        super(entitytypes, world);
        this.health = 5;
        this.bobOffs = this.random.nextFloat() * 3.1415927F * 2.0F;
        this.setYRot(this.random.nextFloat() * 360.0F);
    }

    public EntityItem(World world, double d0, double d1, double d2, ItemStack itemstack) {
        this(world, d0, d1, d2, itemstack, world.random.nextDouble() * 0.2D - 0.1D, 0.2D, world.random.nextDouble() * 0.2D - 0.1D);
    }

    public EntityItem(World world, double d0, double d1, double d2, ItemStack itemstack, double d3, double d4, double d5) {
        this(EntityTypes.ITEM, world);
        this.setPos(d0, d1, d2);
        this.setDeltaMovement(d3, d4, d5);
        this.setItem(itemstack);
    }

    private EntityItem(EntityItem entityitem) {
        super(entityitem.getType(), entityitem.level);
        this.health = 5;
        this.setItem(entityitem.getItem().copy());
        this.copyPosition(entityitem);
        this.age = entityitem.age;
        this.bobOffs = entityitem.bobOffs;
    }

    @Override
    public boolean occludesVibrations() {
        return TagsItem.OCCLUDES_VIBRATION_SIGNALS.contains(this.getItem().getItem());
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(EntityItem.DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        if (this.getItem().isEmpty()) {
            this.discard();
        } else {
            super.tick();
            // CraftBukkit start - Use wall time for pickup and despawn timers
            int elapsedTicks = MinecraftServer.currentTick - this.lastTick;
            if (this.pickupDelay != 32767) this.pickupDelay -= elapsedTicks;
            if (this.age != -32768) this.age += elapsedTicks;
            this.lastTick = MinecraftServer.currentTick;
            // CraftBukkit end

            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            Vec3D vec3d = this.getDeltaMovement();
            float f = this.getEyeHeight() - 0.11111111F;

            if (this.isInWater() && this.getFluidHeight(TagsFluid.WATER) > (double) f) {
                this.setUnderwaterMovement();
            } else if (this.isInLava() && this.getFluidHeight(TagsFluid.LAVA) > (double) f) {
                this.setUnderLavaMovement();
            } else if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            }

            if (this.level.isClientSide) {
                this.noPhysics = false;
            } else {
                this.noPhysics = !this.level.noCollision(this, this.getBoundingBox().deflate(1.0E-7D));
                if (this.noPhysics) {
                    this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D, this.getZ());
                }
            }

            if (!this.onGround || this.getDeltaMovement().horizontalDistanceSqr() > 9.999999747378752E-6D || (this.tickCount + this.getId()) % 4 == 0) {
                this.move(EnumMoveType.SELF, this.getDeltaMovement());
                float f1 = 0.98F;

                if (this.onGround) {
                    f1 = this.level.getBlockState(new BlockPosition(this.getX(), this.getY() - 1.0D, this.getZ())).getBlock().getFriction() * 0.98F;
                }

                this.setDeltaMovement(this.getDeltaMovement().multiply((double) f1, 0.98D, (double) f1));
                if (this.onGround) {
                    Vec3D vec3d1 = this.getDeltaMovement();

                    if (vec3d1.y < 0.0D) {
                        this.setDeltaMovement(vec3d1.multiply(1.0D, -0.5D, 1.0D));
                    }
                }
            }

            boolean flag = MathHelper.floor(this.xo) != MathHelper.floor(this.getX()) || MathHelper.floor(this.yo) != MathHelper.floor(this.getY()) || MathHelper.floor(this.zo) != MathHelper.floor(this.getZ());
            int i = flag ? 2 : 40;

            if (this.tickCount % i == 0 && !this.level.isClientSide && this.isMergable()) {
                this.mergeWithNeighbours();
            }

            /* CraftBukkit start - moved up
            if (this.age != -32768) {
                ++this.age;
            }
            // CraftBukkit end */

            this.hasImpulse |= this.updateInWaterStateAndDoFluidPushing();
            if (!this.level.isClientSide) {
                double d0 = this.getDeltaMovement().subtract(vec3d).lengthSqr();

                if (d0 > 0.01D) {
                    this.hasImpulse = true;
                }
            }

            if (!this.level.isClientSide && this.age >= level.spigotConfig.itemDespawnRate) { // Spigot
                // CraftBukkit start - fire ItemDespawnEvent
                if (org.bukkit.craftbukkit.event.CraftEventFactory.callItemDespawnEvent(this).isCancelled()) {
                    this.age = 0;
                    return;
                }
                // CraftBukkit end
                this.discard();
            }

        }
    }

    // Spigot start - copied from above
    @Override
    public void inactiveTick() {
        // CraftBukkit start - Use wall time for pickup and despawn timers
        int elapsedTicks = MinecraftServer.currentTick - this.lastTick;
        if (this.pickupDelay != 32767) this.pickupDelay -= elapsedTicks;
        if (this.age != -32768) this.age += elapsedTicks;
        this.lastTick = MinecraftServer.currentTick;
        // CraftBukkit end

        if (!this.level.isClientSide && this.age >= level.spigotConfig.itemDespawnRate) { // Spigot
            // CraftBukkit start - fire ItemDespawnEvent
            if (org.bukkit.craftbukkit.event.CraftEventFactory.callItemDespawnEvent(this).isCancelled()) {
                this.age = 0;
                return;
            }
            // CraftBukkit end
            this.discard();
        }
    }
    // Spigot end

    private void setUnderwaterMovement() {
        Vec3D vec3d = this.getDeltaMovement();

        this.setDeltaMovement(vec3d.x * 0.9900000095367432D, vec3d.y + (double) (vec3d.y < 0.05999999865889549D ? 5.0E-4F : 0.0F), vec3d.z * 0.9900000095367432D);
    }

    private void setUnderLavaMovement() {
        Vec3D vec3d = this.getDeltaMovement();

        this.setDeltaMovement(vec3d.x * 0.949999988079071D, vec3d.y + (double) (vec3d.y < 0.05999999865889549D ? 5.0E-4F : 0.0F), vec3d.z * 0.949999988079071D);
    }

    private void mergeWithNeighbours() {
        if (this.isMergable()) {
            // Spigot start
            double radius = level.spigotConfig.itemMerge;
            List<EntityItem> list = this.level.getEntitiesOfClass(EntityItem.class, this.getBoundingBox().inflate(radius, radius, radius), (entityitem) -> {
                // Spigot end
                return entityitem != this && entityitem.isMergable();
            });
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityItem entityitem = (EntityItem) iterator.next();

                if (entityitem.isMergable()) {
                    this.tryToMerge(entityitem);
                    if (this.isRemoved()) {
                        break;
                    }
                }
            }

        }
    }

    private boolean isMergable() {
        ItemStack itemstack = this.getItem();

        return this.isAlive() && this.pickupDelay != 32767 && this.age != -32768 && this.age < 6000 && itemstack.getCount() < itemstack.getMaxStackSize();
    }

    private void tryToMerge(EntityItem entityitem) {
        ItemStack itemstack = this.getItem();
        ItemStack itemstack1 = entityitem.getItem();

        if (Objects.equals(this.getOwner(), entityitem.getOwner()) && areMergable(itemstack, itemstack1)) {
            if (true || itemstack1.getCount() < itemstack.getCount()) { // Spigot
                merge(this, itemstack, entityitem, itemstack1);
            } else {
                merge(entityitem, itemstack1, this, itemstack);
            }

        }
    }

    public static boolean areMergable(ItemStack itemstack, ItemStack itemstack1) {
        return !itemstack1.is(itemstack.getItem()) ? false : (itemstack1.getCount() + itemstack.getCount() > itemstack1.getMaxStackSize() ? false : (itemstack1.hasTag() ^ itemstack.hasTag() ? false : !itemstack1.hasTag() || itemstack1.getTag().equals(itemstack.getTag())));
    }

    public static ItemStack merge(ItemStack itemstack, ItemStack itemstack1, int i) {
        int j = Math.min(Math.min(itemstack.getMaxStackSize(), i) - itemstack.getCount(), itemstack1.getCount());
        ItemStack itemstack2 = itemstack.copy();

        itemstack2.grow(j);
        itemstack1.shrink(j);
        return itemstack2;
    }

    private static void merge(EntityItem entityitem, ItemStack itemstack, ItemStack itemstack1) {
        ItemStack itemstack2 = merge(itemstack, itemstack1, 64);

        if (!itemstack2.isEmpty()) entityitem.setItem(itemstack2); // CraftBukkit - don't set empty stacks
    }

    private static void merge(EntityItem entityitem, ItemStack itemstack, EntityItem entityitem1, ItemStack itemstack1) {
        if (org.bukkit.craftbukkit.event.CraftEventFactory.callItemMergeEvent(entityitem1, entityitem).isCancelled()) return; // CraftBukkit
        merge(entityitem, itemstack, itemstack1);
        entityitem.pickupDelay = Math.max(entityitem.pickupDelay, entityitem1.pickupDelay);
        entityitem.age = Math.min(entityitem.age, entityitem1.age);
        if (itemstack1.isEmpty()) {
            entityitem1.discard();
        }

    }

    @Override
    public boolean fireImmune() {
        return this.getItem().getItem().isFireResistant() || super.fireImmune();
    }

    @Override
    public boolean hurt(DamageSource damagesource, float f) {
        if (this.isInvulnerableTo(damagesource)) {
            return false;
        } else if (!this.getItem().isEmpty() && this.getItem().is(Items.NETHER_STAR) && damagesource.isExplosion()) {
            return false;
        } else if (!this.getItem().getItem().canBeHurtBy(damagesource)) {
            return false;
        } else {
            // CraftBukkit start
            if (org.bukkit.craftbukkit.event.CraftEventFactory.handleNonLivingEntityDamageEvent(this, damagesource, f)) {
                return false;
            }
            // CraftBukkit end
            this.markHurt();
            this.health = (int) ((float) this.health - f);
            this.gameEvent(GameEvent.ENTITY_DAMAGED, damagesource.getEntity());
            if (this.health <= 0) {
                this.getItem().onDestroyed(this);
                this.discard();
            }

            return true;
        }
    }

    @Override
    public void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        nbttagcompound.putShort("Health", (short) this.health);
        nbttagcompound.putShort("Age", (short) this.age);
        nbttagcompound.putShort("PickupDelay", (short) this.pickupDelay);
        if (this.getThrower() != null) {
            nbttagcompound.putUUID("Thrower", this.getThrower());
        }

        if (this.getOwner() != null) {
            nbttagcompound.putUUID("Owner", this.getOwner());
        }

        if (!this.getItem().isEmpty()) {
            nbttagcompound.put("Item", this.getItem().save(new NBTTagCompound()));
        }

    }

    @Override
    public void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        this.health = nbttagcompound.getShort("Health");
        this.age = nbttagcompound.getShort("Age");
        if (nbttagcompound.contains("PickupDelay")) {
            this.pickupDelay = nbttagcompound.getShort("PickupDelay");
        }

        if (nbttagcompound.hasUUID("Owner")) {
            this.owner = nbttagcompound.getUUID("Owner");
        }

        if (nbttagcompound.hasUUID("Thrower")) {
            this.thrower = nbttagcompound.getUUID("Thrower");
        }

        NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("Item");

        this.setItem(ItemStack.of(nbttagcompound1));
        if (this.getItem().isEmpty()) {
            this.discard();
        }

    }

    @Override
    public void playerTouch(EntityHuman entityhuman) {
        if (!this.level.isClientSide) {
            ItemStack itemstack = this.getItem();
            Item item = itemstack.getItem();
            int i = itemstack.getCount();

            // CraftBukkit start - fire PlayerPickupItemEvent
            int canHold = entityhuman.getInventory().canHold(itemstack);
            int remaining = i - canHold;

            if (this.pickupDelay <= 0 && canHold > 0) {
                itemstack.setCount(canHold);
                // Call legacy event
                PlayerPickupItemEvent playerEvent = new PlayerPickupItemEvent((org.bukkit.entity.Player) entityhuman.getBukkitEntity(), (org.bukkit.entity.Item) this.getBukkitEntity(), remaining);
                playerEvent.setCancelled(!playerEvent.getPlayer().getCanPickupItems());
                this.level.getCraftServer().getPluginManager().callEvent(playerEvent);
                if (playerEvent.isCancelled()) {
                    itemstack.setCount(i); // SPIGOT-5294 - restore count
                    return;
                }

                // Call newer event afterwards
                EntityPickupItemEvent entityEvent = new EntityPickupItemEvent((org.bukkit.entity.Player) entityhuman.getBukkitEntity(), (org.bukkit.entity.Item) this.getBukkitEntity(), remaining);
                entityEvent.setCancelled(!entityEvent.getEntity().getCanPickupItems());
                this.level.getCraftServer().getPluginManager().callEvent(entityEvent);
                if (entityEvent.isCancelled()) {
                    itemstack.setCount(i); // SPIGOT-5294 - restore count
                    return;
                }

                // Update the ItemStack if it was changed in the event
                ItemStack current = this.getItem();
                if (!itemstack.equals(current)) {
                    itemstack = current;
                } else {
                    itemstack.setCount(canHold + remaining); // = i
                }

                // Possibly < 0; fix here so we do not have to modify code below
                this.pickupDelay = 0;
            } else if (this.pickupDelay == 0) {
                // ensure that the code below isn't triggered if canHold says we can't pick the items up
                this.pickupDelay = -1;
            }
            // CraftBukkit end

            if (this.pickupDelay == 0 && (this.owner == null || this.owner.equals(entityhuman.getUUID())) && entityhuman.getInventory().add(itemstack)) {
                entityhuman.take(this, i);
                if (itemstack.isEmpty()) {
                    this.discard();
                    itemstack.setCount(i);
                }

                entityhuman.awardStat(StatisticList.ITEM_PICKED_UP.get(item), i);
                entityhuman.onItemPickup(this);
            }

        }
    }

    @Override
    public IChatBaseComponent getName() {
        IChatBaseComponent ichatbasecomponent = this.getCustomName();

        return (IChatBaseComponent) (ichatbasecomponent != null ? ichatbasecomponent : new ChatMessage(this.getItem().getDescriptionId()));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Nullable
    @Override
    public Entity changeDimension(WorldServer worldserver) {
        Entity entity = super.changeDimension(worldserver);

        if (!this.level.isClientSide && entity instanceof EntityItem) {
            ((EntityItem) entity).mergeWithNeighbours();
        }

        return entity;
    }

    public ItemStack getItem() {
        return (ItemStack) this.getEntityData().get(EntityItem.DATA_ITEM);
    }

    public void setItem(ItemStack itemstack) {
        com.google.common.base.Preconditions.checkArgument(!itemstack.isEmpty(), "Cannot drop air"); // CraftBukkit
        this.getEntityData().set(EntityItem.DATA_ITEM, itemstack);
        this.getEntityData().markDirty(EntityItem.DATA_ITEM); // CraftBukkit - SPIGOT-4591, must mark dirty
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> datawatcherobject) {
        super.onSyncedDataUpdated(datawatcherobject);
        if (EntityItem.DATA_ITEM.equals(datawatcherobject)) {
            this.getItem().setEntityRepresentation(this);
        }

    }

    @Nullable
    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(@Nullable UUID uuid) {
        this.owner = uuid;
    }

    @Nullable
    public UUID getThrower() {
        return this.thrower;
    }

    public void setThrower(@Nullable UUID uuid) {
        this.thrower = uuid;
    }

    public int getAge() {
        return this.age;
    }

    public void setDefaultPickUpDelay() {
        this.pickupDelay = 10;
    }

    public void setNoPickUpDelay() {
        this.pickupDelay = 0;
    }

    public void setNeverPickUp() {
        this.pickupDelay = 32767;
    }

    public void setPickUpDelay(int i) {
        this.pickupDelay = i;
    }

    public boolean hasPickUpDelay() {
        return this.pickupDelay > 0;
    }

    public void setUnlimitedLifetime() {
        this.age = -32768;
    }

    public void setExtendedLifetime() {
        this.age = -6000;
    }

    public void makeFakeItem() {
        this.setNeverPickUp();
        this.age = level.spigotConfig.itemDespawnRate - 1; // Spigot
    }

    public float getSpin(float f) {
        return ((float) this.getAge() + f) / 20.0F + this.bobOffs;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }

    public EntityItem copy() {
        return new EntityItem(this);
    }

    @Override
    public SoundCategory getSoundSource() {
        return SoundCategory.AMBIENT;
    }
}
