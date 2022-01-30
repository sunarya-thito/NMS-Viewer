package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockShulkerBox;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

// CraftBukkit start
import net.minecraft.server.level.WorldServer;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.event.block.BlockCanBuildEvent;
// CraftBukkit end

public class ItemBlock extends Item {

    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    public static final String BLOCK_STATE_TAG = "BlockStateTag";
    /** @deprecated */
    @Deprecated
    private final Block block;

    public ItemBlock(Block block, Item.Info item_info) {
        super(item_info);
        this.block = block;
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext itemactioncontext) {
        EnumInteractionResult enuminteractionresult = this.place(new BlockActionContext(itemactioncontext));

        if (!enuminteractionresult.consumesAction() && this.isEdible()) {
            EnumInteractionResult enuminteractionresult1 = this.use(itemactioncontext.getLevel(), itemactioncontext.getPlayer(), itemactioncontext.getHand()).getResult();

            return enuminteractionresult1 == EnumInteractionResult.CONSUME ? EnumInteractionResult.CONSUME_PARTIAL : enuminteractionresult1;
        } else {
            return enuminteractionresult;
        }
    }

    public EnumInteractionResult place(BlockActionContext blockactioncontext) {
        if (!blockactioncontext.canPlace()) {
            return EnumInteractionResult.FAIL;
        } else {
            BlockActionContext blockactioncontext1 = this.updatePlacementContext(blockactioncontext);

            if (blockactioncontext1 == null) {
                return EnumInteractionResult.FAIL;
            } else {
                IBlockData iblockdata = this.getPlacementState(blockactioncontext1);
                // CraftBukkit start - special case for handling block placement with water lilies and snow buckets
                org.bukkit.block.BlockState blockstate = null;
                if (this instanceof ItemWaterLily || this instanceof SolidBucketItem) {
                    blockstate = org.bukkit.craftbukkit.block.CraftBlockStates.getBlockState(blockactioncontext1.getLevel(), blockactioncontext1.getClickedPos());
                }
                // CraftBukkit end

                if (iblockdata == null) {
                    return EnumInteractionResult.FAIL;
                } else if (!this.placeBlock(blockactioncontext1, iblockdata)) {
                    return EnumInteractionResult.FAIL;
                } else {
                    BlockPosition blockposition = blockactioncontext1.getClickedPos();
                    World world = blockactioncontext1.getLevel();
                    EntityHuman entityhuman = blockactioncontext1.getPlayer();
                    ItemStack itemstack = blockactioncontext1.getItemInHand();
                    IBlockData iblockdata1 = world.getBlockState(blockposition);

                    if (iblockdata1.is(iblockdata.getBlock())) {
                        iblockdata1 = this.updateBlockStateFromTag(blockposition, world, itemstack, iblockdata1);
                        this.updateCustomBlockEntityTag(blockposition, world, entityhuman, itemstack, iblockdata1);
                        iblockdata1.getBlock().setPlacedBy(world, blockposition, iblockdata1, entityhuman, itemstack);
                        // CraftBukkit start
                        if (blockstate != null) {
                            org.bukkit.event.block.BlockPlaceEvent placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent((WorldServer) world, entityhuman, blockactioncontext1.getHand(), blockstate, blockposition.getX(), blockposition.getY(), blockposition.getZ());
                            if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                                blockstate.update(true, false);

                                if (this instanceof SolidBucketItem) {
                                    ((EntityPlayer) entityhuman).getBukkitEntity().updateInventory(); // SPIGOT-4541
                                }
                                return EnumInteractionResult.FAIL;
                            }
                        }
                        // CraftBukkit end
                        if (entityhuman instanceof EntityPlayer) {
                            CriterionTriggers.PLACED_BLOCK.trigger((EntityPlayer) entityhuman, blockposition, itemstack);
                        }
                    }

                    SoundEffectType soundeffecttype = iblockdata1.getSoundType();

                    // world.playSound(entityhuman, blockposition, this.getPlaceSound(iblockdata1), SoundCategory.BLOCKS, (soundeffecttype.getVolume() + 1.0F) / 2.0F, soundeffecttype.getPitch() * 0.8F);
                    world.gameEvent(entityhuman, GameEvent.BLOCK_PLACE, blockposition);
                    if ((entityhuman == null || !entityhuman.getAbilities().instabuild) && itemstack != ItemStack.EMPTY) { // CraftBukkit
                        itemstack.shrink(1);
                    }

                    return EnumInteractionResult.sidedSuccess(world.isClientSide);
                }
            }
        }
    }

    protected SoundEffect getPlaceSound(IBlockData iblockdata) {
        return iblockdata.getSoundType().getPlaceSound();
    }

    @Nullable
    public BlockActionContext updatePlacementContext(BlockActionContext blockactioncontext) {
        return blockactioncontext;
    }

    protected boolean updateCustomBlockEntityTag(BlockPosition blockposition, World world, @Nullable EntityHuman entityhuman, ItemStack itemstack, IBlockData iblockdata) {
        return updateCustomBlockEntityTag(world, entityhuman, blockposition, itemstack);
    }

    @Nullable
    protected IBlockData getPlacementState(BlockActionContext blockactioncontext) {
        IBlockData iblockdata = this.getBlock().getStateForPlacement(blockactioncontext);

        return iblockdata != null && this.canPlace(blockactioncontext, iblockdata) ? iblockdata : null;
    }

    private IBlockData updateBlockStateFromTag(BlockPosition blockposition, World world, ItemStack itemstack, IBlockData iblockdata) {
        IBlockData iblockdata1 = iblockdata;
        NBTTagCompound nbttagcompound = itemstack.getTag();

        if (nbttagcompound != null) {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("BlockStateTag");
            // CraftBukkit start
            iblockdata1 = getBlockState(iblockdata1, nbttagcompound1);
        }

        if (iblockdata1 != iblockdata) {
            world.setBlock(blockposition, iblockdata1, 2);
        }

        return iblockdata1;
    }

    public static IBlockData getBlockState(IBlockData iblockdata, NBTTagCompound nbttagcompound1) {
        IBlockData iblockdata1 = iblockdata;
        {
            // CraftBukkit end
            BlockStateList<Block, IBlockData> blockstatelist = iblockdata.getBlock().getStateDefinition();
            Iterator iterator = nbttagcompound1.getAllKeys().iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();
                IBlockState<?> iblockstate = blockstatelist.getProperty(s);

                if (iblockstate != null) {
                    String s1 = nbttagcompound1.get(s).getAsString();

                    iblockdata1 = updateState(iblockdata1, iblockstate, s1);
                }
            }
        }
        return iblockdata1;
    }

    private static <T extends Comparable<T>> IBlockData updateState(IBlockData iblockdata, IBlockState<T> iblockstate, String s) {
        return (IBlockData) iblockstate.getValue(s).map((comparable) -> {
            return (IBlockData) iblockdata.setValue(iblockstate, comparable);
        }).orElse(iblockdata);
    }

    protected boolean canPlace(BlockActionContext blockactioncontext, IBlockData iblockdata) {
        EntityHuman entityhuman = blockactioncontext.getPlayer();
        VoxelShapeCollision voxelshapecollision = entityhuman == null ? VoxelShapeCollision.empty() : VoxelShapeCollision.of(entityhuman);
        // CraftBukkit start - store default return
        boolean defaultReturn = (!this.mustSurvive() || iblockdata.canSurvive(blockactioncontext.getLevel(), blockactioncontext.getClickedPos())) && blockactioncontext.getLevel().isUnobstructed(iblockdata, blockactioncontext.getClickedPos(), voxelshapecollision);
        org.bukkit.entity.Player player = (blockactioncontext.getPlayer() instanceof EntityPlayer) ? (org.bukkit.entity.Player) blockactioncontext.getPlayer().getBukkitEntity() : null;

        BlockCanBuildEvent event = new BlockCanBuildEvent(CraftBlock.at(blockactioncontext.getLevel(), blockactioncontext.getClickedPos()), player, CraftBlockData.fromData(iblockdata), defaultReturn);
        blockactioncontext.getLevel().getCraftServer().getPluginManager().callEvent(event);

        return event.isBuildable();
        // CraftBukkit end
    }

    protected boolean mustSurvive() {
        return true;
    }

    protected boolean placeBlock(BlockActionContext blockactioncontext, IBlockData iblockdata) {
        return blockactioncontext.getLevel().setBlock(blockactioncontext.getClickedPos(), iblockdata, 11);
    }

    public static boolean updateCustomBlockEntityTag(World world, @Nullable EntityHuman entityhuman, BlockPosition blockposition, ItemStack itemstack) {
        MinecraftServer minecraftserver = world.getServer();

        if (minecraftserver == null) {
            return false;
        } else {
            NBTTagCompound nbttagcompound = getBlockEntityData(itemstack);

            if (nbttagcompound != null) {
                TileEntity tileentity = world.getBlockEntity(blockposition);

                if (tileentity != null) {
                    if (!world.isClientSide && tileentity.onlyOpCanSetNbt() && (entityhuman == null || !(entityhuman.canUseGameMasterBlocks() || (entityhuman.getAbilities().instabuild && entityhuman.getBukkitEntity().hasPermission("minecraft.nbt.place"))))) { // Spigot - add permission
                        return false;
                    }

                    NBTTagCompound nbttagcompound1 = tileentity.saveWithoutMetadata();
                    NBTTagCompound nbttagcompound2 = nbttagcompound1.copy();

                    nbttagcompound1.merge(nbttagcompound);
                    if (!nbttagcompound1.equals(nbttagcompound2)) {
                        tileentity.load(nbttagcompound1);
                        tileentity.setChanged();
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    public String getDescriptionId() {
        return this.getBlock().getDescriptionId();
    }

    @Override
    public void fillItemCategory(CreativeModeTab creativemodetab, NonNullList<ItemStack> nonnulllist) {
        if (this.allowdedIn(creativemodetab)) {
            this.getBlock().fillItemCategory(creativemodetab, nonnulllist);
        }

    }

    @Override
    public void appendHoverText(ItemStack itemstack, @Nullable World world, List<IChatBaseComponent> list, TooltipFlag tooltipflag) {
        super.appendHoverText(itemstack, world, list, tooltipflag);
        this.getBlock().appendHoverText(itemstack, world, list, tooltipflag);
    }

    public Block getBlock() {
        return this.block;
    }

    public void registerBlocks(Map<Block, Item> map, Item item) {
        map.put(this.getBlock(), item);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return !(this.block instanceof BlockShulkerBox);
    }

    @Override
    public void onDestroyed(EntityItem entityitem) {
        if (this.block instanceof BlockShulkerBox) {
            ItemStack itemstack = entityitem.getItem();
            NBTTagCompound nbttagcompound = getBlockEntityData(itemstack);

            if (nbttagcompound != null && nbttagcompound.contains("Items", 9)) {
                NBTTagList nbttaglist = nbttagcompound.getList("Items", 10);
                Stream<net.minecraft.nbt.NBTBase> stream = nbttaglist.stream(); // CraftBukkit - decompile error

                Objects.requireNonNull(NBTTagCompound.class);
                ItemLiquidUtil.onContainerDestroyed(entityitem, stream.map(NBTTagCompound.class::cast).map(ItemStack::of));
            }
        }

    }

    @Nullable
    public static NBTTagCompound getBlockEntityData(ItemStack itemstack) {
        return itemstack.getTagElement("BlockEntityTag");
    }

    public static void setBlockEntityData(ItemStack itemstack, TileEntityTypes<?> tileentitytypes, NBTTagCompound nbttagcompound) {
        if (nbttagcompound.isEmpty()) {
            itemstack.removeTagKey("BlockEntityTag");
        } else {
            TileEntity.addEntityType(nbttagcompound, tileentitytypes);
            itemstack.addTagElement("BlockEntityTag", nbttagcompound);
        }

    }
}
