package net.minecraft.server;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;

public interface IChunkAccess extends IStructureAccess {

    @Nullable
    IBlockData setType(BlockPosition blockposition, IBlockData iblockdata, boolean flag);

    void setTileEntity(BlockPosition blockposition, TileEntity tileentity);

    void a(Entity entity);

    @Nullable
    default ChunkSection a() {
        ChunkSection[] achunksection = this.getSections();

        for (int i = achunksection.length - 1; i >= 0; --i) {
            ChunkSection chunksection = achunksection[i];

            if (!ChunkSection.a(chunksection)) {
                return chunksection;
            }
        }

        return null;
    }

    default int b() {
        ChunkSection chunksection = this.a();

        return chunksection == null ? 0 : chunksection.getYPosition();
    }

    Set<BlockPosition> c();

    ChunkSection[] getSections();

    @Nullable
    LightEngine e();

    default int a(BlockPosition blockposition, int i, boolean flag) {
        LightEngine lightengine = this.e();

        if (lightengine != null && this.getChunkStatus().b(ChunkStatus.LIGHT)) {
            int j = flag ? lightengine.a(EnumSkyBlock.SKY).b(blockposition) - i : 0;
            int k = lightengine.a(EnumSkyBlock.BLOCK).b(blockposition);

            return Math.max(k, j);
        } else {
            return 0;
        }
    }

    Collection<Entry<HeightMap.Type, HeightMap>> f();

    void a(HeightMap.Type heightmap_type, long[] along);

    HeightMap b(HeightMap.Type heightmap_type);

    int a(HeightMap.Type heightmap_type, int i, int j);

    ChunkCoordIntPair getPos();

    void setLastSaved(long i);

    Map<String, StructureStart> h();

    void a(Map<String, StructureStart> map);

    default BiomeBase getBiome(BlockPosition blockposition) {
        int i = blockposition.getX() & 15;
        int j = blockposition.getZ() & 15;

        return this.getBiomeIndex()[j << 4 | i];
    }

    default boolean a(int i, int j) {
        if (i < 0) {
            i = 0;
        }

        if (j >= 256) {
            j = 255;
        }

        for (int k = i; k <= j; k += 16) {
            if (!ChunkSection.a(this.getSections()[k >> 4])) {
                return false;
            }
        }

        return true;
    }

    BiomeBase[] getBiomeIndex();

    void setNeedsSaving(boolean flag);

    boolean isNeedsSaving();

    ChunkStatus getChunkStatus();

    void removeTileEntity(BlockPosition blockposition);

    void a(LightEngine lightengine);

    default void f(BlockPosition blockposition) {
        LogManager.getLogger().warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", blockposition);
    }

    ShortList[] l();

    default void a(short short0, int i) {
        a(this.l(), i).add(short0);
    }

    default void a(NBTTagCompound nbttagcompound) {
        LogManager.getLogger().warn("Trying to set a BlockEntity, but this operation is not supported.");
    }

    @Nullable
    NBTTagCompound i(BlockPosition blockposition);

    @Nullable
    NBTTagCompound j(BlockPosition blockposition);

    default void a(BiomeBase[] abiomebase) {
        throw new UnsupportedOperationException();
    }

    Stream<BlockPosition> m();

    TickList<Block> n();

    TickList<FluidType> o();

    default BitSet a(WorldGenStage.Features worldgenstage_features) {
        throw new RuntimeException("Meaningless in this context");
    }

    ChunkConverter p();

    void b(long i);

    long q();

    static ShortList a(ShortList[] ashortlist, int i) {
        if (ashortlist[i] == null) {
            ashortlist[i] = new ShortArrayList();
        }

        return ashortlist[i];
    }

    boolean r();

    void b(boolean flag);
}
