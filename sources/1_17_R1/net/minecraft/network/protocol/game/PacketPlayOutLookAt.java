package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class PacketPlayOutLookAt implements Packet<PacketListenerPlayOut> {

    private final double x;
    private final double y;
    private final double z;
    private final int entity;
    private final ArgumentAnchor.Anchor fromAnchor;
    private final ArgumentAnchor.Anchor toAnchor;
    private final boolean atEntity;

    public PacketPlayOutLookAt(ArgumentAnchor.Anchor argumentanchor_anchor, double d0, double d1, double d2) {
        this.fromAnchor = argumentanchor_anchor;
        this.x = d0;
        this.y = d1;
        this.z = d2;
        this.entity = 0;
        this.atEntity = false;
        this.toAnchor = null;
    }

    public PacketPlayOutLookAt(ArgumentAnchor.Anchor argumentanchor_anchor, Entity entity, ArgumentAnchor.Anchor argumentanchor_anchor1) {
        this.fromAnchor = argumentanchor_anchor;
        this.entity = entity.getId();
        this.toAnchor = argumentanchor_anchor1;
        Vec3D vec3d = argumentanchor_anchor1.a(entity);

        this.x = vec3d.x;
        this.y = vec3d.y;
        this.z = vec3d.z;
        this.atEntity = true;
    }

    public PacketPlayOutLookAt(PacketDataSerializer packetdataserializer) {
        this.fromAnchor = (ArgumentAnchor.Anchor) packetdataserializer.a(ArgumentAnchor.Anchor.class);
        this.x = packetdataserializer.readDouble();
        this.y = packetdataserializer.readDouble();
        this.z = packetdataserializer.readDouble();
        this.atEntity = packetdataserializer.readBoolean();
        if (this.atEntity) {
            this.entity = packetdataserializer.j();
            this.toAnchor = (ArgumentAnchor.Anchor) packetdataserializer.a(ArgumentAnchor.Anchor.class);
        } else {
            this.entity = 0;
            this.toAnchor = null;
        }

    }

    @Override
    public void a(PacketDataSerializer packetdataserializer) {
        packetdataserializer.a((Enum) this.fromAnchor);
        packetdataserializer.writeDouble(this.x);
        packetdataserializer.writeDouble(this.y);
        packetdataserializer.writeDouble(this.z);
        packetdataserializer.writeBoolean(this.atEntity);
        if (this.atEntity) {
            packetdataserializer.d(this.entity);
            packetdataserializer.a((Enum) this.toAnchor);
        }

    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

    public ArgumentAnchor.Anchor b() {
        return this.fromAnchor;
    }

    @Nullable
    public Vec3D a(World world) {
        if (this.atEntity) {
            Entity entity = world.getEntity(this.entity);

            return entity == null ? new Vec3D(this.x, this.y, this.z) : this.toAnchor.a(entity);
        } else {
            return new Vec3D(this.x, this.y, this.z);
        }
    }
}
