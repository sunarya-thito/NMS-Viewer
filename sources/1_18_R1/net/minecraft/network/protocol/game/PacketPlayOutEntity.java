package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public abstract class PacketPlayOutEntity implements Packet<PacketListenerPlayOut> {

    private static final double TRUNCATION_STEPS = 4096.0D;
    protected final int entityId;
    protected final short xa;
    protected final short ya;
    protected final short za;
    protected final byte yRot;
    protected final byte xRot;
    protected final boolean onGround;
    protected final boolean hasRot;
    protected final boolean hasPos;

    public static long entityToPacket(double d0) {
        return MathHelper.lfloor(d0 * 4096.0D);
    }

    public static double packetToEntity(long i) {
        return (double) i / 4096.0D;
    }

    public Vec3D updateEntityPosition(Vec3D vec3d) {
        double d0 = this.xa == 0 ? vec3d.x : packetToEntity(entityToPacket(vec3d.x) + (long) this.xa);
        double d1 = this.ya == 0 ? vec3d.y : packetToEntity(entityToPacket(vec3d.y) + (long) this.ya);
        double d2 = this.za == 0 ? vec3d.z : packetToEntity(entityToPacket(vec3d.z) + (long) this.za);

        return new Vec3D(d0, d1, d2);
    }

    public static Vec3D packetToEntity(long i, long j, long k) {
        return (new Vec3D((double) i, (double) j, (double) k)).scale(2.44140625E-4D);
    }

    protected PacketPlayOutEntity(int i, short short0, short short1, short short2, byte b0, byte b1, boolean flag, boolean flag1, boolean flag2) {
        this.entityId = i;
        this.xa = short0;
        this.ya = short1;
        this.za = short2;
        this.yRot = b0;
        this.xRot = b1;
        this.onGround = flag;
        this.hasRot = flag1;
        this.hasPos = flag2;
    }

    public void handle(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.handleMoveEntity(this);
    }

    public String toString() {
        return "Entity_" + super.toString();
    }

    @Nullable
    public Entity getEntity(World world) {
        return world.getEntity(this.entityId);
    }

    public short getXa() {
        return this.xa;
    }

    public short getYa() {
        return this.ya;
    }

    public short getZa() {
        return this.za;
    }

    public byte getyRot() {
        return this.yRot;
    }

    public byte getxRot() {
        return this.xRot;
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public static class PacketPlayOutEntityLook extends PacketPlayOutEntity {

        public PacketPlayOutEntityLook(int i, byte b0, byte b1, boolean flag) {
            super(i, (short) 0, (short) 0, (short) 0, b0, b1, flag, true, false);
        }

        public static PacketPlayOutEntity.PacketPlayOutEntityLook read(PacketDataSerializer packetdataserializer) {
            int i = packetdataserializer.readVarInt();
            byte b0 = packetdataserializer.readByte();
            byte b1 = packetdataserializer.readByte();
            boolean flag = packetdataserializer.readBoolean();

            return new PacketPlayOutEntity.PacketPlayOutEntityLook(i, b0, b1, flag);
        }

        @Override
        public void write(PacketDataSerializer packetdataserializer) {
            packetdataserializer.writeVarInt(this.entityId);
            packetdataserializer.writeByte(this.yRot);
            packetdataserializer.writeByte(this.xRot);
            packetdataserializer.writeBoolean(this.onGround);
        }
    }

    public static class PacketPlayOutRelEntityMove extends PacketPlayOutEntity {

        public PacketPlayOutRelEntityMove(int i, short short0, short short1, short short2, boolean flag) {
            super(i, short0, short1, short2, (byte) 0, (byte) 0, flag, false, true);
        }

        public static PacketPlayOutEntity.PacketPlayOutRelEntityMove read(PacketDataSerializer packetdataserializer) {
            int i = packetdataserializer.readVarInt();
            short short0 = packetdataserializer.readShort();
            short short1 = packetdataserializer.readShort();
            short short2 = packetdataserializer.readShort();
            boolean flag = packetdataserializer.readBoolean();

            return new PacketPlayOutEntity.PacketPlayOutRelEntityMove(i, short0, short1, short2, flag);
        }

        @Override
        public void write(PacketDataSerializer packetdataserializer) {
            packetdataserializer.writeVarInt(this.entityId);
            packetdataserializer.writeShort(this.xa);
            packetdataserializer.writeShort(this.ya);
            packetdataserializer.writeShort(this.za);
            packetdataserializer.writeBoolean(this.onGround);
        }
    }

    public static class PacketPlayOutRelEntityMoveLook extends PacketPlayOutEntity {

        public PacketPlayOutRelEntityMoveLook(int i, short short0, short short1, short short2, byte b0, byte b1, boolean flag) {
            super(i, short0, short1, short2, b0, b1, flag, true, true);
        }

        public static PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook read(PacketDataSerializer packetdataserializer) {
            int i = packetdataserializer.readVarInt();
            short short0 = packetdataserializer.readShort();
            short short1 = packetdataserializer.readShort();
            short short2 = packetdataserializer.readShort();
            byte b0 = packetdataserializer.readByte();
            byte b1 = packetdataserializer.readByte();
            boolean flag = packetdataserializer.readBoolean();

            return new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(i, short0, short1, short2, b0, b1, flag);
        }

        @Override
        public void write(PacketDataSerializer packetdataserializer) {
            packetdataserializer.writeVarInt(this.entityId);
            packetdataserializer.writeShort(this.xa);
            packetdataserializer.writeShort(this.ya);
            packetdataserializer.writeShort(this.za);
            packetdataserializer.writeByte(this.yRot);
            packetdataserializer.writeByte(this.xRot);
            packetdataserializer.writeBoolean(this.onGround);
        }
    }
}
