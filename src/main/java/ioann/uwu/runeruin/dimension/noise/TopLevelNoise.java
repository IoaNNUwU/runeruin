package ioann.uwu.runeruin.dimension.noise;

public class TopLevelNoise implements Noise {

    private final Noise baseNoise = Noise.flatten(0.152f, Noise.multi(
            new SingleNoise("bigNoise1".hashCode(), 0.5f),
            new SingleNoise("bigNoise2".hashCode(), 0.4f),
            new SingleNoise("bigNoise3".hashCode(), 0.3f)
    ));

    private final Noise miniNoise = Noise.flatten(0.22f, Noise.multi(
            new SingleNoise("miniNoise4".hashCode(), 0.9f),
            new SingleNoise("miniNoise5".hashCode(), 1.4f),
            new SingleNoise("miniNoise11".hashCode(), 1.4f),
            new SingleNoise("miniNoise6".hashCode(), 0.3f)
    ));

    @Override
    public float noise(float x, float y, float z) {

        float base = baseNoise.noise(x, y, z);

        float n1 = base * miniNoise.noise(x, y, z);

        return (
                base * 0.5f
                        + n1 * 0.5f
        );
    }
}