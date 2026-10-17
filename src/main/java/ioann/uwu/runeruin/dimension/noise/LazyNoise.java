package ioann.uwu.runeruin.dimension.noise;

import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;

public class LazyNoise {

    // A non-noise generator gets a RandomState with a dummy noise router, and the biome source only
    // sees its climate sampler, so neither carries the world seed. RRChunkGenerator.createState binds
    // the seed to the sampler of each level's RandomState. Weak identity keys: one entry per level.
    private static final Map<Climate.Sampler, Long> SEEDS = new MapMaker().weakKeys().makeMap();

    private final Function<Long, Noise> seedToNoise;
    private final Map<Long, Noise> noisesBySeed = new ConcurrentHashMap<>();

    /** {@code seedToNoise} takes a name of its own into the seed, as {@link #single} does. */
    public LazyNoise(Function<Long, Noise> seedToNoise) {
        this.seedToNoise = seedToNoise;
    }

    /** A noise seeded by its name and the world seed, so two noises with different names never repeat each other. */
    public static LazyNoise single(String noiseName, float frequency) {
        return new LazyNoise(seed -> new SingleNoise(Noise.hashString(noiseName + seed), frequency));
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
        return forSeed(seed);
    }

    /** The noise of this world seed. Noises built on it share the instance, and with it what it caches. */
    public Noise forSeed(long seed) {
        return noisesBySeed.computeIfAbsent(seed, this.seedToNoise);
    }

    public static LazyNoise chain(LazyNoise base, Function<Noise, Noise> transform) {
        return new LazyNoise(seed -> transform.apply(base.forSeed(seed)));
    }
}
