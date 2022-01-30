package net.minecraft.world.entity.projectile;

import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

public abstract class EntityProjectileThrowable extends EntityProjectile implements ItemSupplier {

    private static final DataWatcherObject<ItemStack> DATA_ITEM_STACK = DataWatcher.a(EntityProjectileThrowable.class, DataWatcherRegistry.ITEM_STACK);

    public EntityProjectileThrowable(EntityTypes<? extends EntityProjectileThrowable> entitytypes, World world) {
        super(entitytypes, world);
    }

    public EntityProjectileThrowable(EntityTypes<? extends EntityProjectileThrowable> entitytypes, double d0, double d1, double d2, World world) {
        super(entitytypes, d0, d1, d2, world);
    }

    public EntityProjectileThrowable(EntityTypes<? extends EntityProjectileThrowable> entitytypes, EntityLiving entityliving, World world) {
        super(entitytypes, entityliving, world);
    }

    public void setItem(ItemStack itemstack) {
        if (!itemstack.a(this.getDefaultItem()) || itemstack.hasTag()) {
            this.getDataWatcher().set(EntityProjectileThrowable.DATA_ITEM_STACK, (ItemStack) SystemUtils.a(itemstack.cloneItemStack(), (itemstack1) -> { // CraftBukkit - decompile error
                if (!itemstack1.isEmpty()) itemstack1.setCount(1); // CraftBukkit
            }));
        }

    }

    protected abstract Item getDefaultItem();

    // CraftBukkit start
    public Item getDefaultItemPublic() {
        return getDefaultItem();
    }
    // CraftBukkit end

    public ItemStack getItem() {
        return (ItemStack) this.getDataWatcher().get(EntityProjectileThrowable.DATA_ITEM_STACK);
    }

    @Override
    public ItemStack getSuppliedItem() {
        ItemStack itemstack = this.getItem();

        return itemstack.isEmpty() ? new ItemStack(this.getDefaultItem()) : itemstack;
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(EntityProjectileThrowable.DATA_ITEM_STACK, ItemStack.EMPTY);
    }

    @Override
    public void saveData(NBTTagCompound nbttagcompound) {
        super.saveData(nbttagcompound);
        ItemStack itemstack = this.getItem();

        if (!itemstack.isEmpty()) {
            nbttagcompound.set("Item", itemstack.save(new NBTTagCompound()));
        }

    }

    @Override
    public void loadData(NBTTagCompound nbttagcompound) {
        super.loadData(nbttagcompound);
        ItemStack itemstack = ItemStack.a(nbttagcompound.getCompound("Item"));

        this.setItem(itemstack);
    }
}
