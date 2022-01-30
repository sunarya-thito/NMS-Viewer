package net.minecraft.world.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.arguments.blocks.ArgumentBlock;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentDurability;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.util.datafix.fixes.DataConverterTypes;
import net.minecraft.world.level.block.BlockJukeBox;
import net.minecraft.world.level.block.BlockSapling;
import net.minecraft.world.level.block.BlockTileEntity;
import net.minecraft.world.level.block.BlockWitherSkull;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.world.StructureGrowEvent;
// CraftBukkit end

public final class ItemStack {

    public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IRegistry.ITEM.byNameCodec().fieldOf("id").forGetter((itemstack) -> {
            return itemstack.item;
        }), Codec.INT.fieldOf("Count").forGetter((itemstack) -> {
            return itemstack.count;
        }), NBTTagCompound.CODEC.optionalFieldOf("tag").forGetter((itemstack) -> {
            return Optional.ofNullable(itemstack.tag);
        })).apply(instance, ItemStack::new);
    });
    private static final Logger LOGGER = LogManager.getLogger();
    public static final ItemStack EMPTY = new ItemStack((Item) null);
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = (DecimalFormat) SystemUtils.make(new DecimalFormat("#.##"), (decimalformat) -> {
        decimalformat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    });
    public static final String TAG_ENCH = "Enchantments";
    public static final String TAG_DISPLAY = "display";
    public static final String TAG_DISPLAY_NAME = "Name";
    public static final String TAG_LORE = "Lore";
    public static final String TAG_DAMAGE = "Damage";
    public static final String TAG_COLOR = "color";
    private static final String TAG_UNBREAKABLE = "Unbreakable";
    private static final String TAG_REPAIR_COST = "RepairCost";
    private static final String TAG_CAN_DESTROY_BLOCK_LIST = "CanDestroy";
    private static final String TAG_CAN_PLACE_ON_BLOCK_LIST = "CanPlaceOn";
    private static final String TAG_HIDE_FLAGS = "HideFlags";
    private static final int DONT_HIDE_TOOLTIP = 0;
    private static final ChatModifier LORE_STYLE = ChatModifier.EMPTY.withColor(EnumChatFormat.DARK_PURPLE).withItalic(true);
    private int count;
    private int popTime;
    /** @deprecated */
    @Deprecated
    private Item item;
    @Nullable
    private NBTTagCompound tag;
    private boolean emptyCacheFlag;
    @Nullable
    private Entity entityRepresentation;
    @Nullable
    private AdventureModeCheck adventureBreakCheck;
    @Nullable
    private AdventureModeCheck adventurePlaceCheck;

    public Optional<TooltipComponent> getTooltipImage() {
        return this.getItem().getTooltipImage(this);
    }

    public ItemStack(IMaterial imaterial) {
        this(imaterial, 1);
    }

    private ItemStack(IMaterial imaterial, int i, Optional<NBTTagCompound> optional) {
        this(imaterial, i);
        optional.ifPresent(this::setTag);
    }

    public ItemStack(IMaterial imaterial, int i) {
        this.item = imaterial == null ? null : imaterial.asItem();
        this.count = i;
        if (this.item != null && this.item.canBeDepleted()) {
            this.setDamageValue(this.getDamageValue());
        }

        this.updateEmptyCacheFlag();
    }

    // Called to run this stack through the data converter to handle older storage methods and serialized items
    public void convertStack(int version) {
        if (0 < version && version < CraftMagicNumbers.INSTANCE.getDataVersion()) {
            NBTTagCompound savedStack = new NBTTagCompound();
            this.save(savedStack);
            savedStack = (NBTTagCompound) MinecraftServer.getServer().fixerUpper.update(DataConverterTypes.ITEM_STACK, new Dynamic(DynamicOpsNBT.INSTANCE, savedStack), version, CraftMagicNumbers.INSTANCE.getDataVersion()).getValue();
            this.load(savedStack);
        }
    }

    private void updateEmptyCacheFlag() {
        if (this.emptyCacheFlag && this == ItemStack.EMPTY) throw new AssertionError("TRAP"); // CraftBukkit
        this.emptyCacheFlag = false;
        this.emptyCacheFlag = this.isEmpty();
    }

    // CraftBukkit - break into own method
    private void load(NBTTagCompound nbttagcompound) {
        this.item = (Item) IRegistry.ITEM.get(new MinecraftKey(nbttagcompound.getString("id")));
        this.count = nbttagcompound.getByte("Count");
        if (nbttagcompound.contains("tag", 10)) {
            // CraftBukkit start - make defensive copy as this data may be coming from the save thread
            this.tag = nbttagcompound.getCompound("tag").copy();
            // CraftBukkit end
            this.getItem().verifyTagAfterLoad(this.tag);
        }

        if (this.getItem().canBeDepleted()) {
            this.setDamageValue(this.getDamageValue());
        }

    }

    private ItemStack(NBTTagCompound nbttagcompound) {
        this.load(nbttagcompound);
        // CraftBukkit end
        this.updateEmptyCacheFlag();
    }

    public static ItemStack of(NBTTagCompound nbttagcompound) {
        try {
            return new ItemStack(nbttagcompound);
        } catch (RuntimeException runtimeexception) {
            ItemStack.LOGGER.debug("Tried to load invalid item: {}", nbttagcompound, runtimeexception);
            return ItemStack.EMPTY;
        }
    }

    public boolean isEmpty() {
        return this == ItemStack.EMPTY ? true : (this.getItem() != null && !this.is(Items.AIR) ? this.count <= 0 : true);
    }

    public ItemStack split(int i) {
        int j = Math.min(i, this.count);
        ItemStack itemstack = this.copy();

        itemstack.setCount(j);
        this.shrink(j);
        return itemstack;
    }

    public Item getItem() {
        return this.emptyCacheFlag ? Items.AIR : this.item;
    }

    public boolean is(Tag<Item> tag) {
        return tag.contains(this.getItem());
    }

    public boolean is(Item item) {
        return this.getItem() == item;
    }

    public EnumInteractionResult useOn(ItemActionContext itemactioncontext, EnumHand enumhand) { // CraftBukkit - add hand
        EntityHuman entityhuman = itemactioncontext.getPlayer();
        BlockPosition blockposition = itemactioncontext.getClickedPos();
        ShapeDetectorBlock shapedetectorblock = new ShapeDetectorBlock(itemactioncontext.getLevel(), blockposition, false);

        if (entityhuman != null && !entityhuman.getAbilities().mayBuild && !this.hasAdventureModePlaceTagForBlock(itemactioncontext.getLevel().getTagManager(), shapedetectorblock)) {
            return EnumInteractionResult.PASS;
        } else {
            // CraftBukkit start - handle all block place event logic here
            NBTTagCompound oldData = this.getTagClone();
            int oldCount = this.getCount();
            WorldServer world = (WorldServer) itemactioncontext.getLevel();

            if (!(this.getItem() instanceof ItemBucket || this.getItem() instanceof SolidBucketItem)) { // if not bucket
                world.captureBlockStates = true;
                // special case bonemeal
                if (this.getItem() == Items.BONE_MEAL) {
                    world.captureTreeGeneration = true;
                }
            }
            Item item = this.getItem();
            EnumInteractionResult enuminteractionresult = item.useOn(itemactioncontext);
            NBTTagCompound newData = this.getTagClone();
            int newCount = this.getCount();
            this.setCount(oldCount);
            this.setTagClone(oldData);
            world.captureBlockStates = false;
            if (enuminteractionresult.consumesAction() && world.captureTreeGeneration && world.capturedBlockStates.size() > 0) {
                world.captureTreeGeneration = false;
                Location location = new Location(world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ());
                TreeType treeType = BlockSapling.treeType;
                BlockSapling.treeType = null;
                List<BlockState> blocks = new java.util.ArrayList<>(world.capturedBlockStates.values());
                world.capturedBlockStates.clear();
                StructureGrowEvent structureEvent = null;
                if (treeType != null) {
                    boolean isBonemeal = getItem() == Items.BONE_MEAL;
                    structureEvent = new StructureGrowEvent(location, treeType, isBonemeal, (Player) entityhuman.getBukkitEntity(), blocks);
                    org.bukkit.Bukkit.getPluginManager().callEvent(structureEvent);
                }

                BlockFertilizeEvent fertilizeEvent = new BlockFertilizeEvent(CraftBlock.at(world, blockposition), (Player) entityhuman.getBukkitEntity(), blocks);
                fertilizeEvent.setCancelled(structureEvent != null && structureEvent.isCancelled());
                org.bukkit.Bukkit.getPluginManager().callEvent(fertilizeEvent);

                if (!fertilizeEvent.isCancelled()) {
                    // Change the stack to its new contents if it hasn't been tampered with.
                    if (this.getCount() == oldCount && Objects.equals(this.tag, oldData)) {
                        this.setTag(newData);
                        this.setCount(newCount);
                    }
                    for (BlockState blockstate : blocks) {
                        blockstate.update(true);
                    }
                }

                ItemSign.openSign = null; // SPIGOT-6758 - Reset on early return
                return enuminteractionresult;
            }
            world.captureTreeGeneration = false;

            if (entityhuman != null && enuminteractionresult.shouldAwardStats()) {
                org.bukkit.event.block.BlockPlaceEvent placeEvent = null;
                List<BlockState> blocks = new java.util.ArrayList<>(world.capturedBlockStates.values());
                world.capturedBlockStates.clear();
                if (blocks.size() > 1) {
                    placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockMultiPlaceEvent(world, entityhuman, enumhand, blocks, blockposition.getX(), blockposition.getY(), blockposition.getZ());
                } else if (blocks.size() == 1) {
                    placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(world, entityhuman, enumhand, blocks.get(0), blockposition.getX(), blockposition.getY(), blockposition.getZ());
                }

                if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                    enuminteractionresult = EnumInteractionResult.FAIL; // cancel placement
                    // PAIL: Remove this when MC-99075 fixed
                    placeEvent.getPlayer().updateInventory();
                    // revert back all captured blocks
                    world.preventPoiUpdated = true; // CraftBukkit - SPIGOT-5710
                    for (BlockState blockstate : blocks) {
                        blockstate.update(true, false);
                    }
                    world.preventPoiUpdated = false;

                    // Brute force all possible updates
                    BlockPosition placedPos = ((CraftBlock) placeEvent.getBlock()).getPosition();
                    for (EnumDirection dir : EnumDirection.values()) {
                        ((EntityPlayer) entityhuman).connection.send(new PacketPlayOutBlockChange(world, placedPos.relative(dir)));
                    }
                    ItemSign.openSign = null; // SPIGOT-6758 - Reset on early return
                } else {
                    // Change the stack to its new contents if it hasn't been tampered with.
                    if (this.getCount() == oldCount && Objects.equals(this.tag, oldData)) {
                        this.setTag(newData);
                        this.setCount(newCount);
                    }

                    for (Map.Entry<BlockPosition, TileEntity> e : world.capturedTileEntities.entrySet()) {
                        world.setBlockEntity(e.getValue());
                    }

                    for (BlockState blockstate : blocks) {
                        int updateFlag = ((CraftBlockState) blockstate).getFlag();
                        IBlockData oldBlock = ((CraftBlockState) blockstate).getHandle();
                        BlockPosition newblockposition = ((CraftBlockState) blockstate).getPosition();
                        IBlockData block = world.getBlockState(newblockposition);

                        if (!(block.getBlock() instanceof BlockTileEntity)) { // Containers get placed automatically
                            block.getBlock().onPlace(block, world, newblockposition, oldBlock, true);
                        }

                        world.notifyAndUpdatePhysics(newblockposition, null, oldBlock, block, world.getBlockState(newblockposition), updateFlag, 512); // send null chunk as chunk.k() returns false by this point
                    }

                    // Special case juke boxes as they update their tile entity. Copied from ItemRecord.
                    // PAIL: checkme on updates.
                    if (this.item instanceof ItemRecord) {
                        ((BlockJukeBox) Blocks.JUKEBOX).setRecord(world, blockposition, world.getBlockState(blockposition), this);
                        world.levelEvent((EntityHuman) null, 1010, blockposition, Item.getId(this.item));
                        this.shrink(1);
                        entityhuman.awardStat(StatisticList.PLAY_RECORD);
                    }

                    if (this.item == Items.WITHER_SKELETON_SKULL) { // Special case skulls to allow wither spawns to be cancelled
                        BlockPosition bp = blockposition;
                        if (!world.getBlockState(blockposition).getMaterial().isReplaceable()) {
                            if (!world.getBlockState(blockposition).getMaterial().isSolid()) {
                                bp = null;
                            } else {
                                bp = bp.relative(itemactioncontext.getClickedFace());
                            }
                        }
                        if (bp != null) {
                            TileEntity te = world.getBlockEntity(bp);
                            if (te instanceof TileEntitySkull) {
                                BlockWitherSkull.checkSpawn(world, bp, (TileEntitySkull) te);
                            }
                        }
                    }

                    // SPIGOT-4678
                    if (this.item instanceof ItemSign && ItemSign.openSign != null) {
                        try {
                            entityhuman.openTextEdit((TileEntitySign) world.getBlockEntity(ItemSign.openSign));
                        } finally {
                            ItemSign.openSign = null;
                        }
                    }

                    // SPIGOT-1288 - play sound stripped from ItemBlock
                    if (this.item instanceof ItemBlock) {
                        SoundEffectType soundeffecttype = ((ItemBlock) this.item).getBlock().getSoundType(null);
                        world.playSound(entityhuman, blockposition, soundeffecttype.getPlaceSound(), SoundCategory.BLOCKS, (soundeffecttype.getVolume() + 1.0F) / 2.0F, soundeffecttype.getPitch() * 0.8F);
                    }

                    entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
                }
            }
            world.capturedTileEntities.clear();
            world.capturedBlockStates.clear();
            // CraftBukkit end

            return enuminteractionresult;
        }
    }

    public float getDestroySpeed(IBlockData iblockdata) {
        return this.getItem().getDestroySpeed(this, iblockdata);
    }

    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman entityhuman, EnumHand enumhand) {
        return this.getItem().use(world, entityhuman, enumhand);
    }

    public ItemStack finishUsingItem(World world, EntityLiving entityliving) {
        return this.getItem().finishUsingItem(this, world, entityliving);
    }

    public NBTTagCompound save(NBTTagCompound nbttagcompound) {
        MinecraftKey minecraftkey = IRegistry.ITEM.getKey(this.getItem());

        nbttagcompound.putString("id", minecraftkey == null ? "minecraft:air" : minecraftkey.toString());
        nbttagcompound.putByte("Count", (byte) this.count);
        if (this.tag != null) {
            nbttagcompound.put("tag", this.tag.copy());
        }

        return nbttagcompound;
    }

    public int getMaxStackSize() {
        return this.getItem().getMaxStackSize();
    }

    public boolean isStackable() {
        return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
    }

    public boolean isDamageableItem() {
        if (!this.emptyCacheFlag && this.getItem().getMaxDamage() > 0) {
            NBTTagCompound nbttagcompound = this.getTag();

            return nbttagcompound == null || !nbttagcompound.getBoolean("Unbreakable");
        } else {
            return false;
        }
    }

    public boolean isDamaged() {
        return this.isDamageableItem() && this.getDamageValue() > 0;
    }

    public int getDamageValue() {
        return this.tag == null ? 0 : this.tag.getInt("Damage");
    }

    public void setDamageValue(int i) {
        this.getOrCreateTag().putInt("Damage", Math.max(0, i));
    }

    public int getMaxDamage() {
        return this.getItem().getMaxDamage();
    }

    public boolean hurt(int i, Random random, @Nullable EntityPlayer entityplayer) {
        if (!this.isDamageableItem()) {
            return false;
        } else {
            int j;

            if (i > 0) {
                j = EnchantmentManager.getItemEnchantmentLevel(Enchantments.UNBREAKING, this);
                int k = 0;

                for (int l = 0; j > 0 && l < i; ++l) {
                    if (EnchantmentDurability.shouldIgnoreDurabilityDrop(this, j, random)) {
                        ++k;
                    }
                }

                i -= k;
                // CraftBukkit start
                if (entityplayer != null) {
                    PlayerItemDamageEvent event = new PlayerItemDamageEvent(entityplayer.getBukkitEntity(), CraftItemStack.asCraftMirror(this), i);
                    event.getPlayer().getServer().getPluginManager().callEvent(event);

                    if (i != event.getDamage() || event.isCancelled()) {
                        event.getPlayer().updateInventory();
                    }
                    if (event.isCancelled()) {
                        return false;
                    }

                    i = event.getDamage();
                }
                // CraftBukkit end
                if (i <= 0) {
                    return false;
                }
            }

            if (entityplayer != null && i != 0) {
                CriterionTriggers.ITEM_DURABILITY_CHANGED.trigger(entityplayer, this, this.getDamageValue() + i);
            }

            j = this.getDamageValue() + i;
            this.setDamageValue(j);
            return j >= this.getMaxDamage();
        }
    }

    public <T extends EntityLiving> void hurtAndBreak(int i, T t0, Consumer<T> consumer) {
        if (!t0.level.isClientSide && (!(t0 instanceof EntityHuman) || !((EntityHuman) t0).getAbilities().instabuild)) {
            if (this.isDamageableItem()) {
                if (this.hurt(i, t0.getRandom(), t0 instanceof EntityPlayer ? (EntityPlayer) t0 : null)) {
                    consumer.accept(t0);
                    Item item = this.getItem();
                    // CraftBukkit start - Check for item breaking
                    if (this.count == 1 && t0 instanceof EntityHuman) {
                        org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerItemBreakEvent((EntityHuman) t0, this);
                    }
                    // CraftBukkit end

                    this.shrink(1);
                    if (t0 instanceof EntityHuman) {
                        ((EntityHuman) t0).awardStat(StatisticList.ITEM_BROKEN.get(item));
                    }

                    this.setDamageValue(0);
                }

            }
        }
    }

    public boolean isBarVisible() {
        return this.item.isBarVisible(this);
    }

    public int getBarWidth() {
        return this.item.getBarWidth(this);
    }

    public int getBarColor() {
        return this.item.getBarColor(this);
    }

    public boolean overrideStackedOnOther(Slot slot, ClickAction clickaction, EntityHuman entityhuman) {
        return this.getItem().overrideStackedOnOther(this, slot, clickaction, entityhuman);
    }

    public boolean overrideOtherStackedOnMe(ItemStack itemstack, Slot slot, ClickAction clickaction, EntityHuman entityhuman, SlotAccess slotaccess) {
        return this.getItem().overrideOtherStackedOnMe(this, itemstack, slot, clickaction, entityhuman, slotaccess);
    }

    public void hurtEnemy(EntityLiving entityliving, EntityHuman entityhuman) {
        Item item = this.getItem();

        if (item.hurtEnemy(this, entityliving, entityhuman)) {
            entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
        }

    }

    public void mineBlock(World world, IBlockData iblockdata, BlockPosition blockposition, EntityHuman entityhuman) {
        Item item = this.getItem();

        if (item.mineBlock(this, world, iblockdata, blockposition, entityhuman)) {
            entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
        }

    }

    public boolean isCorrectToolForDrops(IBlockData iblockdata) {
        return this.getItem().isCorrectToolForDrops(iblockdata);
    }

    public EnumInteractionResult interactLivingEntity(EntityHuman entityhuman, EntityLiving entityliving, EnumHand enumhand) {
        return this.getItem().interactLivingEntity(this, entityhuman, entityliving, enumhand);
    }

    public ItemStack copy() {
        if (this.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemstack = new ItemStack(this.getItem(), this.count);

            itemstack.setPopTime(this.getPopTime());
            if (this.tag != null) {
                itemstack.tag = this.tag.copy();
            }

            return itemstack;
        }
    }

    public static boolean tagMatches(ItemStack itemstack, ItemStack itemstack1) {
        return itemstack.isEmpty() && itemstack1.isEmpty() ? true : (!itemstack.isEmpty() && !itemstack1.isEmpty() ? (itemstack.tag == null && itemstack1.tag != null ? false : itemstack.tag == null || itemstack.tag.equals(itemstack1.tag)) : false);
    }

    public static boolean matches(ItemStack itemstack, ItemStack itemstack1) {
        return itemstack.isEmpty() && itemstack1.isEmpty() ? true : (!itemstack.isEmpty() && !itemstack1.isEmpty() ? itemstack.matches(itemstack1) : false);
    }

    private boolean matches(ItemStack itemstack) {
        return this.count != itemstack.count ? false : (!this.is(itemstack.getItem()) ? false : (this.tag == null && itemstack.tag != null ? false : this.tag == null || this.tag.equals(itemstack.tag)));
    }

    public static boolean isSame(ItemStack itemstack, ItemStack itemstack1) {
        return itemstack == itemstack1 ? true : (!itemstack.isEmpty() && !itemstack1.isEmpty() ? itemstack.sameItem(itemstack1) : false);
    }

    public static boolean isSameIgnoreDurability(ItemStack itemstack, ItemStack itemstack1) {
        return itemstack == itemstack1 ? true : (!itemstack.isEmpty() && !itemstack1.isEmpty() ? itemstack.sameItemStackIgnoreDurability(itemstack1) : false);
    }

    public boolean sameItem(ItemStack itemstack) {
        return !itemstack.isEmpty() && this.is(itemstack.getItem());
    }

    public boolean sameItemStackIgnoreDurability(ItemStack itemstack) {
        return !this.isDamageableItem() ? this.sameItem(itemstack) : !itemstack.isEmpty() && this.is(itemstack.getItem());
    }

    public static boolean isSameItemSameTags(ItemStack itemstack, ItemStack itemstack1) {
        return itemstack.is(itemstack1.getItem()) && tagMatches(itemstack, itemstack1);
    }

    public String getDescriptionId() {
        return this.getItem().getDescriptionId(this);
    }

    public String toString() {
        return this.count + " " + this.getItem();
    }

    public void inventoryTick(World world, Entity entity, int i, boolean flag) {
        if (this.popTime > 0) {
            --this.popTime;
        }

        if (this.getItem() != null) {
            this.getItem().inventoryTick(this, world, entity, i, flag);
        }

    }

    public void onCraftedBy(World world, EntityHuman entityhuman, int i) {
        entityhuman.awardStat(StatisticList.ITEM_CRAFTED.get(this.getItem()), i);
        this.getItem().onCraftedBy(this, world, entityhuman);
    }

    public int getUseDuration() {
        return this.getItem().getUseDuration(this);
    }

    public EnumAnimation getUseAnimation() {
        return this.getItem().getUseAnimation(this);
    }

    public void releaseUsing(World world, EntityLiving entityliving, int i) {
        this.getItem().releaseUsing(this, world, entityliving, i);
    }

    public boolean useOnRelease() {
        return this.getItem().useOnRelease(this);
    }

    public boolean hasTag() {
        return !this.emptyCacheFlag && this.tag != null && !this.tag.isEmpty();
    }

    @Nullable
    public NBTTagCompound getTag() {
        return this.tag;
    }

    // CraftBukkit start
    @Nullable
    private NBTTagCompound getTagClone() {
        return this.tag == null ? null : this.tag.copy();
    }

    private void setTagClone(@Nullable NBTTagCompound nbtttagcompound) {
        this.setTag(nbtttagcompound == null ? null : nbtttagcompound.copy());
    }
    // CraftBukkit end

    public NBTTagCompound getOrCreateTag() {
        if (this.tag == null) {
            this.setTag(new NBTTagCompound());
        }

        return this.tag;
    }

    public NBTTagCompound getOrCreateTagElement(String s) {
        if (this.tag != null && this.tag.contains(s, 10)) {
            return this.tag.getCompound(s);
        } else {
            NBTTagCompound nbttagcompound = new NBTTagCompound();

            this.addTagElement(s, nbttagcompound);
            return nbttagcompound;
        }
    }

    @Nullable
    public NBTTagCompound getTagElement(String s) {
        return this.tag != null && this.tag.contains(s, 10) ? this.tag.getCompound(s) : null;
    }

    public void removeTagKey(String s) {
        if (this.tag != null && this.tag.contains(s)) {
            this.tag.remove(s);
            if (this.tag.isEmpty()) {
                this.tag = null;
            }
        }

    }

    public NBTTagList getEnchantmentTags() {
        return this.tag != null ? this.tag.getList("Enchantments", 10) : new NBTTagList();
    }

    public void setTag(@Nullable NBTTagCompound nbttagcompound) {
        this.tag = nbttagcompound;
        if (this.getItem().canBeDepleted()) {
            this.setDamageValue(this.getDamageValue());
        }

        if (nbttagcompound != null) {
            this.getItem().verifyTagAfterLoad(nbttagcompound);
        }

    }

    public IChatBaseComponent getHoverName() {
        NBTTagCompound nbttagcompound = this.getTagElement("display");

        if (nbttagcompound != null && nbttagcompound.contains("Name", 8)) {
            try {
                IChatMutableComponent ichatmutablecomponent = IChatBaseComponent.ChatSerializer.fromJson(nbttagcompound.getString("Name"));

                if (ichatmutablecomponent != null) {
                    return ichatmutablecomponent;
                }

                nbttagcompound.remove("Name");
            } catch (Exception exception) {
                nbttagcompound.remove("Name");
            }
        }

        return this.getItem().getName(this);
    }

    public ItemStack setHoverName(@Nullable IChatBaseComponent ichatbasecomponent) {
        NBTTagCompound nbttagcompound = this.getOrCreateTagElement("display");

        if (ichatbasecomponent != null) {
            nbttagcompound.putString("Name", IChatBaseComponent.ChatSerializer.toJson(ichatbasecomponent));
        } else {
            nbttagcompound.remove("Name");
        }

        return this;
    }

    public void resetHoverName() {
        NBTTagCompound nbttagcompound = this.getTagElement("display");

        if (nbttagcompound != null) {
            nbttagcompound.remove("Name");
            if (nbttagcompound.isEmpty()) {
                this.removeTagKey("display");
            }
        }

        if (this.tag != null && this.tag.isEmpty()) {
            this.tag = null;
        }

    }

    public boolean hasCustomHoverName() {
        NBTTagCompound nbttagcompound = this.getTagElement("display");

        return nbttagcompound != null && nbttagcompound.contains("Name", 8);
    }

    public List<IChatBaseComponent> getTooltipLines(@Nullable EntityHuman entityhuman, TooltipFlag tooltipflag) {
        List<IChatBaseComponent> list = Lists.newArrayList();
        IChatMutableComponent ichatmutablecomponent = (new ChatComponentText("")).append(this.getHoverName()).withStyle(this.getRarity().color);

        if (this.hasCustomHoverName()) {
            ichatmutablecomponent.withStyle(EnumChatFormat.ITALIC);
        }

        list.add(ichatmutablecomponent);
        if (!tooltipflag.isAdvanced() && !this.hasCustomHoverName() && this.is(Items.FILLED_MAP)) {
            Integer integer = ItemWorldMap.getMapId(this);

            if (integer != null) {
                list.add((new ChatComponentText("#" + integer)).withStyle(EnumChatFormat.GRAY));
            }
        }

        int i = this.getHideFlags();

        if (shouldShowInTooltip(i, ItemStack.HideFlags.ADDITIONAL)) {
            this.getItem().appendHoverText(this, entityhuman == null ? null : entityhuman.level, list, tooltipflag);
        }

        int j;

        if (this.hasTag()) {
            if (shouldShowInTooltip(i, ItemStack.HideFlags.ENCHANTMENTS)) {
                appendEnchantmentNames(list, this.getEnchantmentTags());
            }

            if (this.tag.contains("display", 10)) {
                NBTTagCompound nbttagcompound = this.tag.getCompound("display");

                if (shouldShowInTooltip(i, ItemStack.HideFlags.DYE) && nbttagcompound.contains("color", 99)) {
                    if (tooltipflag.isAdvanced()) {
                        list.add((new ChatMessage("item.color", new Object[]{String.format("#%06X", nbttagcompound.getInt("color"))})).withStyle(EnumChatFormat.GRAY));
                    } else {
                        list.add((new ChatMessage("item.dyed")).withStyle(new EnumChatFormat[]{EnumChatFormat.GRAY, EnumChatFormat.ITALIC}));
                    }
                }

                if (nbttagcompound.getTagType("Lore") == 9) {
                    NBTTagList nbttaglist = nbttagcompound.getList("Lore", 8);

                    for (j = 0; j < nbttaglist.size(); ++j) {
                        String s = nbttaglist.getString(j);

                        try {
                            IChatMutableComponent ichatmutablecomponent1 = IChatBaseComponent.ChatSerializer.fromJson(s);

                            if (ichatmutablecomponent1 != null) {
                                list.add(ChatComponentUtils.mergeStyles(ichatmutablecomponent1, ItemStack.LORE_STYLE));
                            }
                        } catch (Exception exception) {
                            nbttagcompound.remove("Lore");
                        }
                    }
                }
            }
        }

        int k;

        if (shouldShowInTooltip(i, ItemStack.HideFlags.MODIFIERS)) {
            EnumItemSlot[] aenumitemslot = EnumItemSlot.values();

            k = aenumitemslot.length;

            for (j = 0; j < k; ++j) {
                EnumItemSlot enumitemslot = aenumitemslot[j];
                Multimap<AttributeBase, AttributeModifier> multimap = this.getAttributeModifiers(enumitemslot);

                if (!multimap.isEmpty()) {
                    list.add(ChatComponentText.EMPTY);
                    list.add((new ChatMessage("item.modifiers." + enumitemslot.getName())).withStyle(EnumChatFormat.GRAY));
                    Iterator iterator = multimap.entries().iterator();

                    while (iterator.hasNext()) {
                        Entry<AttributeBase, AttributeModifier> entry = (Entry) iterator.next();
                        AttributeModifier attributemodifier = (AttributeModifier) entry.getValue();
                        double d0 = attributemodifier.getAmount();
                        boolean flag = false;

                        if (entityhuman != null) {
                            if (attributemodifier.getId() == Item.BASE_ATTACK_DAMAGE_UUID) {
                                d0 += entityhuman.getAttributeBaseValue(GenericAttributes.ATTACK_DAMAGE);
                                d0 += (double) EnchantmentManager.getDamageBonus(this, EnumMonsterType.UNDEFINED);
                                flag = true;
                            } else if (attributemodifier.getId() == Item.BASE_ATTACK_SPEED_UUID) {
                                d0 += entityhuman.getAttributeBaseValue(GenericAttributes.ATTACK_SPEED);
                                flag = true;
                            }
                        }

                        double d1;

                        if (attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                            if (((AttributeBase) entry.getKey()).equals(GenericAttributes.KNOCKBACK_RESISTANCE)) {
                                d1 = d0 * 10.0D;
                            } else {
                                d1 = d0;
                            }
                        } else {
                            d1 = d0 * 100.0D;
                        }

                        if (flag) {
                            list.add((new ChatComponentText(" ")).append((IChatBaseComponent) (new ChatMessage("attribute.modifier.equals." + attributemodifier.getOperation().toValue(), new Object[]{ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), new ChatMessage(((AttributeBase) entry.getKey()).getDescriptionId())}))).withStyle(EnumChatFormat.DARK_GREEN));
                        } else if (d0 > 0.0D) {
                            list.add((new ChatMessage("attribute.modifier.plus." + attributemodifier.getOperation().toValue(), new Object[]{ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), new ChatMessage(((AttributeBase) entry.getKey()).getDescriptionId())})).withStyle(EnumChatFormat.BLUE));
                        } else if (d0 < 0.0D) {
                            d1 *= -1.0D;
                            list.add((new ChatMessage("attribute.modifier.take." + attributemodifier.getOperation().toValue(), new Object[]{ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), new ChatMessage(((AttributeBase) entry.getKey()).getDescriptionId())})).withStyle(EnumChatFormat.RED));
                        }
                    }
                }
            }
        }

        if (this.hasTag()) {
            if (shouldShowInTooltip(i, ItemStack.HideFlags.UNBREAKABLE) && this.tag.getBoolean("Unbreakable")) {
                list.add((new ChatMessage("item.unbreakable")).withStyle(EnumChatFormat.BLUE));
            }

            NBTTagList nbttaglist1;

            if (shouldShowInTooltip(i, ItemStack.HideFlags.CAN_DESTROY) && this.tag.contains("CanDestroy", 9)) {
                nbttaglist1 = this.tag.getList("CanDestroy", 8);
                if (!nbttaglist1.isEmpty()) {
                    list.add(ChatComponentText.EMPTY);
                    list.add((new ChatMessage("item.canBreak")).withStyle(EnumChatFormat.GRAY));

                    for (k = 0; k < nbttaglist1.size(); ++k) {
                        list.addAll(expandBlockState(nbttaglist1.getString(k)));
                    }
                }
            }

            if (shouldShowInTooltip(i, ItemStack.HideFlags.CAN_PLACE) && this.tag.contains("CanPlaceOn", 9)) {
                nbttaglist1 = this.tag.getList("CanPlaceOn", 8);
                if (!nbttaglist1.isEmpty()) {
                    list.add(ChatComponentText.EMPTY);
                    list.add((new ChatMessage("item.canPlace")).withStyle(EnumChatFormat.GRAY));

                    for (k = 0; k < nbttaglist1.size(); ++k) {
                        list.addAll(expandBlockState(nbttaglist1.getString(k)));
                    }
                }
            }
        }

        if (tooltipflag.isAdvanced()) {
            if (this.isDamaged()) {
                list.add(new ChatMessage("item.durability", new Object[]{this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()}));
            }

            list.add((new ChatComponentText(IRegistry.ITEM.getKey(this.getItem()).toString())).withStyle(EnumChatFormat.DARK_GRAY));
            if (this.hasTag()) {
                list.add((new ChatMessage("item.nbt_tags", new Object[]{this.tag.getAllKeys().size()})).withStyle(EnumChatFormat.DARK_GRAY));
            }
        }

        return list;
    }

    private static boolean shouldShowInTooltip(int i, ItemStack.HideFlags itemstack_hideflags) {
        return (i & itemstack_hideflags.getMask()) == 0;
    }

    private int getHideFlags() {
        return this.hasTag() && this.tag.contains("HideFlags", 99) ? this.tag.getInt("HideFlags") : 0;
    }

    public void hideTooltipPart(ItemStack.HideFlags itemstack_hideflags) {
        NBTTagCompound nbttagcompound = this.getOrCreateTag();

        nbttagcompound.putInt("HideFlags", nbttagcompound.getInt("HideFlags") | itemstack_hideflags.getMask());
    }

    public static void appendEnchantmentNames(List<IChatBaseComponent> list, NBTTagList nbttaglist) {
        for (int i = 0; i < nbttaglist.size(); ++i) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompound(i);

            IRegistry.ENCHANTMENT.getOptional(EnchantmentManager.getEnchantmentId(nbttagcompound)).ifPresent((enchantment) -> {
                list.add(enchantment.getFullname(EnchantmentManager.getEnchantmentLevel(nbttagcompound)));
            });
        }

    }

    private static Collection<IChatBaseComponent> expandBlockState(String s) {
        try {
            ArgumentBlock argumentblock = (new ArgumentBlock(new StringReader(s), true)).parse(true);
            IBlockData iblockdata = argumentblock.getState();
            MinecraftKey minecraftkey = argumentblock.getTag();
            boolean flag = iblockdata != null;
            boolean flag1 = minecraftkey != null;

            if (flag || flag1) {
                if (flag) {
                    return Lists.newArrayList(new IChatBaseComponent[]{iblockdata.getBlock().getName().withStyle(EnumChatFormat.DARK_GRAY)});
                }

                Tag<Block> tag = TagsBlock.getAllTags().getTag(minecraftkey);

                if (tag != null) {
                    Collection<Block> collection = tag.getValues();

                    if (!collection.isEmpty()) {
                        return (Collection) collection.stream().map(Block::getName).map((ichatmutablecomponent) -> {
                            return ichatmutablecomponent.withStyle(EnumChatFormat.DARK_GRAY);
                        }).collect(Collectors.toList());
                    }
                }
            }
        } catch (CommandSyntaxException commandsyntaxexception) {
            ;
        }

        return Lists.newArrayList(new IChatBaseComponent[]{(new ChatComponentText("missingno")).withStyle(EnumChatFormat.DARK_GRAY)});
    }

    public boolean hasFoil() {
        return this.getItem().isFoil(this);
    }

    public EnumItemRarity getRarity() {
        return this.getItem().getRarity(this);
    }

    public boolean isEnchantable() {
        return !this.getItem().isEnchantable(this) ? false : !this.isEnchanted();
    }

    public void enchant(Enchantment enchantment, int i) {
        this.getOrCreateTag();
        if (!this.tag.contains("Enchantments", 9)) {
            this.tag.put("Enchantments", new NBTTagList());
        }

        NBTTagList nbttaglist = this.tag.getList("Enchantments", 10);

        nbttaglist.add(EnchantmentManager.storeEnchantment(EnchantmentManager.getEnchantmentId(enchantment), (byte) i));
    }

    public boolean isEnchanted() {
        return this.tag != null && this.tag.contains("Enchantments", 9) ? !this.tag.getList("Enchantments", 10).isEmpty() : false;
    }

    public void addTagElement(String s, NBTBase nbtbase) {
        this.getOrCreateTag().put(s, nbtbase);
    }

    public boolean isFramed() {
        return this.entityRepresentation instanceof EntityItemFrame;
    }

    public void setEntityRepresentation(@Nullable Entity entity) {
        this.entityRepresentation = entity;
    }

    @Nullable
    public EntityItemFrame getFrame() {
        return this.entityRepresentation instanceof EntityItemFrame ? (EntityItemFrame) this.getEntityRepresentation() : null;
    }

    @Nullable
    public Entity getEntityRepresentation() {
        return !this.emptyCacheFlag ? this.entityRepresentation : null;
    }

    public int getBaseRepairCost() {
        return this.hasTag() && this.tag.contains("RepairCost", 3) ? this.tag.getInt("RepairCost") : 0;
    }

    public void setRepairCost(int i) {
        // CraftBukkit start - remove RepairCost tag when 0 (SPIGOT-3945)
        if (i == 0) {
            this.removeTagKey("RepairCost");
            return;
        }
        // CraftBukkit end
        this.getOrCreateTag().putInt("RepairCost", i);
    }

    public Multimap<AttributeBase, AttributeModifier> getAttributeModifiers(EnumItemSlot enumitemslot) {
        Object object;

        if (this.hasTag() && this.tag.contains("AttributeModifiers", 9)) {
            object = HashMultimap.create();
            NBTTagList nbttaglist = this.tag.getList("AttributeModifiers", 10);

            for (int i = 0; i < nbttaglist.size(); ++i) {
                NBTTagCompound nbttagcompound = nbttaglist.getCompound(i);

                if (!nbttagcompound.contains("Slot", 8) || nbttagcompound.getString("Slot").equals(enumitemslot.getName())) {
                    Optional<AttributeBase> optional = IRegistry.ATTRIBUTE.getOptional(MinecraftKey.tryParse(nbttagcompound.getString("AttributeName")));

                    if (optional.isPresent()) {
                        AttributeModifier attributemodifier = AttributeModifier.load(nbttagcompound);

                        if (attributemodifier != null && attributemodifier.getId().getLeastSignificantBits() != 0L && attributemodifier.getId().getMostSignificantBits() != 0L) {
                            ((Multimap) object).put((AttributeBase) optional.get(), attributemodifier);
                        }
                    }
                }
            }
        } else {
            object = this.getItem().getDefaultAttributeModifiers(enumitemslot);
        }

        return (Multimap) object;
    }

    public void addAttributeModifier(AttributeBase attributebase, AttributeModifier attributemodifier, @Nullable EnumItemSlot enumitemslot) {
        this.getOrCreateTag();
        if (!this.tag.contains("AttributeModifiers", 9)) {
            this.tag.put("AttributeModifiers", new NBTTagList());
        }

        NBTTagList nbttaglist = this.tag.getList("AttributeModifiers", 10);
        NBTTagCompound nbttagcompound = attributemodifier.save();

        nbttagcompound.putString("AttributeName", IRegistry.ATTRIBUTE.getKey(attributebase).toString());
        if (enumitemslot != null) {
            nbttagcompound.putString("Slot", enumitemslot.getName());
        }

        nbttaglist.add(nbttagcompound);
    }

    // CraftBukkit start
    @Deprecated
    public void setItem(Item item) {
        this.item = item;
    }
    // CraftBukkit end

    public IChatBaseComponent getDisplayName() {
        IChatMutableComponent ichatmutablecomponent = (new ChatComponentText("")).append(this.getHoverName());

        if (this.hasCustomHoverName()) {
            ichatmutablecomponent.withStyle(EnumChatFormat.ITALIC);
        }

        IChatMutableComponent ichatmutablecomponent1 = ChatComponentUtils.wrapInSquareBrackets(ichatmutablecomponent);

        if (!this.emptyCacheFlag) {
            ichatmutablecomponent1.withStyle(this.getRarity().color).withStyle((chatmodifier) -> {
                return chatmodifier.withHoverEvent(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_ITEM, new ChatHoverable.c(this)));
            });
        }

        return ichatmutablecomponent1;
    }

    public boolean hasAdventureModePlaceTagForBlock(ITagRegistry itagregistry, ShapeDetectorBlock shapedetectorblock) {
        if (this.adventurePlaceCheck == null) {
            this.adventurePlaceCheck = new AdventureModeCheck("CanPlaceOn");
        }

        return this.adventurePlaceCheck.test(this, itagregistry, shapedetectorblock);
    }

    public boolean hasAdventureModeBreakTagForBlock(ITagRegistry itagregistry, ShapeDetectorBlock shapedetectorblock) {
        if (this.adventureBreakCheck == null) {
            this.adventureBreakCheck = new AdventureModeCheck("CanDestroy");
        }

        return this.adventureBreakCheck.test(this, itagregistry, shapedetectorblock);
    }

    public int getPopTime() {
        return this.popTime;
    }

    public void setPopTime(int i) {
        this.popTime = i;
    }

    public int getCount() {
        return this.emptyCacheFlag ? 0 : this.count;
    }

    public void setCount(int i) {
        this.count = i;
        this.updateEmptyCacheFlag();
    }

    public void grow(int i) {
        this.setCount(this.count + i);
    }

    public void shrink(int i) {
        this.grow(-i);
    }

    public void onUseTick(World world, EntityLiving entityliving, int i) {
        this.getItem().onUseTick(world, entityliving, this, i);
    }

    public void onDestroyed(EntityItem entityitem) {
        this.getItem().onDestroyed(entityitem);
    }

    public boolean isEdible() {
        return this.getItem().isEdible();
    }

    public SoundEffect getDrinkingSound() {
        return this.getItem().getDrinkingSound();
    }

    public SoundEffect getEatingSound() {
        return this.getItem().getEatingSound();
    }

    @Nullable
    public SoundEffect getEquipSound() {
        return this.getItem().getEquipSound();
    }

    public static enum HideFlags {

        ENCHANTMENTS, MODIFIERS, UNBREAKABLE, CAN_DESTROY, CAN_PLACE, ADDITIONAL, DYE;

        private final int mask = 1 << this.ordinal();

        private HideFlags() {}

        public int getMask() {
            return this.mask;
        }
    }
}
