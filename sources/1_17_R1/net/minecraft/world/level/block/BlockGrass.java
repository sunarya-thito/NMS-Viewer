package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.WorldGenFlowers;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;

public class BlockGrass extends BlockDirtSnowSpreadable implements IBlockFragilePlantElement {

    public BlockGrass(BlockBase.Info blockbase_info) {
        super(blockbase_info);
    }

    @Override
    public boolean a(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, boolean flag) {
        return iblockaccess.getType(blockposition.up()).isAir();
    }

    @Override
    public boolean a(World world, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        return true;
    }

    @Override
    public void a(WorldServer worldserver, Random random, BlockPosition blockposition, IBlockData iblockdata) {
        BlockPosition blockposition1 = blockposition.up();
        IBlockData iblockdata1 = Blocks.GRASS.getBlockData();
        int i = 0;

        while (i < 128) {
            BlockPosition blockposition2 = blockposition1;
            int j = 0;

            while (true) {
                if (j < i / 16) {
                    blockposition2 = blockposition2.c(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                    if (worldserver.getType(blockposition2.down()).a((Block) this) && !worldserver.getType(blockposition2).r(worldserver, blockposition2)) {
                        ++j;
                        continue;
                    }
                } else {
                    IBlockData iblockdata2 = worldserver.getType(blockposition2);

                    if (iblockdata2.a(iblockdata1.getBlock()) && random.nextInt(10) == 0) {
                        ((IBlockFragilePlantElement) iblockdata1.getBlock()).a(worldserver, random, blockposition2, iblockdata2);
                    }

                    if (iblockdata2.isAir()) {
                        label38:
                        {
                            IBlockData iblockdata3;

                            if (random.nextInt(8) == 0) {
                                List<WorldGenFeatureConfigured<?, ?>> list = worldserver.getBiome(blockposition2).e().b();

                                if (list.isEmpty()) {
                                    break label38;
                                }

                                iblockdata3 = a(random, blockposition2, (WorldGenFeatureConfigured) list.get(0));
                            } else {
                                iblockdata3 = iblockdata1;
                            }

                            if (iblockdata3.canPlace(worldserver, blockposition2)) {
                                org.bukkit.craftbukkit.event.CraftEventFactory.handleBlockGrowEvent(worldserver, blockposition2, iblockdata3, 3); // CraftBukkit
                            }
                        }
                    }
                }

                ++i;
                break;
            }
        }

    }

    private static <U extends WorldGenFeatureConfiguration> IBlockData a(Random random, BlockPosition blockposition, WorldGenFeatureConfigured<U, ?> worldgenfeatureconfigured) {
        WorldGenFlowers<U> worldgenflowers = (WorldGenFlowers) worldgenfeatureconfigured.feature;

        return worldgenflowers.b(random, blockposition, worldgenfeatureconfigured.c());
    }
}
