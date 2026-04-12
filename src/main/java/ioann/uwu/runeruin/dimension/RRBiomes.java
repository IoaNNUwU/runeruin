package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RRBiomes {

    // --- Top Layer ---
    public static final ResourceKey<Biome> ELDEN_GARDEN = ResourceKey.create(Registries.BIOME, RR.id("elden_garden"));

    // --- Blooming caves Layer ---
    public static final ResourceKey<Biome> BLOOMING_SWAMP = ResourceKey.create(Registries.BIOME, RR.id("blooming_swamp"));

    public static void bootstrap(BootstrapContext<Biome> ctx) {
        ctx.register(ELDEN_GARDEN, eldenGarden(ctx.lookup(Registries.PLACED_FEATURE), ctx.lookup(Registries.CONFIGURED_CARVER)));
        ctx.register(BLOOMING_SWAMP, bloomingSwamp(ctx.lookup(Registries.PLACED_FEATURE), ctx.lookup(Registries.CONFIGURED_CARVER)));
    }

    public static Biome eldenGarden(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.CREATURE, 1, new MobSpawnSettings.SpawnerData(EntityType.PIG, 1, 2))
                .addSpawn(MobCategory.CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 4))
                .addSpawn(MobCategory.CREATURE, 2, new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 2, 4));

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.SMALL_RED_WALL_MUSHROOM);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.SMALL_BROWN_WALL_MUSHROOM);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.BIG_RED_WALL_MUSHROOM);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.BIG_BROWN_WALL_MUSHROOM);

        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);

        BiomeDefaultFeatures.addPlainGrass(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);

        BiomeDefaultFeatures.addCherryGroveVegetation(generation);
        BiomeDefaultFeatures.addExtraEmeralds(generation);

        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
                .waterColor(0xF7F284)
                .grassColorOverride(0xFFAA70);

        Biome.BiomeBuilder biomeBuilder = new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(1f)
                .downfall(0.5f)
                .setAttribute(EnvironmentAttributes.SKY_COLOR, 0xFFAA70)
                .setAttribute(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, 0xFF0000)
                .setAttribute(EnvironmentAttributes.CLOUD_COLOR, 0xFF0000)
                .setAttribute(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, 0x0A0A0A)
                // TODO: Make MUSHROOM cave biome ambient color PINK or something
                .setAttribute(EnvironmentAttributes.CLOUD_HEIGHT, 280f)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, 0xFF8860)
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .specialEffects(effects.build());

        return biomeBuilder.build();
    }

    public static Biome bloomingSwamp(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        generation.addFeature(GenerationStep.Decoration.RAW_GENERATION, RRPlacedFeatures.TUFF_MOSS_BOULDER);

        BiomeDefaultFeatures.addLushCavesVegetationFeatures(generation);
        BiomeDefaultFeatures.addLushCavesSpecialOres(generation);

        BiomeDefaultFeatures.addJungleTrees(generation);
        BiomeDefaultFeatures.addExtraEmeralds(generation);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.LONG_CEILING_BLOCK_VINE);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.CEILING_VINE);

        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x20AA80);
                //.grassColorOverride(0xFFAA70);

        Biome.BiomeBuilder biomeBuilder = new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(1f)
                .downfall(0.5f)
                //.setAttribute(EnvironmentAttributes.SKY_COLOR, 0xFFAA70)
                //.setAttribute(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, 0xFF0000)
                //.setAttribute(EnvironmentAttributes.CLOUD_COLOR, 0xFF0000)
                .setAttribute(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, 0x0A0A0A)
                //.setAttribute(EnvironmentAttributes.CLOUD_HEIGHT, 280f)
                .setAttribute(EnvironmentAttributes.FOG_COLOR, 0x88AA60)
                .mobSpawnSettings(mobs.build())
                .generationSettings(generation.build())
                .specialEffects(effects.build());

        return biomeBuilder.build();
    }
}
