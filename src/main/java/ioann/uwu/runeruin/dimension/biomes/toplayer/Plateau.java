package ioann.uwu.runeruin.dimension.biomes.toplayer;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Flat-topped heights with steep sides and acacias on them, after the vanilla savanna plateau. */
public class Plateau {

    public static Biome bootstrap(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobs);
        mobs.addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.HORSE, 2, 6))
                .addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityTypes.DONKEY, 1, 1))
                .addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(EntityTypes.LLAMA, 4, 4));
        BiomeDefaultFeatures.commonSpawnWithZombieHorse(mobs);

        BiomeGenerationSettings.Builder generation = TopLayerBiomes.generation(placedFeatures, carvers);
        BiomeDefaultFeatures.addSavannaGrass(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addSavannaTrees(generation);
        BiomeDefaultFeatures.addWarmFlowers(generation);
        BiomeDefaultFeatures.addSavannaExtraGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation, true);

        return TopLayerBiomes.biome(1.5f, 0f)
                .hasPrecipitation(false)
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .build();
    }
}
