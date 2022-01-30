package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class WorldGenCanyon extends WorldGenCarverAbstract<CanyonCarverConfiguration> {

    public WorldGenCanyon(Codec<CanyonCarverConfiguration> codec) {
        super(codec);
    }

    public boolean isStartChunk(CanyonCarverConfiguration canyoncarverconfiguration, Random random) {
        return random.nextFloat() <= canyoncarverconfiguration.probability;
    }

    public boolean carve(CarvingContext carvingcontext, CanyonCarverConfiguration canyoncarverconfiguration, IChunkAccess ichunkaccess, Function<BlockPosition, BiomeBase> function, Random random, Aquifer aquifer, ChunkCoordIntPair chunkcoordintpair, CarvingMask carvingmask) {
        int i = (this.getRange() * 2 - 1) * 16;
        double d0 = (double) chunkcoordintpair.getBlockX(random.nextInt(16));
        int j = canyoncarverconfiguration.y.sample(random, carvingcontext);
        double d1 = (double) chunkcoordintpair.getBlockZ(random.nextInt(16));
        float f = random.nextFloat() * 6.2831855F;
        float f1 = canyoncarverconfiguration.verticalRotation.sample(random);
        double d2 = (double) canyoncarverconfiguration.yScale.sample(random);
        float f2 = canyoncarverconfiguration.shape.thickness.sample(random);
        int k = (int) ((float) i * canyoncarverconfiguration.shape.distanceFactor.sample(random));
        boolean flag = false;

        this.doCarve(carvingcontext, canyoncarverconfiguration, ichunkaccess, function, random.nextLong(), aquifer, d0, (double) j, d1, f2, f, f1, 0, k, d2, carvingmask);
        return true;
    }

    private void doCarve(CarvingContext carvingcontext, CanyonCarverConfiguration canyoncarverconfiguration, IChunkAccess ichunkaccess, Function<BlockPosition, BiomeBase> function, long i, Aquifer aquifer, double d0, double d1, double d2, float f, float f1, float f2, int j, int k, double d3, CarvingMask carvingmask) {
        Random random = new Random(i);
        float[] afloat = this.initWidthFactors(carvingcontext, canyoncarverconfiguration, random);
        float f3 = 0.0F;
        float f4 = 0.0F;

        for (int l = j; l < k; ++l) {
            double d4 = 1.5D + (double) (MathHelper.sin((float) l * 3.1415927F / (float) k) * f);
            double d5 = d4 * d3;

            d4 *= (double) canyoncarverconfiguration.shape.horizontalRadiusFactor.sample(random);
            d5 = this.updateVerticalRadius(canyoncarverconfiguration, random, d5, (float) k, (float) l);
            float f5 = MathHelper.cos(f2);
            float f6 = MathHelper.sin(f2);

            d0 += (double) (MathHelper.cos(f1) * f5);
            d1 += (double) f6;
            d2 += (double) (MathHelper.sin(f1) * f5);
            f2 *= 0.7F;
            f2 += f4 * 0.05F;
            f1 += f3 * 0.05F;
            f4 *= 0.8F;
            f3 *= 0.5F;
            f4 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            f3 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (random.nextInt(4) != 0) {
                if (!canReach(ichunkaccess.getPos(), d0, d2, l, k, f)) {
                    return;
                }

                this.carveEllipsoid(carvingcontext, canyoncarverconfiguration, ichunkaccess, function, aquifer, d0, d1, d2, d4, d5, carvingmask, (carvingcontext1, d6, d7, d8, i1) -> {
                    return this.shouldSkip(carvingcontext1, afloat, d6, d7, d8, i1);
                });
            }
        }

    }

    private float[] initWidthFactors(CarvingContext carvingcontext, CanyonCarverConfiguration canyoncarverconfiguration, Random random) {
        int i = carvingcontext.getGenDepth();
        float[] afloat = new float[i];
        float f = 1.0F;

        for (int j = 0; j < i; ++j) {
            if (j == 0 || random.nextInt(canyoncarverconfiguration.shape.widthSmoothness) == 0) {
                f = 1.0F + random.nextFloat() * random.nextFloat();
            }

            afloat[j] = f * f;
        }

        return afloat;
    }

    private double updateVerticalRadius(CanyonCarverConfiguration canyoncarverconfiguration, Random random, double d0, float f, float f1) {
        float f2 = 1.0F - MathHelper.abs(0.5F - f1 / f) * 2.0F;
        float f3 = canyoncarverconfiguration.shape.verticalRadiusDefaultFactor + canyoncarverconfiguration.shape.verticalRadiusCenterFactor * f2;

        return (double) f3 * d0 * (double) MathHelper.randomBetween(random, 0.75F, 1.0F);
    }

    private boolean shouldSkip(CarvingContext carvingcontext, float[] afloat, double d0, double d1, double d2, int i) {
        int j = i - carvingcontext.getMinGenY();

        return (d0 * d0 + d2 * d2) * (double) afloat[j - 1] + d1 * d1 / 6.0D >= 1.0D;
    }
}
