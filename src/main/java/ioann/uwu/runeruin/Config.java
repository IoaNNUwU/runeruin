package ioann.uwu.runeruin;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder COMMON = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue GLOWING_MOSS_DYNAMIC_LIGHT = COMMON
            .comment("When false, glowing moss keeps a fixed light level and no longer reacts to nearby players.")
            .define("glowing_moss_dynamic_light", true);

    public static final ModConfigSpec.BooleanValue RENDER_CLOUDS_BELOW_TOP_LAYER = CLIENT
            .comment("Whether to render a layer of clouds below the top layer, in the Blooming caves")
            .define("render_clouds_below_top_layer", true);

    static final ModConfigSpec COMMON_SPEC = COMMON.build();
    public static final ModConfigSpec CLIENT_SPEC = CLIENT.build();
}
