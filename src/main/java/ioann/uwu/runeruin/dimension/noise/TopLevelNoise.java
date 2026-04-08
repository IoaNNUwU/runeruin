package ioann.uwu.runeruin.dimension.noise;

public class TopLevelNoise implements Noise {

    private final Noise flattenedBaseTopLevelNoise;

    private final Noise miniNoise = Noise.flatten(0.22f, Noise.multi(
            new SingleNoise("miniNoise4".hashCode(), 0.9f),
            new SingleNoise("miniNoise5".hashCode(), 1.4f),
            new SingleNoise("miniNoise11".hashCode(), 1.4f),
            new SingleNoise("miniNoise6".hashCode(), 0.3f)
    ));

    public TopLevelNoise(Noise flattenedBaseTopLevelNoise) {
        this.flattenedBaseTopLevelNoise = flattenedBaseTopLevelNoise;
    }

    @Override
    public float noise(float x, float y, float z) {

        float base = flattenedBaseTopLevelNoise.noise(x, y, z);

        float miniNoise = base * this.miniNoise.noise(x, y, z);

        return (base * 0.5f + miniNoise * 0.5f);
    }
}