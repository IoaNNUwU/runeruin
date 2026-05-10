package ioann.uwu.runeruin.dimension.noise;

public class TopLevelNoise implements Noise {

    private final Noise moreFlattenedBaseTopLevelNoise;
    private final Noise flattenedBaseTopLevelNoise;

    private final Noise miniNoise = Noise.flatten(0.22f, Noise.multi(
            new SingleNoise("miniNoise4".hashCode(), 0.9f),
            new SingleNoise("miniNoise5".hashCode(), 1.4f),
            new SingleNoise("miniNoise11".hashCode(), 1.4f),
            new SingleNoise("miniNoise6".hashCode(), 0.3f)
    ));

    public TopLevelNoise(Noise baseTopLevelNoise, Noise flattenedBaseTopLevelNoise) {

        Noise lowerBaseTopLevelNoise = (x, y, z) -> Math.max(baseTopLevelNoise.noise(x, y, z) - 0.04f, 0.0f);
        this.moreFlattenedBaseTopLevelNoise = Noise.flatten(0.1f, lowerBaseTopLevelNoise);

        this.flattenedBaseTopLevelNoise = flattenedBaseTopLevelNoise;
    }

    @Override
    public float noise(float x, float y, float z) {

        float base = flattenedBaseTopLevelNoise.noise(x, y, z);

        float baseWithEdge = moreFlattenedBaseTopLevelNoise.noise(x, y, z);
        float miniNoise = baseWithEdge * this.miniNoise.noise(x, y, z);

        return (base * 0.45f
                + miniNoise * 0.5f
        );
    }
}