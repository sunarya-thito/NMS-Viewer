package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;

public class WorldGenFeatureDoublePlantConfiguration implements WorldGenFeatureConfiguration {

    public final IBlockData a;

    public WorldGenFeatureDoublePlantConfiguration(IBlockData iblockdata) {
        this.a = iblockdata;
    }

    @Override
    public <T> Dynamic<T> a(DynamicOps<T> dynamicops) {
        return new Dynamic(dynamicops, dynamicops.createMap(ImmutableMap.of(dynamicops.createString("state"), IBlockData.a(dynamicops, this.a).getValue())));
    }

    public static <T> WorldGenFeatureDoublePlantConfiguration a(Dynamic<T> dynamic) {
        IBlockData iblockdata = (IBlockData) dynamic.get("state").map(IBlockData::a).orElse(Blocks.AIR.getBlockData());

        return new WorldGenFeatureDoublePlantConfiguration(iblockdata);
    }
}
