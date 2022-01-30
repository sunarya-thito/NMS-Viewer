package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;

public class PathEntity {

    private final List<PathPoint> nodes;
    private PathPoint[] openSet = new PathPoint[0];
    private PathPoint[] closedSet = new PathPoint[0];
    private Set<PathDestination> targetNodes;
    private int nextNodeIndex;
    private final BlockPosition target;
    private final float distToTarget;
    private final boolean reached;

    public PathEntity(List<PathPoint> list, BlockPosition blockposition, boolean flag) {
        this.nodes = list;
        this.target = blockposition;
        this.distToTarget = list.isEmpty() ? Float.MAX_VALUE : ((PathPoint) this.nodes.get(this.nodes.size() - 1)).c(this.target);
        this.reached = flag;
    }

    public void a() {
        ++this.nextNodeIndex;
    }

    public boolean b() {
        return this.nextNodeIndex <= 0;
    }

    public boolean c() {
        return this.nextNodeIndex >= this.nodes.size();
    }

    @Nullable
    public PathPoint d() {
        return !this.nodes.isEmpty() ? (PathPoint) this.nodes.get(this.nodes.size() - 1) : null;
    }

    public PathPoint a(int i) {
        return (PathPoint) this.nodes.get(i);
    }

    public void b(int i) {
        if (this.nodes.size() > i) {
            this.nodes.subList(i, this.nodes.size()).clear();
        }

    }

    public void a(int i, PathPoint pathpoint) {
        this.nodes.set(i, pathpoint);
    }

    public int e() {
        return this.nodes.size();
    }

    public int f() {
        return this.nextNodeIndex;
    }

    public void c(int i) {
        this.nextNodeIndex = i;
    }

    public Vec3D a(Entity entity, int i) {
        PathPoint pathpoint = (PathPoint) this.nodes.get(i);
        double d0 = (double) pathpoint.x + (double) ((int) (entity.getWidth() + 1.0F)) * 0.5D;
        double d1 = (double) pathpoint.y;
        double d2 = (double) pathpoint.z + (double) ((int) (entity.getWidth() + 1.0F)) * 0.5D;

        return new Vec3D(d0, d1, d2);
    }

    public BlockPosition d(int i) {
        return ((PathPoint) this.nodes.get(i)).a();
    }

    public Vec3D a(Entity entity) {
        return this.a(entity, this.nextNodeIndex);
    }

    public BlockPosition g() {
        return ((PathPoint) this.nodes.get(this.nextNodeIndex)).a();
    }

    public PathPoint h() {
        return (PathPoint) this.nodes.get(this.nextNodeIndex);
    }

    @Nullable
    public PathPoint i() {
        return this.nextNodeIndex > 0 ? (PathPoint) this.nodes.get(this.nextNodeIndex - 1) : null;
    }

    public boolean a(@Nullable PathEntity pathentity) {
        if (pathentity == null) {
            return false;
        } else if (pathentity.nodes.size() != this.nodes.size()) {
            return false;
        } else {
            for (int i = 0; i < this.nodes.size(); ++i) {
                PathPoint pathpoint = (PathPoint) this.nodes.get(i);
                PathPoint pathpoint1 = (PathPoint) pathentity.nodes.get(i);

                if (pathpoint.x != pathpoint1.x || pathpoint.y != pathpoint1.y || pathpoint.z != pathpoint1.z) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean j() {
        return this.reached;
    }

    @VisibleForDebug
    void a(PathPoint[] apathpoint, PathPoint[] apathpoint1, Set<PathDestination> set) {
        this.openSet = apathpoint;
        this.closedSet = apathpoint1;
        this.targetNodes = set;
    }

    @VisibleForDebug
    public PathPoint[] k() {
        return this.openSet;
    }

    @VisibleForDebug
    public PathPoint[] l() {
        return this.closedSet;
    }

    public void a(PacketDataSerializer packetdataserializer) {
        if (this.targetNodes != null && !this.targetNodes.isEmpty()) {
            packetdataserializer.writeBoolean(this.reached);
            packetdataserializer.writeInt(this.nextNodeIndex);
            packetdataserializer.writeInt(this.targetNodes.size());
            this.targetNodes.forEach((pathdestination) -> {
                pathdestination.a(packetdataserializer);
            });
            packetdataserializer.writeInt(this.target.getX());
            packetdataserializer.writeInt(this.target.getY());
            packetdataserializer.writeInt(this.target.getZ());
            packetdataserializer.writeInt(this.nodes.size());
            Iterator iterator = this.nodes.iterator();

            while (iterator.hasNext()) {
                PathPoint pathpoint = (PathPoint) iterator.next();

                pathpoint.a(packetdataserializer);
            }

            packetdataserializer.writeInt(this.openSet.length);
            PathPoint[] apathpoint = this.openSet;
            int i = apathpoint.length;

            PathPoint pathpoint1;
            int j;

            for (j = 0; j < i; ++j) {
                pathpoint1 = apathpoint[j];
                pathpoint1.a(packetdataserializer);
            }

            packetdataserializer.writeInt(this.closedSet.length);
            apathpoint = this.closedSet;
            i = apathpoint.length;

            for (j = 0; j < i; ++j) {
                pathpoint1 = apathpoint[j];
                pathpoint1.a(packetdataserializer);
            }

        }
    }

    public static PathEntity b(PacketDataSerializer packetdataserializer) {
        boolean flag = packetdataserializer.readBoolean();
        int i = packetdataserializer.readInt();
        int j = packetdataserializer.readInt();
        Set<PathDestination> set = Sets.newHashSet();

        for (int k = 0; k < j; ++k) {
            set.add(PathDestination.c(packetdataserializer));
        }

        BlockPosition blockposition = new BlockPosition(packetdataserializer.readInt(), packetdataserializer.readInt(), packetdataserializer.readInt());
        List<PathPoint> list = Lists.newArrayList();
        int l = packetdataserializer.readInt();

        for (int i1 = 0; i1 < l; ++i1) {
            list.add(PathPoint.b(packetdataserializer));
        }

        PathPoint[] apathpoint = new PathPoint[packetdataserializer.readInt()];

        for (int j1 = 0; j1 < apathpoint.length; ++j1) {
            apathpoint[j1] = PathPoint.b(packetdataserializer);
        }

        PathPoint[] apathpoint1 = new PathPoint[packetdataserializer.readInt()];

        for (int k1 = 0; k1 < apathpoint1.length; ++k1) {
            apathpoint1[k1] = PathPoint.b(packetdataserializer);
        }

        PathEntity pathentity = new PathEntity(list, blockposition, flag);

        pathentity.openSet = apathpoint;
        pathentity.closedSet = apathpoint1;
        pathentity.targetNodes = set;
        pathentity.nextNodeIndex = i;
        return pathentity;
    }

    public String toString() {
        return "Path(length=" + this.nodes.size() + ")";
    }

    public BlockPosition m() {
        return this.target;
    }

    public float n() {
        return this.distToTarget;
    }
}
