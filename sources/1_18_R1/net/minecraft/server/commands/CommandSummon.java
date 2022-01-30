package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntitySummon;
import net.minecraft.commands.arguments.ArgumentNBTTag;
import net.minecraft.commands.arguments.coordinates.ArgumentVec3;
import net.minecraft.commands.synchronization.CompletionProviders;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class CommandSummon {

    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.summon.failed"));
    private static final SimpleCommandExceptionType ERROR_DUPLICATE_UUID = new SimpleCommandExceptionType(new ChatMessage("commands.summon.failed.uuid"));
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(new ChatMessage("commands.summon.invalidPosition"));

    public CommandSummon() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("summon").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("entity", ArgumentEntitySummon.id()).suggests(CompletionProviders.SUMMONABLE_ENTITIES).executes((commandcontext) -> {
            return spawnEntity((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntitySummon.getSummonableEntity(commandcontext, "entity"), ((CommandListenerWrapper) commandcontext.getSource()).getPosition(), new NBTTagCompound(), true);
        })).then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("pos", ArgumentVec3.vec3()).executes((commandcontext) -> {
            return spawnEntity((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntitySummon.getSummonableEntity(commandcontext, "entity"), ArgumentVec3.getVec3(commandcontext, "pos"), new NBTTagCompound(), true);
        })).then(net.minecraft.commands.CommandDispatcher.argument("nbt", ArgumentNBTTag.compoundTag()).executes((commandcontext) -> {
            return spawnEntity((CommandListenerWrapper) commandcontext.getSource(), ArgumentEntitySummon.getSummonableEntity(commandcontext, "entity"), ArgumentVec3.getVec3(commandcontext, "pos"), ArgumentNBTTag.getCompoundTag(commandcontext, "nbt"), false);
        })))));
    }

    private static int spawnEntity(CommandListenerWrapper commandlistenerwrapper, MinecraftKey minecraftkey, Vec3D vec3d, NBTTagCompound nbttagcompound, boolean flag) throws CommandSyntaxException {
        BlockPosition blockposition = new BlockPosition(vec3d);

        if (!World.isInSpawnableBounds(blockposition)) {
            throw CommandSummon.INVALID_POSITION.create();
        } else {
            NBTTagCompound nbttagcompound1 = nbttagcompound.copy();

            nbttagcompound1.putString("id", minecraftkey.toString());
            WorldServer worldserver = commandlistenerwrapper.getLevel();
            Entity entity = EntityTypes.loadEntityRecursive(nbttagcompound1, worldserver, (entity1) -> {
                entity1.moveTo(vec3d.x, vec3d.y, vec3d.z, entity1.getYRot(), entity1.getXRot());
                return entity1;
            });

            if (entity == null) {
                throw CommandSummon.ERROR_FAILED.create();
            } else {
                if (flag && entity instanceof EntityInsentient) {
                    ((EntityInsentient) entity).finalizeSpawn(commandlistenerwrapper.getLevel(), commandlistenerwrapper.getLevel().getCurrentDifficultyAt(entity.blockPosition()), EnumMobSpawn.COMMAND, (GroupDataEntity) null, (NBTTagCompound) null);
                }

                if (!worldserver.tryAddFreshEntityWithPassengers(entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.COMMAND)) { // CraftBukkit - pass a spawn reason of "COMMAND"
                    throw CommandSummon.ERROR_DUPLICATE_UUID.create();
                } else {
                    commandlistenerwrapper.sendSuccess(new ChatMessage("commands.summon.success", new Object[]{entity.getDisplayName()}), true);
                    return 1;
                }
            }
        }
    }
}
