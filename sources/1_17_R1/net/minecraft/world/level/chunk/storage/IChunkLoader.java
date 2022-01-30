package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.levelgen.structure.PersistentStructureLegacy;
import net.minecraft.world.level.storage.WorldPersistentData;

// CraftBukkit start
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionManager;
// CraftBukkit end

public class IChunkLoader implements AutoCloseable {

    private final IOWorker worker;
    protected final DataFixer fixerUpper;
    @Nullable
    private PersistentStructureLegacy legacyStructureHandler;

    public IChunkLoader(File file, DataFixer datafixer, boolean flag) {
        this.fixerUpper = datafixer;
        this.worker = new IOWorker(file, flag, "chunk");
    }

    // CraftBukkit start
    private boolean check(ChunkProviderServer cps, int x, int z) throws IOException {
        ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);
        if (cps != null) {
            com.google.common.base.Preconditions.checkState(org.bukkit.Bukkit.isPrimaryThread(), "primary thread");
            if (cps.isLoaded(x, z)) {
                return true;
            }
        }

        NBTTagCompound nbt = read(pos);
        if (nbt != null) {
            NBTTagCompound level = nbt.getCompound("Level");
            if (level.getBoolean("TerrainPopulated")) {
                return true;
            }

            ChunkStatus status = ChunkStatus.a(level.getString("Status"));
            if (status != null && status.b(ChunkStatus.FEATURES)) {
                return true;
            }
        }

        return false;
    }

    public NBTTagCompound getChunkData(ResourceKey<DimensionManager> resourcekey, Supplier<WorldPersistentData> supplier, NBTTagCompound nbttagcompound, ChunkCoordIntPair pos, @Nullable GeneratorAccess generatoraccess) throws IOException {
        // CraftBukkit end
        int i = a(nbttagcompound);
        boolean flag = true;

        // CraftBukkit start
        if (i < 1466) {
            NBTTagCompound level = nbttagcompound.getCompound("Level");
            if (level.getBoolean("TerrainPopulated") && !level.getBoolean("LightPopulated")) {
                ChunkProviderServer cps = (generatoraccess == null) ? null : ((WorldServer) generatoraccess).getChunkProvider();
                if (check(cps, pos.x - 1, pos.z) && check(cps, pos.x - 1, pos.z - 1) && check(cps, pos.x, pos.z - 1)) {
                    level.setBoolean("LightPopulated", true);
                }
            }
        }
        // CraftBukkit end

        if (i < 1493) {
            nbttagcompound = GameProfileSerializer.a(this.fixerUpper, DataFixTypes.CHUNK, nbttagcompound, i, 1493);
            if (nbttagcompound.getCompound("Level").getBoolean("hasLegacyStructureData")) {
                if (this.legacyStructureHandler == null) {
                    this.legacyStructureHandler = PersistentStructureLegacy.a(resourcekey, (WorldPersistentData) supplier.get());
                }

                nbttagcompound = this.legacyStructureHandler.a(nbttagcompound);
            }
        }

        nbttagcompound = GameProfileSerializer.a(this.fixerUpper, DataFixTypes.CHUNK, nbttagcompound, Math.max(1493, i));
        if (i < SharedConstants.getGameVersion().getWorldVersion()) {
            nbttagcompound.setInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        }

        return nbttagcompound;
    }

    public static int a(NBTTagCompound nbttagcompound) {
        return nbttagcompound.hasKeyOfType("DataVersion", 99) ? nbttagcompound.getInt("DataVersion") : -1;
    }

    @Nullable
    public NBTTagCompound read(ChunkCoordIntPair chunkcoordintpair) throws IOException {
        return this.worker.a(chunkcoordintpair);
    }

    public void a(ChunkCoordIntPair chunkcoordintpair, NBTTagCompound nbttagcompound) {
        this.worker.a(chunkcoordintpair, nbttagcompound);
        if (this.legacyStructureHandler != null) {
            this.legacyStructureHandler.a(chunkcoordintpair.pair());
        }

    }

    public void j() {
        this.worker.a(true).join();
    }

    public void close() throws IOException {
        this.worker.close();
    }
}
