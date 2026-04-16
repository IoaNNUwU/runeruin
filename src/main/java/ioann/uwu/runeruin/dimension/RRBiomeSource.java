package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.stream.Stream;

import static ioann.uwu.runeruin.dimension.RRChunkGenerator.*;

public class RRBiomeSource extends BiomeSource {

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> REGISTRY =
            DeferredRegister.create(Registries.BIOME_SOURCE, RR.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<RRBiomeSource>> BIOME_SOURCE =
            REGISTRY.register("runeruin_biome_source", () -> RRBiomeSource.CODEC);

    private final HolderSet<Biome> topLevelBiomes;
    private final HolderSet<Biome> bloomingCavesCeilingBiomes;
    private final HolderSet<Biome> bloomingCavesBiomes;
    private final HolderSet<Biome> deepCavesCeilingBiomes;
    private final HolderSet<Biome> deepCavesBiomes;
    private final HolderSet<Biome> lostCavesCeilingBiomes;
    private final HolderSet<Biome> lostCavesBiomes;
    private final HolderSet<Biome> voidBiomes;

    public RRBiomeSource(
            HolderSet<Biome> topLevelBiomes,
            HolderSet<Biome> bloomingCavesCeilingBiomes,
            HolderSet<Biome> bloomingCavesBiomes,
            HolderSet<Biome> deepCavesCeilingBiomes,
            HolderSet<Biome> deepCavesBiomes,
            HolderSet<Biome> lostCavesCeilingBiomes,
            HolderSet<Biome> lostCavesBiomes,
            HolderSet<Biome> voidBiomes
    ) {
        this.topLevelBiomes = topLevelBiomes;
        this.bloomingCavesBiomes = bloomingCavesBiomes;
        this.bloomingCavesCeilingBiomes = bloomingCavesCeilingBiomes;
        this.deepCavesBiomes = deepCavesBiomes;
        this.deepCavesCeilingBiomes = deepCavesCeilingBiomes;
        this.lostCavesBiomes = lostCavesBiomes;
        this.lostCavesCeilingBiomes = lostCavesCeilingBiomes;
        this.voidBiomes = voidBiomes;
    }

    public static final MapCodec<RRBiomeSource> CODEC = Codec.list(Biome.LIST_CODEC).xmap(
            list -> new RRBiomeSource(
                    list.get(0),
                    list.get(1),
                    list.get(2),
                    list.get(3),
                    list.get(4),
                    list.get(5),
                    list.get(6),
                    list.get(7)
            ),
            biomeSource -> List.of(
                    biomeSource.topLevelBiomes,
                    biomeSource.bloomingCavesCeilingBiomes,
                    biomeSource.bloomingCavesBiomes,
                    biomeSource.deepCavesCeilingBiomes,
                    biomeSource.deepCavesBiomes,
                    biomeSource.lostCavesCeilingBiomes,
                    biomeSource.lostCavesBiomes,
                    biomeSource.voidBiomes
            )
    ).fieldOf("top_to_bottom_biomes");

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(
                topLevelBiomes,
                bloomingCavesCeilingBiomes,
                bloomingCavesBiomes,
                deepCavesCeilingBiomes,
                deepCavesBiomes,
                lostCavesCeilingBiomes,
                lostCavesBiomes,
                voidBiomes
        ).flatMap(HolderSet::stream);
    }

    private static final RandomSource randomSource = RandomSource.createThreadLocalInstance(1022101L);

    private static final int CEILING_BIOME_HEIGHT = RRChunkGenerator.CEILING_TERRAIN_HEIGHT + 15;
    // private static final int ARCANE_PLATE_BIOME_HEIGHT = RRChunkGenerator.ARCANE_PLATE_HEIGHT / 2;

    private static final Noise topLevelBiomesNoise = new SingleNoise("topLevelBiomeNoise".hashCode(), 0.2f);
    private static final Noise bloomingCavesCeilingBiomesNoise = new SingleNoise("bloomingCavesCeilingBiomeNoise".hashCode(), 0.4f);
    private static final Noise bloomingCavesBiomesNoise = new SingleNoise("bloomingCavesBiomeNoise".hashCode(), 0.2f);

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {

        x = QuartPos.toBlock(x);
        y = QuartPos.toBlock(y);
        z = QuartPos.toBlock(z);

        float baselineNoise = RRChunkGenerator.topLevelBaselineNoise.noise(x, y, z);
        int baseLine = BLOOMING_CAVES_CEILING_Y +
                (int) (TOP_LAYER_MAX_BASELINE_HEIGHT * baselineNoise) +
                TOP_LAYER_OFFSET - 10;

        if (y > baseLine) {

            float noise = topLevelBiomesNoise.noise(x, z);
            int idx = (int) (topLevelBiomes.size() * noise * 0.99999f);
            return this.topLevelBiomes.get(idx);

        } else if (y > BLOOMING_CAVES_CEILING_Y - CEILING_BIOME_HEIGHT) {

            float noise = bloomingCavesCeilingBiomesNoise.noise(x, z);
            int idx = (int) (bloomingCavesCeilingBiomes.size() * noise * 0.99999f);
            return this.bloomingCavesCeilingBiomes.get(idx);

        } else if (y > BLOOMING_CAVES_Y) {

            float noise = bloomingCavesBiomesNoise.noise(x, z);
            int idx = (int) (bloomingCavesBiomes.size() * noise * 0.99999f);
            return this.bloomingCavesBiomes.get(idx);

        } else if (y > DEEP_CAVES_CEILING_Y - CEILING_BIOME_HEIGHT) {
            return this.deepCavesCeilingBiomes.getRandomElement(randomSource).get();
        } else if (y > DEEP_CAVES_Y) {
            return this.deepCavesBiomes.getRandomElement(randomSource).get();
        } else if (y > LOST_CAVES_CEILING_Y - CEILING_BIOME_HEIGHT) {
            return this.lostCavesCeilingBiomes.getRandomElement(randomSource).get();
        } else if (y > LOST_CAVES_Y) {
            return this.lostCavesBiomes.getRandomElement(randomSource).get();
        } else {
            return this.voidBiomes.getRandomElement(randomSource).get();
        }
    }


}
