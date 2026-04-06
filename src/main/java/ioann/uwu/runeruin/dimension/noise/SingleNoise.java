package ioann.uwu.runeruin.dimension.noise;

import ioann.uwu.runeruin.dimension.noise.impl.FastNoise;

public class SingleNoise implements Noise {

    private final float frequency;
    private final float yFrequency;

    private final FastNoise fastNoise;

    public SingleNoise(long seed) {
        this(seed, 1.0f);
    }

    public SingleNoise(long seed, float frequency) {
        this(seed, frequency, 1.0f);
    }

    public SingleNoise(long seed, float frequency, float yFrequency) {
        this.frequency = frequency;
        this.yFrequency = yFrequency;
        this.fastNoise = new FastNoise((int) seed);
    }

    @Override
    public float noise(float x, float y, float z) {
        float fastNoise = this.fastNoise.GetNoise(
                x * frequency,
                y * yFrequency,
                z * frequency
        );
        return (fastNoise + 1) / 2;
    }
}
