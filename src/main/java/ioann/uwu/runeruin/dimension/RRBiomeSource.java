package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ioann.uwu.runeruin.RR;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.stream.Stream;

public class RRBiomeSource extends BiomeSource {

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> REGISTRY =
            DeferredRegister.create(Registries.BIOME_SOURCE, RR.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<RRBiomeSource>> BIOME_SOURCE =
            REGISTRY.register("runeruin_biome_source", () -> RRBiomeSource.CODEC);

    private final HolderSet<Biome> topLevelBiomes;
    private final HolderSet<Biome> topLevelUndergroundBiomes;
    private final HolderSet<Biome> secondLevelBiomes;
    private final HolderSet<Biome> secondLevelUndergroundBiomes;
    private final HolderSet<Biome> thirdLevelBiomes;
    private final HolderSet<Biome> thirdLevelUndergroundBiomes;
    private final HolderSet<Biome> fourthLevelBiomes;
    private final HolderSet<Biome> fourthLevelUndergroundBiomes;

    public RRBiomeSource(
            HolderSet<Biome> topLevelBiomes,
            HolderSet<Biome> topLevelUndergroundBiomes,
            HolderSet<Biome> secondLevelBiomes,
            HolderSet<Biome> secondLevelUndergroundBiomes,
            HolderSet<Biome> thirdLevelBiomes,
            HolderSet<Biome> thirdLevelUndergroundBiomes,
            HolderSet<Biome> fourthLevelBiomes,
            HolderSet<Biome> fourthLevelUndergroundBiomes
    ) {
        this.topLevelBiomes = topLevelBiomes;
        this.topLevelUndergroundBiomes = topLevelUndergroundBiomes;
        this.secondLevelBiomes = secondLevelBiomes;
        this.secondLevelUndergroundBiomes = secondLevelUndergroundBiomes;
        this.thirdLevelBiomes = thirdLevelBiomes;
        this.thirdLevelUndergroundBiomes = thirdLevelUndergroundBiomes;
        this.fourthLevelBiomes = fourthLevelBiomes;
        this.fourthLevelUndergroundBiomes = fourthLevelUndergroundBiomes;
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
                    biomeSource.topLevelUndergroundBiomes,
                    biomeSource.secondLevelBiomes,
                    biomeSource.secondLevelUndergroundBiomes,
                    biomeSource.thirdLevelBiomes,
                    biomeSource.thirdLevelUndergroundBiomes,
                    biomeSource.fourthLevelBiomes,
                    biomeSource.fourthLevelUndergroundBiomes
            )
    ).fieldOf("layer_biomes");

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(
                topLevelBiomes,
                topLevelUndergroundBiomes,
                secondLevelBiomes,
                secondLevelUndergroundBiomes,
                thirdLevelBiomes,
                thirdLevelUndergroundBiomes,
                fourthLevelBiomes,
                fourthLevelUndergroundBiomes
        ).flatMap(HolderSet::stream);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        var rnd = RandomSource.create();
        if (y < 5) {
            return this.fourthLevelBiomes.getRandomElement(rnd).get();
        } else if (y < 23) {
            return this.thirdLevelBiomes.getRandomElement(rnd).get();
        } else if (y < 42) {
            return this.secondLevelBiomes.getRandomElement(rnd).get();
        } else {
            return this.topLevelBiomes.getRandomElement(rnd).get();
        }
    }
}
