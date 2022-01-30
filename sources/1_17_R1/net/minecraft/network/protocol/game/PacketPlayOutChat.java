// mc-dev import
package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutChat implements Packet<PacketListenerPlayOut> {

    private final IChatBaseComponent message;
    public net.md_5.bungee.api.chat.BaseComponent[] components; // Spigot
    private final ChatMessageType type;
    private final UUID sender;

    public PacketPlayOutChat(IChatBaseComponent ichatbasecomponent, ChatMessageType chatmessagetype, UUID uuid) {
        this.message = ichatbasecomponent;
        this.type = chatmessagetype;
        this.sender = uuid;
    }

    public PacketPlayOutChat(PacketDataSerializer packetdataserializer) {
        this.message = packetdataserializer.i();
        this.type = ChatMessageType.a(packetdataserializer.readByte());
        this.sender = packetdataserializer.l();
    }

    @Override
    public void a(PacketDataSerializer packetdataserializer) {
        // Spigot start
        if (components != null) {
            packetdataserializer.a(net.md_5.bungee.chat.ComponentSerializer.toString(components));
        } else {
            packetdataserializer.a(this.message);
        }
        // Spigot end
        packetdataserializer.writeByte(this.type.a());
        packetdataserializer.a(this.sender);
    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

    public IChatBaseComponent b() {
        return this.message;
    }

    public ChatMessageType c() {
        return this.type;
    }

    public UUID d() {
        return this.sender;
    }

    @Override
    public boolean a() {
        return true;
    }
}
