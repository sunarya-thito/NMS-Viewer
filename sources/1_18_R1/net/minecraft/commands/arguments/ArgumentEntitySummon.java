package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.IRegistry;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.EntityTypes;

public class ArgumentEntitySummon implements ArgumentType<MinecraftKey> {

    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_ENTITY = new DynamicCommandExceptionType((object) -> {
        return new ChatMessage("entity.notFound", new Object[]{object});
    });

    public ArgumentEntitySummon() {}

    public static ArgumentEntitySummon id() {
        return new ArgumentEntitySummon();
    }

    public static MinecraftKey getSummonableEntity(CommandContext<CommandListenerWrapper> commandcontext, String s) throws CommandSyntaxException {
        return verifyCanSummon((MinecraftKey) commandcontext.getArgument(s, MinecraftKey.class));
    }

    private static MinecraftKey verifyCanSummon(MinecraftKey minecraftkey) throws CommandSyntaxException {
        IRegistry.ENTITY_TYPE.getOptional(minecraftkey).filter(EntityTypes::canSummon).orElseThrow(() -> {
            return ArgumentEntitySummon.ERROR_UNKNOWN_ENTITY.create(minecraftkey);
        });
        return minecraftkey;
    }

    public MinecraftKey parse(StringReader stringreader) throws CommandSyntaxException {
        return verifyCanSummon(MinecraftKey.read(stringreader));
    }

    public Collection<String> getExamples() {
        return ArgumentEntitySummon.EXAMPLES;
    }
}
