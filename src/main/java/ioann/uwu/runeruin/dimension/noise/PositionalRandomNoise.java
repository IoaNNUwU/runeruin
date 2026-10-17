package ioann.uwu.runeruin.dimension.noise;

import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

/** A random value per block, seeded by its name and the world seed, so two such noises never repeat each other. */
public class PositionalRandomNoise implements Noise {

    private final PositionalRandomFactory positionalRandom;

    public PositionalRandomNoise(String name, long seed) {
        this.positionalRandom = new XoroshiroRandomSource.XoroshiroPositionalRandomFactory(
                Noise.hashString(name + "1" + seed),
                Noise.hashString(name + "2" + seed)
        );
    }

    @Override
    public float noise(float x, float y, float z) {
        return positionalRandom.at((int) x, (int) y, (int) z).nextFloat();
    }
}
