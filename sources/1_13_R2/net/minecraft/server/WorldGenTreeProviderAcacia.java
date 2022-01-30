package net.minecraft.server;

import java.util.Random;
import javax.annotation.Nullable;

public class WorldGenTreeProviderAcacia extends WorldGenTreeProvider {

    public WorldGenTreeProviderAcacia() {}

    @Nullable
    protected WorldGenTreeAbstract<WorldGenFeatureEmptyConfiguration> b(Random random) {
        return new WorldGenAcaciaTree(true);
    }
}
