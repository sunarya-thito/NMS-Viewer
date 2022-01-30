package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Clearable;
import net.minecraft.world.IInventory;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerLectern;
import net.minecraft.world.inventory.IContainerProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWrittenBook;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BlockLectern;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

// CraftBukkit start
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Lectern;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
// CraftBukkit end

public class TileEntityLectern extends TileEntity implements Clearable, ITileInventory, ICommandListener { // CraftBukkit - ICommandListener

    public static final int DATA_PAGE = 0;
    public static final int NUM_DATA = 1;
    public static final int SLOT_BOOK = 0;
    public static final int NUM_SLOTS = 1;
    // CraftBukkit start - add fields and methods
    public final IInventory bookAccess = new LecternInventory();
    public class LecternInventory implements IInventory {

        public List<HumanEntity> transaction = new ArrayList<>();
        private int maxStack = 1;

        @Override
        public List<ItemStack> getContents() {
            return Arrays.asList(book);
        }

        @Override
        public void onOpen(CraftHumanEntity who) {
            transaction.add(who);
        }

        @Override
        public void onClose(CraftHumanEntity who) {
            transaction.remove(who);
        }

        @Override
        public List<HumanEntity> getViewers() {
            return transaction;
        }

        @Override
        public void setMaxStackSize(int i) {
            maxStack = i;
        }

        @Override
        public Location getLocation() {
            return new Location(level.getWorld(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        }

        @Override
        public InventoryHolder getOwner() {
            return (Lectern) TileEntityLectern.this.getOwner();
        }

        public TileEntityLectern getLectern() {
            return TileEntityLectern.this;
        }
        // CraftBukkit end

        @Override
        public int getSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return TileEntityLectern.this.book.isEmpty();
        }

        @Override
        public ItemStack getItem(int i) {
            return i == 0 ? TileEntityLectern.this.book : ItemStack.EMPTY;
        }

        @Override
        public ItemStack splitStack(int i, int j) {
            if (i == 0) {
                ItemStack itemstack = TileEntityLectern.this.book.cloneAndSubtract(j);

                if (TileEntityLectern.this.book.isEmpty()) {
                    TileEntityLectern.this.j();
                }

                return itemstack;
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public ItemStack splitWithoutUpdate(int i) {
            if (i == 0) {
                ItemStack itemstack = TileEntityLectern.this.book;

                TileEntityLectern.this.book = ItemStack.EMPTY;
                TileEntityLectern.this.j();
                return itemstack;
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        // CraftBukkit start
        public void setItem(int i, ItemStack itemstack) {
            if (i == 0) {
                TileEntityLectern.this.setBook(itemstack);
                if (TileEntityLectern.this.getWorld() != null) {
                    BlockLectern.setHasBook(TileEntityLectern.this.getWorld(), TileEntityLectern.this.getPosition(), TileEntityLectern.this.getBlock(), TileEntityLectern.this.hasBook());
                }
            }
        }
        // CraftBukkit end

        @Override
        public int getMaxStackSize() {
            return maxStack; // CraftBukkit
        }

        @Override
        public void update() {
            TileEntityLectern.this.update();
        }

        @Override
        public boolean a(EntityHuman entityhuman) {
            return TileEntityLectern.this.level.getTileEntity(TileEntityLectern.this.worldPosition) != TileEntityLectern.this ? false : (entityhuman.h((double) TileEntityLectern.this.worldPosition.getX() + 0.5D, (double) TileEntityLectern.this.worldPosition.getY() + 0.5D, (double) TileEntityLectern.this.worldPosition.getZ() + 0.5D) > 64.0D ? false : TileEntityLectern.this.hasBook());
        }

        @Override
        public boolean b(int i, ItemStack itemstack) {
            return false;
        }

        @Override
        public void clear() {}
    };
    private final IContainerProperties dataAccess = new IContainerProperties() {
        @Override
        public int getProperty(int i) {
            return i == 0 ? TileEntityLectern.this.page : 0;
        }

        @Override
        public void setProperty(int i, int j) {
            if (i == 0) {
                TileEntityLectern.this.setPage(j);
            }

        }

        @Override
        public int a() {
            return 1;
        }
    };
    ItemStack book;
    int page;
    private int pageCount;

    public TileEntityLectern(BlockPosition blockposition, IBlockData iblockdata) {
        super(TileEntityTypes.LECTERN, blockposition, iblockdata);
        this.book = ItemStack.EMPTY;
    }

    public ItemStack getBook() {
        return this.book;
    }

    public boolean hasBook() {
        return this.book.a(Items.WRITABLE_BOOK) || this.book.a(Items.WRITTEN_BOOK);
    }

    public void setBook(ItemStack itemstack) {
        this.a(itemstack, (EntityHuman) null);
    }

    void j() {
        this.page = 0;
        this.pageCount = 0;
        BlockLectern.setHasBook(this.getWorld(), this.getPosition(), this.getBlock(), false);
    }

    public void a(ItemStack itemstack, @Nullable EntityHuman entityhuman) {
        this.book = this.b(itemstack, entityhuman);
        this.page = 0;
        this.pageCount = ItemWrittenBook.k(this.book);
        this.update();
    }

    public void setPage(int i) {
        int j = MathHelper.clamp(i, 0, this.pageCount - 1);

        if (j != this.page) {
            this.page = j;
            this.update();
            if (this.level != null) BlockLectern.a(this.getWorld(), this.getPosition(), this.getBlock()); // CraftBukkit
        }

    }

    public int getPage() {
        return this.page;
    }

    public int i() {
        float f = this.pageCount > 1 ? (float) this.getPage() / ((float) this.pageCount - 1.0F) : 1.0F;

        return MathHelper.d(f * 14.0F) + (this.hasBook() ? 1 : 0);
    }

    private ItemStack b(ItemStack itemstack, @Nullable EntityHuman entityhuman) {
        if (this.level instanceof WorldServer && itemstack.a(Items.WRITTEN_BOOK)) {
            ItemWrittenBook.a(itemstack, this.a(entityhuman), entityhuman);
        }

        return itemstack;
    }

    // CraftBukkit start
    @Override
    public void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid) {
    }

    @Override
    public org.bukkit.command.CommandSender getBukkitSender(CommandListenerWrapper wrapper) {
        return wrapper.getEntity() != null ? wrapper.getEntity().getBukkitSender(wrapper) : new org.bukkit.craftbukkit.command.CraftBlockCommandSender(wrapper, this);
    }

    @Override
    public boolean shouldSendSuccess() {
        return false;
    }

    @Override
    public boolean shouldSendFailure() {
        return false;
    }

    @Override
    public boolean shouldBroadcastCommands() {
        return false;
    }

    // CraftBukkit end
    private CommandListenerWrapper a(@Nullable EntityHuman entityhuman) {
        String s;
        Object object;

        if (entityhuman == null) {
            s = "Lectern";
            object = new ChatComponentText("Lectern");
        } else {
            s = entityhuman.getDisplayName().getString();
            object = entityhuman.getScoreboardDisplayName();
        }

        Vec3D vec3d = Vec3D.a((BaseBlockPosition) this.worldPosition);

        // CraftBukkit - this
        return new CommandListenerWrapper(this, vec3d, Vec2F.ZERO, (WorldServer) this.level, 2, s, (IChatBaseComponent) object, this.level.getMinecraftServer(), entityhuman);
    }

    @Override
    public boolean isFilteredNBT() {
        return true;
    }

    @Override
    public void load(NBTTagCompound nbttagcompound) {
        super.load(nbttagcompound);
        if (nbttagcompound.hasKeyOfType("Book", 10)) {
            this.book = this.b(ItemStack.a(nbttagcompound.getCompound("Book")), (EntityHuman) null);
        } else {
            this.book = ItemStack.EMPTY;
        }

        this.pageCount = ItemWrittenBook.k(this.book);
        this.page = MathHelper.clamp(nbttagcompound.getInt("Page"), 0, this.pageCount - 1);
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbttagcompound) {
        super.save(nbttagcompound);
        if (!this.getBook().isEmpty()) {
            nbttagcompound.set("Book", this.getBook().save(new NBTTagCompound()));
            nbttagcompound.setInt("Page", this.page);
        }

        return nbttagcompound;
    }

    @Override
    public void clear() {
        this.setBook(ItemStack.EMPTY);
    }

    @Override
    public Container createMenu(int i, PlayerInventory playerinventory, EntityHuman entityhuman) {
        return new ContainerLectern(i, this.bookAccess, this.dataAccess, playerinventory); // CraftBukkit
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return new ChatMessage("container.lectern");
    }
}
