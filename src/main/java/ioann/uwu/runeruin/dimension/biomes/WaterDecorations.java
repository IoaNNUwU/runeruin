package ioann.uwu.runeruin.dimension.biomes;

import ioann.uwu.runeruin.dimension.RRPlacedFeatures;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * Pool plants shared by every biome with water: goblet moss, seagrass, kelp, lily pads,
 * water lilies and deep roots. One list keeps their order identical in every biome,
 * which FeatureSorter requires.
 */
public final class WaterDecorations {

    private WaterDecorations() {}

    public static void add(BiomeGenerationSettings.Builder generation) {
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.GOBLET_MOSS_PATCH);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.GOBLET_MOSS_PATCH_UNDERWATER);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.GOBLET_SEAGRASS);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.GOBLET_KELP);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.SMALL_LILY_PAD_PATCH);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.BIG_LILY_PAD_PATCH);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.WATER_LILY);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.GOBLET_DEEP_ROOTS);
    }
}
