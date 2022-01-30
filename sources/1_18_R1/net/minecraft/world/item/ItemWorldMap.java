package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MaterialMapColor;
import net.minecraft.world.level.saveddata.maps.WorldMap;

// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.event.server.MapInitializeEvent;
// CraftBukkit end

public class ItemWorldMap extends ItemWorldMapBase {

    public static final int IMAGE_WIDTH = 128;
    public static final int IMAGE_HEIGHT = 128;
    private static final int DEFAULT_MAP_COLOR = -12173266;
    private static final String TAG_MAP = "map";

    public ItemWorldMap(Item.Info item_info) {
        super(item_info);
    }

    public static ItemStack create(World world, int i, int j, byte b0, boolean flag, boolean flag1) {
        ItemStack itemstack = new ItemStack(Items.FILLED_MAP);

        createAndStoreSavedData(itemstack, world, i, j, b0, flag, flag1, world.dimension());
        return itemstack;
    }

    @Nullable
    public static WorldMap getSavedData(@Nullable Integer integer, World world) {
        return integer == null ? null : world.getMapData(makeKey(integer));
    }

    @Nullable
    public static WorldMap getSavedData(ItemStack itemstack, World world) {
        Integer integer = getMapId(itemstack);

        return getSavedData(integer, world);
    }

    @Nullable
    public static Integer getMapId(ItemStack itemstack) {
        NBTTagCompound nbttagcompound = itemstack.getTag();

        return nbttagcompound != null && nbttagcompound.contains("map", 99) ? nbttagcompound.getInt("map") : -1; // CraftBukkit - make new maps for no tag
    }

    public static int createNewSavedData(World world, int i, int j, int k, boolean flag, boolean flag1, ResourceKey<World> resourcekey) {
        WorldMap worldmap = WorldMap.createFresh((double) i, (double) j, (byte) k, flag, flag1, resourcekey);
        int l = world.getFreeMapId();

        world.setMapData(makeKey(l), worldmap);
        // CraftBukkit start
        MapInitializeEvent event = new MapInitializeEvent(worldmap.mapView);
        Bukkit.getServer().getPluginManager().callEvent(event);
        // CraftBukkit end
        return l;
    }

    private static void storeMapData(ItemStack itemstack, int i) {
        itemstack.getOrCreateTag().putInt("map", i);
    }

    private static void createAndStoreSavedData(ItemStack itemstack, World world, int i, int j, int k, boolean flag, boolean flag1, ResourceKey<World> resourcekey) {
        int l = createNewSavedData(world, i, j, k, flag, flag1, resourcekey);

        storeMapData(itemstack, l);
    }

    public static String makeKey(int i) {
        return "map_" + i;
    }

    public void update(World world, Entity entity, WorldMap worldmap) {
        if (world.dimension() == worldmap.dimension && entity instanceof EntityHuman) {
            int i = 1 << worldmap.scale;
            int j = worldmap.x;
            int k = worldmap.z;
            int l = MathHelper.floor(entity.getX() - (double) j) / i + 64;
            int i1 = MathHelper.floor(entity.getZ() - (double) k) / i + 64;
            int j1 = 128 / i;

            if (world.dimensionType().hasCeiling()) {
                j1 /= 2;
            }

            WorldMap.WorldMapHumanTracker worldmap_worldmaphumantracker = worldmap.getHoldingPlayer((EntityHuman) entity);

            ++worldmap_worldmaphumantracker.step;
            boolean flag = false;

            for (int k1 = l - j1 + 1; k1 < l + j1; ++k1) {
                if ((k1 & 15) == (worldmap_worldmaphumantracker.step & 15) || flag) {
                    flag = false;
                    double d0 = 0.0D;

                    for (int l1 = i1 - j1 - 1; l1 < i1 + j1; ++l1) {
                        if (k1 >= 0 && l1 >= -1 && k1 < 128 && l1 < 128) {
                            int i2 = k1 - l;
                            int j2 = l1 - i1;
                            boolean flag1 = i2 * i2 + j2 * j2 > (j1 - 2) * (j1 - 2);
                            int k2 = (j / i + k1 - 64) * i;
                            int l2 = (k / i + l1 - 64) * i;
                            Multiset<MaterialMapColor> multiset = LinkedHashMultiset.create();
                            Chunk chunk = world.getChunkAt(new BlockPosition(k2, 0, l2));

                            if (!chunk.isEmpty()) {
                                ChunkCoordIntPair chunkcoordintpair = chunk.getPos();
                                int i3 = k2 & 15;
                                int j3 = l2 & 15;
                                int k3 = 0;
                                double d1 = 0.0D;

                                if (world.dimensionType().hasCeiling()) {
                                    int l3 = k2 + l2 * 231871;

                                    l3 = l3 * l3 * 31287121 + l3 * 11;
                                    if ((l3 >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(world, BlockPosition.ZERO), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.defaultBlockState().getMapColor(world, BlockPosition.ZERO), 100);
                                    }

                                    d1 = 100.0D;
                                } else {
                                    BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();
                                    BlockPosition.MutableBlockPosition blockposition_mutableblockposition1 = new BlockPosition.MutableBlockPosition();

                                    for (int i4 = 0; i4 < i; ++i4) {
                                        for (int j4 = 0; j4 < i; ++j4) {
                                            int k4 = chunk.getHeight(HeightMap.Type.WORLD_SURFACE, i4 + i3, j4 + j3) + 1;
                                            IBlockData iblockdata;

                                            if (k4 > world.getMinBuildHeight() + 1) {
                                                do {
                                                    --k4;
                                                    blockposition_mutableblockposition.set(chunkcoordintpair.getMinBlockX() + i4 + i3, k4, chunkcoordintpair.getMinBlockZ() + j4 + j3);
                                                    iblockdata = chunk.getBlockState(blockposition_mutableblockposition);
                                                } while (iblockdata.getMapColor(world, blockposition_mutableblockposition) == MaterialMapColor.NONE && k4 > world.getMinBuildHeight());

                                                if (k4 > world.getMinBuildHeight() && !iblockdata.getFluidState().isEmpty()) {
                                                    int l4 = k4 - 1;

                                                    blockposition_mutableblockposition1.set(blockposition_mutableblockposition);

                                                    IBlockData iblockdata1;

                                                    do {
                                                        blockposition_mutableblockposition1.setY(l4--);
                                                        iblockdata1 = chunk.getBlockState(blockposition_mutableblockposition1);
                                                        ++k3;
                                                    } while (l4 > world.getMinBuildHeight() && !iblockdata1.getFluidState().isEmpty());

                                                    iblockdata = this.getCorrectStateForFluidBlock(world, iblockdata, blockposition_mutableblockposition);
                                                }
                                            } else {
                                                iblockdata = Blocks.BEDROCK.defaultBlockState();
                                            }

                                            worldmap.checkBanners(world, chunkcoordintpair.getMinBlockX() + i4 + i3, chunkcoordintpair.getMinBlockZ() + j4 + j3);
                                            d1 += (double) k4 / (double) (i * i);
                                            multiset.add(iblockdata.getMapColor(world, blockposition_mutableblockposition));
                                        }
                                    }
                                }

                                k3 /= i * i;
                                MaterialMapColor materialmapcolor = (MaterialMapColor) Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MaterialMapColor.NONE);
                                double d2;
                                MaterialMapColor.a materialmapcolor_a;

                                if (materialmapcolor == MaterialMapColor.WATER) {
                                    d2 = (double) k3 * 0.1D + (double) (k1 + l1 & 1) * 0.2D;
                                    if (d2 < 0.5D) {
                                        materialmapcolor_a = MaterialMapColor.a.HIGH;
                                    } else if (d2 > 0.9D) {
                                        materialmapcolor_a = MaterialMapColor.a.LOW;
                                    } else {
                                        materialmapcolor_a = MaterialMapColor.a.NORMAL;
                                    }
                                } else {
                                    d2 = (d1 - d0) * 4.0D / (double) (i + 4) + ((double) (k1 + l1 & 1) - 0.5D) * 0.4D;
                                    if (d2 > 0.6D) {
                                        materialmapcolor_a = MaterialMapColor.a.HIGH;
                                    } else if (d2 < -0.6D) {
                                        materialmapcolor_a = MaterialMapColor.a.LOW;
                                    } else {
                                        materialmapcolor_a = MaterialMapColor.a.NORMAL;
                                    }
                                }

                                d0 = d1;
                                if (l1 >= 0 && i2 * i2 + j2 * j2 < j1 * j1 && (!flag1 || (k1 + l1 & 1) != 0)) {
                                    flag |= worldmap.updateColor(k1, l1, materialmapcolor.getPackedId(materialmapcolor_a));
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private IBlockData getCorrectStateForFluidBlock(World world, IBlockData iblockdata, BlockPosition blockposition) {
        Fluid fluid = iblockdata.getFluidState();

        return !fluid.isEmpty() && !iblockdata.isFaceSturdy(world, blockposition, EnumDirection.UP) ? fluid.createLegacyBlock() : iblockdata;
    }

    private static boolean isBiomeWatery(boolean[] aboolean, int i, int j, int k) {
        return aboolean[j * i + k * i * 128 * i];
    }

    public static void renderBiomePreviewMap(WorldServer worldserver, ItemStack itemstack) {
        WorldMap worldmap = getSavedData(itemstack, worldserver);

        if (worldmap != null) {
            if (worldserver.dimension() == worldmap.dimension) {
                int i = 1 << worldmap.scale;
                int j = worldmap.x;
                int k = worldmap.z;
                boolean[] aboolean = new boolean[128 * i * 128 * i];

                int l;
                int i1;

                for (l = 0; l < 128 * i; ++l) {
                    for (i1 = 0; i1 < 128 * i; ++i1) {
                        BiomeBase.Geography biomebase_geography = worldserver.getBiome(new BlockPosition((j / i - 64) * i + i1, 0, (k / i - 64) * i + l)).getBiomeCategory();

                        aboolean[l * 128 * i + i1] = biomebase_geography == BiomeBase.Geography.OCEAN || biomebase_geography == BiomeBase.Geography.RIVER || biomebase_geography == BiomeBase.Geography.SWAMP;
                    }
                }

                for (l = 0; l < 128; ++l) {
                    for (i1 = 0; i1 < 128; ++i1) {
                        if (l > 0 && i1 > 0 && l < 127 && i1 < 127) {
                            int j1 = 8;

                            if (!isBiomeWatery(aboolean, i, l - 1, i1 - 1)) {
                                --j1;
                            }

                            if (!isBiomeWatery(aboolean, i, l - 1, i1 + 1)) {
                                --j1;
                            }

                            if (!isBiomeWatery(aboolean, i, l - 1, i1)) {
                                --j1;
                            }

                            if (!isBiomeWatery(aboolean, i, l + 1, i1 - 1)) {
                                --j1;
                            }

                            if (!isBiomeWatery(aboolean, i, l + 1, i1 + 1)) {
                                --j1;
                            }

                            if (!isBiomeWatery(aboolean, i, l + 1, i1)) {
                                --j1;
                            }

                            if (!isBiomeWatery(aboolean, i, l, i1 - 1)) {
                                --j1;
                            }

                            if (!isBiomeWatery(aboolean, i, l, i1 + 1)) {
                                --j1;
                            }

                            MaterialMapColor.a materialmapcolor_a = MaterialMapColor.a.LOWEST;
                            MaterialMapColor materialmapcolor = MaterialMapColor.NONE;

                            if (isBiomeWatery(aboolean, i, l, i1)) {
                                materialmapcolor = MaterialMapColor.COLOR_ORANGE;
                                if (j1 > 7 && i1 % 2 == 0) {
                                    switch ((l + (int) (MathHelper.sin((float) i1 + 0.0F) * 7.0F)) / 8 % 5) {
                                        case 0:
                                        case 4:
                                            materialmapcolor_a = MaterialMapColor.a.LOW;
                                            break;
                                        case 1:
                                        case 3:
                                            materialmapcolor_a = MaterialMapColor.a.NORMAL;
                                            break;
                                        case 2:
                                            materialmapcolor_a = MaterialMapColor.a.HIGH;
                                    }
                                } else if (j1 > 7) {
                                    materialmapcolor = MaterialMapColor.NONE;
                                } else if (j1 > 5) {
                                    materialmapcolor_a = MaterialMapColor.a.NORMAL;
                                } else if (j1 > 3) {
                                    materialmapcolor_a = MaterialMapColor.a.LOW;
                                } else if (j1 > 1) {
                                    materialmapcolor_a = MaterialMapColor.a.LOW;
                                }
                            } else if (j1 > 0) {
                                materialmapcolor = MaterialMapColor.COLOR_BROWN;
                                if (j1 > 3) {
                                    materialmapcolor_a = MaterialMapColor.a.NORMAL;
                                } else {
                                    materialmapcolor_a = MaterialMapColor.a.LOWEST;
                                }
                            }

                            if (materialmapcolor != MaterialMapColor.NONE) {
                                worldmap.setColor(l, i1, materialmapcolor.getPackedId(materialmapcolor_a));
                            }
                        }
                    }
                }

            }
        }
    }

    @Override
    public void inventoryTick(ItemStack itemstack, World world, Entity entity, int i, boolean flag) {
        if (!world.isClientSide) {
            WorldMap worldmap = getSavedData(itemstack, world);

            if (worldmap != null) {
                if (entity instanceof EntityHuman) {
                    EntityHuman entityhuman = (EntityHuman) entity;

                    worldmap.tickCarriedBy(entityhuman, itemstack);
                }

                if (!worldmap.locked && (flag || entity instanceof EntityHuman && ((EntityHuman) entity).getOffhandItem() == itemstack)) {
                    this.update(world, entity, worldmap);
                }

            }
        }
    }

    @Nullable
    @Override
    public Packet<?> getUpdatePacket(ItemStack itemstack, World world, EntityHuman entityhuman) {
        Integer integer = getMapId(itemstack);
        WorldMap worldmap = getSavedData(integer, world);

        return worldmap != null ? worldmap.getUpdatePacket(integer, entityhuman) : null;
    }

    @Override
    public void onCraftedBy(ItemStack itemstack, World world, EntityHuman entityhuman) {
        NBTTagCompound nbttagcompound = itemstack.getTag();

        if (nbttagcompound != null && nbttagcompound.contains("map_scale_direction", 99)) {
            scaleMap(itemstack, world, nbttagcompound.getInt("map_scale_direction"));
            nbttagcompound.remove("map_scale_direction");
        } else if (nbttagcompound != null && nbttagcompound.contains("map_to_lock", 1) && nbttagcompound.getBoolean("map_to_lock")) {
            lockMap(world, itemstack);
            nbttagcompound.remove("map_to_lock");
        }

    }

    private static void scaleMap(ItemStack itemstack, World world, int i) {
        WorldMap worldmap = getSavedData(itemstack, world);

        if (worldmap != null) {
            int j = world.getFreeMapId();

            world.setMapData(makeKey(j), worldmap.scaled(i));
            storeMapData(itemstack, j);
        }

    }

    public static void lockMap(World world, ItemStack itemstack) {
        WorldMap worldmap = getSavedData(itemstack, world);

        if (worldmap != null) {
            int i = world.getFreeMapId();
            String s = makeKey(i);
            WorldMap worldmap1 = worldmap.locked();

            world.setMapData(s, worldmap1);
            storeMapData(itemstack, i);
        }

    }

    @Override
    public void appendHoverText(ItemStack itemstack, @Nullable World world, List<IChatBaseComponent> list, TooltipFlag tooltipflag) {
        Integer integer = getMapId(itemstack);
        WorldMap worldmap = world == null ? null : getSavedData(integer, world);

        if (worldmap != null && worldmap.locked) {
            list.add((new ChatMessage("filled_map.locked", new Object[]{integer})).withStyle(EnumChatFormat.GRAY));
        }

        if (tooltipflag.isAdvanced()) {
            if (worldmap != null) {
                list.add((new ChatMessage("filled_map.id", new Object[]{integer})).withStyle(EnumChatFormat.GRAY));
                list.add((new ChatMessage("filled_map.scale", new Object[]{1 << worldmap.scale})).withStyle(EnumChatFormat.GRAY));
                list.add((new ChatMessage("filled_map.level", new Object[]{worldmap.scale, 4})).withStyle(EnumChatFormat.GRAY));
            } else {
                list.add((new ChatMessage("filled_map.unknown")).withStyle(EnumChatFormat.GRAY));
            }
        }

    }

    public static int getColor(ItemStack itemstack) {
        NBTTagCompound nbttagcompound = itemstack.getTagElement("display");

        if (nbttagcompound != null && nbttagcompound.contains("MapColor", 99)) {
            int i = nbttagcompound.getInt("MapColor");

            return -16777216 | i & 16777215;
        } else {
            return -12173266;
        }
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext itemactioncontext) {
        IBlockData iblockdata = itemactioncontext.getLevel().getBlockState(itemactioncontext.getClickedPos());

        if (iblockdata.is((Tag) TagsBlock.BANNERS)) {
            if (!itemactioncontext.getLevel().isClientSide) {
                WorldMap worldmap = getSavedData(itemactioncontext.getItemInHand(), itemactioncontext.getLevel());

                if (worldmap != null && !worldmap.toggleBanner(itemactioncontext.getLevel(), itemactioncontext.getClickedPos())) {
                    return EnumInteractionResult.FAIL;
                }
            }

            return EnumInteractionResult.sidedSuccess(itemactioncontext.getLevel().isClientSide);
        } else {
            return super.useOn(itemactioncontext);
        }
    }
}
