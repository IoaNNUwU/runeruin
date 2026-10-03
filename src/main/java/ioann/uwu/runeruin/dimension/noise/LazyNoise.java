package ioann.uwu.runeruin.dimension.noise;

import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;

public class LazyNoise {

    // A non-noise generator gets a RandomState with a dummy noise router, and the biome source only
    // sees its climate sampler, so neither carries the world seed. RRChunkGenerator.createState binds
    // the seed to the sampler of each level's RandomState. Weak identity keys: one entry per level.
    private static final Map<Climate.Sampler, Long> SEEDS = new MapMaker().weakKeys().makeMap();

    private final String noiseName;
    private final Function<Long, Noise> seedToNoise;
    private final Map<Long, Noise> noisesBySeed = new ConcurrentHashMap<>();

    public LazyNoise(String noiseName, Function<Long, Noise> seedToNoise) {
        this.noiseName = noiseName;
        this.seedToNoise = seedToNoise;
    }

    /** A noise seeded by its name and the world seed, so two noises with different names never repeat each other. */
    public static LazyNoise single(String noiseName, float frequency) {
        return new LazyNoise(noiseName, seed -> new SingleNoise(Noise.hashString(noiseName + seed), frequency));
    }

    public static void bindSeed(RandomState randomState, long seed) {
        SEEDS.put(randomState.sampler(), seed);
    }

    public Noise getOrCreateNoise(RandomState randomState) {
        return getOrCreateNoise(randomState.sampler());
    }

    public Noise getOrCreateNoise(Climate.Sampler sampler) {
        Long seed = SEEDS.get(sampler);
        if (seed == null) {
            throw new IllegalStateException("No world seed is bound to this RandomState; call LazyNoise.bindSeed first");
        }
        return noisesBySeed.computeIfAbsent(seed, this.seedToNoise);
    }

    public static LazyNoise chain(String noiseName, LazyNoise base, Function<Noise, Noise> transform) {
        return new LazyNoise(
                base.noiseName + ":" + noiseName,
                seed -> {
                    Noise baseNoise = base.seedToNoise.apply(seed);
                    return transform.apply(baseNoise);
                }
        );
    }

    public static LazyNoise chain(String noiseName, LazyNoise base1, LazyNoise base2, BiFunction<Noise, Noise, Noise> transform) {
        return new LazyNoise(
                base1.noiseName + "/" + base2.noiseName + ":" + noiseName,

                seed -> {
                    Noise baseNoise1 = base1.seedToNoise.apply(seed);
                    Noise baseNoise2 = base2.seedToNoise.apply(seed);

                    return transform.apply(baseNoise1, baseNoise2);
                }
        );
    }
}
