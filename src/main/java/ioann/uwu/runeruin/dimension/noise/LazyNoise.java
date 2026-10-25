package ioann.uwu.runeruin.dimension.noise;

import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;

public class LazyNoise {

    // A non-noise generator gets a RandomState with a dummy noise router, and the biome source only
    // sees its climate sampler, so neither carries the world seed. RRChunkGenerator.createState binds
    // the seed to the sampler of each level's RandomState. Weak identity keys: the noises of a level
    // go away with it.
    private static final Map<Climate.Sampler, Level> LEVELS = new MapMaker().weakKeys().makeMap();
    // Each LazyNoise has its own slot in the noises of every level.
    private static final int MAX_NOISES = 64;
    private static final AtomicInteger NEXT_SLOT = new AtomicInteger();

    private final Function<Level, Noise> levelToNoise;
    private final int slot;

    /** {@code seedToNoise} takes a name of its own into the seed, as {@link #single} does. */
    public LazyNoise(Function<Long, Noise> seedToNoise) {
        this(level -> seedToNoise.apply(level.seed()), NEXT_SLOT.getAndIncrement());
    }

    private LazyNoise(Function<Level, Noise> levelToNoise, int slot) {
        if (slot >= MAX_NOISES) {
            throw new IllegalStateException("More than " + MAX_NOISES + " LazyNoise objects; raise LazyNoise.MAX_NOISES");
        }
        this.levelToNoise = levelToNoise;
        this.slot = slot;
    }

    /** A noise seeded by its name and the world seed, so two noises with different names never repeat each other. */
    public static LazyNoise single(String noiseName, float frequency) {
        return new LazyNoise(seed -> new SingleNoise(Noise.hashString(noiseName + seed), frequency));
    }

    /** A noise built on other noises of the level, which it shares with their other readers. */
    public static LazyNoise ofLevel(Function<Level, Noise> levelToNoise) {
        return new LazyNoise(levelToNoise, NEXT_SLOT.getAndIncrement());
    }

    public static LazyNoise chain(LazyNoise base, Function<Noise, Noise> transform) {
        return ofLevel(level -> transform.apply(base.forLevel(level)));
    }

    public static void bindSeed(RandomState randomState, long seed) {
        LEVELS.put(randomState.sampler(), new Level(seed));
    }

    public Noise getOrCreateNoise(RandomState randomState) {
        return getOrCreateNoise(randomState.sampler());
    }

    public Noise getOrCreateNoise(Climate.Sampler sampler) {
        Level level = LEVELS.get(sampler);
        if (level == null) {
            throw new IllegalStateException("No world seed is bound to this RandomState; call LazyNoise.bindSeed first");
        }
        return forLevel(level);
    }

    /** The noise of this level. Noises built on it share the instance, and with it what it caches. */
    public Noise forLevel(Level level) {
        Noise noise = level.noises.get(this.slot);
        if (noise != null) {
            return noise;
        }
        noise = this.levelToNoise.apply(level);
        Noise raced = level.noises.compareAndExchange(this.slot, null, noise);
        return raced == null ? noise : raced;
    }

    /** The world seed of a level and the noises built for it. Nothing in it may hold the level's sampler, or the level would never be released. */
    public static final class Level {

        private final long seed;
        private final AtomicReferenceArray<Noise> noises = new AtomicReferenceArray<>(MAX_NOISES);

        private Level(long seed) {
            this.seed = seed;
        }

        public long seed() {
            return this.seed;
        }
    }
}
