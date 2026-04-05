package ioann.uwu.runeruin.dimension.noise;

public class TopLevelNoise implements Noise{

    private final Noise bigNoise = new Noise() {

        private final Noise noise = new SingleNoise("bigNoise".hashCode(), 0.5f);

        @Override
        public float noise(float x, float z) {

            float negRange = noise.noise(x, z) * 2 - 1;

            float ampMultiplier = (float) Math.pow(negRange, 0.222);

            return ampMultiplier; // / 2 + 1;
        }
    };

    @Override
    public float noise(float x, float z) {
        return bigNoise.noise(x, z);
    }
}
