package ioann.uwu.runeruin.dimension.noise;

import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import net.minecraft.util.Mth;

public class TopLevelBouldersNoise implements Noise {

    private final TopLevelNoise topLevelNoise;

    private final Noise noise = new SingleNoise("topLevelBorders".hashCode(), 5f, 0.3f);

    public TopLevelBouldersNoise(TopLevelNoise topLevelNoise) {
        this.topLevelNoise = topLevelNoise;
    }

    public static final int BOULDERS_HEIGHT = 13;
    public static final int BOULDERS_HEIGHT_BELOW_PLATE = 3;

    @Override
    public float noise(float x, float y, float z) {
        float base = topLevelNoise.superBaseNoise.noise(x, z);

        float needSubtract = Mth.abs(Mth.cos(base * Math.PI));
        float nearEdgeFactor = (Noise.flatten(base * 5f, 0.133f) - needSubtract * 8f);

        float noise = this.noise.noise(x, y, z);
        if (nearEdgeFactor * noise < 0.4f) {
            return 0f;
        }


        float yMultiplier = (y - RRChunkGenerator.TOP_LAYER_Y) / (BOULDERS_HEIGHT + BOULDERS_HEIGHT_BELOW_PLATE + RRChunkGenerator.ARCANE_PLATE_HEIGHT); // 0..1
        float negRangeYMultiplier = yMultiplier * 2 - 1;

        float howFarFromTopOrBottomFactor = Noise.flatten((float) Math.abs(Math.cos(negRangeYMultiplier)), 0.332f);

        return nearEdgeFactor * noise * howFarFromTopOrBottomFactor;
    }
}
