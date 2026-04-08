package ioann.uwu.runeruin.dimension.noise;

import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class PositionalRandomNoise implements Noise {

    private final PositionalRandomFactory positionalRandom;

    public PositionalRandomNoise(long seed) {
        this.positionalRandom = new XoroshiroRandomSource.XoroshiroPositionalRandomFactory(
                ("randomNoise1" + seed).hashCode(),
                ("randomNoise2" + seed).hashCode()
        );
    }

    @Override
    public float noise(float x, float y, float z) {
        return positionalRandom.at((int) x, (int) y, (int) z).nextFloat();
    }
}
