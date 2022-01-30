package net.minecraft.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.commands.synchronization.ArgumentRegistry;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.gametest.framework.GameTestHarnessTestCommand;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.game.PacketPlayOutCommands;
import net.minecraft.server.commands.CommandAdvancement;
import net.minecraft.server.commands.CommandAttribute;
import net.minecraft.server.commands.CommandBan;
import net.minecraft.server.commands.CommandBanIp;
import net.minecraft.server.commands.CommandBanList;
import net.minecraft.server.commands.CommandBossBar;
import net.minecraft.server.commands.CommandClear;
import net.minecraft.server.commands.CommandClone;
import net.minecraft.server.commands.CommandDatapack;
import net.minecraft.server.commands.CommandDebug;
import net.minecraft.server.commands.CommandDeop;
import net.minecraft.server.commands.CommandDifficulty;
import net.minecraft.server.commands.CommandEffect;
import net.minecraft.server.commands.CommandEnchant;
import net.minecraft.server.commands.CommandExecute;
import net.minecraft.server.commands.CommandFill;
import net.minecraft.server.commands.CommandForceload;
import net.minecraft.server.commands.CommandFunction;
import net.minecraft.server.commands.CommandGamemode;
import net.minecraft.server.commands.CommandGamemodeDefault;
import net.minecraft.server.commands.CommandGamerule;
import net.minecraft.server.commands.CommandGive;
import net.minecraft.server.commands.CommandHelp;
import net.minecraft.server.commands.CommandIdleTimeout;
import net.minecraft.server.commands.CommandKick;
import net.minecraft.server.commands.CommandKill;
import net.minecraft.server.commands.CommandList;
import net.minecraft.server.commands.CommandLocate;
import net.minecraft.server.commands.CommandLocateBiome;
import net.minecraft.server.commands.CommandLoot;
import net.minecraft.server.commands.CommandMe;
import net.minecraft.server.commands.CommandOp;
import net.minecraft.server.commands.CommandPardon;
import net.minecraft.server.commands.CommandPardonIP;
import net.minecraft.server.commands.CommandParticle;
import net.minecraft.server.commands.CommandPlaySound;
import net.minecraft.server.commands.CommandPublish;
import net.minecraft.server.commands.CommandRecipe;
import net.minecraft.server.commands.CommandReload;
import net.minecraft.server.commands.CommandSaveAll;
import net.minecraft.server.commands.CommandSaveOff;
import net.minecraft.server.commands.CommandSaveOn;
import net.minecraft.server.commands.CommandSay;
import net.minecraft.server.commands.CommandSchedule;
import net.minecraft.server.commands.CommandScoreboard;
import net.minecraft.server.commands.CommandSeed;
import net.minecraft.server.commands.CommandSetBlock;
import net.minecraft.server.commands.CommandSetWorldSpawn;
import net.minecraft.server.commands.CommandSpawnpoint;
import net.minecraft.server.commands.CommandSpectate;
import net.minecraft.server.commands.CommandSpreadPlayers;
import net.minecraft.server.commands.CommandStop;
import net.minecraft.server.commands.CommandStopSound;
import net.minecraft.server.commands.CommandSummon;
import net.minecraft.server.commands.CommandTag;
import net.minecraft.server.commands.CommandTeam;
import net.minecraft.server.commands.CommandTeamMsg;
import net.minecraft.server.commands.CommandTeleport;
import net.minecraft.server.commands.CommandTell;
import net.minecraft.server.commands.CommandTellRaw;
import net.minecraft.server.commands.CommandTime;
import net.minecraft.server.commands.CommandTitle;
import net.minecraft.server.commands.CommandTrigger;
import net.minecraft.server.commands.CommandWeather;
import net.minecraft.server.commands.CommandWhitelist;
import net.minecraft.server.commands.CommandWorldBorder;
import net.minecraft.server.commands.CommandXp;
import net.minecraft.server.commands.ItemCommands;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.commands.data.CommandData;
import net.minecraft.server.level.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import com.google.common.base.Joiner;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerCommandEvent;
// CraftBukkit end

public class CommandDispatcher {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_MODERATORS = 1;
    public static final int LEVEL_GAMEMASTERS = 2;
    public static final int LEVEL_ADMINS = 3;
    public static final int LEVEL_OWNERS = 4;
    private final com.mojang.brigadier.CommandDispatcher<CommandListenerWrapper> dispatcher = new com.mojang.brigadier.CommandDispatcher();

    public CommandDispatcher(CommandDispatcher.ServerType commanddispatcher_servertype) {
        this(); // CraftBukkit
        CommandAdvancement.a(this.dispatcher);
        CommandAttribute.a(this.dispatcher);
        CommandExecute.a(this.dispatcher);
        CommandBossBar.a(this.dispatcher);
        CommandClear.a(this.dispatcher);
        CommandClone.a(this.dispatcher);
        CommandData.a(this.dispatcher);
        CommandDatapack.a(this.dispatcher);
        CommandDebug.a(this.dispatcher);
        CommandGamemodeDefault.a(this.dispatcher);
        CommandDifficulty.a(this.dispatcher);
        CommandEffect.a(this.dispatcher);
        CommandMe.a(this.dispatcher);
        CommandEnchant.a(this.dispatcher);
        CommandXp.a(this.dispatcher);
        CommandFill.a(this.dispatcher);
        CommandForceload.a(this.dispatcher);
        CommandFunction.a(this.dispatcher);
        CommandGamemode.a(this.dispatcher);
        CommandGamerule.a(this.dispatcher);
        CommandGive.a(this.dispatcher);
        CommandHelp.a(this.dispatcher);
        ItemCommands.a(this.dispatcher);
        CommandKick.a(this.dispatcher);
        CommandKill.a(this.dispatcher);
        CommandList.a(this.dispatcher);
        CommandLocate.a(this.dispatcher);
        CommandLocateBiome.a(this.dispatcher);
        CommandLoot.a(this.dispatcher);
        CommandTell.a(this.dispatcher);
        CommandParticle.a(this.dispatcher);
        CommandPlaySound.a(this.dispatcher);
        CommandReload.a(this.dispatcher);
        CommandRecipe.a(this.dispatcher);
        CommandSay.a(this.dispatcher);
        CommandSchedule.a(this.dispatcher);
        CommandScoreboard.a(this.dispatcher);
        CommandSeed.a(this.dispatcher, commanddispatcher_servertype != CommandDispatcher.ServerType.INTEGRATED);
        CommandSetBlock.a(this.dispatcher);
        CommandSpawnpoint.a(this.dispatcher);
        CommandSetWorldSpawn.a(this.dispatcher);
        CommandSpectate.a(this.dispatcher);
        CommandSpreadPlayers.a(this.dispatcher);
        CommandStopSound.a(this.dispatcher);
        CommandSummon.a(this.dispatcher);
        CommandTag.a(this.dispatcher);
        CommandTeam.a(this.dispatcher);
        CommandTeamMsg.a(this.dispatcher);
        CommandTeleport.a(this.dispatcher);
        CommandTellRaw.a(this.dispatcher);
        CommandTime.a(this.dispatcher);
        CommandTitle.a(this.dispatcher);
        CommandTrigger.a(this.dispatcher);
        CommandWeather.a(this.dispatcher);
        CommandWorldBorder.a(this.dispatcher);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            GameTestHarnessTestCommand.a(this.dispatcher);
        }

        if (commanddispatcher_servertype.includeDedicated) {
            CommandBanIp.a(this.dispatcher);
            CommandBanList.a(this.dispatcher);
            CommandBan.a(this.dispatcher);
            CommandDeop.a(this.dispatcher);
            CommandOp.a(this.dispatcher);
            CommandPardon.a(this.dispatcher);
            CommandPardonIP.a(this.dispatcher);
            PerfCommand.a(this.dispatcher);
            CommandSaveAll.a(this.dispatcher);
            CommandSaveOff.a(this.dispatcher);
            CommandSaveOn.a(this.dispatcher);
            CommandIdleTimeout.a(this.dispatcher);
            CommandStop.a(this.dispatcher);
            CommandWhitelist.a(this.dispatcher);
        }

        if (commanddispatcher_servertype.includeIntegrated) {
            CommandPublish.a(this.dispatcher);
        }

        this.dispatcher.findAmbiguities((commandnode, commandnode1, commandnode2, collection) -> {
            // CommandDispatcher.LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", this.b.getPath(commandnode1), this.b.getPath(commandnode2), collection); // CraftBukkit
        });
    }

    // CraftBukkit start
    public CommandDispatcher() {
        this.dispatcher.setConsumer((commandcontext, flag1, i) -> {
            ((CommandListenerWrapper) commandcontext.getSource()).a(commandcontext, flag1, i);
        });
    }

    public int dispatchServerCommand(CommandListenerWrapper sender, String command) {
        Joiner joiner = Joiner.on(" ");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        ServerCommandEvent event = new ServerCommandEvent(sender.getBukkitSender(), command);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return 0;
        }
        command = event.getCommand();

        String[] args = command.split(" ");

        String cmd = args[0];
        if (cmd.startsWith("minecraft:")) cmd = cmd.substring("minecraft:".length());
        if (cmd.startsWith("bukkit:")) cmd = cmd.substring("bukkit:".length());

        // Block disallowed commands
        if (cmd.equalsIgnoreCase("stop") || cmd.equalsIgnoreCase("kick") || cmd.equalsIgnoreCase("op")
                || cmd.equalsIgnoreCase("deop") || cmd.equalsIgnoreCase("ban") || cmd.equalsIgnoreCase("ban-ip")
                || cmd.equalsIgnoreCase("pardon") || cmd.equalsIgnoreCase("pardon-ip") || cmd.equalsIgnoreCase("reload")) {
            return 0;
        }

        // Handle vanilla commands;
        if (sender.getWorld().getCraftServer().getCommandBlockOverride(args[0])) {
            args[0] = "minecraft:" + args[0];
        }

        String newCommand = joiner.join(args);
        return this.a(sender, newCommand, newCommand, false);
    }

    public int a(CommandListenerWrapper commandlistenerwrapper, String s) {
        return this.a(commandlistenerwrapper, s, s, true);
    }

    public int a(CommandListenerWrapper commandlistenerwrapper, String s, String label, boolean stripSlash) {
        StringReader stringreader = new StringReader(s);

        if (stripSlash && stringreader.canRead() && stringreader.peek() == '/') {
            // CraftBukkit end
            stringreader.skip();
        }

        commandlistenerwrapper.getServer().getMethodProfiler().enter(s);

        byte b0;

        try {
            byte b1;

            try {
                int i = this.dispatcher.execute(stringreader, commandlistenerwrapper);

                return i;
            } catch (CommandException commandexception) {
                commandlistenerwrapper.sendFailureMessage(commandexception.a());
                b1 = 0;
                return b1;
            } catch (CommandSyntaxException commandsyntaxexception) {
                commandlistenerwrapper.sendFailureMessage(ChatComponentUtils.a(commandsyntaxexception.getRawMessage()));
                if (commandsyntaxexception.getInput() != null && commandsyntaxexception.getCursor() >= 0) {
                    int j = Math.min(commandsyntaxexception.getInput().length(), commandsyntaxexception.getCursor());
                    IChatMutableComponent ichatmutablecomponent = (new ChatComponentText("")).a(EnumChatFormat.GRAY).format((chatmodifier) -> {
                        return chatmodifier.setChatClickable(new ChatClickable(ChatClickable.EnumClickAction.SUGGEST_COMMAND, label)); // CraftBukkit
                    });

                    if (j > 10) {
                        ichatmutablecomponent.c("...");
                    }

                    ichatmutablecomponent.c(commandsyntaxexception.getInput().substring(Math.max(0, j - 10), j));
                    if (j < commandsyntaxexception.getInput().length()) {
                        IChatMutableComponent ichatmutablecomponent1 = (new ChatComponentText(commandsyntaxexception.getInput().substring(j))).a(new EnumChatFormat[]{EnumChatFormat.RED, EnumChatFormat.UNDERLINE});

                        ichatmutablecomponent.addSibling(ichatmutablecomponent1);
                    }

                    ichatmutablecomponent.addSibling((new ChatMessage("command.context.here")).a(new EnumChatFormat[]{EnumChatFormat.RED, EnumChatFormat.ITALIC}));
                    commandlistenerwrapper.sendFailureMessage(ichatmutablecomponent);
                }

                b1 = 0;
                return b1;
            } catch (Exception exception) {
                ChatComponentText chatcomponenttext = new ChatComponentText(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage());

                if (CommandDispatcher.LOGGER.isDebugEnabled()) {
                    CommandDispatcher.LOGGER.error("Command exception: {}", s, exception);
                    StackTraceElement[] astacktraceelement = exception.getStackTrace();

                    for (int k = 0; k < Math.min(astacktraceelement.length, 3); ++k) {
                        chatcomponenttext.c("\n\n").c(astacktraceelement[k].getMethodName()).c("\n ").c(astacktraceelement[k].getFileName()).c(":").c(String.valueOf(astacktraceelement[k].getLineNumber()));
                    }
                }

                commandlistenerwrapper.sendFailureMessage((new ChatMessage("command.failed")).format((chatmodifier) -> {
                    return chatmodifier.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_TEXT, chatcomponenttext));
                }));
                if (SharedConstants.IS_RUNNING_IN_IDE) {
                    commandlistenerwrapper.sendFailureMessage(new ChatComponentText(SystemUtils.d(exception)));
                    CommandDispatcher.LOGGER.error("'{}' threw an exception", s, exception);
                }

                b0 = 0;
            }
        } finally {
            commandlistenerwrapper.getServer().getMethodProfiler().exit();
        }

        return b0;
    }

    public void a(EntityPlayer entityplayer) {
        if ( org.spigotmc.SpigotConfig.tabComplete < 0 ) return; // Spigot
        // CraftBukkit start
        // Register Vanilla commands into builtRoot as before
        Map<CommandNode<CommandListenerWrapper>, CommandNode<ICompletionProvider>> map = Maps.newIdentityHashMap(); // Use identity to prevent aliasing issues
        RootCommandNode vanillaRoot = new RootCommandNode();

        RootCommandNode<CommandListenerWrapper> vanilla = entityplayer.server.vanillaCommandDispatcher.a().getRoot();
        map.put(vanilla, vanillaRoot);
        this.a(vanilla, vanillaRoot, entityplayer.getCommandListener(), (Map) map);

        // Now build the global commands in a second pass
        RootCommandNode<ICompletionProvider> rootcommandnode = new RootCommandNode();

        map.put(this.dispatcher.getRoot(), rootcommandnode);
        this.a(this.dispatcher.getRoot(), rootcommandnode, entityplayer.getCommandListener(), (Map) map);

        Collection<String> bukkit = new LinkedHashSet<>();
        for (CommandNode node : rootcommandnode.getChildren()) {
            bukkit.add(node.getName());
        }

        PlayerCommandSendEvent event = new PlayerCommandSendEvent(entityplayer.getBukkitEntity(), new LinkedHashSet<>(bukkit));
        event.getPlayer().getServer().getPluginManager().callEvent(event);

        // Remove labels that were removed during the event
        for (String orig : bukkit) {
            if (!event.getCommands().contains(orig)) {
                rootcommandnode.removeCommand(orig);
            }
        }
        // CraftBukkit end
        entityplayer.connection.sendPacket(new PacketPlayOutCommands(rootcommandnode));
    }

    private void a(CommandNode<CommandListenerWrapper> commandnode, CommandNode<ICompletionProvider> commandnode1, CommandListenerWrapper commandlistenerwrapper, Map<CommandNode<CommandListenerWrapper>, CommandNode<ICompletionProvider>> map) {
        Iterator iterator = commandnode.getChildren().iterator();

        while (iterator.hasNext()) {
            CommandNode<CommandListenerWrapper> commandnode2 = (CommandNode) iterator.next();
            if ( !org.spigotmc.SpigotConfig.sendNamespaced && commandnode2.getName().contains( ":" ) ) continue; // Spigot

            if (commandnode2.canUse(commandlistenerwrapper)) {
                ArgumentBuilder argumentbuilder = commandnode2.createBuilder(); // CraftBukkit - decompile error

                argumentbuilder.requires((icompletionprovider) -> {
                    return true;
                });
                if (argumentbuilder.getCommand() != null) {
                    argumentbuilder.executes((commandcontext) -> {
                        return 0;
                    });
                }

                if (argumentbuilder instanceof RequiredArgumentBuilder) {
                    RequiredArgumentBuilder<ICompletionProvider, ?> requiredargumentbuilder = (RequiredArgumentBuilder) argumentbuilder;

                    if (requiredargumentbuilder.getSuggestionsProvider() != null) {
                        requiredargumentbuilder.suggests(CompletionProviders.b(requiredargumentbuilder.getSuggestionsProvider()));
                    }
                }

                if (argumentbuilder.getRedirect() != null) {
                    argumentbuilder.redirect((CommandNode) map.get(argumentbuilder.getRedirect()));
                }

                CommandNode commandnode3 = argumentbuilder.build(); // CraftBukkit - decompile error

                map.put(commandnode2, commandnode3);
                commandnode1.addChild(commandnode3);
                if (!commandnode2.getChildren().isEmpty()) {
                    this.a(commandnode2, commandnode3, commandlistenerwrapper, map);
                }
            }
        }

    }

    public static LiteralArgumentBuilder<CommandListenerWrapper> a(String s) {
        return LiteralArgumentBuilder.literal(s);
    }

    public static <T> RequiredArgumentBuilder<CommandListenerWrapper, T> a(String s, ArgumentType<T> argumenttype) {
        return RequiredArgumentBuilder.argument(s, argumenttype);
    }

    public static Predicate<String> a(CommandDispatcher.b commanddispatcher_b) {
        return (s) -> {
            try {
                commanddispatcher_b.parse(new StringReader(s));
                return true;
            } catch (CommandSyntaxException commandsyntaxexception) {
                return false;
            }
        };
    }

    public com.mojang.brigadier.CommandDispatcher<CommandListenerWrapper> a() {
        return this.dispatcher;
    }

    @Nullable
    public static <S> CommandSyntaxException a(ParseResults<S> parseresults) {
        return !parseresults.getReader().canRead() ? null : (parseresults.getExceptions().size() == 1 ? (CommandSyntaxException) parseresults.getExceptions().values().iterator().next() : (parseresults.getContext().getRange().isEmpty() ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parseresults.getReader()) : CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parseresults.getReader())));
    }

    public static void b() {
        RootCommandNode<CommandListenerWrapper> rootcommandnode = (new CommandDispatcher(CommandDispatcher.ServerType.ALL)).a().getRoot();
        Set<ArgumentType<?>> set = ArgumentRegistry.a((CommandNode) rootcommandnode);
        Set<ArgumentType<?>> set1 = (Set) set.stream().filter((argumenttype) -> {
            return !ArgumentRegistry.a(argumenttype);
        }).collect(Collectors.toSet());

        if (!set1.isEmpty()) {
            CommandDispatcher.LOGGER.warn("Missing type registration for following arguments:\n {}", set1.stream().map((argumenttype) -> {
                return "\t" + argumenttype;
            }).collect(Collectors.joining(",\n")));
            throw new IllegalStateException("Unregistered argument types");
        }
    }

    public static enum ServerType {

        ALL(true, true), DEDICATED(false, true), INTEGRATED(true, false);

        final boolean includeIntegrated;
        final boolean includeDedicated;

        private ServerType(boolean flag, boolean flag1) {
            this.includeIntegrated = flag;
            this.includeDedicated = flag1;
        }
    }

    @FunctionalInterface
    public interface b {

        void parse(StringReader stringreader) throws CommandSyntaxException;
    }
}
