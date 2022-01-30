package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.commands.arguments.coordinates.ArgumentVec2;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.scores.ScoreboardTeamBase;

public class CommandSpreadPlayers {

    private static final int MAX_ITERATION_COUNT = 10000;
    private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_TEAMS = new Dynamic4CommandExceptionType((object, object1, object2, object3) -> {
        return new ChatMessage("commands.spreadplayers.failed.teams", new Object[]{object, object1, object2, object3});
    });
    private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_ENTITIES = new Dynamic4CommandExceptionType((object, object1, object2, object3) -> {
        return new ChatMessage("commands.spreadplayers.failed.entities", new Object[]{object, object1, object2, object3});
    });

    public CommandSpreadPlayers() {}

    public static void a(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.a("spreadplayers").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.a("center", (ArgumentType) ArgumentVec2.a()).then(net.minecraft.commands.CommandDispatcher.a("spreadDistance", (ArgumentType) FloatArgumentType.floatArg(0.0F)).then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.a("maxRange", (ArgumentType) FloatArgumentType.floatArg(1.0F)).then(net.minecraft.commands.CommandDispatcher.a("respectTeams", (ArgumentType) BoolArgumentType.bool()).then(net.minecraft.commands.CommandDispatcher.a("targets", (ArgumentType) ArgumentEntity.multipleEntities()).executes((commandcontext) -> {
            return a((CommandListenerWrapper) commandcontext.getSource(), ArgumentVec2.a(commandcontext, "center"), FloatArgumentType.getFloat(commandcontext, "spreadDistance"), FloatArgumentType.getFloat(commandcontext, "maxRange"), ((CommandListenerWrapper) commandcontext.getSource()).getWorld().getMaxBuildHeight(), BoolArgumentType.getBool(commandcontext, "respectTeams"), ArgumentEntity.b(commandcontext, "targets"));
        })))).then(net.minecraft.commands.CommandDispatcher.a("under").then(net.minecraft.commands.CommandDispatcher.a("maxHeight", (ArgumentType) IntegerArgumentType.integer(0)).then(net.minecraft.commands.CommandDispatcher.a("respectTeams", (ArgumentType) BoolArgumentType.bool()).then(net.minecraft.commands.CommandDispatcher.a("targets", (ArgumentType) ArgumentEntity.multipleEntities()).executes((commandcontext) -> {
            return a((CommandListenerWrapper) commandcontext.getSource(), ArgumentVec2.a(commandcontext, "center"), FloatArgumentType.getFloat(commandcontext, "spreadDistance"), FloatArgumentType.getFloat(commandcontext, "maxRange"), IntegerArgumentType.getInteger(commandcontext, "maxHeight"), BoolArgumentType.getBool(commandcontext, "respectTeams"), ArgumentEntity.b(commandcontext, "targets"));
        })))))))));
    }

    private static int a(CommandListenerWrapper commandlistenerwrapper, Vec2F vec2f, float f, float f1, int i, boolean flag, Collection<? extends Entity> collection) throws CommandSyntaxException {
        Random random = new Random();
        double d0 = (double) (vec2f.x - f1);
        double d1 = (double) (vec2f.y - f1);
        double d2 = (double) (vec2f.x + f1);
        double d3 = (double) (vec2f.y + f1);
        CommandSpreadPlayers.a[] acommandspreadplayers_a = a(random, flag ? a(collection) : collection.size(), d0, d1, d2, d3);

        a(vec2f, (double) f, commandlistenerwrapper.getWorld(), random, d0, d1, d2, d3, i, acommandspreadplayers_a, flag);
        double d4 = a(collection, commandlistenerwrapper.getWorld(), acommandspreadplayers_a, i, flag);

        commandlistenerwrapper.sendMessage(new ChatMessage("commands.spreadplayers.success." + (flag ? "teams" : "entities"), new Object[]{acommandspreadplayers_a.length, vec2f.x, vec2f.y, String.format(Locale.ROOT, "%.2f", d4)}), true);
        return acommandspreadplayers_a.length;
    }

    private static int a(Collection<? extends Entity> collection) {
        Set<ScoreboardTeamBase> set = Sets.newHashSet();
        Iterator iterator = collection.iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();

            if (entity instanceof EntityHuman) {
                set.add(entity.getScoreboardTeam());
            } else {
                set.add((ScoreboardTeamBase) null); // CraftBukkit - decompile error
            }
        }

        return set.size();
    }

    private static void a(Vec2F vec2f, double d0, WorldServer worldserver, Random random, double d1, double d2, double d3, double d4, int i, CommandSpreadPlayers.a[] acommandspreadplayers_a, boolean flag) throws CommandSyntaxException {
        boolean flag1 = true;
        double d5 = 3.4028234663852886E38D;

        int j;

        for (j = 0; j < 10000 && flag1; ++j) {
            flag1 = false;
            d5 = 3.4028234663852886E38D;

            int k;
            CommandSpreadPlayers.a commandspreadplayers_a;

            for (int l = 0; l < acommandspreadplayers_a.length; ++l) {
                CommandSpreadPlayers.a commandspreadplayers_a1 = acommandspreadplayers_a[l];

                k = 0;
                commandspreadplayers_a = new CommandSpreadPlayers.a();

                for (int i1 = 0; i1 < acommandspreadplayers_a.length; ++i1) {
                    if (l != i1) {
                        CommandSpreadPlayers.a commandspreadplayers_a2 = acommandspreadplayers_a[i1];
                        double d6 = commandspreadplayers_a1.a(commandspreadplayers_a2);

                        d5 = Math.min(d6, d5);
                        if (d6 < d0) {
                            ++k;
                            commandspreadplayers_a.x += commandspreadplayers_a2.x - commandspreadplayers_a1.x;
                            commandspreadplayers_a.z += commandspreadplayers_a2.z - commandspreadplayers_a1.z;
                        }
                    }
                }

                if (k > 0) {
                    commandspreadplayers_a.x /= (double) k;
                    commandspreadplayers_a.z /= (double) k;
                    double d7 = commandspreadplayers_a.b();

                    if (d7 > 0.0D) {
                        commandspreadplayers_a.a();
                        commandspreadplayers_a1.b(commandspreadplayers_a);
                    } else {
                        commandspreadplayers_a1.a(random, d1, d2, d3, d4);
                    }

                    flag1 = true;
                }

                if (commandspreadplayers_a1.a(d1, d2, d3, d4)) {
                    flag1 = true;
                }
            }

            if (!flag1) {
                CommandSpreadPlayers.a[] acommandspreadplayers_a1 = acommandspreadplayers_a;
                int j1 = acommandspreadplayers_a.length;

                for (k = 0; k < j1; ++k) {
                    commandspreadplayers_a = acommandspreadplayers_a1[k];
                    if (!commandspreadplayers_a.b(worldserver, i)) {
                        commandspreadplayers_a.a(random, d1, d2, d3, d4);
                        flag1 = true;
                    }
                }
            }
        }

        if (d5 == 3.4028234663852886E38D) {
            d5 = 0.0D;
        }

        if (j >= 10000) {
            if (flag) {
                throw CommandSpreadPlayers.ERROR_FAILED_TO_SPREAD_TEAMS.create(acommandspreadplayers_a.length, vec2f.x, vec2f.y, String.format(Locale.ROOT, "%.2f", d5));
            } else {
                throw CommandSpreadPlayers.ERROR_FAILED_TO_SPREAD_ENTITIES.create(acommandspreadplayers_a.length, vec2f.x, vec2f.y, String.format(Locale.ROOT, "%.2f", d5));
            }
        }
    }

    private static double a(Collection<? extends Entity> collection, WorldServer worldserver, CommandSpreadPlayers.a[] acommandspreadplayers_a, int i, boolean flag) {
        double d0 = 0.0D;
        int j = 0;
        Map<ScoreboardTeamBase, CommandSpreadPlayers.a> map = Maps.newHashMap();

        double d1;

        for (Iterator iterator = collection.iterator(); iterator.hasNext(); d0 += d1) {
            Entity entity = (Entity) iterator.next();
            CommandSpreadPlayers.a commandspreadplayers_a;

            if (flag) {
                ScoreboardTeamBase scoreboardteambase = entity instanceof EntityHuman ? entity.getScoreboardTeam() : null;

                if (!map.containsKey(scoreboardteambase)) {
                    map.put(scoreboardteambase, acommandspreadplayers_a[j++]);
                }

                commandspreadplayers_a = (CommandSpreadPlayers.a) map.get(scoreboardteambase);
            } else {
                commandspreadplayers_a = acommandspreadplayers_a[j++];
            }

            entity.enderTeleportAndLoad((double) MathHelper.floor(commandspreadplayers_a.x) + 0.5D, (double) commandspreadplayers_a.a(worldserver, i), (double) MathHelper.floor(commandspreadplayers_a.z) + 0.5D);
            d1 = Double.MAX_VALUE;
            CommandSpreadPlayers.a[] acommandspreadplayers_a1 = acommandspreadplayers_a;
            int k = acommandspreadplayers_a.length;

            for (int l = 0; l < k; ++l) {
                CommandSpreadPlayers.a commandspreadplayers_a1 = acommandspreadplayers_a1[l];

                if (commandspreadplayers_a != commandspreadplayers_a1) {
                    double d2 = commandspreadplayers_a.a(commandspreadplayers_a1);

                    d1 = Math.min(d2, d1);
                }
            }
        }

        if (collection.size() < 2) {
            return 0.0D;
        } else {
            d0 /= (double) collection.size();
            return d0;
        }
    }

    private static CommandSpreadPlayers.a[] a(Random random, int i, double d0, double d1, double d2, double d3) {
        CommandSpreadPlayers.a[] acommandspreadplayers_a = new CommandSpreadPlayers.a[i];

        for (int j = 0; j < acommandspreadplayers_a.length; ++j) {
            CommandSpreadPlayers.a commandspreadplayers_a = new CommandSpreadPlayers.a();

            commandspreadplayers_a.a(random, d0, d1, d2, d3);
            acommandspreadplayers_a[j] = commandspreadplayers_a;
        }

        return acommandspreadplayers_a;
    }

    private static class a {

        double x;
        double z;

        a() {}

        double a(CommandSpreadPlayers.a commandspreadplayers_a) {
            double d0 = this.x - commandspreadplayers_a.x;
            double d1 = this.z - commandspreadplayers_a.z;

            return Math.sqrt(d0 * d0 + d1 * d1);
        }

        void a() {
            double d0 = this.b();

            this.x /= d0;
            this.z /= d0;
        }

        double b() {
            return Math.sqrt(this.x * this.x + this.z * this.z);
        }

        public void b(CommandSpreadPlayers.a commandspreadplayers_a) {
            this.x -= commandspreadplayers_a.x;
            this.z -= commandspreadplayers_a.z;
        }

        public boolean a(double d0, double d1, double d2, double d3) {
            boolean flag = false;

            if (this.x < d0) {
                this.x = d0;
                flag = true;
            } else if (this.x > d2) {
                this.x = d2;
                flag = true;
            }

            if (this.z < d1) {
                this.z = d1;
                flag = true;
            } else if (this.z > d3) {
                this.z = d3;
                flag = true;
            }

            return flag;
        }

        public int a(IBlockAccess iblockaccess, int i) {
            BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition(this.x, (double) (i + 1), this.z);
            boolean flag = iblockaccess.getType(blockposition_mutableblockposition).isAir();

            blockposition_mutableblockposition.c(EnumDirection.DOWN);

            boolean flag1;

            for (boolean flag2 = iblockaccess.getType(blockposition_mutableblockposition).isAir(); blockposition_mutableblockposition.getY() > iblockaccess.getMinBuildHeight(); flag2 = flag1) {
                blockposition_mutableblockposition.c(EnumDirection.DOWN);
                flag1 = getType(iblockaccess, blockposition_mutableblockposition).isAir(); // CraftBukkit
                if (!flag1 && flag2 && flag) {
                    return blockposition_mutableblockposition.getY() + 1;
                }

                flag = flag2;
            }

            return i + 1;
        }

        public boolean b(IBlockAccess iblockaccess, int i) {
            BlockPosition blockposition = new BlockPosition(this.x, (double) (this.a(iblockaccess, i) - 1), this.z);
            IBlockData iblockdata = getType(iblockaccess, blockposition); // CraftBukkit
            Material material = iblockdata.getMaterial();

            return blockposition.getY() < i && !material.isLiquid() && material != Material.FIRE;
        }

        public void a(Random random, double d0, double d1, double d2, double d3) {
            this.x = MathHelper.a(random, d0, d2);
            this.z = MathHelper.a(random, d1, d3);
        }

        // CraftBukkit start - add a version of getType which force loads chunks
        private static IBlockData getType(IBlockAccess iblockaccess, BlockPosition position) {
            ((WorldServer) iblockaccess).getChunkProvider().getChunkAt(position.getX() >> 4, position.getZ() >> 4, true);
            return iblockaccess.getType(position);
        }
        // CraftBukkit end
    }
}
