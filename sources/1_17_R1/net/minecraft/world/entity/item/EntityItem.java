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
import net.minecraft.tags.Tag;
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

    private static final DataWatcherObject<ItemStack> DATA_ITEM = DataWatcher.a(EntityItem.class, DataWatcherRegistry.ITEM_STACK);
    private static final int LIFETIME = 6000;
    private static final int INFINITE_PICKUP_DELAY = 32767;
    private static final int INFINITE_LIFETIME = -32768;
    public int age;
    public int pickupDelay;
    private int health;
    private UUID thrower;
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
        this.setPosition(d0, d1, d2);
        this.setMot(d3, d4, d5);
        this.setItemStack(itemstack);
    }

    private EntityItem(EntityItem entityitem) {
        super(entityitem.getEntityType(), entityitem.level);
        this.health = 5;
        this.setItemStack(entityitem.getItemStack().cloneItemStack());
        this.s(entityitem);
        this.age = entityitem.age;
        this.bobOffs = entityitem.bobOffs;
    }

    @Override
    public boolean aJ() {
        return TagsItem.OCCLUDES_VIBRATION_SIGNALS.isTagged(this.getItemStack().getItem());
    }

    @Override
    protected Entity.MovementEmission aI() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(EntityItem.DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        if (this.getItemStack().isEmpty()) {
            this.die();
        } else {
            super.tick();
            // CraftBukkit start - Use wall time for pickup and despawn timers
            int elapsedTicks = MinecraftServer.currentTick - this.lastTick;
            if (this.pickupDelay != 32767) this.pickupDelay -= elapsedTicks;
            if (this.age != -32768) this.age += elapsedTicks;
            this.lastTick = MinecraftServer.currentTick;
            // CraftBukkit end

            this.xo = this.locX();
            this.yo = this.locY();
            this.zo = this.locZ();
            Vec3D vec3d = this.getMot();
            float f = this.getHeadHeight() - 0.11111111F;

            if (this.isInWater() && this.b((Tag) TagsFluid.WATER) > (double) f) {
                this.w();
            } else if (this.aX() && this.b((Tag) TagsFluid.LAVA) > (double) f) {
                this.x();
            } else if (!this.isNoGravity()) {
                this.setMot(this.getMot().add(0.0D, -0.04D, 0.0D));
            }

            if (this.level.isClientSide) {
                this.noPhysics = false;
            } else {
                this.noPhysics = !this.level.b((Entity) this, this.getBoundingBox().shrink(1.0E-7D), (entity) -> {
                    return true;
                });
                if (this.noPhysics) {
                    this.l(this.locX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D, this.locZ());
                }
            }

            if (!this.onGround || this.getMot().i() > 9.999999747378752E-6D || (this.tickCount + this.getId()) % 4 == 0) {
                this.move(EnumMoveType.SELF, this.getMot());
                float f1 = 0.98F;

                if (this.onGround) {
                    f1 = this.level.getType(new BlockPosition(this.locX(), this.locY() - 1.0D, this.locZ())).getBlock().getFrictionFactor() * 0.98F;
                }

                this.setMot(this.getMot().d((double) f1, 0.98D, (double) f1));
                if (this.onGround) {
                    Vec3D vec3d1 = this.getMot();

                    if (vec3d1.y < 0.0D) {
                        this.setMot(vec3d1.d(1.0D, -0.5D, 1.0D));
                    }
                }
            }

            boolean flag = MathHelper.floor(this.xo) != MathHelper.floor(this.locX()) || MathHelper.floor(this.yo) != MathHelper.floor(this.locY()) || MathHelper.floor(this.zo) != MathHelper.floor(this.locZ());
            int i = flag ? 2 : 40;

            if (this.tickCount % i == 0 && !this.level.isClientSide && this.A()) {
                this.mergeNearby();
            }

            /* CraftBukkit start - moved up
            if (this.age != -32768) {
                ++this.age;
            }
            // CraftBukkit end */

            this.hasImpulse |= this.aR();
            if (!this.level.isClientSide) {
                double d0 = this.getMot().d(vec3d).g();

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
                this.die();
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
            this.die();
        }
    }
    // Spigot end

    private void w() {
        Vec3D vec3d = this.getMot();

        this.setMot(vec3d.x * 0.9900000095367432D, vec3d.y + (double) (vec3d.y < 0.05999999865889549D ? 5.0E-4F : 0.0F), vec3d.z * 0.9900000095367432D);
    }

    private void x() {
        Vec3D vec3d = this.getMot();

        this.setMot(vec3d.x * 0.949999988079071D, vec3d.y + (double) (vec3d.y < 0.05999999865889549D ? 5.0E-4F : 0.0F), vec3d.z * 0.949999988079071D);
    }

    private void mergeNearby() {
        if (this.A()) {
            // Spigot start
            double radius = level.spigotConfig.itemMerge;
            List<EntityItem> list = this.level.a(EntityItem.class, this.getBoundingBox().grow(radius, radius, radius), (entityitem) -> {
                // Spigot end
                return entityitem != this && entityitem.A();
            });
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityItem entityitem = (EntityItem) iterator.next();

                if (entityitem.A()) {
                    this.a(entityitem);
                    if (this.isRemoved()) {
                        break;
                    }
                }
            }

        }
    }

    private boolean A() {
        ItemStack itemstack = this.getItemStack();

        return this.isAlive() && this.pickupDelay != 32767 && this.age != -32768 && this.age < 6000 && itemstack.getCount() < itemstack.getMaxStackSize();
    }

    private void a(EntityItem entityitem) {
        ItemStack itemstack = this.getItemStack();
        ItemStack itemstack1 = entityitem.getItemStack();

        if (Objects.equals(this.getOwner(), entityitem.getOwner()) && a(itemstack, itemstack1)) {
            if (true || itemstack1.getCount() < itemstack.getCount()) { // Spigot
                a(this, itemstack, entityitem, itemstack1);
            } else {
                a(entityitem, itemstack1, this, itemstack);
            }

        }
    }

    public static boolean a(ItemStack itemstack, ItemStack itemstack1) {
        return !itemstack1.a(itemstack.getItem()) ? false : (itemstack1.getCount() + itemstack.getCount() > itemstack1.getMaxStackSize() ? false : (itemstack1.hasTag() ^ itemstack.hasTag() ? false : !itemstack1.hasTag() || itemstack1.getTag().equals(itemstack.getTag())));
    }

    public static ItemStack a(ItemStack itemstack, ItemStack itemstack1, int i) {
        int j = Math.min(Math.min(itemstack.getMaxStackSize(), i) - itemstack.getCount(), itemstack1.getCount());
        ItemStack itemstack2 = itemstack.cloneItemStack();

        itemstack2.add(j);
        itemstack1.subtract(j);
        return itemstack2;
    }

    private static void a(EntityItem entityitem, ItemStack itemstack, ItemStack itemstack1) {
        ItemStack itemstack2 = a(itemstack, itemstack1, 64);

        if (!itemstack2.isEmpty()) entityitem.setItemStack(itemstack2); // CraftBukkit - don't set empty stacks
    }

    private static void a(EntityItem entityitem, ItemStack itemstack, EntityItem entityitem1, ItemStack itemstack1) {
        if (org.bukkit.craftbukkit.event.CraftEventFactory.callItemMergeEvent(entityitem1, entityitem).isCancelled()) return; // CraftBukkit
        a(entityitem, itemstack, itemstack1);
        entityitem.pickupDelay = Math.max(entityitem.pickupDelay, entityitem1.pickupDelay);
        entityitem.age = Math.min(entityitem.age, entityitem1.age);
        if (itemstack1.isEmpty()) {
            entityitem1.die();
        }

    }

    @Override
    public boolean isFireProof() {
        return this.getItemStack().getItem().w() || super.isFireProof();
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (this.isInvulnerable(damagesource)) {
            return false;
        } else if (!this.getItemStack().isEmpty() && this.getItemStack().a(Items.NETHER_STAR) && damagesource.isExplosion()) {
            return false;
        } else if (!this.getItemStack().getItem().a(damagesource)) {
            return false;
        } else {
            // CraftBukkit start
            if (org.bukkit.craftbukkit.event.CraftEventFactory.handleNonLivingEntityDamageEvent(this, damagesource, f)) {
                return false;
            }
            // CraftBukkit end
            this.velocityChanged();
            this.health = (int) ((float) this.health - f);
            this.a(GameEvent.ENTITY_DAMAGED, damagesource.getEntity());
            if (this.health <= 0) {
                this.getItemStack().a(this);
                this.die();
            }

            return true;
        }
    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        nbttagcompound.setShort("Health", (short) this.health);
        nbttagcompound.setShort("Age", (short) this.age);
        nbttagcompound.setShort("PickupDelay", (short) this.pickupDelay);
        if (this.getThrower() != null) {
            nbttagcompound.a("Thrower", this.getThrower());
        }

        if (this.getOwner() != null) {
            nbttagcompound.a("Owner", this.getOwner());
        }

        if (!this.getItemStack().isEmpty()) {
            nbttagcompound.set("Item", this.getItemStack().save(new NBTTagCompound()));
        }

    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        this.health = nbttagcompound.getShort("Health");
        this.age = nbttagcompound.getShort("Age");
        if (nbttagcompound.hasKey("PickupDelay")) {
            this.pickupDelay = nbttagcompound.getShort("PickupDelay");
        }

        if (nbttagcompound.b("Owner")) {
            this.owner = nbttagcompound.a("Owner");
        }

        if (nbttagcompound.b("Thrower")) {
            this.thrower = nbttagcompound.a("Thrower");
        }

        NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("Item");

        this.setItemStack(ItemStack.a(nbttagcompound1));
        if (this.getItemStack().isEmpty()) {
            this.die();
        }

    }

    @Override
    public void pickup(EntityHuman entityhuman) {
        if (!this.level.isClientSide) {
            ItemStack itemstack = this.getItemStack();
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
                ItemStack current = this.getItemStack();
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

            if (this.pickupDelay == 0 && (this.owner == null || this.owner.equals(entityhuman.getUniqueID())) && entityhuman.getInventory().pickup(itemstack)) {
                entityhuman.receive(this, i);
                if (itemstack.isEmpty()) {
                    this.die();
                    itemstack.setCount(i);
                }

                entityhuman.a(StatisticList.ITEM_PICKED_UP.b(item), i);
                entityhuman.a(this);
            }

        }
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        IChatBaseComponent ichatbasecomponent = this.getCustomName();

        return (IChatBaseComponent) (ichatbasecomponent != null ? ichatbasecomponent : new ChatMessage(this.getItemStack().n()));
    }

    @Override
    public boolean ca() {
        return false;
    }

    @Nullable
    @Override
    public Entity b(WorldServer worldserver) {
        Entity entity = super.b(worldserver);

        if (!this.level.isClientSide && entity instanceof EntityItem) {
            ((EntityItem) entity).mergeNearby();
        }

        return entity;
    }

    public ItemStack getItemStack() {
        return (ItemStack) this.getDataWatcher().get(EntityItem.DATA_ITEM);
    }

    public void setItemStack(ItemStack itemstack) {
        com.google.common.base.Preconditions.checkArgument(!itemstack.isEmpty(), "Cannot drop air"); // CraftBukkit
        this.getDataWatcher().set(EntityItem.DATA_ITEM, itemstack);
        this.getDataWatcher().markDirty(EntityItem.DATA_ITEM); // CraftBukkit - SPIGOT-4591, must mark dirty
    }

    @Override
    public void a(DataWatcherObject<?> datawatcherobject) {
        super.a(datawatcherobject);
        if (EntityItem.DATA_ITEM.equals(datawatcherobject)) {
            this.getItemStack().a((Entity) this);
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

    public int l() {
        return this.age;
    }

    public void defaultPickupDelay() {
        this.pickupDelay = 10;
    }

    public void o() {
        this.pickupDelay = 0;
    }

    public void p() {
        this.pickupDelay = 32767;
    }

    public void setPickupDelay(int i) {
        this.pickupDelay = i;
    }

    public boolean q() {
        return this.pickupDelay > 0;
    }

    public void r() {
        this.age = -32768;
    }

    public void s() {
        this.age = -6000;
    }

    public void t() {
        this.p();
        this.age = level.spigotConfig.itemDespawnRate - 1; // Spigot
    }

    public float a(float f) {
        return ((float) this.l() + f) / 20.0F + this.bobOffs;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }

    public EntityItem v() {
        return new EntityItem(this);
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.AMBIENT;
    }
}
