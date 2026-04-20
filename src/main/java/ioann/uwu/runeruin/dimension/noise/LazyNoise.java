package ioann.uwu.runeruin.dimension.noise;

import ioann.uwu.runeruin.RR;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
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

        long worldSeed = extractWorldSeed(randomState);
        String mapKey = this.noiseName + worldSeed;

        return REGISTRY.computeIfAbsent(mapKey, _ -> this.seedToNoise.apply(worldSeed));
    }

    private static final Identifier RANDOM_FACTORY_ID = RR.id("worldgen_random_factory");

    private static long extractWorldSeed(RandomState randomState) {
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        long seed = factory.fromHashOf("worldgen_random_factory").nextLong();

        // This is not the real seed, just random seed-dependant value
        return seed;
    }
}
