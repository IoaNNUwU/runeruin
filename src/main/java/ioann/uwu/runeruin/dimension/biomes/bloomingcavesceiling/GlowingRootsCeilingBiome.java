package ioann.uwu.runeruin.dimension.biomes.bloomingcavesceiling;

import ioann.uwu.runeruin.dimension.RRPlacedFeatures;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class GlowingRootsCeilingBiome {

    public static Biome bootstrap(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {

        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();

        // mobs.addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 1));
        // mobs.addSpawn(MobCategory.MONSTER, 30, new MobSpawnSettings.SpawnerData(EntityType.BOGGED, 4, 4));
        // mobs.addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityType.FROG, 2, 5));

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.ROOTED_AZALEA_TREE);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.SPORE_BLOSSOM);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.CLASSIC_VINES);

        BiomeDefaultFeatures.addExtraEmeralds(generation);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.LONG_CEILING_BLOCK_VINE);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.CEILING_VINE);

        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x20AA80);
        //      .grassColorOverride(0xFFAA70);

        Biome.BiomeBuilder biomeBuilder = new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(1f)
                .downfall(0.5f)
                //.setAttribute(EnvironmentAttributes.SKY_COLOR, 0xFFAA70)
                //.setAttribute(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, 0xFF0000)
                //.setAttribute(EnvironmentAttributes.CLOUD_COLOR, 0xFF0000)
                .setAttribute(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, 0x0A0A0A)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, 0x88AA60)
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .specialEffects(effects.build());

        return biomeBuilder.build();
    }
}
