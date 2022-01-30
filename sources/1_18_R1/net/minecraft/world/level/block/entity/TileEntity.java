package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketListenerPlayOut;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.inventory.InventoryHolder;
// CraftBukkit end

import org.spigotmc.CustomTimingsHandler; // Spigot

public abstract class TileEntity {

    public CustomTimingsHandler tickTimer = org.bukkit.craftbukkit.SpigotTimings.getTileEntityTimings(this); // Spigot
    // CraftBukkit start - data containers
    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public CraftPersistentDataContainer persistentDataContainer;
    // CraftBukkit end
    private static final Logger LOGGER = LogManager.getLogger();
    private final TileEntityTypes<?> type;
    @Nullable
    protected World level;
    protected final BlockPosition worldPosition;
    protected boolean remove;
    private IBlockData blockState;

    public TileEntity(TileEntityTypes<?> tileentitytypes, BlockPosition blockposition, IBlockData iblockdata) {
        this.type = tileentitytypes;
        this.worldPosition = blockposition.immutable();
        this.blockState = iblockdata;
    }

    public static BlockPosition getPosFromTag(NBTTagCompound nbttagcompound) {
        return new BlockPosition(nbttagcompound.getInt("x"), nbttagcompound.getInt("y"), nbttagcompound.getInt("z"));
    }

    @Nullable
    public World getLevel() {
        return this.level;
    }

    public void setLevel(World world) {
        this.level = world;
    }

    public boolean hasLevel() {
        return this.level != null;
    }

    // CraftBukkit start - read container
    public void load(NBTTagCompound nbttagcompound) {
        this.persistentDataContainer = new CraftPersistentDataContainer(DATA_TYPE_REGISTRY);

        net.minecraft.nbt.NBTBase persistentDataTag = nbttagcompound.get("PublicBukkitValues");
        if (persistentDataTag instanceof NBTTagCompound) {
            this.persistentDataContainer.putAll((NBTTagCompound) persistentDataTag);
        }
    }
    // CraftBukkit end

    protected void saveAdditional(NBTTagCompound nbttagcompound) {}

    public final NBTTagCompound saveWithFullMetadata() {
        NBTTagCompound nbttagcompound = this.saveWithoutMetadata();

        this.saveMetadata(nbttagcompound);
        return nbttagcompound;
    }

    public final NBTTagCompound saveWithId() {
        NBTTagCompound nbttagcompound = this.saveWithoutMetadata();

        this.saveId(nbttagcompound);
        return nbttagcompound;
    }

    public final NBTTagCompound saveWithoutMetadata() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();

        this.saveAdditional(nbttagcompound);
        // CraftBukkit start - store container
        if (this.persistentDataContainer != null && !this.persistentDataContainer.isEmpty()) {
            nbttagcompound.put("PublicBukkitValues", this.persistentDataContainer.toTagCompound());
        }
        // CraftBukkit end
        return nbttagcompound;
    }

    private void saveId(NBTTagCompound nbttagcompound) {
        MinecraftKey minecraftkey = TileEntityTypes.getKey(this.getType());

        if (minecraftkey == null) {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        } else {
            nbttagcompound.putString("id", minecraftkey.toString());
        }
    }

    public static void addEntityType(NBTTagCompound nbttagcompound, TileEntityTypes<?> tileentitytypes) {
        nbttagcompound.putString("id", TileEntityTypes.getKey(tileentitytypes).toString());
    }

    public void saveToItem(ItemStack itemstack) {
        ItemBlock.setBlockEntityData(itemstack, this.getType(), this.saveWithoutMetadata());
    }

    private void saveMetadata(NBTTagCompound nbttagcompound) {
        this.saveId(nbttagcompound);
        nbttagcompound.putInt("x", this.worldPosition.getX());
        nbttagcompound.putInt("y", this.worldPosition.getY());
        nbttagcompound.putInt("z", this.worldPosition.getZ());
    }

    @Nullable
    public static TileEntity loadStatic(BlockPosition blockposition, IBlockData iblockdata, NBTTagCompound nbttagcompound) {
        String s = nbttagcompound.getString("id");
        MinecraftKey minecraftkey = MinecraftKey.tryParse(s);

        if (minecraftkey == null) {
            TileEntity.LOGGER.error("Block entity has invalid type: {}", s);
            return null;
        } else {
            return (TileEntity) IRegistry.BLOCK_ENTITY_TYPE.getOptional(minecraftkey).map((tileentitytypes) -> {
                try {
                    return tileentitytypes.create(blockposition, iblockdata);
                } catch (Throwable throwable) {
                    TileEntity.LOGGER.error("Failed to create block entity {}", s, throwable);
                    return null;
                }
            }).map((tileentity) -> {
                try {
                    tileentity.load(nbttagcompound);
                    return tileentity;
                } catch (Throwable throwable) {
                    TileEntity.LOGGER.error("Failed to load data for block entity {}", s, throwable);
                    return null;
                }
            }).orElseGet(() -> {
                TileEntity.LOGGER.warn("Skipping BlockEntity with id {}", s);
                return null;
            });
        }
    }

    public void setChanged() {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.blockState);
        }

    }

    protected static void setChanged(World world, BlockPosition blockposition, IBlockData iblockdata) {
        world.blockEntityChanged(blockposition);
        if (!iblockdata.isAir()) {
            world.updateNeighbourForOutputSignal(blockposition, iblockdata.getBlock());
        }

    }

    public BlockPosition getBlockPos() {
        return this.worldPosition;
    }

    public IBlockData getBlockState() {
        return this.blockState;
    }

    @Nullable
    public Packet<PacketListenerPlayOut> getUpdatePacket() {
        return null;
    }

    public NBTTagCompound getUpdateTag() {
        return new NBTTagCompound();
    }

    public boolean isRemoved() {
        return this.remove;
    }

    public void setRemoved() {
        this.remove = true;
    }

    public void clearRemoved() {
        this.remove = false;
    }

    public boolean triggerEvent(int i, int j) {
        return false;
    }

    public void fillCrashReportCategory(CrashReportSystemDetails crashreportsystemdetails) {
        crashreportsystemdetails.setDetail("Name", () -> {
            MinecraftKey minecraftkey = IRegistry.BLOCK_ENTITY_TYPE.getKey(this.getType());

            return minecraftkey + " // " + this.getClass().getCanonicalName();
        });
        if (this.level != null) {
            CrashReportSystemDetails.populateBlockDetails(crashreportsystemdetails, this.level, this.worldPosition, this.getBlockState());
            CrashReportSystemDetails.populateBlockDetails(crashreportsystemdetails, this.level, this.worldPosition, this.level.getBlockState(this.worldPosition));
        }
    }

    public boolean onlyOpCanSetNbt() {
        return false;
    }

    public TileEntityTypes<?> getType() {
        return this.type;
    }

    /** @deprecated */
    @Deprecated
    public void setBlockState(IBlockData iblockdata) {
        this.blockState = iblockdata;
    }

    // CraftBukkit start - add method
    public InventoryHolder getOwner() {
        if (level == null) return null;
        org.bukkit.block.BlockState state = level.getWorld().getBlockAt(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()).getState();
        if (state instanceof InventoryHolder) return (InventoryHolder) state;
        return null;
    }
    // CraftBukkit end
}
