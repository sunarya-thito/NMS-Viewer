package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;

// CraftBukkit start
import net.minecraft.world.entity.player.PlayerInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
// CraftBukkit end

public class ContainerBeacon extends Container {

    private static final int PAYMENT_SLOT = 0;
    private static final int SLOT_COUNT = 1;
    private static final int DATA_COUNT = 3;
    private static final int INV_SLOT_START = 1;
    private static final int INV_SLOT_END = 28;
    private static final int USE_ROW_SLOT_START = 28;
    private static final int USE_ROW_SLOT_END = 37;
    private final IInventory beacon;
    private final ContainerBeacon.SlotBeacon paymentSlot;
    private final ContainerAccess access;
    private final IContainerProperties beaconData;
    // CraftBukkit start
    private CraftInventoryView bukkitEntity = null;
    private PlayerInventory player;
    // CraftBukkit end

    public ContainerBeacon(int i, IInventory iinventory) {
        this(i, iinventory, new ContainerProperties(3), ContainerAccess.NULL);
    }

    public ContainerBeacon(int i, IInventory iinventory, IContainerProperties icontainerproperties, ContainerAccess containeraccess) {
        super(Containers.BEACON, i);
        player = (PlayerInventory) iinventory; // CraftBukkit - TODO: check this
        this.beacon = new InventorySubcontainer(1) {
            @Override
            public boolean canPlaceItem(int j, ItemStack itemstack) {
                return itemstack.is((Tag) TagsItem.BEACON_PAYMENT_ITEMS);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        checkContainerDataCount(icontainerproperties, 3);
        this.beaconData = icontainerproperties;
        this.access = containeraccess;
        this.paymentSlot = new ContainerBeacon.SlotBeacon(this.beacon, 0, 136, 110);
        this.addSlot(this.paymentSlot);
        this.addDataSlots(icontainerproperties);
        boolean flag = true;
        boolean flag1 = true;

        int j;

        for (j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(iinventory, k + j * 9 + 9, 36 + k * 18, 137 + j * 18));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(iinventory, j, 36 + j * 18, 195));
        }

    }

    @Override
    public void removed(EntityHuman entityhuman) {
        super.removed(entityhuman);
        if (!entityhuman.level.isClientSide) {
            ItemStack itemstack = this.paymentSlot.remove(this.paymentSlot.getMaxStackSize());

            if (!itemstack.isEmpty()) {
                entityhuman.drop(itemstack, false);
            }

        }
    }

    @Override
    public boolean stillValid(EntityHuman entityhuman) {
        if (!this.checkReachable) return true; // CraftBukkit
        return stillValid(this.access, entityhuman, Blocks.BEACON);
    }

    @Override
    public void setData(int i, int j) {
        super.setData(i, j);
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(EntityHuman entityhuman, int i) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(i);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();

            itemstack = itemstack1.copy();
            if (i == 0) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.paymentSlot.hasItem() && this.paymentSlot.mayPlace(itemstack1) && itemstack1.getCount() == 1) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i >= 1 && i < 28) {
                if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i >= 28 && i < 37) {
                if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(entityhuman, itemstack1);
        }

        return itemstack;
    }

    public int getLevels() {
        return this.beaconData.get(0);
    }

    @Nullable
    public MobEffectList getPrimaryEffect() {
        return MobEffectList.byId(this.beaconData.get(1));
    }

    @Nullable
    public MobEffectList getSecondaryEffect() {
        return MobEffectList.byId(this.beaconData.get(2));
    }

    public void updateEffects(int i, int j) {
        if (this.paymentSlot.hasItem()) {
            this.beaconData.set(1, i);
            this.beaconData.set(2, j);
            this.paymentSlot.remove(1);
            this.access.execute(World::blockEntityChanged);
        }

    }

    public boolean hasPayment() {
        return !this.beacon.getItem(0).isEmpty();
    }

    private class SlotBeacon extends Slot {

        public SlotBeacon(IInventory iinventory, int i, int j, int k) {
            super(iinventory, i, j, k);
        }

        @Override
        public boolean mayPlace(ItemStack itemstack) {
            return itemstack.is((Tag) TagsItem.BEACON_PAYMENT_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // CraftBukkit start
    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        org.bukkit.craftbukkit.inventory.CraftInventory inventory = new org.bukkit.craftbukkit.inventory.CraftInventoryBeacon(this.beacon);
        bukkitEntity = new CraftInventoryView(this.player.player.getBukkitEntity(), inventory, this);
        return bukkitEntity;
    }
    // CraftBukkit end
}
