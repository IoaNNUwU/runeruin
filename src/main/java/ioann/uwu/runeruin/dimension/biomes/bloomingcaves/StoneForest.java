package ioann.uwu.runeruin.dimension.biomes.bloomingcaves;

import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.RRPlacedFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class StoneForest {

    public static Biome bootstrap(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> carvers) {

        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();

        mobs.addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 1));
        mobs.addSpawn(MobCategory.MONSTER, 30, new MobSpawnSettings.SpawnerData(EntityType.BOGGED, 4, 4));
        mobs.addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityType.FROG, 2, 5));

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

        generation.addFeature(GenerationStep.Decoration.RAW_GENERATION, RRPlacedFeatures.MOSS_LAKE);

        generation.addFeature(GenerationStep.Decoration.RAW_GENERATION, RRPlacedFeatures.TUFF_MOSS_BOULDER);
        generation.addFeature(GenerationStep.Decoration.RAW_GENERATION, RRPlacedFeatures.MONOLITH);

        // generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.LUSH_CAVES_CEILING_VEGETATION);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.CAVE_VINES);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.LUSH_CAVES_VEGETATION);
        // generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.ROOTED_AZALEA_TREE);
        // generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.SPORE_BLOSSOM);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.CLASSIC_VINES);

        BiomeDefaultFeatures.addLushCavesSpecialOres(generation);
        BiomeDefaultFeatures.addExtraEmeralds(generation);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.MOSS_BERRY_BUSH_PATCH);

        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.COMMON_STONE_LILY);

        // TODO: move this to to ceiling biome
        // generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.LONG_CEILING_BLOCK_VINE);
        // generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, RRPlacedFeatures.CEILING_VINE);

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

    public static boolean checkSlimeSpawnRulesInRuneRuinDimension(EntityType<Slime> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {

        if (level.getDifficulty() != Difficulty.PEACEFUL) {

            if (EntitySpawnReason.isSpawner(spawnReason)) {
                return Mob.checkMobSpawnRules(type, level, spawnReason, pos, random);
            }

            if (level.getBiome(pos).is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS)
                    && pos.getY() > RRChunkGenerator.BLOOMING_CAVES_Y
                    && pos.getY() < RRChunkGenerator.BLOOMING_CAVES_CEILING_Y) {

                if (level.getMaxLocalRawBrightness(pos) <= random.nextInt(8)) {
                    System.out.println("  CHECK_SPAWN_RULES: " + Mob.checkMobSpawnRules(type, level, spawnReason, pos, random));
                    return Mob.checkMobSpawnRules(type, level, spawnReason, pos, random);
                }
            }
        }

        return false;
    }
}
