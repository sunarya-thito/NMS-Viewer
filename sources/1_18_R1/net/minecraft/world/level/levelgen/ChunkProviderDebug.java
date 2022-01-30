package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.BlockColumn;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.WorldChunkManagerHell;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;

public class ChunkProviderDebug extends ChunkGenerator {

    public static final Codec<ChunkProviderDebug> CODEC = RegistryLookupCodec.create(IRegistry.BIOME_REGISTRY).xmap(ChunkProviderDebug::new, ChunkProviderDebug::biomes).stable().codec();
    private static final int BLOCK_MARGIN = 2;
    private static final List<IBlockData> ALL_BLOCKS = (List) StreamSupport.stream(IRegistry.BLOCK.spliterator(), false).flatMap((block) -> {
        return block.getStateDefinition().getPossibleStates().stream();
    }).collect(Collectors.toList());
    private static final int GRID_WIDTH = MathHelper.ceil(MathHelper.sqrt((float) ChunkProviderDebug.ALL_BLOCKS.size()));
    private static final int GRID_HEIGHT = MathHelper.ceil((float) ChunkProviderDebug.ALL_BLOCKS.size() / (float) ChunkProviderDebug.GRID_WIDTH);
    protected static final IBlockData AIR = Blocks.AIR.defaultBlockState();
    protected static final IBlockData BARRIER = Blocks.BARRIER.defaultBlockState();
    public static final int HEIGHT = 70;
    public static final int BARRIER_HEIGHT = 60;
    private final IRegistry<BiomeBase> biomes;

    public ChunkProviderDebug(IRegistry<BiomeBase> iregistry) {
        super(new WorldChunkManagerHell((BiomeBase) iregistry.getOrThrow(Biomes.PLAINS)), new StructureSettings(false));
        this.biomes = iregistry;
    }

    public IRegistry<BiomeBase> biomes() {
        return this.biomes;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return ChunkProviderDebug.CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long i) {
        return this;
    }

    @Override
    public void buildSurface(RegionLimitedWorldAccess regionlimitedworldaccess, StructureManager structuremanager, IChunkAccess ichunkaccess) {}

    @Override
    public void applyBiomeDecoration(GeneratorAccessSeed generatoraccessseed, IChunkAccess ichunkaccess, StructureManager structuremanager) {
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();
        ChunkCoordIntPair chunkcoordintpair = ichunkaccess.getPos();
        int i = chunkcoordintpair.x;
        int j = chunkcoordintpair.z;

        for (int k = 0; k < 16; ++k) {
            for (int l = 0; l < 16; ++l) {
                int i1 = SectionPosition.sectionToBlockCoord(i, k);
                int j1 = SectionPosition.sectionToBlockCoord(j, l);

                generatoraccessseed.setBlock(blockposition_mutableblockposition.set(i1, 60, j1), ChunkProviderDebug.BARRIER, 2);
                IBlockData iblockdata = getBlockStateFor(i1, j1);

                generatoraccessseed.setBlock(blockposition_mutableblockposition.set(i1, 70, j1), iblockdata, 2);
            }
        }

    }

    @Override
    public CompletableFuture<IChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureManager structuremanager, IChunkAccess ichunkaccess) {
        return CompletableFuture.completedFuture(ichunkaccess);
    }

    @Override
    public int getBaseHeight(int i, int j, HeightMap.Type heightmap_type, LevelHeightAccessor levelheightaccessor) {
        return 0;
    }

    @Override
    public BlockColumn getBaseColumn(int i, int j, LevelHeightAccessor levelheightaccessor) {
        return new BlockColumn(0, new IBlockData[0]);
    }

    public static IBlockData getBlockStateFor(int i, int j) {
        IBlockData iblockdata = ChunkProviderDebug.AIR;

        if (i > 0 && j > 0 && i % 2 != 0 && j % 2 != 0) {
            i /= 2;
            j /= 2;
            if (i <= ChunkProviderDebug.GRID_WIDTH && j <= ChunkProviderDebug.GRID_HEIGHT) {
                int k = MathHelper.abs(i * ChunkProviderDebug.GRID_WIDTH + j);

                if (k < ChunkProviderDebug.ALL_BLOCKS.size()) {
                    iblockdata = (IBlockData) ChunkProviderDebug.ALL_BLOCKS.get(k);
                }
            }
        }

        return iblockdata;
    }

    @Override
    public Climate.Sampler climateSampler() {
        return (i, j, k) -> {
            return Climate.target(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        };
    }

    @Override
    public void applyCarvers(RegionLimitedWorldAccess regionlimitedworldaccess, long i, BiomeManager biomemanager, StructureManager structuremanager, IChunkAccess ichunkaccess, WorldGenStage.Features worldgenstage_features) {}

    @Override
    public void spawnOriginalMobs(RegionLimitedWorldAccess regionlimitedworldaccess) {}

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }
}
