package ioann.uwu.runeruin.dimension.noise;

import java.util.UUID;

public interface Noise {

    /// @return noise in range `[0..1]`
    default float noise(float x, float z) {
        return this.noise(x, 1.0f, z);
    }

    /// @return noise in range `[0..1]`
    float noise(float x, float y, float z);

    static Noise multi(Noise... noises) {
        return (x, y, z) -> {
            float noiseAccum = 0;
            for (Noise noise : noises) {
                noiseAccum += noise.noise(x, y, z) / noises.length;
            }
            return noiseAccum;
        };
    }

    static Noise constant(float noise) {
        return (x, y, z) -> noise;
    }

    static Noise flatten(float smoothness, Noise noise) {
        return (x, y, z) -> flatten(noise.noise(x, y, z), smoothness);
    }

    /// Flattens noise
    /// @param smoothness how smooth final noise should be. `2.0f` - super smooth. `0.1f` - super flat.
    /// @return flattened noise in range `[0..1]`
    static float flatten(float noise, float smoothness) {
        float negRange = noise * 2 - 1;
        float flattened = (float) Math.pow(negRange, smoothness);

        if (Float.isNaN(flattened)) {
            return 0;
        } else {
            return flattened;
        }
    }

    static long hashString(String string) {
        return UUID.nameUUIDFromBytes(string.getBytes()).getMostSignificantBits();
    }
}
