package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInChat implements Packet<PacketListenerPlayIn> {

    private static final int MAX_MESSAGE_LENGTH = 256;
    private final String message;

    public PacketPlayInChat(String s) {
        if (s.length() > 256) {
            s = s.substring(0, 256);
        }

        this.message = s;
    }

    public PacketPlayInChat(PacketDataSerializer packetdataserializer) {
        this.message = org.apache.commons.lang3.StringUtils.normalizeSpace(packetdataserializer.readUtf(256)); // CraftBukkit - see PlayerConnection
    }

    @Override
    public void write(PacketDataSerializer packetdataserializer) {
        packetdataserializer.writeUtf(this.message);
    }

    // Spigot Start
    private static final java.util.concurrent.ExecutorService executors = java.util.concurrent.Executors.newCachedThreadPool(
            new com.google.common.util.concurrent.ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "Async Chat Thread - #%d" ).build() );
    public void handle(final PacketListenerPlayIn packetlistenerplayin) {
        if ( !message.startsWith("/") )
        {
            executors.submit( new Runnable()
            {

                @Override
                public void run()
                {
                    packetlistenerplayin.handleChat( PacketPlayInChat.this );
                }
            } );
            return;
        }
        // Spigot End
        packetlistenerplayin.handleChat(this);
    }

    public String getMessage() {
        return this.message;
    }
}
