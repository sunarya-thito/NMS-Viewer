package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
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

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("spreadplayers").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.argument("center", ArgumentVec2.vec2()).then(net.minecraft.commands.CommandDispatcher.argument("spreadDistance", FloatArgumentType.floatArg(0.0F)).then(((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("maxRange", FloatArgumentType.floatArg(1.0F)).then(net.minecraft.commands.CommandDispatcher.argument("respectTeams", BoolArgumentType.bool()).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.entities()).executes((commandcontext) -> {
            return spreadPlayers((CommandListenerWrapper) commandcontext.getSource(), ArgumentVec2.getVec2(commandcontext, "center"), FloatArgumentType.getFloat(commandcontext, "spreadDistance"), FloatArgumentType.getFloat(commandcontext, "maxRange"), ((CommandListenerWrapper) commandcontext.getSource()).getLevel().getMaxBuildHeight(), BoolArgumentType.getBool(commandcontext, "respectTeams"), ArgumentEntity.getEntities(commandcontext, "targets"));
        })))).then(net.minecraft.commands.CommandDispatcher.literal("under").then(net.minecraft.commands.CommandDispatcher.argument("maxHeight", IntegerArgumentType.integer(0)).then(net.minecraft.commands.CommandDispatcher.argument("respectTeams", BoolArgumentType.bool()).then(net.minecraft.commands.CommandDispatcher.argument("targets", ArgumentEntity.entities()).executes((commandcontext) -> {
            return spreadPlayers((CommandListenerWrapper) commandcontext.getSource(), ArgumentVec2.getVec2(commandcontext, "center"), FloatArgumentType.getFloat(commandcontext, "spreadDistance"), FloatArgumentType.getFloat(commandcontext, "maxRange"), IntegerArgumentType.getInteger(commandcontext, "maxHeight"), BoolArgumentType.getBool(commandcontext, "respectTeams"), ArgumentEntity.getEntities(commandcontext, "targets"));
        })))))))));
    }

    private static int spreadPlayers(CommandListenerWrapper commandlistenerwrapper, Vec2F vec2f, float f, float f1, int i, boolean flag, Collection<? extends Entity> collection) throws CommandSyntaxException {
        Random random = new Random();
        double d0 = (double) (vec2f.x - f1);
        double d1 = (double) (vec2f.y - f1);
        double d2 = (double) (vec2f.x + f1);
        double d3 = (double) (vec2f.y + f1);
        CommandSpreadPlayers.a[] acommandspreadplayers_a = createInitialPositions(random, flag ? getNumberOfTeams(collection) : collection.size(), d0, d1, d2, d3);

        spreadPositions(vec2f, (double) f, commandlistenerwrapper.getLevel(), random, d0, d1, d2, d3, i, acommandspreadplayers_a, flag);
        double d4 = setPlayerPositions(collection, commandlistenerwrapper.getLevel(), acommandspreadplayers_a, i, flag);

        commandlistenerwrapper.sendSuccess(new ChatMessage("commands.spreadplayers.success." + (flag ? "teams" : "entities"), new Object[]{acommandspreadplayers_a.length, vec2f.x, vec2f.y, String.format(Locale.ROOT, "%.2f", d4)}), true);
        return acommandspreadplayers_a.length;
    }

    private static int getNumberOfTeams(Collection<? extends Entity> collection) {
        Set<ScoreboardTeamBase> set = Sets.newHashSet();
        Iterator iterator = collection.iterator();

        while (iterator.hasNext()) {
            Entity entity = (Entity) iterator.next();

            if (entity instanceof EntityHuman) {
                set.add(entity.getTeam());
            } else {
                set.add((ScoreboardTeamBase) null); // CraftBukkit - decompile error
            }
        }

        return set.size();
    }

    private static void spreadPositions(Vec2F vec2f, double d0, WorldServer worldserver, Random random, double d1, double d2, double d3, double d4, int i, CommandSpreadPlayers.a[] acommandspreadplayers_a, boolean flag) throws CommandSyntaxException {
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
                        double d6 = commandspreadplayers_a1.dist(commandspreadplayers_a2);

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
                    double d7 = commandspreadplayers_a.getLength();

                    if (d7 > 0.0D) {
                        commandspreadplayers_a.normalize();
                        commandspreadplayers_a1.moveAway(commandspreadplayers_a);
                    } else {
                        commandspreadplayers_a1.randomize(random, d1, d2, d3, d4);
                    }

                    flag1 = true;
                }

                if (commandspreadplayers_a1.clamp(d1, d2, d3, d4)) {
                    flag1 = true;
                }
            }

            if (!flag1) {
                CommandSpreadPlayers.a[] acommandspreadplayers_a1 = acommandspreadplayers_a;
                int j1 = acommandspreadplayers_a.length;

                for (k = 0; k < j1; ++k) {
                    commandspreadplayers_a = acommandspreadplayers_a1[k];
                    if (!commandspreadplayers_a.isSafe(worldserver, i)) {
                        commandspreadplayers_a.randomize(random, d1, d2, d3, d4);
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

    private static double setPlayerPositions(Collection<? extends Entity> collection, WorldServer worldserver, CommandSpreadPlayers.a[] acommandspreadplayers_a, int i, boolean flag) {
        double d0 = 0.0D;
        int j = 0;
        Map<ScoreboardTeamBase, CommandSpreadPlayers.a> map = Maps.newHashMap();

        double d1;

        for (Iterator iterator = collection.iterator(); iterator.hasNext(); d0 += d1) {
            Entity entity = (Entity) iterator.next();
            CommandSpreadPlayers.a commandspreadplayers_a;

            if (flag) {
                ScoreboardTeamBase scoreboardteambase = entity instanceof EntityHuman ? entity.getTeam() : null;

                if (!map.containsKey(scoreboardteambase)) {
                    map.put(scoreboardteambase, acommandspreadplayers_a[j++]);
                }

                commandspreadplayers_a = (CommandSpreadPlayers.a) map.get(scoreboardteambase);
            } else {
                commandspreadplayers_a = acommandspreadplayers_a[j++];
            }

            entity.teleportToWithTicket((double) MathHelper.floor(commandspreadplayers_a.x) + 0.5D, (double) commandspreadplayers_a.getSpawnY(worldserver, i), (double) MathHelper.floor(commandspreadplayers_a.z) + 0.5D);
            d1 = Double.MAX_VALUE;
            CommandSpreadPlayers.a[] acommandspreadplayers_a1 = acommandspreadplayers_a;
            int k = acommandspreadplayers_a.length;

            for (int l = 0; l < k; ++l) {
                CommandSpreadPlayers.a commandspreadplayers_a1 = acommandspreadplayers_a1[l];

                if (commandspreadplayers_a != commandspreadplayers_a1) {
                    double d2 = commandspreadplayers_a.dist(commandspreadplayers_a1);

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

    private static CommandSpreadPlayers.a[] createInitialPositions(Random random, int i, double d0, double d1, double d2, double d3) {
        CommandSpreadPlayers.a[] acommandspreadplayers_a = new CommandSpreadPlayers.a[i];

        for (int j = 0; j < acommandspreadplayers_a.length; ++j) {
            CommandSpreadPlayers.a commandspreadplayers_a = new CommandSpreadPlayers.a();

            commandspreadplayers_a.randomize(random, d0, d1, d2, d3);
            acommandspreadplayers_a[j] = commandspreadplayers_a;
        }

        return acommandspreadplayers_a;
    }

    private static class a {

        double x;
        double z;

        a() {}

        double dist(CommandSpreadPlayers.a commandspreadplayers_a) {
            double d0 = this.x - commandspreadplayers_a.x;
            double d1 = this.z - commandspreadplayers_a.z;

            return Math.sqrt(d0 * d0 + d1 * d1);
        }

        void normalize() {
            double d0 = this.getLength();

            this.x /= d0;
            this.z /= d0;
        }

        double getLength() {
            return Math.sqrt(this.x * this.x + this.z * this.z);
        }

        public void moveAway(CommandSpreadPlayers.a commandspreadplayers_a) {
            this.x -= commandspreadplayers_a.x;
            this.z -= commandspreadplayers_a.z;
        }

        public boolean clamp(double d0, double d1, double d2, double d3) {
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

        public int getSpawnY(IBlockAccess iblockaccess, int i) {
            BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition(this.x, (double) (i + 1), this.z);
            boolean flag = iblockaccess.getBlockState(blockposition_mutableblockposition).isAir();

            blockposition_mutableblockposition.move(EnumDirection.DOWN);

            boolean flag1;

            for (boolean flag2 = iblockaccess.getBlockState(blockposition_mutableblockposition).isAir(); blockposition_mutableblockposition.getY() > iblockaccess.getMinBuildHeight(); flag2 = flag1) {
                blockposition_mutableblockposition.move(EnumDirection.DOWN);
                flag1 = getBlockState(iblockaccess, blockposition_mutableblockposition).isAir(); // CraftBukkit
                if (!flag1 && flag2 && flag) {
                    return blockposition_mutableblockposition.getY() + 1;
                }

                flag = flag2;
            }

            return i + 1;
        }

        public boolean isSafe(IBlockAccess iblockaccess, int i) {
            BlockPosition blockposition = new BlockPosition(this.x, (double) (this.getSpawnY(iblockaccess, i) - 1), this.z);
            IBlockData iblockdata = getBlockState(iblockaccess, blockposition); // CraftBukkit
            Material material = iblockdata.getMaterial();

            return blockposition.getY() < i && !material.isLiquid() && material != Material.FIRE;
        }

        public void randomize(Random random, double d0, double d1, double d2, double d3) {
            this.x = MathHelper.nextDouble(random, d0, d2);
            this.z = MathHelper.nextDouble(random, d1, d3);
        }

        // CraftBukkit start - add a version of getBlockState which force loads chunks
        private static IBlockData getBlockState(IBlockAccess iblockaccess, BlockPosition position) {
            ((WorldServer) iblockaccess).getChunkSource().getChunk(position.getX() >> 4, position.getZ() >> 4, true);
            return iblockaccess.getBlockState(position);
        }
        // CraftBukkit end
    }
}
