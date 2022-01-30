package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.serialization.Codec;
import net.minecraft.core.IRegistry;

public class TrunkPlacers<P extends TrunkPlacer> {

    public static final TrunkPlacers<TrunkPlacerStraight> STRAIGHT_TRUNK_PLACER = register("straight_trunk_placer", TrunkPlacerStraight.CODEC);
    public static final TrunkPlacers<TrunkPlacerForking> FORKING_TRUNK_PLACER = register("forking_trunk_placer", TrunkPlacerForking.CODEC);
    public static final TrunkPlacers<TrunkPlacerGiant> GIANT_TRUNK_PLACER = register("giant_trunk_placer", TrunkPlacerGiant.CODEC);
    public static final TrunkPlacers<TrunkPlacerMegaJungle> MEGA_JUNGLE_TRUNK_PLACER = register("mega_jungle_trunk_placer", TrunkPlacerMegaJungle.CODEC);
    public static final TrunkPlacers<TrunkPlacerDarkOak> DARK_OAK_TRUNK_PLACER = register("dark_oak_trunk_placer", TrunkPlacerDarkOak.CODEC);
    public static final TrunkPlacers<TrunkPlacerFancy> FANCY_TRUNK_PLACER = register("fancy_trunk_placer", TrunkPlacerFancy.CODEC);
    public static final TrunkPlacers<BendingTrunkPlacer> BENDING_TRUNK_PLACER = register("bending_trunk_placer", BendingTrunkPlacer.CODEC);
    private final Codec<P> codec;

    private static <P extends TrunkPlacer> TrunkPlacers<P> register(String s, Codec<P> codec) {
        return (TrunkPlacers) IRegistry.register(IRegistry.TRUNK_PLACER_TYPES, s, new TrunkPlacers<>(codec));
    }

    private TrunkPlacers(Codec<P> codec) {
        this.codec = codec;
    }

    public Codec<P> codec() {
        return this.codec;
    }
}
