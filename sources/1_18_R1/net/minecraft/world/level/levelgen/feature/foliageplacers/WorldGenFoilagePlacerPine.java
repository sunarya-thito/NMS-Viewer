package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.VirtualLevelReadable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;

public class WorldGenFoilagePlacerPine extends WorldGenFoilagePlacer {

    public static final Codec<WorldGenFoilagePlacerPine> CODEC = RecordCodecBuilder.create((instance) -> {
        return foliagePlacerParts(instance).and(IntProvider.codec(0, 24).fieldOf("height").forGetter((worldgenfoilageplacerpine) -> {
            return worldgenfoilageplacerpine.height;
        })).apply(instance, WorldGenFoilagePlacerPine::new);
    });
    private final IntProvider height;

    public WorldGenFoilagePlacerPine(IntProvider intprovider, IntProvider intprovider1, IntProvider intprovider2) {
        super(intprovider, intprovider1);
        this.height = intprovider2;
    }

    @Override
    protected WorldGenFoilagePlacers<?> type() {
        return WorldGenFoilagePlacers.PINE_FOLIAGE_PLACER;
    }

    @Override
    protected void createFoliage(VirtualLevelReadable virtuallevelreadable, BiConsumer<BlockPosition, IBlockData> biconsumer, Random random, WorldGenFeatureTreeConfiguration worldgenfeaturetreeconfiguration, int i, WorldGenFoilagePlacer.a worldgenfoilageplacer_a, int j, int k, int l) {
        int i1 = 0;

        for (int j1 = l; j1 >= l - j; --j1) {
            this.placeLeavesRow(virtuallevelreadable, biconsumer, random, worldgenfeaturetreeconfiguration, worldgenfoilageplacer_a.pos(), i1, j1, worldgenfoilageplacer_a.doubleTrunk());
            if (i1 >= 1 && j1 == l - j + 1) {
                --i1;
            } else if (i1 < k + worldgenfoilageplacer_a.radiusOffset()) {
                ++i1;
            }
        }

    }

    @Override
    public int foliageRadius(Random random, int i) {
        return super.foliageRadius(random, i) + random.nextInt(Math.max(i + 1, 1));
    }

    @Override
    public int foliageHeight(Random random, int i, WorldGenFeatureTreeConfiguration worldgenfeaturetreeconfiguration) {
        return this.height.sample(random);
    }

    @Override
    protected boolean shouldSkipLocation(Random random, int i, int j, int k, int l, boolean flag) {
        return i == l && k == l && l > 0;
    }
}
