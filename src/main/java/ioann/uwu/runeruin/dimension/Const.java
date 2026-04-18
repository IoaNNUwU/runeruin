package ioann.uwu.runeruin.dimension;

public class Const {

    public static final int VOID_HEIGHT = 50;
    public static final int UNDERGROUND_LEVEL_HEIGHT = 75;
    public static final int ARCANE_PLATE_HEIGHT = 5;

    public static final int TOP_LAYER_TERRAIN_HEIGHT = 50;
    public static final int TOP_LAYER_MAX_BASELINE_HEIGHT = 25;
    public static final int TOP_LAYER_OFFSET = -15;

    public static final int TERRAIN_HEIGHT = 25;
    public static final int TERRAIN_MIN_HEIGHT = 10;
    public static final int CEILING_TERRAIN_HEIGHT = 15;
    public static final int CEILING_TERRAIN_MIN_HEIGHT = 5;

    public static final int CEILING_VOID_Y = VOID_HEIGHT;
    public static final int LOST_CAVES_Y = CEILING_VOID_Y + ARCANE_PLATE_HEIGHT + 1;
    public static final int LOST_CAVES_CEILING_Y = LOST_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    public static final int DEEP_CAVES_Y = LOST_CAVES_CEILING_Y + ARCANE_PLATE_HEIGHT + 1;
    public static final int DEEP_CAVES_CEILING_Y = DEEP_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    public static final int BLOOMING_CAVES_Y = DEEP_CAVES_CEILING_Y + ARCANE_PLATE_HEIGHT + 1;
    public static final int BLOOMING_CAVES_CEILING_Y = BLOOMING_CAVES_Y + UNDERGROUND_LEVEL_HEIGHT;
    public static final int TOP_LAYER_Y = BLOOMING_CAVES_CEILING_Y + 1;
}
