package net.minecraft.commands;

import java.util.UUID;
import net.minecraft.network.chat.IChatBaseComponent;

public interface ICommandListener {

    ICommandListener NULL = new ICommandListener() {
        @Override
        public void sendMessage(IChatBaseComponent ichatbasecomponent, UUID uuid) {}

        @Override
        public boolean acceptsSuccess() {
            return false;
        }

        @Override
        public boolean acceptsFailure() {
            return false;
        }

        @Override
        public boolean shouldInformAdmins() {
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

    boolean acceptsSuccess();

    boolean acceptsFailure();

    boolean shouldInformAdmins();

    default boolean alwaysAccepts() {
        return false;
    }

    org.bukkit.command.CommandSender getBukkitSender(CommandListenerWrapper wrapper); // CraftBukkit
}
