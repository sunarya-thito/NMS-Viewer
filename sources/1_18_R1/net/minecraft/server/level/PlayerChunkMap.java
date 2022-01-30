package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.protocol.game.PacketPlayOutAttachEntity;
import net.minecraft.network.protocol.game.PacketPlayOutMount;
import net.minecraft.network.protocol.game.PacketPlayOutViewCentre;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CSVWriter;
import net.minecraft.util.MathHelper;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.thread.IAsyncTaskHandler;
import net.minecraft.util.thread.Mailbox;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.boss.EntityComplexPart;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkConverter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunkExtension;
import net.minecraft.world.level.chunk.storage.ChunkRegionLoader;
import net.minecraft.world.level.chunk.storage.IChunkLoader;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.WorldPersistentData;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.world.level.levelgen.GeneratorSettings;
import org.bukkit.entity.Player;
// CraftBukkit end

public class PlayerChunkMap extends IChunkLoader implements PlayerChunk.e {

    private static final byte CHUNK_TYPE_REPLACEABLE = -1;
    private static final byte CHUNK_TYPE_UNKNOWN = 0;
    private static final byte CHUNK_TYPE_FULL = 1;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CHUNK_SAVED_PER_TICK = 200;
    private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
    private static final int MIN_VIEW_DISTANCE = 3;
    public static final int MAX_VIEW_DISTANCE = 33;
    public static final int MAX_CHUNK_DISTANCE = 33 + ChunkStatus.maxDistance();
    public static final int FORCED_TICKET_LEVEL = 31;
    public final Long2ObjectLinkedOpenHashMap<PlayerChunk> updatingChunkMap = new Long2ObjectLinkedOpenHashMap();
    public volatile Long2ObjectLinkedOpenHashMap<PlayerChunk> visibleChunkMap;
    private final Long2ObjectLinkedOpenHashMap<PlayerChunk> pendingUnloads;
    private final LongSet entitiesInLevel;
    public final WorldServer level;
    private final LightEngineThreaded lightEngine;
    private final IAsyncTaskHandler<Runnable> mainThreadExecutor;
    public ChunkGenerator generator;
    private final Supplier<WorldPersistentData> overworldDataStorage;
    private final VillagePlace poiManager;
    public final LongSet toDrop;
    private boolean modified;
    private final ChunkTaskQueueSorter queueSorter;
    private final Mailbox<ChunkTaskQueueSorter.a<Runnable>> worldgenMailbox;
    private final Mailbox<ChunkTaskQueueSorter.a<Runnable>> mainThreadMailbox;
    public final WorldLoadListener progressListener;
    private final ChunkStatusUpdateListener chunkStatusListener;
    public final PlayerChunkMap.a distanceManager;
    private final AtomicInteger tickingGenerated;
    private final DefinedStructureManager structureManager;
    private final String storageName;
    private final PlayerMap playerMap;
    public final Int2ObjectMap<PlayerChunkMap.EntityTracker> entityMap;
    private final Long2ByteMap chunkTypeCache;
    private final Queue<Runnable> unloadQueue;
    int viewDistance;

    // CraftBukkit start - recursion-safe executor for Chunk loadCallback() and unloadCallback()
    public final CallbackExecutor callbackExecutor = new CallbackExecutor();
    public static final class CallbackExecutor implements java.util.concurrent.Executor, Runnable {

        private final java.util.Queue<Runnable> queue = new java.util.ArrayDeque<>();

        @Override
        public void execute(Runnable runnable) {
            queue.add(runnable);
        }

        @Override
        public void run() {
            Runnable task;
            while ((task = queue.poll()) != null) {
                task.run();
            }
        }
    };
    // CraftBukkit end

    public PlayerChunkMap(WorldServer worldserver, Convertable.ConversionSession convertable_conversionsession, DataFixer datafixer, DefinedStructureManager definedstructuremanager, Executor executor, IAsyncTaskHandler<Runnable> iasynctaskhandler, ILightAccess ilightaccess, ChunkGenerator chunkgenerator, WorldLoadListener worldloadlistener, ChunkStatusUpdateListener chunkstatusupdatelistener, Supplier<WorldPersistentData> supplier, int i, boolean flag) {
        super(convertable_conversionsession.getDimensionPath(worldserver.dimension()).resolve("region"), datafixer, flag);
        this.visibleChunkMap = this.updatingChunkMap.clone();
        this.pendingUnloads = new Long2ObjectLinkedOpenHashMap();
        this.entitiesInLevel = new LongOpenHashSet();
        this.toDrop = new LongOpenHashSet();
        this.tickingGenerated = new AtomicInteger();
        this.playerMap = new PlayerMap();
        this.entityMap = new Int2ObjectOpenHashMap();
        this.chunkTypeCache = new Long2ByteOpenHashMap();
        this.unloadQueue = Queues.newConcurrentLinkedQueue();
        this.structureManager = definedstructuremanager;
        Path path = convertable_conversionsession.getDimensionPath(worldserver.dimension());

        this.storageName = path.getFileName().toString();
        this.level = worldserver;
        this.generator = chunkgenerator;
        this.mainThreadExecutor = iasynctaskhandler;
        ThreadedMailbox<Runnable> threadedmailbox = ThreadedMailbox.create(executor, "worldgen");

        Objects.requireNonNull(iasynctaskhandler);
        Mailbox<Runnable> mailbox = Mailbox.of("main", iasynctaskhandler::tell);

        this.progressListener = worldloadlistener;
        this.chunkStatusListener = chunkstatusupdatelistener;
        ThreadedMailbox<Runnable> threadedmailbox1 = ThreadedMailbox.create(executor, "light");

        this.queueSorter = new ChunkTaskQueueSorter(ImmutableList.of(threadedmailbox, mailbox, threadedmailbox1), executor, Integer.MAX_VALUE);
        this.worldgenMailbox = this.queueSorter.getProcessor(threadedmailbox, false);
        this.mainThreadMailbox = this.queueSorter.getProcessor(mailbox, false);
        this.lightEngine = new LightEngineThreaded(ilightaccess, this, this.level.dimensionType().hasSkyLight(), threadedmailbox1, this.queueSorter.getProcessor(threadedmailbox1, false));
        this.distanceManager = new PlayerChunkMap.a(executor, iasynctaskhandler);
        this.overworldDataStorage = supplier;
        this.poiManager = new VillagePlace(path.resolve("poi"), datafixer, flag, worldserver);
        this.setViewDistance(i);
    }

    protected ChunkGenerator generator() {
        return this.generator;
    }

    public void debugReloadGenerator() {
        DataResult<JsonElement> dataresult = ChunkGenerator.CODEC.encodeStart(JsonOps.INSTANCE, this.generator);
        DataResult<ChunkGenerator> dataresult1 = dataresult.flatMap((jsonelement) -> {
            return ChunkGenerator.CODEC.parse(JsonOps.INSTANCE, jsonelement);
        });

        dataresult1.result().ifPresent((chunkgenerator) -> {
            this.generator = chunkgenerator;
        });
    }

    private static double euclideanDistanceSquared(ChunkCoordIntPair chunkcoordintpair, Entity entity) {
        double d0 = (double) SectionPosition.sectionToBlockCoord(chunkcoordintpair.x, 8);
        double d1 = (double) SectionPosition.sectionToBlockCoord(chunkcoordintpair.z, 8);
        double d2 = d0 - entity.getX();
        double d3 = d1 - entity.getZ();

        return d2 * d2 + d3 * d3;
    }

    public static boolean isChunkInRange(int i, int j, int k, int l, int i1) {
        int j1 = Math.max(0, Math.abs(i - k) - 1);
        int k1 = Math.max(0, Math.abs(j - l) - 1);
        int l1 = Math.max(0, Math.max(j1, k1) - 1);
        int i2 = Math.min(j1, k1);
        int j2 = i2 * i2 + l1 * l1;
        int k2 = i1 - 1;
        int l2 = k2 * k2;

        return j2 <= l2;
    }

    private static boolean isChunkOnRangeBorder(int i, int j, int k, int l, int i1) {
        return !isChunkInRange(i, j, k, l, i1) ? false : (!isChunkInRange(i + 1, j, k, l, i1) ? true : (!isChunkInRange(i, j + 1, k, l, i1) ? true : (!isChunkInRange(i - 1, j, k, l, i1) ? true : !isChunkInRange(i, j - 1, k, l, i1))));
    }

    protected LightEngineThreaded getLightEngine() {
        return this.lightEngine;
    }

    @Nullable
    protected PlayerChunk getUpdatingChunkIfPresent(long i) {
        return (PlayerChunk) this.updatingChunkMap.get(i);
    }

    @Nullable
    protected PlayerChunk getVisibleChunkIfPresent(long i) {
        return (PlayerChunk) this.visibleChunkMap.get(i);
    }

    protected IntSupplier getChunkQueueLevel(long i) {
        return () -> {
            PlayerChunk playerchunk = this.getVisibleChunkIfPresent(i);

            return playerchunk == null ? ChunkTaskQueue.PRIORITY_LEVEL_COUNT - 1 : Math.min(playerchunk.getQueueLevel(), ChunkTaskQueue.PRIORITY_LEVEL_COUNT - 1);
        };
    }

    public String getChunkDebugData(ChunkCoordIntPair chunkcoordintpair) {
        PlayerChunk playerchunk = this.getVisibleChunkIfPresent(chunkcoordintpair.toLong());

        if (playerchunk == null) {
            return "null";
        } else {
            String s = playerchunk.getTicketLevel() + "\n";
            ChunkStatus chunkstatus = playerchunk.getLastAvailableStatus();
            IChunkAccess ichunkaccess = playerchunk.getLastAvailable();

            if (chunkstatus != null) {
                s = s + "St: \u00a7" + chunkstatus.getIndex() + chunkstatus + "\u00a7r\n";
            }

            if (ichunkaccess != null) {
                s = s + "Ch: \u00a7" + ichunkaccess.getStatus().getIndex() + ichunkaccess.getStatus() + "\u00a7r\n";
            }

            PlayerChunk.State playerchunk_state = playerchunk.getFullStatus();

            s = s + "\u00a7" + playerchunk_state.ordinal() + playerchunk_state;
            return s + "\u00a7r";
        }
    }

    private CompletableFuture<Either<List<IChunkAccess>, PlayerChunk.Failure>> getChunkRangeFuture(ChunkCoordIntPair chunkcoordintpair, int i, IntFunction<ChunkStatus> intfunction) {
        List<CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> list = new ArrayList();
        List<PlayerChunk> list1 = new ArrayList();
        int j = chunkcoordintpair.x;
        int k = chunkcoordintpair.z;

        for (int l = -i; l <= i; ++l) {
            for (int i1 = -i; i1 <= i; ++i1) {
                int j1 = Math.max(Math.abs(i1), Math.abs(l));
                final ChunkCoordIntPair chunkcoordintpair1 = new ChunkCoordIntPair(j + i1, k + l);
                long k1 = chunkcoordintpair1.toLong();
                PlayerChunk playerchunk = this.getUpdatingChunkIfPresent(k1);

                if (playerchunk == null) {
                    return CompletableFuture.completedFuture(Either.right(new PlayerChunk.Failure() {
                        public String toString() {
                            return "Unloaded " + chunkcoordintpair1;
                        }
                    }));
                }

                ChunkStatus chunkstatus = (ChunkStatus) intfunction.apply(j1);
                CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture = playerchunk.getOrScheduleFuture(chunkstatus, this);

                list1.add(playerchunk);
                list.add(completablefuture);
            }
        }

        CompletableFuture<List<Either<IChunkAccess, PlayerChunk.Failure>>> completablefuture1 = SystemUtils.sequence(list);
        CompletableFuture<Either<List<IChunkAccess>, PlayerChunk.Failure>> completablefuture2 = completablefuture1.thenApply((list2) -> {
            List<IChunkAccess> list3 = Lists.newArrayList();
            // CraftBukkit start - decompile error
            int cnt = 0;

            for (Iterator iterator = list2.iterator(); iterator.hasNext(); ++cnt) {
                final int l1 = cnt;
                // CraftBukkit end
                final Either<IChunkAccess, PlayerChunk.Failure> either = (Either) iterator.next();
                Optional<IChunkAccess> optional = either.left();

                if (!optional.isPresent()) {
                    return Either.right(new PlayerChunk.Failure() {
                        public String toString() {
                            ChunkCoordIntPair chunkcoordintpair2 = new ChunkCoordIntPair(j + l1 % (i * 2 + 1), k + l1 / (i * 2 + 1));

                            return "Unloaded " + chunkcoordintpair2 + " " + either.right().get();
                        }
                    });
                }

                list3.add((IChunkAccess) optional.get());
            }

            return Either.left(list3);
        });
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            PlayerChunk playerchunk1 = (PlayerChunk) iterator.next();

            playerchunk1.addSaveDependency("getChunkRangeFuture " + chunkcoordintpair + " " + i, completablefuture2);
        }

        return completablefuture2;
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> prepareEntityTickingChunk(ChunkCoordIntPair chunkcoordintpair) {
        return this.getChunkRangeFuture(chunkcoordintpair, 2, (i) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((either) -> {
            return either.mapLeft((list) -> {
                return (Chunk) list.get(list.size() / 2);
            });
        }, this.mainThreadExecutor);
    }

    @Nullable
    PlayerChunk updateChunkScheduling(long i, int j, @Nullable PlayerChunk playerchunk, int k) {
        if (k > PlayerChunkMap.MAX_CHUNK_DISTANCE && j > PlayerChunkMap.MAX_CHUNK_DISTANCE) {
            return playerchunk;
        } else {
            if (playerchunk != null) {
                playerchunk.setTicketLevel(j);
            }

            if (playerchunk != null) {
                if (j > PlayerChunkMap.MAX_CHUNK_DISTANCE) {
                    this.toDrop.add(i);
                } else {
                    this.toDrop.remove(i);
                }
            }

            if (j <= PlayerChunkMap.MAX_CHUNK_DISTANCE && playerchunk == null) {
                playerchunk = (PlayerChunk) this.pendingUnloads.remove(i);
                if (playerchunk != null) {
                    playerchunk.setTicketLevel(j);
                } else {
                    playerchunk = new PlayerChunk(new ChunkCoordIntPair(i), j, this.level, this.lightEngine, this.queueSorter, this);
                }

                this.updatingChunkMap.put(i, playerchunk);
                this.modified = true;
            }

            return playerchunk;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.queueSorter.close();
            this.poiManager.close();
        } finally {
            super.close();
        }

    }

    protected void saveAllChunks(boolean flag) {
        if (flag) {
            List<PlayerChunk> list = (List) this.visibleChunkMap.values().stream().filter(PlayerChunk::wasAccessibleSinceLastSave).peek(PlayerChunk::refreshAccessibility).collect(Collectors.toList());
            MutableBoolean mutableboolean = new MutableBoolean();

            do {
                mutableboolean.setFalse();
                list.stream().map((playerchunk) -> {
                    CompletableFuture completablefuture;

                    do {
                        completablefuture = playerchunk.getChunkToSave();
                        IAsyncTaskHandler iasynctaskhandler = this.mainThreadExecutor;

                        Objects.requireNonNull(completablefuture);
                        iasynctaskhandler.managedBlock(completablefuture::isDone);
                    } while (completablefuture != playerchunk.getChunkToSave());

                    return (IChunkAccess) completablefuture.join();
                }).filter((ichunkaccess) -> {
                    return ichunkaccess instanceof ProtoChunkExtension || ichunkaccess instanceof Chunk;
                }).filter(this::save).forEach((ichunkaccess) -> {
                    mutableboolean.setTrue();
                });
            } while (mutableboolean.isTrue());

            this.processUnloads(() -> {
                return true;
            });
            this.flushWorker();
        } else {
            this.visibleChunkMap.values().forEach(this::saveChunkIfNeeded);
        }

    }

    protected void tick(BooleanSupplier booleansupplier) {
        GameProfilerFiller gameprofilerfiller = this.level.getProfiler();

        gameprofilerfiller.push("poi");
        this.poiManager.tick(booleansupplier);
        gameprofilerfiller.popPush("chunk_unload");
        if (!this.level.noSave()) {
            this.processUnloads(booleansupplier);
        }

        gameprofilerfiller.pop();
    }

    private static final double UNLOAD_QUEUE_RESIZE_FACTOR = 0.96; // Spigot

    private void processUnloads(BooleanSupplier booleansupplier) {
        LongIterator longiterator = this.toDrop.iterator();
        // Spigot start
        org.spigotmc.SlackActivityAccountant activityAccountant = this.level.getServer().slackActivityAccountant;
        activityAccountant.startActivity(0.5);
        int targetSize = (int) (this.toDrop.size() * UNLOAD_QUEUE_RESIZE_FACTOR);
        // Spigot end
        while (longiterator.hasNext()) { // Spigot
            long j = longiterator.nextLong();
            longiterator.remove(); // Spigot
            PlayerChunk playerchunk = (PlayerChunk) this.updatingChunkMap.remove(j);

            if (playerchunk != null) {
                this.pendingUnloads.put(j, playerchunk);
                this.modified = true;
                // Spigot start
                if (!booleansupplier.getAsBoolean() && this.toDrop.size() <= targetSize && activityAccountant.activityTimeIsExhausted()) {
                    break;
                }
                // Spigot end
                this.scheduleUnload(j, playerchunk);
            }
        }
        activityAccountant.endActivity(); // Spigot

        int k = Math.max(0, this.unloadQueue.size() - 2000);

        Runnable runnable;

        while ((booleansupplier.getAsBoolean() || k > 0) && (runnable = (Runnable) this.unloadQueue.poll()) != null) {
            --k;
            runnable.run();
        }

        int l = 0;
        ObjectIterator objectiterator = this.visibleChunkMap.values().iterator();

        while (l < 20 && booleansupplier.getAsBoolean() && objectiterator.hasNext()) {
            if (this.saveChunkIfNeeded((PlayerChunk) objectiterator.next())) {
                ++l;
            }
        }

    }

    private void scheduleUnload(long i, PlayerChunk playerchunk) {
        CompletableFuture<IChunkAccess> completablefuture = playerchunk.getChunkToSave();
        Consumer<IChunkAccess> consumer = (ichunkaccess) -> { // CraftBukkit - decompile error
            CompletableFuture<IChunkAccess> completablefuture1 = playerchunk.getChunkToSave();

            if (completablefuture1 != completablefuture) {
                this.scheduleUnload(i, playerchunk);
            } else {
                if (this.pendingUnloads.remove(i, playerchunk) && ichunkaccess != null) {
                    if (ichunkaccess instanceof Chunk) {
                        ((Chunk) ichunkaccess).setLoaded(false);
                    }

                    this.save(ichunkaccess);
                    if (this.entitiesInLevel.remove(i) && ichunkaccess instanceof Chunk) {
                        Chunk chunk = (Chunk) ichunkaccess;

                        this.level.unload(chunk);
                    }

                    this.lightEngine.updateChunkStatus(ichunkaccess.getPos());
                    this.lightEngine.tryScheduleUpdate();
                    this.progressListener.onStatusChange(ichunkaccess.getPos(), (ChunkStatus) null);
                }

            }
        };
        Queue queue = this.unloadQueue;

        Objects.requireNonNull(this.unloadQueue);
        completablefuture.thenAcceptAsync(consumer, queue::add).whenComplete((ovoid, throwable) -> {
            if (throwable != null) {
                PlayerChunkMap.LOGGER.error("Failed to save chunk {}", playerchunk.getPos(), throwable);
            }

        });
    }

    protected boolean promoteChunkMap() {
        if (!this.modified) {
            return false;
        } else {
            this.visibleChunkMap = this.updatingChunkMap.clone();
            this.modified = false;
            return true;
        }
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> schedule(PlayerChunk playerchunk, ChunkStatus chunkstatus) {
        ChunkCoordIntPair chunkcoordintpair = playerchunk.getPos();

        if (chunkstatus == ChunkStatus.EMPTY) {
            return this.scheduleChunkLoad(chunkcoordintpair);
        } else {
            if (chunkstatus == ChunkStatus.LIGHT) {
                this.distanceManager.addTicket(TicketType.LIGHT, chunkcoordintpair, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), chunkcoordintpair);
            }

            Optional<IChunkAccess> optional = ((Either) playerchunk.getOrScheduleFuture(chunkstatus.getParent(), this).getNow(PlayerChunk.UNLOADED_CHUNK)).left();

            if (optional.isPresent() && ((IChunkAccess) optional.get()).getStatus().isOrAfter(chunkstatus)) {
                CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture = chunkstatus.load(this.level, this.structureManager, this.lightEngine, (ichunkaccess) -> {
                    return this.protoChunkToFullChunk(playerchunk);
                }, (IChunkAccess) optional.get());

                this.progressListener.onStatusChange(chunkcoordintpair, chunkstatus);
                return completablefuture;
            } else {
                return this.scheduleChunkGeneration(playerchunk, chunkstatus);
            }
        }
    }

    private CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> scheduleChunkLoad(ChunkCoordIntPair chunkcoordintpair) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.level.getProfiler().incrementCounter("chunkLoad");
                NBTTagCompound nbttagcompound = this.readChunk(chunkcoordintpair);

                if (nbttagcompound != null) {
                    boolean flag = nbttagcompound.contains("Status", 8);

                    if (flag) {
                        ProtoChunk protochunk = ChunkRegionLoader.read(this.level, this.poiManager, chunkcoordintpair, nbttagcompound);

                        this.markPosition(chunkcoordintpair, protochunk.getStatus().getChunkType());
                        return Either.left(protochunk);
                    }

                    PlayerChunkMap.LOGGER.error("Chunk file at {} is missing level data, skipping", chunkcoordintpair);
                }
            } catch (ReportedException reportedexception) {
                Throwable throwable = reportedexception.getCause();

                if (!(throwable instanceof IOException)) {
                    this.markPositionReplaceable(chunkcoordintpair);
                    throw reportedexception;
                }

                PlayerChunkMap.LOGGER.error("Couldn't load chunk {}", chunkcoordintpair, throwable);
            } catch (Exception exception) {
                PlayerChunkMap.LOGGER.error("Couldn't load chunk {}", chunkcoordintpair, exception);
            }

            this.markPositionReplaceable(chunkcoordintpair);
            return Either.left(new ProtoChunk(chunkcoordintpair, ChunkConverter.EMPTY, this.level, this.level.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), (BlendingData) null));
        }, this.mainThreadExecutor);
    }

    private void markPositionReplaceable(ChunkCoordIntPair chunkcoordintpair) {
        this.chunkTypeCache.put(chunkcoordintpair.toLong(), (byte) -1);
    }

    private byte markPosition(ChunkCoordIntPair chunkcoordintpair, ChunkStatus.Type chunkstatus_type) {
        return this.chunkTypeCache.put(chunkcoordintpair.toLong(), (byte) (chunkstatus_type == ChunkStatus.Type.PROTOCHUNK ? -1 : 1));
    }

    private CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> scheduleChunkGeneration(PlayerChunk playerchunk, ChunkStatus chunkstatus) {
        ChunkCoordIntPair chunkcoordintpair = playerchunk.getPos();
        CompletableFuture<Either<List<IChunkAccess>, PlayerChunk.Failure>> completablefuture = this.getChunkRangeFuture(chunkcoordintpair, chunkstatus.getRange(), (i) -> {
            return this.getDependencyStatus(chunkstatus, i);
        });

        this.level.getProfiler().incrementCounter(() -> {
            return "chunkGenerate " + chunkstatus.getName();
        });
        Executor executor = (runnable) -> {
            this.worldgenMailbox.tell(ChunkTaskQueueSorter.message(playerchunk, runnable));
        };

        return completablefuture.thenComposeAsync((either) -> {
            return (CompletionStage) either.map((list) -> {
                try {
                    CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture1 = chunkstatus.generate(executor, this.level, this.generator, this.structureManager, this.lightEngine, (ichunkaccess) -> {
                        return this.protoChunkToFullChunk(playerchunk);
                    }, list, false);

                    this.progressListener.onStatusChange(chunkcoordintpair, chunkstatus);
                    return completablefuture1;
                } catch (Exception exception) {
                    exception.getStackTrace();
                    CrashReport crashreport = CrashReport.forThrowable(exception, "Exception generating new chunk");
                    CrashReportSystemDetails crashreportsystemdetails = crashreport.addCategory("Chunk to be generated");

                    crashreportsystemdetails.setDetail("Location", (Object) String.format("%d,%d", chunkcoordintpair.x, chunkcoordintpair.z));
                    crashreportsystemdetails.setDetail("Position hash", (Object) ChunkCoordIntPair.asLong(chunkcoordintpair.x, chunkcoordintpair.z));
                    crashreportsystemdetails.setDetail("Generator", (Object) this.generator);
                    this.mainThreadExecutor.execute(() -> {
                        throw new ReportedException(crashreport);
                    });
                    throw new ReportedException(crashreport);
                }
            }, (playerchunk_failure) -> {
                this.releaseLightTicket(chunkcoordintpair);
                return CompletableFuture.completedFuture(Either.right(playerchunk_failure));
            });
        }, executor);
    }

    protected void releaseLightTicket(ChunkCoordIntPair chunkcoordintpair) {
        this.mainThreadExecutor.tell(SystemUtils.name(() -> {
            this.distanceManager.removeTicket(TicketType.LIGHT, chunkcoordintpair, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), chunkcoordintpair);
        }, () -> {
            return "release light ticket " + chunkcoordintpair;
        }));
    }

    private ChunkStatus getDependencyStatus(ChunkStatus chunkstatus, int i) {
        ChunkStatus chunkstatus1;

        if (i == 0) {
            chunkstatus1 = chunkstatus.getParent();
        } else {
            chunkstatus1 = ChunkStatus.getStatusAroundFullChunk(ChunkStatus.getDistance(chunkstatus) + i);
        }

        return chunkstatus1;
    }

    private static void postLoadProtoChunk(WorldServer worldserver, List<NBTTagCompound> list) {
        if (!list.isEmpty()) {
            // CraftBukkit start - these are spawned serialized (DefinedStructure) and we don't call an add event below at the moment due to ordering complexities
            worldserver.addWorldGenChunkEntities(EntityTypes.loadEntitiesRecursive(list, worldserver).filter((entity) -> {
                boolean needsRemoval = false;
                net.minecraft.server.dedicated.DedicatedServer server = worldserver.getCraftServer().getServer();
                if (!server.areNpcsEnabled() && entity instanceof net.minecraft.world.entity.npc.NPC) {
                    entity.discard();
                    needsRemoval = true;
                }
                if (!server.isSpawningAnimals() && (entity instanceof net.minecraft.world.entity.animal.EntityAnimal || entity instanceof net.minecraft.world.entity.animal.EntityWaterAnimal)) {
                    entity.discard();
                    needsRemoval = true;
                }
                return !needsRemoval;
            }));
            // CraftBukkit end
        }

    }

    private CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> protoChunkToFullChunk(PlayerChunk playerchunk) {
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completablefuture = playerchunk.getFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());

        return completablefuture.thenApplyAsync((either) -> {
            ChunkStatus chunkstatus = PlayerChunk.getStatus(playerchunk.getTicketLevel());

            return !chunkstatus.isOrAfter(ChunkStatus.FULL) ? PlayerChunk.UNLOADED_CHUNK : either.mapLeft((ichunkaccess) -> {
                ChunkCoordIntPair chunkcoordintpair = playerchunk.getPos();
                ProtoChunk protochunk = (ProtoChunk) ichunkaccess;
                Chunk chunk;

                if (protochunk instanceof ProtoChunkExtension) {
                    chunk = ((ProtoChunkExtension) protochunk).getWrapped();
                } else {
                    chunk = new Chunk(this.level, protochunk, (chunk1) -> {
                        postLoadProtoChunk(this.level, protochunk.getEntities());
                    });
                    playerchunk.replaceProtoChunk(new ProtoChunkExtension(chunk, false));
                }

                chunk.setFullStatus(() -> {
                    return PlayerChunk.getFullChunkStatus(playerchunk.getTicketLevel());
                });
                chunk.runPostLoad();
                if (this.entitiesInLevel.add(chunkcoordintpair.toLong())) {
                    chunk.setLoaded(true);
                    chunk.registerAllBlockEntitiesAfterLevelLoad();
                    chunk.registerTickContainerInLevel(this.level);
                }

                return chunk;
            });
        }, (runnable) -> {
            Mailbox mailbox = this.mainThreadMailbox;
            long i = playerchunk.getPos().toLong();

            Objects.requireNonNull(playerchunk);
            mailbox.tell(ChunkTaskQueueSorter.message(runnable, i, playerchunk::getTicketLevel));
        });
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> prepareTickingChunk(PlayerChunk playerchunk) {
        ChunkCoordIntPair chunkcoordintpair = playerchunk.getPos();
        CompletableFuture<Either<List<IChunkAccess>, PlayerChunk.Failure>> completablefuture = this.getChunkRangeFuture(chunkcoordintpair, 1, (i) -> {
            return ChunkStatus.FULL;
        });
        CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completablefuture1 = completablefuture.thenApplyAsync((either) -> {
            return either.flatMap((list) -> {
                Chunk chunk = (Chunk) list.get(list.size() / 2);

                chunk.postProcessGeneration();
                this.level.startTickingChunk(chunk);
                return Either.left(chunk);
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskQueueSorter.message(playerchunk, runnable));
        });

        completablefuture1.thenAcceptAsync((either) -> {
            either.ifLeft((chunk) -> {
                this.tickingGenerated.getAndIncrement();
                MutableObject<ClientboundLevelChunkWithLightPacket> mutableobject = new MutableObject();

                this.getPlayers(chunkcoordintpair, false).forEach((entityplayer) -> {
                    this.playerLoadedChunk(entityplayer, mutableobject, chunk);
                });
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskQueueSorter.message(playerchunk, runnable));
        });
        return completablefuture1;
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> prepareAccessibleChunk(PlayerChunk playerchunk) {
        return this.getChunkRangeFuture(playerchunk.getPos(), 1, ChunkStatus::getStatusAroundFullChunk).thenApplyAsync((either) -> {
            return either.mapLeft((list) -> {
                Chunk chunk = (Chunk) list.get(list.size() / 2);

                return chunk;
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskQueueSorter.message(playerchunk, runnable));
        });
    }

    public int getTickingGenerated() {
        return this.tickingGenerated.get();
    }

    private boolean saveChunkIfNeeded(PlayerChunk playerchunk) {
        if (!playerchunk.wasAccessibleSinceLastSave()) {
            return false;
        } else {
            IChunkAccess ichunkaccess = (IChunkAccess) playerchunk.getChunkToSave().getNow(null); // CraftBukkit - decompile error

            if (!(ichunkaccess instanceof ProtoChunkExtension) && !(ichunkaccess instanceof Chunk)) {
                return false;
            } else {
                boolean flag = this.save(ichunkaccess);

                playerchunk.refreshAccessibility();
                return flag;
            }
        }
    }

    public boolean save(IChunkAccess ichunkaccess) {
        this.poiManager.flush(ichunkaccess.getPos());
        if (!ichunkaccess.isUnsaved()) {
            return false;
        } else {
            ichunkaccess.setUnsaved(false);
            ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();

            try {
                ChunkStatus chunkstatus = ichunkaccess.getStatus();

                if (chunkstatus.getChunkType() != ChunkStatus.Type.LEVELCHUNK) {
                    if (this.isExistingChunkFull(chunkcoordintpair)) {
                        return false;
                    }

                    if (chunkstatus == ChunkStatus.EMPTY && ichunkaccess.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return false;
                    }
                }

                this.level.getProfiler().incrementCounter("chunkSave");
                NBTTagCompound nbttagcompound = ChunkRegionLoader.write(this.level, ichunkaccess);

                this.write(chunkcoordintpair, nbttagcompound);
                this.markPosition(chunkcoordintpair, chunkstatus.getChunkType());
                return true;
            } catch (Exception exception) {
                PlayerChunkMap.LOGGER.error("Failed to save chunk {},{}", chunkcoordintpair.x, chunkcoordintpair.z, exception);
                return false;
            }
        }
    }

    private boolean isExistingChunkFull(ChunkCoordIntPair chunkcoordintpair) {
        byte b0 = this.chunkTypeCache.get(chunkcoordintpair.toLong());

        if (b0 != 0) {
            return b0 == 1;
        } else {
            NBTTagCompound nbttagcompound;

            try {
                nbttagcompound = this.readChunk(chunkcoordintpair);
                if (nbttagcompound == null) {
                    this.markPositionReplaceable(chunkcoordintpair);
                    return false;
                }
            } catch (Exception exception) {
                PlayerChunkMap.LOGGER.error("Failed to read chunk {}", chunkcoordintpair, exception);
                this.markPositionReplaceable(chunkcoordintpair);
                return false;
            }

            ChunkStatus.Type chunkstatus_type = ChunkRegionLoader.getChunkTypeFromTag(nbttagcompound);

            return this.markPosition(chunkcoordintpair, chunkstatus_type) == 1;
        }
    }

    protected void setViewDistance(int i) {
        int j = MathHelper.clamp(i + 1, (int) 3, (int) 33);

        if (j != this.viewDistance) {
            int k = this.viewDistance;

            this.viewDistance = j;
            this.distanceManager.updatePlayerTickets(this.viewDistance + 1);
            ObjectIterator objectiterator = this.updatingChunkMap.values().iterator();

            while (objectiterator.hasNext()) {
                PlayerChunk playerchunk = (PlayerChunk) objectiterator.next();
                ChunkCoordIntPair chunkcoordintpair = playerchunk.getPos();
                MutableObject<ClientboundLevelChunkWithLightPacket> mutableobject = new MutableObject();

                this.getPlayers(chunkcoordintpair, false).forEach((entityplayer) -> {
                    SectionPosition sectionposition = entityplayer.getLastSectionPos();
                    boolean flag = isChunkInRange(chunkcoordintpair.x, chunkcoordintpair.z, sectionposition.x(), sectionposition.z(), k);
                    boolean flag1 = isChunkInRange(chunkcoordintpair.x, chunkcoordintpair.z, sectionposition.x(), sectionposition.z(), this.viewDistance);

                    this.updateChunkTracking(entityplayer, chunkcoordintpair, mutableobject, flag, flag1);
                });
            }
        }

    }

    protected void updateChunkTracking(EntityPlayer entityplayer, ChunkCoordIntPair chunkcoordintpair, MutableObject<ClientboundLevelChunkWithLightPacket> mutableobject, boolean flag, boolean flag1) {
        if (entityplayer.level == this.level) {
            if (flag1 && !flag) {
                PlayerChunk playerchunk = this.getVisibleChunkIfPresent(chunkcoordintpair.toLong());

                if (playerchunk != null) {
                    Chunk chunk = playerchunk.getTickingChunk();

                    if (chunk != null) {
                        this.playerLoadedChunk(entityplayer, mutableobject, chunk);
                    }

                    PacketDebug.sendPoiPacketsForChunk(this.level, chunkcoordintpair);
                }
            }

            if (!flag1 && flag) {
                entityplayer.untrackChunk(chunkcoordintpair);
            }

        }
    }

    public int size() {
        return this.visibleChunkMap.size();
    }

    public ChunkMapDistance getDistanceManager() {
        return this.distanceManager;
    }

    protected Iterable<PlayerChunk> getChunks() {
        return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
    }

    void dumpChunks(Writer writer) throws IOException {
        CSVWriter csvwriter = CSVWriter.builder().addColumn("x").addColumn("z").addColumn("level").addColumn("in_memory").addColumn("status").addColumn("full_status").addColumn("accessible_ready").addColumn("ticking_ready").addColumn("entity_ticking_ready").addColumn("ticket").addColumn("spawning").addColumn("block_entity_count").addColumn("ticking_ticket").addColumn("ticking_level").addColumn("block_ticks").addColumn("fluid_ticks").build(writer);
        TickingTracker tickingtracker = this.distanceManager.tickingTracker();
        ObjectBidirectionalIterator objectbidirectionaliterator = this.visibleChunkMap.long2ObjectEntrySet().iterator();

        while (objectbidirectionaliterator.hasNext()) {
            Entry<PlayerChunk> entry = (Entry) objectbidirectionaliterator.next();
            long i = entry.getLongKey();
            ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i);
            PlayerChunk playerchunk = (PlayerChunk) entry.getValue();
            Optional<IChunkAccess> optional = Optional.ofNullable(playerchunk.getLastAvailable());
            Optional<Chunk> optional1 = optional.flatMap((ichunkaccess) -> {
                return ichunkaccess instanceof Chunk ? Optional.of((Chunk) ichunkaccess) : Optional.empty();
            });

            // CraftBukkit - decompile error
            csvwriter.writeRow(chunkcoordintpair.x, chunkcoordintpair.z, playerchunk.getTicketLevel(), optional.isPresent(), optional.map(IChunkAccess::getStatus).orElse(null), optional1.map(Chunk::getFullStatus).orElse(null), printFuture(playerchunk.getFullChunkFuture()), printFuture(playerchunk.getTickingChunkFuture()), printFuture(playerchunk.getEntityTickingChunkFuture()), this.distanceManager.getTicketDebugString(i), this.anyPlayerCloseEnoughForSpawning(chunkcoordintpair), optional1.map((chunk) -> {
                return chunk.getBlockEntities().size();
            }).orElse(0), tickingtracker.getTicketDebugString(i), tickingtracker.getLevel(i), optional1.map((chunk) -> {
                return chunk.getBlockTicks().count();
            }).orElse(0), optional1.map((chunk) -> {
                return chunk.getFluidTicks().count();
            }).orElse(0));
        }

    }

    private static String printFuture(CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completablefuture) {
        try {
            Either<Chunk, PlayerChunk.Failure> either = (Either) completablefuture.getNow(null); // CraftBukkit - decompile error

            return either != null ? (String) either.map((chunk) -> {
                return "done";
            }, (playerchunk_failure) -> {
                return "unloaded";
            }) : "not completed";
        } catch (CompletionException completionexception) {
            return "failed " + completionexception.getCause().getMessage();
        } catch (CancellationException cancellationexception) {
            return "cancelled";
        }
    }

    @Nullable
    private NBTTagCompound readChunk(ChunkCoordIntPair chunkcoordintpair) throws IOException {
        NBTTagCompound nbttagcompound = this.read(chunkcoordintpair);

        return nbttagcompound == null ? null : this.upgradeChunkTag(this.level.getTypeKey(), this.overworldDataStorage, nbttagcompound, this.generator.getTypeNameForDataFixer(), chunkcoordintpair, level); // CraftBukkit
    }

    boolean anyPlayerCloseEnoughForSpawning(ChunkCoordIntPair chunkcoordintpair) {
        // Spigot start
        return anyPlayerCloseEnoughForSpawning(chunkcoordintpair, false);
    }

    boolean anyPlayerCloseEnoughForSpawning(ChunkCoordIntPair chunkcoordintpair, boolean reducedRange) {
        int chunkRange = level.spigotConfig.mobSpawnRange;
        chunkRange = (chunkRange > level.spigotConfig.viewDistance) ? (byte) level.spigotConfig.viewDistance : chunkRange;
        chunkRange = (chunkRange > 8) ? 8 : chunkRange;

        double blockRange = (reducedRange) ? Math.pow(chunkRange << 4, 2) : 16384.0D;
        // Spigot end
        long i = chunkcoordintpair.toLong();

        if (!this.distanceManager.hasPlayersNearby(i)) {
            return false;
        } else {
            Iterator iterator = this.playerMap.getPlayers(i).iterator();

            EntityPlayer entityplayer;

            do {
                if (!iterator.hasNext()) {
                    return false;
                }

                entityplayer = (EntityPlayer) iterator.next();
            } while (!this.playerIsCloseEnoughForSpawning(entityplayer, chunkcoordintpair, blockRange)); // Spigot

            return true;
        }
    }

    public List<EntityPlayer> getPlayersCloseForSpawning(ChunkCoordIntPair chunkcoordintpair) {
        long i = chunkcoordintpair.toLong();

        if (!this.distanceManager.hasPlayersNearby(i)) {
            return List.of();
        } else {
            Builder<EntityPlayer> builder = ImmutableList.builder();
            Iterator iterator = this.playerMap.getPlayers(i).iterator();

            while (iterator.hasNext()) {
                EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                if (this.playerIsCloseEnoughForSpawning(entityplayer, chunkcoordintpair, 16384.0D)) { // Spigot
                    builder.add(entityplayer);
                }
            }

            return builder.build();
        }
    }

    private boolean playerIsCloseEnoughForSpawning(EntityPlayer entityplayer, ChunkCoordIntPair chunkcoordintpair, double range) { // Spigot
        if (entityplayer.isSpectator()) {
            return false;
        } else {
            double d0 = euclideanDistanceSquared(chunkcoordintpair, entityplayer);

            return d0 < range; // Spigot
        }
    }

    private boolean skipPlayer(EntityPlayer entityplayer) {
        return entityplayer.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
    }

    void updatePlayerStatus(EntityPlayer entityplayer, boolean flag) {
        boolean flag1 = this.skipPlayer(entityplayer);
        boolean flag2 = this.playerMap.ignoredOrUnknown(entityplayer);
        int i = SectionPosition.blockToSectionCoord(entityplayer.getBlockX());
        int j = SectionPosition.blockToSectionCoord(entityplayer.getBlockZ());

        if (flag) {
            this.playerMap.addPlayer(ChunkCoordIntPair.asLong(i, j), entityplayer, flag1);
            this.updatePlayerPos(entityplayer);
            if (!flag1) {
                this.distanceManager.addPlayer(SectionPosition.of((Entity) entityplayer), entityplayer);
            }
        } else {
            SectionPosition sectionposition = entityplayer.getLastSectionPos();

            this.playerMap.removePlayer(sectionposition.chunk().toLong(), entityplayer);
            if (!flag2) {
                this.distanceManager.removePlayer(sectionposition, entityplayer);
            }
        }

        for (int k = i - this.viewDistance - 1; k <= i + this.viewDistance + 1; ++k) {
            for (int l = j - this.viewDistance - 1; l <= j + this.viewDistance + 1; ++l) {
                if (isChunkInRange(k, l, i, j, this.viewDistance)) {
                    ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(k, l);

                    this.updateChunkTracking(entityplayer, chunkcoordintpair, new MutableObject(), !flag, flag);
                }
            }
        }

    }

    private SectionPosition updatePlayerPos(EntityPlayer entityplayer) {
        SectionPosition sectionposition = SectionPosition.of((Entity) entityplayer);

        entityplayer.setLastSectionPos(sectionposition);
        entityplayer.connection.send(new PacketPlayOutViewCentre(sectionposition.x(), sectionposition.z()));
        return sectionposition;
    }

    public void move(EntityPlayer entityplayer) {
        ObjectIterator objectiterator = this.entityMap.values().iterator();

        while (objectiterator.hasNext()) {
            PlayerChunkMap.EntityTracker playerchunkmap_entitytracker = (PlayerChunkMap.EntityTracker) objectiterator.next();

            if (playerchunkmap_entitytracker.entity == entityplayer) {
                playerchunkmap_entitytracker.updatePlayers(this.level.players());
            } else {
                playerchunkmap_entitytracker.updatePlayer(entityplayer);
            }
        }

        int i = SectionPosition.blockToSectionCoord(entityplayer.getBlockX());
        int j = SectionPosition.blockToSectionCoord(entityplayer.getBlockZ());
        SectionPosition sectionposition = entityplayer.getLastSectionPos();
        SectionPosition sectionposition1 = SectionPosition.of((Entity) entityplayer);
        long k = sectionposition.chunk().toLong();
        long l = sectionposition1.chunk().toLong();
        boolean flag = this.playerMap.ignored(entityplayer);
        boolean flag1 = this.skipPlayer(entityplayer);
        boolean flag2 = sectionposition.asLong() != sectionposition1.asLong();

        if (flag2 || flag != flag1) {
            this.updatePlayerPos(entityplayer);
            if (!flag) {
                this.distanceManager.removePlayer(sectionposition, entityplayer);
            }

            if (!flag1) {
                this.distanceManager.addPlayer(sectionposition1, entityplayer);
            }

            if (!flag && flag1) {
                this.playerMap.ignorePlayer(entityplayer);
            }

            if (flag && !flag1) {
                this.playerMap.unIgnorePlayer(entityplayer);
            }

            if (k != l) {
                this.playerMap.updatePlayer(k, l, entityplayer);
            }
        }

        int i1 = sectionposition.x();
        int j1 = sectionposition.z();
        int k1;
        int l1;

        if (Math.abs(i1 - i) <= this.viewDistance * 2 && Math.abs(j1 - j) <= this.viewDistance * 2) {
            k1 = Math.min(i, i1) - this.viewDistance - 1;
            l1 = Math.min(j, j1) - this.viewDistance - 1;
            int i2 = Math.max(i, i1) + this.viewDistance + 1;
            int j2 = Math.max(j, j1) + this.viewDistance + 1;

            for (int k2 = k1; k2 <= i2; ++k2) {
                for (int l2 = l1; l2 <= j2; ++l2) {
                    boolean flag3 = isChunkInRange(k2, l2, i1, j1, this.viewDistance);
                    boolean flag4 = isChunkInRange(k2, l2, i, j, this.viewDistance);

                    this.updateChunkTracking(entityplayer, new ChunkCoordIntPair(k2, l2), new MutableObject(), flag3, flag4);
                }
            }
        } else {
            boolean flag5;
            boolean flag6;

            for (k1 = i1 - this.viewDistance - 1; k1 <= i1 + this.viewDistance + 1; ++k1) {
                for (l1 = j1 - this.viewDistance - 1; l1 <= j1 + this.viewDistance + 1; ++l1) {
                    if (isChunkInRange(k1, l1, i1, j1, this.viewDistance)) {
                        flag5 = true;
                        flag6 = false;
                        this.updateChunkTracking(entityplayer, new ChunkCoordIntPair(k1, l1), new MutableObject(), true, false);
                    }
                }
            }

            for (k1 = i - this.viewDistance - 1; k1 <= i + this.viewDistance + 1; ++k1) {
                for (l1 = j - this.viewDistance - 1; l1 <= j + this.viewDistance + 1; ++l1) {
                    if (isChunkInRange(k1, l1, i, j, this.viewDistance)) {
                        flag5 = false;
                        flag6 = true;
                        this.updateChunkTracking(entityplayer, new ChunkCoordIntPair(k1, l1), new MutableObject(), false, true);
                    }
                }
            }
        }

    }

    @Override
    public List<EntityPlayer> getPlayers(ChunkCoordIntPair chunkcoordintpair, boolean flag) {
        Set<EntityPlayer> set = this.playerMap.getPlayers(chunkcoordintpair.toLong());
        Builder<EntityPlayer> builder = ImmutableList.builder();
        Iterator iterator = set.iterator();

        while (iterator.hasNext()) {
            EntityPlayer entityplayer = (EntityPlayer) iterator.next();
            SectionPosition sectionposition = entityplayer.getLastSectionPos();

            if (flag && isChunkOnRangeBorder(chunkcoordintpair.x, chunkcoordintpair.z, sectionposition.x(), sectionposition.z(), this.viewDistance) || !flag && isChunkInRange(chunkcoordintpair.x, chunkcoordintpair.z, sectionposition.x(), sectionposition.z(), this.viewDistance)) {
                builder.add(entityplayer);
            }
        }

        return builder.build();
    }

    protected void addEntity(Entity entity) {
        org.spigotmc.AsyncCatcher.catchOp("entity track"); // Spigot
        if (!(entity instanceof EntityComplexPart)) {
            EntityTypes<?> entitytypes = entity.getType();
            int i = entitytypes.clientTrackingRange() * 16;
            i = org.spigotmc.TrackingRange.getEntityTrackingRange(entity, i); // Spigot

            if (i != 0) {
                int j = entitytypes.updateInterval();

                if (this.entityMap.containsKey(entity.getId())) {
                    throw (IllegalStateException) SystemUtils.pauseInIde(new IllegalStateException("Entity is already tracked!"));
                } else {
                    PlayerChunkMap.EntityTracker playerchunkmap_entitytracker = new PlayerChunkMap.EntityTracker(entity, i, j, entitytypes.trackDeltas());

                    this.entityMap.put(entity.getId(), playerchunkmap_entitytracker);
                    playerchunkmap_entitytracker.updatePlayers(this.level.players());
                    if (entity instanceof EntityPlayer) {
                        EntityPlayer entityplayer = (EntityPlayer) entity;

                        this.updatePlayerStatus(entityplayer, true);
                        ObjectIterator objectiterator = this.entityMap.values().iterator();

                        while (objectiterator.hasNext()) {
                            PlayerChunkMap.EntityTracker playerchunkmap_entitytracker1 = (PlayerChunkMap.EntityTracker) objectiterator.next();

                            if (playerchunkmap_entitytracker1.entity != entityplayer) {
                                playerchunkmap_entitytracker1.updatePlayer(entityplayer);
                            }
                        }
                    }

                }
            }
        }
    }

    protected void removeEntity(Entity entity) {
        org.spigotmc.AsyncCatcher.catchOp("entity untrack"); // Spigot
        if (entity instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) entity;

            this.updatePlayerStatus(entityplayer, false);
            ObjectIterator objectiterator = this.entityMap.values().iterator();

            while (objectiterator.hasNext()) {
                PlayerChunkMap.EntityTracker playerchunkmap_entitytracker = (PlayerChunkMap.EntityTracker) objectiterator.next();

                playerchunkmap_entitytracker.removePlayer(entityplayer);
            }
        }

        PlayerChunkMap.EntityTracker playerchunkmap_entitytracker1 = (PlayerChunkMap.EntityTracker) this.entityMap.remove(entity.getId());

        if (playerchunkmap_entitytracker1 != null) {
            playerchunkmap_entitytracker1.broadcastRemoved();
        }

    }

    protected void tick() {
        List<EntityPlayer> list = Lists.newArrayList();
        List<EntityPlayer> list1 = this.level.players();
        ObjectIterator objectiterator = this.entityMap.values().iterator();

        PlayerChunkMap.EntityTracker playerchunkmap_entitytracker;

        while (objectiterator.hasNext()) {
            playerchunkmap_entitytracker = (PlayerChunkMap.EntityTracker) objectiterator.next();
            SectionPosition sectionposition = playerchunkmap_entitytracker.lastSectionPos;
            SectionPosition sectionposition1 = SectionPosition.of(playerchunkmap_entitytracker.entity);
            boolean flag = !Objects.equals(sectionposition, sectionposition1);

            if (flag) {
                playerchunkmap_entitytracker.updatePlayers(list1);
                Entity entity = playerchunkmap_entitytracker.entity;

                if (entity instanceof EntityPlayer) {
                    list.add((EntityPlayer) entity);
                }

                playerchunkmap_entitytracker.lastSectionPos = sectionposition1;
            }

            if (flag || this.distanceManager.inEntityTickingRange(sectionposition1.chunk().toLong())) {
                playerchunkmap_entitytracker.serverEntity.sendChanges();
            }
        }

        if (!list.isEmpty()) {
            objectiterator = this.entityMap.values().iterator();

            while (objectiterator.hasNext()) {
                playerchunkmap_entitytracker = (PlayerChunkMap.EntityTracker) objectiterator.next();
                playerchunkmap_entitytracker.updatePlayers(list);
            }
        }

    }

    public void broadcast(Entity entity, Packet<?> packet) {
        PlayerChunkMap.EntityTracker playerchunkmap_entitytracker = (PlayerChunkMap.EntityTracker) this.entityMap.get(entity.getId());

        if (playerchunkmap_entitytracker != null) {
            playerchunkmap_entitytracker.broadcast(packet);
        }

    }

    protected void broadcastAndSend(Entity entity, Packet<?> packet) {
        PlayerChunkMap.EntityTracker playerchunkmap_entitytracker = (PlayerChunkMap.EntityTracker) this.entityMap.get(entity.getId());

        if (playerchunkmap_entitytracker != null) {
            playerchunkmap_entitytracker.broadcastAndSend(packet);
        }

    }

    private void playerLoadedChunk(EntityPlayer entityplayer, MutableObject<ClientboundLevelChunkWithLightPacket> mutableobject, Chunk chunk) {
        if (mutableobject.getValue() == null) {
            mutableobject.setValue(new ClientboundLevelChunkWithLightPacket(chunk, this.lightEngine, (BitSet) null, (BitSet) null, true));
        }

        entityplayer.trackChunk(chunk.getPos(), (Packet) mutableobject.getValue());
        PacketDebug.sendPoiPacketsForChunk(this.level, chunk.getPos());
        List<Entity> list = Lists.newArrayList();
        List<Entity> list1 = Lists.newArrayList();
        ObjectIterator objectiterator = this.entityMap.values().iterator();

        while (objectiterator.hasNext()) {
            PlayerChunkMap.EntityTracker playerchunkmap_entitytracker = (PlayerChunkMap.EntityTracker) objectiterator.next();
            Entity entity = playerchunkmap_entitytracker.entity;

            if (entity != entityplayer && entity.chunkPosition().equals(chunk.getPos())) {
                playerchunkmap_entitytracker.updatePlayer(entityplayer);
                if (entity instanceof EntityInsentient && ((EntityInsentient) entity).getLeashHolder() != null) {
                    list.add(entity);
                }

                if (!entity.getPassengers().isEmpty()) {
                    list1.add(entity);
                }
            }
        }

        Iterator iterator;
        Entity entity1;

        if (!list.isEmpty()) {
            iterator = list.iterator();

            while (iterator.hasNext()) {
                entity1 = (Entity) iterator.next();
                entityplayer.connection.send(new PacketPlayOutAttachEntity(entity1, ((EntityInsentient) entity1).getLeashHolder()));
            }
        }

        if (!list1.isEmpty()) {
            iterator = list1.iterator();

            while (iterator.hasNext()) {
                entity1 = (Entity) iterator.next();
                entityplayer.connection.send(new PacketPlayOutMount(entity1));
            }
        }

    }

    protected VillagePlace getPoiManager() {
        return this.poiManager;
    }

    public String getStorageName() {
        return this.storageName;
    }

    void onFullChunkStatusChange(ChunkCoordIntPair chunkcoordintpair, PlayerChunk.State playerchunk_state) {
        this.chunkStatusListener.onChunkStatusChange(chunkcoordintpair, playerchunk_state);
    }

    private class a extends ChunkMapDistance {

        protected a(Executor executor, Executor executor1) {
            super(executor, executor1);
        }

        @Override
        protected boolean isChunkToRemove(long i) {
            return PlayerChunkMap.this.toDrop.contains(i);
        }

        @Nullable
        @Override
        protected PlayerChunk getChunk(long i) {
            return PlayerChunkMap.this.getUpdatingChunkIfPresent(i);
        }

        @Nullable
        @Override
        protected PlayerChunk updateChunkScheduling(long i, int j, @Nullable PlayerChunk playerchunk, int k) {
            return PlayerChunkMap.this.updateChunkScheduling(i, j, playerchunk, k);
        }
    }

    public class EntityTracker {

        final EntityTrackerEntry serverEntity;
        final Entity entity;
        private final int range;
        SectionPosition lastSectionPos;
        public final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

        public EntityTracker(Entity entity, int i, int j, boolean flag) {
            this.serverEntity = new EntityTrackerEntry(PlayerChunkMap.this.level, entity, j, flag, this::broadcast, seenBy); // CraftBukkit
            this.entity = entity;
            this.range = i;
            this.lastSectionPos = SectionPosition.of(entity);
        }

        public boolean equals(Object object) {
            return object instanceof PlayerChunkMap.EntityTracker ? ((PlayerChunkMap.EntityTracker) object).entity.getId() == this.entity.getId() : false;
        }

        public int hashCode() {
            return this.entity.getId();
        }

        public void broadcast(Packet<?> packet) {
            Iterator iterator = this.seenBy.iterator();

            while (iterator.hasNext()) {
                ServerPlayerConnection serverplayerconnection = (ServerPlayerConnection) iterator.next();

                serverplayerconnection.send(packet);
            }

        }

        public void broadcastAndSend(Packet<?> packet) {
            this.broadcast(packet);
            if (this.entity instanceof EntityPlayer) {
                ((EntityPlayer) this.entity).connection.send(packet);
            }

        }

        public void broadcastRemoved() {
            Iterator iterator = this.seenBy.iterator();

            while (iterator.hasNext()) {
                ServerPlayerConnection serverplayerconnection = (ServerPlayerConnection) iterator.next();

                this.serverEntity.removePairing(serverplayerconnection.getPlayer());
            }

        }

        public void removePlayer(EntityPlayer entityplayer) {
            org.spigotmc.AsyncCatcher.catchOp("player tracker clear"); // Spigot
            if (this.seenBy.remove(entityplayer.connection)) {
                this.serverEntity.removePairing(entityplayer);
            }

        }

        public void updatePlayer(EntityPlayer entityplayer) {
            org.spigotmc.AsyncCatcher.catchOp("player tracker update"); // Spigot
            if (entityplayer != this.entity) {
                Vec3D vec3d = entityplayer.position().subtract(this.entity.position()); // MC-155077, SPIGOT-5113
                double d0 = (double) Math.min(this.getEffectiveRange(), (PlayerChunkMap.this.viewDistance - 1) * 16);
                double d1 = vec3d.x * vec3d.x + vec3d.z * vec3d.z;
                double d2 = d0 * d0;
                boolean flag = d1 <= d2 && this.entity.broadcastToPlayer(entityplayer);

                // CraftBukkit start - respect vanish API
                if (!entityplayer.getBukkitEntity().canSee(this.entity.getBukkitEntity())) {
                    flag = false;
                }
                // CraftBukkit end
                if (flag) {
                    if (this.seenBy.add(entityplayer.connection)) {
                        this.serverEntity.addPairing(entityplayer);
                    }
                } else if (this.seenBy.remove(entityplayer.connection)) {
                    this.serverEntity.removePairing(entityplayer);
                }

            }
        }

        private int scaledRange(int i) {
            return PlayerChunkMap.this.level.getServer().getScaledTrackingDistance(i);
        }

        private int getEffectiveRange() {
            int i = this.range;
            Iterator iterator = this.entity.getIndirectPassengers().iterator();

            while (iterator.hasNext()) {
                Entity entity = (Entity) iterator.next();
                int j = entity.getType().clientTrackingRange() * 16;

                if (j > i) {
                    i = j;
                }
            }

            return this.scaledRange(i);
        }

        public void updatePlayers(List<EntityPlayer> list) {
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                this.updatePlayer(entityplayer);
            }

        }
    }
}
