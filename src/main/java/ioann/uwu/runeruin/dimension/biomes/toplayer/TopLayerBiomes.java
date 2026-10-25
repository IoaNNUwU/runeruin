package ioann.uwu.runeruin.dimension.biomes.toplayer;

import ioann.uwu.runeruin.dimension.RRConfiguredCarvers;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** What the biomes of the top layer that follow an overworld biome share, as the overworld biomes share their base. */
final class TopLayerBiomes {

    private TopLayerBiomes() {}

    static BiomeGenerationSettings.Builder generation(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        generation.addCarver(RRConfiguredCarvers.TOP_LAYER_CAVES);
        generation.addFeature(GenerationStep.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND);
        generation.addFeature(GenerationStep.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_SURFACE);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
        return generation;
    }

    static Biome.BiomeBuilder biome(float temperature, float downfall) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(temperature)
                .downfall(downfall)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, OverworldBiomes.calculateSkyColor(temperature))
                .setAttribute(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, 0x0A0A0A)
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x3F76E4).build());
    }
}
