package net.minecraft.server.level;

import java.util.Comparator;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkCoordIntPair;

public class TicketType<T> {

    private final String name;
    private final Comparator<T> comparator;
    public long timeout;
    public static final TicketType<Unit> START = a("start", (unit, unit1) -> {
        return 0;
    });
    public static final TicketType<Unit> DRAGON = a("dragon", (unit, unit1) -> {
        return 0;
    });
    public static final TicketType<ChunkCoordIntPair> PLAYER = a("player", Comparator.comparingLong(ChunkCoordIntPair::pair));
    public static final TicketType<ChunkCoordIntPair> FORCED = a("forced", Comparator.comparingLong(ChunkCoordIntPair::pair));
    public static final TicketType<ChunkCoordIntPair> LIGHT = a("light", Comparator.comparingLong(ChunkCoordIntPair::pair));
    public static final TicketType<BlockPosition> PORTAL = a("portal", BaseBlockPosition::compareTo, 300);
    public static final TicketType<Integer> POST_TELEPORT = a("post_teleport", Integer::compareTo, 5);
    public static final TicketType<ChunkCoordIntPair> UNKNOWN = a("unknown", Comparator.comparingLong(ChunkCoordIntPair::pair), 1);
    public static final TicketType<Unit> PLUGIN = a("plugin", (a, b) -> 0); // CraftBukkit
    public static final TicketType<org.bukkit.plugin.Plugin> PLUGIN_TICKET = a("plugin_ticket", (plugin1, plugin2) -> plugin1.getClass().getName().compareTo(plugin2.getClass().getName())); // CraftBukkit

    public static <T> TicketType<T> a(String s, Comparator<T> comparator) {
        return new TicketType<>(s, comparator, 0L);
    }

    public static <T> TicketType<T> a(String s, Comparator<T> comparator, int i) {
        return new TicketType<>(s, comparator, (long) i);
    }

    protected TicketType(String s, Comparator<T> comparator, long i) {
        this.name = s;
        this.comparator = comparator;
        this.timeout = i;
    }

    public String toString() {
        return this.name;
    }

    public Comparator<T> a() {
        return this.comparator;
    }

    public long b() {
        return this.timeout;
    }
}
