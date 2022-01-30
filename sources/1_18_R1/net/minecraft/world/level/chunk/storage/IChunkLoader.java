package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.PersistentStructureLegacy;
import net.minecraft.world.level.storage.WorldPersistentData;

// CraftBukkit start
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.WorldDimension;
// CraftBukkit end

public class IChunkLoader implements AutoCloseable {

    public static final int LAST_MONOLYTH_STRUCTURE_DATA_VERSION = 1493;
    private final IOWorker worker;
    protected final DataFixer fixerUpper;
    @Nullable
    private PersistentStructureLegacy legacyStructureHandler;

    public IChunkLoader(Path path, DataFixer datafixer, boolean flag) {
        this.fixerUpper = datafixer;
        this.worker = new IOWorker(path, flag, "chunk");
    }

    // CraftBukkit start
    private boolean check(ChunkProviderServer cps, int x, int z) throws IOException {
        ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
        if (cps != null) {
            com.google.common.base.Preconditions.checkState(org.bukkit.Bukkit.isPrimaryThread(), "primary thread");
            if (cps.hasChunk(x, z)) {
                return true;
            }
        }

        NBTTagCompound nbt = read(pos);
        if (nbt != null) {
            NBTTagCompound level = nbt.getCompound("Level");
            if (level.getBoolean("TerrainPopulated")) {
                return true;
            }

            ChunkStatus status = ChunkStatus.byName(level.getString("Status"));
            if (status != null && status.isOrAfter(ChunkStatus.FEATURES)) {
                return true;
            }
        }

        return false;
    }

    public NBTTagCompound upgradeChunkTag(ResourceKey<WorldDimension> resourcekey, Supplier<WorldPersistentData> supplier, NBTTagCompound nbttagcompound, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> optional, ChunkCoordIntPair pos, @Nullable GeneratorAccess generatoraccess) throws IOException {
        // CraftBukkit end
        int i = getVersion(nbttagcompound);

        // CraftBukkit start
        if (i < 1466) {
            NBTTagCompound level = nbttagcompound.getCompound("Level");
            if (level.getBoolean("TerrainPopulated") && !level.getBoolean("LightPopulated")) {
                ChunkProviderServer cps = (generatoraccess == null) ? null : ((WorldServer) generatoraccess).getChunkSource();
                if (check(cps, pos.x - 1, pos.z) && check(cps, pos.x - 1, pos.z - 1) && check(cps, pos.x, pos.z - 1)) {
                    level.putBoolean("LightPopulated", true);
                }
            }
        }
        // CraftBukkit end

        if (i < 1493) {
            nbttagcompound = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.CHUNK, nbttagcompound, i, 1493);
            if (nbttagcompound.getCompound("Level").getBoolean("hasLegacyStructureData")) {
                if (this.legacyStructureHandler == null) {
                    this.legacyStructureHandler = PersistentStructureLegacy.getLegacyStructureHandler(resourcekey, (WorldPersistentData) supplier.get());
                }

                nbttagcompound = this.legacyStructureHandler.updateFromLegacy(nbttagcompound);
            }
        }

        // Spigot start - SPIGOT-6806: Quick and dirty way to prevent below zero generation in old chunks, by setting the status to heightmap instead of empty
        boolean stopBelowZero = false;
        boolean belowZeroGenerationInExistingChunks = (generatoraccess != null) ? ((WorldServer) generatoraccess).spigotConfig.belowZeroGenerationInExistingChunks : org.spigotmc.SpigotConfig.belowZeroGenerationInExistingChunks;

        if (i <= 2730 && !belowZeroGenerationInExistingChunks) {
            stopBelowZero = ChunkStatus.FULL.getName().equals(nbttagcompound.getCompound("Level").getString("Status"));
        }
        // Spigot end

        injectDatafixingContext(nbttagcompound, resourcekey, optional);
        nbttagcompound = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.CHUNK, nbttagcompound, Math.max(1493, i));
        if (i < SharedConstants.getCurrentVersion().getWorldVersion()) {
            nbttagcompound.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        }

        // Spigot start
        if (stopBelowZero) {
            nbttagcompound.putString("Status", ChunkStatus.HEIGHTMAPS.getName());
        }
        // Spigot end

        nbttagcompound.remove("__context");
        return nbttagcompound;
    }

    public static void injectDatafixingContext(NBTTagCompound nbttagcompound, ResourceKey<WorldDimension> resourcekey, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> optional) { // CraftBukkit
        NBTTagCompound nbttagcompound1 = new NBTTagCompound();

        nbttagcompound1.putString("dimension", resourcekey.location().toString());
        optional.ifPresent((resourcekey1) -> {
            nbttagcompound1.putString("generator", resourcekey1.location().toString());
        });
        nbttagcompound.put("__context", nbttagcompound1);
    }

    public static int getVersion(NBTTagCompound nbttagcompound) {
        return nbttagcompound.contains("DataVersion", 99) ? nbttagcompound.getInt("DataVersion") : -1;
    }

    @Nullable
    public NBTTagCompound read(ChunkCoordIntPair chunkcoordintpair) throws IOException {
        return this.worker.load(chunkcoordintpair);
    }

    public void write(ChunkCoordIntPair chunkcoordintpair, NBTTagCompound nbttagcompound) {
        this.worker.store(chunkcoordintpair, nbttagcompound);
        if (this.legacyStructureHandler != null) {
            this.legacyStructureHandler.removeIndex(chunkcoordintpair.toLong());
        }

    }

    public void flushWorker() {
        this.worker.synchronize(true).join();
    }

    public void close() throws IOException {
        this.worker.close();
    }

    public ChunkScanAccess chunkScanner() {
        return this.worker;
    }
}
