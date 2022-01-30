package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BiasedToBottomHeight extends HeightProvider {

    public static final Codec<BiasedToBottomHeight> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter((biasedtobottomheight) -> {
            return biasedtobottomheight.minInclusive;
        }), VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter((biasedtobottomheight) -> {
            return biasedtobottomheight.maxInclusive;
        }), Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter((biasedtobottomheight) -> {
            return biasedtobottomheight.inner;
        })).apply(instance, BiasedToBottomHeight::new);
    });
    private static final Logger LOGGER = LogManager.getLogger();
    private final VerticalAnchor minInclusive;
    private final VerticalAnchor maxInclusive;
    private final int inner;

    private BiasedToBottomHeight(VerticalAnchor verticalanchor, VerticalAnchor verticalanchor1, int i) {
        this.minInclusive = verticalanchor;
        this.maxInclusive = verticalanchor1;
        this.inner = i;
    }

    public static BiasedToBottomHeight a(VerticalAnchor verticalanchor, VerticalAnchor verticalanchor1, int i) {
        return new BiasedToBottomHeight(verticalanchor, verticalanchor1, i);
    }

    @Override
    public int a(Random random, WorldGenerationContext worldgenerationcontext) {
        int i = this.minInclusive.a(worldgenerationcontext);
        int j = this.maxInclusive.a(worldgenerationcontext);

        if (j - i - this.inner + 1 <= 0) {
            BiasedToBottomHeight.LOGGER.warn("Empty height range: {}", this);
            return i;
        } else {
            int k = random.nextInt(j - i - this.inner + 1);

            return random.nextInt(k + this.inner) + i;
        }
    }

    @Override
    public HeightProviderType<?> a() {
        return HeightProviderType.BIASED_TO_BOTTOM;
    }

    public String toString() {
        return "biased[" + this.minInclusive + "-" + this.maxInclusive + " inner: " + this.inner + "]";
    }
}
