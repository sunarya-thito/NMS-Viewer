package net.minecraft.server.network;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.status.PacketStatusInListener;
import net.minecraft.network.protocol.status.PacketStatusInPing;
import net.minecraft.network.protocol.status.PacketStatusInStart;
import net.minecraft.network.protocol.status.PacketStatusOutPong;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import net.minecraft.server.MinecraftServer;

// CraftBukkit start
import com.mojang.authlib.GameProfile;
import java.net.InetSocketAddress;
import java.util.Iterator;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.status.ServerPing;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftIconCache;
import org.bukkit.entity.Player;
// CraftBukkit end

public class PacketStatusListener implements PacketStatusInListener {

    private static final IChatBaseComponent DISCONNECT_REASON = new ChatMessage("multiplayer.status.request_handled");
    private final MinecraftServer server;
    private final NetworkManager connection;
    private boolean hasRequestedStatus;

    public PacketStatusListener(MinecraftServer minecraftserver, NetworkManager networkmanager) {
        this.server = minecraftserver;
        this.connection = networkmanager;
    }

    @Override
    public void onDisconnect(IChatBaseComponent ichatbasecomponent) {}

    @Override
    public NetworkManager getConnection() {
        return this.connection;
    }

    @Override
    public void handleStatusRequest(PacketStatusInStart packetstatusinstart) {
        if (this.hasRequestedStatus) {
            this.connection.disconnect(PacketStatusListener.DISCONNECT_REASON);
        } else {
            this.hasRequestedStatus = true;
            // CraftBukkit start
            // this.connection.send(new PacketStatusOutServerInfo(this.server.getStatus()));
            final Object[] players = server.getPlayerList().players.toArray();
            class ServerListPingEvent extends org.bukkit.event.server.ServerListPingEvent {

                CraftIconCache icon = server.server.getServerIcon();

                ServerListPingEvent() {
                    super(((InetSocketAddress) connection.getRemoteAddress()).getAddress(), server.getMotd(), server.getPlayerList().getMaxPlayers());
                }

                @Override
                public void setServerIcon(org.bukkit.util.CachedServerIcon icon) {
                    if (!(icon instanceof CraftIconCache)) {
                        throw new IllegalArgumentException(icon + " was not created by " + org.bukkit.craftbukkit.CraftServer.class);
                    }
                    this.icon = (CraftIconCache) icon;
                }

                @Override
                public Iterator<Player> iterator() throws UnsupportedOperationException {
                    return new Iterator<Player>() {
                        int i;
                        int ret = Integer.MIN_VALUE;
                        EntityPlayer player;

                        @Override
                        public boolean hasNext() {
                            if (player != null) {
                                return true;
                            }
                            final Object[] currentPlayers = players;
                            for (int length = currentPlayers.length, i = this.i; i < length; i++) {
                                final EntityPlayer player = (EntityPlayer) currentPlayers[i];
                                if (player != null) {
                                    this.i = i + 1;
                                    this.player = player;
                                    return true;
                                }
                            }
                            return false;
                        }

                        @Override
                        public Player next() {
                            if (!hasNext()) {
                                throw new java.util.NoSuchElementException();
                            }
                            final EntityPlayer player = this.player;
                            this.player = null;
                            this.ret = this.i - 1;
                            return player.getBukkitEntity();
                        }

                        @Override
                        public void remove() {
                            final Object[] currentPlayers = players;
                            final int i = this.ret;
                            if (i < 0 || currentPlayers[i] == null) {
                                throw new IllegalStateException();
                            }
                            currentPlayers[i] = null;
                        }
                    };
                }
            }

            ServerListPingEvent event = new ServerListPingEvent();
            this.server.server.getPluginManager().callEvent(event);

            java.util.List<GameProfile> profiles = new java.util.ArrayList<GameProfile>(players.length);
            for (Object player : players) {
                if (player != null) {
                    EntityPlayer entityPlayer = ((EntityPlayer) player);
                    if (entityPlayer.allowsListing()) {
                        profiles.add(entityPlayer.getGameProfile());
                    } else {
                        profiles.add(MinecraftServer.ANONYMOUS_PLAYER_PROFILE);
                    }
                }
            }

            ServerPing.ServerPingPlayerSample playerSample = new ServerPing.ServerPingPlayerSample(event.getMaxPlayers(), profiles.size());
            if (!this.server.hidesOnlinePlayers()) {
                // Spigot Start
                if ( !profiles.isEmpty() )
                {
                    java.util.Collections.shuffle( profiles ); // This sucks, its inefficient but we have no simple way of doing it differently
                    profiles = profiles.subList( 0, Math.min( profiles.size(), org.spigotmc.SpigotConfig.playerSample ) ); // Cap the sample to n (or less) displayed players, ie: Vanilla behaviour
                }
                // Spigot End
                playerSample.setSample(profiles.toArray(new GameProfile[profiles.size()]));
            }

            ServerPing ping = new ServerPing();
            ping.setFavicon(event.icon.value);
            ping.setDescription(CraftChatMessage.fromString(event.getMotd(), true)[0]);
            ping.setPlayers(playerSample);
            int version = SharedConstants.getCurrentVersion().getProtocolVersion();
            ping.setVersion(new ServerPing.ServerData(server.getServerModName() + " " + server.getServerVersion(), version));

            this.connection.send(new PacketStatusOutServerInfo(ping));
            // CraftBukkit end
        }
    }

    @Override
    public void handlePingRequest(PacketStatusInPing packetstatusinping) {
        this.connection.send(new PacketStatusOutPong(packetstatusinping.getTime()));
        this.connection.disconnect(PacketStatusListener.DISCONNECT_REASON);
    }
}
