package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.util.MathHelper;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EntityComplexPart;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.WorldDataMutable;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.level.border.IWorldBorderListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.craftbukkit.block.CapturedBlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.GenericGameEvent;
// CraftBukkit end

public abstract class World implements GeneratorAccess, AutoCloseable {

    protected static final Logger LOGGER = LogManager.getLogger();
    public static final Codec<ResourceKey<World>> RESOURCE_KEY_CODEC = MinecraftKey.CODEC.xmap(ResourceKey.b(IRegistry.DIMENSION_REGISTRY), ResourceKey::a);
    public static final ResourceKey<World> OVERWORLD = ResourceKey.a(IRegistry.DIMENSION_REGISTRY, new MinecraftKey("overworld"));
    public static final ResourceKey<World> NETHER = ResourceKey.a(IRegistry.DIMENSION_REGISTRY, new MinecraftKey("the_nether"));
    public static final ResourceKey<World> END = ResourceKey.a(IRegistry.DIMENSION_REGISTRY, new MinecraftKey("the_end"));
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final int LONG_PARTICLE_CLIP_RANGE = 512;
    public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    public static final int MAX_BRIGHTNESS = 15;
    public static final int TICKS_PER_DAY = 24000;
    public static final int MAX_ENTITY_SPAWN_Y = 20000000;
    public static final int MIN_ENTITY_SPAWN_Y = -20000000;
    protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
    private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
    private boolean tickingBlockEntities;
    public final Thread thread;
    private final boolean isDebug;
    private int skyDarken;
    protected int randValue = (new Random()).nextInt();
    protected final int addend = 1013904223;
    protected float oRainLevel;
    public float rainLevel;
    protected float oThunderLevel;
    public float thunderLevel;
    public final Random random = new Random();
    private final DimensionManager dimensionType;
    public final WorldDataMutable levelData;
    private final Supplier<GameProfilerFiller> profiler;
    public final boolean isClientSide;
    private final WorldBorder worldBorder;
    private final BiomeManager biomeManager;
    private final ResourceKey<World> dimension;

    // CraftBukkit start Added the following
    private final ResourceKey<DimensionManager> typeKey;
    private final CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public org.bukkit.generator.ChunkGenerator generator;

    public boolean preventPoiUpdated = false; // CraftBukkit - SPIGOT-5710
    public boolean captureBlockStates = false;
    public boolean captureTreeGeneration = false;
    public Map<BlockPosition, CapturedBlockState> capturedBlockStates = new java.util.LinkedHashMap<>();
    public Map<BlockPosition, TileEntity> capturedTileEntities = new HashMap<>();
    public List<EntityItem> captureDrops;
    public long ticksPerAnimalSpawns;
    public long ticksPerMonsterSpawns;
    public long ticksPerWaterSpawns;
    public long ticksPerWaterAmbientSpawns;
    public long ticksPerWaterUndergroundCreatureSpawns;
    public long ticksPerAmbientSpawns;
    public boolean populating;
    public final org.spigotmc.SpigotWorldConfig spigotConfig; // Spigot

    public final SpigotTimings.WorldTimingsHandler timings; // Spigot
    public static BlockPosition lastPhysicsProblem; // Spigot
    private org.spigotmc.TickLimiter entityLimiter;
    private org.spigotmc.TickLimiter tileLimiter;
    private int tileTickPosition;

    public CraftWorld getWorld() {
        return this.world;
    }

    public CraftServer getCraftServer() {
        return (CraftServer) Bukkit.getServer();
    }

    public ResourceKey<DimensionManager> getTypeKey() {
        return typeKey;
    }

    protected World(WorldDataMutable worlddatamutable, ResourceKey<World> resourcekey, final DimensionManager dimensionmanager, Supplier<GameProfilerFiller> supplier, boolean flag, boolean flag1, long i, org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider, org.bukkit.World.Environment env) {
        this.spigotConfig = new org.spigotmc.SpigotWorldConfig(((net.minecraft.world.level.storage.WorldDataServer) worlddatamutable).getName()); // Spigot
        this.generator = gen;
        this.world = new CraftWorld((WorldServer) this, gen, biomeProvider, env);
        this.ticksPerAnimalSpawns = this.getCraftServer().getTicksPerAnimalSpawns(); // CraftBukkit
        this.ticksPerMonsterSpawns = this.getCraftServer().getTicksPerMonsterSpawns(); // CraftBukkit
        this.ticksPerWaterSpawns = this.getCraftServer().getTicksPerWaterSpawns(); // CraftBukkit
        this.ticksPerWaterAmbientSpawns = this.getCraftServer().getTicksPerWaterAmbientSpawns(); // CraftBukkit
        this.ticksPerWaterUndergroundCreatureSpawns = this.getCraftServer().getTicksPerWaterUndergroundCreatureSpawns(); // CraftBukkit
        this.ticksPerAmbientSpawns = this.getCraftServer().getTicksPerAmbientSpawns(); // CraftBukkit
        this.typeKey = (ResourceKey) this.getCraftServer().getHandle().getServer().registryHolder.d(IRegistry.DIMENSION_TYPE_REGISTRY).c(dimensionmanager).orElseThrow(() -> {
            return new IllegalStateException("Unregistered dimension type: " + dimensionmanager);
        });
        // CraftBukkit end
        this.profiler = supplier;
        this.levelData = worlddatamutable;
        this.dimensionType = dimensionmanager;
        this.dimension = resourcekey;
        this.isClientSide = flag;
        if (dimensionmanager.getCoordinateScale() != 1.0D) {
            this.worldBorder = new WorldBorder() {
                @Override
                public double getCenterX() {
                    return super.getCenterX(); // CraftBukkit
                }

                @Override
                public double getCenterZ() {
                    return super.getCenterZ(); // CraftBukkit
                }
            };
        } else {
            this.worldBorder = new WorldBorder();
        }

        this.thread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, i, dimensionmanager.getGenLayerZoomer());
        this.isDebug = flag1;
        // CraftBukkit start
        getWorldBorder().world = (WorldServer) this;
        // From PlayerList.setPlayerFileData
        getWorldBorder().a(new IWorldBorderListener() {
            public void a(WorldBorder worldborder, double d0) {
                getCraftServer().getHandle().sendAll(new ClientboundSetBorderSizePacket(worldborder), worldborder.world);
            }

            public void a(WorldBorder worldborder, double d0, double d1, long i) {
                getCraftServer().getHandle().sendAll(new ClientboundSetBorderLerpSizePacket(worldborder), worldborder.world);
            }

            public void a(WorldBorder worldborder, double d0, double d1) {
                getCraftServer().getHandle().sendAll(new ClientboundSetBorderCenterPacket(worldborder), worldborder.world);
            }

            public void a(WorldBorder worldborder, int i) {
                getCraftServer().getHandle().sendAll(new ClientboundSetBorderWarningDelayPacket(worldborder), worldborder.world);
            }

            public void b(WorldBorder worldborder, int i) {
                getCraftServer().getHandle().sendAll(new ClientboundSetBorderWarningDistancePacket(worldborder), worldborder.world);
            }

            public void b(WorldBorder worldborder, double d0) {}

            public void c(WorldBorder worldborder, double d0) {}
        });
        // CraftBukkit end
        timings = new SpigotTimings.WorldTimingsHandler(this); // Spigot - code below can generate new world and access timings
        this.entityLimiter = new org.spigotmc.TickLimiter(spigotConfig.entityMaxTickTime);
        this.tileLimiter = new org.spigotmc.TickLimiter(spigotConfig.tileMaxTickTime);
    }

    @Override
    public boolean isClientSide() {
        return this.isClientSide;
    }

    @Nullable
    @Override
    public MinecraftServer getMinecraftServer() {
        return null;
    }

    public boolean isValidLocation(BlockPosition blockposition) {
        return !this.isOutsideWorld(blockposition) && E(blockposition);
    }

    public static boolean l(BlockPosition blockposition) {
        return !b(blockposition.getY()) && E(blockposition);
    }

    private static boolean E(BlockPosition blockposition) {
        return blockposition.getX() >= -30000000 && blockposition.getZ() >= -30000000 && blockposition.getX() < 30000000 && blockposition.getZ() < 30000000;
    }

    private static boolean b(int i) {
        return i < -20000000 || i >= 20000000;
    }

    public Chunk getChunkAtWorldCoords(BlockPosition blockposition) {
        return this.getChunkAt(SectionPosition.a(blockposition.getX()), SectionPosition.a(blockposition.getZ()));
    }

    @Override
    public Chunk getChunkAt(int i, int j) {
        return (Chunk) this.getChunkAt(i, j, ChunkStatus.FULL);
    }

    @Nullable
    @Override
    public IChunkAccess getChunkAt(int i, int j, ChunkStatus chunkstatus, boolean flag) {
        IChunkAccess ichunkaccess = this.getChunkProvider().getChunkAt(i, j, chunkstatus, flag);

        if (ichunkaccess == null && flag) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        } else {
            return ichunkaccess;
        }
    }

    @Override
    public boolean setTypeAndData(BlockPosition blockposition, IBlockData iblockdata, int i) {
        return this.a(blockposition, iblockdata, i, 512);
    }

    @Override
    public boolean a(BlockPosition blockposition, IBlockData iblockdata, int i, int j) {
        // CraftBukkit start - tree generation
        if (this.captureTreeGeneration) {
            CapturedBlockState blockstate = capturedBlockStates.get(blockposition);
            if (blockstate == null) {
                blockstate = CapturedBlockState.getTreeBlockState(this, blockposition, i);
                this.capturedBlockStates.put(blockposition.immutableCopy(), blockstate);
            }
            blockstate.setData(iblockdata);
            return true;
        }
        // CraftBukkit end
        if (this.isOutsideWorld(blockposition)) {
            return false;
        } else if (!this.isClientSide && this.isDebugWorld()) {
            return false;
        } else {
            Chunk chunk = this.getChunkAtWorldCoords(blockposition);
            Block block = iblockdata.getBlock();

            // CraftBukkit start - capture blockstates
            boolean captured = false;
            if (this.captureBlockStates && !this.capturedBlockStates.containsKey(blockposition)) {
                CapturedBlockState blockstate = CapturedBlockState.getBlockState(this, blockposition, i);
                this.capturedBlockStates.put(blockposition.immutableCopy(), blockstate);
                captured = true;
            }
            // CraftBukkit end

            IBlockData iblockdata1 = chunk.setType(blockposition, iblockdata, (i & 64) != 0, (i & 1024) == 0); // CraftBukkit custom NO_PLACE flag

            if (iblockdata1 == null) {
                // CraftBukkit start - remove blockstate if failed (or the same)
                if (this.captureBlockStates && captured) {
                    this.capturedBlockStates.remove(blockposition);
                }
                // CraftBukkit end
                return false;
            } else {
                IBlockData iblockdata2 = this.getType(blockposition);

                if ((i & 128) == 0 && iblockdata2 != iblockdata1 && (iblockdata2.b((IBlockAccess) this, blockposition) != iblockdata1.b((IBlockAccess) this, blockposition) || iblockdata2.f() != iblockdata1.f() || iblockdata2.e() || iblockdata1.e())) {
                    this.getMethodProfiler().enter("queueCheckLight");
                    this.getChunkProvider().getLightEngine().a(blockposition);
                    this.getMethodProfiler().exit();
                }

                /*
                if (iblockdata2 == iblockdata) {
                    if (iblockdata1 != iblockdata2) {
                        this.b(blockposition, iblockdata1, iblockdata2);
                    }

                    if ((i & 2) != 0 && (!this.isClientSide || (i & 4) == 0) && (this.isClientSide || chunk.getState() != null && chunk.getState().isAtLeast(PlayerChunk.State.TICKING))) {
                        this.notify(blockposition, iblockdata1, iblockdata, i);
                    }

                    if ((i & 1) != 0) {
                        this.update(blockposition, iblockdata1.getBlock());
                        if (!this.isClientSide && iblockdata.isComplexRedstone()) {
                            this.updateAdjacentComparators(blockposition, block);
                        }
                    }

                    if ((i & 16) == 0 && j > 0) {
                        int k = i & -34;

                        iblockdata1.b(this, blockposition, k, j - 1);
                        iblockdata.a((GeneratorAccess) this, blockposition, k, j - 1);
                        iblockdata.b(this, blockposition, k, j - 1);
                    }

                    this.a(blockposition, iblockdata1, iblockdata2);
                }
                */

                // CraftBukkit start
                if (!this.captureBlockStates) { // Don't notify clients or update physics while capturing blockstates
                    // Modularize client and physic updates
                    // Spigot start
                    try {
                        notifyAndUpdatePhysics(blockposition, chunk, iblockdata1, iblockdata, iblockdata2, i, j);
                    } catch (StackOverflowError ex) {
                        lastPhysicsProblem = new BlockPosition(blockposition);
                    }
                    // Spigot end
                }
                // CraftBukkit end

                return true;
            }
        }
    }

    // CraftBukkit start - Split off from above in order to directly send client and physic updates
    public void notifyAndUpdatePhysics(BlockPosition blockposition, Chunk chunk, IBlockData oldBlock, IBlockData newBlock, IBlockData actualBlock, int i, int j) {
        IBlockData iblockdata = newBlock;
        IBlockData iblockdata1 = oldBlock;
        IBlockData iblockdata2 = actualBlock;
        if (iblockdata2 == iblockdata) {
            if (iblockdata1 != iblockdata2) {
                this.b(blockposition, iblockdata1, iblockdata2);
            }

            if ((i & 2) != 0 && (!this.isClientSide || (i & 4) == 0) && (this.isClientSide || chunk == null || (chunk.getState() != null && chunk.getState().isAtLeast(PlayerChunk.State.TICKING)))) { // allow chunk to be null here as chunk.isReady() is false when we send our notification during block placement
                this.notify(blockposition, iblockdata1, iblockdata, i);
            }

            if ((i & 1) != 0) {
                this.update(blockposition, iblockdata1.getBlock());
                if (!this.isClientSide && iblockdata.isComplexRedstone()) {
                    this.updateAdjacentComparators(blockposition, newBlock.getBlock());
                }
            }

            if ((i & 16) == 0 && j > 0) {
                int k = i & -34;

                // CraftBukkit start
                iblockdata1.b(this, blockposition, k, j - 1); // Don't call an event for the old block to limit event spam
                CraftWorld world = ((WorldServer) this).getWorld();
                if (world != null) {
                    BlockPhysicsEvent event = new BlockPhysicsEvent(world.getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()), CraftBlockData.fromData(iblockdata));
                    this.getCraftServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return;
                    }
                }
                // CraftBukkit end
                iblockdata.a((GeneratorAccess) this, blockposition, k, j - 1);
                iblockdata.b(this, blockposition, k, j - 1);
            }

            // CraftBukkit start - SPIGOT-5710
            if (!preventPoiUpdated) {
                this.a(blockposition, iblockdata1, iblockdata2);
            }
            // CraftBukkit end
        }
    }
    // CraftBukkit end

    public void a(BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1) {}

    @Override
    public boolean a(BlockPosition blockposition, boolean flag) {
        Fluid fluid = this.getFluid(blockposition);

        return this.setTypeAndData(blockposition, fluid.getBlockData(), 3 | (flag ? 64 : 0));
    }

    @Override
    public boolean a(BlockPosition blockposition, boolean flag, @Nullable Entity entity, int i) {
        IBlockData iblockdata = this.getType(blockposition);

        if (iblockdata.isAir()) {
            return false;
        } else {
            Fluid fluid = this.getFluid(blockposition);

            if (!(iblockdata.getBlock() instanceof BlockFireAbstract)) {
                this.triggerEffect(2001, blockposition, Block.getCombinedId(iblockdata));
            }

            if (flag) {
                TileEntity tileentity = iblockdata.isTileEntity() ? this.getTileEntity(blockposition) : null;

                Block.dropItems(iblockdata, this, blockposition, tileentity, entity, ItemStack.EMPTY);
            }

            boolean flag1 = this.a(blockposition, fluid.getBlockData(), 3, i);

            if (flag1) {
                this.a(entity, GameEvent.BLOCK_DESTROY, blockposition);
            }

            return flag1;
        }
    }

    public void a(BlockPosition blockposition, IBlockData iblockdata) {}

    public boolean setTypeUpdate(BlockPosition blockposition, IBlockData iblockdata) {
        return this.setTypeAndData(blockposition, iblockdata, 3);
    }

    public abstract void notify(BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1, int i);

    public void b(BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1) {}

    public void applyPhysics(BlockPosition blockposition, Block block) {
        this.a(blockposition.west(), block, blockposition);
        this.a(blockposition.east(), block, blockposition);
        this.a(blockposition.down(), block, blockposition);
        this.a(blockposition.up(), block, blockposition);
        this.a(blockposition.north(), block, blockposition);
        this.a(blockposition.south(), block, blockposition);
    }

    public void a(BlockPosition blockposition, Block block, EnumDirection enumdirection) {
        if (enumdirection != EnumDirection.WEST) {
            this.a(blockposition.west(), block, blockposition);
        }

        if (enumdirection != EnumDirection.EAST) {
            this.a(blockposition.east(), block, blockposition);
        }

        if (enumdirection != EnumDirection.DOWN) {
            this.a(blockposition.down(), block, blockposition);
        }

        if (enumdirection != EnumDirection.UP) {
            this.a(blockposition.up(), block, blockposition);
        }

        if (enumdirection != EnumDirection.NORTH) {
            this.a(blockposition.north(), block, blockposition);
        }

        if (enumdirection != EnumDirection.SOUTH) {
            this.a(blockposition.south(), block, blockposition);
        }

    }

    public void a(BlockPosition blockposition, Block block, BlockPosition blockposition1) {
        if (!this.isClientSide) {
            IBlockData iblockdata = this.getType(blockposition);

            try {
                // CraftBukkit start
                CraftWorld world = ((WorldServer) this).getWorld();
                if (world != null) {
                    BlockPhysicsEvent event = new BlockPhysicsEvent(world.getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()), CraftBlockData.fromData(iblockdata), world.getBlockAt(blockposition1.getX(), blockposition1.getY(), blockposition1.getZ()));
                    this.getCraftServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return;
                    }
                }
                // CraftBukkit end
                iblockdata.doPhysics(this, blockposition, block, blockposition1, false);
            // Spigot Start
            } catch (StackOverflowError ex) {
                lastPhysicsProblem = new BlockPosition(blockposition);
                // Spigot End
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.a(throwable, "Exception while updating neighbours");
                CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Block being updated");

                crashreportsystemdetails.a("Source block type", () -> {
                    try {
                        return String.format("ID #%s (%s // %s)", IRegistry.BLOCK.getKey(block), block.h(), block.getClass().getCanonicalName());
                    } catch (Throwable throwable1) {
                        return "ID #" + IRegistry.BLOCK.getKey(block);
                    }
                });
                CrashReportSystemDetails.a(crashreportsystemdetails, this, blockposition, iblockdata);
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public int a(HeightMap.Type heightmap_type, int i, int j) {
        int k;

        if (i >= -30000000 && j >= -30000000 && i < 30000000 && j < 30000000) {
            if (this.isChunkLoaded(SectionPosition.a(i), SectionPosition.a(j))) {
                k = this.getChunkAt(SectionPosition.a(i), SectionPosition.a(j)).getHighestBlock(heightmap_type, i & 15, j & 15) + 1;
            } else {
                k = this.getMinBuildHeight();
            }
        } else {
            k = this.getSeaLevel() + 1;
        }

        return k;
    }

    @Override
    public LightEngine k_() {
        return this.getChunkProvider().getLightEngine();
    }

    @Override
    public IBlockData getType(BlockPosition blockposition) {
        // CraftBukkit start - tree generation
        if (captureTreeGeneration) {
            CapturedBlockState previous = capturedBlockStates.get(blockposition);
            if (previous != null) {
                return previous.getHandle();
            }
        }
        // CraftBukkit end
        if (this.isOutsideWorld(blockposition)) {
            return Blocks.VOID_AIR.getBlockData();
        } else {
            Chunk chunk = this.getChunkAt(SectionPosition.a(blockposition.getX()), SectionPosition.a(blockposition.getZ()));

            return chunk.getType(blockposition);
        }
    }

    @Override
    public Fluid getFluid(BlockPosition blockposition) {
        if (this.isOutsideWorld(blockposition)) {
            return FluidTypes.EMPTY.h();
        } else {
            Chunk chunk = this.getChunkAtWorldCoords(blockposition);

            return chunk.getFluid(blockposition);
        }
    }

    public boolean isDay() {
        return !this.getDimensionManager().isFixedTime() && this.skyDarken < 4;
    }

    public boolean isNight() {
        return !this.getDimensionManager().isFixedTime() && !this.isDay();
    }

    @Override
    public void playSound(@Nullable EntityHuman entityhuman, BlockPosition blockposition, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1) {
        this.playSound(entityhuman, (double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D, soundeffect, soundcategory, f, f1);
    }

    public abstract void playSound(@Nullable EntityHuman entityhuman, double d0, double d1, double d2, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1);

    public abstract void playSound(@Nullable EntityHuman entityhuman, Entity entity, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1);

    public void a(double d0, double d1, double d2, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1, boolean flag) {}

    @Override
    public void addParticle(ParticleParam particleparam, double d0, double d1, double d2, double d3, double d4, double d5) {}

    public void a(ParticleParam particleparam, boolean flag, double d0, double d1, double d2, double d3, double d4, double d5) {}

    public void b(ParticleParam particleparam, double d0, double d1, double d2, double d3, double d4, double d5) {}

    public void b(ParticleParam particleparam, boolean flag, double d0, double d1, double d2, double d3, double d4, double d5) {}

    public float a(float f) {
        float f1 = this.f(f);

        return f1 * 6.2831855F;
    }

    public void a(TickingBlockEntity tickingblockentity) {
        (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(tickingblockentity);
    }

    protected void tickBlockEntities() {
        GameProfilerFiller gameprofilerfiller = this.getMethodProfiler();

        gameprofilerfiller.enter("blockEntities");
        timings.tileEntityPending.startTiming(); // Spigot
        this.tickingBlockEntities = true;
        if (!this.pendingBlockEntityTickers.isEmpty()) {
            this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
            this.pendingBlockEntityTickers.clear();
        }
        timings.tileEntityPending.stopTiming(); // Spigot

        timings.tileEntityTick.startTiming(); // Spigot
        // Spigot start
        // Iterator iterator = this.blockEntityTickers.iterator();
        int tilesThisCycle = 0;
        for (tileLimiter.initTick();
                tilesThisCycle < blockEntityTickers.size() && (tilesThisCycle % 10 != 0 || tileLimiter.shouldContinue());
                tileTickPosition++, tilesThisCycle++) {
            tileTickPosition = (tileTickPosition < blockEntityTickers.size()) ? tileTickPosition : 0;
            TickingBlockEntity tickingblockentity = (TickingBlockEntity) this.blockEntityTickers.get(tileTickPosition);
            // Spigot start
            if (tickingblockentity == null) {
                getCraftServer().getLogger().severe("Spigot has detected a null entity and has removed it, preventing a crash");
                tilesThisCycle--;
                this.blockEntityTickers.remove(tileTickPosition--);
                continue;
            }
            // Spigot end

            if (tickingblockentity.b()) {
                // Spigot start
                tilesThisCycle--;
                this.blockEntityTickers.remove(tileTickPosition--);
                // Spigot end
            } else {
                tickingblockentity.a();
            }
        }

        timings.tileEntityTick.stopTiming(); // Spigot
        this.tickingBlockEntities = false;
        gameprofilerfiller.exit();
        spigotConfig.currentPrimedTnt = 0; // Spigot
    }

    public <T extends Entity> void a(Consumer<T> consumer, T t0) {
        try {
            SpigotTimings.tickEntityTimer.startTiming(); // Spigot
            consumer.accept(t0);
            SpigotTimings.tickEntityTimer.stopTiming(); // Spigot
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Ticking entity");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Entity being ticked");

            t0.appendEntityCrashDetails(crashreportsystemdetails);
            throw new ReportedException(crashreport);
        }
    }

    public Explosion explode(@Nullable Entity entity, double d0, double d1, double d2, float f, Explosion.Effect explosion_effect) {
        return this.createExplosion(entity, (DamageSource) null, (ExplosionDamageCalculator) null, d0, d1, d2, f, false, explosion_effect);
    }

    public Explosion createExplosion(@Nullable Entity entity, double d0, double d1, double d2, float f, boolean flag, Explosion.Effect explosion_effect) {
        return this.createExplosion(entity, (DamageSource) null, (ExplosionDamageCalculator) null, d0, d1, d2, f, flag, explosion_effect);
    }

    public Explosion createExplosion(@Nullable Entity entity, @Nullable DamageSource damagesource, @Nullable ExplosionDamageCalculator explosiondamagecalculator, double d0, double d1, double d2, float f, boolean flag, Explosion.Effect explosion_effect) {
        Explosion explosion = new Explosion(this, entity, damagesource, explosiondamagecalculator, d0, d1, d2, f, flag, explosion_effect);

        explosion.a();
        explosion.a(true);
        return explosion;
    }

    public abstract String J();

    @Nullable
    @Override
    // CraftBukkit start
    public TileEntity getTileEntity(BlockPosition blockposition) {
        return getTileEntity(blockposition, true);
    }

    @Nullable
    public TileEntity getTileEntity(BlockPosition blockposition, boolean validate) {
        if (capturedTileEntities.containsKey(blockposition)) {
            return capturedTileEntities.get(blockposition);
        }
        // CraftBukkit end
        return this.isOutsideWorld(blockposition) ? null : (!this.isClientSide && Thread.currentThread() != this.thread ? null : this.getChunkAtWorldCoords(blockposition).a(blockposition, Chunk.EnumTileEntityState.IMMEDIATE));
    }

    public void setTileEntity(TileEntity tileentity) {
        BlockPosition blockposition = tileentity.getPosition();

        if (!this.isOutsideWorld(blockposition)) {
            // CraftBukkit start
            if (captureBlockStates) {
                capturedTileEntities.put(blockposition.immutableCopy(), tileentity);
                return;
            }
            // CraftBukkit end
            this.getChunkAtWorldCoords(blockposition).b(tileentity);
        }
    }

    public void removeTileEntity(BlockPosition blockposition) {
        if (!this.isOutsideWorld(blockposition)) {
            this.getChunkAtWorldCoords(blockposition).removeTileEntity(blockposition);
        }
    }

    public boolean o(BlockPosition blockposition) {
        return this.isOutsideWorld(blockposition) ? false : this.getChunkProvider().isLoaded(SectionPosition.a(blockposition.getX()), SectionPosition.a(blockposition.getZ()));
    }

    public boolean a(BlockPosition blockposition, Entity entity, EnumDirection enumdirection) {
        if (this.isOutsideWorld(blockposition)) {
            return false;
        } else {
            IChunkAccess ichunkaccess = this.getChunkAt(SectionPosition.a(blockposition.getX()), SectionPosition.a(blockposition.getZ()), ChunkStatus.FULL, false);

            return ichunkaccess == null ? false : ichunkaccess.getType(blockposition).a((IBlockAccess) this, blockposition, entity, enumdirection);
        }
    }

    public boolean a(BlockPosition blockposition, Entity entity) {
        return this.a(blockposition, entity, EnumDirection.UP);
    }

    public void S() {
        double d0 = 1.0D - (double) (this.d(1.0F) * 5.0F) / 16.0D;
        double d1 = 1.0D - (double) (this.b(1.0F) * 5.0F) / 16.0D;
        double d2 = 0.5D + 2.0D * MathHelper.a((double) MathHelper.cos(this.f(1.0F) * 6.2831855F), -0.25D, 0.25D);

        this.skyDarken = (int) ((1.0D - d2 * d0 * d1) * 11.0D);
    }

    public void setSpawnFlags(boolean flag, boolean flag1) {
        this.getChunkProvider().a(flag, flag1);
    }

    protected void T() {
        if (this.levelData.hasStorm()) {
            this.rainLevel = 1.0F;
            if (this.levelData.isThundering()) {
                this.thunderLevel = 1.0F;
            }
        }

    }

    public void close() throws IOException {
        this.getChunkProvider().close();
    }

    @Nullable
    @Override
    public IBlockAccess c(int i, int j) {
        return this.getChunkAt(i, j, ChunkStatus.FULL, false);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AxisAlignedBB axisalignedbb, Predicate<? super Entity> predicate) {
        this.getMethodProfiler().c("getEntities");
        List<Entity> list = Lists.newArrayList();

        this.getEntities().a(axisalignedbb, (entity1) -> {
            if (entity1 != entity && predicate.test(entity1)) {
                list.add(entity1);
            }

            if (entity1 instanceof EntityEnderDragon) {
                EntityComplexPart[] aentitycomplexpart = ((EntityEnderDragon) entity1).t();
                int i = aentitycomplexpart.length;

                for (int j = 0; j < i; ++j) {
                    EntityComplexPart entitycomplexpart = aentitycomplexpart[j];

                    if (entity1 != entity && predicate.test(entitycomplexpart)) {
                        list.add(entitycomplexpart);
                    }
                }
            }

        });
        return list;
    }

    @Override
    public <T extends Entity> List<T> a(EntityTypeTest<Entity, T> entitytypetest, AxisAlignedBB axisalignedbb, Predicate<? super T> predicate) {
        this.getMethodProfiler().c("getEntities");
        List<T> list = Lists.newArrayList();

        this.getEntities().a(entitytypetest, axisalignedbb, (entity) -> {
            if (predicate.test(entity)) {
                list.add(entity);
            }

            if (entity instanceof EntityEnderDragon) {
                EntityComplexPart[] aentitycomplexpart = ((EntityEnderDragon) entity).t();
                int i = aentitycomplexpart.length;

                for (int j = 0; j < i; ++j) {
                    EntityComplexPart entitycomplexpart = aentitycomplexpart[j];
                    T t0 = entitytypetest.a(entitycomplexpart);

                    if (t0 != null && predicate.test(t0)) {
                        list.add(t0);
                    }
                }
            }

        });
        return list;
    }

    @Nullable
    public abstract Entity getEntity(int i);

    public void p(BlockPosition blockposition) {
        if (this.isLoaded(blockposition)) {
            this.getChunkAtWorldCoords(blockposition).markDirty();
        }

    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    public int getBlockPower(BlockPosition blockposition) {
        byte b0 = 0;
        int i = Math.max(b0, this.c(blockposition.down(), EnumDirection.DOWN));

        if (i >= 15) {
            return i;
        } else {
            i = Math.max(i, this.c(blockposition.up(), EnumDirection.UP));
            if (i >= 15) {
                return i;
            } else {
                i = Math.max(i, this.c(blockposition.north(), EnumDirection.NORTH));
                if (i >= 15) {
                    return i;
                } else {
                    i = Math.max(i, this.c(blockposition.south(), EnumDirection.SOUTH));
                    if (i >= 15) {
                        return i;
                    } else {
                        i = Math.max(i, this.c(blockposition.west(), EnumDirection.WEST));
                        if (i >= 15) {
                            return i;
                        } else {
                            i = Math.max(i, this.c(blockposition.east(), EnumDirection.EAST));
                            return i >= 15 ? i : i;
                        }
                    }
                }
            }
        }
    }

    public boolean isBlockFacePowered(BlockPosition blockposition, EnumDirection enumdirection) {
        return this.getBlockFacePower(blockposition, enumdirection) > 0;
    }

    public int getBlockFacePower(BlockPosition blockposition, EnumDirection enumdirection) {
        IBlockData iblockdata = this.getType(blockposition);
        int i = iblockdata.b((IBlockAccess) this, blockposition, enumdirection);

        return iblockdata.isOccluding(this, blockposition) ? Math.max(i, this.getBlockPower(blockposition)) : i;
    }

    public boolean isBlockIndirectlyPowered(BlockPosition blockposition) {
        return this.getBlockFacePower(blockposition.down(), EnumDirection.DOWN) > 0 ? true : (this.getBlockFacePower(blockposition.up(), EnumDirection.UP) > 0 ? true : (this.getBlockFacePower(blockposition.north(), EnumDirection.NORTH) > 0 ? true : (this.getBlockFacePower(blockposition.south(), EnumDirection.SOUTH) > 0 ? true : (this.getBlockFacePower(blockposition.west(), EnumDirection.WEST) > 0 ? true : this.getBlockFacePower(blockposition.east(), EnumDirection.EAST) > 0))));
    }

    public int s(BlockPosition blockposition) {
        int i = 0;
        EnumDirection[] aenumdirection = World.DIRECTIONS;
        int j = aenumdirection.length;

        for (int k = 0; k < j; ++k) {
            EnumDirection enumdirection = aenumdirection[k];
            int l = this.getBlockFacePower(blockposition.shift(enumdirection), enumdirection);

            if (l >= 15) {
                return 15;
            }

            if (l > i) {
                i = l;
            }
        }

        return i;
    }

    public void U() {}

    public long getTime() {
        return this.levelData.getTime();
    }

    public long getDayTime() {
        return this.levelData.getDayTime();
    }

    public boolean a(EntityHuman entityhuman, BlockPosition blockposition) {
        return true;
    }

    public void broadcastEntityEffect(Entity entity, byte b0) {}

    public void playBlockAction(BlockPosition blockposition, Block block, int i, int j) {
        this.getType(blockposition).a(this, blockposition, i, j);
    }

    @Override
    public WorldData getWorldData() {
        return this.levelData;
    }

    public GameRules getGameRules() {
        return this.levelData.q();
    }

    public float b(float f) {
        return MathHelper.h(f, this.oThunderLevel, this.thunderLevel) * this.d(f);
    }

    public void c(float f) {
        float f1 = MathHelper.a(f, 0.0F, 1.0F);

        this.oThunderLevel = f1;
        this.thunderLevel = f1;
    }

    public float d(float f) {
        return MathHelper.h(f, this.oRainLevel, this.rainLevel);
    }

    public void e(float f) {
        float f1 = MathHelper.a(f, 0.0F, 1.0F);

        this.oRainLevel = f1;
        this.rainLevel = f1;
    }

    public boolean Y() {
        return this.getDimensionManager().hasSkyLight() && !this.getDimensionManager().hasCeiling() ? (double) this.b(1.0F) > 0.9D : false;
    }

    public boolean isRaining() {
        return (double) this.d(1.0F) > 0.2D;
    }

    public boolean isRainingAt(BlockPosition blockposition) {
        if (!this.isRaining()) {
            return false;
        } else if (!this.g(blockposition)) {
            return false;
        } else if (this.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, blockposition).getY() > blockposition.getY()) {
            return false;
        } else {
            BiomeBase biomebase = this.getBiome(blockposition);

            return biomebase.c() == BiomeBase.Precipitation.RAIN && biomebase.getAdjustedTemperature(blockposition) >= 0.15F;
        }
    }

    public boolean u(BlockPosition blockposition) {
        BiomeBase biomebase = this.getBiome(blockposition);

        return biomebase.d();
    }

    @Nullable
    public abstract WorldMap a(String s);

    public abstract void a(String s, WorldMap worldmap);

    public abstract int getWorldMapCount();

    public void b(int i, BlockPosition blockposition, int j) {}

    public CrashReportSystemDetails a(CrashReport crashreport) {
        CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Affected level", 1);

        crashreportsystemdetails.a("All players", () -> {
            int i = this.getPlayers().size();

            return i + " total; " + this.getPlayers();
        });
        IChunkProvider ichunkprovider = this.getChunkProvider();

        Objects.requireNonNull(ichunkprovider);
        crashreportsystemdetails.a("Chunk stats", ichunkprovider::getName);
        crashreportsystemdetails.a("Level dimension", () -> {
            return this.getDimensionKey().a().toString();
        });

        try {
            this.levelData.a(crashreportsystemdetails, this);
        } catch (Throwable throwable) {
            crashreportsystemdetails.a("Level Data Unobtainable", throwable);
        }

        return crashreportsystemdetails;
    }

    public abstract void a(int i, BlockPosition blockposition, int j);

    public void a(double d0, double d1, double d2, double d3, double d4, double d5, @Nullable NBTTagCompound nbttagcompound) {}

    public abstract Scoreboard getScoreboard();

    public void updateAdjacentComparators(BlockPosition blockposition, Block block) {
        Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

        while (iterator.hasNext()) {
            EnumDirection enumdirection = (EnumDirection) iterator.next();
            BlockPosition blockposition1 = blockposition.shift(enumdirection);

            if (this.isLoaded(blockposition1)) {
                IBlockData iblockdata = this.getType(blockposition1);

                if (iblockdata.a(Blocks.COMPARATOR)) {
                    iblockdata.doPhysics(this, blockposition1, block, blockposition, false);
                } else if (iblockdata.isOccluding(this, blockposition1)) {
                    blockposition1 = blockposition1.shift(enumdirection);
                    iblockdata = this.getType(blockposition1);
                    if (iblockdata.a(Blocks.COMPARATOR)) {
                        iblockdata.doPhysics(this, blockposition1, block, blockposition, false);
                    }
                }
            }
        }

    }

    @Override
    public DifficultyDamageScaler getDamageScaler(BlockPosition blockposition) {
        long i = 0L;
        float f = 0.0F;

        if (this.isLoaded(blockposition)) {
            f = this.ak();
            i = this.getChunkAtWorldCoords(blockposition).getInhabitedTime();
        }

        return new DifficultyDamageScaler(this.getDifficulty(), this.getDayTime(), i, f);
    }

    @Override
    public int n_() {
        return this.skyDarken;
    }

    public void c(int i) {}

    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    public void a(Packet<?> packet) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionManager getDimensionManager() {
        return this.dimensionType;
    }

    public ResourceKey<World> getDimensionKey() {
        return this.dimension;
    }

    @Override
    public Random getRandom() {
        return this.random;
    }

    @Override
    public boolean a(BlockPosition blockposition, Predicate<IBlockData> predicate) {
        return predicate.test(this.getType(blockposition));
    }

    @Override
    public boolean b(BlockPosition blockposition, Predicate<Fluid> predicate) {
        return predicate.test(this.getFluid(blockposition));
    }

    public abstract CraftingManager getCraftingManager();

    public abstract ITagRegistry r();

    public BlockPosition a(int i, int j, int k, int l) {
        this.randValue = this.randValue * 3 + 1013904223;
        int i1 = this.randValue >> 2;

        return new BlockPosition(i + (i1 & 15), j + (i1 >> 16 & l), k + (i1 >> 8 & 15));
    }

    public boolean isSavingDisabled() {
        return false;
    }

    public GameProfilerFiller getMethodProfiler() {
        return (GameProfilerFiller) this.profiler.get();
    }

    public Supplier<GameProfilerFiller> getMethodProfilerSupplier() {
        return this.profiler;
    }

    @Override
    public BiomeManager r_() {
        return this.biomeManager;
    }

    public final boolean isDebugWorld() {
        return this.isDebug;
    }

    public abstract LevelEntityGetter<Entity> getEntities();

    protected void a(@Nullable Entity entity, GameEvent gameevent, BlockPosition blockposition, int i) {
        // CraftBukkit start
        GenericGameEvent event = new GenericGameEvent(org.bukkit.GameEvent.getByKey(CraftNamespacedKey.fromMinecraft(IRegistry.GAME_EVENT.getKey(gameevent))), new Location(this.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ()), (entity == null) ? null : entity.getBukkitEntity(), i);
        getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        i = event.getRadius();
        // CraftBukkit end
        int j = SectionPosition.a(blockposition.getX() - i);
        int k = SectionPosition.a(blockposition.getZ() - i);
        int l = SectionPosition.a(blockposition.getX() + i);
        int i1 = SectionPosition.a(blockposition.getZ() + i);
        int j1 = SectionPosition.a(blockposition.getY() - i);
        int k1 = SectionPosition.a(blockposition.getY() + i);

        for (int l1 = j; l1 <= l; ++l1) {
            for (int i2 = k; i2 <= i1; ++i2) {
                Chunk chunk = this.getChunkProvider().a(l1, i2);

                if (chunk != null) {
                    for (int j2 = j1; j2 <= k1; ++j2) {
                        chunk.a(j2).a(gameevent, entity, blockposition);
                    }
                }
            }
        }

    }
}
