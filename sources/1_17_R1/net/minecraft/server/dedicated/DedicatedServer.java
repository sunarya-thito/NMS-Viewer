package net.minecraft.server.dedicated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.SystemUtils;
import net.minecraft.ThreadNamedUncaughtExceptionHandler;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.DataPackResources;
import net.minecraft.server.IMinecraftServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerCommand;
import net.minecraft.server.gui.ServerGUI;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.level.progress.WorldLoadListenerFactory;
import net.minecraft.server.network.ITextFilter;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.packs.repository.ResourcePackRepository;
import net.minecraft.server.players.NameReferencingFileConverter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserCache;
import net.minecraft.server.rcon.RemoteControlCommandListener;
import net.minecraft.server.rcon.thread.RemoteControlListener;
import net.minecraft.server.rcon.thread.RemoteStatusListener;
import net.minecraft.util.MathHelper;
import net.minecraft.util.monitoring.jmx.MinecraftServerBeans;
import net.minecraft.world.MojangStatisticsGenerator;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.SaveData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.world.level.DataPackConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
// CraftBukkit end

public class DedicatedServer extends MinecraftServer implements IMinecraftServer {

    static final Logger LOGGER = LogManager.getLogger();
    private static final int CONVERSION_RETRY_DELAY_MS = 5000;
    private static final int CONVERSION_RETRIES = 2;
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    private final List<ServerCommand> consoleInput = Collections.synchronizedList(Lists.newArrayList());
    private RemoteStatusListener queryThreadGs4;
    public final RemoteControlCommandListener rconConsoleSource;
    private RemoteControlListener rconThread;
    public DedicatedServerSettings settings;
    @Nullable
    private ServerGUI gui;
    @Nullable
    private final TextFilter textFilterClient;
    @Nullable
    private final IChatBaseComponent resourcePackPrompt;

    // CraftBukkit start - Signature changed
    public DedicatedServer(joptsimple.OptionSet options, DataPackConfiguration datapackconfiguration, Thread thread, IRegistryCustom.Dimension iregistrycustom_dimension, Convertable.ConversionSession convertable_conversionsession, ResourcePackRepository resourcepackrepository, DataPackResources datapackresources, SaveData savedata, DedicatedServerSettings dedicatedserversettings, DataFixer datafixer, MinecraftSessionService minecraftsessionservice, GameProfileRepository gameprofilerepository, UserCache usercache, WorldLoadListenerFactory worldloadlistenerfactory) {
        super(options, datapackconfiguration, thread, iregistrycustom_dimension, convertable_conversionsession, savedata, resourcepackrepository, Proxy.NO_PROXY, datafixer, datapackresources, minecraftsessionservice, gameprofilerepository, usercache, worldloadlistenerfactory);
        // CraftBukkit end
        this.settings = dedicatedserversettings;
        this.rconConsoleSource = new RemoteControlCommandListener(this);
        this.textFilterClient = TextFilter.a(dedicatedserversettings.getProperties().textFilteringConfig);
        this.resourcePackPrompt = a(dedicatedserversettings);
    }

    @Override
    public boolean init() throws IOException {
        Thread thread = new Thread("Server console handler") {
            public void run() {
                // CraftBukkit start
                if (!org.bukkit.craftbukkit.Main.useConsole) {
                    return;
                }
                jline.console.ConsoleReader bufferedreader = reader;

                // MC-33041, SPIGOT-5538: if System.in is not valid due to javaw, then return
                try {
                    System.in.available();
                } catch (IOException ex) {
                    return;
                }
                // CraftBukkit end

                String s;

                try {
                    // CraftBukkit start - JLine disabling compatibility
                    while (!DedicatedServer.this.isStopped() && DedicatedServer.this.isRunning()) {
                        if (org.bukkit.craftbukkit.Main.useJline) {
                            s = bufferedreader.readLine(">", null);
                        } else {
                            s = bufferedreader.readLine();
                        }

                        // SPIGOT-5220: Throttle if EOF (ctrl^d) or stdin is /dev/null
                        if (s == null) {
                            try {
                                Thread.sleep(50L);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                            continue;
                        }
                        if (s.trim().length() > 0) { // Trim to filter lines which are just spaces
                            DedicatedServer.this.issueCommand(s, DedicatedServer.this.getServerCommandListener());
                        }
                        // CraftBukkit end
                    }
                } catch (IOException ioexception) {
                    DedicatedServer.LOGGER.error("Exception handling console input", ioexception);
                }

            }
        };

        // CraftBukkit start - TODO: handle command-line logging arguments
        java.util.logging.Logger global = java.util.logging.Logger.getLogger("");
        global.setUseParentHandlers(false);
        for (java.util.logging.Handler handler : global.getHandlers()) {
            global.removeHandler(handler);
        }
        global.addHandler(new org.bukkit.craftbukkit.util.ForwardLogHandler());

        final org.apache.logging.log4j.core.Logger logger = ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger());
        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
            if (appender instanceof org.apache.logging.log4j.core.appender.ConsoleAppender) {
                logger.removeAppender(appender);
            }
        }

        new org.bukkit.craftbukkit.util.TerminalConsoleWriterThread(System.out, this.reader).start();

        System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(logger).setLevel(Level.WARN).buildPrintStream());
        // CraftBukkit end

        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(DedicatedServer.LOGGER));
        thread.start();
        DedicatedServer.LOGGER.info("Starting minecraft server version {}", SharedConstants.getGameVersion().getName());
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            DedicatedServer.LOGGER.warn("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        DedicatedServer.LOGGER.info("Loading properties");
        DedicatedServerProperties dedicatedserverproperties = this.settings.getProperties();

        if (this.isEmbeddedServer()) {
            this.a_("127.0.0.1");
        } else {
            this.setOnlineMode(dedicatedserverproperties.onlineMode);
            this.e(dedicatedserverproperties.preventProxyConnections);
            this.a_(dedicatedserverproperties.serverIp);
        }
        // Spigot start
        this.a((PlayerList) (new DedicatedPlayerList(this, this.registryHolder, this.playerDataStorage)));
        org.spigotmc.SpigotConfig.init((java.io.File) options.valueOf("spigot-settings"));
        org.spigotmc.SpigotConfig.registerCommands();
        // Spigot end

        this.setPVP(dedicatedserverproperties.pvp);
        this.setAllowFlight(dedicatedserverproperties.allowFlight);
        this.setResourcePack(dedicatedserverproperties.resourcePack, this.be());
        this.setMotd(dedicatedserverproperties.motd);
        super.setIdleTimeout((Integer) dedicatedserverproperties.playerIdleTimeout.get());
        this.setEnforceWhitelist(dedicatedserverproperties.enforceWhitelist);
        // this.worldData.setGameType(dedicatedserverproperties.gamemode); // CraftBukkit - moved to world loading
        DedicatedServer.LOGGER.info("Default game type: {}", dedicatedserverproperties.gamemode);
        InetAddress inetaddress = null;

        if (!this.getServerIp().isEmpty()) {
            inetaddress = InetAddress.getByName(this.getServerIp());
        }

        if (this.getPort() < 0) {
            this.setPort(dedicatedserverproperties.serverPort);
        }

        this.P();
        DedicatedServer.LOGGER.info("Starting Minecraft server on {}:{}", this.getServerIp().isEmpty() ? "*" : this.getServerIp(), this.getPort());

        try {
            this.getServerConnection().a(inetaddress, this.getPort());
        } catch (IOException ioexception) {
            DedicatedServer.LOGGER.warn("**** FAILED TO BIND TO PORT!");
            DedicatedServer.LOGGER.warn("The exception was: {}", ioexception.toString());
            DedicatedServer.LOGGER.warn("Perhaps a server is already running on that port?");
            return false;
        }

        // CraftBukkit start
        // this.a((PlayerList) (new DedicatedPlayerList(this, this.customRegistry, this.worldNBTStorage))); // Spigot - moved up
        server.loadPlugins();
        server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.STARTUP);
        // CraftBukkit end

        if (!this.getOnlineMode()) {
            DedicatedServer.LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            DedicatedServer.LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
            // Spigot start
            if (org.spigotmc.SpigotConfig.bungee) {
                DedicatedServer.LOGGER.warn("Whilst this makes it possible to use BungeeCord, unless access to your server is properly restricted, it also opens up the ability for hackers to connect with any username they choose.");
                DedicatedServer.LOGGER.warn("Please see http://www.spigotmc.org/wiki/firewall-guide/ for further information.");
            } else {
                DedicatedServer.LOGGER.warn("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            }
            // Spigot end
            DedicatedServer.LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
        }

        if (this.convertNames()) {
            this.getUserCache().b();
        }

        if (!NameReferencingFileConverter.e(this)) {
            return false;
        } else {
            // this.a((PlayerList) (new DedicatedPlayerList(this, this.customRegistry, this.worldNBTStorage))); // CraftBukkit - moved up
            long i = SystemUtils.getMonotonicNanos();

            TileEntitySkull.a(this.getUserCache());
            TileEntitySkull.a(this.getMinecraftSessionService());
            TileEntitySkull.a((Executor) this);
            UserCache.a(this.getOnlineMode());
            DedicatedServer.LOGGER.info("Preparing level \"{}\"", this.getWorld());
            this.loadWorld(storageSource.getLevelName()); // CraftBukkit
            long j = SystemUtils.getMonotonicNanos() - i;
            String s = String.format(Locale.ROOT, "%.3fs", (double) j / 1.0E9D);

            DedicatedServer.LOGGER.info("Done ({})! For help, type \"help\"", s);
            if (dedicatedserverproperties.announcePlayerAchievements != null) {
                ((GameRules.GameRuleBoolean) this.getGameRules().get(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)).a(dedicatedserverproperties.announcePlayerAchievements, (MinecraftServer) this);
            }

            if (dedicatedserverproperties.enableQuery) {
                DedicatedServer.LOGGER.info("Starting GS4 status listener");
                this.queryThreadGs4 = RemoteStatusListener.a((IMinecraftServer) this);
            }

            if (dedicatedserverproperties.enableRcon) {
                DedicatedServer.LOGGER.info("Starting remote control listener");
                this.rconThread = RemoteControlListener.a((IMinecraftServer) this);
                this.remoteConsole = new org.bukkit.craftbukkit.command.CraftRemoteConsoleCommandSender(this.rconConsoleSource); // CraftBukkit
            }

            if (false && this.getMaxTickTime() > 0L) {  // Spigot - disable
                Thread thread1 = new Thread(new ThreadWatchdog(this));

                thread1.setUncaughtExceptionHandler(new ThreadNamedUncaughtExceptionHandler(DedicatedServer.LOGGER));
                thread1.setName("Server Watchdog");
                thread1.setDaemon(true);
                thread1.start();
            }

            Items.AIR.a(CreativeModeTab.TAB_SEARCH, NonNullList.a());
            if (dedicatedserverproperties.enableJmxMonitoring) {
                MinecraftServerBeans.a((MinecraftServer) this);
                DedicatedServer.LOGGER.info("JMX monitoring enabled");
            }

            return true;
        }
    }

    @Override
    public boolean getSpawnAnimals() {
        return this.getDedicatedServerProperties().spawnAnimals && super.getSpawnAnimals();
    }

    @Override
    public boolean getSpawnMonsters() {
        return this.settings.getProperties().spawnMonsters && super.getSpawnMonsters();
    }

    @Override
    public boolean getSpawnNPCs() {
        return this.settings.getProperties().spawnNpcs && super.getSpawnNPCs();
    }

    public String be() {
        DedicatedServerProperties dedicatedserverproperties = this.settings.getProperties();
        String s;

        if (!dedicatedserverproperties.resourcePackSha1.isEmpty()) {
            s = dedicatedserverproperties.resourcePackSha1;
            if (!Strings.isNullOrEmpty(dedicatedserverproperties.resourcePackHash)) {
                DedicatedServer.LOGGER.warn("resource-pack-hash is deprecated and found along side resource-pack-sha1. resource-pack-hash will be ignored.");
            }
        } else if (!Strings.isNullOrEmpty(dedicatedserverproperties.resourcePackHash)) {
            DedicatedServer.LOGGER.warn("resource-pack-hash is deprecated. Please use resource-pack-sha1 instead.");
            s = dedicatedserverproperties.resourcePackHash;
        } else {
            s = "";
        }

        if (!s.isEmpty() && !DedicatedServer.SHA1.matcher(s).matches()) {
            DedicatedServer.LOGGER.warn("Invalid sha1 for ressource-pack-sha1");
        }

        if (!dedicatedserverproperties.resourcePack.isEmpty() && s.isEmpty()) {
            DedicatedServer.LOGGER.warn("You specified a resource pack without providing a sha1 hash. Pack will be updated on the client only if you change the name of the pack.");
        }

        return s;
    }

    @Override
    public DedicatedServerProperties getDedicatedServerProperties() {
        return this.settings.getProperties();
    }

    @Override
    public void updateWorldSettings() {
        this.a(this.getDedicatedServerProperties().difficulty, true);
    }

    @Override
    public boolean isHardcore() {
        return this.getDedicatedServerProperties().hardcore;
    }

    @Override
    public SystemReport a(SystemReport systemreport) {
        systemreport.a("Is Modded", () -> {
            return (String) this.getModded().orElse("Unknown (can't tell)");
        });
        systemreport.a("Type", () -> {
            return "Dedicated Server (map_server.txt)";
        });
        return systemreport;
    }

    @Override
    public void a(Path path) throws IOException {
        DedicatedServerProperties dedicatedserverproperties = this.getDedicatedServerProperties();
        BufferedWriter bufferedwriter = Files.newBufferedWriter(path);

        try {
            bufferedwriter.write(String.format("sync-chunk-writes=%s%n", dedicatedserverproperties.syncChunkWrites));
            bufferedwriter.write(String.format("gamemode=%s%n", dedicatedserverproperties.gamemode));
            bufferedwriter.write(String.format("spawn-monsters=%s%n", dedicatedserverproperties.spawnMonsters));
            bufferedwriter.write(String.format("entity-broadcast-range-percentage=%d%n", dedicatedserverproperties.entityBroadcastRangePercentage));
            bufferedwriter.write(String.format("max-world-size=%d%n", dedicatedserverproperties.maxWorldSize));
            bufferedwriter.write(String.format("spawn-npcs=%s%n", dedicatedserverproperties.spawnNpcs));
            bufferedwriter.write(String.format("view-distance=%d%n", dedicatedserverproperties.viewDistance));
            bufferedwriter.write(String.format("spawn-animals=%s%n", dedicatedserverproperties.spawnAnimals));
            bufferedwriter.write(String.format("generate-structures=%s%n", dedicatedserverproperties.a((IRegistryCustom) this.registryHolder).shouldGenerateMapFeatures()));
            bufferedwriter.write(String.format("use-native=%s%n", dedicatedserverproperties.useNativeTransport));
            bufferedwriter.write(String.format("rate-limit=%d%n", dedicatedserverproperties.rateLimitPacketsPerSecond));
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

    @Override
    public Optional<String> getModded() {
        String s = this.getServerModName();

        return !"vanilla".equals(s) ? Optional.of("Definitely; Server brand changed to '" + s + "'") : Optional.empty();
    }

    @Override
    public void exit() {
        if (this.textFilterClient != null) {
            this.textFilterClient.close();
        }

        if (this.gui != null) {
            this.gui.b();
        }

        if (this.rconThread != null) {
            this.rconThread.b();
        }

        if (this.queryThreadGs4 != null) {
            this.queryThreadGs4.b();
        }

        System.exit(0); // CraftBukkit
    }

    @Override
    public void b(BooleanSupplier booleansupplier) {
        super.b(booleansupplier);
        this.handleCommandQueue();
    }

    @Override
    public boolean getAllowNether() {
        return this.getDedicatedServerProperties().allowNether;
    }

    @Override
    public void a(MojangStatisticsGenerator mojangstatisticsgenerator) {
        mojangstatisticsgenerator.a("whitelist_enabled", (Object) this.getPlayerList().getHasWhitelist());
        mojangstatisticsgenerator.a("whitelist_count", (Object) this.getPlayerList().getWhitelisted().length);
        super.a(mojangstatisticsgenerator);
    }

    @Override
    public boolean U() {
        return this.getDedicatedServerProperties().snooperEnabled;
    }

    public void issueCommand(String s, CommandListenerWrapper commandlistenerwrapper) {
        this.consoleInput.add(new ServerCommand(s, commandlistenerwrapper));
    }

    public void handleCommandQueue() {
        SpigotTimings.serverCommandTimer.startTiming(); // Spigot
        while (!this.consoleInput.isEmpty()) {
            ServerCommand servercommand = (ServerCommand) this.consoleInput.remove(0);

            // CraftBukkit start - ServerCommand for preprocessing
            ServerCommandEvent event = new ServerCommandEvent(console, servercommand.msg);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) continue;
            servercommand = new ServerCommand(event.getCommand(), servercommand.source);

            // this.getCommandDispatcher().a(servercommand.source, servercommand.command); // Called in dispatchServerCommand
            server.dispatchServerCommand(console, servercommand);
            // CraftBukkit end
        }

        SpigotTimings.serverCommandTimer.stopTiming(); // Spigot
    }

    @Override
    public boolean k() {
        return true;
    }

    @Override
    public int l() {
        return this.getDedicatedServerProperties().rateLimitPacketsPerSecond;
    }

    @Override
    public boolean m() {
        return this.getDedicatedServerProperties().useNativeTransport;
    }

    @Override
    public DedicatedPlayerList getPlayerList() {
        return (DedicatedPlayerList) super.getPlayerList();
    }

    @Override
    public boolean o() {
        return true;
    }

    @Override
    public String b() {
        return this.getServerIp();
    }

    @Override
    public int d() {
        return this.getPort();
    }

    @Override
    public String q() {
        return this.getMotd();
    }

    public void bh() {
        if (this.gui == null) {
            this.gui = ServerGUI.a(this);
        }

    }

    @Override
    public boolean ag() {
        return this.gui != null;
    }

    @Override
    public boolean getEnableCommandBlock() {
        return this.getDedicatedServerProperties().enableCommandBlock;
    }

    @Override
    public int getSpawnProtection() {
        return this.getDedicatedServerProperties().spawnProtection;
    }

    @Override
    public boolean a(WorldServer worldserver, BlockPosition blockposition, EntityHuman entityhuman) {
        if (worldserver.getDimensionKey() != World.OVERWORLD) {
            return false;
        } else if (this.getPlayerList().getOPs().isEmpty()) {
            return false;
        } else if (this.getPlayerList().isOp(entityhuman.getProfile())) {
            return false;
        } else if (this.getSpawnProtection() <= 0) {
            return false;
        } else {
            BlockPosition blockposition1 = worldserver.getSpawn();
            int i = MathHelper.a(blockposition.getX() - blockposition1.getX());
            int j = MathHelper.a(blockposition.getZ() - blockposition1.getZ());
            int k = Math.max(i, j);

            return k <= this.getSpawnProtection();
        }
    }

    @Override
    public boolean ak() {
        return this.getDedicatedServerProperties().enableStatus;
    }

    @Override
    public int h() {
        return this.getDedicatedServerProperties().opPermissionLevel;
    }

    @Override
    public int i() {
        return this.getDedicatedServerProperties().functionPermissionLevel;
    }

    @Override
    public void setIdleTimeout(int i) {
        super.setIdleTimeout(i);
        this.settings.setProperty((dedicatedserverproperties) -> {
            return (DedicatedServerProperties) dedicatedserverproperties.playerIdleTimeout.set(this.getCustomRegistry(), i);
        });
    }

    @Override
    public boolean j() {
        return this.getDedicatedServerProperties().broadcastRconToOps;
    }

    @Override
    public boolean shouldBroadcastCommands() {
        return this.getDedicatedServerProperties().broadcastConsoleToOps;
    }

    @Override
    public int as() {
        return this.getDedicatedServerProperties().maxWorldSize;
    }

    @Override
    public int av() {
        return this.getDedicatedServerProperties().networkCompressionThreshold;
    }

    protected boolean convertNames() {
        boolean flag = false;

        int i;

        for (i = 0; !flag && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
                this.bs();
            }

            flag = NameReferencingFileConverter.a((MinecraftServer) this);
        }

        boolean flag1 = false;

        for (i = 0; !flag1 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
                this.bs();
            }

            flag1 = NameReferencingFileConverter.b((MinecraftServer) this);
        }

        boolean flag2 = false;

        for (i = 0; !flag2 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
                this.bs();
            }

            flag2 = NameReferencingFileConverter.c(this);
        }

        boolean flag3 = false;

        for (i = 0; !flag3 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
                this.bs();
            }

            flag3 = NameReferencingFileConverter.d(this);
        }

        boolean flag4 = false;

        for (i = 0; !flag4 && i <= 2; ++i) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
                this.bs();
            }

            flag4 = NameReferencingFileConverter.a(this);
        }

        return flag || flag1 || flag2 || flag3 || flag4;
    }

    private void bs() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException interruptedexception) {
            ;
        }
    }

    public long getMaxTickTime() {
        return this.getDedicatedServerProperties().maxTickTime;
    }

    @Override
    public String getPlugins() {
        // CraftBukkit start - Whole method
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = server.getPluginManager().getPlugins();

        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0 && server.getQueryPlugins()) {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++) {
                if (i > 0) {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
        // CraftBukkit end
    }

    @Override
    public String executeRemoteCommand(String s) {
        this.rconConsoleSource.clearMessages();
        this.executeSync(() -> {
            // CraftBukkit start - fire RemoteServerCommandEvent
            RemoteServerCommandEvent event = new RemoteServerCommandEvent(remoteConsole, s);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            ServerCommand serverCommand = new ServerCommand(event.getCommand(), rconConsoleSource.getWrapper());
            server.dispatchServerCommand(remoteConsole, serverCommand);
            // CraftBukkit end
        });
        return this.rconConsoleSource.getMessages();
    }

    public void setHasWhitelist(boolean flag) {
        this.settings.setProperty((dedicatedserverproperties) -> {
            return (DedicatedServerProperties) dedicatedserverproperties.whiteList.set(this.getCustomRegistry(), flag);
        });
    }

    @Override
    public void stop() {
        super.stop();
        SystemUtils.h();
    }

    @Override
    public boolean a(GameProfile gameprofile) {
        return false;
    }

    @Override
    public int b(int i) {
        return this.getDedicatedServerProperties().entityBroadcastRangePercentage * i / 100;
    }

    @Override
    public String getWorld() {
        return this.storageSource.getLevelName();
    }

    @Override
    public boolean isSyncChunkWrites() {
        return this.settings.getProperties().syncChunkWrites;
    }

    @Override
    public ITextFilter a(EntityPlayer entityplayer) {
        return this.textFilterClient != null ? this.textFilterClient.a(entityplayer.getProfile()) : ITextFilter.DUMMY;
    }

    @Override
    public boolean aX() {
        return this.settings.getProperties().requireResourcePack;
    }

    @Nullable
    @Override
    public EnumGamemode aY() {
        return this.settings.getProperties().forceGameMode ? this.worldData.getGameType() : null;
    }

    @Nullable
    private static IChatBaseComponent a(DedicatedServerSettings dedicatedserversettings) {
        String s = dedicatedserversettings.getProperties().resourcePackPrompt;

        if (!Strings.isNullOrEmpty(s)) {
            try {
                return IChatBaseComponent.ChatSerializer.a(s);
            } catch (Exception exception) {
                DedicatedServer.LOGGER.warn("Failed to parse resource pack prompt '{}'", s, exception);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public IChatBaseComponent ba() {
        return this.resourcePackPrompt;
    }

    // CraftBukkit start
    public boolean isDebugging() {
        return this.getDedicatedServerProperties().debug;
    }

    @Override
    public CommandSender getBukkitSender(CommandListenerWrapper wrapper) {
        return console;
    }
    // CraftBukkit end
}
