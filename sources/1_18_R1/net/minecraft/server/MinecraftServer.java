package net.minecraft.server;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.gametest.framework.GameTestHarnessTicker;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutServerDifficulty;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateTime;
import net.minecraft.network.protocol.status.ServerPing;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.bossevents.BossBattleCustomData;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.DemoPlayerInteractManager;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.PlayerInteractManager;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldProviderNormal;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.server.level.progress.WorldLoadListenerFactory;
import net.minecraft.server.network.ITextFilter;
import net.minecraft.server.network.ServerConnection;
import net.minecraft.server.packs.repository.ResourcePackLoader;
import net.minecraft.server.packs.repository.ResourcePackRepository;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.players.OpListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserCache;
import net.minecraft.server.players.WhiteList;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.util.CircularTimer;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MinecraftEncryption;
import net.minecraft.util.ModCheck;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.profiling.GameProfilerTick;
import net.minecraft.util.profiling.MethodProfilerResults;
import net.minecraft.util.profiling.MethodProfilerResultsEmpty;
import net.minecraft.util.profiling.MethodProfilerResultsField;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.ServerMetricsSamplersProvider;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.IAsyncTaskHandlerReentrant;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.MobSpawnerCat;
import net.minecraft.world.entity.npc.MobSpawnerTrader;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.ForcedChunk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.IWorldBorderListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.MobSpawnerPatrol;
import net.minecraft.world.level.levelgen.MobSpawnerPhantom;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.IWorldDataServer;
import net.minecraft.world.level.storage.PersistentCommandStorage;
import net.minecraft.world.level.storage.SaveData;
import net.minecraft.world.level.storage.SavedFile;
import net.minecraft.world.level.storage.SecondaryWorldData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.WorldNBTStorage;
import net.minecraft.world.level.storage.WorldPersistentData;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootPredicateManager;
import net.minecraft.world.level.storage.loot.LootTableRegistry;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.levelgen.ChunkGeneratorAbstract;
import net.minecraft.world.level.storage.WorldDataServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.Main;
import org.bukkit.craftbukkit.generator.CustomWorldChunkManager;
import org.bukkit.event.server.ServerLoadEvent;
// CraftBukkit end

import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.spigotmc.SlackActivityAccountant; // Spigot

public abstract class MinecraftServer extends IAsyncTaskHandlerReentrant<TickTask> implements ICommandListener, AutoCloseable {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String VANILLA_BRAND = "vanilla";
    private static final float AVERAGE_TICK_TIME_SMOOTHING = 0.8F;
    private static final int TICK_STATS_SPAN = 100;
    public static final int MS_PER_TICK = 50;
    private static final int OVERLOADED_THRESHOLD = 2000;
    private static final int OVERLOADED_WARNING_INTERVAL = 15000;
    public static final String LEVEL_STORAGE_PROTOCOL = "level";
    public static final String LEVEL_STORAGE_SCHEMA = "level://";
    private static final long STATUS_EXPIRE_TIME_NS = 5000000000L;
    private static final int MAX_STATUS_PLAYER_SAMPLE = 12;
    public static final String MAP_RESOURCE_FILE = "resources.zip";
    public static final File USERID_CACHE_FILE = new File("usercache.json");
    public static final int START_CHUNK_RADIUS = 11;
    private static final int START_TICKING_CHUNK_COUNT = 441;
    private static final int AUTOSAVE_INTERVAL = 6000;
    private static final int MAX_TICK_LATENCY = 3;
    public static final int ABSOLUTE_MAX_WORLD_SIZE = 29999984;
    public static final WorldSettings DEMO_SETTINGS = new WorldSettings("Demo World", EnumGamemode.SURVIVAL, false, EnumDifficulty.NORMAL, false, new GameRules(), DataPackConfiguration.DEFAULT);
    private static final long DELAYED_TASKS_TICK_EXTENSION = 50L;
    public static final GameProfile ANONYMOUS_PLAYER_PROFILE = new GameProfile(SystemUtils.NIL_UUID, "Anonymous Player");
    public Convertable.ConversionSession storageSource;
    public final WorldNBTStorage playerDataStorage;
    private final List<Runnable> tickables = Lists.newArrayList();
    private MetricsRecorder metricsRecorder;
    private GameProfilerFiller profiler;
    private Consumer<MethodProfilerResults> onMetricsRecordingStopped;
    private Consumer<Path> onMetricsRecordingFinished;
    private boolean willStartRecordingMetrics;
    @Nullable
    private MinecraftServer.a debugCommandProfiler;
    private boolean debugCommandProfilerDelayStart;
    private ServerConnection connection;
    public final WorldLoadListenerFactory progressListenerFactory;
    private final ServerPing status;
    private final Random random;
    public final DataFixer fixerUpper;
    private String localIp;
    private int port;
    public final IRegistryCustom.Dimension registryHolder;
    public final Map<ResourceKey<World>, WorldServer> levels;
    private PlayerList playerList;
    private volatile boolean running;
    private boolean stopped;
    private int tickCount;
    protected final Proxy proxy;
    private boolean onlineMode;
    private boolean preventProxyConnections;
    private boolean pvp;
    private boolean allowFlight;
    @Nullable
    private String motd;
    private int playerIdleTimeout;
    public final long[] tickTimes;
    @Nullable
    private KeyPair keyPair;
    @Nullable
    private String singleplayerName;
    private boolean isDemo;
    private String resourcePack;
    private String resourcePackHash;
    private volatile boolean isReady;
    private long lastOverloadWarning;
    private final MinecraftSessionService sessionService;
    @Nullable
    private final GameProfileRepository profileRepository;
    @Nullable
    private final UserCache profileCache;
    private long lastServerStatus;
    public final Thread serverThread;
    private long nextTickTime;
    private long delayedTasksMaxNextTickTime;
    private boolean mayHaveDelayedTasks;
    private final ResourcePackRepository packRepository;
    private final ScoreboardServer scoreboard;
    @Nullable
    private PersistentCommandStorage commandStorage;
    private final BossBattleCustomData customBossEvents;
    private final CustomFunctionData functionManager;
    private final CircularTimer frameTimer;
    private boolean enforceWhitelist;
    private float averageTickTime;
    public final Executor executor;
    @Nullable
    private String serverId;
    public DataPackResources resources;
    private final DefinedStructureManager structureManager;
    protected SaveData worldData;
    private volatile boolean isSaving;

    // CraftBukkit start
    public final DataPackConfiguration datapackconfiguration;
    public final RegistryReadOps<NBTBase> registryreadops;
    public org.bukkit.craftbukkit.CraftServer server;
    public OptionSet options;
    public org.bukkit.command.ConsoleCommandSender console;
    public org.bukkit.command.RemoteConsoleCommandSender remoteConsole;
    public ConsoleReader reader;
    public static int currentTick = (int) (System.currentTimeMillis() / 50);
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
    public int autosavePeriod;
    public CommandDispatcher vanillaCommandDispatcher;
    private boolean forceTicks;
    // CraftBukkit end
    // Spigot start
    public static final int TPS = 20;
    public static final int TICK_TIME = 1000000000 / TPS;
    private static final int SAMPLE_INTERVAL = 100;
    public final double[] recentTps = new double[ 3 ];
    public final SlackActivityAccountant slackActivityAccountant = new SlackActivityAccountant();
    // Spigot end

    public static <S extends MinecraftServer> S spin(Function<Thread, S> function) {
        AtomicReference<S> atomicreference = new AtomicReference();
        Thread thread = new Thread(() -> {
            ((MinecraftServer) atomicreference.get()).runServer();
        }, "Server thread");

        thread.setUncaughtExceptionHandler((thread1, throwable) -> {
            MinecraftServer.LOGGER.error(throwable);
        });
        if (Runtime.getRuntime().availableProcessors() > 4) {
            thread.setPriority(8);
        }

        S s0 = function.apply(thread); // CraftBukkit - decompile error

        atomicreference.set(s0);
        thread.start();
        return s0;
    }

    public MinecraftServer(OptionSet options, DataPackConfiguration datapackconfiguration, Thread thread, IRegistryCustom.Dimension iregistrycustom_dimension, Convertable.ConversionSession convertable_conversionsession, SaveData savedata, ResourcePackRepository resourcepackrepository, Proxy proxy, DataFixer datafixer, DataPackResources datapackresources, @Nullable MinecraftSessionService minecraftsessionservice, @Nullable GameProfileRepository gameprofilerepository, @Nullable UserCache usercache, WorldLoadListenerFactory worldloadlistenerfactory) {
        super("Server");
        this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
        this.profiler = this.metricsRecorder.getProfiler();
        this.onMetricsRecordingStopped = (methodprofilerresults) -> {
            this.stopRecordingMetrics();
        };
        this.onMetricsRecordingFinished = (path) -> {
        };
        this.status = new ServerPing();
        this.random = new Random();
        this.port = -1;
        this.levels = Maps.newLinkedHashMap(); // CraftBukkit - keep order, k+v already use identity methods
        this.running = true;
        this.tickTimes = new long[100];
        this.resourcePack = "";
        this.resourcePackHash = "";
        this.nextTickTime = SystemUtils.getMillis();
        this.scoreboard = new ScoreboardServer(this);
        this.customBossEvents = new BossBattleCustomData();
        this.frameTimer = new CircularTimer();
        this.registryHolder = iregistrycustom_dimension;
        this.worldData = savedata;
        this.proxy = proxy;
        this.packRepository = resourcepackrepository;
        this.resources = datapackresources;
        this.sessionService = minecraftsessionservice;
        this.profileRepository = gameprofilerepository;
        this.profileCache = usercache;
        if (usercache != null) {
            usercache.setExecutor(this);
        }

        // this.connection = new ServerConnection(this); // Spigot
        this.progressListenerFactory = worldloadlistenerfactory;
        this.storageSource = convertable_conversionsession;
        this.playerDataStorage = convertable_conversionsession.createPlayerStorage();
        this.fixerUpper = datafixer;
        this.functionManager = new CustomFunctionData(this, datapackresources.getFunctionLibrary());
        this.structureManager = new DefinedStructureManager(datapackresources.getResourceManager(), convertable_conversionsession, datafixer);
        this.serverThread = thread;
        this.executor = SystemUtils.backgroundExecutor();
        // CraftBukkit start
        this.options = options;
        this.datapackconfiguration = datapackconfiguration;
        this.registryreadops = RegistryReadOps.createAndLoad(DynamicOpsNBT.INSTANCE, datapackresources.getResourceManager(), iregistrycustom_dimension);
        this.vanillaCommandDispatcher = datapackresources.commands; // CraftBukkit
        // Try to see if we're actually running in a terminal, disable jline if not
        if (System.console() == null && System.getProperty("jline.terminal") == null) {
            System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
            Main.useJline = false;
        }

        try {
            reader = new ConsoleReader(System.in, System.out);
            reader.setExpandEvents(false); // Avoid parsing exceptions for uncommonly used event designators
        } catch (Throwable e) {
            try {
                // Try again with jline disabled for Windows users without C++ 2008 Redistributable
                System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                System.setProperty("user.language", "en");
                Main.useJline = false;
                reader = new ConsoleReader(System.in, System.out);
                reader.setExpandEvents(false);
            } catch (IOException ex) {
                LOGGER.warn((String) null, ex);
            }
        }
        Runtime.getRuntime().addShutdownHook(new org.bukkit.craftbukkit.util.ServerShutdownThread(this));
    }
    // CraftBukkit end

    private void readScoreboard(WorldPersistentData worldpersistentdata) {
        ScoreboardServer scoreboardserver = this.getScoreboard();

        Objects.requireNonNull(scoreboardserver);
        Function<net.minecraft.nbt.NBTTagCompound, net.minecraft.world.scores.PersistentScoreboard> function = scoreboardserver::createData; // CraftBukkit - decompile error
        ScoreboardServer scoreboardserver1 = this.getScoreboard();

        Objects.requireNonNull(scoreboardserver1);
        worldpersistentdata.computeIfAbsent(function, scoreboardserver1::createData, "scoreboard");
    }

    protected abstract boolean initServer() throws IOException;

    protected void loadLevel(String s) { // CraftBukkit
        if (!JvmProfiler.INSTANCE.isRunning()) {
            ;
        }

        boolean flag = false;
        ProfiledDuration profiledduration = JvmProfiler.INSTANCE.onWorldLoadedStarted();

        loadWorld0(s); // CraftBukkit

        if (profiledduration != null) {
            profiledduration.finish();
        }

        if (flag) {
            try {
                JvmProfiler.INSTANCE.stop();
            } catch (Throwable throwable) {
                MinecraftServer.LOGGER.warn("Failed to stop JFR profiling", throwable);
            }
        }

    }

    // CraftBukkit start
    private void loadWorld0(String s) {
        Convertable.ConversionSession worldSession = this.storageSource;
        IRegistryCustom.Dimension iregistrycustom_dimension = this.registryHolder;
        WorldDataServer overworldData = (WorldDataServer) worldSession.getDataTag(registryreadops, datapackconfiguration);
        if (overworldData == null) {
            WorldSettings worldsettings;
            GeneratorSettings generatorsettings;

            if (this.isDemo()) {
                worldsettings = MinecraftServer.DEMO_SETTINGS;
                generatorsettings = GeneratorSettings.demoSettings(iregistrycustom_dimension);
            } else {
                DedicatedServerProperties dedicatedserverproperties = ((DedicatedServer) this).getProperties();

                worldsettings = new WorldSettings(dedicatedserverproperties.levelName, dedicatedserverproperties.gamemode, dedicatedserverproperties.hardcore, dedicatedserverproperties.difficulty, false, new GameRules(), datapackconfiguration);
                generatorsettings = options.has("bonusChest") ? dedicatedserverproperties.getWorldGenSettings(iregistrycustom_dimension).withBonusChest() : dedicatedserverproperties.getWorldGenSettings(iregistrycustom_dimension);
            }

            overworldData = new WorldDataServer(worldsettings, generatorsettings, Lifecycle.stable());
        }

        GeneratorSettings overworldSettings = overworldData.worldGenSettings();
        RegistryMaterials<WorldDimension> registrymaterials = overworldSettings.dimensions();
        for (Entry<ResourceKey<WorldDimension>, WorldDimension> entry : registrymaterials.entrySet()) {
            ResourceKey<WorldDimension> dimensionKey = entry.getKey();

            WorldServer world;
            int dimension = 0;

            if (dimensionKey == WorldDimension.NETHER) {
                if (isNetherEnabled()) {
                    dimension = -1;
                } else {
                    continue;
                }
            } else if (dimensionKey == WorldDimension.END) {
                if (server.getAllowEnd()) {
                    dimension = 1;
                } else {
                    continue;
                }
            } else if (dimensionKey != WorldDimension.OVERWORLD) {
                dimension = -999;
            }

            String worldType = (dimension == -999) ? dimensionKey.location().getNamespace() + "_" + dimensionKey.location().getPath() : org.bukkit.World.Environment.getEnvironment(dimension).toString().toLowerCase();
            String name = (dimensionKey == WorldDimension.OVERWORLD) ? s : s + "_" + worldType;
            if (dimension != 0) {
                File newWorld = Convertable.getStorageFolder(new File(name).toPath(), dimensionKey).toFile();
                File oldWorld = Convertable.getStorageFolder(new File(s).toPath(), dimensionKey).toFile();
                File oldLevelDat = new File(new File(s), "level.dat"); // The data folders exist on first run as they are created in the PersistentCollection constructor above, but the level.dat won't

                if (!newWorld.isDirectory() && oldWorld.isDirectory() && oldLevelDat.isFile()) {
                    MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder required ----");
                    MinecraftServer.LOGGER.info("Unfortunately due to the way that Minecraft implemented multiworld support in 1.6, Bukkit requires that you move your " + worldType + " folder to a new location in order to operate correctly.");
                    MinecraftServer.LOGGER.info("We will move this folder for you, but it will mean that you need to move it back should you wish to stop using Bukkit in the future.");
                    MinecraftServer.LOGGER.info("Attempting to move " + oldWorld + " to " + newWorld + "...");

                    if (newWorld.exists()) {
                        MinecraftServer.LOGGER.warn("A file or folder already exists at " + newWorld + "!");
                        MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                    } else if (newWorld.getParentFile().mkdirs()) {
                        if (oldWorld.renameTo(newWorld)) {
                            MinecraftServer.LOGGER.info("Success! To restore " + worldType + " in the future, simply move " + newWorld + " to " + oldWorld);
                            // Migrate world data too.
                            try {
                                com.google.common.io.Files.copy(oldLevelDat, new File(new File(name), "level.dat"));
                                org.apache.commons.io.FileUtils.copyDirectory(new File(new File(s), "data"), new File(new File(name), "data"));
                            } catch (IOException exception) {
                                MinecraftServer.LOGGER.warn("Unable to migrate world data.");
                            }
                            MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder complete ----");
                        } else {
                            MinecraftServer.LOGGER.warn("Could not move folder " + oldWorld + " to " + newWorld + "!");
                            MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                        }
                    } else {
                        MinecraftServer.LOGGER.warn("Could not create path for " + newWorld + "!");
                        MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                    }
                }

                try {
                    worldSession = Convertable.createDefault(server.getWorldContainer().toPath()).createAccess(name, dimensionKey);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            org.bukkit.generator.ChunkGenerator gen = this.server.getGenerator(name);
            org.bukkit.generator.BiomeProvider biomeProvider = this.server.getBiomeProvider(name);

            WorldDataServer worlddata = (WorldDataServer) worldSession.getDataTag((DynamicOps) registryreadops, datapackconfiguration);
            if (worlddata == null) {
                WorldSettings worldsettings;
                GeneratorSettings generatorsettings;

                if (this.isDemo()) {
                    worldsettings = MinecraftServer.DEMO_SETTINGS;
                    generatorsettings = GeneratorSettings.demoSettings(iregistrycustom_dimension);
                } else {
                    DedicatedServerProperties dedicatedserverproperties = ((DedicatedServer) this).getProperties();

                    worldsettings = new WorldSettings(dedicatedserverproperties.levelName, dedicatedserverproperties.gamemode, dedicatedserverproperties.hardcore, dedicatedserverproperties.difficulty, false, new GameRules(), datapackconfiguration);
                    generatorsettings = options.has("bonusChest") ? dedicatedserverproperties.getWorldGenSettings(iregistrycustom_dimension).withBonusChest() : dedicatedserverproperties.getWorldGenSettings(iregistrycustom_dimension);
                }

                worlddata = new WorldDataServer(worldsettings, generatorsettings, Lifecycle.stable());
            }
            worlddata.checkName(name); // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
            if (options.has("forceUpgrade")) {
                net.minecraft.server.Main.forceUpgrade(worldSession, DataConverterRegistry.getDataFixer(), options.has("eraseCache"), () -> {
                    return true;
                }, worlddata.worldGenSettings());
            }

            IWorldDataServer iworlddataserver = worlddata;
            GeneratorSettings generatorsettings = worlddata.worldGenSettings();
            boolean flag = generatorsettings.isDebug();
            long i = generatorsettings.seed();
            long j = BiomeManager.obfuscateSeed(i);
            List<MobSpawner> list = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(iworlddataserver));
            WorldDimension worlddimension = (WorldDimension) registrymaterials.get(dimensionKey);
            DimensionManager dimensionmanager;
            ChunkGenerator chunkgenerator;

            if (worlddimension == null) {
                dimensionmanager = (DimensionManager) this.registryHolder.registryOrThrow(IRegistry.DIMENSION_TYPE_REGISTRY).getOrThrow(DimensionManager.OVERWORLD_LOCATION);
                chunkgenerator = GeneratorSettings.makeDefaultOverworld(this.registryHolder, (new Random()).nextLong());
            } else {
                dimensionmanager = worlddimension.type();
                chunkgenerator = worlddimension.generator();
            }

            org.bukkit.generator.WorldInfo worldInfo = new org.bukkit.craftbukkit.generator.CraftWorldInfo(iworlddataserver, worldSession, org.bukkit.World.Environment.getEnvironment(dimension), dimensionmanager);
            if (biomeProvider == null && gen != null) {
                biomeProvider = gen.getDefaultBiomeProvider(worldInfo);
            }

            if (biomeProvider != null) {
                WorldChunkManager worldChunkManager = new CustomWorldChunkManager(worldInfo, biomeProvider, registryHolder.ownedRegistryOrThrow(IRegistry.BIOME_REGISTRY));
                if (chunkgenerator instanceof ChunkGeneratorAbstract) {
                    chunkgenerator = new ChunkGeneratorAbstract(((ChunkGeneratorAbstract) chunkgenerator).noises, worldChunkManager, chunkgenerator.strongholdSeed, ((ChunkGeneratorAbstract) chunkgenerator).settings);
                }
            }

            ResourceKey<World> worldKey = ResourceKey.create(IRegistry.DIMENSION_REGISTRY, dimensionKey.location());

            if (dimensionKey == WorldDimension.OVERWORLD) {
                this.worldData = worlddata;
                this.worldData.setGameType(((DedicatedServer) this).getProperties().gamemode); // From DedicatedServer.init

                WorldLoadListener worldloadlistener = this.progressListenerFactory.create(11);

                world = new WorldServer(this, this.executor, worldSession, iworlddataserver, worldKey, dimensionmanager, worldloadlistener, chunkgenerator, flag, j, list, true, org.bukkit.World.Environment.getEnvironment(dimension), gen, biomeProvider);
                WorldPersistentData worldpersistentdata = world.getDataStorage();
                this.readScoreboard(worldpersistentdata);
                this.server.scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(this, world.getScoreboard());
                this.commandStorage = new PersistentCommandStorage(worldpersistentdata);
            } else {
                WorldLoadListener worldloadlistener = this.progressListenerFactory.create(11);
                world = new WorldServer(this, this.executor, worldSession, iworlddataserver, worldKey, dimensionmanager, worldloadlistener, chunkgenerator, flag, j, ImmutableList.of(), true, org.bukkit.World.Environment.getEnvironment(dimension), gen, biomeProvider);
            }

            worlddata.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
            this.initWorld(world, worlddata, worldData, worlddata.worldGenSettings());

            this.levels.put(world.dimension(), world);
            this.getPlayerList().addWorldborderListener(world);

            if (worlddata.getCustomBossEvents() != null) {
                this.getCustomBossEvents().load(worlddata.getCustomBossEvents());
            }
        }
        this.forceDifficulty();
        for (WorldServer worldserver : this.getAllLevels()) {
            this.prepareLevels(worldserver.getChunkSource().chunkMap.progressListener, worldserver);
            worldserver.entityManager.tick(); // SPIGOT-6526: Load pending entities so they are available to the API
            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(worldserver.getWorld()));
        }

        this.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD);
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
        this.connection.acceptConnections();
    }
    // CraftBukkit end

    protected void forceDifficulty() {}

    // CraftBukkit start
    public void initWorld(WorldServer worldserver, IWorldDataServer iworlddataserver, SaveData saveData, GeneratorSettings generatorsettings) {
        boolean flag = generatorsettings.isDebug();
        // CraftBukkit start
        if (worldserver.generator != null) {
            worldserver.getWorld().getPopulators().addAll(worldserver.generator.getDefaultPopulators(worldserver.getWorld()));
        }
        WorldBorder worldborder = worldserver.getWorldBorder();

        if (!iworlddataserver.isInitialized()) {
            try {
                setInitialSpawn(worldserver, iworlddataserver, generatorsettings.generateBonusChest(), flag);
                iworlddataserver.setInitialized(true);
                if (flag) {
                    this.setupDebugLevel(this.worldData);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception initializing level");

                try {
                    worldserver.fillReportDetails(crashreport);
                } catch (Throwable throwable1) {
                    ;
                }

                throw new ReportedException(crashreport);
            }

            iworlddataserver.setInitialized(true);
        }

        worldborder.applySettings(iworlddataserver.getWorldBorder());
        this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(worldserver.getWorld())); // CraftBukkit - SPIGOT-5569
    }
    // CraftBukkit end

    private static void setInitialSpawn(WorldServer worldserver, IWorldDataServer iworlddataserver, boolean flag, boolean flag1) {
        if (flag1) {
            iworlddataserver.setSpawn(BlockPosition.ZERO.above(80), 0.0F);
        } else {
            ChunkGenerator chunkgenerator = worldserver.getChunkSource().getGenerator();
            ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(chunkgenerator.climateSampler().findSpawnPosition());
            // CraftBukkit start
            if (worldserver.generator != null) {
                Random rand = new Random(worldserver.getSeed());
                org.bukkit.Location spawn = worldserver.generator.getFixedSpawnLocation(worldserver.getWorld(), rand);

                if (spawn != null) {
                    if (spawn.getWorld() != worldserver.getWorld()) {
                        throw new IllegalStateException("Cannot set spawn point for " + iworlddataserver.getLevelName() + " to be in another world (" + spawn.getWorld().getName() + ")");
                    } else {
                        iworlddataserver.setSpawn(new BlockPosition(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ()), spawn.getYaw());
                        return;
                    }
                }
            }
            // CraftBukkit end
            int i = chunkgenerator.getSpawnHeight(worldserver);

            if (i < worldserver.getMinBuildHeight()) {
                BlockPosition blockposition = chunkcoordintpair.getWorldPosition();

                i = worldserver.getHeight(HeightMap.Type.WORLD_SURFACE, blockposition.getX() + 8, blockposition.getZ() + 8);
            }

            iworlddataserver.setSpawn(chunkcoordintpair.getWorldPosition().offset(8, i, 8), 0.0F);
            int j = 0;
            int k = 0;
            int l = 0;
            int i1 = -1;
            boolean flag2 = true;

            for (int j1 = 0; j1 < MathHelper.square(11); ++j1) {
                if (j >= -5 && j <= 5 && k >= -5 && k <= 5) {
                    BlockPosition blockposition1 = WorldProviderNormal.getSpawnPosInChunk(worldserver, new ChunkCoordIntPair(chunkcoordintpair.x + j, chunkcoordintpair.z + k));

                    if (blockposition1 != null) {
                        iworlddataserver.setSpawn(blockposition1, 0.0F);
                        break;
                    }
                }

                if (j == k || j < 0 && j == -k || j > 0 && j == 1 - k) {
                    int k1 = l;

                    l = -i1;
                    i1 = k1;
                }

                j += l;
                k += i1;
            }

            if (flag) {
                WorldGenFeatureConfigured<?, ?> worldgenfeatureconfigured = MiscOverworldFeatures.BONUS_CHEST;

                worldgenfeatureconfigured.place(worldserver, chunkgenerator, worldserver.random, new BlockPosition(iworlddataserver.getXSpawn(), iworlddataserver.getYSpawn(), iworlddataserver.getZSpawn()));
            }

        }
    }

    private void setupDebugLevel(SaveData savedata) {
        savedata.setDifficulty(EnumDifficulty.PEACEFUL);
        savedata.setDifficultyLocked(true);
        IWorldDataServer iworlddataserver = savedata.overworldData();

        iworlddataserver.setRaining(false);
        iworlddataserver.setThundering(false);
        iworlddataserver.setClearWeatherTime(1000000000);
        iworlddataserver.setDayTime(6000L);
        iworlddataserver.setGameType(EnumGamemode.SPECTATOR);
    }

    // CraftBukkit start
    public void prepareLevels(WorldLoadListener worldloadlistener, WorldServer worldserver) {
        if (!worldserver.getWorld().getKeepSpawnInMemory()) {
            return;
        }

        // WorldServer worldserver = this.overworld();
        this.forceTicks = true;
        // CraftBukkit end

        MinecraftServer.LOGGER.info("Preparing start region for dimension {}", worldserver.dimension().location());
        BlockPosition blockposition = worldserver.getSharedSpawnPos();

        worldloadlistener.updateSpawnPos(new ChunkCoordIntPair(blockposition));
        ChunkProviderServer chunkproviderserver = worldserver.getChunkSource();

        chunkproviderserver.getLightEngine().setTaskPerBatch(500);
        this.nextTickTime = SystemUtils.getMillis();
        chunkproviderserver.addRegionTicket(TicketType.START, new ChunkCoordIntPair(blockposition), 11, Unit.INSTANCE);

        while (chunkproviderserver.getTickingGenerated() != 441) {
            // CraftBukkit start
            // this.nextTickTime = SystemUtils.getMillis() + 10L;
            this.executeModerately();
            // CraftBukkit end
        }

        // CraftBukkit start
        // this.nextTickTime = SystemUtils.getMillis() + 10L;
        this.executeModerately();
        // Iterator iterator = this.levels.values().iterator();

        if (true) {
            WorldServer worldserver1 = worldserver;
            // CraftBukkit end
            ForcedChunk forcedchunk = (ForcedChunk) worldserver1.getDataStorage().get(ForcedChunk::load, "chunks");

            if (forcedchunk != null) {
                LongIterator longiterator = forcedchunk.getChunks().iterator();

                while (longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i);

                    worldserver1.getChunkSource().updateChunkForced(chunkcoordintpair, true);
                }
            }
        }

        // CraftBukkit start
        // this.nextTickTime = SystemUtils.getMillis() + 10L;
        this.executeModerately();
        // CraftBukkit end
        worldloadlistener.stop();
        chunkproviderserver.getLightEngine().setTaskPerBatch(5);
        // CraftBukkit start
        // this.updateMobSpawningFlags();
        worldserver.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());

        this.forceTicks = false;
        // CraftBukkit end
    }

    protected void detectBundledResources() {
        File file = this.storageSource.getLevelPath(SavedFile.MAP_RESOURCE_FILE).toFile();

        if (file.isFile()) {
            String s = this.storageSource.getLevelId();

            try {
                this.setResourcePack("level://" + URLEncoder.encode(s, StandardCharsets.UTF_8.toString()) + "/resources.zip", "");
            } catch (UnsupportedEncodingException unsupportedencodingexception) {
                MinecraftServer.LOGGER.warn("Something went wrong url encoding {}", s);
            }
        }

    }

    public EnumGamemode getDefaultGameType() {
        return this.worldData.getGameType();
    }

    public boolean isHardcore() {
        return this.worldData.isHardcore();
    }

    public abstract int getOperatorUserPermissionLevel();

    public abstract int getFunctionCompilationLevel();

    public abstract boolean shouldRconBroadcast();

    public boolean saveAllChunks(boolean flag, boolean flag1, boolean flag2) {
        boolean flag3 = false;

        for (Iterator iterator = this.getAllLevels().iterator(); iterator.hasNext(); flag3 = true) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (!flag) {
                MinecraftServer.LOGGER.info("Saving chunks for level '{}'/{}", worldserver, worldserver.dimension().location());
            }

            worldserver.save((IProgressUpdate) null, flag1, worldserver.noSave && !flag2);
        }

        // CraftBukkit start - moved to WorldServer.save
        /*
        WorldServer worldserver1 = this.overworld();
        IWorldDataServer iworlddataserver = this.worldData.overworldData();

        iworlddataserver.setWorldBorder(worldserver1.getWorldBorder().createSettings());
        this.worldData.setCustomBossEvents(this.getCustomBossEvents().save());
        this.storageSource.saveDataTag(this.registryHolder, this.worldData, this.getPlayerList().getSingleplayerData());
        */
        // CraftBukkit end
        if (flag1) {
            Iterator iterator1 = this.getAllLevels().iterator();

            while (iterator1.hasNext()) {
                WorldServer worldserver2 = (WorldServer) iterator1.next();

                MinecraftServer.LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", worldserver2.getChunkSource().chunkMap.getStorageName());
            }

            MinecraftServer.LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
        }

        return flag3;
    }

    public boolean saveEverything(boolean flag, boolean flag1, boolean flag2) {
        boolean flag3;

        try {
            this.isSaving = true;
            this.getPlayerList().saveAll();
            flag3 = this.saveAllChunks(flag, flag1, flag2);
        } finally {
            this.isSaving = false;
        }

        return flag3;
    }

    @Override
    public void close() {
        this.stopServer();
    }

    // CraftBukkit start
    private boolean hasStopped = false;
    private final Object stopLock = new Object();
    public final boolean hasStopped() {
        synchronized (stopLock) {
            return hasStopped;
        }
    }
    // CraftBukkit end

    public void stopServer() {
        // CraftBukkit start - prevent double stopping on multiple threads
        synchronized(stopLock) {
            if (hasStopped) return;
            hasStopped = true;
        }
        // CraftBukkit end
        MinecraftServer.LOGGER.info("Stopping server");
        // CraftBukkit start
        if (this.server != null) {
            this.server.disablePlugins();
        }
        // CraftBukkit end
        if (this.getConnection() != null) {
            this.getConnection().stop();
        }

        this.isSaving = true;
        if (this.playerList != null) {
            MinecraftServer.LOGGER.info("Saving players");
            this.playerList.saveAll();
            this.playerList.removeAll();
            try { Thread.sleep(100); } catch (InterruptedException ex) {} // CraftBukkit - SPIGOT-625 - give server at least a chance to send packets
        }

        MinecraftServer.LOGGER.info("Saving worlds");
        Iterator iterator = this.getAllLevels().iterator();

        WorldServer worldserver;

        while (iterator.hasNext()) {
            worldserver = (WorldServer) iterator.next();
            if (worldserver != null) {
                worldserver.noSave = false;
            }
        }

        this.saveAllChunks(false, true, false);
        iterator = this.getAllLevels().iterator();

        while (iterator.hasNext()) {
            worldserver = (WorldServer) iterator.next();
            if (worldserver != null) {
                try {
                    worldserver.close();
                } catch (IOException ioexception) {
                    MinecraftServer.LOGGER.error("Exception closing the level", ioexception);
                }
            }
        }

        this.isSaving = false;
        this.resources.close();

        try {
            this.storageSource.close();
        } catch (IOException ioexception1) {
            MinecraftServer.LOGGER.error("Failed to unlock level {}", this.storageSource.getLevelId(), ioexception1);
        }
        // Spigot start
        if (org.spigotmc.SpigotConfig.saveUserCacheOnStopOnly) {
            LOGGER.info("Saving usercache.json");
            this.getProfileCache().save();
        }
        // Spigot end

    }

    public String getLocalIp() {
        return this.localIp;
    }

    public void setLocalIp(String s) {
        this.localIp = s;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void halt(boolean flag) {
        this.running = false;
        if (flag) {
            try {
                this.serverThread.join();
            } catch (InterruptedException interruptedexception) {
                MinecraftServer.LOGGER.error("Error while shutting down", interruptedexception);
            }
        }

    }

    // Spigot Start
    private static double calcTps(double avg, double exp, double tps)
    {
        return ( avg * exp ) + ( tps * ( 1 - exp ) );
    }
    // Spigot End

    protected void runServer() {
        try {
            if (this.initServer()) {
                this.nextTickTime = SystemUtils.getMillis();
                this.status.setDescription(new ChatComponentText(this.motd));
                this.status.setVersion(new ServerPing.ServerData(SharedConstants.getCurrentVersion().getName(), SharedConstants.getCurrentVersion().getProtocolVersion()));
                this.updateStatusIcon(this.status);

                // Spigot start
                Arrays.fill( recentTps, 20 );
                long curTime, tickSection = SystemUtils.getMillis(), tickCount = 1;
                while (this.running) {
                    long i = (curTime = SystemUtils.getMillis()) - this.nextTickTime;

                    if (i > 5000L && this.nextTickTime - this.lastOverloadWarning >= 30000L) { // CraftBukkit
                        long j = i / 50L;

                        if (server.getWarnOnOverload()) // CraftBukkit
                        MinecraftServer.LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);
                        this.nextTickTime += j * 50L;
                        this.lastOverloadWarning = this.nextTickTime;
                    }

                    if ( tickCount++ % SAMPLE_INTERVAL == 0 )
                    {
                        double currentTps = 1E3 / ( curTime - tickSection ) * SAMPLE_INTERVAL;
                        recentTps[0] = calcTps( recentTps[0], 0.92, currentTps ); // 1/exp(5sec/1min)
                        recentTps[1] = calcTps( recentTps[1], 0.9835, currentTps ); // 1/exp(5sec/5min)
                        recentTps[2] = calcTps( recentTps[2], 0.9945, currentTps ); // 1/exp(5sec/15min)
                        tickSection = curTime;
                    }
                    // Spigot end

                    if (this.debugCommandProfilerDelayStart) {
                        this.debugCommandProfilerDelayStart = false;
                        this.debugCommandProfiler = new MinecraftServer.a(SystemUtils.getNanos(), this.tickCount);
                    }

                    MinecraftServer.currentTick = (int) (System.currentTimeMillis() / 50); // CraftBukkit
                    this.nextTickTime += 50L;
                    this.startMetricsRecordingTick();
                    this.profiler.push("tick");
                    this.tickServer(this::haveTime);
                    this.profiler.popPush("nextTickWait");
                    this.mayHaveDelayedTasks = true;
                    this.delayedTasksMaxNextTickTime = Math.max(SystemUtils.getMillis() + 50L, this.nextTickTime);
                    this.waitUntilNextTick();
                    this.profiler.pop();
                    this.endMetricsRecordingTick();
                    this.isReady = true;
                    JvmProfiler.INSTANCE.onServerTick(this.averageTickTime);
                }
            } else {
                this.onServerCrash((CrashReport) null);
            }
        } catch (Throwable throwable) {
            MinecraftServer.LOGGER.error("Encountered an unexpected exception", throwable);
            // Spigot Start
            if ( throwable.getCause() != null )
            {
                MinecraftServer.LOGGER.error( "\tCause of unexpected exception was", throwable.getCause() );
            }
            // Spigot End
            CrashReport crashreport;

            if (throwable instanceof ReportedException) {
                crashreport = ((ReportedException) throwable).getReport();
            } else {
                crashreport = new CrashReport("Exception in server tick loop", throwable);
            }

            this.fillSystemReport(crashreport.getSystemReport());
            File file = new File(this.getServerDirectory(), "crash-reports");
            SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
            Date date = new Date();
            File file1 = new File(file, "crash-" + simpledateformat.format(date) + "-server.txt");

            if (crashreport.saveToFile(file1)) {
                MinecraftServer.LOGGER.error("This crash report has been saved to: {}", file1.getAbsolutePath());
            } else {
                MinecraftServer.LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.onServerCrash(crashreport);
        } finally {
            try {
                this.stopped = true;
                this.stopServer();
            } catch (Throwable throwable1) {
                MinecraftServer.LOGGER.error("Exception stopping the server", throwable1);
            } finally {
                if (this.profileCache != null) {
                    this.profileCache.clearExecutor();
                }

                org.spigotmc.WatchdogThread.doStop(); // Spigot
                // CraftBukkit start - Restore terminal to original settings
                try {
                    reader.getTerminal().restore();
                } catch (Exception ignored) {
                }
                // CraftBukkit end
                this.onServerExit();
            }

        }

    }

    private boolean haveTime() {
        // CraftBukkit start
        return this.forceTicks || this.runningTask() || SystemUtils.getMillis() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTime : this.nextTickTime);
    }

    private void executeModerately() {
        this.runAllTasks();
        java.util.concurrent.locks.LockSupport.parkNanos("executing tasks", 1000L);
    }
    // CraftBukkit end

    protected void waitUntilNextTick() {
        this.runAllTasks();
        this.managedBlock(() -> {
            return !this.haveTime();
        });
    }

    @Override
    public TickTask wrapRunnable(Runnable runnable) {
        return new TickTask(this.tickCount, runnable);
    }

    protected boolean shouldRun(TickTask ticktask) {
        return ticktask.getTick() + 3 < this.tickCount || this.haveTime();
    }

    @Override
    public boolean pollTask() {
        boolean flag = this.pollTaskInternal();

        this.mayHaveDelayedTasks = flag;
        return flag;
    }

    private boolean pollTaskInternal() {
        if (super.pollTask()) {
            return true;
        } else {
            if (this.haveTime()) {
                Iterator iterator = this.getAllLevels().iterator();

                while (iterator.hasNext()) {
                    WorldServer worldserver = (WorldServer) iterator.next();

                    if (worldserver.getChunkSource().pollTask()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public void doRunTask(TickTask ticktask) { // CraftBukkit - decompile error
        this.getProfiler().incrementCounter("runTask");
        super.doRunTask(ticktask);
    }

    private void updateStatusIcon(ServerPing serverping) {
        Optional<File> optional = Optional.of(this.getFile("server-icon.png")).filter(File::isFile);

        if (!optional.isPresent()) {
            optional = this.storageSource.getIconFile().map(Path::toFile).filter(File::isFile);
        }

        optional.ifPresent((file) -> {
            try {
                BufferedImage bufferedimage = ImageIO.read(file);

                Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
                Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();

                ImageIO.write(bufferedimage, "PNG", bytearrayoutputstream);
                byte[] abyte = Base64.getEncoder().encode(bytearrayoutputstream.toByteArray());
                String s = new String(abyte, StandardCharsets.UTF_8);

                serverping.setFavicon("data:image/png;base64," + s);
            } catch (Exception exception) {
                MinecraftServer.LOGGER.error("Couldn't load server icon", exception);
            }

        });
    }

    public Optional<Path> getWorldScreenshotFile() {
        return this.storageSource.getIconFile();
    }

    public File getServerDirectory() {
        return new File(".");
    }

    protected void onServerCrash(CrashReport crashreport) {}

    public void onServerExit() {}

    public void tickServer(BooleanSupplier booleansupplier) {
        SpigotTimings.serverTickTimer.startTiming(); // Spigot
        this.slackActivityAccountant.tickStarted(); // Spigot
        long i = SystemUtils.getNanos();

        ++this.tickCount;
        this.tickChildren(booleansupplier);
        if (i - this.lastServerStatus >= 5000000000L) {
            this.lastServerStatus = i;
            this.status.setPlayers(new ServerPing.ServerPingPlayerSample(this.getMaxPlayers(), this.getPlayerCount()));
            if (!this.hidesOnlinePlayers()) {
                GameProfile[] agameprofile = new GameProfile[Math.min(this.getPlayerCount(), 12)];
                int j = MathHelper.nextInt(this.random, 0, this.getPlayerCount() - agameprofile.length);

                for (int k = 0; k < agameprofile.length; ++k) {
                    EntityPlayer entityplayer = (EntityPlayer) this.playerList.getPlayers().get(j + k);

                    if (entityplayer.allowsListing()) {
                        agameprofile[k] = entityplayer.getGameProfile();
                    } else {
                        agameprofile[k] = MinecraftServer.ANONYMOUS_PLAYER_PROFILE;
                    }
                }

                Collections.shuffle(Arrays.asList(agameprofile));
                this.status.getPlayers().setSample(agameprofile);
            }
        }

        if (autosavePeriod > 0 && this.tickCount % autosavePeriod == 0) { // CraftBukkit
            SpigotTimings.worldSaveTimer.startTiming(); // Spigot
            MinecraftServer.LOGGER.debug("Autosave started");
            this.profiler.push("save");
            this.saveEverything(true, false, false);
            this.profiler.pop();
            MinecraftServer.LOGGER.debug("Autosave finished");
            SpigotTimings.worldSaveTimer.stopTiming(); // Spigot
        }

        this.profiler.push("tallying");
        long l = this.tickTimes[this.tickCount % 100] = SystemUtils.getNanos() - i;

        this.averageTickTime = this.averageTickTime * 0.8F + (float) l / 1000000.0F * 0.19999999F;
        long i1 = SystemUtils.getNanos();

        this.frameTimer.logFrameDuration(i1 - i);
        this.profiler.pop();
        org.spigotmc.WatchdogThread.tick(); // Spigot
        this.slackActivityAccountant.tickEnded(l); // Spigot
        SpigotTimings.serverTickTimer.stopTiming(); // Spigot
        org.spigotmc.CustomTimingsHandler.tick(); // Spigot
    }

    public void tickChildren(BooleanSupplier booleansupplier) {
        SpigotTimings.schedulerTimer.startTiming(); // Spigot
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount); // CraftBukkit
        SpigotTimings.schedulerTimer.stopTiming(); // Spigot
        this.profiler.push("commandFunctions");
        SpigotTimings.commandFunctionsTimer.startTiming(); // Spigot
        this.getFunctions().tick();
        SpigotTimings.commandFunctionsTimer.stopTiming(); // Spigot
        this.profiler.popPush("levels");
        Iterator iterator = this.getAllLevels().iterator();

        // CraftBukkit start
        // Run tasks that are waiting on processing
        SpigotTimings.processQueueTimer.startTiming(); // Spigot
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }
        SpigotTimings.processQueueTimer.stopTiming(); // Spigot

        SpigotTimings.timeUpdateTimer.startTiming(); // Spigot
        // Send time updates to everyone, it will get the right time from the world the player is in.
        if (this.tickCount % 20 == 0) {
            for (int i = 0; i < this.getPlayerList().players.size(); ++i) {
                EntityPlayer entityplayer = (EntityPlayer) this.getPlayerList().players.get(i);
                entityplayer.connection.send(new PacketPlayOutUpdateTime(entityplayer.level.getGameTime(), entityplayer.getPlayerTime(), entityplayer.level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))); // Add support for per player time
            }
        }
        SpigotTimings.timeUpdateTimer.stopTiming(); // Spigot

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            this.profiler.push(() -> {
                return worldserver + " " + worldserver.dimension().location();
            });
            /* Drop global time updates
            if (this.tickCount % 20 == 0) {
                this.profiler.push("timeSync");
                this.playerList.broadcastAll(new PacketPlayOutUpdateTime(worldserver.getGameTime(), worldserver.getDayTime(), worldserver.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)), worldserver.dimension());
                this.profiler.pop();
            }
            // CraftBukkit end */

            this.profiler.push("tick");

            try {
                worldserver.timings.doTick.startTiming(); // Spigot
                worldserver.tick(booleansupplier);
                worldserver.timings.doTick.stopTiming(); // Spigot
            } catch (Throwable throwable) {
                // Spigot Start
                CrashReport crashreport;
                try {
                    crashreport = CrashReport.forThrowable(throwable, "Exception ticking world");
                } catch (Throwable t) {
                    throw new RuntimeException("Error generating crash report", t);
                }
                // Spigot End

                worldserver.fillReportDetails(crashreport);
                throw new ReportedException(crashreport);
            }

            this.profiler.pop();
            this.profiler.pop();
        }

        this.profiler.popPush("connection");
        SpigotTimings.connectionTimer.startTiming(); // Spigot
        this.getConnection().tick();
        SpigotTimings.connectionTimer.stopTiming(); // Spigot
        this.profiler.popPush("players");
        SpigotTimings.playerListTimer.startTiming(); // Spigot
        this.playerList.tick();
        SpigotTimings.playerListTimer.stopTiming(); // Spigot
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            GameTestHarnessTicker.SINGLETON.tick();
        }

        this.profiler.popPush("server gui refresh");

        SpigotTimings.tickablesTimer.startTiming(); // Spigot
        for (int i = 0; i < this.tickables.size(); ++i) {
            ((Runnable) this.tickables.get(i)).run();
        }
        SpigotTimings.tickablesTimer.stopTiming(); // Spigot

        this.profiler.pop();
    }

    public boolean isNetherEnabled() {
        return true;
    }

    public void addTickable(Runnable runnable) {
        this.tickables.add(runnable);
    }

    protected void setId(String s) {
        this.serverId = s;
    }

    public boolean isShutdown() {
        return !this.serverThread.isAlive();
    }

    public File getFile(String s) {
        return new File(this.getServerDirectory(), s);
    }

    public final WorldServer overworld() {
        return (WorldServer) this.levels.get(World.OVERWORLD);
    }

    @Nullable
    public WorldServer getLevel(ResourceKey<World> resourcekey) {
        return (WorldServer) this.levels.get(resourcekey);
    }

    public Set<ResourceKey<World>> levelKeys() {
        return this.levels.keySet();
    }

    public Iterable<WorldServer> getAllLevels() {
        return this.levels.values();
    }

    public String getServerVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    public int getPlayerCount() {
        return this.playerList.getPlayerCount();
    }

    public int getMaxPlayers() {
        return this.playerList.getMaxPlayers();
    }

    public String[] getPlayerNames() {
        return this.playerList.getPlayerNamesArray();
    }

    @DontObfuscate
    public String getServerModName() {
        return "Spigot"; // Spigot - Spigot > // CraftBukkit - cb > vanilla!
    }

    public SystemReport fillSystemReport(SystemReport systemreport) {
        systemreport.setDetail("Server Running", () -> {
            return Boolean.toString(this.running);
        });
        if (this.playerList != null) {
            systemreport.setDetail("Player Count", () -> {
                int i = this.playerList.getPlayerCount();

                return i + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.getPlayers();
            });
        }

        systemreport.setDetail("Data Packs", () -> {
            StringBuilder stringbuilder = new StringBuilder();
            Iterator iterator = this.packRepository.getSelectedPacks().iterator();

            while (iterator.hasNext()) {
                ResourcePackLoader resourcepackloader = (ResourcePackLoader) iterator.next();

                if (stringbuilder.length() > 0) {
                    stringbuilder.append(", ");
                }

                stringbuilder.append(resourcepackloader.getId());
                if (!resourcepackloader.getCompatibility().isCompatible()) {
                    stringbuilder.append(" (incompatible)");
                }
            }

            return stringbuilder.toString();
        });
        if (this.serverId != null) {
            systemreport.setDetail("Server Id", () -> {
                return this.serverId;
            });
        }

        return this.fillServerSystemReport(systemreport);
    }

    public abstract SystemReport fillServerSystemReport(SystemReport systemreport);

    public ModCheck getModdedStatus() {
        return ModCheck.identify("vanilla", this::getServerModName, "Server", MinecraftServer.class);
    }

    @Override
    public void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid) {
        MinecraftServer.LOGGER.info(ichatbasecomponent.getString());
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int i) {
        this.port = i;
    }

    public String getSingleplayerName() {
        return this.singleplayerName;
    }

    public void setSingleplayerName(String s) {
        this.singleplayerName = s;
    }

    public boolean isSingleplayer() {
        return this.singleplayerName != null;
    }

    protected void initializeKeyPair() {
        MinecraftServer.LOGGER.info("Generating keypair");

        try {
            this.keyPair = MinecraftEncryption.generateKeyPair();
        } catch (CryptographyException cryptographyexception) {
            throw new IllegalStateException("Failed to generate key pair", cryptographyexception);
        }
    }

    public void setDifficulty(EnumDifficulty enumdifficulty, boolean flag) {
        if (flag || !this.worldData.isDifficultyLocked()) {
            this.worldData.setDifficulty(this.worldData.isHardcore() ? EnumDifficulty.HARD : enumdifficulty);
            this.updateMobSpawningFlags();
            this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
        }
    }

    public int getScaledTrackingDistance(int i) {
        return i;
    }

    private void updateMobSpawningFlags() {
        Iterator iterator = this.getAllLevels().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            worldserver.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());
        }

    }

    public void setDifficultyLocked(boolean flag) {
        this.worldData.setDifficultyLocked(flag);
        this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
    }

    private void sendDifficultyUpdate(EntityPlayer entityplayer) {
        WorldData worlddata = entityplayer.getLevel().getLevelData();

        entityplayer.connection.send(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
    }

    public boolean isSpawningMonsters() {
        return this.worldData.getDifficulty() != EnumDifficulty.PEACEFUL;
    }

    public boolean isDemo() {
        return this.isDemo;
    }

    public void setDemo(boolean flag) {
        this.isDemo = flag;
    }

    public String getResourcePack() {
        return this.resourcePack;
    }

    public String getResourcePackHash() {
        return this.resourcePackHash;
    }

    public void setResourcePack(String s, String s1) {
        this.resourcePack = s;
        this.resourcePackHash = s1;
    }

    public abstract boolean isDedicatedServer();

    public abstract int getRateLimitPacketsPerSecond();

    public boolean usesAuthentication() {
        return this.onlineMode;
    }

    public void setUsesAuthentication(boolean flag) {
        this.onlineMode = flag;
    }

    public boolean getPreventProxyConnections() {
        return this.preventProxyConnections;
    }

    public void setPreventProxyConnections(boolean flag) {
        this.preventProxyConnections = flag;
    }

    public boolean isSpawningAnimals() {
        return true;
    }

    public boolean areNpcsEnabled() {
        return true;
    }

    public abstract boolean isEpollEnabled();

    public boolean isPvpAllowed() {
        return this.pvp;
    }

    public void setPvpAllowed(boolean flag) {
        this.pvp = flag;
    }

    public boolean isFlightAllowed() {
        return this.allowFlight;
    }

    public void setFlightAllowed(boolean flag) {
        this.allowFlight = flag;
    }

    public abstract boolean isCommandBlockEnabled();

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String s) {
        this.motd = s;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public PlayerList getPlayerList() {
        return this.playerList;
    }

    public void setPlayerList(PlayerList playerlist) {
        this.playerList = playerlist;
    }

    public abstract boolean isPublished();

    public void setDefaultGameType(EnumGamemode enumgamemode) {
        this.worldData.setGameType(enumgamemode);
    }

    @Nullable
    public ServerConnection getConnection() {
        return this.connection == null ? this.connection = new ServerConnection(this) : this.connection; // Spigot
    }

    public boolean isReady() {
        return this.isReady;
    }

    public boolean hasGui() {
        return false;
    }

    public boolean publishServer(@Nullable EnumGamemode enumgamemode, boolean flag, int i) {
        return false;
    }

    public int getTickCount() {
        return this.tickCount;
    }

    public int getSpawnProtectionRadius() {
        return 16;
    }

    public boolean isUnderSpawnProtection(WorldServer worldserver, BlockPosition blockposition, EntityHuman entityhuman) {
        return false;
    }

    public boolean repliesToStatus() {
        return true;
    }

    public boolean hidesOnlinePlayers() {
        return false;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public int getPlayerIdleTimeout() {
        return this.playerIdleTimeout;
    }

    public void setPlayerIdleTimeout(int i) {
        this.playerIdleTimeout = i;
    }

    public MinecraftSessionService getSessionService() {
        return this.sessionService;
    }

    public GameProfileRepository getProfileRepository() {
        return this.profileRepository;
    }

    public UserCache getProfileCache() {
        return this.profileCache;
    }

    public ServerPing getStatus() {
        return this.status;
    }

    public void invalidateStatus() {
        this.lastServerStatus = 0L;
    }

    public int getAbsoluteMaxWorldSize() {
        return 29999984;
    }

    @Override
    public boolean scheduleExecutables() {
        return super.scheduleExecutables() && !this.isStopped();
    }

    @Override
    public Thread getRunningThread() {
        return this.serverThread;
    }

    public int getCompressionThreshold() {
        return 256;
    }

    public long getNextTickTime() {
        return this.nextTickTime;
    }

    public DataFixer getFixerUpper() {
        return this.fixerUpper;
    }

    public int getSpawnRadius(@Nullable WorldServer worldserver) {
        return worldserver != null ? worldserver.getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS) : 10;
    }

    public AdvancementDataWorld getAdvancements() {
        return this.resources.getAdvancements();
    }

    public CustomFunctionData getFunctions() {
        return this.functionManager;
    }

    public CompletableFuture<Void> reloadResources(Collection<String> collection) {
        CompletableFuture<Void> completablefuture = CompletableFuture.supplyAsync(() -> {
            Stream<String> stream = collection.stream(); // CraftBukkit - decompile error
            ResourcePackRepository resourcepackrepository = this.packRepository;

            Objects.requireNonNull(this.packRepository);
            return stream.map(resourcepackrepository::getPack).filter(Objects::nonNull).map(ResourcePackLoader::open).collect(ImmutableList.toImmutableList()); // CraftBukkit - decompile error
        }, this).thenCompose((immutablelist) -> {
            return DataPackResources.loadResources(immutablelist, this.registryHolder, this.isDedicatedServer() ? CommandDispatcher.ServerType.DEDICATED : CommandDispatcher.ServerType.INTEGRATED, this.getFunctionCompilationLevel(), this.executor, this);
        }).thenAcceptAsync((datapackresources) -> {
            this.resources.close();
            this.resources = datapackresources;
            this.server.syncCommands(); // SPIGOT-5884: Lost on reload
            this.packRepository.setSelected(collection);
            this.worldData.setDataPackConfig(getSelectedPacks(this.packRepository));
            datapackresources.updateGlobals();
            this.getPlayerList().saveAll();
            this.getPlayerList().reloadResources();
            this.functionManager.replaceLibrary(this.resources.getFunctionLibrary());
            this.structureManager.onResourceManagerReload(this.resources.getResourceManager());
        }, this);

        if (this.isSameThread()) {
            Objects.requireNonNull(completablefuture);
            this.managedBlock(completablefuture::isDone);
        }

        return completablefuture;
    }

    public static DataPackConfiguration configurePackRepository(ResourcePackRepository resourcepackrepository, DataPackConfiguration datapackconfiguration, boolean flag) {
        resourcepackrepository.reload();
        if (flag) {
            resourcepackrepository.setSelected(Collections.singleton("vanilla"));
            return new DataPackConfiguration(ImmutableList.of("vanilla"), ImmutableList.of());
        } else {
            Set<String> set = Sets.newLinkedHashSet();
            Iterator iterator = datapackconfiguration.getEnabled().iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();

                if (resourcepackrepository.isAvailable(s)) {
                    set.add(s);
                } else {
                    MinecraftServer.LOGGER.warn("Missing data pack {}", s);
                }
            }

            iterator = resourcepackrepository.getAvailablePacks().iterator();

            while (iterator.hasNext()) {
                ResourcePackLoader resourcepackloader = (ResourcePackLoader) iterator.next();
                String s1 = resourcepackloader.getId();

                if (!datapackconfiguration.getDisabled().contains(s1) && !set.contains(s1)) {
                    MinecraftServer.LOGGER.info("Found new data pack {}, loading it automatically", s1);
                    set.add(s1);
                }
            }

            if (set.isEmpty()) {
                MinecraftServer.LOGGER.info("No datapacks selected, forcing vanilla");
                set.add("vanilla");
            }

            resourcepackrepository.setSelected(set);
            return getSelectedPacks(resourcepackrepository);
        }
    }

    private static DataPackConfiguration getSelectedPacks(ResourcePackRepository resourcepackrepository) {
        Collection<String> collection = resourcepackrepository.getSelectedIds();
        List<String> list = ImmutableList.copyOf(collection);
        List<String> list1 = (List) resourcepackrepository.getAvailableIds().stream().filter((s) -> {
            return !collection.contains(s);
        }).collect(ImmutableList.toImmutableList());

        return new DataPackConfiguration(list, list1);
    }

    public void kickUnlistedPlayers(CommandListenerWrapper commandlistenerwrapper) {
        if (this.isEnforceWhitelist()) {
            PlayerList playerlist = commandlistenerwrapper.getServer().getPlayerList();
            WhiteList whitelist = playerlist.getWhiteList();
            List<EntityPlayer> list = Lists.newArrayList(playerlist.getPlayers());
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                if (!whitelist.isWhiteListed(entityplayer.getGameProfile())) {
                    entityplayer.connection.disconnect(new ChatMessage("multiplayer.disconnect.not_whitelisted"));
                }
            }

        }
    }

    public ResourcePackRepository getPackRepository() {
        return this.packRepository;
    }

    public CommandDispatcher getCommands() {
        return this.resources.getCommands();
    }

    public CommandListenerWrapper createCommandSourceStack() {
        WorldServer worldserver = this.overworld();

        return new CommandListenerWrapper(this, worldserver == null ? Vec3D.ZERO : Vec3D.atLowerCornerOf(worldserver.getSharedSpawnPos()), Vec2F.ZERO, worldserver, 4, "Server", new ChatComponentText("Server"), this, (Entity) null);
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public abstract boolean shouldInformAdmins();

    public CraftingManager getRecipeManager() {
        return this.resources.getRecipeManager();
    }

    public ITagRegistry getTags() {
        return this.resources.getTags();
    }

    public ScoreboardServer getScoreboard() {
        return this.scoreboard;
    }

    public PersistentCommandStorage getCommandStorage() {
        if (this.commandStorage == null) {
            throw new NullPointerException("Called before server init");
        } else {
            return this.commandStorage;
        }
    }

    public LootTableRegistry getLootTables() {
        return this.resources.getLootTables();
    }

    public LootPredicateManager getPredicateManager() {
        return this.resources.getPredicateManager();
    }

    public ItemModifierManager getItemModifierManager() {
        return this.resources.getItemModifierManager();
    }

    public GameRules getGameRules() {
        return this.overworld().getGameRules();
    }

    public BossBattleCustomData getCustomBossEvents() {
        return this.customBossEvents;
    }

    public boolean isEnforceWhitelist() {
        return this.enforceWhitelist;
    }

    public void setEnforceWhitelist(boolean flag) {
        this.enforceWhitelist = flag;
    }

    public float getAverageTickTime() {
        return this.averageTickTime;
    }

    public int getProfilePermissions(GameProfile gameprofile) {
        if (this.getPlayerList().isOp(gameprofile)) {
            OpListEntry oplistentry = (OpListEntry) this.getPlayerList().getOps().get(gameprofile);

            return oplistentry != null ? oplistentry.getLevel() : (this.isSingleplayerOwner(gameprofile) ? 4 : (this.isSingleplayer() ? (this.getPlayerList().isAllowCheatsForAllPlayers() ? 4 : 0) : this.getOperatorUserPermissionLevel()));
        } else {
            return 0;
        }
    }

    public CircularTimer getFrameTimer() {
        return this.frameTimer;
    }

    public GameProfilerFiller getProfiler() {
        return this.profiler;
    }

    public abstract boolean isSingleplayerOwner(GameProfile gameprofile);

    public void dumpServerProperties(Path path) throws IOException {}

    private void saveDebugReport(Path path) {
        Path path1 = path.resolve("levels");

        try {
            Iterator iterator = this.levels.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<ResourceKey<World>, WorldServer> entry = (Entry) iterator.next();
                MinecraftKey minecraftkey = ((ResourceKey) entry.getKey()).location();
                Path path2 = path1.resolve(minecraftkey.getNamespace()).resolve(minecraftkey.getPath());

                Files.createDirectories(path2);
                ((WorldServer) entry.getValue()).saveDebugReport(path2);
            }

            this.dumpGameRules(path.resolve("gamerules.txt"));
            this.dumpClasspath(path.resolve("classpath.txt"));
            this.dumpMiscStats(path.resolve("stats.txt"));
            this.dumpThreads(path.resolve("threads.txt"));
            this.dumpServerProperties(path.resolve("server.properties.txt"));
            this.dumpNativeModules(path.resolve("modules.txt"));
        } catch (IOException ioexception) {
            MinecraftServer.LOGGER.warn("Failed to save debug report", ioexception);
        }

    }

    private void dumpMiscStats(Path path) throws IOException {
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path);

        try {
            bufferedwriter.write(String.format("pending_tasks: %d\n", this.getPendingTasksCount()));
            bufferedwriter.write(String.format("average_tick_time: %f\n", this.getAverageTickTime()));
            bufferedwriter.write(String.format("tick_times: %s\n", Arrays.toString(this.tickTimes)));
            bufferedwriter.write(String.format("queue: %s\n", SystemUtils.backgroundExecutor()));
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

    }

    private void dumpGameRules(Path path) throws IOException {
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path);

        try {
            final List<String> list = Lists.newArrayList();
            final GameRules gamerules = this.getGameRules();

            GameRules.visitGameRuleTypes(new GameRules.GameRuleVisitor() {
                @Override
                public <T extends GameRules.GameRuleValue<T>> void visit(GameRules.GameRuleKey<T> gamerules_gamerulekey, GameRules.GameRuleDefinition<T> gamerules_gameruledefinition) {
                    list.add(String.format("%s=%s\n", gamerules_gamerulekey.getId(), gamerules.getRule(gamerules_gamerulekey)));
                }
            });
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();

                bufferedwriter.write(s);
            }
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

    }

    private void dumpClasspath(Path path) throws IOException {
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path);

        try {
            String s = System.getProperty("java.class.path");
            String s1 = System.getProperty("path.separator");
            Iterator iterator = Splitter.on(s1).split(s).iterator();

            while (iterator.hasNext()) {
                String s2 = (String) iterator.next();

                bufferedwriter.write(s2);
                bufferedwriter.write("\n");
            }
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

    }

    private void dumpThreads(Path path) throws IOException {
        ThreadMXBean threadmxbean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] athreadinfo = threadmxbean.dumpAllThreads(true, true);

        Arrays.sort(athreadinfo, Comparator.comparing(ThreadInfo::getThreadName));
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path);

        try {
            ThreadInfo[] athreadinfo1 = athreadinfo;
            int i = athreadinfo.length;

            for (int j = 0; j < i; ++j) {
                ThreadInfo threadinfo = athreadinfo1[j];

                bufferedwriter.write(threadinfo.toString());
                bufferedwriter.write(10);
            }
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

    }

    private void dumpNativeModules(Path path) throws IOException {
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path);

        label50:
        {
            try {
                label51:
                {
                    ArrayList<NativeModuleLister.a> arraylist; // CraftBukkit - decompile error

                    try {
                        arraylist = Lists.newArrayList(NativeModuleLister.listModules());
                    } catch (Throwable throwable) {
                        MinecraftServer.LOGGER.warn("Failed to list native modules", throwable);
                        break label51;
                    }

                    arraylist.sort(Comparator.comparing((nativemodulelister_a) -> {
                        return nativemodulelister_a.name;
                    }));
                    Iterator iterator = arraylist.iterator();

                    while (true) {
                        if (!iterator.hasNext()) {
                            break label50;
                        }

                        NativeModuleLister.a nativemodulelister_a = (NativeModuleLister.a) iterator.next();

                        bufferedwriter.write(nativemodulelister_a.toString());
                        bufferedwriter.write(10);
                    }
                }
            } catch (Throwable throwable1) {
                if (bufferedwriter != null) {
                    try {
                        bufferedwriter.close();
                    } catch (Throwable throwable2) {
                        throwable1.addSuppressed(throwable2);
                    }
                }

                throw throwable1;
            }

            if (bufferedwriter != null) {
                bufferedwriter.close();
            }

            return;
        }

        if (bufferedwriter != null) {
            bufferedwriter.close();
        }

    }

    // CraftBukkit start
    @Override
    public boolean isSameThread() {
        return super.isSameThread() || this.isStopped(); // CraftBukkit - MC-142590
    }

    public boolean isDebugging() {
        return false;
    }

    @Deprecated
    public static MinecraftServer getServer() {
        return (Bukkit.getServer() instanceof CraftServer) ? ((CraftServer) Bukkit.getServer()).getServer() : null;
    }
    // CraftBukkit end

    private void startMetricsRecordingTick() {
        if (this.willStartRecordingMetrics) {
            this.metricsRecorder = ActiveMetricsRecorder.createStarted(new ServerMetricsSamplersProvider(SystemUtils.timeSource, this.isDedicatedServer()), SystemUtils.timeSource, SystemUtils.ioPool(), new MetricsPersister("server"), this.onMetricsRecordingStopped, (path) -> {
                this.executeBlocking(() -> {
                    this.saveDebugReport(path.resolve("server"));
                });
                this.onMetricsRecordingFinished.accept(path);
            });
            this.willStartRecordingMetrics = false;
        }

        this.profiler = GameProfilerTick.decorateFiller(this.metricsRecorder.getProfiler(), GameProfilerTick.createTickProfiler("Server"));
        this.metricsRecorder.startTick();
        this.profiler.startTick();
    }

    private void endMetricsRecordingTick() {
        this.profiler.endTick();
        this.metricsRecorder.endTick();
    }

    public boolean isRecordingMetrics() {
        return this.metricsRecorder.isRecording();
    }

    public void startRecordingMetrics(Consumer<MethodProfilerResults> consumer, Consumer<Path> consumer1) {
        this.onMetricsRecordingStopped = (methodprofilerresults) -> {
            this.stopRecordingMetrics();
            consumer.accept(methodprofilerresults);
        };
        this.onMetricsRecordingFinished = consumer1;
        this.willStartRecordingMetrics = true;
    }

    public void stopRecordingMetrics() {
        this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
    }

    public void finishRecordingMetrics() {
        this.metricsRecorder.end();
    }

    public Path getWorldPath(SavedFile savedfile) {
        return this.storageSource.getLevelPath(savedfile);
    }

    public boolean forceSynchronousWrites() {
        return true;
    }

    public DefinedStructureManager getStructureManager() {
        return this.structureManager;
    }

    public SaveData getWorldData() {
        return this.worldData;
    }

    public IRegistryCustom registryAccess() {
        return this.registryHolder;
    }

    public ITextFilter createTextFilterForPlayer(EntityPlayer entityplayer) {
        return ITextFilter.DUMMY;
    }

    public boolean isResourcePackRequired() {
        return false;
    }

    public PlayerInteractManager createGameModeForPlayer(EntityPlayer entityplayer) {
        return (PlayerInteractManager) (this.isDemo() ? new DemoPlayerInteractManager(entityplayer) : new PlayerInteractManager(entityplayer));
    }

    @Nullable
    public EnumGamemode getForcedGameType() {
        return null;
    }

    public IResourceManager getResourceManager() {
        return this.resources.getResourceManager();
    }

    @Nullable
    public IChatBaseComponent getResourcePackPrompt() {
        return null;
    }

    public boolean isCurrentlySaving() {
        return this.isSaving;
    }

    public boolean isTimeProfilerRunning() {
        return this.debugCommandProfilerDelayStart || this.debugCommandProfiler != null;
    }

    public void startTimeProfiler() {
        this.debugCommandProfilerDelayStart = true;
    }

    public MethodProfilerResults stopTimeProfiler() {
        if (this.debugCommandProfiler == null) {
            return MethodProfilerResultsEmpty.EMPTY;
        } else {
            MethodProfilerResults methodprofilerresults = this.debugCommandProfiler.stop(SystemUtils.getNanos(), this.tickCount);

            this.debugCommandProfiler = null;
            return methodprofilerresults;
        }
    }

    private static class a {

        final long startNanos;
        final int startTick;

        a(long i, int j) {
            this.startNanos = i;
            this.startTick = j;
        }

        MethodProfilerResults stop(final long i, final int j) {
            return new MethodProfilerResults() {
                @Override
                public List<MethodProfilerResultsField> getTimes(String s) {
                    return Collections.emptyList();
                }

                @Override
                public boolean saveResults(Path path) {
                    return false;
                }

                @Override
                public long getStartTimeNano() {
                    return a.this.startNanos;
                }

                @Override
                public int getStartTimeTicks() {
                    return a.this.startTick;
                }

                @Override
                public long getEndTimeNano() {
                    return i;
                }

                @Override
                public int getEndTimeTicks() {
                    return j;
                }

                @Override
                public String getProfilerResults() {
                    return "";
                }
            };
        }
    }
}
