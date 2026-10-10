package ioann.uwu.runeruin.dimension.biomes.deepcaves;

import ioann.uwu.runeruin.dimension.CaveMushroomKind;
import ioann.uwu.runeruin.dimension.RRPlacedFeatures;
import ioann.uwu.runeruin.dimension.biomes.WaterDecorations;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class StoneSpikeCaves {

    public static Biome bootstrap(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.ASHEN_WALL_MUSHROOM_CLUSTER);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.ASHEN_WALL_MUSHROOM);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.CLASSIC_VINES);
        WaterDecorations.add(generation);

        generation.addFeature(GenerationStep.Decoration.RAW_GENERATION, RRPlacedFeatures.STONE_SPIKE);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CaveMushroomKind.YELLOW_HAT.placedKey());
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.DEEP_MOSS_SPROUTS);

        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x20AA80);

        Biome.BiomeBuilder biomeBuilder = new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(1f)
                .downfall(0.5f)
                .setAttribute(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, 0x0A0A0A)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, 0x88AA60)
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .specialEffects(effects.build());

        return biomeBuilder.build();
    }
}
