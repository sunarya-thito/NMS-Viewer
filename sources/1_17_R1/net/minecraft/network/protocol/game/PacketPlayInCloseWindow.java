package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayInCloseWindow implements Packet<PacketListenerPlayIn> {

    private final int containerId;

    public PacketPlayInCloseWindow(int i) {
        this.containerId = i;
    }

    public void a(PacketListenerPlayIn packetlistenerplayin) {
        packetlistenerplayin.a(this);
    }

    public PacketPlayInCloseWindow(PacketDataSerializer packetdataserializer) {
        this.containerId = packetdataserializer.readByte();
    }

    @Override
    public void a(PacketDataSerializer packetdataserializer) {
        packetdataserializer.writeByte(this.containerId);
    }

    public int b() {
        return this.containerId;
    }
}
