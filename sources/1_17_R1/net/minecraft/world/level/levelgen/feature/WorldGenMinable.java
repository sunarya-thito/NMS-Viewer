package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOreConfiguration;

public class WorldGenMinable extends WorldGenerator<WorldGenFeatureOreConfiguration> {

    public WorldGenMinable(Codec<WorldGenFeatureOreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureOreConfiguration> featureplacecontext) {
        Random random = featureplacecontext.c();
        BlockPosition blockposition = featureplacecontext.d();
        GeneratorAccessSeed generatoraccessseed = featureplacecontext.a();
        WorldGenFeatureOreConfiguration worldgenfeatureoreconfiguration = (WorldGenFeatureOreConfiguration) featureplacecontext.e();
        float f = random.nextFloat() * 3.1415927F;
        float f1 = (float) worldgenfeatureoreconfiguration.size / 8.0F;
        int i = MathHelper.f(((float) worldgenfeatureoreconfiguration.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double d0 = (double) blockposition.getX() + Math.sin((double) f) * (double) f1;
        double d1 = (double) blockposition.getX() - Math.sin((double) f) * (double) f1;
        double d2 = (double) blockposition.getZ() + Math.cos((double) f) * (double) f1;
        double d3 = (double) blockposition.getZ() - Math.cos((double) f) * (double) f1;
        boolean flag = true;
        double d4 = (double) (blockposition.getY() + random.nextInt(3) - 2);
        double d5 = (double) (blockposition.getY() + random.nextInt(3) - 2);
        int j = blockposition.getX() - MathHelper.f(f1) - i;
        int k = blockposition.getY() - 2 - i;
        int l = blockposition.getZ() - MathHelper.f(f1) - i;
        int i1 = 2 * (MathHelper.f(f1) + i);
        int j1 = 2 * (2 + i);

        for (int k1 = j; k1 <= j + i1; ++k1) {
            for (int l1 = l; l1 <= l + i1; ++l1) {
                if (k <= generatoraccessseed.a(HeightMap.Type.OCEAN_FLOOR_WG, k1, l1)) {
                    return this.a(generatoraccessseed, random, worldgenfeatureoreconfiguration, d0, d1, d2, d3, d4, d5, j, k, l, i1, j1);
                }
            }
        }

        return false;
    }

    protected boolean a(GeneratorAccessSeed generatoraccessseed, Random random, WorldGenFeatureOreConfiguration worldgenfeatureoreconfiguration, double d0, double d1, double d2, double d3, double d4, double d5, int i, int j, int k, int l, int i1) {
        int j1 = 0;
        BitSet bitset = new BitSet(l * i1 * l);
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();
        int k1 = worldgenfeatureoreconfiguration.size;
        double[] adouble = new double[k1 * 4];

        int l1;
        double d6;
        double d7;
        double d8;
        double d9;

        for (l1 = 0; l1 < k1; ++l1) {
            float f = (float) l1 / (float) k1;

            d6 = MathHelper.d((double) f, d0, d1);
            d7 = MathHelper.d((double) f, d4, d5);
            d8 = MathHelper.d((double) f, d2, d3);
            d9 = random.nextDouble() * (double) k1 / 16.0D;
            double d10 = ((double) (MathHelper.sin(3.1415927F * f) + 1.0F) * d9 + 1.0D) / 2.0D;

            adouble[l1 * 4 + 0] = d6;
            adouble[l1 * 4 + 1] = d7;
            adouble[l1 * 4 + 2] = d8;
            adouble[l1 * 4 + 3] = d10;
        }

        int i2;

        for (l1 = 0; l1 < k1 - 1; ++l1) {
            if (adouble[l1 * 4 + 3] > 0.0D) {
                for (i2 = l1 + 1; i2 < k1; ++i2) {
                    if (adouble[i2 * 4 + 3] > 0.0D) {
                        d6 = adouble[l1 * 4 + 0] - adouble[i2 * 4 + 0];
                        d7 = adouble[l1 * 4 + 1] - adouble[i2 * 4 + 1];
                        d8 = adouble[l1 * 4 + 2] - adouble[i2 * 4 + 2];
                        d9 = adouble[l1 * 4 + 3] - adouble[i2 * 4 + 3];
                        if (d9 * d9 > d6 * d6 + d7 * d7 + d8 * d8) {
                            if (d9 > 0.0D) {
                                adouble[i2 * 4 + 3] = -1.0D;
                            } else {
                                adouble[l1 * 4 + 3] = -1.0D;
                            }
                        }
                    }
                }
            }
        }

        BulkSectionAccess bulksectionaccess = new BulkSectionAccess(generatoraccessseed);

        try {
            for (i2 = 0; i2 < k1; ++i2) {
                d6 = adouble[i2 * 4 + 3];
                if (d6 >= 0.0D) {
                    d7 = adouble[i2 * 4 + 0];
                    d8 = adouble[i2 * 4 + 1];
                    d9 = adouble[i2 * 4 + 2];
                    int j2 = Math.max(MathHelper.floor(d7 - d6), i);
                    int k2 = Math.max(MathHelper.floor(d8 - d6), j);
                    int l2 = Math.max(MathHelper.floor(d9 - d6), k);
                    int i3 = Math.max(MathHelper.floor(d7 + d6), j2);
                    int j3 = Math.max(MathHelper.floor(d8 + d6), k2);
                    int k3 = Math.max(MathHelper.floor(d9 + d6), l2);

                    for (int l3 = j2; l3 <= i3; ++l3) {
                        double d11 = ((double) l3 + 0.5D - d7) / d6;

                        if (d11 * d11 < 1.0D) {
                            for (int i4 = k2; i4 <= j3; ++i4) {
                                double d12 = ((double) i4 + 0.5D - d8) / d6;

                                if (d11 * d11 + d12 * d12 < 1.0D) {
                                    for (int j4 = l2; j4 <= k3; ++j4) {
                                        double d13 = ((double) j4 + 0.5D - d9) / d6;

                                        if (d11 * d11 + d12 * d12 + d13 * d13 < 1.0D && !generatoraccessseed.d(i4)) {
                                            int k4 = l3 - i + (i4 - j) * l + (j4 - k) * l * i1;

                                            if (!bitset.get(k4)) {
                                                bitset.set(k4);
                                                blockposition_mutableblockposition.d(l3, i4, j4);
                                                if (generatoraccessseed.e_(blockposition_mutableblockposition)) {
                                                    ChunkSection chunksection = bulksectionaccess.a(blockposition_mutableblockposition);

                                                    if (chunksection != Chunk.EMPTY_SECTION) {
                                                        int l4 = SectionPosition.b(l3);
                                                        int i5 = SectionPosition.b(i4);
                                                        int j5 = SectionPosition.b(j4);
                                                        IBlockData iblockdata = chunksection.getType(l4, i5, j5);
                                                        Iterator iterator = worldgenfeatureoreconfiguration.targetStates.iterator();

                                                        while (iterator.hasNext()) {
                                                            WorldGenFeatureOreConfiguration.b worldgenfeatureoreconfiguration_b = (WorldGenFeatureOreConfiguration.b) iterator.next();

                                                            Objects.requireNonNull(bulksectionaccess);
                                                            if (a(iblockdata, bulksectionaccess::b, random, worldgenfeatureoreconfiguration, worldgenfeatureoreconfiguration_b, blockposition_mutableblockposition)) {
                                                                chunksection.setType(l4, i5, j5, worldgenfeatureoreconfiguration_b.state, false);
                                                                ++j1;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            try {
                bulksectionaccess.close();
            } catch (Throwable throwable1) {
                throwable.addSuppressed(throwable1);
            }

            throw throwable;
        }

        bulksectionaccess.close();
        return j1 > 0;
    }

    public static boolean a(IBlockData iblockdata, Function<BlockPosition, IBlockData> function, Random random, WorldGenFeatureOreConfiguration worldgenfeatureoreconfiguration, WorldGenFeatureOreConfiguration.b worldgenfeatureoreconfiguration_b, BlockPosition.MutableBlockPosition blockposition_mutableblockposition) {
        return !worldgenfeatureoreconfiguration_b.target.a(iblockdata, random) ? false : (a(random, worldgenfeatureoreconfiguration.discardChanceOnAirExposure) ? true : !a(function, (BlockPosition) blockposition_mutableblockposition));
    }

    protected static boolean a(Random random, float f) {
        return f <= 0.0F ? true : (f >= 1.0F ? false : random.nextFloat() >= f);
    }
}
