package ioann.uwu.runeruin.dimension.noise;

import java.util.List;

public class TopLevelNoise implements Noise{

    private final Noise bigNoise = new Noise() {

        private final Noise noise = new MultiNoise(List.of(
                new SingleNoise("bigNoise2".hashCode(), 0.5f),
                new SingleNoise("bigNoise3".hashCode(), 0.4f),
                new SingleNoise("bigNoise5".hashCode(), 0.3f)
        ));

        @Override
        public float noise(float x, float z) {

            float negRange = noise.noise(x, z) * 2 - 1;

            float ampMultiplier = (float) Math.pow(negRange, 0.222);

            return ampMultiplier; // / 2 + 1;
        }
    };

    @Override
    public float noise(float x, float z) {

        // TODO: Чтобы сделать генерацию территории красивой и чтоб она не затрагивала ямы
        // лучше умножать bigNoise на дополнительный шум, а не складывать.

        return bigNoise.noise(x, z);
    }
}
