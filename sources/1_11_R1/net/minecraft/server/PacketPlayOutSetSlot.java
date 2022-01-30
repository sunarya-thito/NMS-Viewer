package net.minecraft.server;

import java.io.IOException;

public class PacketPlayOutSetSlot implements Packet<PacketListenerPlayOut> {

    private int a;
    private int b;
    private ItemStack c;

    public PacketPlayOutSetSlot() {
        this.c = ItemStack.a;
    }

    public PacketPlayOutSetSlot(int i, int j, ItemStack itemstack) {
        this.c = ItemStack.a;
        this.a = i;
        this.b = j;
        this.c = itemstack.isEmpty() ? ItemStack.a : itemstack.cloneItemStack();
    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.readByte();
        this.b = packetdataserializer.readShort();
        this.c = packetdataserializer.k();
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.writeByte(this.a);
        packetdataserializer.writeShort(this.b);
        packetdataserializer.a(this.c);
    }
}
