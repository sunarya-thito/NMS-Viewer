package net.minecraft.server;

import com.mojang.datafixers.Dynamic;
import java.util.Random;
import java.util.function.Function;

public class WorldGenHugeMushroomBrown extends WorldGenMushrooms {

    public WorldGenHugeMushroomBrown(Function<Dynamic<?>, ? extends WorldGenFeatureMushroomConfiguration> function) {
        super(function);
    }

    @Override
    protected void a(GeneratorAccess generatoraccess, Random random, BlockPosition blockposition, int i, BlockPosition.MutableBlockPosition blockposition_mutableblockposition, WorldGenFeatureMushroomConfiguration worldgenfeaturemushroomconfiguration) {
        int j = worldgenfeaturemushroomconfiguration.c;

        for (int k = -j; k <= j; ++k) {
            for (int l = -j; l <= j; ++l) {
                boolean flag = k == -j;
                boolean flag1 = k == j;
                boolean flag2 = l == -j;
                boolean flag3 = l == j;
                boolean flag4 = flag || flag1;
                boolean flag5 = flag2 || flag3;

                if (!flag4 || !flag5) {
                    blockposition_mutableblockposition.g(blockposition).e(k, i, l);
                    if (!generatoraccess.getType(blockposition_mutableblockposition).g(generatoraccess, blockposition_mutableblockposition)) {
                        boolean flag6 = flag || flag5 && k == 1 - j;
                        boolean flag7 = flag1 || flag5 && k == j - 1;
                        boolean flag8 = flag2 || flag4 && l == 1 - j;
                        boolean flag9 = flag3 || flag4 && l == j - 1;

                        this.a(generatoraccess, blockposition_mutableblockposition, (IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) worldgenfeaturemushroomconfiguration.a.a(random, blockposition).set(BlockHugeMushroom.d, flag6)).set(BlockHugeMushroom.b, flag7)).set(BlockHugeMushroom.a, flag8)).set(BlockHugeMushroom.c, flag9));
                    }
                }
            }
        }

    }

    @Override
    protected int a(int i, int j, int k, int l) {
        return l <= 3 ? 0 : k;
    }
}