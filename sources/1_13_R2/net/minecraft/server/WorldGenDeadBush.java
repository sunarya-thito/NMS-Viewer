package net.minecraft.server;

import java.util.Random;

public class WorldGenDeadBush extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {

    private static final BlockDeadBush a = (BlockDeadBush) Blocks.DEAD_BUSH;

    public WorldGenDeadBush() {}

    public boolean a(GeneratorAccess generatoraccess, ChunkGenerator<? extends GeneratorSettings> chunkgenerator, Random random, BlockPosition blockposition, WorldGenFeatureEmptyConfiguration worldgenfeatureemptyconfiguration) {
        for (IBlockData iblockdata = generatoraccess.getType(blockposition); (iblockdata.isAir() || iblockdata.a(TagsBlock.LEAVES)) && blockposition.getY() > 0; iblockdata = generatoraccess.getType(blockposition)) {
            blockposition = blockposition.down();
        }

        IBlockData iblockdata1 = WorldGenDeadBush.a.getBlockData();

        for (int i = 0; i < 4; ++i) {
            BlockPosition blockposition1 = blockposition.a(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));

            if (generatoraccess.isEmpty(blockposition1) && iblockdata1.canPlace(generatoraccess, blockposition1)) {
                generatoraccess.setTypeAndData(blockposition1, iblockdata1, 2);
            }
        }

        return true;
    }
}
