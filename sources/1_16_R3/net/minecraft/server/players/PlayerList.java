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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
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
import net.minecraft.network.protocol.game.PacketPlayOutAbilities;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
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
import net.minecraft.network.protocol.game.PacketPlayOutWorldBorder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.level.DemoPlayerInteractManager;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.PlayerInteractManager;
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
import net.minecraft.world.level.EnumGamemode;
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
import com.google.common.collect.Iterables;

import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.LoginListener;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;

import org.bukkit.Location;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
// CraftBukkit end

public abstract class PlayerList {

    public static final File b = new File("banned-players.json");
    public static final File c = new File("banned-ips.json");
    public static final File d = new File("ops.json");
    public static final File e = new File("whitelist.json");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat g = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    private final MinecraftServer server;
    public final List<EntityPlayer> players = new java.util.concurrent.CopyOnWriteArrayList(); // CraftBukkit - ArrayList -> CopyOnWriteArrayList: Iterator safety
    private final Map<UUID, EntityPlayer> j = Maps.newHashMap();
    private final GameProfileBanList k;
    private final IpBanList l;
    private final OpList operators;
    private final WhiteList whitelist;
    // CraftBukkit start
    // private final Map<UUID, ServerStatisticManager> o;
    // private final Map<UUID, AdvancementDataPlayer> p;
    // CraftBukkit end
    public final WorldNBTStorage playerFileData;
    private boolean hasWhitelist;
    private final IRegistryCustom.Dimension s;
    protected final int maxPlayers;
    private int viewDistance;
    private EnumGamemode u;
    private boolean v;
    private int w;

    // CraftBukkit start
    private CraftServer cserver;
    private final Map<String,EntityPlayer> playersByName = new java.util.HashMap<>();

    public PlayerList(MinecraftServer minecraftserver, IRegistryCustom.Dimension iregistrycustom_dimension, WorldNBTStorage worldnbtstorage, int i) {
        this.cserver = minecraftserver.server = new CraftServer((DedicatedServer) minecraftserver, this);
        minecraftserver.console = org.bukkit.craftbukkit.command.ColouredConsoleSender.getInstance();
        minecraftserver.reader.addCompleter(new org.bukkit.craftbukkit.command.ConsoleCommandCompleter(minecraftserver.server));
        // CraftBukkit end

        this.k = new GameProfileBanList(PlayerList.b);
        this.l = new IpBanList(PlayerList.c);
        this.operators = new OpList(PlayerList.d);
        this.whitelist = new WhiteList(PlayerList.e);
        // CraftBukkit start
        // this.o = Maps.newHashMap();
        // this.p = Maps.newHashMap();
        // CraftBukkit end
        this.server = minecraftserver;
        this.s = iregistrycustom_dimension;
        this.maxPlayers = i;
        this.playerFileData = worldnbtstorage;
    }

    public void a(NetworkManager networkmanager, EntityPlayer entityplayer) {
        GameProfile gameprofile = entityplayer.getProfile();
        UserCache usercache = this.server.getUserCache();
        GameProfile gameprofile1 = usercache.getProfile(gameprofile.getId());
        String s = gameprofile1 == null ? gameprofile.getName() : gameprofile1.getName();

        usercache.a(gameprofile);
        NBTTagCompound nbttagcompound = this.a(entityplayer);
        ResourceKey resourcekey;
        // CraftBukkit start - Better rename detection
        if (nbttagcompound != null && nbttagcompound.hasKey("bukkit")) {
            NBTTagCompound bukkit = nbttagcompound.getCompound("bukkit");
            s = bukkit.hasKeyOfType("lastKnownName", 8) ? bukkit.getString("lastKnownName") : s;
        }
        // CraftBukkit end

        if (nbttagcompound != null) {
            DataResult dataresult = DimensionManager.a(new Dynamic(DynamicOpsNBT.a, nbttagcompound.get("Dimension")));
            Logger logger = PlayerList.LOGGER;

            logger.getClass();
            resourcekey = (ResourceKey) dataresult.resultOrPartial(logger::error).orElse(World.OVERWORLD);
        } else {
            resourcekey = World.OVERWORLD;
        }

        ResourceKey<World> resourcekey1 = resourcekey;
        WorldServer worldserver = this.server.getWorldServer(resourcekey1);
        WorldServer worldserver1;

        if (worldserver == null) {
            PlayerList.LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", resourcekey1);
            worldserver1 = this.server.E();
        } else {
            worldserver1 = worldserver;
        }

        entityplayer.spawnIn(worldserver1);
        entityplayer.playerInteractManager.a((WorldServer) entityplayer.world);
        String s1 = "local";

        if (networkmanager.getSocketAddress() != null) {
            s1 = networkmanager.getSocketAddress().toString();
        }

        // Spigot start - spawn location event
        Player bukkitPlayer = entityplayer.getBukkitEntity();
        org.spigotmc.event.player.PlayerSpawnLocationEvent ev = new org.spigotmc.event.player.PlayerSpawnLocationEvent(bukkitPlayer, bukkitPlayer.getLocation());
        cserver.getPluginManager().callEvent(ev);

        Location loc = ev.getSpawnLocation();
        worldserver1 = ((CraftWorld) loc.getWorld()).getHandle();

        entityplayer.spawnIn(worldserver1);
        entityplayer.playerInteractManager.a((WorldServer) entityplayer.world);
        entityplayer.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        // Spigot end

        // CraftBukkit - Moved message to after join
        // PlayerList.LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", entityplayer.getDisplayName().getString(), s1, entityplayer.getId(), entityplayer.locX(), entityplayer.locY(), entityplayer.locZ());
        WorldData worlddata = worldserver1.getWorldData();

        this.a(entityplayer, (EntityPlayer) null, worldserver1);
        PlayerConnection playerconnection = new PlayerConnection(this.server, networkmanager, entityplayer);
        GameRules gamerules = worldserver1.getGameRules();
        boolean flag = gamerules.getBoolean(GameRules.DO_IMMEDIATE_RESPAWN);
        boolean flag1 = gamerules.getBoolean(GameRules.REDUCED_DEBUG_INFO);

        // Spigot - view distance
        playerconnection.sendPacket(new PacketPlayOutLogin(entityplayer.getId(), entityplayer.playerInteractManager.getGameMode(), entityplayer.playerInteractManager.c(), BiomeManager.a(worldserver1.getSeed()), worlddata.isHardcore(), this.server.F(), this.s, worldserver1.getDimensionManager(), worldserver1.getDimensionKey(), this.getMaxPlayers(), worldserver1.spigotConfig.viewDistance, flag1, !flag, worldserver1.isDebugWorld(), worldserver1.isFlatWorld()));
        entityplayer.getBukkitEntity().sendSupportedChannels(); // CraftBukkit
        playerconnection.sendPacket(new PacketPlayOutCustomPayload(PacketPlayOutCustomPayload.a, (new PacketDataSerializer(Unpooled.buffer())).a(this.getServer().getServerModName())));
        playerconnection.sendPacket(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
        playerconnection.sendPacket(new PacketPlayOutAbilities(entityplayer.abilities));
        playerconnection.sendPacket(new PacketPlayOutHeldItemSlot(entityplayer.inventory.itemInHandIndex));
        playerconnection.sendPacket(new PacketPlayOutRecipeUpdate(this.server.getCraftingManager().b()));
        playerconnection.sendPacket(new PacketPlayOutTags(this.server.getTagRegistry()));
        this.d(entityplayer);
        entityplayer.getStatisticManager().c();
        entityplayer.getRecipeBook().a(entityplayer);
        this.sendScoreboard(worldserver1.getScoreboard(), entityplayer);
        this.server.invalidatePingSample();
        ChatMessage chatmessage;

        if (entityplayer.getProfile().getName().equalsIgnoreCase(s)) {
            chatmessage = new ChatMessage("multiplayer.player.joined", new Object[]{entityplayer.getScoreboardDisplayName()});
        } else {
            chatmessage = new ChatMessage("multiplayer.player.joined.renamed", new Object[]{entityplayer.getScoreboardDisplayName(), s});
        }
        // CraftBukkit start
        chatmessage.a(EnumChatFormat.YELLOW);
        String joinMessage = CraftChatMessage.fromComponent(chatmessage);

        playerconnection.a(entityplayer.locX(), entityplayer.locY(), entityplayer.locZ(), entityplayer.yaw, entityplayer.pitch);
        this.players.add(entityplayer);
        this.playersByName.put(entityplayer.getName().toLowerCase(java.util.Locale.ROOT), entityplayer); // Spigot
        this.j.put(entityplayer.getUniqueID(), entityplayer);
        // this.sendAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, new EntityPlayer[]{entityplayer})); // CraftBukkit - replaced with loop below

        // CraftBukkit start
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(cserver.getPlayer(entityplayer), joinMessage);
        cserver.getPluginManager().callEvent(playerJoinEvent);

        if (!entityplayer.playerConnection.networkManager.isConnected()) {
            return;
        }

        joinMessage = playerJoinEvent.getJoinMessage();

        if (joinMessage != null && joinMessage.length() > 0) {
            for (IChatBaseComponent line : org.bukkit.craftbukkit.util.CraftChatMessage.fromString(joinMessage)) {
                server.getPlayerList().sendAll(new PacketPlayOutChat(line, ChatMessageType.SYSTEM, SystemUtils.b));
            }
        }
        // CraftBukkit end

        // CraftBukkit start - sendAll above replaced with this loop
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityplayer);

        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer1 = (EntityPlayer) this.players.get(i);

            if (entityplayer1.getBukkitEntity().canSee(entityplayer.getBukkitEntity())) {
                entityplayer1.playerConnection.sendPacket(packet);
            }

            if (!entityplayer.getBukkitEntity().canSee(entityplayer1.getBukkitEntity())) {
                continue;
            }

            entityplayer.playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, new EntityPlayer[] { entityplayer1}));
        }
        entityplayer.sentListPacket = true;
        // CraftBukkit end

        entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityMetadata(entityplayer.getId(), entityplayer.getDataWatcher(), true)); // CraftBukkit - BungeeCord#2321, send complete data to self on spawn

        // CraftBukkit start - Only add if the player wasn't moved in the event
        if (entityplayer.world == worldserver1 && !worldserver1.getPlayers().contains(entityplayer)) {
            worldserver1.addPlayerJoin(entityplayer);
            this.server.getBossBattleCustomData().a(entityplayer);
        }

        worldserver1 = entityplayer.getWorldServer();  // CraftBukkit - Update in case join event changed it
        // CraftBukkit end
        this.a(entityplayer, worldserver1);
        if (!this.server.getResourcePack().isEmpty()) {
            entityplayer.setResourcePack(this.server.getResourcePack(), this.server.getResourcePackHash());
        }

        Iterator iterator = entityplayer.getEffects().iterator();

        while (iterator.hasNext()) {
            MobEffect mobeffect = (MobEffect) iterator.next();

            playerconnection.sendPacket(new PacketPlayOutEntityEffect(entityplayer.getId(), mobeffect));
        }

        if (nbttagcompound != null && nbttagcompound.hasKeyOfType("RootVehicle", 10)) {
            NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("RootVehicle");
            // CraftBukkit start
            WorldServer finalWorldServer = worldserver1;
            Entity entity = EntityTypes.a(nbttagcompound1.getCompound("Entity"), finalWorldServer, (entity1) -> {
                return !finalWorldServer.addEntitySerialized(entity1) ? null : entity1;
                // CraftBukkit end
            });

            if (entity != null) {
                UUID uuid;

                if (nbttagcompound1.b("Attach")) {
                    uuid = nbttagcompound1.a("Attach");
                } else {
                    uuid = null;
                }

                Iterator iterator1;
                Entity entity1;

                if (entity.getUniqueID().equals(uuid)) {
                    entityplayer.a(entity, true);
                } else {
                    iterator1 = entity.getAllPassengers().iterator();

                    while (iterator1.hasNext()) {
                        entity1 = (Entity) iterator1.next();
                        if (entity1.getUniqueID().equals(uuid)) {
                            entityplayer.a(entity1, true);
                            break;
                        }
                    }
                }

                if (!entityplayer.isPassenger()) {
                    PlayerList.LOGGER.warn("Couldn't reattach entity to player");
                    worldserver1.removeEntity(entity);
                    iterator1 = entity.getAllPassengers().iterator();

                    while (iterator1.hasNext()) {
                        entity1 = (Entity) iterator1.next();
                        worldserver1.removeEntity(entity1);
                    }
                }
            }
        }

        entityplayer.syncInventory();
        // CraftBukkit - Moved from above, added world
        PlayerList.LOGGER.info("{}[{}] logged in with entity id {} at ([{}]{}, {}, {})", entityplayer.getDisplayName().getString(), s1, entityplayer.getId(), worldserver1.worldDataServer.getName(), entityplayer.locX(), entityplayer.locY(), entityplayer.locZ());
    }

    public void sendScoreboard(ScoreboardServer scoreboardserver, EntityPlayer entityplayer) {
        Set<ScoreboardObjective> set = Sets.newHashSet();
        Iterator iterator = scoreboardserver.getTeams().iterator();

        while (iterator.hasNext()) {
            ScoreboardTeam scoreboardteam = (ScoreboardTeam) iterator.next();

            entityplayer.playerConnection.sendPacket(new PacketPlayOutScoreboardTeam(scoreboardteam, 0));
        }

        for (int i = 0; i < 19; ++i) {
            ScoreboardObjective scoreboardobjective = scoreboardserver.getObjectiveForSlot(i);

            if (scoreboardobjective != null && !set.contains(scoreboardobjective)) {
                List<Packet<?>> list = scoreboardserver.getScoreboardScorePacketsForObjective(scoreboardobjective);
                Iterator iterator1 = list.iterator();

                while (iterator1.hasNext()) {
                    Packet<?> packet = (Packet) iterator1.next();

                    entityplayer.playerConnection.sendPacket(packet);
                }

                set.add(scoreboardobjective);
            }
        }

    }

    public void setPlayerFileData(WorldServer worldserver) {
        if (playerFileData != null) return; // CraftBukkit
        worldserver.getWorldBorder().a(new IWorldBorderListener() {
            @Override
            public void a(WorldBorder worldborder, double d0) {
                PlayerList.this.sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_SIZE), worldborder.world);
            }

            @Override
            public void a(WorldBorder worldborder, double d0, double d1, long i) {
                PlayerList.this.sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.LERP_SIZE), worldborder.world);
            }

            @Override
            public void a(WorldBorder worldborder, double d0, double d1) {
                PlayerList.this.sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_CENTER), worldborder.world);
            }

            @Override
            public void a(WorldBorder worldborder, int i) {
                PlayerList.this.sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_TIME), worldborder.world);
            }

            @Override
            public void b(WorldBorder worldborder, int i) {
                PlayerList.this.sendAll(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_BLOCKS), worldborder.world);
            }

            @Override
            public void b(WorldBorder worldborder, double d0) {}

            @Override
            public void c(WorldBorder worldborder, double d0) {}
        });
    }

    @Nullable
    public NBTTagCompound a(EntityPlayer entityplayer) {
        NBTTagCompound nbttagcompound = this.server.getSaveData().y();
        NBTTagCompound nbttagcompound1;

        if (entityplayer.getDisplayName().getString().equals(this.server.getSinglePlayerName()) && nbttagcompound != null) {
            nbttagcompound1 = nbttagcompound;
            entityplayer.load(nbttagcompound);
            PlayerList.LOGGER.debug("loading single player");
        } else {
            nbttagcompound1 = this.playerFileData.load(entityplayer);
        }

        return nbttagcompound1;
    }

    protected void savePlayerFile(EntityPlayer entityplayer) {
        if (!entityplayer.getBukkitEntity().isPersistent()) return; // CraftBukkit
        this.playerFileData.save(entityplayer);
        ServerStatisticManager serverstatisticmanager = (ServerStatisticManager) entityplayer.getStatisticManager(); // CraftBukkit

        if (serverstatisticmanager != null) {
            serverstatisticmanager.save();
        }

        AdvancementDataPlayer advancementdataplayer = (AdvancementDataPlayer) entityplayer.getAdvancementData(); // CraftBukkit

        if (advancementdataplayer != null) {
            advancementdataplayer.b();
        }

    }

    public String disconnect(EntityPlayer entityplayer) { // CraftBukkit - return string
        WorldServer worldserver = entityplayer.getWorldServer();

        entityplayer.a(StatisticList.LEAVE_GAME);

        // CraftBukkit start - Quitting must be before we do final save of data, in case plugins need to modify it
        // See SPIGOT-5799, SPIGOT-6145
        if (entityplayer.activeContainer != entityplayer.defaultContainer) {
            entityplayer.closeInventory();
        }

        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(cserver.getPlayer(entityplayer), "\u00A7e" + entityplayer.getName() + " left the game");
        cserver.getPluginManager().callEvent(playerQuitEvent);
        entityplayer.getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());

        entityplayer.playerTick(); // SPIGOT-924
        // CraftBukkit end

        this.savePlayerFile(entityplayer);
        if (entityplayer.isPassenger()) {
            Entity entity = entityplayer.getRootVehicle();

            if (entity.hasSinglePlayerPassenger()) {
                PlayerList.LOGGER.debug("Removing player mount");
                entityplayer.stopRiding();
                worldserver.removeEntity(entity);
                entity.dead = true;

                Entity entity1;

                for (Iterator iterator = entity.getAllPassengers().iterator(); iterator.hasNext(); entity1.dead = true) {
                    entity1 = (Entity) iterator.next();
                    worldserver.removeEntity(entity1);
                }

                worldserver.getChunkAt(entityplayer.chunkX, entityplayer.chunkZ).markDirty();
            }
        }

        entityplayer.decouple();
        worldserver.removePlayer(entityplayer);
        entityplayer.getAdvancementData().a();
        this.players.remove(entityplayer);
        this.playersByName.remove(entityplayer.getName().toLowerCase(java.util.Locale.ROOT)); // Spigot
        this.server.getBossBattleCustomData().b(entityplayer);
        UUID uuid = entityplayer.getUniqueID();
        EntityPlayer entityplayer1 = (EntityPlayer) this.j.get(uuid);

        if (entityplayer1 == entityplayer) {
            this.j.remove(uuid);
            // CraftBukkit start
            // this.o.remove(uuid);
            // this.p.remove(uuid);
            // CraftBukkit end
        }

        // CraftBukkit start
        // this.sendAll(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, new EntityPlayer[]{entityplayer}));
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityplayer);
        for (int i = 0; i < players.size(); i++) {
            EntityPlayer entityplayer2 = (EntityPlayer) this.players.get(i);

            if (entityplayer2.getBukkitEntity().canSee(entityplayer.getBukkitEntity())) {
                entityplayer2.playerConnection.sendPacket(packet);
            } else {
                entityplayer2.getBukkitEntity().removeDisconnectingPlayer(entityplayer.getBukkitEntity());
            }
        }
        // This removes the scoreboard (and player reference) for the specific player in the manager
        cserver.getScoreboardManager().removePlayer(entityplayer.getBukkitEntity());
        // CraftBukkit end

        return playerQuitEvent.getQuitMessage(); // CraftBukkit
    }

    // CraftBukkit start - Whole method, SocketAddress to LoginListener, added hostname to signature, return EntityPlayer
    public EntityPlayer attemptLogin(LoginListener loginlistener, GameProfile gameprofile, String hostname) {
        ChatMessage chatmessage;

        // Moved from processLogin
        UUID uuid = EntityHuman.a(gameprofile);
        List<EntityPlayer> list = Lists.newArrayList();

        EntityPlayer entityplayer;

        for (int i = 0; i < this.players.size(); ++i) {
            entityplayer = (EntityPlayer) this.players.get(i);
            if (entityplayer.getUniqueID().equals(uuid)) {
                list.add(entityplayer);
            }
        }

        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            entityplayer = (EntityPlayer) iterator.next();
            savePlayerFile(entityplayer); // CraftBukkit - Force the player's inventory to be saved
            entityplayer.playerConnection.disconnect(new ChatMessage("multiplayer.disconnect.duplicate_login", new Object[0]));
        }

        // Instead of kicking then returning, we need to store the kick reason
        // in the event, check with plugins to see if it's ok, and THEN kick
        // depending on the outcome.
        SocketAddress socketaddress = loginlistener.networkManager.getSocketAddress();

        EntityPlayer entity = new EntityPlayer(this.server, this.server.getWorldServer(World.OVERWORLD), gameprofile, new PlayerInteractManager(this.server.getWorldServer(World.OVERWORLD)));
        Player player = entity.getBukkitEntity();
        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((java.net.InetSocketAddress) socketaddress).getAddress(), ((java.net.InetSocketAddress) loginlistener.networkManager.getRawAddress()).getAddress());

        if (getProfileBans().isBanned(gameprofile) && !getProfileBans().get(gameprofile).hasExpired()) {
            GameProfileBanEntry gameprofilebanentry = (GameProfileBanEntry) this.k.get(gameprofile);

            chatmessage = new ChatMessage("multiplayer.disconnect.banned.reason", new Object[]{gameprofilebanentry.getReason()});
            if (gameprofilebanentry.getExpires() != null) {
                chatmessage.addSibling(new ChatMessage("multiplayer.disconnect.banned.expiration", new Object[]{PlayerList.g.format(gameprofilebanentry.getExpires())}));
            }

            // return chatmessage;
            if (!gameprofilebanentry.hasExpired()) event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage)); // Spigot
        } else if (!this.isWhitelisted(gameprofile)) {
            chatmessage = new ChatMessage("multiplayer.disconnect.not_whitelisted");
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, org.spigotmc.SpigotConfig.whitelistMessage); // Spigot
        } else if (getIPBans().isBanned(socketaddress) && !getIPBans().get(socketaddress).hasExpired()) {
            IpBanEntry ipbanentry = this.l.get(socketaddress);

            chatmessage = new ChatMessage("multiplayer.disconnect.banned_ip.reason", new Object[]{ipbanentry.getReason()});
            if (ipbanentry.getExpires() != null) {
                chatmessage.addSibling(new ChatMessage("multiplayer.disconnect.banned_ip.expiration", new Object[]{PlayerList.g.format(ipbanentry.getExpires())}));
            }

            // return chatmessage;
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else {
            // return this.players.size() >= this.maxPlayers && !this.f(gameprofile) ? new ChatMessage("multiplayer.disconnect.server_full") : null;
            if (this.players.size() >= this.maxPlayers && !this.f(gameprofile)) {
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

    public EntityPlayer processLogin(GameProfile gameprofile, EntityPlayer player) { // CraftBukkit - added EntityPlayer
        /* CraftBukkit startMoved up
        UUID uuid = EntityHuman.a(gameprofile);
        List<EntityPlayer> list = Lists.newArrayList();

        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            if (entityplayer.getUniqueID().equals(uuid)) {
                list.add(entityplayer);
            }
        }

        EntityPlayer entityplayer1 = (EntityPlayer) this.j.get(gameprofile.getId());

        if (entityplayer1 != null && !list.contains(entityplayer1)) {
            list.add(entityplayer1);
        }

        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer2 = (EntityPlayer) iterator.next();

            entityplayer2.playerConnection.disconnect(new ChatMessage("multiplayer.disconnect.duplicate_login"));
        }

        WorldServer worldserver = this.server.E();
        Object object;

        if (this.server.isDemoMode()) {
            object = new DemoPlayerInteractManager(worldserver);
        } else {
            object = new PlayerInteractManager(worldserver);
        }

        return new EntityPlayer(this.server, worldserver, gameprofile, (PlayerInteractManager) object);
        */
        return player;
        // CraftBukkit end
    }

    // CraftBukkit start
    public EntityPlayer moveToWorld(EntityPlayer entityplayer, boolean flag) {
        return this.moveToWorld(entityplayer, this.server.getWorldServer(entityplayer.getSpawnDimension()), flag, null, true);
    }

    public EntityPlayer moveToWorld(EntityPlayer entityplayer, WorldServer worldserver, boolean flag, Location location, boolean avoidSuffocation) {
        entityplayer.stopRiding(); // CraftBukkit
        this.players.remove(entityplayer);
        this.playersByName.remove(entityplayer.getName().toLowerCase(java.util.Locale.ROOT)); // Spigot
        entityplayer.getWorldServer().removePlayer(entityplayer);
        BlockPosition blockposition = entityplayer.getSpawn();
        float f = entityplayer.getSpawnAngle();
        boolean flag1 = entityplayer.isSpawnForced();
        /* CraftBukkit start
        WorldServer worldserver = this.server.getWorldServer(entityplayer.getSpawnDimension());
        Optional optional;

        if (worldserver != null && blockposition != null) {
            optional = EntityHuman.getBed(worldserver, blockposition, f, flag1, flag);
        } else {
            optional = Optional.empty();
        }

        WorldServer worldserver1 = worldserver != null && optional.isPresent() ? worldserver : this.server.E();
        Object object;

        if (this.server.isDemoMode()) {
            object = new DemoPlayerInteractManager(worldserver1);
        } else {
            object = new PlayerInteractManager(worldserver1);
        }

        EntityPlayer entityplayer1 = new EntityPlayer(this.server, worldserver1, entityplayer.getProfile(), (PlayerInteractManager) object);
        // */
        EntityPlayer entityplayer1 = entityplayer;
        org.bukkit.World fromWorld = entityplayer.getBukkitEntity().getWorld();
        entityplayer.viewingCredits = false;
        // CraftBukkit end

        entityplayer1.playerConnection = entityplayer.playerConnection;
        entityplayer1.copyFrom(entityplayer, flag);
        entityplayer1.e(entityplayer.getId());
        entityplayer1.a(entityplayer.getMainHand());
        Iterator iterator = entityplayer.getScoreboardTags().iterator();

        while (iterator.hasNext()) {
            String s = (String) iterator.next();

            entityplayer1.addScoreboardTag(s);
        }

        // this.a(entityplayer1, entityplayer, worldserver1); // CraftBukkit - removed
        boolean flag2 = false;

        // CraftBukkit start - fire PlayerRespawnEvent
        if (location == null) {
            boolean isBedSpawn = false;
            WorldServer worldserver1 = this.server.getWorldServer(entityplayer.getSpawnDimension());
            if (worldserver1 != null) {
                Optional optional;

                if (blockposition != null) {
                    optional = EntityHuman.getBed(worldserver1, blockposition, f, flag1, flag);
                } else {
                    optional = Optional.empty();
                }

                if (optional.isPresent()) {
                    IBlockData iblockdata = worldserver1.getType(blockposition);
                    boolean flag3 = iblockdata.a(Blocks.RESPAWN_ANCHOR);
                    Vec3D vec3d = (Vec3D) optional.get();
                    float f1;

                    if (!iblockdata.a((Tag) TagsBlock.BEDS) && !flag3) {
                        f1 = f;
                    } else {
                        Vec3D vec3d1 = Vec3D.c((BaseBlockPosition) blockposition).d(vec3d).d();

                        f1 = (float) MathHelper.g(MathHelper.d(vec3d1.z, vec3d1.x) * 57.2957763671875D - 90.0D);
                    }

                    entityplayer1.setRespawnPosition(worldserver1.getDimensionKey(), blockposition, f, flag1, false);
                    flag2 = !flag && flag3;
                    isBedSpawn = true;
                    location = new Location(worldserver1.getWorld(), vec3d.x, vec3d.y, vec3d.z, f1, 0.0F);
                } else if (blockposition != null) {
                    entityplayer1.playerConnection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.a, 0.0F));
                }
            }

            if (location == null) {
                worldserver1 = this.server.getWorldServer(World.OVERWORLD);
                blockposition = entityplayer1.getSpawnPoint(worldserver1);
                location = new Location(worldserver1.getWorld(), (double) ((float) blockposition.getX() + 0.5F), (double) ((float) blockposition.getY() + 0.1F), (double) ((float) blockposition.getZ() + 0.5F));
            }

            Player respawnPlayer = cserver.getPlayer(entityplayer1);
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn && !flag2, flag2);
            cserver.getPluginManager().callEvent(respawnEvent);
            // Spigot Start
            if (entityplayer.playerConnection.isDisconnected()) {
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

        while (avoidSuffocation && !worldserver1.getCubes(entityplayer1) && entityplayer1.locY() < 256.0D) {
            entityplayer1.setPosition(entityplayer1.locX(), entityplayer1.locY() + 1.0D, entityplayer1.locZ());
        }
        // CraftBukkit start
        WorldData worlddata = worldserver1.getWorldData();
        entityplayer1.playerConnection.sendPacket(new PacketPlayOutRespawn(worldserver1.getDimensionManager(), worldserver1.getDimensionKey(), BiomeManager.a(worldserver1.getSeed()), entityplayer1.playerInteractManager.getGameMode(), entityplayer1.playerInteractManager.c(), worldserver1.isDebugWorld(), worldserver1.isFlatWorld(), flag));
        entityplayer1.playerConnection.sendPacket(new PacketPlayOutViewDistance(worldserver1.spigotConfig.viewDistance)); // Spigot
        entityplayer1.spawnIn(worldserver1);
        entityplayer1.dead = false;
        entityplayer1.playerConnection.teleport(new Location(worldserver1.getWorld(), entityplayer1.locX(), entityplayer1.locY(), entityplayer1.locZ(), entityplayer1.yaw, entityplayer1.pitch));
        entityplayer1.setSneaking(false);

        // entityplayer1.playerConnection.a(entityplayer1.locX(), entityplayer1.locY(), entityplayer1.locZ(), entityplayer1.yaw, entityplayer1.pitch);
        entityplayer1.playerConnection.sendPacket(new PacketPlayOutSpawnPosition(worldserver1.getSpawn(), worldserver1.v()));
        entityplayer1.playerConnection.sendPacket(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
        entityplayer1.playerConnection.sendPacket(new PacketPlayOutExperience(entityplayer1.exp, entityplayer1.expTotal, entityplayer1.expLevel));
        this.a(entityplayer1, worldserver1);
        this.d(entityplayer1);
        if (!entityplayer.playerConnection.isDisconnected()) {
            worldserver1.addPlayerRespawn(entityplayer1);
            this.players.add(entityplayer1);
            this.playersByName.put(entityplayer1.getName().toLowerCase(java.util.Locale.ROOT), entityplayer1); // Spigot
            this.j.put(entityplayer1.getUniqueID(), entityplayer1);
        }
        // entityplayer1.syncInventory();
        entityplayer1.setHealth(entityplayer1.getHealth());
        if (flag2) {
            entityplayer1.playerConnection.sendPacket(new PacketPlayOutNamedSoundEffect(SoundEffects.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.BLOCKS, (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ(), 1.0F, 1.0F));
        }
        // Added from changeDimension
        updateClient(entityplayer); // Update health, etc...
        entityplayer.updateAbilities();
        for (Object o1 : entityplayer.getEffects()) {
            MobEffect mobEffect = (MobEffect) o1;
            entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEffect(entityplayer.getId(), mobEffect));
        }

        // Fire advancement trigger
        entityplayer.triggerDimensionAdvancements(((CraftWorld) fromWorld).getHandle());

        // Don't fire on respawn
        if (fromWorld != location.getWorld()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(entityplayer.getBukkitEntity(), fromWorld);
            server.server.getPluginManager().callEvent(event);
        }

        // Save player file again if they were disconnected
        if (entityplayer.playerConnection.isDisconnected()) {
            this.savePlayerFile(entityplayer);
        }
        // CraftBukkit end
        return entityplayer1;
    }

    public void d(EntityPlayer entityplayer) {
        GameProfile gameprofile = entityplayer.getProfile();
        int i = this.server.b(gameprofile);

        this.a(entityplayer, i);
    }

    public void tick() {
        if (++this.w > 600) {
            // CraftBukkit start
            for (int i = 0; i < this.players.size(); ++i) {
                final EntityPlayer target = (EntityPlayer) this.players.get(i);

                target.playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_LATENCY, Iterables.filter(this.players, new Predicate<EntityPlayer>() {
                    @Override
                    public boolean apply(EntityPlayer input) {
                        return target.getBukkitEntity().canSee(input.getBukkitEntity());
                    }
                })));
            }
            // CraftBukkit end
            this.w = 0;
        }

    }

    public void sendAll(Packet<?> packet) {
        for (int i = 0; i < this.players.size(); ++i) {
            ((EntityPlayer) this.players.get(i)).playerConnection.sendPacket(packet);
        }

    }

    // CraftBukkit start - add a world/entity limited version
    public void sendAll(Packet packet, EntityHuman entityhuman) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer =  this.players.get(i);
            if (entityhuman != null && entityhuman instanceof EntityPlayer && !entityplayer.getBukkitEntity().canSee(((EntityPlayer) entityhuman).getBukkitEntity())) {
                continue;
            }
            ((EntityPlayer) this.players.get(i)).playerConnection.sendPacket(packet);
        }
    }

    public void sendAll(Packet packet, World world) {
        for (int i = 0; i < world.getPlayers().size(); ++i) {
            ((EntityPlayer) world.getPlayers().get(i)).playerConnection.sendPacket(packet);
        }

    }
    // CraftBukkit end

    public void a(Packet<?> packet, ResourceKey<World> resourcekey) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            if (entityplayer.world.getDimensionKey() == resourcekey) {
                entityplayer.playerConnection.sendPacket(packet);
            }
        }

    }

    public void a(EntityHuman entityhuman, IChatBaseComponent ichatbasecomponent) {
        ScoreboardTeamBase scoreboardteambase = entityhuman.getScoreboardTeam();

        if (scoreboardteambase != null) {
            Collection<String> collection = scoreboardteambase.getPlayerNameSet();
            Iterator iterator = collection.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();
                EntityPlayer entityplayer = this.getPlayer(s);

                if (entityplayer != null && entityplayer != entityhuman) {
                    entityplayer.sendMessage(ichatbasecomponent, entityhuman.getUniqueID());
                }
            }

        }
    }

    public void b(EntityHuman entityhuman, IChatBaseComponent ichatbasecomponent) {
        ScoreboardTeamBase scoreboardteambase = entityhuman.getScoreboardTeam();

        if (scoreboardteambase == null) {
            this.sendMessage(ichatbasecomponent, ChatMessageType.SYSTEM, entityhuman.getUniqueID());
        } else {
            for (int i = 0; i < this.players.size(); ++i) {
                EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

                if (entityplayer.getScoreboardTeam() != scoreboardteambase) {
                    entityplayer.sendMessage(ichatbasecomponent, entityhuman.getUniqueID());
                }
            }

        }
    }

    public String[] e() {
        String[] astring = new String[this.players.size()];

        for (int i = 0; i < this.players.size(); ++i) {
            astring[i] = ((EntityPlayer) this.players.get(i)).getProfile().getName();
        }

        return astring;
    }

    public GameProfileBanList getProfileBans() {
        return this.k;
    }

    public IpBanList getIPBans() {
        return this.l;
    }

    public void addOp(GameProfile gameprofile) {
        this.operators.add(new OpListEntry(gameprofile, this.server.g(), this.operators.b(gameprofile)));
        EntityPlayer entityplayer = this.getPlayer(gameprofile.getId());

        if (entityplayer != null) {
            this.d(entityplayer);
        }

    }

    public void removeOp(GameProfile gameprofile) {
        this.operators.remove(gameprofile);
        EntityPlayer entityplayer = this.getPlayer(gameprofile.getId());

        if (entityplayer != null) {
            this.d(entityplayer);
        }

    }

    private void a(EntityPlayer entityplayer, int i) {
        if (entityplayer.playerConnection != null) {
            byte b0;

            if (i <= 0) {
                b0 = 24;
            } else if (i >= 4) {
                b0 = 28;
            } else {
                b0 = (byte) (24 + i);
            }

            entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityStatus(entityplayer, b0));
        }

        entityplayer.getBukkitEntity().recalculatePermissions(); // CraftBukkit
        this.server.getCommandDispatcher().a(entityplayer);
    }

    public boolean isWhitelisted(GameProfile gameprofile) {
        return !this.hasWhitelist || this.operators.d(gameprofile) || this.whitelist.d(gameprofile);
    }

    public boolean isOp(GameProfile gameprofile) {
        return this.operators.d(gameprofile) || this.server.a(gameprofile) && this.server.getSaveData().o() || this.v;
    }

    @Nullable
    public EntityPlayer getPlayer(String s) {
        return this.playersByName.get(s.toLowerCase(java.util.Locale.ROOT)); // Spigot
    }

    public void sendPacketNearby(@Nullable EntityHuman entityhuman, double d0, double d1, double d2, double d3, ResourceKey<World> resourcekey, Packet<?> packet) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            // CraftBukkit start - Test if player receiving packet can see the source of the packet
            if (entityhuman != null && entityhuman instanceof EntityPlayer && !entityplayer.getBukkitEntity().canSee(((EntityPlayer) entityhuman).getBukkitEntity())) {
               continue;
            }
            // CraftBukkit end

            if (entityplayer != entityhuman && entityplayer.world.getDimensionKey() == resourcekey) {
                double d4 = d0 - entityplayer.locX();
                double d5 = d1 - entityplayer.locY();
                double d6 = d2 - entityplayer.locZ();

                if (d4 * d4 + d5 * d5 + d6 * d6 < d3 * d3) {
                    entityplayer.playerConnection.sendPacket(packet);
                }
            }
        }

    }

    public void savePlayers() {
        for (int i = 0; i < this.players.size(); ++i) {
            this.savePlayerFile((EntityPlayer) this.players.get(i));
        }

    }

    public WhiteList getWhitelist() {
        return this.whitelist;
    }

    public String[] getWhitelisted() {
        return this.whitelist.getEntries();
    }

    public OpList getOPs() {
        return this.operators;
    }

    public String[] l() {
        return this.operators.getEntries();
    }

    public void reloadWhitelist() {}

    public void a(EntityPlayer entityplayer, WorldServer worldserver) {
        WorldBorder worldborder = entityplayer.world.getWorldBorder(); // CraftBukkit

        entityplayer.playerConnection.sendPacket(new PacketPlayOutWorldBorder(worldborder, PacketPlayOutWorldBorder.EnumWorldBorderAction.INITIALIZE));
        entityplayer.playerConnection.sendPacket(new PacketPlayOutUpdateTime(worldserver.getTime(), worldserver.getDayTime(), worldserver.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)));
        entityplayer.playerConnection.sendPacket(new PacketPlayOutSpawnPosition(worldserver.getSpawn(), worldserver.v()));
        if (worldserver.isRaining()) {
            // CraftBukkit start - handle player weather
            // entityplayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.b, 0.0F));
            // entityplayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.h, worldserver.d(1.0F)));
            // entityplayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.i, worldserver.b(1.0F)));
            entityplayer.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL, false);
            entityplayer.updateWeather(-worldserver.rainLevel, worldserver.rainLevel, -worldserver.thunderLevel, worldserver.thunderLevel);
            // CraftBukkit end
        }

    }

    public void updateClient(EntityPlayer entityplayer) {
        entityplayer.updateInventory(entityplayer.defaultContainer);
        // entityplayer.triggerHealthUpdate();
        entityplayer.getBukkitEntity().updateScaledHealth(); // CraftBukkit - Update scaled health on respawn and worldchange
        entityplayer.playerConnection.sendPacket(new PacketPlayOutHeldItemSlot(entityplayer.inventory.itemInHandIndex));
        // CraftBukkit start - from GameRules
        int i = entityplayer.world.getGameRules().getBoolean(GameRules.REDUCED_DEBUG_INFO) ? 22 : 23;
        entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityStatus(entityplayer, (byte) i));
        float immediateRespawn = entityplayer.world.getGameRules().getBoolean(GameRules.DO_IMMEDIATE_RESPAWN) ? 1.0F: 0.0F;
        entityplayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.l, immediateRespawn));
        // CraftBukkit end
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public boolean getHasWhitelist() {
        return this.hasWhitelist;
    }

    public void setHasWhitelist(boolean flag) {
        this.hasWhitelist = flag;
    }

    public List<EntityPlayer> b(String s) {
        List<EntityPlayer> list = Lists.newArrayList();
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();

            if (entityplayer.v().equals(s)) {
                list.add(entityplayer);
            }
        }

        return list;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public NBTTagCompound save() {
        return null;
    }

    private void a(EntityPlayer entityplayer, @Nullable EntityPlayer entityplayer1, WorldServer worldserver) {
        if (entityplayer1 != null) {
            entityplayer.playerInteractManager.a(entityplayer1.playerInteractManager.getGameMode(), entityplayer1.playerInteractManager.c());
        } else if (this.u != null) {
            entityplayer.playerInteractManager.a(this.u, EnumGamemode.NOT_SET);
        }

        entityplayer.playerInteractManager.b(worldserver.worldDataServer.getGameType()); // CraftBukkit
    }

    public void shutdown() {
        // CraftBukkit start - disconnect safely
        for (EntityPlayer player : this.players) {
            player.playerConnection.disconnect(this.server.server.getShutdownMessage()); // CraftBukkit - add custom shutdown message
        }
        // CraftBukkit end

    }

    // CraftBukkit start
    public void sendMessage(IChatBaseComponent[] iChatBaseComponents) {
        for (IChatBaseComponent component : iChatBaseComponents) {
            sendMessage(component, ChatMessageType.SYSTEM, SystemUtils.b);
        }
    }
    // CraftBukkit end

    public void sendMessage(IChatBaseComponent ichatbasecomponent, ChatMessageType chatmessagetype, UUID uuid) {
        this.server.sendMessage(ichatbasecomponent, uuid);
        // CraftBukkit start - we run this through our processor first so we can get web links etc
        this.sendAll(new PacketPlayOutChat(CraftChatMessage.fixComponent(ichatbasecomponent), chatmessagetype, uuid));
        // CraftBukkit end
    }

    public void sendMessage(IChatBaseComponent ichatbasecomponent) {
        this.sendMessage(ichatbasecomponent, ChatMessageType.SYSTEM, SystemUtils.b);
    }

    // CraftBukkit start
    public ServerStatisticManager getStatisticManager(EntityPlayer entityhuman) {
        ServerStatisticManager serverstatisticmanager = entityhuman.getStatisticManager();
        return serverstatisticmanager == null ? getStatisticManager(entityhuman.getUniqueID(), entityhuman.getDisplayName().getString()) : serverstatisticmanager;
    }

    public ServerStatisticManager getStatisticManager(UUID uuid, String displayName) {
        EntityPlayer entityhuman = this.getPlayer(uuid);
        ServerStatisticManager serverstatisticmanager = entityhuman == null ? null : (ServerStatisticManager) entityhuman.getStatisticManager();
        // CraftBukkit end

        if (serverstatisticmanager == null) {
            File file = this.server.a(SavedFile.STATS).toFile();
            File file1 = new File(file, uuid + ".json");

            if (!file1.exists()) {
                File file2 = new File(file, displayName + ".json"); // CraftBukkit

                if (file2.exists() && file2.isFile()) {
                    file2.renameTo(file1);
                }
            }

            serverstatisticmanager = new ServerStatisticManager(this.server, file1);
            // this.o.put(uuid, serverstatisticmanager); // CraftBukkit
        }

        return serverstatisticmanager;
    }

    public AdvancementDataPlayer f(EntityPlayer entityplayer) {
        UUID uuid = entityplayer.getUniqueID();
        AdvancementDataPlayer advancementdataplayer = (AdvancementDataPlayer) entityplayer.getAdvancementData(); // CraftBukkit

        if (advancementdataplayer == null) {
            File file = this.server.a(SavedFile.ADVANCEMENTS).toFile();
            File file1 = new File(file, uuid + ".json");

            advancementdataplayer = new AdvancementDataPlayer(this.server.getDataFixer(), this, this.server.getAdvancementData(), file1, entityplayer);
            // this.p.put(uuid, advancementdataplayer); // CraftBukkit
        }

        advancementdataplayer.a(entityplayer);
        return advancementdataplayer;
    }

    public void a(int i) {
        this.viewDistance = i;
        this.sendAll(new PacketPlayOutViewDistance(i));
        Iterator iterator = this.server.getWorlds().iterator();

        while (iterator.hasNext()) {
            WorldServer worldserver = (WorldServer) iterator.next();

            if (worldserver != null) {
                worldserver.getChunkProvider().setViewDistance(i);
            }
        }

    }

    public List<EntityPlayer> getPlayers() {
        return this.players;
    }

    @Nullable
    public EntityPlayer getPlayer(UUID uuid) {
        return (EntityPlayer) this.j.get(uuid);
    }

    public boolean f(GameProfile gameprofile) {
        return false;
    }

    public void reload() {
        // CraftBukkit start
        /*Iterator iterator = this.p.values().iterator();

        while (iterator.hasNext()) {
            AdvancementDataPlayer advancementdataplayer = (AdvancementDataPlayer) iterator.next();

            advancementdataplayer.a(this.server.getAdvancementData());
        }*/

        for (EntityPlayer player : players) {
            player.getAdvancementData().a(this.server.getAdvancementData());
            player.getAdvancementData().b(player); // CraftBukkit - trigger immediate flush of advancements
        }
        // CraftBukkit end

        this.sendAll(new PacketPlayOutTags(this.server.getTagRegistry()));
        PacketPlayOutRecipeUpdate packetplayoutrecipeupdate = new PacketPlayOutRecipeUpdate(this.server.getCraftingManager().b());
        Iterator iterator1 = this.players.iterator();

        while (iterator1.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator1.next();

            entityplayer.playerConnection.sendPacket(packetplayoutrecipeupdate);
            entityplayer.getRecipeBook().a(entityplayer);
        }

    }

    public boolean u() {
        return this.v;
    }
}
