package net.minecraft.server.network;

import net.minecraft.SharedConstants;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.handshake.PacketHandshakingInListener;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.server.MinecraftServer;

// CraftBukkit start
import java.net.InetAddress;
import java.util.HashMap;
// CraftBukkit end

public class HandshakeListener implements PacketHandshakingInListener {

    // Spigot start
    private static final com.google.gson.Gson gson = new com.google.gson.Gson();
    static final java.util.regex.Pattern HOST_PATTERN = java.util.regex.Pattern.compile("[0-9a-f\\.:]{0,45}");
    static final java.util.regex.Pattern PROP_PATTERN = java.util.regex.Pattern.compile("\\w{0,16}");
    // Spigot end
    // CraftBukkit start - add fields
    private static final HashMap<InetAddress, Long> throttleTracker = new HashMap<InetAddress, Long>();
    private static int throttleCounter = 0;
    // CraftBukkit end
    private static final IChatBaseComponent IGNORE_STATUS_REASON = new ChatComponentText("Ignoring status request");
    private final MinecraftServer server;
    private final NetworkManager connection;

    public HandshakeListener(MinecraftServer minecraftserver, NetworkManager networkmanager) {
        this.server = minecraftserver;
        this.connection = networkmanager;
    }

    @Override
    public void a(PacketHandshakingInSetProtocol packethandshakinginsetprotocol) {
        switch (packethandshakinginsetprotocol.b()) {
            case LOGIN:
                this.connection.setProtocol(EnumProtocol.LOGIN);
                // CraftBukkit start - Connection throttle
                try {
                    long currentTime = System.currentTimeMillis();
                    long connectionThrottle = this.server.server.getConnectionThrottle();
                    InetAddress address = ((java.net.InetSocketAddress) this.connection.getSocketAddress()).getAddress();

                    synchronized (throttleTracker) {
                        if (throttleTracker.containsKey(address) && !"127.0.0.1".equals(address.getHostAddress()) && currentTime - throttleTracker.get(address) < connectionThrottle) {
                            throttleTracker.put(address, currentTime);
                            ChatMessage chatmessage = new ChatMessage("Connection throttled! Please wait before reconnecting.");
                            this.connection.sendPacket(new PacketLoginOutDisconnect(chatmessage));
                            this.connection.close(chatmessage);
                            return;
                        }

                        throttleTracker.put(address, currentTime);
                        throttleCounter++;
                        if (throttleCounter > 200) {
                            throttleCounter = 0;

                            // Cleanup stale entries
                            java.util.Iterator iter = throttleTracker.entrySet().iterator();
                            while (iter.hasNext()) {
                                java.util.Map.Entry<InetAddress, Long> entry = (java.util.Map.Entry) iter.next();
                                if (entry.getValue() > connectionThrottle) {
                                    iter.remove();
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    org.apache.logging.log4j.LogManager.getLogger().debug("Failed to check connection throttle", t);
                }
                // CraftBukkit end
                if (packethandshakinginsetprotocol.c() != SharedConstants.getGameVersion().getProtocolVersion()) {
                    ChatMessage chatmessage;

                    if (packethandshakinginsetprotocol.c() < 754) {
                        chatmessage = new ChatMessage( java.text.MessageFormat.format( org.spigotmc.SpigotConfig.outdatedClientMessage.replaceAll("'", "''"), SharedConstants.getGameVersion().getName() ) ); // Spigot
                    } else {
                        chatmessage = new ChatMessage( java.text.MessageFormat.format( org.spigotmc.SpigotConfig.outdatedServerMessage.replaceAll("'", "''"), SharedConstants.getGameVersion().getName() ) ); // Spigot
                    }

                    this.connection.sendPacket(new PacketLoginOutDisconnect(chatmessage));
                    this.connection.close(chatmessage);
                } else {
                    this.connection.setPacketListener(new LoginListener(this.server, this.connection));
                    // Spigot Start
                    if (org.spigotmc.SpigotConfig.bungee) {
                        String[] split = packethandshakinginsetprotocol.hostName.split("\00");
                        if ( ( split.length == 3 || split.length == 4 ) && ( HOST_PATTERN.matcher( split[1] ).matches() ) ) {
                            packethandshakinginsetprotocol.hostName = split[0];
                            connection.address = new java.net.InetSocketAddress(split[1], ((java.net.InetSocketAddress) connection.getSocketAddress()).getPort());
                            connection.spoofedUUID = com.mojang.util.UUIDTypeAdapter.fromString( split[2] );
                        } else
                        {
                            ChatMessage chatmessage = new ChatMessage("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
                            this.connection.sendPacket(new PacketLoginOutDisconnect(chatmessage));
                            this.connection.close(chatmessage);
                            return;
                        }
                        if ( split.length == 4 )
                        {
                            connection.spoofedProfile = gson.fromJson(split[3], com.mojang.authlib.properties.Property[].class);
                        }
                    }
                    // Spigot End
                    ((LoginListener) this.connection.j()).hostname = packethandshakinginsetprotocol.hostName + ":" + packethandshakinginsetprotocol.port; // CraftBukkit - set hostname
                }
                break;
            case STATUS:
                if (this.server.ak()) {
                    this.connection.setProtocol(EnumProtocol.STATUS);
                    this.connection.setPacketListener(new PacketStatusListener(this.server, this.connection));
                } else {
                    this.connection.close(HandshakeListener.IGNORE_STATUS_REASON);
                }
                break;
            default:
                throw new UnsupportedOperationException("Invalid intention " + packethandshakinginsetprotocol.b());
        }

    }

    @Override
    public void a(IChatBaseComponent ichatbasecomponent) {}

    @Override
    public NetworkManager a() {
        return this.connection;
    }
}
