package net.minecraft.network.protocol;

import net.minecraft.network.PacketListener;
import net.minecraft.server.CancelledPacketHandleException;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.thread.IAsyncTaskHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.PlayerConnection;
// CraftBukkit end

public class PlayerConnectionUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public PlayerConnectionUtils() {}

    public static <T extends PacketListener> void ensureMainThread(Packet<T> packet, T t0, WorldServer worldserver) throws CancelledPacketHandleException {
        ensureMainThread(packet, t0, (IAsyncTaskHandler) worldserver.getMinecraftServer());
    }

    public static <T extends PacketListener> void ensureMainThread(Packet<T> packet, T t0, IAsyncTaskHandler<?> iasynctaskhandler) throws CancelledPacketHandleException {
        if (!iasynctaskhandler.isMainThread()) {
            iasynctaskhandler.execute(() -> {
                if (MinecraftServer.getServer().hasStopped() || (t0 instanceof PlayerConnection && ((PlayerConnection) t0).processedDisconnect)) return; // CraftBukkit, MC-142590
                if (t0.a().isConnected()) {
                    packet.a(t0);
                } else {
                    PlayerConnectionUtils.LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
                }

            });
            throw CancelledPacketHandleException.RUNNING_ON_DIFFERENT_THREAD;
            // CraftBukkit start - SPIGOT-5477, MC-142590
        } else if (MinecraftServer.getServer().hasStopped() || (t0 instanceof PlayerConnection && ((PlayerConnection) t0).processedDisconnect)) {
            throw CancelledPacketHandleException.RUNNING_ON_DIFFERENT_THREAD;
            // CraftBukkit end
        }
    }
}
