package net.minecraft.data.worldgen.biome;

import net.minecraft.data.worldgen.BiomeSettings;
import net.minecraft.data.worldgen.placement.EndPlacements;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeFog;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.CaveSoundSettings;
import net.minecraft.world.level.levelgen.WorldGenStage;

public class EndBiomes {

    public EndBiomes() {}

    private static BiomeBase baseEndBiome(BiomeSettingsGeneration.a biomesettingsgeneration_a) {
        BiomeSettingsMobs.a biomesettingsmobs_a = new BiomeSettingsMobs.a();

        BiomeSettings.endSpawns(biomesettingsmobs_a);
        return (new BiomeBase.a()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.THEEND).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.a()).waterColor(4159204).waterFogColor(329011).fogColor(10518688).skyColor(0).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(biomesettingsmobs_a.build()).generationSettings(biomesettingsgeneration_a.build()).build();
    }

    public static BiomeBase endBarrens() {
        BiomeSettingsGeneration.a biomesettingsgeneration_a = new BiomeSettingsGeneration.a();

        return baseEndBiome(biomesettingsgeneration_a);
    }

    public static BiomeBase theEnd() {
        BiomeSettingsGeneration.a biomesettingsgeneration_a = (new BiomeSettingsGeneration.a()).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, EndPlacements.END_SPIKE);

        return baseEndBiome(biomesettingsgeneration_a);
    }

    public static BiomeBase endMidlands() {
        BiomeSettingsGeneration.a biomesettingsgeneration_a = new BiomeSettingsGeneration.a();

        return baseEndBiome(biomesettingsgeneration_a);
    }

    public static BiomeBase endHighlands() {
        BiomeSettingsGeneration.a biomesettingsgeneration_a = (new BiomeSettingsGeneration.a()).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, EndPlacements.END_GATEWAY_RETURN).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, EndPlacements.CHORUS_PLANT);

        return baseEndBiome(biomesettingsgeneration_a);
    }

    public static BiomeBase smallEndIslands() {
        BiomeSettingsGeneration.a biomesettingsgeneration_a = (new BiomeSettingsGeneration.a()).addFeature(WorldGenStage.Decoration.RAW_GENERATION, EndPlacements.END_ISLAND_DECORATED);

        return baseEndBiome(biomesettingsgeneration_a);
    }
}
