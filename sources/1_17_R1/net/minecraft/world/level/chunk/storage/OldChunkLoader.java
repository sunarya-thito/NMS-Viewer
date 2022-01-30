package net.minecraft.world.level.chunk.storage;

import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.BiomeStorage;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.level.chunk.OldNibbleArray;

public class OldChunkLoader {

    private static final int DATALAYER_BITS = 7;
    private static final LevelHeightAccessor OLD_LEVEL_HEIGHT = new LevelHeightAccessor() {
        @Override
        public int getMinBuildHeight() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 128;
        }
    };

    public OldChunkLoader() {}

    public static OldChunkLoader.OldChunk a(NBTTagCompound nbttagcompound) {
        int i = nbttagcompound.getInt("xPos");
        int j = nbttagcompound.getInt("zPos");
        OldChunkLoader.OldChunk oldchunkloader_oldchunk = new OldChunkLoader.OldChunk(i, j);

        oldchunkloader_oldchunk.blocks = nbttagcompound.getByteArray("Blocks");
        oldchunkloader_oldchunk.data = new OldNibbleArray(nbttagcompound.getByteArray("Data"), 7);
        oldchunkloader_oldchunk.skyLight = new OldNibbleArray(nbttagcompound.getByteArray("SkyLight"), 7);
        oldchunkloader_oldchunk.blockLight = new OldNibbleArray(nbttagcompound.getByteArray("BlockLight"), 7);
        oldchunkloader_oldchunk.heightmap = nbttagcompound.getByteArray("HeightMap");
        oldchunkloader_oldchunk.terrainPopulated = nbttagcompound.getBoolean("TerrainPopulated");
        oldchunkloader_oldchunk.entities = nbttagcompound.getList("Entities", 10);
        oldchunkloader_oldchunk.blockEntities = nbttagcompound.getList("TileEntities", 10);
        oldchunkloader_oldchunk.blockTicks = nbttagcompound.getList("TileTicks", 10);

        try {
            oldchunkloader_oldchunk.lastUpdated = nbttagcompound.getLong("LastUpdate");
        } catch (ClassCastException classcastexception) {
            oldchunkloader_oldchunk.lastUpdated = (long) nbttagcompound.getInt("LastUpdate");
        }

        return oldchunkloader_oldchunk;
    }

    public static void a(IRegistryCustom.Dimension iregistrycustom_dimension, OldChunkLoader.OldChunk oldchunkloader_oldchunk, NBTTagCompound nbttagcompound, WorldChunkManager worldchunkmanager) {
        nbttagcompound.setInt("xPos", oldchunkloader_oldchunk.x);
        nbttagcompound.setInt("zPos", oldchunkloader_oldchunk.z);
        nbttagcompound.setLong("LastUpdate", oldchunkloader_oldchunk.lastUpdated);
        int[] aint = new int[oldchunkloader_oldchunk.heightmap.length];

        for (int i = 0; i < oldchunkloader_oldchunk.heightmap.length; ++i) {
            aint[i] = oldchunkloader_oldchunk.heightmap[i];
        }

        nbttagcompound.setIntArray("HeightMap", aint);
        nbttagcompound.setBoolean("TerrainPopulated", oldchunkloader_oldchunk.terrainPopulated);
        NBTTagList nbttaglist = new NBTTagList();

        for (int j = 0; j < 8; ++j) {
            boolean flag = true;

            for (int k = 0; k < 16 && flag; ++k) {
                int l = 0;

                while (l < 16 && flag) {
                    int i1 = 0;

                    while (true) {
                        if (i1 < 16) {
                            int j1 = k << 11 | i1 << 7 | l + (j << 4);
                            byte b0 = oldchunkloader_oldchunk.blocks[j1];

                            if (b0 == 0) {
                                ++i1;
                                continue;
                            }

                            flag = false;
                        }

                        ++l;
                        break;
                    }
                }
            }

            if (!flag) {
                byte[] abyte = new byte[4096];
                NibbleArray nibblearray = new NibbleArray();
                NibbleArray nibblearray1 = new NibbleArray();
                NibbleArray nibblearray2 = new NibbleArray();

                for (int k1 = 0; k1 < 16; ++k1) {
                    for (int l1 = 0; l1 < 16; ++l1) {
                        for (int i2 = 0; i2 < 16; ++i2) {
                            int j2 = k1 << 11 | i2 << 7 | l1 + (j << 4);
                            byte b1 = oldchunkloader_oldchunk.blocks[j2];

                            abyte[l1 << 8 | i2 << 4 | k1] = (byte) (b1 & 255);
                            nibblearray.a(k1, l1, i2, oldchunkloader_oldchunk.data.a(k1, l1 + (j << 4), i2));
                            nibblearray1.a(k1, l1, i2, oldchunkloader_oldchunk.skyLight.a(k1, l1 + (j << 4), i2));
                            nibblearray2.a(k1, l1, i2, oldchunkloader_oldchunk.blockLight.a(k1, l1 + (j << 4), i2));
                        }
                    }
                }

                NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                nbttagcompound1.setByte("Y", (byte) (j & 255));
                nbttagcompound1.setByteArray("Blocks", abyte);
                nbttagcompound1.setByteArray("Data", nibblearray.asBytes());
                nbttagcompound1.setByteArray("SkyLight", nibblearray1.asBytes());
                nbttagcompound1.setByteArray("BlockLight", nibblearray2.asBytes());
                nbttaglist.add(nbttagcompound1);
            }
        }

        nbttagcompound.set("Sections", nbttaglist);
        nbttagcompound.setIntArray("Biomes", (new BiomeStorage(iregistrycustom_dimension.d(IRegistry.BIOME_REGISTRY), OldChunkLoader.OLD_LEVEL_HEIGHT, new ChunkCoordIntPair(oldchunkloader_oldchunk.x, oldchunkloader_oldchunk.z), worldchunkmanager)).a());
        nbttagcompound.set("Entities", oldchunkloader_oldchunk.entities);
        nbttagcompound.set("TileEntities", oldchunkloader_oldchunk.blockEntities);
        if (oldchunkloader_oldchunk.blockTicks != null) {
            nbttagcompound.set("TileTicks", oldchunkloader_oldchunk.blockTicks);
        }

        nbttagcompound.setBoolean("convertedFromAlphaFormat", true);
    }

    public static class OldChunk {

        public long lastUpdated;
        public boolean terrainPopulated;
        public byte[] heightmap;
        public OldNibbleArray blockLight;
        public OldNibbleArray skyLight;
        public OldNibbleArray data;
        public byte[] blocks;
        public NBTTagList entities;
        public NBTTagList blockEntities;
        public NBTTagList blockTicks;
        public final int x;
        public final int z;

        public OldChunk(int i, int j) {
            this.x = i;
            this.z = j;
        }
    }
}
