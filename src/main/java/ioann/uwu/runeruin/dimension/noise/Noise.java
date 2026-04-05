package ioann.uwu.runeruin.dimension.noise;

public interface Noise {

    /// @return noise in range `[0..1]`
    float noise(float x, float z);
}
