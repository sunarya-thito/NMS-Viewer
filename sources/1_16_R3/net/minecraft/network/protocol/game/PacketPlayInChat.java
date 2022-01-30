package net.minecraft.network.protocol.game;

import java.io.IOException;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInChat implements Packet<PacketListenerPlayIn> {

    private String a;

    public PacketPlayInChat() {}

    public PacketPlayInChat(String s) {
        if (s.length() > 256) {
            s = s.substring(0, 256);
        }

        this.a = s;
    }

    @Override
    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = org.apache.commons.lang3.StringUtils.normalizeSpace(packetdataserializer.e(256)); // CraftBukkit - see PlayerConnection
    }

    @Override
    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.a(this.a);
    }

    // Spigot Start
    private static final java.util.concurrent.ExecutorService executors = java.util.concurrent.Executors.newCachedThreadPool(
            new com.google.common.util.concurrent.ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "Async Chat Thread - #%d" ).build() );
    public void a(final PacketListenerPlayIn packetlistenerplayin) {
        if ( !a.startsWith("/") )
        {
            executors.submit( new Runnable()
            {

                @Override
                public void run()
                {
                    packetlistenerplayin.a( PacketPlayInChat.this );
                }
            } );
            return;
        }
        // Spigot End
        packetlistenerplayin.a(this);
    }

    public String b() {
        return this.a;
    }
}
