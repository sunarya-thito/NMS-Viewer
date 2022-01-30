package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistry;

public interface FloatProviderType<P extends FloatProvider> {

    FloatProviderType<ConstantFloat> CONSTANT = register("constant", ConstantFloat.CODEC);
    FloatProviderType<UniformFloat> UNIFORM = register("uniform", UniformFloat.CODEC);
    FloatProviderType<ClampedNormalFloat> CLAMPED_NORMAL = register("clamped_normal", ClampedNormalFloat.CODEC);
    FloatProviderType<TrapezoidFloat> TRAPEZOID = register("trapezoid", TrapezoidFloat.CODEC);

    Codec<P> codec();

    static <P extends FloatProvider> FloatProviderType<P> register(String s, Codec<P> codec) {
        return (FloatProviderType) IRegistry.register(IRegistry.FLOAT_PROVIDER_TYPES, s, () -> {
            return codec;
        });
    }
}
