package ioann.uwu.runeruin.dimension.noise;

public class ConstantNoise implements Noise{

    private final float noise;

    public ConstantNoise(float noise) {
        this.noise = noise;
    }

    @Override
    public float noise(float x, float z) {
        return this.noise;
    }
}
