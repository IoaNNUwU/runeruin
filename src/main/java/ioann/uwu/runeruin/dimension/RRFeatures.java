package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.features.AshenMushroomClusterFeature;
import ioann.uwu.runeruin.dimension.features.BoulderFeature;
import ioann.uwu.runeruin.dimension.features.CaveMushroomFeature;
import ioann.uwu.runeruin.dimension.features.CeilingBallFeature;
import ioann.uwu.runeruin.dimension.features.CeilingBlockVineFeature;
import ioann.uwu.runeruin.dimension.features.EldenGiantTreeFeature;
import ioann.uwu.runeruin.dimension.features.FloatingMossPatchFeature;
import ioann.uwu.runeruin.dimension.features.GlowingMushroomFeature;
import ioann.uwu.runeruin.dimension.features.GlowingMushroomPatchFeature;
import ioann.uwu.runeruin.dimension.features.GobletKelpFeature;
import ioann.uwu.runeruin.dimension.features.GobletMossPatchFeature;
import ioann.uwu.runeruin.dimension.features.GobletSeagrassFeature;
import ioann.uwu.runeruin.dimension.features.HangingChorusFeature;
import ioann.uwu.runeruin.dimension.features.InvertedTreeFeature;
import ioann.uwu.runeruin.dimension.features.LilyPadPatchFeature;
import ioann.uwu.runeruin.dimension.features.MiniVolcanoFeature;
import ioann.uwu.runeruin.dimension.features.MonolithFeature;
import ioann.uwu.runeruin.dimension.features.MossHummockFeature;
import ioann.uwu.runeruin.dimension.features.MossySpikeFeature;
import ioann.uwu.runeruin.dimension.features.PowderedMossVeinFeature;
import ioann.uwu.runeruin.dimension.features.StoneLilyFeature;
import ioann.uwu.runeruin.dimension.features.WallMushroomFeature;
import ioann.uwu.runeruin.dimension.features.WaterLilyFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRFeatures {

    public static final DeferredRegister<Feature<?>> REGISTRY = DeferredRegister.create(Registries.FEATURE, RR.MODID);

    public static final DeferredHolder<Feature<?>, WallMushroomFeature> WALL_MUSHROOM = REGISTRY.register("wall_mushroom", WallMushroomFeature::new);
    public static final DeferredHolder<Feature<?>, AshenMushroomClusterFeature> ASHEN_MUSHROOM_CLUSTER = REGISTRY.register("ashen_mushroom_cluster", AshenMushroomClusterFeature::new);
    public static final DeferredHolder<Feature<?>, GlowingMushroomFeature> GLOWING_MUSHROOM = REGISTRY.register("glowing_mushroom", GlowingMushroomFeature::new);
    public static final DeferredHolder<Feature<?>, GlowingMushroomPatchFeature> GLOWING_MUSHROOM_PATCH = REGISTRY.register("glowing_mushroom_patch", GlowingMushroomPatchFeature::new);

    public static final DeferredHolder<Feature<?>, CaveMushroomFeature> CAVE_MUSHROOM = REGISTRY.register("cave_mushroom", CaveMushroomFeature::new);

    public static final DeferredHolder<Feature<?>, CeilingBlockVineFeature> CEILING_BLOCK_VINE = REGISTRY.register("ceiling_block_vine", CeilingBlockVineFeature::new);
    public static final DeferredHolder<Feature<?>, CeilingBallFeature> CEILING_BALL = REGISTRY.register("ceiling_ball", CeilingBallFeature::new);

    public static final DeferredHolder<Feature<?>, BoulderFeature> BOULDER = REGISTRY.register("boulder", BoulderFeature::new);
    public static final DeferredHolder<Feature<?>, MiniVolcanoFeature> MINI_VOLCANO = REGISTRY.register("mini_volcano", MiniVolcanoFeature::new);
    public static final DeferredHolder<Feature<?>, MonolithFeature> MONOLITH = REGISTRY.register("monolith", MonolithFeature::new);

    public static final DeferredHolder<Feature<?>, StoneLilyFeature> STONE_LILY = REGISTRY.register("stone_lily", StoneLilyFeature::new);

    public static final DeferredHolder<Feature<?>, InvertedTreeFeature> INVERTED_TREE = REGISTRY.register("inverted_tree", InvertedTreeFeature::new);
    public static final DeferredHolder<Feature<?>, EldenGiantTreeFeature> ELDEN_GIANT_TREE = REGISTRY.register("elden_giant_tree", EldenGiantTreeFeature::new);

    public static final DeferredHolder<Feature<?>, HangingChorusFeature> HANGING_CHORUS = REGISTRY.register("hanging_chorus", HangingChorusFeature::new);

    public static final DeferredHolder<Feature<?>, MossySpikeFeature> MOSSY_SPIKE = REGISTRY.register("giant_spike", MossySpikeFeature::new);

    public static final DeferredHolder<Feature<?>, GobletMossPatchFeature> GOBLET_MOSS_PATCH = REGISTRY.register("goblet_moss_patch", GobletMossPatchFeature::new);
    public static final DeferredHolder<Feature<?>, MossHummockFeature> MOSS_HUMMOCK = REGISTRY.register("moss_hummock", MossHummockFeature::new);
    public static final DeferredHolder<Feature<?>, FloatingMossPatchFeature> FLOATING_MOSS_PATCH = REGISTRY.register("floating_moss_patch", FloatingMossPatchFeature::new);
    public static final DeferredHolder<Feature<?>, GobletSeagrassFeature> GOBLET_SEAGRASS = REGISTRY.register("goblet_seagrass", GobletSeagrassFeature::new);
    public static final DeferredHolder<Feature<?>, GobletKelpFeature> GOBLET_KELP = REGISTRY.register("goblet_kelp", GobletKelpFeature::new);

    public static final DeferredHolder<Feature<?>, LilyPadPatchFeature> LILY_PAD_PATCH = REGISTRY.register("lily_pad_patch", LilyPadPatchFeature::new);
    public static final DeferredHolder<Feature<?>, WaterLilyFeature> WATER_LILY = REGISTRY.register("water_lily", WaterLilyFeature::new);
    public static final DeferredHolder<Feature<?>, PowderedMossVeinFeature> POWDERED_MOSS_VEIN = REGISTRY.register("powdered_moss_vein", PowderedMossVeinFeature::new);
}
