package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.CustomFunction;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.commands.arguments.ArgumentTime;
import net.minecraft.commands.arguments.item.ArgumentTag;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.timers.CustomFunctionCallback;
import net.minecraft.world.level.timers.CustomFunctionCallbackTag;
import net.minecraft.world.level.timers.CustomFunctionCallbackTimerQueue;

public class CommandSchedule {

    private static final SimpleCommandExceptionType ERROR_SAME_TICK = new SimpleCommandExceptionType(new ChatMessage("commands.schedule.same_tick"));
    private static final DynamicCommandExceptionType ERROR_CANT_REMOVE = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("commands.schedule.cleared.failure", new Object[]{object});
    });
    private static final SuggestionProvider<CommandListenerWrapper> SUGGEST_SCHEDULE = (commandcontext, suggestionsbuilder) -> {
        return ICompletionProvider.suggest((Iterable) ((CommandListenerWrapper) commandcontext.getSource()).getServer().getWorldData().overworldData().getScheduledEvents().getEventsIds(), suggestionsbuilder);
    };

    public CommandSchedule() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("schedule").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.literal("function").then(net.minecraft.commands.CommandDispatcher.argument("function", ArgumentTag.functions()).suggests(CommandFunction.SUGGEST_FUNCTION).then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("time", ArgumentTime.time()).executes((commandcontext) -> {
            return schedule((CommandListenerWrapper) commandcontext.getSource(), ArgumentTag.getFunctionOrTag(commandcontext, "function"), IntegerArgumentType.getInteger(commandcontext, "time"), true);
        })).then(net.minecraft.commands.CommandDispatcher.literal("append").executes((commandcontext) -> {
            return schedule((CommandListenerWrapper) commandcontext.getSource(), ArgumentTag.getFunctionOrTag(commandcontext, "function"), IntegerArgumentType.getInteger(commandcontext, "time"), false);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("replace").executes((commandcontext) -> {
            return schedule((CommandListenerWrapper) commandcontext.getSource(), ArgumentTag.getFunctionOrTag(commandcontext, "function"), IntegerArgumentType.getInteger(commandcontext, "time"), true);
        })))))).then(net.minecraft.commands.CommandDispatcher.literal("clear").then(net.minecraft.commands.CommandDispatcher.argument("function", StringArgumentType.greedyString()).suggests(CommandSchedule.SUGGEST_SCHEDULE).executes((commandcontext) -> {
            return remove((CommandListenerWrapper) commandcontext.getSource(), StringArgumentType.getString(commandcontext, "function"));
        }))));
    }

    private static int schedule(CommandListenerWrapper commandlistenerwrapper, Pair<MinecraftKey, Either<CustomFunction, Tag<CustomFunction>>> pair, int i, boolean flag) throws CommandSyntaxException {
        if (i == 0) {
            throw CommandSchedule.ERROR_SAME_TICK.create();
        } else {
            long j = commandlistenerwrapper.getLevel().getGameTime() + (long) i;
            MinecraftKey minecraftkey = (MinecraftKey) pair.getFirst();
            CustomFunctionCallbackTimerQueue<MinecraftServer> customfunctioncallbacktimerqueue = commandlistenerwrapper.getLevel().serverLevelData.overworldData().getScheduledEvents(); // CraftBukkit - SPIGOT-6667: Use world specific function timer

            ((Either) pair.getSecond()).ifLeft((customfunction) -> {
                String s = minecraftkey.toString();

                if (flag) {
                    customfunctioncallbacktimerqueue.remove(s);
                }

                customfunctioncallbacktimerqueue.schedule(s, j, new CustomFunctionCallback(minecraftkey));
                commandlistenerwrapper.sendSuccess(new ChatMessage("commands.schedule.created.function", new Object[]{minecraftkey, i, j}), true);
            }).ifRight((tag) -> {
                String s = "#" + minecraftkey;

                if (flag) {
                    customfunctioncallbacktimerqueue.remove(s);
                }

                customfunctioncallbacktimerqueue.schedule(s, j, new CustomFunctionCallbackTag(minecraftkey));
                commandlistenerwrapper.sendSuccess(new ChatMessage("commands.schedule.created.tag", new Object[]{minecraftkey, i, j}), true);
            });
            return Math.floorMod(j, Integer.MAX_VALUE);
        }
    }

    private static int remove(CommandListenerWrapper commandlistenerwrapper, String s) throws CommandSyntaxException {
        int i = commandlistenerwrapper.getServer().getWorldData().overworldData().getScheduledEvents().remove(s);

        if (i == 0) {
            throw CommandSchedule.ERROR_CANT_REMOVE.create(s);
        } else {
            commandlistenerwrapper.sendSuccess(new ChatMessage("commands.schedule.cleared.success", new Object[]{i, s}), true);
            return i;
        }
    }
}
