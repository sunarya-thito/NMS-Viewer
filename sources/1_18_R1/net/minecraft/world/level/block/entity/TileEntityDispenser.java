package net.minecraft.world.level.block.entity;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerDispenser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;

// CraftBukkit start
import java.util.List;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
// CraftBukkit end

public class TileEntityDispenser extends TileEntityLootable {

    private static final Random RANDOM = new Random();
    public static final int CONTAINER_SIZE = 9;
    private NonNullList<ItemStack> items;

    // CraftBukkit start - add fields and methods
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    private int maxStack = MAX_STACK;

    public List<ItemStack> getContents() {
        return this.items;
    }

    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public int getMaxStackSize() {
        return maxStack;
    }

    public void setMaxStackSize(int size) {
        maxStack = size;
    }
    // CraftBukkit end

    protected TileEntityDispenser(TileEntityTypes<?> tileentitytypes, BlockPosition blockposition, IBlockData iblockdata) {
        super(tileentitytypes, blockposition, iblockdata);
        this.items = NonNullList.withSize(9, ItemStack.EMPTY);
    }

    public TileEntityDispenser(BlockPosition blockposition, IBlockData iblockdata) {
        this(TileEntityTypes.DISPENSER, blockposition, iblockdata);
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    public int getRandomSlot() {
        this.unpackLootTable((EntityHuman) null);
        int i = -1;
        int j = 1;

        for (int k = 0; k < this.items.size(); ++k) {
            if (!((ItemStack) this.items.get(k)).isEmpty() && TileEntityDispenser.RANDOM.nextInt(j++) == 0) {
                i = k;
            }
        }

        return i;
    }

    public int addItem(ItemStack itemstack) {
        for (int i = 0; i < this.items.size(); ++i) {
            if (((ItemStack) this.items.get(i)).isEmpty()) {
                this.setItem(i, itemstack);
                return i;
            }
        }

        return -1;
    }

    @Override
    protected IChatBaseComponent getDefaultName() {
        return new ChatMessage("container.dispenser");
    }

    @Override
    public void load(NBTTagCompound nbttagcompound) {
        super.load(nbttagcompound);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbttagcompound)) {
            ContainerUtil.loadAllItems(nbttagcompound, this.items);
        }

    }

    @Override
    protected void saveAdditional(NBTTagCompound nbttagcompound) {
        super.saveAdditional(nbttagcompound);
        if (!this.trySaveLootTable(nbttagcompound)) {
            ContainerUtil.saveAllItems(nbttagcompound, this.items);
        }

    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonnulllist) {
        this.items = nonnulllist;
    }

    @Override
    protected Container createMenu(int i, PlayerInventory playerinventory) {
        return new ContainerDispenser(i, playerinventory, this);
    }
}
