package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.IRegistry;

public abstract class FeatureSize {

    public static final Codec<FeatureSize> CODEC = IRegistry.FEATURE_SIZE_TYPES.byNameCodec().dispatch(FeatureSize::type, FeatureSizeType::codec);
    protected static final int MAX_WIDTH = 16;
    protected final OptionalInt minClippedHeight;

    protected static <S extends FeatureSize> RecordCodecBuilder<S, OptionalInt> minClippedHeightCodec() {
        return Codec.intRange(0, 80).optionalFieldOf("min_clipped_height").xmap((optional) -> {
            return (OptionalInt) optional.map(OptionalInt::of).orElse(OptionalInt.empty());
        }, (optionalint) -> {
            return optionalint.isPresent() ? Optional.of(optionalint.getAsInt()) : Optional.empty();
        }).forGetter((featuresize) -> {
            return featuresize.minClippedHeight;
        });
    }

    public FeatureSize(OptionalInt optionalint) {
        this.minClippedHeight = optionalint;
    }

    protected abstract FeatureSizeType<?> type();

    public abstract int getSizeAtHeight(int i, int j);

    public OptionalInt minClippedHeight() {
        return this.minClippedHeight;
    }
}
