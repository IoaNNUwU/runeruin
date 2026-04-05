package ioann.uwu.runeruin.dimension.noise;

import java.util.List;

public class MultiNoise implements Noise{

    private final List<Noise> noises;

    public MultiNoise(List<Noise> noises) {
        this.noises = noises;
    }

    @Override
    public float noise(float x, float z) {
       float noiseAccum = 0;
       for (Noise noise : this.noises) {
           noiseAccum += noise.noise(x, z) / noises.size();
       }
       return noiseAccum;
    }
}
