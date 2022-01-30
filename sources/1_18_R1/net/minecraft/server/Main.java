package net.minecraft.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Lifecycle;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.WorldLoadListenerLogger;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ResourcePackRepository;
import net.minecraft.server.packs.repository.ResourcePackSource;
import net.minecraft.server.packs.repository.ResourcePackSourceFolder;
import net.minecraft.server.packs.repository.ResourcePackSourceVanilla;
import net.minecraft.server.players.UserCache;
import net.minecraft.util.MathHelper;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.SaveData;
import net.minecraft.world.level.storage.SavedFile;
import net.minecraft.world.level.storage.WorldDataServer;
import net.minecraft.world.level.storage.WorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.SharedConstants;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.WorldDimension;
// CraftBukkit end

public class Main {

    private static final Logger LOGGER;

    public Main() {}

    @DontObfuscate
    public static void main(final OptionSet optionset) { // CraftBukkit - replaces main(String[] astring)
        SharedConstants.tryDetectVersion();
        /* CraftBukkit start - Replace everything
        OptionParser optionparser = new OptionParser();
        OptionSpec<Void> optionspec = optionparser.accepts("nogui");
        OptionSpec<Void> optionspec1 = optionparser.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
        OptionSpec<Void> optionspec2 = optionparser.accepts("demo");
        OptionSpec<Void> optionspec3 = optionparser.accepts("bonusChest");
        OptionSpec<Void> optionspec4 = optionparser.accepts("forceUpgrade");
        OptionSpec<Void> optionspec5 = optionparser.accepts("eraseCache");
        OptionSpec<Void> optionspec6 = optionparser.accepts("safeMode", "Loads level with vanilla datapack only");
        OptionSpec<Void> optionspec7 = optionparser.accepts("help").forHelp();
        OptionSpec<String> optionspec8 = optionparser.accepts("singleplayer").withRequiredArg();
        OptionSpec<String> optionspec9 = optionparser.accepts("universe").withRequiredArg().defaultsTo(".", new String[0]);
        OptionSpec<String> optionspec10 = optionparser.accepts("world").withRequiredArg();
        OptionSpec<Integer> optionspec11 = optionparser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1, new Integer[0]);
        OptionSpec<String> optionspec12 = optionparser.accepts("serverId").withRequiredArg();
        OptionSpec<Void> optionspec13 = optionparser.accepts("jfrProfile");
        NonOptionArgumentSpec nonoptionargumentspec = optionparser.nonOptions();

        try {
            OptionSet optionset = optionparser.parse(astring);

            if (optionset.has(optionspec7)) {
                optionparser.printHelpOn(System.err);
                return;
            }
            */ // CraftBukkit end

        try {

            CrashReport.preload();
            if (optionset.has("jfrProfile")) { // CraftBukkit
                JvmProfiler.INSTANCE.start(Environment.SERVER);
            }

            DispenserRegistry.bootStrap();
            DispenserRegistry.validate();
            SystemUtils.startTimerHackThread();
            IRegistryCustom.Dimension iregistrycustom_dimension = IRegistryCustom.builtin();
            Path path = Paths.get("server.properties");
            DedicatedServerSettings dedicatedserversettings = new DedicatedServerSettings(optionset); // CraftBukkit - CLI argument support

            dedicatedserversettings.forceSave();
            Path path1 = Paths.get("eula.txt");
            EULA eula = new EULA(path1);

            if (optionset.has("initSettings")) { // CraftBukkit
                Main.LOGGER.info("Initialized '{}' and '{}'", path.toAbsolutePath(), path1.toAbsolutePath());
                return;
            }

            // Spigot Start
            boolean eulaAgreed = Boolean.getBoolean( "com.mojang.eula.agree" );
            if ( eulaAgreed )
            {
                System.err.println( "You have used the Spigot command line EULA agreement flag." );
                System.err.println( "By using this setting you are indicating your agreement to Mojang's EULA (https://account.mojang.com/documents/minecraft_eula)." );
                System.err.println( "If you do not agree to the above EULA please stop your server and remove this flag immediately." );
            }
            // Spigot End
            if (!eula.hasAgreedToEULA() && !eulaAgreed) { // Spigot
                Main.LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
                return;
            }

            File file = (File) optionset.valueOf("universe"); // CraftBukkit
            YggdrasilAuthenticationService yggdrasilauthenticationservice = new YggdrasilAuthenticationService(Proxy.NO_PROXY);
            MinecraftSessionService minecraftsessionservice = yggdrasilauthenticationservice.createMinecraftSessionService();
            GameProfileRepository gameprofilerepository = yggdrasilauthenticationservice.createProfileRepository();
            UserCache usercache = new UserCache(gameprofilerepository, new File(file, MinecraftServer.USERID_CACHE_FILE.getName()));
            // CraftBukkit start
            String s = (String) Optional.ofNullable((String) optionset.valueOf("world")).orElse(dedicatedserversettings.getProperties().levelName);
            Convertable convertable = Convertable.createDefault(file.toPath());
            Convertable.ConversionSession convertable_conversionsession = convertable.createAccess(s, WorldDimension.OVERWORLD);
            WorldInfo worldinfo = convertable_conversionsession.getSummary();

            if (worldinfo != null) {
                if (worldinfo.requiresManualConversion()) {
                    Main.LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
                    return;
                }

                if (!worldinfo.isCompatible()) {
                    Main.LOGGER.info("This world was created by an incompatible version.");
                    return;
                }
            }

            DataPackConfiguration datapackconfiguration = convertable_conversionsession.getDataPacks();
            boolean flag = optionset.has("safeMode");

            if (flag) {
                Main.LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
            }

            ResourcePackRepository resourcepackrepository = new ResourcePackRepository(EnumResourcePackType.SERVER_DATA, new ResourcePackSource[]{new ResourcePackSourceVanilla(), new ResourcePackSourceFolder(convertable_conversionsession.getLevelPath(SavedFile.DATAPACK_DIR).toFile(), PackSource.WORLD)});
            // CraftBukkit start
            File bukkitDataPackFolder = new File(convertable_conversionsession.getLevelPath(SavedFile.DATAPACK_DIR).toFile(), "bukkit");
            if (!bukkitDataPackFolder.exists()) {
                bukkitDataPackFolder.mkdirs();
            }
            File mcMeta = new File(bukkitDataPackFolder, "pack.mcmeta");
            try {
                com.google.common.io.Files.write("{\n"
                        + "    \"pack\": {\n"
                        + "        \"description\": \"Data pack for resources provided by Bukkit plugins\",\n"
                        + "        \"pack_format\": " + SharedConstants.getCurrentVersion().getPackVersion() + "\n"
                        + "    }\n"
                        + "}\n", mcMeta, com.google.common.base.Charsets.UTF_8);
            } catch (java.io.IOException ex) {
                throw new RuntimeException("Could not initialize Bukkit datapack", ex);
            }
            // CraftBukkit end
            DataPackConfiguration datapackconfiguration1 = MinecraftServer.configurePackRepository(resourcepackrepository, datapackconfiguration == null ? DataPackConfiguration.DEFAULT : datapackconfiguration, flag);
            CompletableFuture completablefuture = DataPackResources.loadResources(resourcepackrepository.openAllSelected(), iregistrycustom_dimension, CommandDispatcher.ServerType.DEDICATED, dedicatedserversettings.getProperties().functionPermissionLevel, SystemUtils.backgroundExecutor(), Runnable::run);

            DataPackResources datapackresources;

            try {
                datapackresources = (DataPackResources) completablefuture.get();
            } catch (Exception exception) {
                Main.LOGGER.warn("Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", exception);
                resourcepackrepository.close();
                return;
            }

            datapackresources.updateGlobals();
            /*
            RegistryReadOps<NBTBase> registryreadops = RegistryReadOps.createAndLoad(DynamicOpsNBT.INSTANCE, datapackresources.getResourceManager(), iregistrycustom_dimension);

            dedicatedserversettings.getProperties().getWorldGenSettings(iregistrycustom_dimension);
            Object object = convertable_conversionsession.getDataTag(registryreadops, datapackconfiguration1);

            if (object == null) {
                WorldSettings worldsettings;
                GeneratorSettings generatorsettings;

                if (optionset.has(optionspec2)) {
                    worldsettings = MinecraftServer.DEMO_SETTINGS;
                    generatorsettings = GeneratorSettings.demoSettings(iregistrycustom_dimension);
                } else {
                    DedicatedServerProperties dedicatedserverproperties = dedicatedserversettings.getProperties();

                    worldsettings = new WorldSettings(dedicatedserverproperties.levelName, dedicatedserverproperties.gamemode, dedicatedserverproperties.hardcore, dedicatedserverproperties.difficulty, false, new GameRules(), datapackconfiguration1);
                    generatorsettings = optionset.has(optionspec3) ? dedicatedserverproperties.getWorldGenSettings(iregistrycustom_dimension).withBonusChest() : dedicatedserverproperties.getWorldGenSettings(iregistrycustom_dimension);
                }

                object = new WorldDataServer(worldsettings, generatorsettings, Lifecycle.stable());
            }

            if (optionset.has(optionspec4)) {
                forceUpgrade(convertable_conversionsession, DataConverterRegistry.getDataFixer(), optionset.has(optionspec5), () -> {
                    return true;
                }, ((SaveData) object).worldGenSettings());
            }

            convertable_conversionsession.saveDataTag(iregistrycustom_dimension, (SaveData) object);
            */
            final DedicatedServer dedicatedserver = (DedicatedServer) MinecraftServer.spin((thread) -> {
                DedicatedServer dedicatedserver1 = new DedicatedServer(optionset, datapackconfiguration1, thread, iregistrycustom_dimension, convertable_conversionsession, resourcepackrepository, datapackresources, null, dedicatedserversettings, DataConverterRegistry.getDataFixer(), minecraftsessionservice, gameprofilerepository, usercache, WorldLoadListenerLogger::new);

                /*
                dedicatedserver1.setSingleplayerName((String) optionset.valueOf(optionspec8));
                dedicatedserver1.setPort((Integer) optionset.valueOf(optionspec11));
                dedicatedserver1.setDemo(optionset.has(optionspec2));
                dedicatedserver1.setId((String) optionset.valueOf(optionspec12));
                */
                boolean flag1 = !optionset.has("nogui") && !optionset.nonOptionArguments().contains("nogui");

                if (flag1 && !GraphicsEnvironment.isHeadless()) {
                    dedicatedserver1.showGui();
                }

                if (optionset.has("port")) {
                    int port = (Integer) optionset.valueOf("port");
                    if (port > 0) {
                        dedicatedserver1.setPort(port);
                    }
                }

                return dedicatedserver1;
            });
            /* CraftBukkit start
            Thread thread = new Thread("Server Shutdown Thread") {
                public void run() {
                    dedicatedserver.halt(true);
                }
            };

            thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(Main.LOGGER));
            Runtime.getRuntime().addShutdownHook(thread);
            */ // CraftBukkit end
        } catch (Exception exception1) {
            Main.LOGGER.fatal("Failed to start the minecraft server", exception1);
        }

    }

    public static void forceUpgrade(Convertable.ConversionSession convertable_conversionsession, DataFixer datafixer, boolean flag, BooleanSupplier booleansupplier, GeneratorSettings generatorsettings) {
        Main.LOGGER.info("Forcing world upgrade! {}", convertable_conversionsession.getLevelId()); // CraftBukkit
        WorldUpgrader worldupgrader = new WorldUpgrader(convertable_conversionsession, datafixer, generatorsettings, flag);
        IChatBaseComponent ichatbasecomponent = null;

        while (!worldupgrader.isFinished()) {
            IChatBaseComponent ichatbasecomponent1 = worldupgrader.getStatus();

            if (ichatbasecomponent != ichatbasecomponent1) {
                ichatbasecomponent = ichatbasecomponent1;
                Main.LOGGER.info(worldupgrader.getStatus().getString());
            }

            int i = worldupgrader.getTotalChunks();

            if (i > 0) {
                int j = worldupgrader.getConverted() + worldupgrader.getSkipped();

                Main.LOGGER.info("{}% completed ({} / {} chunks)...", MathHelper.floor((float) j / (float) i * 100.0F), j, i);
            }

            if (!booleansupplier.getAsBoolean()) {
                worldupgrader.cancel();
            } else {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException interruptedexception) {
                    ;
                }
            }
        }

    }

    static {
        SystemUtils.preInitLog4j();
        LOGGER = LogManager.getLogger();
    }
}
