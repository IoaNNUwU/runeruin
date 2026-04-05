package ioann.uwu.runeruin.dimension.noise;

import ioann.uwu.runeruin.dimension.noise.impl.FastNoise;

public class SingleNoise implements Noise {

    private final float frequency;

    private final FastNoise fastNoise;

    public SingleNoise(long seed) {
        this(seed, 1.0f);
    }

    public SingleNoise(long seed, float frequency) {
        this.frequency = frequency;
        this.fastNoise = new FastNoise((int) seed);
    }

    @Override
    public float noise(float x, float z) {
        float fastNoise = this.fastNoise.GetNoise(
                x * frequency,
                z * frequency
        );
        return (fastNoise + 1) / 2;
    }
}
