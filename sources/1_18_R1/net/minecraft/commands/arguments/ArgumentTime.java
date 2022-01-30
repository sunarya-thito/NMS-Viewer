package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.ICompletionProvider;
import net.minecraft.network.chat.ChatMessage;

public class ArgumentTime implements ArgumentType<Integer> {

    private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
    private static final SimpleCommandExceptionType ERROR_INVALID_UNIT = new SimpleCommandExceptionType(new ChatMessage("argument.time.invalid_unit"));
    private static final DynamicCommandExceptionType ERROR_INVALID_TICK_COUNT = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("argument.time.invalid_tick_count", new Object[]{object});
    });
    private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap();

    public ArgumentTime() {}

    public static ArgumentTime time() {
        return new ArgumentTime();
    }

    public Integer parse(StringReader stringreader) throws CommandSyntaxException {
        float f = stringreader.readFloat();
        String s = stringreader.readUnquotedString();
        int i = ArgumentTime.UNITS.getOrDefault(s, 0);

        if (i == 0) {
            throw ArgumentTime.ERROR_INVALID_UNIT.create();
        } else {
            int j = Math.round(f * (float) i);

            if (j < 0) {
                throw ArgumentTime.ERROR_INVALID_TICK_COUNT.create(j);
            } else {
                return j;
            }
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandcontext, SuggestionsBuilder suggestionsbuilder) {
        StringReader stringreader = new StringReader(suggestionsbuilder.getRemaining());

        try {
            stringreader.readFloat();
        } catch (CommandSyntaxException commandsyntaxexception) {
            return suggestionsbuilder.buildFuture();
        }

        return ICompletionProvider.suggest((Iterable) ArgumentTime.UNITS.keySet(), suggestionsbuilder.createOffset(suggestionsbuilder.getStart() + stringreader.getCursor()));
    }

    public Collection<String> getExamples() {
        return ArgumentTime.EXAMPLES;
    }

    static {
        ArgumentTime.UNITS.put("d", 24000);
        ArgumentTime.UNITS.put("s", 20);
        ArgumentTime.UNITS.put("t", 1);
        ArgumentTime.UNITS.put("", 1);
    }
}
