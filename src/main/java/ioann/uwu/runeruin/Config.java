package ioann.uwu.runeruin;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue RENDER_CLOUDS_BELOW_TOP_LAYER = BUILDER
            .comment("Whether to render clouds on top layer")
            .define("render_clouds_below_top_layer", true);

    public static final ModConfigSpec.BooleanValue GLOWING_MOSS_DYNAMIC_LIGHT = BUILDER
            .comment("When false, glowing moss keeps a fixed light level and no longer reacts to nearby players.")
            .define("glowing_moss_dynamic_light", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
