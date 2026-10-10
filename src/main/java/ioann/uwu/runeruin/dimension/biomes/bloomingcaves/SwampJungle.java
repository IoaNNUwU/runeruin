package ioann.uwu.runeruin.dimension.biomes.bloomingcaves;

import ioann.uwu.runeruin.dimension.CaveMushroomKind;
import ioann.uwu.runeruin.dimension.RRPlacedFeatures;
import ioann.uwu.runeruin.entities.RREntityTypes;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SwampJungle {

    public static Biome bootstrap(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(RREntityTypes.SNAIL.get(), 1, 3));

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        generation.addFeature(GenerationStep.Decoration.RAW_GENERATION, RRPlacedFeatures.MINI_VOLCANO);
        generation.addFeature(GenerationStep.Decoration.RAW_GENERATION, RRPlacedFeatures.MONOLITH);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.MOSS_HUMMOCK);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.MOSS_VEGETATION);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.POWDERED_MOSS);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.CLASSIC_VINES);

        BiomeDefaultFeatures.addLushCavesSpecialOres(generation);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.SWAMP_JUNGLE_TREES);
        BiomeDefaultFeatures.addExtraEmeralds(generation);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.RARE_STONE_LILY);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CaveMushroomKind.RED_ARCH.placedKey());

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
