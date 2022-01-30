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
        this.message = packetdataserializer.readComponent();
        this.type = ChatMessageType.getForIndex(packetdataserializer.readByte());
        this.sender = packetdataserializer.readUUID();
    }

    @Override
    public void write(PacketDataSerializer packetdataserializer) {
        // Spigot start
        if (components != null) {
            packetdataserializer.writeUtf(net.md_5.bungee.chat.ComponentSerializer.toString(components));
        } else {
            packetdataserializer.writeComponent(this.message);
        }
        // Spigot end
        packetdataserializer.writeByte(this.type.getIndex());
        packetdataserializer.writeUUID(this.sender);
    }

    public void handle(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.handleChat(this);
    }

    public IChatBaseComponent getMessage() {
        return this.message;
    }

    public ChatMessageType getType() {
        return this.type;
    }

    public UUID getSender() {
        return this.sender;
    }

    @Override
    public boolean isSkippable() {
        return true;
    }
}
