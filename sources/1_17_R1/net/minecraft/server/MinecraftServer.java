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
import java.awt.GraphicsEnvironment;
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
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.data.worldgen.BiomeDecoratorGroups;
import net.minecraft.gametest.framework.GameTestHarnessTicker;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
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
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.CircularTimer;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MinecraftEncryption;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.profiling.GameProfilerTick;
import net.minecraft.util.profiling.MethodProfilerResults;
import net.minecraft.util.profiling.MethodProfilerResultsEmpty;
import net.minecraft.util.profiling.MethodProfilerResultsField;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.ServerMetricsSamplersProvider;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.IAsyncTaskHandlerReentrant;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.IMojangStatistics;
import net.minecraft.world.MojangStatisticsGenerator;
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
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.block.Block;
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
import com.google.common.collect.ImmutableSet;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.util.datafix.DataConverterRegistry;
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

public abstract class MinecraftServer extends IAsyncTaskHandlerReentrant<TickTask> implements IMojangStatistics, ICommandListener, AutoCloseable {

    public static final Logger LOGGER = LogManager.getLogger();
    private static final float AVERAGE_TICK_TIME_SMOOTHING = 0.8F;
    private static final int TICK_STATS_SPAN = 100;
    public static final int MS_PER_TICK = 50;
    private static final int SNOOPER_UPDATE_INTERVAL = 6000;
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
    public Convertable.ConversionSession storageSource;
    public final WorldNBTStorage playerDataStorage;
    private final MojangStatisticsGenerator snooper = new MojangStatisticsGenerator("server", this, SystemUtils.getMonotonicMillis());
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

    // CraftBukkit start
    public DataPackConfiguration datapackconfiguration;
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

    public static <S extends MinecraftServer> S a(Function<Thread, S> function) {
        AtomicReference<S> atomicreference = new AtomicReference();
        Thread thread = new Thread(() -> {
            ((MinecraftServer) atomicreference.get()).x();
        }, "Server thread");

        thread.setUncaughtExceptionHandler((thread1, throwable) -> {
            MinecraftServer.LOGGER.error(throwable);
        });
        S s0 = function.apply(thread); // CraftBukkit - decompile error

        atomicreference.set(s0);
        thread.start();
        return s0;
    }

    public MinecraftServer(OptionSet options, DataPackConfiguration datapackconfiguration, Thread thread, IRegistryCustom.Dimension iregistrycustom_dimension, Convertable.ConversionSession convertable_conversionsession, SaveData savedata, ResourcePackRepository resourcepackrepository, Proxy proxy, DataFixer datafixer, DataPackResources datapackresources, @Nullable MinecraftSessionService minecraftsessionservice, @Nullable GameProfileRepository gameprofilerepository, @Nullable UserCache usercache, WorldLoadListenerFactory worldloadlistenerfactory) {
        super("Server");
        this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
        this.profiler = this.metricsRecorder.e();
        this.onMetricsRecordingStopped = (methodprofilerresults) -> {
            this.aR();
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
        this.nextTickTime = SystemUtils.getMonotonicMillis();
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
            usercache.a((Executor) this);
        }

        // this.connection = new ServerConnection(this); // Spigot
        this.progressListenerFactory = worldloadlistenerfactory;
        this.storageSource = convertable_conversionsession;
        this.playerDataStorage = convertable_conversionsession.b();
        this.fixerUpper = datafixer;
        this.functionManager = new CustomFunctionData(this, datapackresources.a());
        this.structureManager = new DefinedStructureManager(datapackresources.i(), convertable_conversionsession, datafixer);
        this.serverThread = thread;
        this.executor = SystemUtils.f();
        // CraftBukkit start
        this.options = options;
        this.datapackconfiguration = datapackconfiguration;
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

    private void initializeScoreboards(WorldPersistentData worldpersistentdata) {
        ScoreboardServer scoreboardserver = this.getScoreboard();

        Objects.requireNonNull(scoreboardserver);
        Function<net.minecraft.nbt.NBTTagCompound, net.minecraft.world.scores.PersistentScoreboard> function = scoreboardserver::a; // CraftBukkit - decompile error
        ScoreboardServer scoreboardserver1 = this.getScoreboard();

        Objects.requireNonNull(scoreboardserver1);
        worldpersistentdata.a(function, scoreboardserver1::b, "scoreboard");
    }

    protected abstract boolean init() throws IOException;

    public static void convertWorld(Convertable.ConversionSession convertable_conversionsession) {
        if (convertable_conversionsession.isConvertable()) {
            MinecraftServer.LOGGER.info("Converting map! {}", convertable_conversionsession.getLevelName()); // CraftBukkit
            convertable_conversionsession.convert(new IProgressUpdate() {
                private long timeStamp = SystemUtils.getMonotonicMillis();

                @Override
                public void a(IChatBaseComponent ichatbasecomponent) {}

                @Override
                public void b(IChatBaseComponent ichatbasecomponent) {}

                @Override
                public void a(int i) {
                    if (SystemUtils.getMonotonicMillis() - this.timeStamp >= 1000L) {
                        this.timeStamp = SystemUtils.getMonotonicMillis();
                        MinecraftServer.LOGGER.info("Converting... {}%", i);
                    }

                }

                @Override
                public void a() {}

                @Override
                public void c(IChatBaseComponent ichatbasecomponent) {}
            });
        }

    }

    protected void loadWorld(String s) {
        // CraftBukkit start
        Convertable.ConversionSession worldSession = this.storageSource;
        IRegistryCustom.Dimension iregistrycustom_dimension = this.registryHolder;
        RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.a((DynamicOps) DynamicOpsNBT.INSTANCE, this.resources.i(), (IRegistryCustom) iregistrycustom_dimension);
        WorldDataServer overworldData = (WorldDataServer) worldSession.a((DynamicOps) registryreadops, datapackconfiguration);
        if (overworldData == null) {
            WorldSettings worldsettings;
            GeneratorSettings generatorsettings;

            if (this.isDemoMode()) {
                worldsettings = MinecraftServer.DEMO_SETTINGS;
                generatorsettings = GeneratorSettings.a((IRegistryCustom) iregistrycustom_dimension);
            } else {
                DedicatedServerProperties dedicatedserverproperties = ((DedicatedServer) this).getDedicatedServerProperties();

                worldsettings = new WorldSettings(dedicatedserverproperties.levelName, dedicatedserverproperties.gamemode, dedicatedserverproperties.hardcore, dedicatedserverproperties.difficulty, false, new GameRules(), datapackconfiguration);
                generatorsettings = options.has("bonusChest") ? dedicatedserverproperties.a((IRegistryCustom) iregistrycustom_dimension).j() : dedicatedserverproperties.a((IRegistryCustom) iregistrycustom_dimension);
            }

            overworldData = new WorldDataServer(worldsettings, generatorsettings, Lifecycle.stable());
        }

        GeneratorSettings overworldSettings = overworldData.getGeneratorSettings();
        RegistryMaterials<WorldDimension> registrymaterials = overworldSettings.d();
        for (Entry<ResourceKey<WorldDimension>, WorldDimension> entry : registrymaterials.d()) {
            ResourceKey<WorldDimension> dimensionKey = entry.getKey();

            WorldServer world;
            int dimension = 0;

            if (dimensionKey == WorldDimension.NETHER) {
                if (getAllowNether()) {
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

            String worldType = (dimension == -999) ? dimensionKey.a().getNamespace() + "_" + dimensionKey.a().getKey() : org.bukkit.World.Environment.getEnvironment(dimension).toString().toLowerCase();
            String name = (dimensionKey == WorldDimension.OVERWORLD) ? s : s + "_" + worldType;
            if (dimension != 0) {
                File newWorld = Convertable.getFolder(new File(name), dimensionKey);
                File oldWorld = Convertable.getFolder(new File(s), dimensionKey);
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
                    worldSession = Convertable.a(server.getWorldContainer().toPath()).c(name, dimensionKey);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                MinecraftServer.convertWorld(worldSession); // Run conversion now
            }

            org.bukkit.generator.ChunkGenerator gen = this.server.getGenerator(name);
            org.bukkit.generator.BiomeProvider biomeProvider = this.server.getBiomeProvider(name);

            WorldDataServer worlddata = (WorldDataServer) worldSession.a((DynamicOps) registryreadops, datapackconfiguration);
            if (worlddata == null) {
                WorldSettings worldsettings;
                GeneratorSettings generatorsettings;

                if (this.isDemoMode()) {
                    worldsettings = MinecraftServer.DEMO_SETTINGS;
                    generatorsettings = GeneratorSettings.a((IRegistryCustom) iregistrycustom_dimension);
                } else {
                    DedicatedServerProperties dedicatedserverproperties = ((DedicatedServer) this).getDedicatedServerProperties();

                    worldsettings = new WorldSettings(dedicatedserverproperties.levelName, dedicatedserverproperties.gamemode, dedicatedserverproperties.hardcore, dedicatedserverproperties.difficulty, false, new GameRules(), datapackconfiguration);
                    generatorsettings = options.has("bonusChest") ? dedicatedserverproperties.a((IRegistryCustom) iregistrycustom_dimension).j() : dedicatedserverproperties.a((IRegistryCustom) iregistrycustom_dimension);
                }

                worlddata = new WorldDataServer(worldsettings, generatorsettings, Lifecycle.stable());
            }
            worlddata.checkName(name); // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
            if (options.has("forceUpgrade")) {
                net.minecraft.server.Main.convertWorld(worldSession, DataConverterRegistry.a(), options.has("eraseCache"), () -> {
                    return true;
                }, worlddata.getGeneratorSettings().d().d().stream().map((entry1) -> {
                    return ResourceKey.a(IRegistry.DIMENSION_TYPE_REGISTRY, ((ResourceKey) entry1.getKey()).a());
                }).collect(ImmutableSet.toImmutableSet()));
            }

            IWorldDataServer iworlddataserver = worlddata;
            GeneratorSettings generatorsettings = worlddata.getGeneratorSettings();
            boolean flag = generatorsettings.isDebugWorld();
            long i = generatorsettings.getSeed();
            long j = BiomeManager.a(i);
            List<MobSpawner> list = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(iworlddataserver));
            WorldDimension worlddimension = (WorldDimension) registrymaterials.a(dimensionKey);
            DimensionManager dimensionmanager;
            ChunkGenerator chunkgenerator;

            if (worlddimension == null) {
                dimensionmanager = (DimensionManager) this.registryHolder.d(IRegistry.DIMENSION_TYPE_REGISTRY).d(DimensionManager.OVERWORLD_LOCATION);
                chunkgenerator = GeneratorSettings.a(this.registryHolder.d(IRegistry.BIOME_REGISTRY), this.registryHolder.d(IRegistry.NOISE_GENERATOR_SETTINGS_REGISTRY), (new Random()).nextLong());
            } else {
                dimensionmanager = worlddimension.b();
                chunkgenerator = worlddimension.c();
            }

            org.bukkit.generator.WorldInfo worldInfo = new org.bukkit.craftbukkit.generator.CraftWorldInfo(iworlddataserver, worldSession, org.bukkit.World.Environment.getEnvironment(dimension), dimensionmanager);
            if (biomeProvider == null && gen != null) {
                biomeProvider = gen.getDefaultBiomeProvider(worldInfo);
            }

            if (biomeProvider != null) {
                WorldChunkManager worldChunkManager = new CustomWorldChunkManager(worldInfo, biomeProvider, registryHolder.b(IRegistry.BIOME_REGISTRY));
                if (chunkgenerator instanceof ChunkGeneratorAbstract) {
                    chunkgenerator = new ChunkGeneratorAbstract(worldChunkManager, chunkgenerator.strongholdSeed, ((ChunkGeneratorAbstract) chunkgenerator).settings);
                }
            }

            ResourceKey<World> worldKey = ResourceKey.a(IRegistry.DIMENSION_REGISTRY, dimensionKey.a());

            if (dimensionKey == WorldDimension.OVERWORLD) {
                this.worldData = worlddata;
                this.worldData.setGameType(((DedicatedServer) this).getDedicatedServerProperties().gamemode); // From DedicatedServer.init

                WorldLoadListener worldloadlistener = this.progressListenerFactory.create(11);

                world = new WorldServer(this, this.executor, worldSession, iworlddataserver, worldKey, dimensionmanager, worldloadlistener, chunkgenerator, flag, j, list, true, org.bukkit.World.Environment.getEnvironment(dimension), gen, biomeProvider);
                WorldPersistentData worldpersistentdata = world.getWorldPersistentData();
                this.initializeScoreboards(worldpersistentdata);
                this.server.scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(this, world.getScoreboard());
                this.commandStorage = new PersistentCommandStorage(worldpersistentdata);
            } else {
                WorldLoadListener worldloadlistener = this.progressListenerFactory.create(11);
                world = new WorldServer(this, this.executor, worldSession, iworlddataserver, worldKey, dimensionmanager, worldloadlistener, chunkgenerator, flag, j, ImmutableList.of(), true, org.bukkit.World.Environment.getEnvironment(dimension), gen, biomeProvider);
            }

            worlddata.a(this.getServerModName(), this.getModded().isPresent());
            this.initWorld(world, worlddata, worldData, worlddata.getGeneratorSettings());

            this.levels.put(world.getDimensionKey(), world);
            this.getPlayerList().setPlayerFileData(world);

            if (worlddata.getCustomBossEvents() != null) {
                this.getBossBattleCustomData().load(worlddata.getCustomBossEvents());
            }
        }
        this.updateWorldSettings();
        for (WorldServer worldserver : this.getWorlds()) {
            this.loadSpawn(worldserver.getChunkProvider().chunkMap.progressListener, worldserver);
            worldserver.entityManager.tick(); // SPIGOT-6526: Load pending entities so they are available to the API
            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(worldserver.getWorld()));
        }

        this.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD);
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
        this.connection.acceptConnections();
        // CraftBukkit end
    }

    protected void updateWorldSettings() {}

    // CraftBukkit start
    public void initWorld(WorldServer worldserver, IWorldDataServer iworlddataserver, SaveData saveData, GeneratorSettings generatorsettings) {
        boolean flag = generatorsettings.isDebugWorld();
        // CraftBukkit start
        if (worldserver.generator != null) {
            worldserver.getWorld().getPopulators().addAll(worldserver.generator.getDefaultPopulators(worldserver.getWorld()));
        }
        WorldBorder worldborder = worldserver.getWorldBorder();

        worldborder.a(iworlddataserver.r());
        this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(worldserver.getWorld())); // CraftBukkit - SPIGOT-5569
        if (!iworlddataserver.p()) {
            try {
                a(worldserver, iworlddataserver, generatorsettings.c(), flag);
                iworlddataserver.c(true);
                if (flag) {
                    this.a(this.worldData);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.a(throwable, "Exception initializing level");

                try {
                    worldserver.a(crashreport);
                } catch (Throwable throwable1) {
                    ;
                }

                throw new ReportedException(crashreport);
            }

            iworlddataserver.c(true);
        }
    }
    // CraftBukkit end

    private static void a(WorldServer worldserver, IWorldDataServer iworlddataserver, boolean flag, boolean flag1) {
        if (flag1) {
            iworlddataserver.setSpawn(BlockPosition.ZERO.up(80), 0.0F);
        } else {
            ChunkGenerator chunkgenerator = worldserver.getChunkProvider().getChunkGenerator();
            WorldChunkManager worldchunkmanager = chunkgenerator.getWorldChunkManager();
            Random random = new Random(worldserver.getSeed());
            BlockPosition blockposition = worldchunkmanager.a(0, worldserver.getSeaLevel(), 0, 256, (biomebase) -> {
                return biomebase.b().b();
            }, random);
            ChunkCoordIntPair chunkcoordintpair = blockposition == null ? new ChunkCoordIntPair(0, 0) : new ChunkCoordIntPair(blockposition);
            // CraftBukkit start
            if (worldserver.generator != null) {
                Random rand = new Random(worldserver.getSeed());
                org.bukkit.Location spawn = worldserver.generator.getFixedSpawnLocation(worldserver.getWorld(), rand);

                if (spawn != null) {
                    if (spawn.getWorld() != worldserver.getWorld()) {
                        throw new IllegalStateException("Cannot set spawn point for " + iworlddataserver.getName() + " to be in another world (" + spawn.getWorld().getName() + ")");
                    } else {
                        iworlddataserver.setSpawn(new BlockPosition(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ()), spawn.getYaw());
                        return;
                    }
                }
            }
            // CraftBukkit end

            if (blockposition == null) {
                MinecraftServer.LOGGER.warn("Unable to find spawn biome");
            }

            boolean flag2 = false;
            Iterator iterator = TagsBlock.VALID_SPAWN.getTagged().iterator();

            while (iterator.hasNext()) {
                Block block = (Block) iterator.next();

                if (worldchunkmanager.c().contains(block.getBlockData())) {
                    flag2 = true;
                    break;
                }
            }

            int i = chunkgenerator.getSpawnHeight(worldserver);

            if (i < worldserver.getMinBuildHeight()) {
                BlockPosition blockposition1 = chunkcoordintpair.l();

                i = worldserver.a(HeightMap.Type.WORLD_SURFACE, blockposition1.getX() + 8, blockposition1.getZ() + 8);
            }

            iworlddataserver.setSpawn(chunkcoordintpair.l().c(8, i, 8), 0.0F);
            int j = 0;
            int k = 0;
            int l = 0;
            int i1 = -1;
            boolean flag3 = true;

            for (int j1 = 0; j1 < 1024; ++j1) {
                if (j > -16 && j <= 16 && k > -16 && k <= 16) {
                    BlockPosition blockposition2 = WorldProviderNormal.a(worldserver, new ChunkCoordIntPair(chunkcoordintpair.x + j, chunkcoordintpair.z + k), flag2);

                    if (blockposition2 != null) {
                        iworlddataserver.setSpawn(blockposition2, 0.0F);
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
                WorldGenFeatureConfigured<?, ?> worldgenfeatureconfigured = BiomeDecoratorGroups.BONUS_CHEST;

                worldgenfeatureconfigured.a(worldserver, chunkgenerator, worldserver.random, new BlockPosition(iworlddataserver.a(), iworlddataserver.b(), iworlddataserver.c()));
            }

        }
    }

    private void a(SaveData savedata) {
        savedata.setDifficulty(EnumDifficulty.PEACEFUL);
        savedata.d(true);
        IWorldDataServer iworlddataserver = savedata.H();

        iworlddataserver.setStorm(false);
        iworlddataserver.setThundering(false);
        iworlddataserver.setClearWeatherTime(1000000000);
        iworlddataserver.setDayTime(6000L);
        iworlddataserver.setGameType(EnumGamemode.SPECTATOR);
    }

    // CraftBukkit start
    public void loadSpawn(WorldLoadListener worldloadlistener, WorldServer worldserver) {
        if (!worldserver.getWorld().getKeepSpawnInMemory()) {
            return;
        }

        // WorldServer worldserver = this.E();
        this.forceTicks = true;
        // CraftBukkit end

        MinecraftServer.LOGGER.info("Preparing start region for dimension {}", worldserver.getDimensionKey().a());
        BlockPosition blockposition = worldserver.getSpawn();

        worldloadlistener.a(new ChunkCoordIntPair(blockposition));
        ChunkProviderServer chunkproviderserver = worldserver.getChunkProvider();

        chunkproviderserver.getLightEngine().a(500);
        this.nextTickTime = SystemUtils.getMonotonicMillis();
        chunkproviderserver.addTicket(TicketType.START, new ChunkCoordIntPair(blockposition), 11, Unit.INSTANCE);

        while (chunkproviderserver.b() != 441) {
            // CraftBukkit start
            // this.nextTickTime = SystemUtils.getMonotonicMillis() + 10L;
            this.executeModerately();
            // CraftBukkit end
        }

        // CraftBukkit start
        // this.nextTickTime = SystemUtils.getMonotonicMillis() + 10L;
        this.executeModerately();
        // Iterator iterator = this.worldServer.values().iterator();

        if (true) {
            WorldServer worldserver1 = worldserver;
            // CraftBukkit end
            ForcedChunk forcedchunk = (ForcedChunk) worldserver1.getWorldPersistentData().a(ForcedChunk::b, "chunks");

            if (forcedchunk != null) {
                LongIterator longiterator = forcedchunk.a().iterator();

                while (longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i);

                    worldserver1.getChunkProvider().a(chunkcoordintpair, true);
                }
            }
        }

        // CraftBukkit start
        // this.nextTick = SystemUtils.getMonotonicMillis() + 10L;
        this.executeModerately();
        // CraftBukkit end
        worldloadlistener.b();
        chunkproviderserver.getLightEngine().a(5);
        // CraftBukkit start
        // this.updateSpawnFlags();
        worldserver.setSpawnFlags(this.getSpawnMonsters(), this.getSpawnAnimals());

        this.forceTicks = false;
        // CraftBukkit end
    }

    protected void loadResourcesZip() {
        File file = this.storageSource.getWorldFolder(SavedFile.MAP_RESOURCE_FILE).toFile();

        if (file.isFile()) {
            String s = this.storageSource.getLevelName();

            try {
                this.setResourcePack("level://" + URLEncoder.encode(s, StandardCharsets.UTF_8.toString()) + "/resources.zip", "");
            } catch (UnsupportedEncodingException unsupportedencodingexception) {
                MinecraftServer.LOGGER.warn("Something went wrong url encoding {}", s);
            }
        }

    }

    public EnumGamemode getGamemode() {
        return this.worldData.getGameType();
    }

    public boolean isHardcore() {
        return this.worldData.isHardcore();
    }

    public abstract int h();

    public abstract int i();

    public abstract boolean j();

    public boolean saveChunks(boolean flag, boolean flag1, boolean flag2) {
        boolean flag3 = false;

        for (Iterator iterator = this.getWorlds().iterator(); iterator.hasNext(); flag3 = true) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (!flag) {
                MinecraftServer.LOGGER.info("Saving chunks for level '{}'/{}", worldserver, worldserver.getDimensionKey().a());
            }

            worldserver.save((IProgressUpdate) null, flag1, worldserver.noSave && !flag2);
        }

        // CraftBukkit start - moved to WorldServer.save
        /*
        WorldServer worldserver1 = this.E();
        IWorldDataServer iworlddataserver = this.worldData.H();

        iworlddataserver.a(worldserver1.getWorldBorder().t());
        this.worldData.setCustomBossEvents(this.getBossBattleCustomData().save());
        this.storageSource.a(this.registryHolder, this.worldData, this.getPlayerList().save());
        */
        // CraftBukkit end
        if (flag1) {
            Iterator iterator1 = this.getWorlds().iterator();

            while (iterator1.hasNext()) {
                WorldServer worldserver2 = (WorldServer) iterator1.next();

                MinecraftServer.LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", worldserver2.getChunkProvider().chunkMap.i());
            }

            MinecraftServer.LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
        }

        return flag3;
    }

    @Override
    public void close() {
        this.stop();
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

    public void stop() {
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
        if (this.getServerConnection() != null) {
            this.getServerConnection().b();
        }

        if (this.playerList != null) {
            MinecraftServer.LOGGER.info("Saving players");
            this.playerList.savePlayers();
            this.playerList.shutdown();
            try { Thread.sleep(100); } catch (InterruptedException ex) {} // CraftBukkit - SPIGOT-625 - give server at least a chance to send packets
        }

        MinecraftServer.LOGGER.info("Saving worlds");
        Iterator iterator = this.getWorlds().iterator();

        WorldServer worldserver;

        while (iterator.hasNext()) {
            worldserver = (WorldServer) iterator.next();
            if (worldserver != null) {
                worldserver.noSave = false;
            }
        }

        this.saveChunks(false, true, false);
        iterator = this.getWorlds().iterator();

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

        if (this.snooper.d()) {
            this.snooper.e();
        }

        this.resources.close();

        try {
            this.storageSource.close();
        } catch (IOException ioexception1) {
            MinecraftServer.LOGGER.error("Failed to unlock level {}", this.storageSource.getLevelName(), ioexception1);
        }
        // Spigot start
        if (org.spigotmc.SpigotConfig.saveUserCacheOnStopOnly) {
            LOGGER.info("Saving usercache.json");
            this.getUserCache().b();
        }
        // Spigot end

    }

    public String getServerIp() {
        return this.localIp;
    }

    public void a_(String s) {
        this.localIp = s;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void safeShutdown(boolean flag) {
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

    protected void x() {
        try {
            if (this.init()) {
                this.nextTickTime = SystemUtils.getMonotonicMillis();
                this.status.setMOTD(new ChatComponentText(this.motd));
                this.status.setServerInfo(new ServerPing.ServerData(SharedConstants.getGameVersion().getName(), SharedConstants.getGameVersion().getProtocolVersion()));
                this.a(this.status);

                // Spigot start
                Arrays.fill( recentTps, 20 );
                long curTime, tickSection = SystemUtils.getMonotonicMillis(), tickCount = 1;
                while (this.running) {
                    long i = (curTime = SystemUtils.getMonotonicMillis()) - this.nextTickTime;

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
                        this.debugCommandProfiler = new MinecraftServer.a(SystemUtils.getMonotonicNanos(), this.tickCount);
                    }

                    MinecraftServer.currentTick = (int) (System.currentTimeMillis() / 50); // CraftBukkit
                    this.nextTickTime += 50L;
                    this.bh();
                    this.profiler.enter("tick");
                    this.a(this::canSleepForTick);
                    this.profiler.exitEnter("nextTickWait");
                    this.mayHaveDelayedTasks = true;
                    this.delayedTasksMaxNextTickTime = Math.max(SystemUtils.getMonotonicMillis() + 50L, this.nextTickTime);
                    this.sleepForTick();
                    this.profiler.exit();
                    this.bi();
                    this.isReady = true;
                }
            } else {
                this.a((CrashReport) null);
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
                crashreport = ((ReportedException) throwable).a();
            } else {
                crashreport = new CrashReport("Exception in server tick loop", throwable);
            }

            this.b(crashreport.g());
            File file = new File(this.B(), "crash-reports");
            SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
            Date date = new Date();
            File file1 = new File(file, "crash-" + simpledateformat.format(date) + "-server.txt");

            if (crashreport.a(file1)) {
                MinecraftServer.LOGGER.error("This crash report has been saved to: {}", file1.getAbsolutePath());
            } else {
                MinecraftServer.LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.a(crashreport);
        } finally {
            try {
                this.stopped = true;
                this.stop();
            } catch (Throwable throwable1) {
                MinecraftServer.LOGGER.error("Exception stopping the server", throwable1);
            } finally {
                org.spigotmc.WatchdogThread.doStop(); // Spigot
                // CraftBukkit start - Restore terminal to original settings
                try {
                    reader.getTerminal().restore();
                } catch (Exception ignored) {
                }
                // CraftBukkit end
                this.exit();
            }

        }

    }

    private boolean canSleepForTick() {
        // CraftBukkit start
        return this.forceTicks || this.isEntered() || SystemUtils.getMonotonicMillis() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTime : this.nextTickTime);
    }

    private void executeModerately() {
        this.executeAll();
        java.util.concurrent.locks.LockSupport.parkNanos("executing tasks", 1000L);
    }
    // CraftBukkit end

    protected void sleepForTick() {
        this.executeAll();
        this.awaitTasks(() -> {
            return !this.canSleepForTick();
        });
    }

    @Override
    public TickTask postToMainThread(Runnable runnable) {
        return new TickTask(this.tickCount, runnable);
    }

    protected boolean canExecute(TickTask ticktask) {
        return ticktask.a() + 3 < this.tickCount || this.canSleepForTick();
    }

    @Override
    public boolean executeNext() {
        boolean flag = this.bf();

        this.mayHaveDelayedTasks = flag;
        return flag;
    }

    private boolean bf() {
        if (super.executeNext()) {
            return true;
        } else {
            if (this.canSleepForTick()) {
                Iterator iterator = this.getWorlds().iterator();

                while (iterator.hasNext()) {
                    WorldServer worldserver = (WorldServer) iterator.next();

                    if (worldserver.getChunkProvider().runTasks()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    protected void c(TickTask ticktask) {
        this.getMethodProfiler().c("runTask");
        super.executeTask(ticktask);
    }

    private void a(ServerPing serverping) {
        Optional<File> optional = Optional.of(this.c("server-icon.png")).filter(File::isFile);

        if (!optional.isPresent()) {
            optional = this.storageSource.f().map(Path::toFile).filter(File::isFile);
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

    public Optional<Path> A() {
        return this.storageSource.f();
    }

    public File B() {
        return new File(".");
    }

    protected void a(CrashReport crashreport) {}

    public void exit() {}

    public void a(BooleanSupplier booleansupplier) {
        SpigotTimings.serverTickTimer.startTiming(); // Spigot
        this.slackActivityAccountant.tickStarted(); // Spigot
        long i = SystemUtils.getMonotonicNanos();

        ++this.tickCount;
        this.b(booleansupplier);
        if (i - this.lastServerStatus >= 5000000000L) {
            this.lastServerStatus = i;
            this.status.setPlayerSample(new ServerPing.ServerPingPlayerSample(this.getMaxPlayers(), this.getPlayerCount()));
            GameProfile[] agameprofile = new GameProfile[Math.min(this.getPlayerCount(), 12)];
            int j = MathHelper.nextInt(this.random, 0, this.getPlayerCount() - agameprofile.length);

            for (int k = 0; k < agameprofile.length; ++k) {
                agameprofile[k] = ((EntityPlayer) this.playerList.getPlayers().get(j + k)).getProfile();
            }

            Collections.shuffle(Arrays.asList(agameprofile));
            this.status.b().a(agameprofile);
        }

        if (autosavePeriod > 0 && this.tickCount % autosavePeriod == 0) { // CraftBukkit
            SpigotTimings.worldSaveTimer.startTiming(); // Spigot
            MinecraftServer.LOGGER.debug("Autosave started");
            this.profiler.enter("save");
            this.playerList.savePlayers();
            this.saveChunks(true, false, false);
            this.profiler.exit();
            MinecraftServer.LOGGER.debug("Autosave finished");
            SpigotTimings.worldSaveTimer.stopTiming(); // Spigot
        }

        this.profiler.enter("snooper");
        if (((DedicatedServer) this).getDedicatedServerProperties().snooperEnabled && !this.snooper.d() && this.tickCount > 100) { // Spigot
            this.snooper.a();
        }

        if (((DedicatedServer) this).getDedicatedServerProperties().snooperEnabled && this.tickCount % 6000 == 0) { // Spigot
            this.snooper.b();
        }

        this.profiler.exit();
        this.profiler.enter("tallying");
        long l = this.tickTimes[this.tickCount % 100] = SystemUtils.getMonotonicNanos() - i;

        this.averageTickTime = this.averageTickTime * 0.8F + (float) l / 1000000.0F * 0.19999999F;
        long i1 = SystemUtils.getMonotonicNanos();

        this.frameTimer.a(i1 - i);
        this.profiler.exit();
        org.spigotmc.WatchdogThread.tick(); // Spigot
        this.slackActivityAccountant.tickEnded(l); // Spigot
        SpigotTimings.serverTickTimer.stopTiming(); // Spigot
        org.spigotmc.CustomTimingsHandler.tick(); // Spigot
    }

    public void b(BooleanSupplier booleansupplier) {
        SpigotTimings.schedulerTimer.startTiming(); // Spigot
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount); // CraftBukkit
        SpigotTimings.schedulerTimer.stopTiming(); // Spigot
        this.profiler.enter("commandFunctions");
        SpigotTimings.commandFunctionsTimer.startTiming(); // Spigot
        this.getFunctionData().tick();
        SpigotTimings.commandFunctionsTimer.stopTiming(); // Spigot
        this.profiler.exitEnter("levels");
        Iterator iterator = this.getWorlds().iterator();

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
                entityplayer.connection.sendPacket(new PacketPlayOutUpdateTime(entityplayer.level.getTime(), entityplayer.getPlayerTime(), entityplayer.level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))); // Add support for per player time
            }
        }
        SpigotTimings.timeUpdateTimer.stopTiming(); // Spigot

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            this.profiler.a(() -> {
                return worldserver + " " + worldserver.getDimensionKey().a();
            });
            /* Drop global time updates
            if (this.tickCount % 20 == 0) {
                this.profiler.enter("timeSync");
                this.playerList.a((Packet) (new PacketPlayOutUpdateTime(worldserver.getTime(), worldserver.getDayTime(), worldserver.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))), worldserver.getDimensionKey());
                this.profiler.exit();
            }
            // CraftBukkit end */

            this.profiler.enter("tick");

            try {
                worldserver.timings.doTick.startTiming(); // Spigot
                worldserver.doTick(booleansupplier);
                worldserver.timings.doTick.stopTiming(); // Spigot
            } catch (Throwable throwable) {
                // Spigot Start
                CrashReport crashreport;
                try {
                    crashreport = CrashReport.a(throwable, "Exception ticking world");
                } catch (Throwable t) {
                    throw new RuntimeException("Error generating crash report", t);
                }
                // Spigot End

                worldserver.a(crashreport);
                throw new ReportedException(crashreport);
            }

            this.profiler.exit();
            this.profiler.exit();
        }

        this.profiler.exitEnter("connection");
        SpigotTimings.connectionTimer.startTiming(); // Spigot
        this.getServerConnection().c();
        SpigotTimings.connectionTimer.stopTiming(); // Spigot
        this.profiler.exitEnter("players");
        SpigotTimings.playerListTimer.startTiming(); // Spigot
        this.playerList.tick();
        SpigotTimings.playerListTimer.stopTiming(); // Spigot
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            GameTestHarnessTicker.SINGLETON.b();
        }

        this.profiler.exitEnter("server gui refresh");

        SpigotTimings.tickablesTimer.startTiming(); // Spigot
        for (int i = 0; i < this.tickables.size(); ++i) {
            ((Runnable) this.tickables.get(i)).run();
        }
        SpigotTimings.tickablesTimer.stopTiming(); // Spigot

        this.profiler.exit();
    }

    public boolean getAllowNether() {
        return true;
    }

    public void b(Runnable runnable) {
        this.tickables.add(runnable);
    }

    protected void b(String s) {
        this.serverId = s;
    }

    public boolean D() {
        return !this.serverThread.isAlive();
    }

    public File c(String s) {
        return new File(this.B(), s);
    }

    public final WorldServer E() {
        return (WorldServer) this.levels.get(World.OVERWORLD);
    }

    @Nullable
    public WorldServer getWorldServer(ResourceKey<World> resourcekey) {
        return (WorldServer) this.levels.get(resourcekey);
    }

    public Set<ResourceKey<World>> F() {
        return this.levels.keySet();
    }

    public Iterable<WorldServer> getWorlds() {
        return this.levels.values();
    }

    public String getVersion() {
        return SharedConstants.getGameVersion().getName();
    }

    public int getPlayerCount() {
        return this.playerList.getPlayerCount();
    }

    public int getMaxPlayers() {
        return this.playerList.getMaxPlayers();
    }

    public String[] getPlayers() {
        return this.playerList.e();
    }

    @DontObfuscate
    public String getServerModName() {
        return "Spigot"; // Spigot - Spigot > // CraftBukkit - cb > vanilla!
    }

    public SystemReport b(SystemReport systemreport) {
        if (this.playerList != null) {
            systemreport.a("Player Count", () -> {
                int i = this.playerList.getPlayerCount();

                return i + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.getPlayers();
            });
        }

        systemreport.a("Data Packs", () -> {
            StringBuilder stringbuilder = new StringBuilder();
            Iterator iterator = this.packRepository.e().iterator();

            while (iterator.hasNext()) {
                ResourcePackLoader resourcepackloader = (ResourcePackLoader) iterator.next();

                if (stringbuilder.length() > 0) {
                    stringbuilder.append(", ");
                }

                stringbuilder.append(resourcepackloader.e());
                if (!resourcepackloader.c().a()) {
                    stringbuilder.append(" (incompatible)");
                }
            }

            return stringbuilder.toString();
        });
        if (this.serverId != null) {
            systemreport.a("Server Id", () -> {
                return this.serverId;
            });
        }

        return this.a(systemreport);
    }

    public abstract SystemReport a(SystemReport systemreport);

    public abstract Optional<String> getModded();

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

    public String getSinglePlayerName() {
        return this.singleplayerName;
    }

    public void d(String s) {
        this.singleplayerName = s;
    }

    public boolean isEmbeddedServer() {
        return this.singleplayerName != null;
    }

    protected void P() {
        MinecraftServer.LOGGER.info("Generating keypair");

        try {
            this.keyPair = MinecraftEncryption.b();
        } catch (CryptographyException cryptographyexception) {
            throw new IllegalStateException("Failed to generate key pair", cryptographyexception);
        }
    }

    public void a(EnumDifficulty enumdifficulty, boolean flag) {
        if (flag || !this.worldData.isDifficultyLocked()) {
            this.worldData.setDifficulty(this.worldData.isHardcore() ? EnumDifficulty.HARD : enumdifficulty);
            this.updateSpawnFlags();
            this.getPlayerList().getPlayers().forEach(this::c);
        }
    }

    public int b(int i) {
        return i;
    }

    private void updateSpawnFlags() {
        Iterator iterator = this.getWorlds().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            worldserver.setSpawnFlags(this.getSpawnMonsters(), this.getSpawnAnimals());
        }

    }

    public void b(boolean flag) {
        this.worldData.d(flag);
        this.getPlayerList().getPlayers().forEach(this::c);
    }

    private void c(EntityPlayer entityplayer) {
        WorldData worlddata = entityplayer.getWorldServer().getWorldData();

        entityplayer.connection.sendPacket(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
    }

    public boolean getSpawnMonsters() {
        return this.worldData.getDifficulty() != EnumDifficulty.PEACEFUL;
    }

    public boolean isDemoMode() {
        return this.isDemo;
    }

    public void c(boolean flag) {
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

    @Override
    public void a(MojangStatisticsGenerator mojangstatisticsgenerator) {
        mojangstatisticsgenerator.a("whitelist_enabled", (Object) false);
        mojangstatisticsgenerator.a("whitelist_count", (int) 0);
        if (this.playerList != null) {
            mojangstatisticsgenerator.a("players_current", (Object) this.getPlayerCount());
            mojangstatisticsgenerator.a("players_max", (Object) this.getMaxPlayers());
            mojangstatisticsgenerator.a("players_seen", (Object) this.playerDataStorage.getSeenPlayers().length);
        }

        mojangstatisticsgenerator.a("uses_auth", (Object) this.onlineMode);
        mojangstatisticsgenerator.a("gui_state", (Object) (this.ag() ? "enabled" : "disabled"));
        mojangstatisticsgenerator.a("run_time", (Object) ((SystemUtils.getMonotonicMillis() - mojangstatisticsgenerator.g()) / 60L * 1000L));
        mojangstatisticsgenerator.a("avg_tick_ms", (Object) ((int) (MathHelper.a(this.tickTimes) * 1.0E-6D)));
        int i = 0;
        Iterator iterator = this.getWorlds().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (worldserver != null) {
                mojangstatisticsgenerator.a("world[" + i + "][dimension]", (Object) worldserver.getDimensionKey().a());
                mojangstatisticsgenerator.a("world[" + i + "][mode]", (Object) this.worldData.getGameType());
                mojangstatisticsgenerator.a("world[" + i + "][difficulty]", (Object) worldserver.getDifficulty());
                mojangstatisticsgenerator.a("world[" + i + "][hardcore]", (Object) this.worldData.isHardcore());
                mojangstatisticsgenerator.a("world[" + i + "][height]", (Object) worldserver.getMaxBuildHeight());
                mojangstatisticsgenerator.a("world[" + i + "][chunks_loaded]", (Object) worldserver.getChunkProvider().h());
                ++i;
            }
        }

        mojangstatisticsgenerator.a("worlds", (Object) i);
    }

    @Override
    public void b(MojangStatisticsGenerator mojangstatisticsgenerator) {
        mojangstatisticsgenerator.b("singleplayer", this.isEmbeddedServer());
        mojangstatisticsgenerator.b("server_brand", this.getServerModName());
        mojangstatisticsgenerator.b("gui_supported", GraphicsEnvironment.isHeadless() ? "headless" : "supported");
        mojangstatisticsgenerator.b("dedicated", this.k());
    }

    @Override
    public boolean U() {
        return true;
    }

    public abstract boolean k();

    public abstract int l();

    public boolean getOnlineMode() {
        return this.onlineMode;
    }

    public void setOnlineMode(boolean flag) {
        this.onlineMode = flag;
    }

    public boolean W() {
        return this.preventProxyConnections;
    }

    public void e(boolean flag) {
        this.preventProxyConnections = flag;
    }

    public boolean getSpawnAnimals() {
        return true;
    }

    public boolean getSpawnNPCs() {
        return true;
    }

    public abstract boolean m();

    public boolean getPVP() {
        return this.pvp;
    }

    public void setPVP(boolean flag) {
        this.pvp = flag;
    }

    public boolean getAllowFlight() {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean flag) {
        this.allowFlight = flag;
    }

    public abstract boolean getEnableCommandBlock();

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

    public void a(PlayerList playerlist) {
        this.playerList = playerlist;
    }

    public abstract boolean o();

    public void a(EnumGamemode enumgamemode) {
        this.worldData.setGameType(enumgamemode);
    }

    @Nullable
    public ServerConnection getServerConnection() {
        return this.connection == null ? this.connection = new ServerConnection(this) : this.connection; // Spigot
    }

    public boolean af() {
        return this.isReady;
    }

    public boolean ag() {
        return false;
    }

    public boolean a(@Nullable EnumGamemode enumgamemode, boolean flag, int i) {
        return false;
    }

    public int ah() {
        return this.tickCount;
    }

    public MojangStatisticsGenerator ai() {
        return this.snooper;
    }

    public int getSpawnProtection() {
        return 16;
    }

    public boolean a(WorldServer worldserver, BlockPosition blockposition, EntityHuman entityhuman) {
        return false;
    }

    public boolean ak() {
        return true;
    }

    public Proxy al() {
        return this.proxy;
    }

    public int getIdleTimeout() {
        return this.playerIdleTimeout;
    }

    public void setIdleTimeout(int i) {
        this.playerIdleTimeout = i;
    }

    public MinecraftSessionService getMinecraftSessionService() {
        return this.sessionService;
    }

    public GameProfileRepository getGameProfileRepository() {
        return this.profileRepository;
    }

    public UserCache getUserCache() {
        return this.profileCache;
    }

    public ServerPing getServerPing() {
        return this.status;
    }

    public void invalidatePingSample() {
        this.lastServerStatus = 0L;
    }

    public int as() {
        return 29999984;
    }

    @Override
    public boolean isNotMainThread() {
        return super.isNotMainThread() && !this.isStopped();
    }

    @Override
    public Thread getThread() {
        return this.serverThread;
    }

    public int av() {
        return 256;
    }

    public long aw() {
        return this.nextTickTime;
    }

    public DataFixer getDataFixer() {
        return this.fixerUpper;
    }

    public int a(@Nullable WorldServer worldserver) {
        return worldserver != null ? worldserver.getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS) : 10;
    }

    public AdvancementDataWorld getAdvancementData() {
        return this.resources.h();
    }

    public CustomFunctionData getFunctionData() {
        return this.functionManager;
    }

    public CompletableFuture<Void> a(Collection<String> collection) {
        CompletableFuture<Void> completablefuture = CompletableFuture.supplyAsync(() -> {
            Stream<String> stream = collection.stream(); // CraftBukkit - decompile error
            ResourcePackRepository resourcepackrepository = this.packRepository;

            Objects.requireNonNull(this.packRepository);
            return stream.map(resourcepackrepository::a).filter(Objects::nonNull).map(ResourcePackLoader::d).collect(ImmutableList.toImmutableList()); // CraftBukkit - decompile error
        }, this).thenCompose((immutablelist) -> {
            return DataPackResources.a(immutablelist, this.registryHolder, this.k() ? CommandDispatcher.ServerType.DEDICATED : CommandDispatcher.ServerType.INTEGRATED, this.i(), this.executor, this);
        }).thenAcceptAsync((datapackresources) -> {
            this.resources.close();
            this.resources = datapackresources;
            this.server.syncCommands(); // SPIGOT-5884: Lost on reload
            this.packRepository.a(collection);
            this.worldData.a(a(this.packRepository));
            datapackresources.j();
            this.getPlayerList().savePlayers();
            this.getPlayerList().reload();
            this.functionManager.a(this.resources.a());
            this.structureManager.a(this.resources.i());
        }, this);

        if (this.isMainThread()) {
            Objects.requireNonNull(completablefuture);
            this.awaitTasks(completablefuture::isDone);
        }

        return completablefuture;
    }

    public static DataPackConfiguration a(ResourcePackRepository resourcepackrepository, DataPackConfiguration datapackconfiguration, boolean flag) {
        resourcepackrepository.a();
        if (flag) {
            resourcepackrepository.a((Collection) Collections.singleton("vanilla"));
            return new DataPackConfiguration(ImmutableList.of("vanilla"), ImmutableList.of());
        } else {
            Set<String> set = Sets.newLinkedHashSet();
            Iterator iterator = datapackconfiguration.a().iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();

                if (resourcepackrepository.b(s)) {
                    set.add(s);
                } else {
                    MinecraftServer.LOGGER.warn("Missing data pack {}", s);
                }
            }

            iterator = resourcepackrepository.c().iterator();

            while (iterator.hasNext()) {
                ResourcePackLoader resourcepackloader = (ResourcePackLoader) iterator.next();
                String s1 = resourcepackloader.e();

                if (!datapackconfiguration.b().contains(s1) && !set.contains(s1)) {
                    MinecraftServer.LOGGER.info("Found new data pack {}, loading it automatically", s1);
                    set.add(s1);
                }
            }

            if (set.isEmpty()) {
                MinecraftServer.LOGGER.info("No datapacks selected, forcing vanilla");
                set.add("vanilla");
            }

            resourcepackrepository.a((Collection) set);
            return a(resourcepackrepository);
        }
    }

    private static DataPackConfiguration a(ResourcePackRepository resourcepackrepository) {
        Collection<String> collection = resourcepackrepository.d();
        List<String> list = ImmutableList.copyOf(collection);
        List<String> list1 = (List) resourcepackrepository.b().stream().filter((s) -> {
            return !collection.contains(s);
        }).collect(ImmutableList.toImmutableList());

        return new DataPackConfiguration(list, list1);
    }

    public void a(CommandListenerWrapper commandlistenerwrapper) {
        if (this.isEnforceWhitelist()) {
            PlayerList playerlist = commandlistenerwrapper.getServer().getPlayerList();
            WhiteList whitelist = playerlist.getWhitelist();
            List<EntityPlayer> list = Lists.newArrayList(playerlist.getPlayers());
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                if (!whitelist.isWhitelisted(entityplayer.getProfile())) {
                    entityplayer.connection.disconnect(new ChatMessage("multiplayer.disconnect.not_whitelisted"));
                }
            }

        }
    }

    public ResourcePackRepository getResourcePackRepository() {
        return this.packRepository;
    }

    public CommandDispatcher getCommandDispatcher() {
        return this.resources.g();
    }

    public CommandListenerWrapper getServerCommandListener() {
        WorldServer worldserver = this.E();

        return new CommandListenerWrapper(this, worldserver == null ? Vec3D.ZERO : Vec3D.b((BaseBlockPosition) worldserver.getSpawn()), Vec2F.ZERO, worldserver, 4, "Server", new ChatComponentText("Server"), this, (Entity) null);
    }

    @Override
    public boolean shouldSendSuccess() {
        return true;
    }

    @Override
    public boolean shouldSendFailure() {
        return true;
    }

    @Override
    public abstract boolean shouldBroadcastCommands();

    public CraftingManager getCraftingManager() {
        return this.resources.f();
    }

    public ITagRegistry getTagRegistry() {
        return this.resources.e();
    }

    public ScoreboardServer getScoreboard() {
        return this.scoreboard;
    }

    public PersistentCommandStorage aG() {
        if (this.commandStorage == null) {
            throw new NullPointerException("Called before server init");
        } else {
            return this.commandStorage;
        }
    }

    public LootTableRegistry getLootTableRegistry() {
        return this.resources.c();
    }

    public LootPredicateManager getLootPredicateManager() {
        return this.resources.b();
    }

    public ItemModifierManager aJ() {
        return this.resources.d();
    }

    public GameRules getGameRules() {
        return this.E().getGameRules();
    }

    public BossBattleCustomData getBossBattleCustomData() {
        return this.customBossEvents;
    }

    public boolean isEnforceWhitelist() {
        return this.enforceWhitelist;
    }

    public void setEnforceWhitelist(boolean flag) {
        this.enforceWhitelist = flag;
    }

    public float aN() {
        return this.averageTickTime;
    }

    public int b(GameProfile gameprofile) {
        if (this.getPlayerList().isOp(gameprofile)) {
            OpListEntry oplistentry = (OpListEntry) this.getPlayerList().getOPs().get(gameprofile);

            return oplistentry != null ? oplistentry.a() : (this.a(gameprofile) ? 4 : (this.isEmbeddedServer() ? (this.getPlayerList().u() ? 4 : 0) : this.h()));
        } else {
            return 0;
        }
    }

    public CircularTimer aO() {
        return this.frameTimer;
    }

    public GameProfilerFiller getMethodProfiler() {
        return this.profiler;
    }

    public abstract boolean a(GameProfile gameprofile);

    public void a(Path path) throws IOException {}

    private void b(Path path) {
        Path path1 = path.resolve("levels");

        try {
            Iterator iterator = this.levels.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<ResourceKey<World>, WorldServer> entry = (Entry) iterator.next();
                MinecraftKey minecraftkey = ((ResourceKey) entry.getKey()).a();
                Path path2 = path1.resolve(minecraftkey.getNamespace()).resolve(minecraftkey.getKey());

                Files.createDirectories(path2);
                ((WorldServer) entry.getValue()).a(path2);
            }

            this.d(path.resolve("gamerules.txt"));
            this.e(path.resolve("classpath.txt"));
            this.c(path.resolve("stats.txt"));
            this.f(path.resolve("threads.txt"));
            this.a(path.resolve("server.properties.txt"));
        } catch (IOException ioexception) {
            MinecraftServer.LOGGER.warn("Failed to save debug report", ioexception);
        }

    }

    private void c(Path path) throws IOException {
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path);

        try {
            bufferedwriter.write(String.format("pending_tasks: %d\n", this.bm()));
            bufferedwriter.write(String.format("average_tick_time: %f\n", this.aN()));
            bufferedwriter.write(String.format("tick_times: %s\n", Arrays.toString(this.tickTimes)));
            bufferedwriter.write(String.format("queue: %s\n", SystemUtils.f()));
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

    private void d(Path path) throws IOException {
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path);

        try {
            final List<String> list = Lists.newArrayList();
            final GameRules gamerules = this.getGameRules();

            GameRules.a(new GameRules.GameRuleVisitor() {
                @Override
                public <T extends GameRules.GameRuleValue<T>> void a(GameRules.GameRuleKey<T> gamerules_gamerulekey, GameRules.GameRuleDefinition<T> gamerules_gameruledefinition) {
                    list.add(String.format("%s=%s\n", gamerules_gamerulekey.a(), gamerules.get(gamerules_gamerulekey)));
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

    private void e(Path path) throws IOException {
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

    private void f(Path path) throws IOException {
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

    // CraftBukkit start
    @Override
    public boolean isMainThread() {
        return super.isMainThread() || this.isStopped(); // CraftBukkit - MC-142590
    }

    public boolean isDebugging() {
        return false;
    }

    @Deprecated
    public static MinecraftServer getServer() {
        return (Bukkit.getServer() instanceof CraftServer) ? ((CraftServer) Bukkit.getServer()).getServer() : null;
    }
    // CraftBukkit end

    private void bh() {
        if (this.willStartRecordingMetrics) {
            this.metricsRecorder = ActiveMetricsRecorder.a(new ServerMetricsSamplersProvider(SystemUtils.timeSource, this.k()), SystemUtils.timeSource, SystemUtils.g(), new MetricsPersister("server"), this.onMetricsRecordingStopped, (path) -> {
                this.executeSync(() -> {
                    this.b(path.resolve("server"));
                });
                this.onMetricsRecordingFinished.accept(path);
            });
            this.willStartRecordingMetrics = false;
        }

        this.profiler = GameProfilerTick.a(this.metricsRecorder.e(), GameProfilerTick.a("Server"));
        this.metricsRecorder.b();
        this.profiler.a();
    }

    private void bi() {
        this.profiler.b();
        this.metricsRecorder.c();
    }

    public boolean aQ() {
        return this.metricsRecorder.d();
    }

    public void a(Consumer<MethodProfilerResults> consumer, Consumer<Path> consumer1) {
        this.onMetricsRecordingStopped = (methodprofilerresults) -> {
            this.aR();
            consumer.accept(methodprofilerresults);
        };
        this.onMetricsRecordingFinished = consumer1;
        this.willStartRecordingMetrics = true;
    }

    public void aR() {
        this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
    }

    public void aS() {
        this.metricsRecorder.a();
    }

    public Path a(SavedFile savedfile) {
        return this.storageSource.getWorldFolder(savedfile);
    }

    public boolean isSyncChunkWrites() {
        return true;
    }

    public DefinedStructureManager getDefinedStructureManager() {
        return this.structureManager;
    }

    public SaveData getSaveData() {
        return this.worldData;
    }

    public IRegistryCustom getCustomRegistry() {
        return this.registryHolder;
    }

    public ITextFilter a(EntityPlayer entityplayer) {
        return ITextFilter.DUMMY;
    }

    public boolean aX() {
        return false;
    }

    public PlayerInteractManager b(EntityPlayer entityplayer) {
        return (PlayerInteractManager) (this.isDemoMode() ? new DemoPlayerInteractManager(entityplayer) : new PlayerInteractManager(entityplayer));
    }

    @Nullable
    public EnumGamemode aY() {
        return null;
    }

    public IResourceManager aZ() {
        return this.resources.i();
    }

    @Nullable
    public IChatBaseComponent ba() {
        return null;
    }

    public boolean bb() {
        return this.debugCommandProfilerDelayStart || this.debugCommandProfiler != null;
    }

    public void bc() {
        this.debugCommandProfilerDelayStart = true;
    }

    public MethodProfilerResults bd() {
        if (this.debugCommandProfiler == null) {
            return MethodProfilerResultsEmpty.EMPTY;
        } else {
            MethodProfilerResults methodprofilerresults = this.debugCommandProfiler.a(SystemUtils.getMonotonicNanos(), this.tickCount);

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

        MethodProfilerResults a(final long i, final int j) {
            return new MethodProfilerResults() {
                @Override
                public List<MethodProfilerResultsField> a(String s) {
                    return Collections.emptyList();
                }

                @Override
                public boolean a(Path path) {
                    return false;
                }

                @Override
                public long a() {
                    return a.this.startNanos;
                }

                @Override
                public int b() {
                    return a.this.startTick;
                }

                @Override
                public long c() {
                    return i;
                }

                @Override
                public int d() {
                    return j;
                }

                @Override
                public String e() {
                    return "";
                }
            };
        }
    }
}
