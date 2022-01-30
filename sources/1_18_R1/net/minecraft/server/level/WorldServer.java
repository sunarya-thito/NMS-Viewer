package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddVibrationSignalPacket;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.protocol.game.PacketPlayOutBlockAction;
import net.minecraft.network.protocol.game.PacketPlayOutBlockBreakAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutEntitySound;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutExplosion;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnPosition;
import net.minecraft.network.protocol.game.PacketPlayOutWorldEvent;
import net.minecraft.network.protocol.game.PacketPlayOutWorldParticles;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.util.CSVWriter;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.entity.ReputationHandler;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.village.ReputationEvent;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.animal.EntityWaterAnimal;
import net.minecraft.world.entity.animal.horse.EntityHorseSkeleton;
import net.minecraft.world.entity.boss.EntityComplexPart;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.npc.NPC;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.raid.PersistentRaid;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.level.BlockActionData;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ForcedChunk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.SpawnerCreature;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.end.EnderDragonBattle;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListenerRegistrar;
import net.minecraft.world.level.gameevent.vibrations.VibrationPath;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.portal.PortalTravelAgent;
import net.minecraft.world.level.saveddata.maps.PersistentIdCounts;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.IWorldDataServer;
import net.minecraft.world.level.storage.WorldPersistentData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import net.minecraft.world.ticks.TickListServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.world.entity.monster.EntityDrowned;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.storage.WorldDataServer;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.WorldUUID;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.TimeSkipEvent;
// CraftBukkit end

public class WorldServer extends World implements GeneratorAccessSeed {

    public static final BlockPosition END_SPAWN_POINT = new BlockPosition(100, 50, 0);
    private static final int MIN_RAIN_DELAY_TIME = 12000;
    private static final int MAX_RAIN_DELAY_TIME = 180000;
    private static final int MIN_RAIN_TIME = 12000;
    private static final int MAX_RAIN_TIME = 24000;
    private static final int MIN_THUNDER_DELAY_TIME = 12000;
    private static final int MAX_THUNDER_DELAY_TIME = 180000;
    private static final int MIN_THUNDER_TIME = 3600;
    private static final int MAX_THUNDER_TIME = 15600;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int EMPTY_TIME_NO_TICK = 300;
    private static final int MAX_SCHEDULED_TICKS_PER_TICK = 65536;
    final List<EntityPlayer> players;
    private final ChunkProviderServer chunkSource;
    private final MinecraftServer server;
    public final WorldDataServer serverLevelData; // CraftBukkit - type
    final EntityTickList entityTickList;
    public final PersistentEntitySectionManager<Entity> entityManager;
    public boolean noSave;
    private final SleepStatus sleepStatus;
    private int emptyTime;
    private final PortalTravelAgent portalForcer;
    private final TickListServer<Block> blockTicks;
    private final TickListServer<FluidType> fluidTicks;
    final Set<EntityInsentient> navigatingMobs;
    volatile boolean isUpdatingNavigations;
    protected final PersistentRaid raids;
    private final ObjectLinkedOpenHashSet<BlockActionData> blockEvents;
    private final List<BlockActionData> blockEventsToReschedule;
    private boolean handlingTick;
    private final List<MobSpawner> customSpawners;
    @Nullable
    private final EnderDragonBattle dragonFight;
    final Int2ObjectMap<EntityComplexPart> dragonParts;
    private final StructureManager structureFeatureManager;
    private final StructureCheck structureCheck;
    private final boolean tickTime;


    // CraftBukkit start
    private int tickPosition;
    public final Convertable.ConversionSession convertable;
    public final UUID uuid;

    public Chunk getChunkIfLoaded(int x, int z) {
        return this.chunkSource.getChunk(x, z, false);
    }

    @Override
    public ResourceKey<WorldDimension> getTypeKey() {
        return convertable.dimensionType;
    }

    // Add env and gen to constructor, WorldData -> WorldDataServer
    public WorldServer(MinecraftServer minecraftserver, Executor executor, Convertable.ConversionSession convertable_conversionsession, IWorldDataServer iworlddataserver, ResourceKey<World> resourcekey, DimensionManager dimensionmanager, WorldLoadListener worldloadlistener, ChunkGenerator chunkgenerator, boolean flag, long i, List<MobSpawner> list, boolean flag1, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider) {
        // Objects.requireNonNull(minecraftserver); // CraftBukkit - decompile error
        super(iworlddataserver, resourcekey, dimensionmanager, minecraftserver::getProfiler, false, flag, i, gen, biomeProvider, env);
        this.pvpMode = minecraftserver.isPvpAllowed();
        convertable = convertable_conversionsession;
        uuid = WorldUUID.getUUID(convertable_conversionsession.levelPath.toFile());
        // CraftBukkit end
        this.players = Lists.newArrayList();
        this.entityTickList = new EntityTickList();
        this.blockTicks = new TickListServer<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
        this.fluidTicks = new TickListServer<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
        this.navigatingMobs = new ObjectOpenHashSet();
        this.blockEvents = new ObjectLinkedOpenHashSet();
        this.blockEventsToReschedule = new ArrayList(64);
        this.dragonParts = new Int2ObjectOpenHashMap();
        this.tickTime = flag1;
        this.server = minecraftserver;
        this.customSpawners = list;
        // CraftBukkit start
        this.serverLevelData = (WorldDataServer) iworlddataserver;
        serverLevelData.world = this;
        if (gen != null) {
            chunkgenerator = new org.bukkit.craftbukkit.generator.CustomChunkGenerator(this, chunkgenerator, gen);
        }
        // CraftBukkit end
        boolean flag2 = minecraftserver.forceSynchronousWrites();
        DataFixer datafixer = minecraftserver.getFixerUpper();
        EntityPersistentStorage<Entity> entitypersistentstorage = new EntityStorage(this, convertable_conversionsession.getDimensionPath(resourcekey).resolve("entities"), datafixer, flag2, minecraftserver);

        this.entityManager = new PersistentEntitySectionManager<>(Entity.class, new WorldServer.a(), entitypersistentstorage);
        DefinedStructureManager definedstructuremanager = minecraftserver.getStructureManager();
        int j = this.spigotConfig.viewDistance; // Spigot
        int k = this.spigotConfig.simulationDistance; // Spigot
        PersistentEntitySectionManager persistententitysectionmanager = this.entityManager;

        Objects.requireNonNull(this.entityManager);
        this.chunkSource = new ChunkProviderServer(this, convertable_conversionsession, datafixer, definedstructuremanager, executor, chunkgenerator, j, k, flag2, worldloadlistener, persistententitysectionmanager::updateChunkStatus, () -> {
            return minecraftserver.overworld().getDataStorage();
        });
        this.portalForcer = new PortalTravelAgent(this);
        this.updateSkyBrightness();
        this.prepareWeather();
        this.getWorldBorder().setAbsoluteMaxSize(minecraftserver.getAbsoluteMaxWorldSize());
        this.raids = (PersistentRaid) this.getDataStorage().computeIfAbsent((nbttagcompound) -> {
            return PersistentRaid.load(this, nbttagcompound);
        }, () -> {
            return new PersistentRaid(this);
        }, PersistentRaid.getFileId(this.dimensionType()));
        if (!minecraftserver.isSingleplayer()) {
            iworlddataserver.setGameType(minecraftserver.getDefaultGameType());
        }

        long l = minecraftserver.getWorldData().worldGenSettings().seed();

        this.structureCheck = new StructureCheck(this.chunkSource.chunkScanner(), this.registryAccess(), minecraftserver.getStructureManager(), resourcekey, chunkgenerator, this, chunkgenerator.getBiomeSource(), l, datafixer);
        this.structureFeatureManager = new StructureManager(this, this.serverLevelData.worldGenSettings(), structureCheck); // CraftBukkit
        if (this.dimensionType().createDragonFight()) {
            this.dragonFight = new EnderDragonBattle(this, this.serverLevelData.worldGenSettings().seed(), this.serverLevelData.endDragonFightData()); // CraftBukkit
        } else {
            this.dragonFight = null;
        }

        this.sleepStatus = new SleepStatus();
        this.getCraftServer().addWorld(this.getWorld()); // CraftBukkit
    }

    public void setWeatherParameters(int i, int j, boolean flag, boolean flag1) {
        this.serverLevelData.setClearWeatherTime(i);
        this.serverLevelData.setRainTime(j);
        this.serverLevelData.setThunderTime(j);
        this.serverLevelData.setRaining(flag);
        this.serverLevelData.setThundering(flag1);
    }

    @Override
    public BiomeBase getUncachedNoiseBiome(int i, int j, int k) {
        return this.getChunkSource().getGenerator().getNoiseBiome(i, j, k);
    }

    public StructureManager structureFeatureManager() {
        return this.structureFeatureManager;
    }

    public void tick(BooleanSupplier booleansupplier) {
        GameProfilerFiller gameprofilerfiller = this.getProfiler();

        this.handlingTick = true;
        gameprofilerfiller.push("world border");
        this.getWorldBorder().tick();
        gameprofilerfiller.popPush("weather");
        this.advanceWeatherCycle();
        int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        long j;

        if (this.sleepStatus.areEnoughSleeping(i) && this.sleepStatus.areEnoughDeepSleeping(i, this.players)) {
            // CraftBukkit start
            j = this.levelData.getDayTime() + 24000L;
            TimeSkipEvent event = new TimeSkipEvent(this.getWorld(), TimeSkipEvent.SkipReason.NIGHT_SKIP, (j - j % 24000L) - this.getDayTime());
            if (this.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                getCraftServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    this.setDayTime(this.getDayTime() + event.getSkipAmount());
                }
            }

            if (!event.isCancelled()) {
                this.wakeUpAllPlayers();
            }
            // CraftBukkit end
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && this.isRaining()) {
                this.resetWeatherCycle();
            }
        }

        this.updateSkyBrightness();
        this.tickTime();
        gameprofilerfiller.popPush("tickPending");
        timings.doTickPending.startTiming(); // Spigot
        if (!this.isDebug()) {
            j = this.getGameTime();
            gameprofilerfiller.push("blockTicks");
            this.blockTicks.tick(j, 65536, this::tickBlock);
            gameprofilerfiller.popPush("fluidTicks");
            this.fluidTicks.tick(j, 65536, this::tickFluid);
            gameprofilerfiller.pop();
        }
        timings.doTickPending.stopTiming(); // Spigot

        gameprofilerfiller.popPush("raid");
        this.raids.tick();
        gameprofilerfiller.popPush("chunkSource");
        this.getChunkSource().tick(booleansupplier);
        gameprofilerfiller.popPush("blockEvents");
        timings.doSounds.startTiming(); // Spigot
        this.runBlockEvents();
        timings.doSounds.stopTiming(); // Spigot
        this.handlingTick = false;
        gameprofilerfiller.pop();
        boolean flag = true || !this.players.isEmpty() || !this.getForcedChunks().isEmpty(); // CraftBukkit - this prevents entity cleanup, other issues on servers with no players

        if (flag) {
            this.resetEmptyTime();
        }

        if (flag || this.emptyTime++ < 300) {
            gameprofilerfiller.push("entities");
            timings.tickEntities.startTiming(); // Spigot
            if (this.dragonFight != null) {
                gameprofilerfiller.push("dragonFight");
                this.dragonFight.tick();
                gameprofilerfiller.pop();
            }

            org.spigotmc.ActivationRange.activateEntities(this); // Spigot
            timings.entityTick.startTiming(); // Spigot
            this.entityTickList.forEach((entity) -> {
                if (!entity.isRemoved()) {
                    if (false && this.shouldDiscardEntity(entity)) { // CraftBukkit - We prevent spawning in general, so this butchering is not needed
                        entity.discard();
                    } else {
                        gameprofilerfiller.push("checkDespawn");
                        entity.checkDespawn();
                        gameprofilerfiller.pop();
                        if (this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(entity.chunkPosition().toLong())) {
                            Entity entity1 = entity.getVehicle();

                            if (entity1 != null) {
                                if (!entity1.isRemoved() && entity1.hasPassenger(entity)) {
                                    return;
                                }

                                entity.stopRiding();
                            }

                            gameprofilerfiller.push("tick");
                            this.guardEntityTick(this::tickNonPassenger, entity);
                            gameprofilerfiller.pop();
                        }
                    }
                }
            });
            timings.entityTick.stopTiming(); // Spigot
            timings.tickEntities.stopTiming(); // Spigot
            gameprofilerfiller.pop();
            this.tickBlockEntities();
        }

        gameprofilerfiller.push("entityManagement");
        this.entityManager.tick();
        gameprofilerfiller.pop();
    }

    @Override
    public boolean shouldTickBlocksAt(long i) {
        return this.chunkSource.chunkMap.getDistanceManager().inBlockTickingRange(i);
    }

    protected void tickTime() {
        if (this.tickTime) {
            long i = this.levelData.getGameTime() + 1L;

            this.serverLevelData.setGameTime(i);
            this.serverLevelData.getScheduledEvents().tick(this.server, i);
            if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                this.setDayTime(this.levelData.getDayTime() + 1L);
            }

        }
    }

    public void setDayTime(long i) {
        this.serverLevelData.setDayTime(i);
    }

    public void tickCustomSpawners(boolean flag, boolean flag1) {
        Iterator iterator = this.customSpawners.iterator();

        while (iterator.hasNext()) {
            MobSpawner mobspawner = (MobSpawner) iterator.next();

            mobspawner.tick(this, flag, flag1);
        }

    }

    private boolean shouldDiscardEntity(Entity entity) {
        return !this.server.isSpawningAnimals() && (entity instanceof EntityAnimal || entity instanceof EntityWaterAnimal) ? true : !this.server.areNpcsEnabled() && entity instanceof NPC;
    }

    private void wakeUpAllPlayers() {
        this.sleepStatus.removeAllSleepers();
        (this.players.stream().filter(EntityLiving::isSleeping).collect(Collectors.toList())).forEach((entityplayer) -> { // CraftBukkit - decompile error
            entityplayer.stopSleepInBed(false, false);
        });
    }

    public void tickChunk(Chunk chunk, int i) {
        ChunkCoordIntPair chunkcoordintpair = chunk.getPos();
        boolean flag = this.isRaining();
        int j = chunkcoordintpair.getMinBlockX();
        int k = chunkcoordintpair.getMinBlockZ();
        GameProfilerFiller gameprofilerfiller = this.getProfiler();

        gameprofilerfiller.push("thunder");
        BlockPosition blockposition;

        if (flag && this.isThundering() && this.spigotConfig.thunderChance > 0 && this.random.nextInt(this.spigotConfig.thunderChance) == 0) { // Spigot
            blockposition = this.findLightningTargetAround(this.getBlockRandomPos(j, 0, k, 15));
            if (this.isRainingAt(blockposition)) {
                DifficultyDamageScaler difficultydamagescaler = this.getCurrentDifficultyAt(blockposition);
                boolean flag1 = this.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && this.random.nextDouble() < (double) difficultydamagescaler.getEffectiveDifficulty() * 0.01D && !this.getBlockState(blockposition.below()).is(Blocks.LIGHTNING_ROD);

                if (flag1) {
                    EntityHorseSkeleton entityhorseskeleton = (EntityHorseSkeleton) EntityTypes.SKELETON_HORSE.create(this);

                    entityhorseskeleton.setTrap(true);
                    entityhorseskeleton.setAge(0);
                    entityhorseskeleton.setPos((double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ());
                    this.addFreshEntity(entityhorseskeleton, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.LIGHTNING); // CraftBukkit
                }

                EntityLightning entitylightning = (EntityLightning) EntityTypes.LIGHTNING_BOLT.create(this);

                entitylightning.moveTo(Vec3D.atBottomCenterOf(blockposition));
                entitylightning.setVisualOnly(flag1);
                this.strikeLightning(entitylightning, org.bukkit.event.weather.LightningStrikeEvent.Cause.WEATHER); // CraftBukkit
            }
        }

        gameprofilerfiller.popPush("iceandsnow");
        if (this.random.nextInt(16) == 0) {
            blockposition = this.getHeightmapPos(HeightMap.Type.MOTION_BLOCKING, this.getBlockRandomPos(j, 0, k, 15));
            BlockPosition blockposition1 = blockposition.below();
            BiomeBase biomebase = this.getBiome(blockposition);

            if (biomebase.shouldFreeze(this, blockposition1)) {
                org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockFormEvent(this, blockposition1, Blocks.ICE.defaultBlockState(), null); // CraftBukkit
            }

            if (flag) {
                if (biomebase.shouldSnow(this, blockposition)) {
                    org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockFormEvent(this, blockposition, Blocks.SNOW.defaultBlockState(), null); // CraftBukkit
                }

                IBlockData iblockdata = this.getBlockState(blockposition1);
                BiomeBase.Precipitation biomebase_precipitation = this.getBiome(blockposition).getPrecipitation();

                if (biomebase_precipitation == BiomeBase.Precipitation.RAIN && biomebase.coldEnoughToSnow(blockposition1)) {
                    biomebase_precipitation = BiomeBase.Precipitation.SNOW;
                }

                iblockdata.getBlock().handlePrecipitation(iblockdata, this, blockposition1, biomebase_precipitation);
            }
        }

        gameprofilerfiller.popPush("tickBlocks");
        if (i > 0) {
            ChunkSection[] achunksection = chunk.getSections();
            int l = achunksection.length;

            for (int i1 = 0; i1 < l; ++i1) {
                ChunkSection chunksection = achunksection[i1];

                if (chunksection.isRandomlyTicking()) {
                    int j1 = chunksection.bottomBlockY();

                    for (int k1 = 0; k1 < i; ++k1) {
                        BlockPosition blockposition2 = this.getBlockRandomPos(j, j1, k, 15);

                        gameprofilerfiller.push("randomTick");
                        IBlockData iblockdata1 = chunksection.getBlockState(blockposition2.getX() - j, blockposition2.getY() - j1, blockposition2.getZ() - k);

                        if (iblockdata1.isRandomlyTicking()) {
                            iblockdata1.randomTick(this, blockposition2, this.random);
                        }

                        Fluid fluid = iblockdata1.getFluidState();

                        if (fluid.isRandomlyTicking()) {
                            fluid.randomTick(this, blockposition2, this.random);
                        }

                        gameprofilerfiller.pop();
                    }
                }
            }
        }

        gameprofilerfiller.pop();
    }

    private Optional<BlockPosition> findLightningRod(BlockPosition blockposition) {
        Optional<BlockPosition> optional = this.getPoiManager().findClosest((villageplacetype) -> {
            return villageplacetype == VillagePlaceType.LIGHTNING_ROD;
        }, (blockposition1) -> {
            return blockposition1.getY() == this.getLevel().getHeight(HeightMap.Type.WORLD_SURFACE, blockposition1.getX(), blockposition1.getZ()) - 1;
        }, blockposition, 128, VillagePlace.Occupancy.ANY);

        return optional.map((blockposition1) -> {
            return blockposition1.above(1);
        });
    }

    protected BlockPosition findLightningTargetAround(BlockPosition blockposition) {
        BlockPosition blockposition1 = this.getHeightmapPos(HeightMap.Type.MOTION_BLOCKING, blockposition);
        Optional<BlockPosition> optional = this.findLightningRod(blockposition1);

        if (optional.isPresent()) {
            return (BlockPosition) optional.get();
        } else {
            AxisAlignedBB axisalignedbb = (new AxisAlignedBB(blockposition1, new BlockPosition(blockposition1.getX(), this.getMaxBuildHeight(), blockposition1.getZ()))).inflate(3.0D);
            List<EntityLiving> list = this.getEntitiesOfClass(EntityLiving.class, axisalignedbb, (entityliving) -> {
                return entityliving != null && entityliving.isAlive() && this.canSeeSky(entityliving.blockPosition());
            });

            if (!list.isEmpty()) {
                return ((EntityLiving) list.get(this.random.nextInt(list.size()))).blockPosition();
            } else {
                if (blockposition1.getY() == this.getMinBuildHeight() - 1) {
                    blockposition1 = blockposition1.above(2);
                }

                return blockposition1;
            }
        }
    }

    public boolean isHandlingTick() {
        return this.handlingTick;
    }

    public boolean canSleepThroughNights() {
        return this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) <= 100;
    }

    private void announceSleepStatus() {
        if (this.canSleepThroughNights()) {
            if (!this.getServer().isSingleplayer() || this.getServer().isPublished()) {
                int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
                ChatMessage chatmessage;

                if (this.sleepStatus.areEnoughSleeping(i)) {
                    chatmessage = new ChatMessage("sleep.skipping_night");
                } else {
                    chatmessage = new ChatMessage("sleep.players_sleeping", new Object[]{this.sleepStatus.amountSleeping(), this.sleepStatus.sleepersNeeded(i)});
                }

                Iterator iterator = this.players.iterator();

                while (iterator.hasNext()) {
                    EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                    entityplayer.displayClientMessage(chatmessage, true);
                }

            }
        }
    }

    public void updateSleepingPlayerList() {
        if (!this.players.isEmpty() && this.sleepStatus.update(this.players)) {
            this.announceSleepStatus();
        }

    }

    @Override
    public ScoreboardServer getScoreboard() {
        return this.server.getScoreboard();
    }

    private void advanceWeatherCycle() {
        boolean flag = this.isRaining();

        if (this.dimensionType().hasSkyLight()) {
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                int i = this.serverLevelData.getClearWeatherTime();
                int j = this.serverLevelData.getThunderTime();
                int k = this.serverLevelData.getRainTime();
                boolean flag1 = this.levelData.isThundering();
                boolean flag2 = this.levelData.isRaining();

                if (i > 0) {
                    --i;
                    j = flag1 ? 0 : 1;
                    k = flag2 ? 0 : 1;
                    flag1 = false;
                    flag2 = false;
                } else {
                    if (j > 0) {
                        --j;
                        if (j == 0) {
                            flag1 = !flag1;
                        }
                    } else if (flag1) {
                        j = MathHelper.randomBetweenInclusive(this.random, 3600, 15600);
                    } else {
                        j = MathHelper.randomBetweenInclusive(this.random, 12000, 180000);
                    }

                    if (k > 0) {
                        --k;
                        if (k == 0) {
                            flag2 = !flag2;
                        }
                    } else if (flag2) {
                        k = MathHelper.randomBetweenInclusive(this.random, 12000, 24000);
                    } else {
                        k = MathHelper.randomBetweenInclusive(this.random, 12000, 180000);
                    }
                }

                this.serverLevelData.setThunderTime(j);
                this.serverLevelData.setRainTime(k);
                this.serverLevelData.setClearWeatherTime(i);
                this.serverLevelData.setThundering(flag1);
                this.serverLevelData.setRaining(flag2);
            }

            this.oThunderLevel = this.thunderLevel;
            if (this.levelData.isThundering()) {
                this.thunderLevel = (float) ((double) this.thunderLevel + 0.01D);
            } else {
                this.thunderLevel = (float) ((double) this.thunderLevel - 0.01D);
            }

            this.thunderLevel = MathHelper.clamp(this.thunderLevel, 0.0F, 1.0F);
            this.oRainLevel = this.rainLevel;
            if (this.levelData.isRaining()) {
                this.rainLevel = (float) ((double) this.rainLevel + 0.01D);
            } else {
                this.rainLevel = (float) ((double) this.rainLevel - 0.01D);
            }

            this.rainLevel = MathHelper.clamp(this.rainLevel, 0.0F, 1.0F);
        }

        /* CraftBukkit start
        if (this.oRainLevel != this.rainLevel) {
            this.server.getPlayerList().broadcastAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
        }

        if (this.oThunderLevel != this.thunderLevel) {
            this.server.getPlayerList().broadcastAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
        }

        if (flag != this.isRaining()) {
            if (flag) {
                this.server.getPlayerList().broadcastAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.STOP_RAINING, 0.0F));
            } else {
                this.server.getPlayerList().broadcastAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.START_RAINING, 0.0F));
            }

            this.server.getPlayerList().broadcastAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.RAIN_LEVEL_CHANGE, this.rainLevel));
            this.server.getPlayerList().broadcastAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.THUNDER_LEVEL_CHANGE, this.thunderLevel));
        }
        // */
        for (int idx = 0; idx < this.players.size(); ++idx) {
            if (((EntityPlayer) this.players.get(idx)).level == this) {
                ((EntityPlayer) this.players.get(idx)).tickWeather();
            }
        }

        if (flag != this.isRaining()) {
            // Only send weather packets to those affected
            for (int idx = 0; idx < this.players.size(); ++idx) {
                if (((EntityPlayer) this.players.get(idx)).level == this) {
                    ((EntityPlayer) this.players.get(idx)).setPlayerWeather((!flag ? WeatherType.DOWNFALL : WeatherType.CLEAR), false);
                }
            }
        }
        for (int idx = 0; idx < this.players.size(); ++idx) {
            if (((EntityPlayer) this.players.get(idx)).level == this) {
                ((EntityPlayer) this.players.get(idx)).updateWeather(this.oRainLevel, this.rainLevel, this.oThunderLevel, this.thunderLevel);
            }
        }
        // CraftBukkit end

    }

    private void resetWeatherCycle() {
        // CraftBukkit start
        this.serverLevelData.setRaining(false);
        // If we stop due to everyone sleeping we should reset the weather duration to some other random value.
        // Not that everyone ever manages to get the whole server to sleep at the same time....
        if (!this.serverLevelData.isRaining()) {
            this.serverLevelData.setRainTime(0);
        }
        // CraftBukkit end
        this.serverLevelData.setThundering(false);
        // CraftBukkit start
        // If we stop due to everyone sleeping we should reset the weather duration to some other random value.
        // Not that everyone ever manages to get the whole server to sleep at the same time....
        if (!this.serverLevelData.isThundering()) {
            this.serverLevelData.setThunderTime(0);
        }
        // CraftBukkit end
    }

    public void resetEmptyTime() {
        this.emptyTime = 0;
    }

    private void tickFluid(BlockPosition blockposition, FluidType fluidtype) {
        Fluid fluid = this.getFluidState(blockposition);

        if (fluid.is(fluidtype)) {
            fluid.tick(this, blockposition);
        }

    }

    private void tickBlock(BlockPosition blockposition, Block block) {
        IBlockData iblockdata = this.getBlockState(blockposition);

        if (iblockdata.is(block)) {
            iblockdata.tick(this, blockposition, this.random);
        }

    }

    public void tickNonPassenger(Entity entity) {
        // Spigot start
        if (!org.spigotmc.ActivationRange.checkIfActive(entity)) {
            entity.tickCount++;
            entity.inactiveTick();
            return;
        }
        // Spigot end
        entity.tickTimer.startTiming(); // Spigot
        entity.setOldPosAndRot();
        GameProfilerFiller gameprofilerfiller = this.getProfiler();

        ++entity.tickCount;
        this.getProfiler().push(() -> {
            return IRegistry.ENTITY_TYPE.getKey(entity.getType()).toString();
        });
        gameprofilerfiller.incrementCounter("tickNonPassenger");
        entity.tick();
        entity.postTick(); // CraftBukkit
        this.getProfiler().pop();
        Iterator iterator = entity.getPassengers().iterator();

        while (iterator.hasNext()) {
            Entity entity1 = (Entity) iterator.next();

            this.tickPassenger(entity, entity1);
        }
        entity.tickTimer.stopTiming(); // Spigot

    }

    private void tickPassenger(Entity entity, Entity entity1) {
        if (!entity1.isRemoved() && entity1.getVehicle() == entity) {
            if (entity1 instanceof EntityHuman || this.entityTickList.contains(entity1)) {
                entity1.setOldPosAndRot();
                ++entity1.tickCount;
                GameProfilerFiller gameprofilerfiller = this.getProfiler();

                gameprofilerfiller.push(() -> {
                    return IRegistry.ENTITY_TYPE.getKey(entity1.getType()).toString();
                });
                gameprofilerfiller.incrementCounter("tickPassenger");
                entity1.rideTick();
                entity1.postTick(); // CraftBukkit
                gameprofilerfiller.pop();
                Iterator iterator = entity1.getPassengers().iterator();

                while (iterator.hasNext()) {
                    Entity entity2 = (Entity) iterator.next();

                    this.tickPassenger(entity1, entity2);
                }

            }
        } else {
            entity1.stopRiding();
        }
    }

    @Override
    public boolean mayInteract(EntityHuman entityhuman, BlockPosition blockposition) {
        return !this.server.isUnderSpawnProtection(this, blockposition, entityhuman) && this.getWorldBorder().isWithinBounds(blockposition);
    }

    public void save(@Nullable IProgressUpdate iprogressupdate, boolean flag, boolean flag1) {
        ChunkProviderServer chunkproviderserver = this.getChunkSource();

        if (!flag1) {
            org.bukkit.Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.WorldSaveEvent(getWorld())); // CraftBukkit
            if (iprogressupdate != null) {
                iprogressupdate.progressStartNoAbort(new ChatMessage("menu.savingLevel"));
            }

            this.saveLevelData();
            if (iprogressupdate != null) {
                iprogressupdate.progressStage(new ChatMessage("menu.savingChunks"));
            }

            chunkproviderserver.save(flag);
            if (flag) {
                this.entityManager.saveAll();
            } else {
                this.entityManager.autoSave();
            }

        }

        // CraftBukkit start - moved from MinecraftServer.saveChunks
        WorldServer worldserver1 = this;

        serverLevelData.setWorldBorder(worldserver1.getWorldBorder().createSettings());
        serverLevelData.setCustomBossEvents(this.server.getCustomBossEvents().save());
        convertable.saveDataTag(this.server.registryHolder, this.serverLevelData, this.server.getPlayerList().getSingleplayerData());
        // CraftBukkit end
    }

    private void saveLevelData() {
        if (this.dragonFight != null) {
            this.serverLevelData.setEndDragonFightData(this.dragonFight.saveData()); // CraftBukkit
        }

        this.getChunkSource().getDataStorage().save();
    }

    public <T extends Entity> List<? extends T> getEntities(EntityTypeTest<Entity, T> entitytypetest, Predicate<? super T> predicate) {
        List<T> list = Lists.newArrayList();

        this.getEntities().get(entitytypetest, (entity) -> {
            if (predicate.test(entity)) {
                list.add(entity);
            }

        });
        return list;
    }

    public List<? extends EntityEnderDragon> getDragons() {
        return this.getEntities((EntityTypeTest) EntityTypes.ENDER_DRAGON, EntityLiving::isAlive);
    }

    public List<EntityPlayer> getPlayers(Predicate<? super EntityPlayer> predicate) {
        List<EntityPlayer> list = Lists.newArrayList();
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            if (predicate.test(entityplayer)) {
                list.add(entityplayer);
            }
        }

        return list;
    }

    @Nullable
    public EntityPlayer getRandomPlayer() {
        List<EntityPlayer> list = this.getPlayers(EntityLiving::isAlive);

        return list.isEmpty() ? null : (EntityPlayer) list.get(this.random.nextInt(list.size()));
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        // CraftBukkit start
        return this.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    @Override
    public boolean addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return this.addEntity(entity, reason);
        // CraftBukkit end
    }

    public boolean addWithUUID(Entity entity) {
        // CraftBukkit start
        return this.addWithUUID(entity, CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    public boolean addWithUUID(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return this.addEntity(entity, reason);
        // CraftBukkit end
    }

    public void addDuringTeleport(Entity entity) {
        // CraftBukkit start
        this.addDuringTeleport(entity, CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    public void addDuringTeleport(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        this.addEntity(entity, reason);
        // CraftBukkit end
    }

    public void addDuringCommandTeleport(EntityPlayer entityplayer) {
        this.addPlayer(entityplayer);
    }

    public void addDuringPortalTeleport(EntityPlayer entityplayer) {
        this.addPlayer(entityplayer);
    }

    public void addNewPlayer(EntityPlayer entityplayer) {
        this.addPlayer(entityplayer);
    }

    public void addRespawnedPlayer(EntityPlayer entityplayer) {
        this.addPlayer(entityplayer);
    }

    private void addPlayer(EntityPlayer entityplayer) {
        Entity entity = (Entity) this.getEntities().get(entityplayer.getUUID());

        if (entity != null) {
            WorldServer.LOGGER.warn("Force-added player with duplicate UUID {}", entityplayer.getUUID().toString());
            entity.unRide();
            this.removePlayerImmediately((EntityPlayer) entity, Entity.RemovalReason.DISCARDED);
        }

        this.entityManager.addNewEntity(entityplayer);
    }

    // CraftBukkit start
    private boolean addEntity(Entity entity, CreatureSpawnEvent.SpawnReason spawnReason) {
        org.spigotmc.AsyncCatcher.catchOp("entity add"); // Spigot
        if (entity.isRemoved()) {
            // WorldServer.LOGGER.warn("Tried to add entity {} but it was marked as removed already", EntityTypes.getKey(entity.getType())); // CraftBukkit
            return false;
        } else {
            if (!CraftEventFactory.doEntityAddEventCalling(this, entity, spawnReason)) {
                return false;
            }
            // CraftBukkit end

            return this.entityManager.addNewEntity(entity);
        }
    }

    public boolean tryAddFreshEntityWithPassengers(Entity entity) {
        // CraftBukkit start
        return this.tryAddFreshEntityWithPassengers(entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT);
    }

    public boolean tryAddFreshEntityWithPassengers(Entity entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason) {
        // CraftBukkit end
        Stream<UUID> stream = entity.getSelfAndPassengers().map(Entity::getUUID); // CraftBukkit - decompile error
        PersistentEntitySectionManager persistententitysectionmanager = this.entityManager;

        Objects.requireNonNull(this.entityManager);
        if (stream.anyMatch(persistententitysectionmanager::isLoaded)) {
            return false;
        } else {
            this.addFreshEntityWithPassengers(entity, reason); // CraftBukkit
            return true;
        }
    }

    public void unload(Chunk chunk) {
        // Spigot Start
        for (net.minecraft.world.level.block.entity.TileEntity tileentity : chunk.getBlockEntities().values()) {
            if (tileentity instanceof net.minecraft.world.IInventory) {
                for (org.bukkit.entity.HumanEntity h : Lists.newArrayList(((net.minecraft.world.IInventory) tileentity).getViewers())) {
                    h.closeInventory();
                }
            }
        }
        // Spigot End
        chunk.clearAllBlockEntities();
        chunk.unregisterTickContainerFromLevel(this);
    }

    public void removePlayerImmediately(EntityPlayer entityplayer, Entity.RemovalReason entity_removalreason) {
        entityplayer.remove(entity_removalreason);
    }

    // CraftBukkit start
    public boolean strikeLightning(Entity entitylightning) {
        return this.strikeLightning(entitylightning, LightningStrikeEvent.Cause.UNKNOWN);
    }

    public boolean strikeLightning(Entity entitylightning, LightningStrikeEvent.Cause cause) {
        LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((org.bukkit.entity.LightningStrike) entitylightning.getBukkitEntity(), cause);

        if (lightning.isCancelled()) {
            return false;
        }

        return this.addFreshEntity(entitylightning);
    }
    // CraftBukkit end

    @Override
    public void destroyBlockProgress(int i, BlockPosition blockposition, int j) {
        Iterator iterator = this.server.getPlayerList().getPlayers().iterator();

        // CraftBukkit start
        EntityHuman entityhuman = null;
        Entity entity = this.getEntity(i);
        if (entity instanceof EntityHuman) entityhuman = (EntityHuman) entity;
        // CraftBukkit end

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            if (entityplayer != null && entityplayer.level == this && entityplayer.getId() != i) {
                double d0 = (double) blockposition.getX() - entityplayer.getX();
                double d1 = (double) blockposition.getY() - entityplayer.getY();
                double d2 = (double) blockposition.getZ() - entityplayer.getZ();

                // CraftBukkit start
                if (entityhuman != null && !entityplayer.getBukkitEntity().canSee(entityhuman.getBukkitEntity())) {
                    continue;
                }
                // CraftBukkit end

                if (d0 * d0 + d1 * d1 + d2 * d2 < 1024.0D) {
                    entityplayer.connection.send(new PacketPlayOutBlockBreakAnimation(i, blockposition, j));
                }
            }
        }

    }

    @Override
    public void playSound(@Nullable EntityHuman entityhuman, double d0, double d1, double d2, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1) {
        this.server.getPlayerList().broadcast(entityhuman, d0, d1, d2, f > 1.0F ? (double) (16.0F * f) : 16.0D, this.dimension(), new PacketPlayOutNamedSoundEffect(soundeffect, soundcategory, d0, d1, d2, f, f1));
    }

    @Override
    public void playSound(@Nullable EntityHuman entityhuman, Entity entity, SoundEffect soundeffect, SoundCategory soundcategory, float f, float f1) {
        this.server.getPlayerList().broadcast(entityhuman, entity.getX(), entity.getY(), entity.getZ(), f > 1.0F ? (double) (16.0F * f) : 16.0D, this.dimension(), new PacketPlayOutEntitySound(soundeffect, soundcategory, entity, f, f1));
    }

    @Override
    public void globalLevelEvent(int i, BlockPosition blockposition, int j) {
        this.server.getPlayerList().broadcastAll(new PacketPlayOutWorldEvent(i, blockposition, j, true));
    }

    @Override
    public void levelEvent(@Nullable EntityHuman entityhuman, int i, BlockPosition blockposition, int j) {
        this.server.getPlayerList().broadcast(entityhuman, (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ(), 64.0D, this.dimension(), new PacketPlayOutWorldEvent(i, blockposition, j, false));
    }

    public int getLogicalHeight() {
        return this.dimensionType().logicalHeight();
    }

    @Override
    public void gameEvent(@Nullable Entity entity, GameEvent gameevent, BlockPosition blockposition) {
        this.postGameEventInRadius(entity, gameevent, blockposition, gameevent.getNotificationRadius());
    }

    @Override
    public void sendBlockUpdated(BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1, int i) {
        if (this.isUpdatingNavigations) {
            String s = "recursive call to sendBlockUpdated";

            SystemUtils.logAndPauseIfInIde("recursive call to sendBlockUpdated", new IllegalStateException("recursive call to sendBlockUpdated"));
        }

        this.getChunkSource().blockChanged(blockposition);
        VoxelShape voxelshape = iblockdata.getCollisionShape(this, blockposition);
        VoxelShape voxelshape1 = iblockdata1.getCollisionShape(this, blockposition);

        if (VoxelShapes.joinIsNotEmpty(voxelshape, voxelshape1, OperatorBoolean.NOT_SAME)) {
            List<NavigationAbstract> list = new ObjectArrayList();
            Iterator iterator = this.navigatingMobs.iterator();

            while (iterator.hasNext()) {
                // CraftBukkit start - fix SPIGOT-6362
                EntityInsentient entityinsentient;
                try {
                    entityinsentient = (EntityInsentient) iterator.next();
                } catch (java.util.ConcurrentModificationException ex) {
                    // This can happen because the pathfinder update below may trigger a chunk load, which in turn may cause more navigators to register
                    // In this case we just run the update again across all the iterators as the chunk will then be loaded
                    // As this is a relative edge case it is much faster than copying navigators (on either read or write)
                    sendBlockUpdated(blockposition, iblockdata, iblockdata1, i);
                    return;
                }
                // CraftBukkit end
                NavigationAbstract navigationabstract = entityinsentient.getNavigation();

                if (navigationabstract.shouldRecomputePath(blockposition)) {
                    list.add(navigationabstract);
                }
            }

            try {
                this.isUpdatingNavigations = true;
                iterator = list.iterator();

                while (iterator.hasNext()) {
                    NavigationAbstract navigationabstract1 = (NavigationAbstract) iterator.next();

                    navigationabstract1.recomputePath();
                }
            } finally {
                this.isUpdatingNavigations = false;
            }

        }
    }

    @Override
    public void broadcastEntityEvent(Entity entity, byte b0) {
        this.getChunkSource().broadcastAndSend(entity, new PacketPlayOutEntityStatus(entity, b0));
    }

    @Override
    public ChunkProviderServer getChunkSource() {
        return this.chunkSource;
    }

    @Override
    public Explosion explode(@Nullable Entity entity, @Nullable DamageSource damagesource, @Nullable ExplosionDamageCalculator explosiondamagecalculator, double d0, double d1, double d2, float f, boolean flag, Explosion.Effect explosion_effect) {
        // CraftBukkit start
        Explosion explosion = super.explode(entity, damagesource, explosiondamagecalculator, d0, d1, d2, f, flag, explosion_effect);

        if (explosion.wasCanceled) {
            return explosion;
        }

        /* Remove
        Explosion explosion = new Explosion(this, entity, damagesource, explosiondamagecalculator, d0, d1, d2, f, flag, explosion_effect);

        explosion.explode();
        explosion.finalizeExplosion(false);
        */
        // CraftBukkit end - TODO: Check if explosions are still properly implemented
        if (explosion_effect == Explosion.Effect.NONE) {
            explosion.clearToBlow();
        }

        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            if (entityplayer.distanceToSqr(d0, d1, d2) < 4096.0D) {
                entityplayer.connection.send(new PacketPlayOutExplosion(d0, d1, d2, f, explosion.getToBlow(), (Vec3D) explosion.getHitPlayers().get(entityplayer)));
            }
        }

        return explosion;
    }

    @Override
    public void blockEvent(BlockPosition blockposition, Block block, int i, int j) {
        this.blockEvents.add(new BlockActionData(blockposition, block, i, j));
    }

    private void runBlockEvents() {
        this.blockEventsToReschedule.clear();

        while (!this.blockEvents.isEmpty()) {
            BlockActionData blockactiondata = (BlockActionData) this.blockEvents.removeFirst();

            if (this.shouldTickBlocksAt(ChunkCoordIntPair.asLong(blockactiondata.pos()))) {
                if (this.doBlockEvent(blockactiondata)) {
                    this.server.getPlayerList().broadcast((EntityHuman) null, (double) blockactiondata.pos().getX(), (double) blockactiondata.pos().getY(), (double) blockactiondata.pos().getZ(), 64.0D, this.dimension(), new PacketPlayOutBlockAction(blockactiondata.pos(), blockactiondata.block(), blockactiondata.paramA(), blockactiondata.paramB()));
                }
            } else {
                this.blockEventsToReschedule.add(blockactiondata);
            }
        }

        this.blockEvents.addAll(this.blockEventsToReschedule);
    }

    private boolean doBlockEvent(BlockActionData blockactiondata) {
        IBlockData iblockdata = this.getBlockState(blockactiondata.pos());

        return iblockdata.is(blockactiondata.block()) ? iblockdata.triggerEvent(this, blockactiondata.pos(), blockactiondata.paramA(), blockactiondata.paramB()) : false;
    }

    @Override
    public TickListServer<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickListServer<FluidType> getFluidTicks() {
        return this.fluidTicks;
    }

    @Nonnull
    @Override
    public MinecraftServer getServer() {
        return this.server;
    }

    public PortalTravelAgent getPortalForcer() {
        return this.portalForcer;
    }

    public DefinedStructureManager getStructureManager() {
        return this.server.getStructureManager();
    }

    public void sendVibrationParticle(VibrationPath vibrationpath) {
        BlockPosition blockposition = vibrationpath.getOrigin();
        ClientboundAddVibrationSignalPacket clientboundaddvibrationsignalpacket = new ClientboundAddVibrationSignalPacket(vibrationpath);

        this.players.forEach((entityplayer) -> {
            this.sendParticles(entityplayer, false, (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ(), clientboundaddvibrationsignalpacket);
        });
    }

    public <T extends ParticleParam> int sendParticles(T t0, double d0, double d1, double d2, int i, double d3, double d4, double d5, double d6) {
        // CraftBukkit - visibility api support
        return sendParticles(null, t0, d0, d1, d2, i, d3, d4, d5, d6, false);
    }

    public <T extends ParticleParam> int sendParticles(EntityPlayer sender, T t0, double d0, double d1, double d2, int i, double d3, double d4, double d5, double d6, boolean force) {
        PacketPlayOutWorldParticles packetplayoutworldparticles = new PacketPlayOutWorldParticles(t0, force, d0, d1, d2, (float) d3, (float) d4, (float) d5, (float) d6, i);
        // CraftBukkit end
        int j = 0;

        for (int k = 0; k < this.players.size(); ++k) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(k);
            if (sender != null && !entityplayer.getBukkitEntity().canSee(sender.getBukkitEntity())) continue; // CraftBukkit

            if (this.sendParticles(entityplayer, force, d0, d1, d2, packetplayoutworldparticles)) { // CraftBukkit
                ++j;
            }
        }

        return j;
    }

    public <T extends ParticleParam> boolean sendParticles(EntityPlayer entityplayer, T t0, boolean flag, double d0, double d1, double d2, int i, double d3, double d4, double d5, double d6) {
        Packet<?> packet = new PacketPlayOutWorldParticles(t0, flag, d0, d1, d2, (float) d3, (float) d4, (float) d5, (float) d6, i);

        return this.sendParticles(entityplayer, flag, d0, d1, d2, packet);
    }

    private boolean sendParticles(EntityPlayer entityplayer, boolean flag, double d0, double d1, double d2, Packet<?> packet) {
        if (entityplayer.getLevel() != this) {
            return false;
        } else {
            BlockPosition blockposition = entityplayer.blockPosition();

            if (blockposition.closerThan((IPosition) (new Vec3D(d0, d1, d2)), flag ? 512.0D : 32.0D)) {
                entityplayer.connection.send(packet);
                return true;
            } else {
                return false;
            }
        }
    }

    @Nullable
    @Override
    public Entity getEntity(int i) {
        return (Entity) this.getEntities().get(i);
    }

    /** @deprecated */
    @Deprecated
    @Nullable
    public Entity getEntityOrPart(int i) {
        Entity entity = (Entity) this.getEntities().get(i);

        return entity != null ? entity : (Entity) this.dragonParts.get(i);
    }

    @Nullable
    public Entity getEntity(UUID uuid) {
        return (Entity) this.getEntities().get(uuid);
    }

    @Nullable
    public BlockPosition findNearestMapFeature(StructureGenerator<?> structuregenerator, BlockPosition blockposition, int i, boolean flag) {
        return !this.serverLevelData.worldGenSettings().generateFeatures() ? null : this.getChunkSource().getGenerator().findNearestMapFeature(this, structuregenerator, blockposition, i, flag); // CraftBukkit
    }

    @Nullable
    public BlockPosition findNearestBiome(BiomeBase biomebase, BlockPosition blockposition, int i, int j) {
        return this.getChunkSource().getGenerator().getBiomeSource().findBiomeHorizontal(blockposition.getX(), blockposition.getY(), blockposition.getZ(), i, j, (biomebase1) -> {
            return biomebase1 == biomebase;
        }, this.random, true, this.getChunkSource().getGenerator().climateSampler());
    }

    @Override
    public CraftingManager getRecipeManager() {
        return this.server.getRecipeManager();
    }

    @Override
    public ITagRegistry getTagManager() {
        return this.server.getTags();
    }

    @Override
    public boolean noSave() {
        return this.noSave;
    }

    @Override
    public IRegistryCustom registryAccess() {
        return this.server.registryAccess();
    }

    public WorldPersistentData getDataStorage() {
        return this.getChunkSource().getDataStorage();
    }

    @Nullable
    @Override
    public WorldMap getMapData(String s) {
        // CraftBukkit start
        return (WorldMap) this.getServer().overworld().getDataStorage().get((nbttagcompound) -> {
            // We only get here when the data file exists, but is not a valid map
            WorldMap newMap = WorldMap.load(nbttagcompound);
            newMap.id = s;
            MapInitializeEvent event = new MapInitializeEvent(newMap.mapView);
            Bukkit.getServer().getPluginManager().callEvent(event);
            return newMap;
        }, s);
        // CraftBukkit end
    }

    @Override
    public void setMapData(String s, WorldMap worldmap) {
        worldmap.id = s; // CraftBukkit
        this.getServer().overworld().getDataStorage().set(s, worldmap);
    }

    @Override
    public int getFreeMapId() {
        return ((PersistentIdCounts) this.getServer().overworld().getDataStorage().computeIfAbsent(PersistentIdCounts::load, PersistentIdCounts::new, "idcounts")).getFreeAuxValueForMap();
    }

    public void setDefaultSpawnPos(BlockPosition blockposition, float f) {
        ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(new BlockPosition(this.levelData.getXSpawn(), 0, this.levelData.getZSpawn()));

        this.levelData.setSpawn(blockposition, f);
        this.getChunkSource().removeRegionTicket(TicketType.START, chunkcoordintpair, 11, Unit.INSTANCE);
        this.getChunkSource().addRegionTicket(TicketType.START, new ChunkCoordIntPair(blockposition), 11, Unit.INSTANCE);
        this.getServer().getPlayerList().broadcastAll(new PacketPlayOutSpawnPosition(blockposition, f));
    }

    public BlockPosition getSharedSpawnPos() {
        BlockPosition blockposition = new BlockPosition(this.levelData.getXSpawn(), this.levelData.getYSpawn(), this.levelData.getZSpawn());

        if (!this.getWorldBorder().isWithinBounds(blockposition)) {
            blockposition = this.getHeightmapPos(HeightMap.Type.MOTION_BLOCKING, new BlockPosition(this.getWorldBorder().getCenterX(), 0.0D, this.getWorldBorder().getCenterZ()));
        }

        return blockposition;
    }

    public float getSharedSpawnAngle() {
        return this.levelData.getSpawnAngle();
    }

    public LongSet getForcedChunks() {
        ForcedChunk forcedchunk = (ForcedChunk) this.getDataStorage().get(ForcedChunk::load, "chunks");

        return (LongSet) (forcedchunk != null ? LongSets.unmodifiable(forcedchunk.getChunks()) : LongSets.EMPTY_SET);
    }

    public boolean setChunkForced(int i, int j, boolean flag) {
        ForcedChunk forcedchunk = (ForcedChunk) this.getDataStorage().computeIfAbsent(ForcedChunk::load, ForcedChunk::new, "chunks");
        ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i, j);
        long k = chunkcoordintpair.toLong();
        boolean flag1;

        if (flag) {
            flag1 = forcedchunk.getChunks().add(k);
            if (flag1) {
                this.getChunk(i, j);
            }
        } else {
            flag1 = forcedchunk.getChunks().remove(k);
        }

        forcedchunk.setDirty(flag1);
        if (flag1) {
            this.getChunkSource().updateChunkForced(chunkcoordintpair, flag);
        }

        return flag1;
    }

    @Override
    public List<EntityPlayer> players() {
        return this.players;
    }

    @Override
    public void onBlockStateChange(BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1) {
        Optional<VillagePlaceType> optional = VillagePlaceType.forState(iblockdata);
        Optional<VillagePlaceType> optional1 = VillagePlaceType.forState(iblockdata1);

        if (!Objects.equals(optional, optional1)) {
            BlockPosition blockposition1 = blockposition.immutable();

            optional.ifPresent((villageplacetype) -> {
                this.getServer().execute(() -> {
                    this.getPoiManager().remove(blockposition1);
                    PacketDebug.sendPoiRemovedPacket(this, blockposition1);
                });
            });
            optional1.ifPresent((villageplacetype) -> {
                this.getServer().execute(() -> {
                    this.getPoiManager().add(blockposition1, villageplacetype);
                    PacketDebug.sendPoiAddedPacket(this, blockposition1);
                });
            });
        }
    }

    public VillagePlace getPoiManager() {
        return this.getChunkSource().getPoiManager();
    }

    public boolean isVillage(BlockPosition blockposition) {
        return this.isCloseToVillage(blockposition, 1);
    }

    public boolean isVillage(SectionPosition sectionposition) {
        return this.isVillage(sectionposition.center());
    }

    public boolean isCloseToVillage(BlockPosition blockposition, int i) {
        return i > 6 ? false : this.sectionsToVillage(SectionPosition.of(blockposition)) <= i;
    }

    public int sectionsToVillage(SectionPosition sectionposition) {
        return this.getPoiManager().sectionsToVillage(sectionposition);
    }

    public PersistentRaid getRaids() {
        return this.raids;
    }

    @Nullable
    public Raid getRaidAt(BlockPosition blockposition) {
        return this.raids.getNearbyRaid(blockposition, 9216);
    }

    public boolean isRaided(BlockPosition blockposition) {
        return this.getRaidAt(blockposition) != null;
    }

    public void onReputationEvent(ReputationEvent reputationevent, Entity entity, ReputationHandler reputationhandler) {
        reputationhandler.onReputationEventFrom(reputationevent, entity);
    }

    public void saveDebugReport(Path path) throws IOException {
        PlayerChunkMap playerchunkmap = this.getChunkSource().chunkMap;
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path.resolve("stats.txt"));

        try {
            bufferedwriter.write(String.format("spawning_chunks: %d\n", playerchunkmap.getDistanceManager().getNaturalSpawnChunkCount()));
            SpawnerCreature.d spawnercreature_d = this.getChunkSource().getLastSpawnState();

            if (spawnercreature_d != null) {
                ObjectIterator objectiterator = spawnercreature_d.getMobCategoryCounts().object2IntEntrySet().iterator();

                while (objectiterator.hasNext()) {
                    Entry<EnumCreatureType> entry = (Entry) objectiterator.next();

                    bufferedwriter.write(String.format("spawn_count.%s: %d\n", ((EnumCreatureType) entry.getKey()).getName(), entry.getIntValue()));
                }
            }

            bufferedwriter.write(String.format("entities: %s\n", this.entityManager.gatherStats()));
            bufferedwriter.write(String.format("block_entity_tickers: %d\n", this.blockEntityTickers.size()));
            bufferedwriter.write(String.format("block_ticks: %d\n", this.getBlockTicks().count()));
            bufferedwriter.write(String.format("fluid_ticks: %d\n", this.getFluidTicks().count()));
            bufferedwriter.write("distance_manager: " + playerchunkmap.getDistanceManager().getDebugStatus() + "\n");
            bufferedwriter.write(String.format("pending_tasks: %d\n", this.getChunkSource().getPendingTasksCount()));
        } catch (Throwable throwable) {
            if (bufferedwriter != null) {
                try {
                    bufferedwriter.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            }

            throw throwable;
        }

        if (bufferedwriter != null) {
            bufferedwriter.close();
        }

        CrashReport crashreport = new CrashReport("Level dump", new Exception("dummy"));

        this.fillReportDetails(crashreport);
        BufferedWriter bufferedwriter1 = Files.newBufferedWriter(path.resolve("example_crash.txt"));

        try {
            bufferedwriter1.write(crashreport.getFriendlyReport());
        } catch (Throwable throwable2) {
            if (bufferedwriter1 != null) {
                try {
                    bufferedwriter1.close();
                } catch (Throwable throwable3) {
                    throwable2.addSuppressed(throwable3);
                }
            }

            throw throwable2;
        }

        if (bufferedwriter1 != null) {
            bufferedwriter1.close();
        }

        Path path1 = path.resolve("chunks.csv");
        BufferedWriter bufferedwriter2 = Files.newBufferedWriter(path1);

        try {
            playerchunkmap.dumpChunks(bufferedwriter2);
        } catch (Throwable throwable4) {
            if (bufferedwriter2 != null) {
                try {
                    bufferedwriter2.close();
                } catch (Throwable throwable5) {
                    throwable4.addSuppressed(throwable5);
                }
            }

            throw throwable4;
        }

        if (bufferedwriter2 != null) {
            bufferedwriter2.close();
        }

        Path path2 = path.resolve("entity_chunks.csv");
        BufferedWriter bufferedwriter3 = Files.newBufferedWriter(path2);

        try {
            this.entityManager.dumpSections(bufferedwriter3);
        } catch (Throwable throwable6) {
            if (bufferedwriter3 != null) {
                try {
                    bufferedwriter3.close();
                } catch (Throwable throwable7) {
                    throwable6.addSuppressed(throwable7);
                }
            }

            throw throwable6;
        }

        if (bufferedwriter3 != null) {
            bufferedwriter3.close();
        }

        Path path3 = path.resolve("entities.csv");
        BufferedWriter bufferedwriter4 = Files.newBufferedWriter(path3);

        try {
            dumpEntities(bufferedwriter4, this.getEntities().getAll());
        } catch (Throwable throwable8) {
            if (bufferedwriter4 != null) {
                try {
                    bufferedwriter4.close();
                } catch (Throwable throwable9) {
                    throwable8.addSuppressed(throwable9);
                }
            }

            throw throwable8;
        }

        if (bufferedwriter4 != null) {
            bufferedwriter4.close();
        }

        Path path4 = path.resolve("block_entities.csv");
        BufferedWriter bufferedwriter5 = Files.newBufferedWriter(path4);

        try {
            this.dumpBlockEntityTickers(bufferedwriter5);
        } catch (Throwable throwable10) {
            if (bufferedwriter5 != null) {
                try {
                    bufferedwriter5.close();
                } catch (Throwable throwable11) {
                    throwable10.addSuppressed(throwable11);
                }
            }

            throw throwable10;
        }

        if (bufferedwriter5 != null) {
            bufferedwriter5.close();
        }

    }

    private static void dumpEntities(Writer writer, Iterable<Entity> iterable) throws IOException {
        CSVWriter csvwriter = CSVWriter.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("uuid").addColumn("type").addColumn("alive").addColumn("display_name").addColumn("custom_name").build(writer);
        Iterator iterator = iterable.iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();
            IChatBaseComponent ichatbasecomponent = entity.getCustomName();
            IChatBaseComponent ichatbasecomponent1 = entity.getDisplayName();

            csvwriter.writeRow(entity.getX(), entity.getY(), entity.getZ(), entity.getUUID(), IRegistry.ENTITY_TYPE.getKey(entity.getType()), entity.isAlive(), ichatbasecomponent1.getString(), ichatbasecomponent != null ? ichatbasecomponent.getString() : null);
        }

    }

    private void dumpBlockEntityTickers(Writer writer) throws IOException {
        CSVWriter csvwriter = CSVWriter.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("type").build(writer);
        Iterator iterator = this.blockEntityTickers.iterator();

        while (iterator.hasNext()) {
            TickingBlockEntity tickingblockentity = (TickingBlockEntity) iterator.next();
            BlockPosition blockposition = tickingblockentity.getPos();

            csvwriter.writeRow(blockposition.getX(), blockposition.getY(), blockposition.getZ(), tickingblockentity.getType());
        }

    }

    @VisibleForTesting
    public void clearBlockEvents(StructureBoundingBox structureboundingbox) {
        this.blockEvents.removeIf((blockactiondata) -> {
            return structureboundingbox.isInside(blockactiondata.pos());
        });
    }

    @Override
    public void blockUpdated(BlockPosition blockposition, Block block) {
        if (!this.isDebug()) {
            // CraftBukkit start
            if (populating) {
                return;
            }
            // CraftBukkit end
            this.updateNeighborsAt(blockposition, block);
        }

    }

    @Override
    public float getShade(EnumDirection enumdirection, boolean flag) {
        return 1.0F;
    }

    public Iterable<Entity> getAllEntities() {
        return this.getEntities().getAll();
    }

    public String toString() {
        return "ServerLevel[" + this.serverLevelData.getLevelName() + "]";
    }

    public boolean isFlat() {
        return this.serverLevelData.worldGenSettings().isFlatWorld(); // CraftBukkit
    }

    @Override
    public long getSeed() {
        return this.serverLevelData.worldGenSettings().seed(); // CraftBukkit
    }

    @Nullable
    public EnderDragonBattle dragonFight() {
        return this.dragonFight;
    }

    @Override
    public List<? extends StructureStart<?>> startsForFeature(SectionPosition sectionposition, StructureGenerator<?> structuregenerator) {
        return this.structureFeatureManager().startsForFeature(sectionposition, structuregenerator);
    }

    @Override
    public WorldServer getLevel() {
        return this;
    }

    @VisibleForTesting
    public String getWatchdogStats() {
        return String.format("players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s", this.players.size(), this.entityManager.gatherStats(), getTypeCount(this.entityManager.getEntityGetter().getAll(), (entity) -> {
            return IRegistry.ENTITY_TYPE.getKey(entity.getType()).toString();
        }), this.blockEntityTickers.size(), getTypeCount(this.blockEntityTickers, TickingBlockEntity::getType), this.getBlockTicks().count(), this.getFluidTicks().count(), this.gatherChunkSourceStats());
    }

    private static <T> String getTypeCount(Iterable<T> iterable, Function<T, String> function) {
        try {
            Object2IntOpenHashMap<String> object2intopenhashmap = new Object2IntOpenHashMap();
            Iterator<T> iterator = iterable.iterator(); // CraftBukkit - decompile error

            while (iterator.hasNext()) {
                T t0 = iterator.next();
                String s = (String) function.apply(t0);

                object2intopenhashmap.addTo(s, 1);
            }

            return (String) object2intopenhashmap.object2IntEntrySet().stream().sorted(Comparator.comparing(Entry<String>::getIntValue).reversed()).limit(5L).map((entry) -> { // CraftBukkit - decompile error
                String s1 = (String) entry.getKey();

                return s1 + ":" + entry.getIntValue();
            }).collect(Collectors.joining(","));
        } catch (Exception exception) {
            return "";
        }
    }

    public static void makeObsidianPlatform(WorldServer worldserver) {
        // CraftBukkit start
        WorldServer.makeObsidianPlatform(worldserver, null);
    }

    public static void makeObsidianPlatform(WorldServer worldserver, Entity entity) {
        // CraftBukkit end
        BlockPosition blockposition = WorldServer.END_SPAWN_POINT;
        int i = blockposition.getX();
        int j = blockposition.getY() - 2;
        int k = blockposition.getZ();

        // CraftBukkit start
        org.bukkit.craftbukkit.util.BlockStateListPopulator blockList = new org.bukkit.craftbukkit.util.BlockStateListPopulator(worldserver);
        BlockPosition.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((blockposition1) -> {
            blockList.setBlock(blockposition1, Blocks.AIR.defaultBlockState(), 3);
        });
        BlockPosition.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2).forEach((blockposition1) -> {
            blockList.setBlock(blockposition1, Blocks.OBSIDIAN.defaultBlockState(), 3);
        });
        org.bukkit.World bworld = worldserver.getWorld();
        org.bukkit.event.world.PortalCreateEvent portalEvent = new org.bukkit.event.world.PortalCreateEvent((List<org.bukkit.block.BlockState>) (List) blockList.getList(), bworld, (entity == null) ? null : entity.getBukkitEntity(), org.bukkit.event.world.PortalCreateEvent.CreateReason.END_PLATFORM);

        worldserver.getCraftServer().getPluginManager().callEvent(portalEvent);
        if (!portalEvent.isCancelled()) {
            blockList.updateList();
        }
        // CraftBukkit end
    }

    @Override
    public LevelEntityGetter<Entity> getEntities() {
        org.spigotmc.AsyncCatcher.catchOp("Chunk getEntities call"); // Spigot
        return this.entityManager.getEntityGetter();
    }

    public void addLegacyChunkEntities(Stream<Entity> stream) {
        this.entityManager.addLegacyChunkEntities(stream);
    }

    public void addWorldGenChunkEntities(Stream<Entity> stream) {
        this.entityManager.addWorldGenChunkEntities(stream);
    }

    public void startTickingChunk(Chunk chunk) {
        chunk.unpackTicks(this.getLevelData().getGameTime());
    }

    public void onStructureStartsAvailable(IChunkAccess ichunkaccess) {
        this.server.execute(() -> {
            this.structureCheck.onStructureLoad(ichunkaccess.getPos(), ichunkaccess.getAllStarts());
        });
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.entityManager.close();
    }

    @Override
    public String gatherChunkSourceStats() {
        String s = this.chunkSource.gatherStats();

        return "Chunks[S] W: " + s + " E: " + this.entityManager.gatherStats();
    }

    public boolean areEntitiesLoaded(long i) {
        return this.entityManager.areEntitiesLoaded(i);
    }

    private boolean isPositionTickingWithEntitiesLoaded(long i) {
        return this.areEntitiesLoaded(i) && this.chunkSource.isPositionTicking(i);
    }

    public boolean isPositionEntityTicking(BlockPosition blockposition) {
        return this.entityManager.isPositionTicking(blockposition);
    }

    public boolean isPositionEntityTicking(ChunkCoordIntPair chunkcoordintpair) {
        return this.entityManager.isPositionTicking(chunkcoordintpair);
    }

    private final class a implements LevelCallback<Entity> {

        a() {}

        public void onCreated(Entity entity) {}

        public void onDestroyed(Entity entity) {
            WorldServer.this.getScoreboard().entityRemoved(entity);
        }

        public void onTickingStart(Entity entity) {
            WorldServer.this.entityTickList.add(entity);
        }

        public void onTickingEnd(Entity entity) {
            WorldServer.this.entityTickList.remove(entity);
        }

        public void onTrackingStart(Entity entity) {
            org.spigotmc.AsyncCatcher.catchOp("entity register"); // Spigot
            WorldServer.this.getChunkSource().addEntity(entity);
            if (entity instanceof EntityPlayer) {
                EntityPlayer entityplayer = (EntityPlayer) entity;

                WorldServer.this.players.add(entityplayer);
                WorldServer.this.updateSleepingPlayerList();
            }

            if (entity instanceof EntityInsentient) {
                EntityInsentient entityinsentient = (EntityInsentient) entity;

                if (WorldServer.this.isUpdatingNavigations) {
                    String s = "onTrackingStart called during navigation iteration";

                    SystemUtils.logAndPauseIfInIde("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
                }

                WorldServer.this.navigatingMobs.add(entityinsentient);
            }

            if (entity instanceof EntityEnderDragon) {
                EntityEnderDragon entityenderdragon = (EntityEnderDragon) entity;
                EntityComplexPart[] aentitycomplexpart = entityenderdragon.getSubEntities();
                int i = aentitycomplexpart.length;

                for (int j = 0; j < i; ++j) {
                    EntityComplexPart entitycomplexpart = aentitycomplexpart[j];

                    WorldServer.this.dragonParts.put(entitycomplexpart.getId(), entitycomplexpart);
                }
            }

            entity.valid = true; // CraftBukkit
        }

        public void onTrackingEnd(Entity entity) {
            org.spigotmc.AsyncCatcher.catchOp("entity unregister"); // Spigot
            // Spigot start
            if ( entity instanceof EntityHuman )
            {
                getServer().levels.values().stream().map( WorldServer::getDataStorage ).forEach( (worldData) ->
                {
                    for (Object o : worldData.cache.values() )
                    {
                        if ( o instanceof WorldMap )
                        {
                            WorldMap map = (WorldMap) o;
                            map.carriedByPlayers.remove( (EntityHuman) entity );
                            for ( Iterator<WorldMap.WorldMapHumanTracker> iter = (Iterator<WorldMap.WorldMapHumanTracker>) map.carriedBy.iterator(); iter.hasNext(); )
                            {
                                if ( iter.next().player == entity )
                                {
                                    iter.remove();
                                }
                            }
                        }
                    }
                } );
            }
            // Spigot end
            // Spigot Start
            if (entity.getBukkitEntity() instanceof org.bukkit.inventory.InventoryHolder && (!(entity instanceof EntityPlayer) || entity.getRemovalReason() != Entity.RemovalReason.KILLED)) { // SPIGOT-6876: closeInventory clears death message
                for (org.bukkit.entity.HumanEntity h : Lists.newArrayList(((org.bukkit.inventory.InventoryHolder) entity.getBukkitEntity()).getInventory().getViewers())) {
                    h.closeInventory();
                }
            }
            // Spigot End
            WorldServer.this.getChunkSource().removeEntity(entity);
            if (entity instanceof EntityPlayer) {
                EntityPlayer entityplayer = (EntityPlayer) entity;

                WorldServer.this.players.remove(entityplayer);
                WorldServer.this.updateSleepingPlayerList();
            }

            if (entity instanceof EntityInsentient) {
                EntityInsentient entityinsentient = (EntityInsentient) entity;

                if (WorldServer.this.isUpdatingNavigations) {
                    String s = "onTrackingStart called during navigation iteration";

                    SystemUtils.logAndPauseIfInIde("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
                }

                WorldServer.this.navigatingMobs.remove(entityinsentient);
            }

            if (entity instanceof EntityEnderDragon) {
                EntityEnderDragon entityenderdragon = (EntityEnderDragon) entity;
                EntityComplexPart[] aentitycomplexpart = entityenderdragon.getSubEntities();
                int i = aentitycomplexpart.length;

                for (int j = 0; j < i; ++j) {
                    EntityComplexPart entitycomplexpart = aentitycomplexpart[j];

                    WorldServer.this.dragonParts.remove(entitycomplexpart.getId());
                }
            }

            GameEventListenerRegistrar gameeventlistenerregistrar = entity.getGameEventListenerRegistrar();

            if (gameeventlistenerregistrar != null) {
                gameeventlistenerregistrar.onListenerRemoved(entity.level);
            }

            // CraftBukkit start
            entity.valid = false;
            if (!(entity instanceof EntityPlayer)) {
                for (EntityPlayer player : players) {
                    player.getBukkitEntity().onEntityRemove(entity);
                }
            }
            // CraftBukkit end
        }
    }
}
