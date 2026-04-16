package ioann.uwu.runeruin.dimension;

import com.mojang.datafixers.util.Pair;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.client.Renderers;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.timeline.Timelines;
import net.neoforged.neoforge.common.world.NeoForgeEnvironmentAttributes;

import java.util.List;
import java.util.Optional;

public class RRDimension {

    // used in `/execute in runeruin:runeruin_dimension run tp ~ ~ ~`
    public static final ResourceKey<LevelStem> LEVEL_STEM = ResourceKey.create(Registries.LEVEL_STEM, RR.id("runeruin_dimension"));

    // Optional. Has to have the same name as LEVEL_STEM
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, RR.id("runeruin_dimension"));

    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, RR.id("runeruin_dimension_type"));

    public static void bootstrapType(BootstrapContext<DimensionType> ctx) {

        var clocks = ctx.lookup(Registries.WORLD_CLOCK);
        var timelines = ctx.lookup(Registries.TIMELINE);

        EnvironmentAttributeMap environmentAttributeMap = EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.CLOUD_COLOR, 0xDDDDDDDD)
                // .set(EnvironmentAttributes.CLOUD_HEIGHT, 320.33f)
                .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
                .set(EnvironmentAttributes.SKY_COLOR, OverworldBiomes.calculateSkyColor(0.8F))
                .set(NeoForgeEnvironmentAttributes.CUSTOM_CLOUDS, Renderers.RUNE_RUIN_CLOUDS_ID)
                .build();

        DimensionType dimensionType = new DimensionType(
                false,
                true,
                false,
                false,
                1.0,
                0,
                512,
                512,
                BlockTags.INFINIBURN_OVERWORLD,
                0.0f,
                new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
                DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT,
                environmentAttributeMap,
                HolderSet.direct(timelines.getOrThrow(Timelines.OVERWORLD_DAY)),
                Optional.of(clocks.getOrThrow(WorldClocks.OVERWORLD))
        );

        ctx.register(DIMENSION_TYPE, dimensionType);
    }

    public static void bootstrapStem(BootstrapContext<LevelStem> ctx) {
        var biomeRegistry = ctx.lookup(Registries.BIOME);
        var dimTypes = ctx.lookup(Registries.DIMENSION_TYPE);
        var noiseGenSettings = ctx.lookup(Registries.NOISE_SETTINGS);

        RRChunkGenerator chunkGenerator = new RRChunkGenerator(new RRBiomeSource(
                HolderSet.direct(
                        biomeRegistry.getOrThrow(RRBiomes.ELDEN_GARDEN),
                        biomeRegistry.getOrThrow(Biomes.FOREST)
                ),
                HolderSet.direct(
                        biomeRegistry.getOrThrow(RRBiomes.GLOWING_ROOTS),
                        biomeRegistry.getOrThrow(RRBiomes.GLOWING_BALLS)
                ),
                HolderSet.direct(
                        biomeRegistry.getOrThrow(RRBiomes.JUNGLE_SWAMP),
                        biomeRegistry.getOrThrow(RRBiomes.STONE_FOREST)
                        // biomeRegistry.getOrThrow(RRBiomes.GHOST_GROVE)
                ),
                HolderSet.direct(
                        // biomeRegistry.getOrThrow(Biomes.DEEP_DARK),
                        // biomeRegistry.getOrThrow(Biomes.COLD_OCEAN),
                        biomeRegistry.getOrThrow(Biomes.DRIPSTONE_CAVES)
                ),
                HolderSet.direct(
                        // biomeRegistry.getOrThrow(Biomes.DEEP_DARK),
                        // biomeRegistry.getOrThrow(Biomes.COLD_OCEAN),
                        biomeRegistry.getOrThrow(Biomes.DRIPSTONE_CAVES)
                ),
                HolderSet.direct(
                        // biomeRegistry.getOrThrow(Biomes.WARPED_FOREST),
                        // biomeRegistry.getOrThrow(Biomes.CRIMSON_FOREST),
                        biomeRegistry.getOrThrow(Biomes.BASALT_DELTAS)
                ),
                HolderSet.direct(
                        // biomeRegistry.getOrThrow(Biomes.WARPED_FOREST),
                        // biomeRegistry.getOrThrow(Biomes.CRIMSON_FOREST),
                        biomeRegistry.getOrThrow(Biomes.BASALT_DELTAS)
                ),
                HolderSet.direct(
                        // biomeRegistry.getOrThrow(Biomes.THE_END),
                        // biomeRegistry.getOrThrow(Biomes.SMALL_END_ISLANDS),
                        biomeRegistry.getOrThrow(Biomes.THE_VOID)
                )
        ));

        LevelStem stem = new LevelStem(dimTypes.getOrThrow(DIMENSION_TYPE), chunkGenerator);

        ctx.register(LEVEL_STEM, stem);
    }
}
