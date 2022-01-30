package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.feature.stateproviders.WorldGenFeatureStateProvider;

public class RootSystemConfiguration implements WorldGenFeatureConfiguration {

    public static final Codec<RootSystemConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureConfigured.CODEC.fieldOf("feature").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.treeFeature;
        }), Codec.intRange(1, 64).fieldOf("required_vertical_space_for_tree").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.requiredVerticalSpaceForTree;
        }), Codec.intRange(1, 64).fieldOf("root_radius").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.rootRadius;
        }), MinecraftKey.CODEC.fieldOf("root_replaceable").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.rootReplaceable;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("root_state_provider").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.rootStateProvider;
        }), Codec.intRange(1, 256).fieldOf("root_placement_attempts").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.rootPlacementAttempts;
        }), Codec.intRange(1, 4096).fieldOf("root_column_max_height").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.rootColumnMaxHeight;
        }), Codec.intRange(1, 64).fieldOf("hanging_root_radius").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.hangingRootRadius;
        }), Codec.intRange(0, 16).fieldOf("hanging_roots_vertical_span").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.hangingRootsVerticalSpan;
        }), WorldGenFeatureStateProvider.CODEC.fieldOf("hanging_root_state_provider").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.hangingRootStateProvider;
        }), Codec.intRange(1, 256).fieldOf("hanging_root_placement_attempts").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.hangingRootPlacementAttempts;
        }), Codec.intRange(1, 64).fieldOf("allowed_vertical_water_for_tree").forGetter((rootsystemconfiguration) -> {
            return rootsystemconfiguration.allowedVerticalWaterForTree;
        })).apply(instance, RootSystemConfiguration::new);
    });
    public final Supplier<WorldGenFeatureConfigured<?, ?>> treeFeature;
    public final int requiredVerticalSpaceForTree;
    public final int rootRadius;
    public final MinecraftKey rootReplaceable;
    public final WorldGenFeatureStateProvider rootStateProvider;
    public final int rootPlacementAttempts;
    public final int rootColumnMaxHeight;
    public final int hangingRootRadius;
    public final int hangingRootsVerticalSpan;
    public final WorldGenFeatureStateProvider hangingRootStateProvider;
    public final int hangingRootPlacementAttempts;
    public final int allowedVerticalWaterForTree;

    public RootSystemConfiguration(Supplier<WorldGenFeatureConfigured<?, ?>> supplier, int i, int j, MinecraftKey minecraftkey, WorldGenFeatureStateProvider worldgenfeaturestateprovider, int k, int l, int i1, int j1, WorldGenFeatureStateProvider worldgenfeaturestateprovider1, int k1, int l1) {
        this.treeFeature = supplier;
        this.requiredVerticalSpaceForTree = i;
        this.rootRadius = j;
        this.rootReplaceable = minecraftkey;
        this.rootStateProvider = worldgenfeaturestateprovider;
        this.rootPlacementAttempts = k;
        this.rootColumnMaxHeight = l;
        this.hangingRootRadius = i1;
        this.hangingRootsVerticalSpan = j1;
        this.hangingRootStateProvider = worldgenfeaturestateprovider1;
        this.hangingRootPlacementAttempts = k1;
        this.allowedVerticalWaterForTree = l1;
    }
}
