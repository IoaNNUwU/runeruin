package ioann.uwu.runeruin.dimension.noise;

/** The relief of the top layer islands and of the Deep caves floor: the base shape with fine detail on top. */
public class TopLevelNoise implements Noise {

    private final Noise moreFlattenedBaseTopLevelNoise;
    private final Noise flattenedBaseTopLevelNoise;
    private final Noise detailNoise;

    /** The detail is seeded by its name and the world seed, so every layer and every world gets its own. */
    public TopLevelNoise(String detailName, long seed, Noise baseTopLevelNoise, Noise flattenedBaseTopLevelNoise) {

        Noise lowerBaseTopLevelNoise = (x, y, z) -> Math.max(baseTopLevelNoise.noise(x, y, z) - 0.045f, 0.0f);
        this.moreFlattenedBaseTopLevelNoise = Noise.flatten(0.15f, lowerBaseTopLevelNoise);

        this.flattenedBaseTopLevelNoise = flattenedBaseTopLevelNoise;

        this.detailNoise = Noise.flatten(0.22f, Noise.multi(
                new SingleNoise(Noise.hashString(detailName + "1" + seed), 0.9f),
                new SingleNoise(Noise.hashString(detailName + "2" + seed), 1.4f),
                new SingleNoise(Noise.hashString(detailName + "3" + seed), 1.4f),
                new SingleNoise(Noise.hashString(detailName + "4" + seed), 0.3f)
        ));
    }

    @Override
    public float noise(float x, float y, float z) {

        float base = flattenedBaseTopLevelNoise.noise(x, y, z);
        float baseWithEdge = moreFlattenedBaseTopLevelNoise.noise(x, y, z);

        float detail = baseWithEdge * this.detailNoise.noise(x, y, z);

        return base * 0.45f + detail * 0.5f;
    }

    /** The fine detail alone, from 0 to 1, without the shape of the islands. */
    public float detail(float x, float z) {
        return this.detailNoise.noise(x, z);
    }
}
