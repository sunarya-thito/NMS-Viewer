package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.blocks.ArgumentBlockPredicate;
import net.minecraft.commands.arguments.coordinates.ArgumentPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.pattern.ShapeDetectorBlock;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;

public class CommandClone {

    private static final int MAX_CLONE_AREA = 32768;
    private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(new ChatMessage("commands.clone.overlap"));
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((object, object1) -> {
        return new ChatMessage("commands.clone.toobig", new Object[]{object, object1});
    });
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(new ChatMessage("commands.clone.failed"));
    public static final Predicate<ShapeDetectorBlock> FILTER_AIR = (shapedetectorblock) -> {
        return !shapedetectorblock.getState().isAir();
    };

    public CommandClone() {}

    public static void register(CommandDispatcher<CommandListenerWrapper> commanddispatcher) {
        commanddispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("clone").requires((commandlistenerwrapper) -> {
            return commandlistenerwrapper.hasPermission(2);
        })).then(net.minecraft.commands.CommandDispatcher.argument("begin", ArgumentPosition.blockPos()).then(net.minecraft.commands.CommandDispatcher.argument("end", ArgumentPosition.blockPos()).then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("destination", ArgumentPosition.blockPos()).executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), (shapedetectorblock) -> {
                return true;
            }, CommandClone.Mode.NORMAL);
        })).then(((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("replace").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), (shapedetectorblock) -> {
                return true;
            }, CommandClone.Mode.NORMAL);
        })).then(net.minecraft.commands.CommandDispatcher.literal("force").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), (shapedetectorblock) -> {
                return true;
            }, CommandClone.Mode.FORCE);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("move").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), (shapedetectorblock) -> {
                return true;
            }, CommandClone.Mode.MOVE);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("normal").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), (shapedetectorblock) -> {
                return true;
            }, CommandClone.Mode.NORMAL);
        })))).then(((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) net.minecraft.commands.CommandDispatcher.literal("masked").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), CommandClone.FILTER_AIR, CommandClone.Mode.NORMAL);
        })).then(net.minecraft.commands.CommandDispatcher.literal("force").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), CommandClone.FILTER_AIR, CommandClone.Mode.FORCE);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("move").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), CommandClone.FILTER_AIR, CommandClone.Mode.MOVE);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("normal").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), CommandClone.FILTER_AIR, CommandClone.Mode.NORMAL);
        })))).then(net.minecraft.commands.CommandDispatcher.literal("filtered").then(((RequiredArgumentBuilder) ((RequiredArgumentBuilder) ((RequiredArgumentBuilder) net.minecraft.commands.CommandDispatcher.argument("filter", ArgumentBlockPredicate.blockPredicate()).executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), ArgumentBlockPredicate.getBlockPredicate(commandcontext, "filter"), CommandClone.Mode.NORMAL);
        })).then(net.minecraft.commands.CommandDispatcher.literal("force").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), ArgumentBlockPredicate.getBlockPredicate(commandcontext, "filter"), CommandClone.Mode.FORCE);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("move").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), ArgumentBlockPredicate.getBlockPredicate(commandcontext, "filter"), CommandClone.Mode.MOVE);
        }))).then(net.minecraft.commands.CommandDispatcher.literal("normal").executes((commandcontext) -> {
            return clone((CommandListenerWrapper) commandcontext.getSource(), ArgumentPosition.getLoadedBlockPos(commandcontext, "begin"), ArgumentPosition.getLoadedBlockPos(commandcontext, "end"), ArgumentPosition.getLoadedBlockPos(commandcontext, "destination"), ArgumentBlockPredicate.getBlockPredicate(commandcontext, "filter"), CommandClone.Mode.NORMAL);
        }))))))));
    }

    private static int clone(CommandListenerWrapper commandlistenerwrapper, BlockPosition blockposition, BlockPosition blockposition1, BlockPosition blockposition2, Predicate<ShapeDetectorBlock> predicate, CommandClone.Mode commandclone_mode) throws CommandSyntaxException {
        StructureBoundingBox structureboundingbox = StructureBoundingBox.fromCorners(blockposition, blockposition1);
        BlockPosition blockposition3 = blockposition2.offset(structureboundingbox.getLength());
        StructureBoundingBox structureboundingbox1 = StructureBoundingBox.fromCorners(blockposition2, blockposition3);

        if (!commandclone_mode.canOverlap() && structureboundingbox1.intersects(structureboundingbox)) {
            throw CommandClone.ERROR_OVERLAP.create();
        } else {
            int i = structureboundingbox.getXSpan() * structureboundingbox.getYSpan() * structureboundingbox.getZSpan();

            if (i > 32768) {
                throw CommandClone.ERROR_AREA_TOO_LARGE.create(32768, i);
            } else {
                WorldServer worldserver = commandlistenerwrapper.getLevel();

                if (worldserver.hasChunksAt(blockposition, blockposition1) && worldserver.hasChunksAt(blockposition2, blockposition3)) {
                    List<CommandClone.CommandCloneStoredTileEntity> list = Lists.newArrayList();
                    List<CommandClone.CommandCloneStoredTileEntity> list1 = Lists.newArrayList();
                    List<CommandClone.CommandCloneStoredTileEntity> list2 = Lists.newArrayList();
                    Deque<BlockPosition> deque = Lists.newLinkedList();
                    BlockPosition blockposition4 = new BlockPosition(structureboundingbox1.minX() - structureboundingbox.minX(), structureboundingbox1.minY() - structureboundingbox.minY(), structureboundingbox1.minZ() - structureboundingbox.minZ());

                    int j;

                    for (int k = structureboundingbox.minZ(); k <= structureboundingbox.maxZ(); ++k) {
                        for (int l = structureboundingbox.minY(); l <= structureboundingbox.maxY(); ++l) {
                            for (j = structureboundingbox.minX(); j <= structureboundingbox.maxX(); ++j) {
                                BlockPosition blockposition5 = new BlockPosition(j, l, k);
                                BlockPosition blockposition6 = blockposition5.offset(blockposition4);
                                ShapeDetectorBlock shapedetectorblock = new ShapeDetectorBlock(worldserver, blockposition5, false);
                                IBlockData iblockdata = shapedetectorblock.getState();

                                if (predicate.test(shapedetectorblock)) {
                                    TileEntity tileentity = worldserver.getBlockEntity(blockposition5);

                                    if (tileentity != null) {
                                        NBTTagCompound nbttagcompound = tileentity.saveWithoutMetadata();

                                        list1.add(new CommandClone.CommandCloneStoredTileEntity(blockposition6, iblockdata, nbttagcompound));
                                        deque.addLast(blockposition5);
                                    } else if (!iblockdata.isSolidRender(worldserver, blockposition5) && !iblockdata.isCollisionShapeFullBlock(worldserver, blockposition5)) {
                                        list2.add(new CommandClone.CommandCloneStoredTileEntity(blockposition6, iblockdata, (NBTTagCompound) null));
                                        deque.addFirst(blockposition5);
                                    } else {
                                        list.add(new CommandClone.CommandCloneStoredTileEntity(blockposition6, iblockdata, (NBTTagCompound) null));
                                        deque.addLast(blockposition5);
                                    }
                                }
                            }
                        }
                    }

                    if (commandclone_mode == CommandClone.Mode.MOVE) {
                        Iterator iterator = deque.iterator();

                        BlockPosition blockposition7;

                        while (iterator.hasNext()) {
                            blockposition7 = (BlockPosition) iterator.next();
                            TileEntity tileentity1 = worldserver.getBlockEntity(blockposition7);

                            Clearable.tryClear(tileentity1);
                            worldserver.setBlock(blockposition7, Blocks.BARRIER.defaultBlockState(), 2);
                        }

                        iterator = deque.iterator();

                        while (iterator.hasNext()) {
                            blockposition7 = (BlockPosition) iterator.next();
                            worldserver.setBlock(blockposition7, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }

                    List<CommandClone.CommandCloneStoredTileEntity> list3 = Lists.newArrayList();

                    list3.addAll(list);
                    list3.addAll(list1);
                    list3.addAll(list2);
                    List<CommandClone.CommandCloneStoredTileEntity> list4 = Lists.reverse(list3);
                    Iterator iterator1 = list4.iterator();

                    while (iterator1.hasNext()) {
                        CommandClone.CommandCloneStoredTileEntity commandclone_commandclonestoredtileentity = (CommandClone.CommandCloneStoredTileEntity) iterator1.next();
                        TileEntity tileentity2 = worldserver.getBlockEntity(commandclone_commandclonestoredtileentity.pos);

                        Clearable.tryClear(tileentity2);
                        worldserver.setBlock(commandclone_commandclonestoredtileentity.pos, Blocks.BARRIER.defaultBlockState(), 2);
                    }

                    j = 0;
                    Iterator iterator2 = list3.iterator();

                    CommandClone.CommandCloneStoredTileEntity commandclone_commandclonestoredtileentity1;

                    while (iterator2.hasNext()) {
                        commandclone_commandclonestoredtileentity1 = (CommandClone.CommandCloneStoredTileEntity) iterator2.next();
                        if (worldserver.setBlock(commandclone_commandclonestoredtileentity1.pos, commandclone_commandclonestoredtileentity1.state, 2)) {
                            ++j;
                        }
                    }

                    for (iterator2 = list1.iterator(); iterator2.hasNext(); worldserver.setBlock(commandclone_commandclonestoredtileentity1.pos, commandclone_commandclonestoredtileentity1.state, 2)) {
                        commandclone_commandclonestoredtileentity1 = (CommandClone.CommandCloneStoredTileEntity) iterator2.next();
                        TileEntity tileentity3 = worldserver.getBlockEntity(commandclone_commandclonestoredtileentity1.pos);

                        if (commandclone_commandclonestoredtileentity1.tag != null && tileentity3 != null) {
                            tileentity3.load(commandclone_commandclonestoredtileentity1.tag);
                            tileentity3.setChanged();
                        }
                    }

                    iterator2 = list4.iterator();

                    while (iterator2.hasNext()) {
                        commandclone_commandclonestoredtileentity1 = (CommandClone.CommandCloneStoredTileEntity) iterator2.next();
                        worldserver.blockUpdated(commandclone_commandclonestoredtileentity1.pos, commandclone_commandclonestoredtileentity1.state.getBlock());
                    }

                    worldserver.getBlockTicks().copyArea(structureboundingbox, blockposition4);
                    if (j == 0) {
                        throw CommandClone.ERROR_FAILED.create();
                    } else {
                        commandlistenerwrapper.sendSuccess(new ChatMessage("commands.clone.success", new Object[]{j}), true);
                        return j;
                    }
                } else {
                    throw ArgumentPosition.ERROR_NOT_LOADED.create();
                }
            }
        }
    }

    private static enum Mode {

        FORCE(true), MOVE(true), NORMAL(false);

        private final boolean canOverlap;

        private Mode(boolean flag) {
            this.canOverlap = flag;
        }

        public boolean canOverlap() {
            return this.canOverlap;
        }
    }

    private static class CommandCloneStoredTileEntity {

        public final BlockPosition pos;
        public final IBlockData state;
        @Nullable
        public final NBTTagCompound tag;

        public CommandCloneStoredTileEntity(BlockPosition blockposition, IBlockData iblockdata, @Nullable NBTTagCompound nbttagcompound) {
            this.pos = blockposition;
            this.state = iblockdata;
            this.tag = nbttagcompound;
        }
    }
}
