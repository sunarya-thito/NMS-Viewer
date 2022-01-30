package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentChat;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.players.IpBanEntry;
import net.minecraft.server.players.IpBanList;

public class CommandBanIp {

    public static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final SimpleCommandExceptionType ERROR_INVALID_IP = new SimpleCommandExceptionType(new ChatMessage("commands.banip.invalid"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(new ChatMessage("commands.banip.failed"));

    public CommandBanIp() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("ban-ip").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(3);
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("target", StringArgumentType.word()).executes((commandcontext) -> {
            return banIpOrName((CommandListenerWrapper) commandcontext.getSource(), StringArgumentType.getString(commandcontext, "target"), (IChatBaseComponent) null);
        })).then(net.minecraft.commands.CommandDispatcher.argument("reason", ArgumentChat.message()).executes((commandcontext) -> {
            return banIpOrName((CommandListenerWrapper) commandcontext.getSource(), StringArgumentType.getString(commandcontext, "target"), ArgumentChat.getMessage(commandcontext, "reason"));
        }))));
    }

    private static int banIpOrName(CommandListenerWrapper commandlistenerwrapper, String s, @Nullable IChatBaseComponent ichatbasecomponent) throws CommandSyntaxException {
        Matcher matcher = CommandBanIp.IP_ADDRESS_PATTERN.matcher(s);

        if (matcher.matches()) {
            return banIp(commandlistenerwrapper, s, ichatbasecomponent);
        } else {
            EntityPlayer entityplayer = commandlistenerwrapper.getServer().getPlayerList().getPlayerByName(s);

            if (entityplayer != null) {
                return banIp(commandlistenerwrapper, entityplayer.getIpAddress(), ichatbasecomponent);
            } else {
                throw CommandBanIp.ERROR_INVALID_IP.create();
            }
        }
    }

    private static int banIp(CommandListenerWrapper commandlistenerwrapper, String s, @Nullable IChatBaseComponent ichatbasecomponent) throws CommandSyntaxException {
        IpBanList ipbanlist = commandlistenerwrapper.getServer().getPlayerList().getIpBans();

        if (ipbanlist.isBanned(s)) {
            throw CommandBanIp.ERROR_ALREADY_BANNED.create();
        } else {
            List<EntityPlayer> list = commandlistenerwrapper.getServer().getPlayerList().getPlayersWithAddress(s);
            IpBanEntry ipbanentry = new IpBanEntry(s, (Date) null, commandlistenerwrapper.getTextName(), (Date) null, ichatbasecomponent == null ? null : ichatbasecomponent.getString());

            ipbanlist.add(ipbanentry);
            commandlistenerwrapper.sendSuccess(new ChatMessage("commands.banip.success", new Object[]{s, ipbanentry.getReason()}), true);
            if (!list.isEmpty()) {
                commandlistenerwrapper.sendSuccess(new ChatMessage("commands.banip.info", new Object[]{list.size(), EntitySelector.joinNames(list)}), true);
            }

            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                entityplayer.connection.disconnect(new ChatMessage("multiplayer.disconnect.ip_banned"));
            }

            return list.size();
        }
    }
}
