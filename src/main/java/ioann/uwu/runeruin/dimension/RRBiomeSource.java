package ioann.uwu.runeruin.dimension;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.chunkgenerator.DeepCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.TopLayerAndBloomingCavesGen;
import ioann.uwu.runeruin.dimension.chunkgenerator.VoidGen;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.CEILING_TERRAIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.DEEP_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.LOST_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.LOST_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_MAX_BASELINE_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.TOP_LAYER_OFFSET;

public class RRBiomeSource extends BiomeSource {

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> REGISTRY =
            DeferredRegister.create(Registries.BIOME_SOURCE, RR.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<RRBiomeSource>> BIOME_SOURCE =
            REGISTRY.register("runeruin_biome_source", () -> RRBiomeSource.CODEC);

    // Temperature of each layer, top to bottom like the biome lists: the layers under the top one pick their biomes
    // by it alone. The name seeds the noise: renaming one reshapes that layer's biome map.
    private static final List<LazyNoise> LAYER_NOISES = List.of(
            LazyNoise.single("topLevelBiomeNoise", 0.2f),
            LazyNoise.single("bloomingCavesCeilingBiomeNoise", 0.4f),
            LazyNoise.single("bloomingCavesBiomeNoise", 0.2f),
            LazyNoise.single("deepCavesCeilingBiomesNoise", 0.4f),
            LazyNoise.single("deepCavesBiomesNoise", 0.2f),
            LazyNoise.single("lostCavesCeilingBiomesNoise", 0.4f),
            LazyNoise.single("lostCavesBiomesNoise", 0.2f),
            LazyNoise.single("voidCeilingBiomesNoise", 0.4f)
    );
    // The top layer picks its biomes like the overworld does, from climate parameters between -1 and 1: temperature,
    // humidity, the landform (erosion: steep land where it is low, flat land where it is high) and the distance to
    // the edge of the island (continentalness), which every biome takes whole for now.
    private static final LazyNoise TOP_HUMIDITY = LazyNoise.single("topLevelHumidityNoise", 0.2f);
    private static final LazyNoise TOP_LANDFORM = LazyNoise.single("topLevelLandformNoise", 0.25f);
    private static final Climate.Parameter ANY = Climate.Parameter.span(-1, 1);
    private static final Climate.Parameter UNUSED = Climate.Parameter.point(0);

    /** The ceiling biomes of the Blooming caves start above this Y, under the lowest terrace of their ceiling. */
    public static final int BLOOMING_CEILING_BIOME_BOTTOM = TopLayerAndBloomingCavesGen.CEILING_LOWEST_Y - 10;
    // The Lost caves ceiling hangs down to this far under LOST_CAVES_CEILING_Y. The ceiling biomes of the Lost and
    // the Deep caves start above this depth under their ceiling Y.
    private static final int CEILING_BIOME_HEIGHT = CEILING_TERRAIN_HEIGHT - TOP_LAYER_OFFSET;

    /**
     * Biome lists top to bottom: top layer, blooming caves ceiling, blooming caves, deep caves ceiling,
     * deep caves, lost caves ceiling, lost caves, void.
     */
    private final List<Climate.ParameterList<Holder<Biome>>> layers;

    public RRBiomeSource(List<Climate.ParameterList<Holder<Biome>>> layers) {
        if (layers.size() != LAYER_NOISES.size()) {
            throw new IllegalArgumentException("Expected " + LAYER_NOISES.size() + " biome lists, got " + layers.size());
        }
        this.layers = List.copyOf(layers);
    }

    public static final MapCodec<RRBiomeSource> CODEC = Climate.ParameterList.codec(Biome.CODEC.fieldOf("biome")).listOf()
            .xmap(RRBiomeSource::new, biomeSource -> biomeSource.layers)
            .fieldOf("top_to_bottom_biomes");

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return layers.stream().flatMap(layer -> layer.values().stream().map(Pair::getSecond));
    }

    public static RRBiomeSource newDefault(HolderGetter<Biome> biomeRegistry) {
        return new RRBiomeSource(List.of(
                new Climate.ParameterList<>(List.of(
                        // Steep land: round hills where it is cooler, flat-topped plateaus where it is warmer.
                        topLayerBiome(biomeRegistry, RRBiomes.HILLS, -1, 0.2f, ANY, -1, -0.2f),
                        topLayerBiome(biomeRegistry, RRBiomes.PLATEAU, 0.2f, 1, ANY, -1, -0.2f),
                        // Rolling land: the forests, split by temperature as before.
                        topLayerBiome(biomeRegistry, RRBiomes.ELDEN_GARDEN, -1, 0, ANY, -0.2f, 0.2f),
                        topLayerBiome(biomeRegistry, RRBiomes.SIMILAR_FOREST, 0, 1, ANY, -0.2f, 0.2f),
                        // Flat land: swamps where it is wet, deserts where it is hot and dry, plains elsewhere.
                        topLayerBiome(biomeRegistry, RRBiomes.PLAINS, -1, 0.2f, Climate.Parameter.span(-1, 0.2f), 0.2f, 1),
                        topLayerBiome(biomeRegistry, RRBiomes.SWAMP, -1, 1, Climate.Parameter.span(0.2f, 1), 0.2f, 1),
                        topLayerBiome(biomeRegistry, RRBiomes.DESERT, 0.2f, 1, Climate.Parameter.span(-1, 0.2f), 0.2f, 1)
                )),
                evenly(biomeRegistry, RRBiomes.GLOWING_ROOTS, RRBiomes.GLOWING_BALLS),
                // RRBiomes.GHOST_GROVE is parked.
                evenly(biomeRegistry, RRBiomes.JUNGLE_SWAMP, RRBiomes.STONE_FOREST),
                evenly(biomeRegistry, RRBiomes.DEEP_ROOTS, RRBiomes.INVERTED_FOREST),
                // Glowing moss keeps one sixth, the three spike biomes split the rest evenly:
                // 3/18 glowing moss, 5/18 stone, 5/18 deepslate, 5/18 dripstone spikes.
                // The moss stays first so its regions did not move when dripstone was appended.
                evenly(biomeRegistry,
                        RRBiomes.GLOWING_MOSS_CAVES,
                        RRBiomes.GLOWING_MOSS_CAVES,
                        RRBiomes.GLOWING_MOSS_CAVES,
                        RRBiomes.STONE_SPIKE_CAVES,
                        RRBiomes.STONE_SPIKE_CAVES,
                        RRBiomes.STONE_SPIKE_CAVES,
                        RRBiomes.STONE_SPIKE_CAVES,
                        RRBiomes.STONE_SPIKE_CAVES,
                        RRBiomes.DEEPSLATE_SPIKE_CAVES,
                        RRBiomes.DEEPSLATE_SPIKE_CAVES,
                        RRBiomes.DEEPSLATE_SPIKE_CAVES,
                        RRBiomes.DEEPSLATE_SPIKE_CAVES,
                        RRBiomes.DEEPSLATE_SPIKE_CAVES,
                        RRBiomes.DEEP_DRIPSTONE_CAVES,
                        RRBiomes.DEEP_DRIPSTONE_CAVES,
                        RRBiomes.DEEP_DRIPSTONE_CAVES,
                        RRBiomes.DEEP_DRIPSTONE_CAVES,
                        RRBiomes.DEEP_DRIPSTONE_CAVES
                ),
                evenly(biomeRegistry, RRBiomes.SPARKLING_CAVES_CEILING),
                evenly(biomeRegistry, RRBiomes.SPARKLING_CAVES),
                evenly(biomeRegistry, RRBiomes.VOID)
        ));
    }

    private static Pair<Climate.ParameterPoint, Holder<Biome>> topLayerBiome(
            HolderGetter<Biome> biomeRegistry, ResourceKey<Biome> biome,
            float minTemperature, float maxTemperature, Climate.Parameter humidity, float minLandform, float maxLandform
    ) {
        Climate.ParameterPoint point = Climate.parameters(
                Climate.Parameter.span(minTemperature, maxTemperature), humidity, ANY,
                Climate.Parameter.span(minLandform, maxLandform), UNUSED, UNUSED, 0
        );
        return Pair.of(point, biomeRegistry.getOrThrow(biome));
    }

    /** Splits the temperature evenly between the biomes in their order: a biome listed n times gets n shares. */
    @SafeVarargs
    private static Climate.ParameterList<Holder<Biome>> evenly(HolderGetter<Biome> biomeRegistry, ResourceKey<Biome>... biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = new ArrayList<>();
        for (int i = 0; i < biomes.length; i++) {
            Climate.Parameter temperature = Climate.Parameter.span(-1 + 2f * i / biomes.length, -1 + 2f * (i + 1) / biomes.length);
            values.add(Pair.of(Climate.parameters(temperature, UNUSED, UNUSED, UNUSED, UNUSED, UNUSED, 0), biomeRegistry.getOrThrow(biomes[i])));
        }
        return new Climate.ParameterList<>(values);
    }

    /** The biome of the top layer in this column. The land of the column follows its {@link TerrainProfile}. */
    public Holder<Biome> topLayerBiome(int x, int z, Climate.Sampler sampler) {
        return layers.getFirst().findValue(Climate.target(
                climate(LAYER_NOISES.getFirst(), x, z, sampler),
                climate(TOP_HUMIDITY, x, z, sampler),
                climate(RRChunkGenerator.flattenedBaseTopLevelNoise, x, z, sampler),
                climate(TOP_LANDFORM, x, z, sampler),
                0,
                0
        ));
    }

    private static float climate(LazyNoise noise, int x, int z, Climate.Sampler sampler) {
        return noise.getOrCreateNoise(sampler).noise(x, z) * 2 - 1;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {

        x = QuartPos.toBlock(x);
        y = QuartPos.toBlock(y);
        z = QuartPos.toBlock(z);

        // The upper layer starts above the top block of the ceiling below it, computed as the terrain does,
        // so the whole ceiling and the air under it are in the ceiling biome.
        float baselineNoise = RRChunkGenerator.topLevelBaselineNoise.getOrCreateNoise(sampler).noise(x, z);
        int baseLine = (int) (BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET);

        // Inside an island the ceiling of the Blooming caves rises a terrace into it and takes its biomes along.
        if (y > baseLine && y <= baseLine + TopLayerAndBloomingCavesGen.CEILING_BIOME_RISE
                && TopLayerAndBloomingCavesGen.ceilingHasTerraces(x, z, sampler)) {
            baseLine += TopLayerAndBloomingCavesGen.CEILING_BIOME_RISE;
        }

        int lostBaseLine = (int) (LOST_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise + TOP_LAYER_OFFSET);

        // The ceiling of the Deep caves rises into the floor of the Blooming caves and keeps its biome there.
        int deepCeilingTop = y <= DEEP_CAVES_CEILING_Y || y > DeepCavesGen.HIGHEST_Y
                ? BLOOMING_CAVES_Y
                : DeepCavesGen.roofY(x, z, sampler);

        // The ceiling of the Void rises into the floor of the Lost caves and keeps its biome there.
        int voidTop = y <= LOST_CAVES_Y || y > VoidGen.HIGHEST_Y
                ? LOST_CAVES_Y
                : VoidGen.roofY(x, z, sampler);

        // Each layer starts above this Y, top to bottom like `layers`.
        int[] layerBottoms = {
                baseLine,
                BLOOMING_CEILING_BIOME_BOTTOM,
                deepCeilingTop,
                DEEP_CAVES_CEILING_Y - CEILING_BIOME_HEIGHT,
                lostBaseLine,
                LOST_CAVES_CEILING_Y - CEILING_BIOME_HEIGHT,
                voidTop,
                Integer.MIN_VALUE
        };
        int layer = 0;
        while (y <= layerBottoms[layer]) {
            layer++;
        }

        if (layer == 0) {
            return topLayerBiome(x, z, sampler);
        }
        return layers.get(layer).findValue(Climate.target(climate(LAYER_NOISES.get(layer), x, z, sampler), 0, 0, 0, 0, 0));
    }
}
