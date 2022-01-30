package net.minecraft.commands;

import java.util.UUID;
import net.minecraft.network.chat.IChatBaseComponent;

public interface ICommandListener {

    ICommandListener NULL = new ICommandListener() {
        @Override
        public void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid) {}

        @Override
        public boolean shouldSendSuccess() {
            return false;
        }

        @Override
        public boolean shouldSendFailure() {
            return false;
        }

        @Override
        public boolean shouldBroadcastCommands() {
            return false;
        }

        // CraftBukkit start
        @Override
        public org.bukkit.command.CommandSender getBukkitSender(CommandListenerWrapper wrapper) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        // CraftBukkit end
    };

    void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid);

    boolean shouldSendSuccess();

    boolean shouldSendFailure();

    boolean shouldBroadcastCommands();

    default boolean c_() {
        return false;
    }

    org.bukkit.command.CommandSender getBukkitSender(CommandListenerWrapper wrapper); // CraftBukkit
}
