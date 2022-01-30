package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.FileUtils;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.PacketPlayOutAbilities;
import net.minecraft.network.protocol.game.PacketPlayOutCustomPayload;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEffect;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutExperience;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.network.protocol.game.PacketPlayOutRecipeUpdate;
import net.minecraft.network.protocol.game.PacketPlayOutRespawn;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.network.protocol.game.PacketPlayOutServerDifficulty;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnPosition;
import net.minecraft.network.protocol.game.PacketPlayOutTags;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateTime;
import net.minecraft.network.protocol.game.PacketPlayOutViewDistance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.ServerStatisticManager;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.IWorldBorderListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.storage.SavedFile;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.WorldNBTStorage;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import com.google.common.base.Predicate;
import java.util.stream.Collectors;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.LoginListener;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
// CraftBukkit end

public abstract class PlayerList {

    public static final File USERBANLIST_FILE = new File("banned-players.json");
    public static final File IPBANLIST_FILE = new File("banned-ips.json");
    public static final File OPLIST_FILE = new File("ops.json");
    public static final File WHITELIST_FILE = new File("whitelist.json");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SEND_PLAYER_INFO_INTERVAL = 600;
    private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    private final MinecraftServer server;
    public final List<EntityPlayer> players = new java.util.concurrent.CopyOnWriteArrayList(); // CraftBukkit - ArrayList -> CopyOnWriteArrayList: Iterator safety
    private final Map<UUID, EntityPlayer> playersByUUID = Maps.newHashMap();
    private final GameProfileBanList bans;
    private final IpBanList ipBans;
    private final OpList ops;
    private final WhiteList whitelist;
    // CraftBukkit start
    // private final Map<UUID, ServerStatisticManager> stats;
    // private final Map<UUID, AdvancementDataPlayer> advancements;
    // CraftBukkit end
    public final WorldNBTStorage playerIo;
    private boolean doWhiteList;
    private final IRegistryCustom.Dimension registryHolder;
    protected final int maxPlayers;
    private int viewDistance;
    private int simulationDistance;
    private boolean allowCheatsForAllPlayers;
    private static final boolean ALLOW_LOGOUTIVATOR = false;
    private int sendAllPlayerInfoIn;

    // CraftBukkit start
    private CraftServer cserver;
    private final Map<String,EntityPlayer> playersByName = new java.util.HashMap<>();

    public PlayerList(MinecraftServer minecraftserver, IRegistryCustom.Dimension iregistrycustom_dimension, WorldNBTStorage worldnbtstorage, int i) {
        this.cserver = minecraftserver.server = new CraftServer((DedicatedServer) minecraftserver, this);
        minecraftserver.console = org.bukkit.craftbukkit.command.ColouredConsoleSender.getInstance();
        minecraftserver.reader.addCompleter(new org.bukkit.craftbukkit.command.ConsoleCommandCompleter(minecraftserver.server));
        // CraftBukkit end

        this.bans = new GameProfileBanList(PlayerList.USERBANLIST_FILE);
        this.ipBans = new IpBanList(PlayerList.IPBANLIST_FILE);
        this.ops = new OpList(PlayerList.OPLIST_FILE);
        this.whitelist = new WhiteList(PlayerList.WHITELIST_FILE);
        // CraftBukkit start
        // this.stats = Maps.newHashMap();
        // this.advancements = Maps.newHashMap();
        // CraftBukkit end
        this.server = minecraftserver;
        this.registryHolder = iregistrycustom_dimension;
        this.maxPlayers = i;
        this.playerIo = worldnbtstorage;
    }

    public void placeNewPlayer(NetworkManager networkmanager, EntityPlayer entityplayer) {
        GameProfile gameprofile = entityplayer.getGameProfile();
        UserCache usercache = this.server.getProfileCache();
        Optional<GameProfile> optional = usercache.get(gameprofile.getId());
        String s = (String) optional.map(GameProfile::getName).orElse(gameprofile.getName());

        usercache.add(gameprofile);
        NBTTagCompound nbttagcompound = this.load(entityplayer);
        ResourceKey resourcekey;
        // CraftBukkit start - Better rename detection
        if (nbttagcompound != null && nbttagcompound.contains("bukkit")) {
            NBTTagCompound bukkit = nbttagcompound.getCompound("bukkit");
            s = bukkit.contains("lastKnownName", 8) ? bukkit.getString("lastKnownName") : s;
        }
        // CraftBukkit end

        if (nbttagcompound != null) {
            DataResult dataresult = DimensionManager.parseLegacy(new Dynamic(DynamicOpsNBT.INSTANCE, nbttagcompound.get("Dimension")));
            Logger logger = PlayerList.LOGGER;

            Objects.requireNonNull(logger);
            resourcekey = (ResourceKey) dataresult.resultOrPartial(logger::error).orElse(World.OVERWORLD);
        } else {
            resourcekey = World.OVERWORLD;
        }

        ResourceKey<World> resourcekey1 = resourcekey;
        WorldServer worldserver = this.server.getLevel(resourcekey1);
        WorldServer worldserver1;

        if (worldserver == null) {
            PlayerList.LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", resourcekey1);
            worldserver1 = this.server.overworld();
        } else {
            worldserver1 = worldserver;
        }

        entityplayer.setLevel(worldserver1);
        String s1 = "local";

        if (networkmanager.getRemoteAddress() != null) {
            s1 = networkmanager.getRemoteAddress().toString();
        }

        // Spigot start - spawn location event
        Player spawnPlayer = entityplayer.getBukkitEntity();
        org.spigotmc.event.player.PlayerSpawnLocationEvent ev = new org.spigotmc.event.player.PlayerSpawnLocationEvent(spawnPlayer, spawnPlayer.getLocation());
        cserver.getPluginManager().callEvent(ev);

        Location loc = ev.getSpawnLocation();
        worldserver1 = ((CraftWorld) loc.getWorld()).getHandle();

        entityplayer.spawnIn(worldserver1);
        entityplayer.gameMode.setLevel((WorldServer) entityplayer.level);
        entityplayer.absMoveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        // Spigot end

        // CraftBukkit - Moved message to after join
        // PlayerList.LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", entityplayer.getName().getString(), s1, entityplayer.getId(), entityplayer.getX(), entityplayer.getY(), entityplayer.getZ());
        WorldData worlddata = worldserver1.getLevelData();

        entityplayer.loadGameTypes(nbttagcompound);
        PlayerConnection playerconnection = new PlayerConnection(this.server, networkmanager, entityplayer);
        GameRules gamerules = worldserver1.getGameRules();
        boolean flag = gamerules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
        boolean flag1 = gamerules.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);

        // Spigot - view distance
        playerconnection.send(new PacketPlayOutLogin(entityplayer.getId(), worlddata.isHardcore(), entityplayer.gameMode.getGameModeForPlayer(), entityplayer.gameMode.getPreviousGameModeForPlayer(), this.server.levelKeys(), this.registryHolder, worldserver1.dimensionType(), worldserver1.dimension(), BiomeManager.obfuscateSeed(worldserver1.getSeed()), this.getMaxPlayers(), worldserver1.spigotConfig.viewDistance, worldserver1.spigotConfig.simulationDistance, flag1, !flag, worldserver1.isDebug(), worldserver1.isFlat()));
        entityplayer.getBukkitEntity().sendSupportedChannels(); // CraftBukkit
        playerconnection.send(new PacketPlayOutCustomPayload(PacketPlayOutCustomPayload.BRAND, (new PacketDataSerializer(Unpooled.buffer())).writeUtf(this.getServer().getServerModName())));
        playerconnection.send(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
        playerconnection.send(new PacketPlayOutAbilities(entityplayer.getAbilities()));
        playerconnection.send(new PacketPlayOutHeldItemSlot(entityplayer.getInventory().selected));
        playerconnection.send(new PacketPlayOutRecipeUpdate(this.server.getRecipeManager().getRecipes()));
        playerconnection.send(new PacketPlayOutTags(this.server.getTags().serializeToNetwork(this.registryHolder)));
        this.sendPlayerPermissionLevel(entityplayer);
        entityplayer.getStats().markAllDirty();
        entityplayer.getRecipeBook().sendInitialRecipeBook(entityplayer);
        this.updateEntireScoreboard(worldserver1.getScoreboard(), entityplayer);
        this.server.invalidateStatus();
        ChatMessage chatmessage;

        if (entityplayer.getGameProfile().getName().equalsIgnoreCase(s)) {
            chatmessage = new ChatMessage("multiplayer.player.joined", new Object[]{entityplayer.getDisplayName()});
        } else {
            chatmessage = new ChatMessage("multiplayer.player.joined.renamed", new Object[]{entityplayer.getDisplayName(), s});
        }
        // CraftBukkit start
        chatmessage.withStyle(EnumChatFormat.YELLOW);
        String joinMessage = CraftChatMessage.fromComponent(chatmessage);

        playerconnection.teleport(entityplayer.getX(), entityplayer.getY(), entityplayer.getZ(), entityplayer.getYRot(), entityplayer.getXRot());
        this.players.add(entityplayer);
        this.playersByName.put(entityplayer.getScoreboardName().toLowerCase(java.util.Locale.ROOT), entityplayer); // Spigot
        this.playersByUUID.put(entityplayer.getUUID(), entityplayer);
        // this.broadcastAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, new EntityPlayer[]{entityplayer})); // CraftBukkit - replaced with loop below

        // CraftBukkit start
        CraftPlayer bukkitPlayer = entityplayer.getBukkitEntity();

        // Ensure that player inventory is populated with its viewer
        entityplayer.containerMenu.transferTo(entityplayer.containerMenu, bukkitPlayer);

        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(bukkitPlayer, joinMessage);
        cserver.getPluginManager().callEvent(playerJoinEvent);

        if (!entityplayer.connection.connection.isConnected()) {
            return;
        }

        joinMessage = playerJoinEvent.getJoinMessage();

        if (joinMessage != null && joinMessage.length() > 0) {
            for (IChatBaseComponent line : org.bukkit.craftbukkit.util.CraftChatMessage.fromString(joinMessage)) {
                server.getPlayerList().broadcastAll(new PacketPlayOutChat(line, ChatMessageType.SYSTEM, SystemUtils.NIL_UUID));
            }
        }
        // CraftBukkit end

        // CraftBukkit start - sendAll above replaced with this loop
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityplayer);

        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer1 = (EntityPlayer) this.players.get(i);

            if (entityplayer1.getBukkitEntity().canSee(bukkitPlayer)) {
                entityplayer1.connection.send(packet);
            }

            if (!bukkitPlayer.canSee(entityplayer1.getBukkitEntity())) {
                continue;
            }

            entityplayer.connection.send(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, new EntityPlayer[]{entityplayer1}));
        }
        entityplayer.sentListPacket = true;
        // CraftBukkit end

        entityplayer.connection.send(new PacketPlayOutEntityMetadata(entityplayer.getId(), entityplayer.getEntityData(), true)); // CraftBukkit - BungeeCord#2321, send complete data to self on spawn

        // CraftBukkit start - Only add if the player wasn't moved in the event
        if (entityplayer.level == worldserver1 && !worldserver1.players().contains(entityplayer)) {
            worldserver1.addNewPlayer(entityplayer);
            this.server.getCustomBossEvents().onPlayerConnect(entityplayer);
        }

        worldserver1 = entityplayer.getLevel(); // CraftBukkit - Update in case join event changed it
        // CraftBukkit end
        this.sendLevelInfo(entityplayer, worldserver1);
        if (!this.server.getResourcePack().isEmpty()) {
            entityplayer.sendTexturePack(this.server.getResourcePack(), this.server.getResourcePackHash(), this.server.isResourcePackRequired(), this.server.getResourcePackPrompt());
        }

        Iterator iterator = entityplayer.getActiveEffects().iterator();

        while (iterator.hasNext()) {
            MobEffect mobeffect = (MobEffect) iterator.next();

            playerconnection.send(new PacketPlayOutEntityEffect(entityplayer.getId(), mobeffect));
        }

        if (nbttagcompound != null && nbttagcompound.contains("RootVehicle", 10)) {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("RootVehicle");
            // CraftBukkit start
            WorldServer finalWorldServer = worldserver1;
            Entity entity = EntityTypes.loadEntityRecursive(nbttagcompound1.getCompound("Entity"), finalWorldServer, (entity1) -> {
                return !finalWorldServer.addWithUUID(entity1) ? null : entity1;
                // CraftBukkit end
            });

            if (entity != null) {
                UUID uuid;

                if (nbttagcompound1.hasUUID("Attach")) {
                    uuid = nbttagcompound1.getUUID("Attach");
                } else {
                    uuid = null;
                }

                Iterator iterator1;
                Entity entity1;

                if (entity.getUUID().equals(uuid)) {
                    entityplayer.startRiding(entity, true);
                } else {
                    iterator1 = entity.getIndirectPassengers().iterator();

                    while (iterator1.hasNext()) {
                        entity1 = (Entity) iterator1.next();
                        if (entity1.getUUID().equals(uuid)) {
                            entityplayer.startRiding(entity1, true);
                            break;
                        }
                    }
                }

                if (!entityplayer.isPassenger()) {
                    PlayerList.LOGGER.warn("Couldn't reattach entity to player");
                    entity.discard();
                    iterator1 = entity.getIndirectPassengers().iterator();

                    while (iterator1.hasNext()) {
                        entity1 = (Entity) iterator1.next();
                        entity1.discard();
                    }
                }
            }
        }

        entityplayer.initInventoryMenu();
        // CraftBukkit - Moved from above, added world
        PlayerList.LOGGER.info("{}[{}] logged in with entity id {} at ([{}]{}, {}, {})", entityplayer.getName().getString(), s1, entityplayer.getId(), worldserver1.serverLevelData.getLevelName(), entityplayer.getX(), entityplayer.getY(), entityplayer.getZ());
    }

    public void updateEntireScoreboard(ScoreboardServer scoreboardserver, EntityPlayer entityplayer) {
        Set<ScoreboardObjective> set = Sets.newHashSet();
        Iterator iterator = scoreboardserver.getPlayerTeams().iterator();

        while (iterator.hasNext()) {
            ScoreboardTeam scoreboardteam = (ScoreboardTeam) iterator.next();

            entityplayer.connection.send(PacketPlayOutScoreboardTeam.createAddOrModifyPacket(scoreboardteam, true));
        }

        for (int i = 0; i < 19; ++i) {
            ScoreboardObjective scoreboardobjective = scoreboardserver.getDisplayObjective(i);

            if (scoreboardobjective != null && !set.contains(scoreboardobjective)) {
                List<Packet<?>> list = scoreboardserver.getStartTrackingPackets(scoreboardobjective);
                Iterator iterator1 = list.iterator();

                while (iterator1.hasNext()) {
                    Packet<?> packet = (Packet) iterator1.next();

                    entityplayer.connection.send(packet);
                }

                set.add(scoreboardobjective);
            }
        }

    }

    public void addWorldborderListener(WorldServer worldserver) {
        if (playerIo != null) return; // CraftBukkit
        worldserver.getWorldBorder().addListener(new IWorldBorderListener() {
            @Override
            public void onBorderSizeSet(WorldBorder worldborder, double d0) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderSizePacket(worldborder), worldborder.world); // CraftBukkit
            }

            @Override
            public void onBorderSizeLerping(WorldBorder worldborder, double d0, double d1, long i) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderLerpSizePacket(worldborder), worldborder.world); // CraftBukkit
            }

            @Override
            public void onBorderCenterSet(WorldBorder worldborder, double d0, double d1) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderCenterPacket(worldborder), worldborder.world); // CraftBukkit
            }

            @Override
            public void onBorderSetWarningTime(WorldBorder worldborder, int i) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDelayPacket(worldborder), worldborder.world); // CraftBukkit
            }

            @Override
            public void onBorderSetWarningBlocks(WorldBorder worldborder, int i) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDistancePacket(worldborder), worldborder.world); // CraftBukkit
            }

            @Override
            public void onBorderSetDamagePerBlock(WorldBorder worldborder, double d0) {}

            @Override
            public void onBorderSetDamageSafeZOne(WorldBorder worldborder, double d0) {}
        });
    }

    @Nullable
    public NBTTagCompound load(EntityPlayer entityplayer) {
        NBTTagCompound nbttagcompound = this.server.getWorldData().getLoadedPlayerTag();
        NBTTagCompound nbttagcompound1;

        if (entityplayer.getName().getString().equals(this.server.getSingleplayerName()) && nbttagcompound != null) {
            nbttagcompound1 = nbttagcompound;
            entityplayer.load(nbttagcompound);
            PlayerList.LOGGER.debug("loading single player");
        } else {
            nbttagcompound1 = this.playerIo.load(entityplayer);
        }

        return nbttagcompound1;
    }

    protected void save(EntityPlayer entityplayer) {
        if (!entityplayer.getBukkitEntity().isPersistent()) return; // CraftBukkit
        this.playerIo.save(entityplayer);
        ServerStatisticManager serverstatisticmanager = (ServerStatisticManager) entityplayer.getStats(); // CraftBukkit

        if (serverstatisticmanager != null) {
            serverstatisticmanager.save();
        }

        AdvancementDataPlayer advancementdataplayer = (AdvancementDataPlayer) entityplayer.getAdvancements(); // CraftBukkit

        if (advancementdataplayer != null) {
            advancementdataplayer.save();
        }

    }

    public String remove(EntityPlayer entityplayer) { // CraftBukkit - return string
        WorldServer worldserver = entityplayer.getLevel();

        entityplayer.awardStat(StatisticList.LEAVE_GAME);

        // CraftBukkit start - Quitting must be before we do final save of data, in case plugins need to modify it
        // See SPIGOT-5799, SPIGOT-6145
        if (entityplayer.containerMenu != entityplayer.inventoryMenu) {
            entityplayer.closeContainer();
        }

        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(entityplayer.getBukkitEntity(), "\u00A7e" + entityplayer.getScoreboardName() + " left the game");
        cserver.getPluginManager().callEvent(playerQuitEvent);
        entityplayer.getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());

        entityplayer.doTick(); // SPIGOT-924
        // CraftBukkit end

        this.save(entityplayer);
        if (entityplayer.isPassenger()) {
            Entity entity = entityplayer.getRootVehicle();

            if (entity.hasExactlyOnePlayerPassenger()) {
                PlayerList.LOGGER.debug("Removing player mount");
                entityplayer.stopRiding();
                entity.getPassengersAndSelf().forEach((entity1) -> {
                    entity1.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                });
            }
        }

        entityplayer.unRide();
        worldserver.removePlayerImmediately(entityplayer, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        entityplayer.getAdvancements().stopListening();
        this.players.remove(entityplayer);
        this.playersByName.remove(entityplayer.getScoreboardName().toLowerCase(java.util.Locale.ROOT)); // Spigot
        this.server.getCustomBossEvents().onPlayerDisconnect(entityplayer);
        UUID uuid = entityplayer.getUUID();
        EntityPlayer entityplayer1 = (EntityPlayer) this.playersByUUID.get(uuid);

        if (entityplayer1 == entityplayer) {
            this.playersByUUID.remove(uuid);
            // CraftBukkit start
            // this.stats.remove(uuid);
            // this.advancements.remove(uuid);
            // CraftBukkit end
        }

        // CraftBukkit start
        // this.broadcastAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, new EntityPlayer[]{entityplayer}));
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityplayer);
        for (int i = 0; i < players.size(); i++) {
            EntityPlayer entityplayer2 = (EntityPlayer) this.players.get(i);

            if (entityplayer2.getBukkitEntity().canSee(entityplayer.getBukkitEntity())) {
                entityplayer2.connection.send(packet);
            } else {
                entityplayer2.getBukkitEntity().onEntityRemove(entityplayer);
            }
        }
        // This removes the scoreboard (and player reference) for the specific player in the manager
        cserver.getScoreboardManager().removePlayer(entityplayer.getBukkitEntity());
        // CraftBukkit end

        return playerQuitEvent.getQuitMessage(); // CraftBukkit
    }

    // CraftBukkit start - Whole method, SocketAddress to LoginListener, added hostname to signature, return EntityPlayer
    public EntityPlayer canPlayerLogin(LoginListener loginlistener, GameProfile gameprofile, String hostname) {
        ChatMessage chatmessage;

        // Moved from processLogin
        UUID uuid = EntityHuman.createPlayerUUID(gameprofile);
        List<EntityPlayer> list = Lists.newArrayList();

        EntityPlayer entityplayer;

        for (int i = 0; i < this.players.size(); ++i) {
            entityplayer = (EntityPlayer) this.players.get(i);
            if (entityplayer.getUUID().equals(uuid)) {
                list.add(entityplayer);
            }
        }

        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            entityplayer = (EntityPlayer) iterator.next();
            save(entityplayer); // CraftBukkit - Force the player's inventory to be saved
            entityplayer.connection.disconnect(new ChatMessage("multiplayer.disconnect.duplicate_login", new Object[0]));
        }

        // Instead of kicking then returning, we need to store the kick reason
        // in the event, check with plugins to see if it's ok, and THEN kick
        // depending on the outcome.
        SocketAddress socketaddress = loginlistener.connection.getRemoteAddress();

        EntityPlayer entity = new EntityPlayer(this.server, this.server.getLevel(World.OVERWORLD), gameprofile);
        Player player = entity.getBukkitEntity();
        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((java.net.InetSocketAddress) socketaddress).getAddress(), ((java.net.InetSocketAddress) loginlistener.connection.getRawAddress()).getAddress());

        if (getBans().isBanned(gameprofile) && !getBans().get(gameprofile).hasExpired()) {
            GameProfileBanEntry gameprofilebanentry = (GameProfileBanEntry) this.bans.get(gameprofile);

            chatmessage = new ChatMessage("multiplayer.disconnect.banned.reason", new Object[]{gameprofilebanentry.getReason()});
            if (gameprofilebanentry.getExpires() != null) {
                chatmessage.append((IChatBaseComponent) (new ChatMessage("multiplayer.disconnect.banned.expiration", new Object[]{PlayerList.BAN_DATE_FORMAT.format(gameprofilebanentry.getExpires())})));
            }

            // return chatmessage;
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else if (!this.isWhiteListed(gameprofile)) {
            chatmessage = new ChatMessage("multiplayer.disconnect.not_whitelisted");
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, org.spigotmc.SpigotConfig.whitelistMessage); // Spigot
        } else if (getIpBans().isBanned(socketaddress) && !getIpBans().get(socketaddress).hasExpired()) {
            IpBanEntry ipbanentry = this.ipBans.get(socketaddress);

            chatmessage = new ChatMessage("multiplayer.disconnect.banned_ip.reason", new Object[]{ipbanentry.getReason()});
            if (ipbanentry.getExpires() != null) {
                chatmessage.append((IChatBaseComponent) (new ChatMessage("multiplayer.disconnect.banned_ip.expiration", new Object[]{PlayerList.BAN_DATE_FORMAT.format(ipbanentry.getExpires())})));
            }

            // return chatmessage;
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else {
            // return this.players.size() >= this.maxPlayers && !this.canBypassPlayerLimit(gameprofile) ? new ChatMessage("multiplayer.disconnect.server_full") : null;
            if (this.players.size() >= this.maxPlayers && !this.canBypassPlayerLimit(gameprofile)) {
                event.disallow(PlayerLoginEvent.Result.KICK_FULL, org.spigotmc.SpigotConfig.serverFullMessage); // Spigot
            }
        }

        cserver.getPluginManager().callEvent(event);
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            loginlistener.disconnect(event.getKickMessage());
            return null;
        }
        return entity;
    }

    public EntityPlayer getPlayerForLogin(GameProfile gameprofile, EntityPlayer player) { // CraftBukkit - added EntityPlayer
        /* CraftBukkit startMoved up
        UUID uuid = EntityHuman.createPlayerUUID(gameprofile);
        List<EntityPlayer> list = Lists.newArrayList();

        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            if (entityplayer.getUUID().equals(uuid)) {
                list.add(entityplayer);
            }
        }

        EntityPlayer entityplayer1 = (EntityPlayer) this.playersByUUID.get(gameprofile.getId());

        if (entityplayer1 != null && !list.contains(entityplayer1)) {
            list.add(entityplayer1);
        }

        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer2 = (EntityPlayer) iterator.next();

            entityplayer2.connection.disconnect(new ChatMessage("multiplayer.disconnect.duplicate_login"));
        }

        return new EntityPlayer(this.server, this.server.overworld(), gameprofile);
        */
        return player;
        // CraftBukkit end
    }

    // CraftBukkit start
    public EntityPlayer respawn(EntityPlayer entityplayer, boolean flag) {
        return this.respawn(entityplayer, this.server.getLevel(entityplayer.getRespawnDimension()), flag, null, true);
    }

    public EntityPlayer respawn(EntityPlayer entityplayer, WorldServer worldserver, boolean flag, Location location, boolean avoidSuffocation) {
        entityplayer.stopRiding(); // CraftBukkit
        this.players.remove(entityplayer);
        this.playersByName.remove(entityplayer.getScoreboardName().toLowerCase(java.util.Locale.ROOT)); // Spigot
        entityplayer.getLevel().removePlayerImmediately(entityplayer, Entity.RemovalReason.DISCARDED);
        BlockPosition blockposition = entityplayer.getRespawnPosition();
        float f = entityplayer.getRespawnAngle();
        boolean flag1 = entityplayer.isRespawnForced();
        /* CraftBukkit start
        WorldServer worldserver = this.server.getLevel(entityplayer.getRespawnDimension());
        Optional optional;

        if (worldserver != null && blockposition != null) {
            optional = EntityHuman.findRespawnPositionAndUseSpawnBlock(worldserver, blockposition, f, flag1, flag);
        } else {
            optional = Optional.empty();
        }

        WorldServer worldserver1 = worldserver != null && optional.isPresent() ? worldserver : this.server.overworld();
        EntityPlayer entityplayer1 = new EntityPlayer(this.server, worldserver1, entityplayer.getGameProfile());
        // */
        EntityPlayer entityplayer1 = entityplayer;
        org.bukkit.World fromWorld = entityplayer.getBukkitEntity().getWorld();
        entityplayer.wonGame = false;
        // CraftBukkit end

        entityplayer1.connection = entityplayer.connection;
        entityplayer1.restoreFrom(entityplayer, flag);
        entityplayer1.setId(entityplayer.getId());
        entityplayer1.setMainArm(entityplayer.getMainArm());
        Iterator iterator = entityplayer.getTags().iterator();

        while (iterator.hasNext()) {
            String s = (String) iterator.next();

            entityplayer1.addTag(s);
        }

        boolean flag2 = false;

        // CraftBukkit start - fire PlayerRespawnEvent
        if (location == null) {
            boolean isBedSpawn = false;
            WorldServer worldserver1 = this.server.getLevel(entityplayer.getRespawnDimension());
            if (worldserver1 != null) {
                Optional optional;

                if (blockposition != null) {
                    optional = EntityHuman.findRespawnPositionAndUseSpawnBlock(worldserver1, blockposition, f, flag1, flag);
                } else {
                    optional = Optional.empty();
                }

                if (optional.isPresent()) {
                    IBlockData iblockdata = worldserver1.getBlockState(blockposition);
                    boolean flag3 = iblockdata.is(Blocks.RESPAWN_ANCHOR);
                    Vec3D vec3d = (Vec3D) optional.get();
                    float f1;

                    if (!iblockdata.is((Tag) TagsBlock.BEDS) && !flag3) {
                        f1 = f;
                    } else {
                        Vec3D vec3d1 = Vec3D.atBottomCenterOf(blockposition).subtract(vec3d).normalize();

                        f1 = (float) MathHelper.wrapDegrees(MathHelper.atan2(vec3d1.z, vec3d1.x) * 57.2957763671875D - 90.0D);
                    }

                    entityplayer1.setRespawnPosition(worldserver1.dimension(), blockposition, f, flag1, false);
                    flag2 = !flag && flag3;
                    isBedSpawn = true;
                    location = new Location(worldserver1.getWorld(), vec3d.x, vec3d.y, vec3d.z, f1, 0.0F);
                } else if (blockposition != null) {
                    entityplayer1.connection.send(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
                    entityplayer1.setRespawnPosition(null, null, 0f, false, false); // CraftBukkit - SPIGOT-5988: Clear respawn location when obstructed
                }
            }

            if (location == null) {
                worldserver1 = this.server.getLevel(World.OVERWORLD);
                blockposition = entityplayer1.getSpawnPoint(worldserver1);
                location = new Location(worldserver1.getWorld(), (double) ((float) blockposition.getX() + 0.5F), (double) ((float) blockposition.getY() + 0.1F), (double) ((float) blockposition.getZ() + 0.5F));
            }

            Player respawnPlayer = entityplayer1.getBukkitEntity();
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn && !flag2, flag2);
            cserver.getPluginManager().callEvent(respawnEvent);
            // Spigot Start
            if (entityplayer.connection.isDisconnected()) {
                return entityplayer;
            }
            // Spigot End

            location = respawnEvent.getRespawnLocation();
            if (!flag) entityplayer.reset(); // SPIGOT-4785
        } else {
            location.setWorld(worldserver.getWorld());
        }
        WorldServer worldserver1 = ((CraftWorld) location.getWorld()).getHandle();
        entityplayer1.forceSetPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        // CraftBukkit end

        while (avoidSuffocation && !worldserver1.noCollision(entityplayer1) && entityplayer1.getY() < (double) worldserver1.getMaxBuildHeight()) {
            entityplayer1.setPos(entityplayer1.getX(), entityplayer1.getY() + 1.0D, entityplayer1.getZ());
        }
        // CraftBukkit start
        WorldData worlddata = worldserver1.getLevelData();
        entityplayer1.connection.send(new PacketPlayOutRespawn(worldserver1.dimensionType(), worldserver1.dimension(), BiomeManager.obfuscateSeed(worldserver1.getSeed()), entityplayer1.gameMode.getGameModeForPlayer(), entityplayer1.gameMode.getPreviousGameModeForPlayer(), worldserver1.isDebug(), worldserver1.isFlat(), flag));
        entityplayer1.connection.send(new PacketPlayOutViewDistance(worldserver1.spigotConfig.viewDistance)); // Spigot
        entityplayer1.connection.send(new ClientboundSetSimulationDistancePacket(worldserver1.spigotConfig.simulationDistance)); // Spigot
        entityplayer1.spawnIn(worldserver1);
        entityplayer1.unsetRemoved();
        entityplayer1.connection.teleport(new Location(worldserver1.getWorld(), entityplayer1.getX(), entityplayer1.getY(), entityplayer1.getZ(), entityplayer1.getYRot(), entityplayer1.getXRot()));
        entityplayer1.setShiftKeyDown(false);

        // entityplayer1.connection.teleport(entityplayer1.getX(), entityplayer1.getY(), entityplayer1.getZ(), entityplayer1.getYRot(), entityplayer1.getXRot());
        entityplayer1.connection.send(new PacketPlayOutSpawnPosition(worldserver1.getSharedSpawnPos(), worldserver1.getSharedSpawnAngle()));
        entityplayer1.connection.send(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
        entityplayer1.connection.send(new PacketPlayOutExperience(entityplayer1.experienceProgress, entityplayer1.totalExperience, entityplayer1.experienceLevel));
        this.sendLevelInfo(entityplayer1, worldserver1);
        this.sendPlayerPermissionLevel(entityplayer1);
        if (!entityplayer.connection.isDisconnected()) {
            worldserver1.addRespawnedPlayer(entityplayer1);
            this.players.add(entityplayer1);
            this.playersByName.put(entityplayer1.getScoreboardName().toLowerCase(java.util.Locale.ROOT), entityplayer1); // Spigot
            this.playersByUUID.put(entityplayer1.getUUID(), entityplayer1);
        }
        // entityplayer1.initInventoryMenu();
        entityplayer1.setHealth(entityplayer1.getHealth());
        if (flag2) {
            entityplayer1.connection.send(new PacketPlayOutNamedSoundEffect(SoundEffects.RESPAWN_ANCHOR_DEPLETE, SoundCategory.BLOCKS, (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ(), 1.0F, 1.0F));
        }
        // Added from changeDimension
        sendAllPlayerInfo(entityplayer); // Update health, etc...
        entityplayer.onUpdateAbilities();
        for (MobEffect mobEffect : entityplayer.getActiveEffects()) {
            entityplayer.connection.send(new PacketPlayOutEntityEffect(entityplayer.getId(), mobEffect));
        }

        // Fire advancement trigger
        entityplayer.triggerDimensionChangeTriggers(((CraftWorld) fromWorld).getHandle());

        // Don't fire on respawn
        if (fromWorld != location.getWorld()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(entityplayer.getBukkitEntity(), fromWorld);
            server.server.getPluginManager().callEvent(event);
        }

        // Save player file again if they were disconnected
        if (entityplayer.connection.isDisconnected()) {
            this.save(entityplayer);
        }
        // CraftBukkit end
        return entityplayer1;
    }

    public void sendPlayerPermissionLevel(EntityPlayer entityplayer) {
        GameProfile gameprofile = entityplayer.getGameProfile();
        int i = this.server.getProfilePermissions(gameprofile);

        this.sendPlayerPermissionLevel(entityplayer, i);
    }

    public void tick() {
        if (++this.sendAllPlayerInfoIn > 600) {
            // CraftBukkit start
            for (int i = 0; i < this.players.size(); ++i) {
                final EntityPlayer target = (EntityPlayer) this.players.get(i);

                target.connection.send(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY, this.players.stream().filter(new Predicate<EntityPlayer>() {
                    @Override
                    public boolean apply(EntityPlayer input) {
                        return target.getBukkitEntity().canSee(input.getBukkitEntity());
                    }
                }).collect(Collectors.toList())));
            }
            // CraftBukkit end
            this.sendAllPlayerInfoIn = 0;
        }

    }

    public void broadcastAll(Packet<?> packet) {
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            entityplayer.connection.send(packet);
        }

    }

    // CraftBukkit start - add a world/entity limited version
    public void broadcastAll(Packet packet, EntityHuman entityhuman) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer =  this.players.get(i);
            if (entityhuman != null && !entityplayer.getBukkitEntity().canSee(entityhuman.getBukkitEntity())) {
                continue;
            }
            ((EntityPlayer) this.players.get(i)).connection.send(packet);
        }
    }

    public void broadcastAll(Packet packet, World world) {
        for (int i = 0; i < world.players().size(); ++i) {
            ((EntityPlayer) world.players().get(i)).connection.send(packet);
        }

    }
    // CraftBukkit end

    public void broadcastAll(Packet<?> packet, ResourceKey<World> resourcekey) {
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            if (entityplayer.level.dimension() == resourcekey) {
                entityplayer.connection.send(packet);
            }
        }

    }

    public void broadcastToTeam(EntityHuman entityhuman, IChatBaseComponent ichatbasecomponent) {
        ScoreboardTeamBase scoreboardteambase = entityhuman.getTeam();

        if (scoreboardteambase != null) {
            Collection<String> collection = scoreboardteambase.getPlayers();
            Iterator iterator = collection.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();
                EntityPlayer entityplayer = this.getPlayerByName(s);

                if (entityplayer != null && entityplayer != entityhuman) {
                    entityplayer.sendMessage(ichatbasecomponent, entityhuman.getUUID());
                }
            }

        }
    }

    public void broadcastToAllExceptTeam(EntityHuman entityhuman, IChatBaseComponent ichatbasecomponent) {
        ScoreboardTeamBase scoreboardteambase = entityhuman.getTeam();

        if (scoreboardteambase == null) {
            this.broadcastMessage(ichatbasecomponent, ChatMessageType.SYSTEM, entityhuman.getUUID());
        } else {
            for (int i = 0; i < this.players.size(); ++i) {
                EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

                if (entityplayer.getTeam() != scoreboardteambase) {
                    entityplayer.sendMessage(ichatbasecomponent, entityhuman.getUUID());
                }
            }

        }
    }

    public String[] getPlayerNamesArray() {
        String[] astring = new String[this.players.size()];

        for (int i = 0; i < this.players.size(); ++i) {
            astring[i] = ((EntityPlayer) this.players.get(i)).getGameProfile().getName();
        }

        return astring;
    }

    public GameProfileBanList getBans() {
        return this.bans;
    }

    public IpBanList getIpBans() {
        return this.ipBans;
    }

    public void op(GameProfile gameprofile) {
        this.ops.add(new OpListEntry(gameprofile, this.server.getOperatorUserPermissionLevel(), this.ops.canBypassPlayerLimit(gameprofile)));
        EntityPlayer entityplayer = this.getPlayer(gameprofile.getId());

        if (entityplayer != null) {
            this.sendPlayerPermissionLevel(entityplayer);
        }

    }

    public void deop(GameProfile gameprofile) {
        this.ops.remove(gameprofile); // CraftBukkit - decompile error
        EntityPlayer entityplayer = this.getPlayer(gameprofile.getId());

        if (entityplayer != null) {
            this.sendPlayerPermissionLevel(entityplayer);
        }

    }

    private void sendPlayerPermissionLevel(EntityPlayer entityplayer, int i) {
        if (entityplayer.connection != null) {
            byte b0;

            if (i <= 0) {
                b0 = 24;
            } else if (i >= 4) {
                b0 = 28;
            } else {
                b0 = (byte) (24 + i);
            }

            entityplayer.connection.send(new PacketPlayOutEntityStatus(entityplayer, b0));
        }

        entityplayer.getBukkitEntity().recalculatePermissions(); // CraftBukkit
        this.server.getCommands().sendCommands(entityplayer);
    }

    public boolean isWhiteListed(GameProfile gameprofile) {
        return !this.doWhiteList || this.ops.contains(gameprofile) || this.whitelist.contains(gameprofile);
    }

    public boolean isOp(GameProfile gameprofile) {
        return this.ops.contains(gameprofile) || this.server.isSingleplayerOwner(gameprofile) && this.server.getWorldData().getAllowCommands() || this.allowCheatsForAllPlayers;
    }

    @Nullable
    public EntityPlayer getPlayerByName(String s) {
        return this.playersByName.get(s.toLowerCase(java.util.Locale.ROOT)); // Spigot
    }

    public void broadcast(@Nullable EntityHuman entityhuman, double d0, double d1, double d2, double d3, ResourceKey<World> resourcekey, Packet<?> packet) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            // CraftBukkit start - Test if player receiving packet can see the source of the packet
            if (entityhuman != null && !entityplayer.getBukkitEntity().canSee(entityhuman.getBukkitEntity())) {
               continue;
            }
            // CraftBukkit end

            if (entityplayer != entityhuman && entityplayer.level.dimension() == resourcekey) {
                double d4 = d0 - entityplayer.getX();
                double d5 = d1 - entityplayer.getY();
                double d6 = d2 - entityplayer.getZ();

                if (d4 * d4 + d5 * d5 + d6 * d6 < d3 * d3) {
                    entityplayer.connection.send(packet);
                }
            }
        }

    }

    public void saveAll() {
        for (int i = 0; i < this.players.size(); ++i) {
            this.save((EntityPlayer) this.players.get(i));
        }

    }

    public WhiteList getWhiteList() {
        return this.whitelist;
    }

    public String[] getWhiteListNames() {
        return this.whitelist.getUserList();
    }

    public OpList getOps() {
        return this.ops;
    }

    public String[] getOpNames() {
        return this.ops.getUserList();
    }

    public void reloadWhiteList() {}

    public void sendLevelInfo(EntityPlayer entityplayer, WorldServer worldserver) {
        WorldBorder worldborder = entityplayer.level.getWorldBorder(); // CraftBukkit

        entityplayer.connection.send(new ClientboundInitializeBorderPacket(worldborder));
        entityplayer.connection.send(new PacketPlayOutUpdateTime(worldserver.getGameTime(), worldserver.getDayTime(), worldserver.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
        entityplayer.connection.send(new PacketPlayOutSpawnPosition(worldserver.getSharedSpawnPos(), worldserver.getSharedSpawnAngle()));
        if (worldserver.isRaining()) {
            // CraftBukkit start - handle player weather
            // entityplayer.connection.send(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.START_RAINING, 0.0F));
            // entityplayer.connection.send(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.RAIN_LEVEL_CHANGE, worldserver.getRainLevel(1.0F)));
            // entityplayer.connection.send(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.THUNDER_LEVEL_CHANGE, worldserver.getThunderLevel(1.0F)));
            entityplayer.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL, false);
            entityplayer.updateWeather(-worldserver.rainLevel, worldserver.rainLevel, -worldserver.thunderLevel, worldserver.thunderLevel);
            // CraftBukkit end
        }

    }

    public void sendAllPlayerInfo(EntityPlayer entityplayer) {
        entityplayer.inventoryMenu.sendAllDataToRemote();
        // entityplayer.resetSentInfo();
        entityplayer.getBukkitEntity().updateScaledHealth(); // CraftBukkit - Update scaled health on respawn and worldchange
        entityplayer.connection.send(new PacketPlayOutHeldItemSlot(entityplayer.getInventory().selected));
        // CraftBukkit start - from GameRules
        int i = entityplayer.level.getGameRules().getBoolean(GameRules.RULE_REDUCEDDEBUGINFO) ? 22 : 23;
        entityplayer.connection.send(new PacketPlayOutEntityStatus(entityplayer, (byte) i));
        float immediateRespawn = entityplayer.level.getGameRules().getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN) ? 1.0F: 0.0F;
        entityplayer.connection.send(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.IMMEDIATE_RESPAWN, immediateRespawn));
        // CraftBukkit end
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public boolean isUsingWhitelist() {
        return this.doWhiteList;
    }

    public void setUsingWhiteList(boolean flag) {
        this.doWhiteList = flag;
    }

    public List<EntityPlayer> getPlayersWithAddress(String s) {
        List<EntityPlayer> list = Lists.newArrayList();
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            if (entityplayer.getIpAddress().equals(s)) {
                list.add(entityplayer);
            }
        }

        return list;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public int getSimulationDistance() {
        return this.simulationDistance;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    @Nullable
    public NBTTagCompound getSingleplayerData() {
        return null;
    }

    public void setAllowCheatsForAllPlayers(boolean flag) {
        this.allowCheatsForAllPlayers = flag;
    }

    public void removeAll() {
        // CraftBukkit start - disconnect safely
        for (EntityPlayer player : this.players) {
            player.connection.disconnect(this.server.server.getShutdownMessage()); // CraftBukkit - add custom shutdown message
        }
        // CraftBukkit end

    }

    // CraftBukkit start
    public void broadcastMessage(IChatBaseComponent[] iChatBaseComponents) {
        for (IChatBaseComponent component : iChatBaseComponents) {
            broadcastMessage(component, ChatMessageType.SYSTEM, SystemUtils.NIL_UUID);
        }
    }
    // CraftBukkit end

    public void broadcastMessage(IChatBaseComponent ichatbasecomponent, ChatMessageType chatmessagetype, UUID uuid) {
        this.server.sendMessage(ichatbasecomponent, uuid);
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            entityplayer.sendMessage(ichatbasecomponent, chatmessagetype, uuid);
        }

    }

    public void broadcastMessage(IChatBaseComponent ichatbasecomponent, Function<EntityPlayer, IChatBaseComponent> function, ChatMessageType chatmessagetype, UUID uuid) {
        this.server.sendMessage(ichatbasecomponent, uuid);
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();
            IChatBaseComponent ichatbasecomponent1 = (IChatBaseComponent) function.apply(entityplayer);

            if (ichatbasecomponent1 != null) {
                entityplayer.sendMessage(ichatbasecomponent1, chatmessagetype, uuid);
            }
        }

    }

    // CraftBukkit start
    public ServerStatisticManager getPlayerStats(EntityPlayer entityhuman) {
        ServerStatisticManager serverstatisticmanager = entityhuman.getStats();
        return serverstatisticmanager == null ? getPlayerStats(entityhuman.getUUID(), entityhuman.getDisplayName().getString()) : serverstatisticmanager;
    }

    public ServerStatisticManager getPlayerStats(UUID uuid, String displayName) {
        EntityPlayer entityhuman = this.getPlayer(uuid);
        ServerStatisticManager serverstatisticmanager = entityhuman == null ? null : (ServerStatisticManager) entityhuman.getStats();
        // CraftBukkit end

        if (serverstatisticmanager == null) {
            File file = this.server.getWorldPath(SavedFile.PLAYER_STATS_DIR).toFile();
            File file1 = new File(file, uuid + ".json");

            if (!file1.exists()) {
                File file2 = new File(file, displayName + ".json"); // CraftBukkit
                Path path = file2.toPath();

                if (FileUtils.isPathNormalized(path) && FileUtils.isPathPortable(path) && path.startsWith(file.getPath()) && file2.isFile()) {
                    file2.renameTo(file1);
                }
            }

            serverstatisticmanager = new ServerStatisticManager(this.server, file1);
            // this.stats.put(uuid, serverstatisticmanager); // CraftBukkit
        }

        return serverstatisticmanager;
    }

    public AdvancementDataPlayer getPlayerAdvancements(EntityPlayer entityplayer) {
        UUID uuid = entityplayer.getUUID();
        AdvancementDataPlayer advancementdataplayer = (AdvancementDataPlayer) entityplayer.getAdvancements(); // CraftBukkit

        if (advancementdataplayer == null) {
            File file = this.server.getWorldPath(SavedFile.PLAYER_ADVANCEMENTS_DIR).toFile();
            File file1 = new File(file, uuid + ".json");

            advancementdataplayer = new AdvancementDataPlayer(this.server.getFixerUpper(), this, this.server.getAdvancements(), file1, entityplayer);
            // this.advancements.put(uuid, advancementdataplayer); // CraftBukkit
        }

        advancementdataplayer.setPlayer(entityplayer);
        return advancementdataplayer;
    }

    public void setViewDistance(int i) {
        this.viewDistance = i;
        this.broadcastAll(new PacketPlayOutViewDistance(i));
        Iterator iterator = this.server.getAllLevels().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (worldserver != null) {
                worldserver.getChunkSource().setViewDistance(i);
            }
        }

    }

    public void setSimulationDistance(int i) {
        this.simulationDistance = i;
        this.broadcastAll(new ClientboundSetSimulationDistancePacket(i));
        Iterator iterator = this.server.getAllLevels().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (worldserver != null) {
                worldserver.getChunkSource().setSimulationDistance(i);
            }
        }

    }

    public List<EntityPlayer> getPlayers() {
        return this.players;
    }

    @Nullable
    public EntityPlayer getPlayer(UUID uuid) {
        return (EntityPlayer) this.playersByUUID.get(uuid);
    }

    public boolean canBypassPlayerLimit(GameProfile gameprofile) {
        return false;
    }

    public void reloadResources() {
        // CraftBukkit start
        /*Iterator iterator = this.advancements.values().iterator();

        while (iterator.hasNext()) {
            AdvancementDataPlayer advancementdataplayer = (AdvancementDataPlayer) iterator.next();

            advancementdataplayer.reload(this.server.getAdvancements());
        }*/

        for (EntityPlayer player : players) {
            player.getAdvancements().reload(this.server.getAdvancements());
            player.getAdvancements().flushDirty(player); // CraftBukkit - trigger immediate flush of advancements
        }
        // CraftBukkit end

        this.broadcastAll(new PacketPlayOutTags(this.server.getTags().serializeToNetwork(this.registryHolder)));
        PacketPlayOutRecipeUpdate packetplayoutrecipeupdate = new PacketPlayOutRecipeUpdate(this.server.getRecipeManager().getRecipes());
        Iterator iterator1 = this.players.iterator();

        while (iterator1.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator1.next();

            entityplayer.connection.send(packetplayoutrecipeupdate);
            entityplayer.getRecipeBook().sendInitialRecipeBook(entityplayer);
        }

    }

    public boolean isAllowCheatsForAllPlayers() {
        return this.allowCheatsForAllPlayers;
    }
}
