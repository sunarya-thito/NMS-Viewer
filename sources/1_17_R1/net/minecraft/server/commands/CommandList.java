package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.EntityHuman;

public class CommandList {

    public CommandList() {}

    public static void a(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.a("list").executes((commandcontext) -> {
            return a((CommandListenerWrapper) commandcontext.getSource());
        })).then(net.minecraft.commands.CommandDispatcher.a("uuids").executes((commandcontext) -> {
            return b((CommandListenerWrapper) commandcontext.getSource());
        })));
    }

    private static int a(CommandListenerWrapper commandlistenerwrapper) {
        return a(commandlistenerwrapper, EntityHuman::getScoreboardDisplayName);
    }

    private static int b(CommandListenerWrapper commandlistenerwrapper) {
        return a(commandlistenerwrapper, (entityplayer) -> {
            return new ChatMessage("commands.list.nameAndId", new Object[]{entityplayer.getDisplayName(), entityplayer.getProfile().getId()});
        });
    }

    private static int a(CommandListenerWrapper commandlistenerwrapper, Function<EntityPlayer, IChatBaseComponent> function) {
        PlayerList playerlist = commandlistenerwrapper.getServer().getPlayerList();
        List<EntityPlayer> list = playerlist.getPlayers();
        // CraftBukkit start
        if (commandlistenerwrapper.getBukkitSender() instanceof org.bukkit.entity.Player) {
            org.bukkit.entity.Player sender = (org.bukkit.entity.Player) commandlistenerwrapper.getBukkitSender();
            list = list.stream().filter((ep) -> sender.canSee(ep.getBukkitEntity())).collect(java.util.stream.Collectors.toList());
        }
        // CraftBukkit end
        IChatBaseComponent ichatbasecomponent = ChatComponentUtils.b(list, function);

        commandlistenerwrapper.sendMessage(new ChatMessage("commands.list.players", new Object[]{list.size(), playerlist.getMaxPlayers(), ichatbasecomponent}), false);
        return list.size();
    }
}
