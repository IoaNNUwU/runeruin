package ioann.uwu.runeruin.dimension.noise;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LazyNoise {

    // TODO: This causes memory leak because noises never removed from the map on world exit
    private static final ConcurrentHashMap<String, Noise> REGISTRY = new ConcurrentHashMap<>();

    private final String noiseName;
    private final Function<Long, Noise> seedToNoise;

    public LazyNoise(String noiseName, Function<Long, Noise> seedToNoise) {
        this.noiseName = noiseName;
        this.seedToNoise = seedToNoise;
    }

    public Noise getOrCreateNoise(RandomState randomState) {
        return getOrCreateNoise(randomState.sampler());
    }

    public Noise getOrCreateNoise(Climate.Sampler sampler) {

        long worldSeed = extractWorldSeed(sampler);
        String mapKey = this.noiseName + worldSeed;

        return REGISTRY.computeIfAbsent(mapKey, _ -> this.seedToNoise.apply(worldSeed));
    }

    private static long extractWorldSeed(Climate.Sampler sampler) {
        return sampler.sample(98, 3, 67).erosion();
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
}
