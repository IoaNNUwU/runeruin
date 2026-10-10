package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.stream.Stream;

import static ioann.uwu.runeruin.dimension.Const.*;

public class RRBiomeSource extends BiomeSource {

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> REGISTRY =
            DeferredRegister.create(Registries.BIOME_SOURCE, RR.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<RRBiomeSource>> BIOME_SOURCE =
            REGISTRY.register("runeruin_biome_source", () -> RRBiomeSource.CODEC);

    // Biome choice noise of each layer, top to bottom like the biome lists. The name seeds the noise:
    // renaming one reshapes that layer's biome map.
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

    private static final int CEILING_BIOME_HEIGHT = CEILING_TERRAIN_HEIGHT + 15;

    /**
     * Biome lists top to bottom: top layer, blooming caves ceiling, blooming caves, deep caves ceiling,
     * deep caves, lost caves ceiling, lost caves, void.
     */
    private final List<HolderSet<Biome>> layers;

    public RRBiomeSource(List<HolderSet<Biome>> layers) {
        if (layers.size() != LAYER_NOISES.size()) {
            throw new IllegalArgumentException("Expected " + LAYER_NOISES.size() + " biome lists, got " + layers.size());
        }
        this.layers = List.copyOf(layers);
    }

    public static final MapCodec<RRBiomeSource> CODEC = Codec.list(Biome.LIST_CODEC)
            .xmap(RRBiomeSource::new, biomeSource -> biomeSource.layers)
            .fieldOf("top_to_bottom_biomes");

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return layers.stream().flatMap(HolderSet::stream);
    }

    public static RRBiomeSource newDefault(HolderGetter<Biome> biomeRegistry) {
        return new RRBiomeSource(List.of(
                HolderSet.direct(
                        biomeRegistry.getOrThrow(RRBiomes.ELDEN_GARDEN),
                        biomeRegistry.getOrThrow(RRBiomes.SIMILAR_FOREST)
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
                        biomeRegistry.getOrThrow(RRBiomes.DEEP_ROOTS),
                        biomeRegistry.getOrThrow(RRBiomes.DEEP_INVERTED_FOREST)
                ),
                HolderSet.direct(
                        // Glowing moss keeps one sixth, the three spike biomes split the rest evenly:
                        // 3/18 glowing moss, 5/18 stone, 5/18 deepslate, 5/18 dripstone spikes.
                        // The moss stays first so its regions did not move when dripstone was appended.
                        biomeRegistry.getOrThrow(RRBiomes.GLOWING_MOSS_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.GLOWING_MOSS_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.GLOWING_MOSS_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.STONE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.STONE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.STONE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.STONE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.STONE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEPSLATE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEPSLATE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEPSLATE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEPSLATE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEPSLATE_SPIKE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEP_DRIPSTONE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEP_DRIPSTONE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEP_DRIPSTONE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEP_DRIPSTONE_CAVES),
                        biomeRegistry.getOrThrow(RRBiomes.DEEP_DRIPSTONE_CAVES)
                ),
                HolderSet.direct(
                        biomeRegistry.getOrThrow(RRBiomes.SPARKLING_CAVES_CEILING)
                ),
                HolderSet.direct(
                        biomeRegistry.getOrThrow(RRBiomes.SPARKLING_CAVES)
                ),
                HolderSet.direct(
                        biomeRegistry.getOrThrow(RRBiomes.VOID)
                )
        ));
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

        float lostBaselineNoise = RRChunkGenerator.lostTopLevelBaselineNoise.getOrCreateNoise(sampler).noise(x, z);
        int lostBaseLine = (int) (LOST_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT * lostBaselineNoise + TOP_LAYER_OFFSET);

        // Each layer starts above this Y, top to bottom like `layers`.
        int[] layerBottoms = {
                baseLine,
                BLOOMING_CAVES_CEILING_Y - CEILING_BIOME_HEIGHT,
                BLOOMING_CAVES_Y,
                DEEP_CAVES_CEILING_Y - CEILING_BIOME_HEIGHT,
                lostBaseLine,
                LOST_CAVES_CEILING_Y - CEILING_BIOME_HEIGHT,
                LOST_CAVES_Y,
                Integer.MIN_VALUE
        };
        int layer = 0;
        while (y <= layerBottoms[layer]) {
            layer++;
        }

        HolderSet<Biome> biomes = layers.get(layer);
        float noise = LAYER_NOISES.get(layer).getOrCreateNoise(sampler).noise(x, z);
        return biomes.get((int) (biomes.size() * noise * 0.99999f));
    }
}
