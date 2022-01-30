package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutKickDisconnect;
import net.minecraft.network.protocol.login.PacketLoginInCustomPayload;
import net.minecraft.network.protocol.login.PacketLoginInEncryptionBegin;
import net.minecraft.network.protocol.login.PacketLoginInListener;
import net.minecraft.network.protocol.login.PacketLoginInStart;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.network.protocol.login.PacketLoginOutEncryptionBegin;
import net.minecraft.network.protocol.login.PacketLoginOutSetCompression;
import net.minecraft.network.protocol.login.PacketLoginOutSuccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import net.minecraft.world.entity.player.EntityHuman;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.network.chat.ChatComponentText;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
// CraftBukkit end

public class LoginListener implements PacketLoginInListener {

    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_TICKS_BEFORE_LOGIN = 600;
    private static final Random RANDOM = new Random();
    private final byte[] nonce = new byte[4];
    final MinecraftServer server;
    public final NetworkManager connection;
    LoginListener.EnumProtocolState state;
    private int tick;
    @Nullable
    GameProfile gameProfile;
    private final String serverId;
    @Nullable
    private EntityPlayer delayedAcceptPlayer;
    public String hostname = ""; // CraftBukkit - add field

    public LoginListener(MinecraftServer minecraftserver, NetworkManager networkmanager) {
        this.state = LoginListener.EnumProtocolState.HELLO;
        this.serverId = "";
        this.server = minecraftserver;
        this.connection = networkmanager;
        LoginListener.RANDOM.nextBytes(this.nonce);
    }

    public void tick() {
        if (this.state == LoginListener.EnumProtocolState.READY_TO_ACCEPT) {
            this.handleAcceptedLogin();
        } else if (this.state == LoginListener.EnumProtocolState.DELAY_ACCEPT) {
            EntityPlayer entityplayer = this.server.getPlayerList().getPlayer(this.gameProfile.getId());

            if (entityplayer == null) {
                this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                this.placeNewPlayer(this.delayedAcceptPlayer);
                this.delayedAcceptPlayer = null;
            }
        }

        if (this.tick++ == 600) {
            this.disconnect(new ChatMessage("multiplayer.disconnect.slow_login"));
        }

    }

    // CraftBukkit start
    @Deprecated
    public void disconnect(String s) {
        try {
            IChatBaseComponent ichatbasecomponent = new ChatComponentText(s);
            LoginListener.LOGGER.info("Disconnecting {}: {}", this.getUserName(), s);
            this.connection.send(new PacketLoginOutDisconnect(ichatbasecomponent));
            this.connection.disconnect(ichatbasecomponent);
        } catch (Exception exception) {
            LoginListener.LOGGER.error("Error whilst disconnecting player", exception);
        }
    }
    // CraftBukkit end

    @Override
    public NetworkManager getConnection() {
        return this.connection;
    }

    public void disconnect(IChatBaseComponent ichatbasecomponent) {
        try {
            LoginListener.LOGGER.info("Disconnecting {}: {}", this.getUserName(), ichatbasecomponent.getString());
            this.connection.send(new PacketLoginOutDisconnect(ichatbasecomponent));
            this.connection.disconnect(ichatbasecomponent);
        } catch (Exception exception) {
            LoginListener.LOGGER.error("Error whilst disconnecting player", exception);
        }

    }

    // Spigot start
    public void initUUID()
    {
        UUID uuid;
        if ( connection.spoofedUUID != null )
        {
            uuid = connection.spoofedUUID;
        } else
        {
            uuid = EntityHuman.createPlayerUUID( this.gameProfile.getName() );
        }

        this.gameProfile = new GameProfile( uuid, this.gameProfile.getName() );

        if (connection.spoofedProfile != null)
        {
            for ( com.mojang.authlib.properties.Property property : connection.spoofedProfile )
            {
                if ( !HandshakeListener.PROP_PATTERN.matcher( property.getName() ).matches() ) continue;
                this.gameProfile.getProperties().put( property.getName(), property );
            }
        }
    }
    // Spigot end

    public void handleAcceptedLogin() {
        // Spigot start - Moved to initUUID
        /*
        if (!this.gameProfile.isComplete()) {
            this.gameProfile = this.createFakeProfile(this.gameProfile);
        }
        */
        // Spigot end

        // CraftBukkit start - fire PlayerLoginEvent
        EntityPlayer s = this.server.getPlayerList().canPlayerLogin(this, this.gameProfile, hostname);

        if (s == null) {
            // this.disconnect(ichatbasecomponent);
            // CraftBukkit end
        } else {
            this.state = LoginListener.EnumProtocolState.ACCEPTED;
            if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
                this.connection.send(new PacketLoginOutSetCompression(this.server.getCompressionThreshold()), (channelfuture) -> {
                    this.connection.setupCompression(this.server.getCompressionThreshold(), true);
                });
            }

            this.connection.send(new PacketLoginOutSuccess(this.gameProfile));
            EntityPlayer entityplayer = this.server.getPlayerList().getPlayer(this.gameProfile.getId());

            try {
                EntityPlayer entityplayer1 = this.server.getPlayerList().getPlayerForLogin(this.gameProfile, s); // CraftBukkit - add player reference

                if (entityplayer != null) {
                    this.state = LoginListener.EnumProtocolState.DELAY_ACCEPT;
                    this.delayedAcceptPlayer = entityplayer1;
                } else {
                    this.placeNewPlayer(entityplayer1);
                }
            } catch (Exception exception) {
                LoginListener.LOGGER.error("Couldn't place player in world", exception);
                ChatMessage chatmessage = new ChatMessage("multiplayer.disconnect.invalid_player_data");

                this.connection.send(new PacketPlayOutKickDisconnect(chatmessage));
                this.connection.disconnect(chatmessage);
            }
        }

    }

    private void placeNewPlayer(EntityPlayer entityplayer) {
        this.server.getPlayerList().placeNewPlayer(this.connection, entityplayer);
    }

    @Override
    public void onDisconnect(IChatBaseComponent ichatbasecomponent) {
        LoginListener.LOGGER.info("{} lost connection: {}", this.getUserName(), ichatbasecomponent.getString());
    }

    public String getUserName() {
        return this.gameProfile != null ? this.gameProfile + " (" + this.connection.getRemoteAddress() + ")" : String.valueOf(this.connection.getRemoteAddress());
    }

    @Override
    public void handleHello(PacketLoginInStart packetlogininstart) {
        Validate.validState(this.state == LoginListener.EnumProtocolState.HELLO, "Unexpected hello packet", new Object[0]);
        this.gameProfile = packetlogininstart.getGameProfile();
        if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
            this.state = LoginListener.EnumProtocolState.KEY;
            this.connection.send(new PacketLoginOutEncryptionBegin("", this.server.getKeyPair().getPublic().getEncoded(), this.nonce));
        } else {
            // Spigot start
            new Thread("User Authenticator #" + LoginListener.UNIQUE_THREAD_ID.incrementAndGet()) {

                @Override
                public void run() {
                    try {
                        initUUID();
                        new LoginHandler().fireEvents();
                    } catch (Exception ex) {
                        disconnect("Failed to verify username!");
                        server.server.getLogger().log(java.util.logging.Level.WARNING, "Exception verifying " + gameProfile.getName(), ex);
                    }
                }
            }.start();
            // Spigot end
        }

    }

    @Override
    public void handleKey(PacketLoginInEncryptionBegin packetlogininencryptionbegin) {
        Validate.validState(this.state == LoginListener.EnumProtocolState.KEY, "Unexpected key packet", new Object[0]);
        PrivateKey privatekey = this.server.getKeyPair().getPrivate();

        final String s;

        try {
            if (!Arrays.equals(this.nonce, packetlogininencryptionbegin.getNonce(privatekey))) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretkey = packetlogininencryptionbegin.getSecretKey(privatekey);
            Cipher cipher = MinecraftEncryption.getCipher(2, secretkey);
            Cipher cipher1 = MinecraftEncryption.getCipher(1, secretkey);

            s = (new BigInteger(MinecraftEncryption.digestData("", this.server.getKeyPair().getPublic(), secretkey))).toString(16);
            this.state = LoginListener.EnumProtocolState.AUTHENTICATING;
            this.connection.setEncryptionKey(cipher, cipher1);
        } catch (CryptographyException cryptographyexception) {
            throw new IllegalStateException("Protocol error", cryptographyexception);
        }

        Thread thread = new Thread("User Authenticator #" + LoginListener.UNIQUE_THREAD_ID.incrementAndGet()) {
            public void run() {
                GameProfile gameprofile = LoginListener.this.gameProfile;

                try {
                    LoginListener.this.gameProfile = LoginListener.this.server.getSessionService().hasJoinedServer(new GameProfile((UUID) null, gameprofile.getName()), s, this.getAddress());
                    if (LoginListener.this.gameProfile != null) {
                        // CraftBukkit start - fire PlayerPreLoginEvent
                        if (!connection.isConnected()) {
                            return;
                        }

                        new LoginHandler().fireEvents();
                    } else if (LoginListener.this.server.isSingleplayer()) {
                        LoginListener.LOGGER.warn("Failed to verify username but will let them in anyway!");
                        LoginListener.this.gameProfile = LoginListener.this.createFakeProfile(gameprofile);
                        LoginListener.this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                    } else {
                        LoginListener.this.disconnect(new ChatMessage("multiplayer.disconnect.unverified_username"));
                        LoginListener.LOGGER.error("Username '{}' tried to join with an invalid session", gameprofile.getName());
                    }
                } catch (AuthenticationUnavailableException authenticationunavailableexception) {
                    if (LoginListener.this.server.isSingleplayer()) {
                        LoginListener.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        LoginListener.this.gameProfile = LoginListener.this.createFakeProfile(gameprofile);
                        LoginListener.this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                    } else {
                        LoginListener.this.disconnect(new ChatMessage("multiplayer.disconnect.authservers_down"));
                        LoginListener.LOGGER.error("Couldn't verify username because servers are unavailable");
                    }
                    // CraftBukkit start - catch all exceptions
                } catch (Exception exception) {
                    disconnect("Failed to verify username!");
                    server.server.getLogger().log(java.util.logging.Level.WARNING, "Exception verifying " + gameprofile.getName(), exception);
                    // CraftBukkit end
                }

            }

            @Nullable
            private InetAddress getAddress() {
                SocketAddress socketaddress = LoginListener.this.connection.getRemoteAddress();

                return LoginListener.this.server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress ? ((InetSocketAddress) socketaddress).getAddress() : null;
            }
        };

        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LoginListener.LOGGER));
        thread.start();
    }

    // Spigot start
    public class LoginHandler {

        public void fireEvents() throws Exception {
                        String playerName = gameProfile.getName();
                        java.net.InetAddress address = ((java.net.InetSocketAddress) connection.getRemoteAddress()).getAddress();
                        java.util.UUID uniqueId = gameProfile.getId();
                        final org.bukkit.craftbukkit.CraftServer server = LoginListener.this.server.server;

                        AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(playerName, address, uniqueId);
                        server.getPluginManager().callEvent(asyncEvent);

                        if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0) {
                            final PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName, address, uniqueId);
                            if (asyncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED) {
                                event.disallow(asyncEvent.getResult(), asyncEvent.getKickMessage());
                            }
                            Waitable<PlayerPreLoginEvent.Result> waitable = new Waitable<PlayerPreLoginEvent.Result>() {
                                @Override
                                protected PlayerPreLoginEvent.Result evaluate() {
                                    server.getPluginManager().callEvent(event);
                                    return event.getResult();
                                }};

                            LoginListener.this.server.processQueue.add(waitable);
                            if (waitable.get() != PlayerPreLoginEvent.Result.ALLOWED) {
                                disconnect(event.getKickMessage());
                                return;
                            }
                        } else {
                            if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                                disconnect(asyncEvent.getKickMessage());
                                return;
                            }
                        }
                        // CraftBukkit end
                        LoginListener.LOGGER.info("UUID of player {} is {}", LoginListener.this.gameProfile.getName(), LoginListener.this.gameProfile.getId());
                        LoginListener.this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
        }
    }
    // Spigot end

    public void handleCustomQueryPacket(PacketLoginInCustomPayload packetloginincustompayload) {
        this.disconnect(new ChatMessage("multiplayer.disconnect.unexpected_query_response"));
    }

    protected GameProfile createFakeProfile(GameProfile gameprofile) {
        UUID uuid = EntityHuman.createPlayerUUID(gameprofile.getName());

        return new GameProfile(uuid, gameprofile.getName());
    }

    private static enum EnumProtocolState {

        HELLO, KEY, AUTHENTICATING, NEGOTIATING, READY_TO_ACCEPT, DELAY_ACCEPT, ACCEPTED;

        private EnumProtocolState() {}
    }
}
