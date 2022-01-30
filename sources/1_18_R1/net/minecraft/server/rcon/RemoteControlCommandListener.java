package net.minecraft.server.rcon;

import java.util.UUID;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class RemoteControlCommandListener implements ICommandListener {

    private static final String RCON = "Rcon";
    private static final IChatBaseComponent RCON_COMPONENT = new ChatComponentText("Rcon");
    private final StringBuffer buffer = new StringBuffer();
    private final MinecraftServer server;

    public RemoteControlCommandListener(MinecraftServer minecraftserver) {
        this.server = minecraftserver;
    }

    public void prepareForCommand() {
        this.buffer.setLength(0);
    }

    public String getCommandResponse() {
        return this.buffer.toString();
    }

    public CommandListenerWrapper createCommandSourceStack() {
        WorldServer worldserver = this.server.overworld();

        return new CommandListenerWrapper(this, Vec3D.atLowerCornerOf(worldserver.getSharedSpawnPos()), Vec2F.ZERO, worldserver, 4, "Rcon", RemoteControlCommandListener.RCON_COMPONENT, this.server, (Entity) null);
    }

    // CraftBukkit start - Send a String
    public void sendMessage(String message) {
        this.buffer.append(message);
    }

    @Override
    public org.bukkit.command.CommandSender getBukkitSender(CommandListenerWrapper wrapper) {
        return server.remoteConsole;
    }
    // CraftBukkit end

    @Override
    public void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid) {
        this.buffer.append(ichatbasecomponent.getString());
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return this.server.shouldRconBroadcast();
    }
}
