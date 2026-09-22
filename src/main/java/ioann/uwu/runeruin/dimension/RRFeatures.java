package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.features.*;
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

    public static final DeferredHolder<Feature<?>, CeilingBlockVineFeature> CEILING_BLOCK_VINE = REGISTRY.register("ceiling_block_wine", CeilingBlockVineFeature::new);
    public static final DeferredHolder<Feature<?>, CeilingBallFeature> CEILING_BALL = REGISTRY.register("ceiling_ball", CeilingBallFeature::new);

    public static final DeferredHolder<Feature<?>, BoulderFeature> BOULDER = REGISTRY.register("boulder", BoulderFeature::new);
    public static final DeferredHolder<Feature<?>, MiniVolcanoFeature> MINI_VOLCANO = REGISTRY.register("mini_volcano", MiniVolcanoFeature::new);
    public static final DeferredHolder<Feature<?>, MonolithFeature> MONOLITH = REGISTRY.register("monolith", MonolithFeature::new);

    public static final DeferredHolder<Feature<?>, StoneLilyFeature> STONE_LILY = REGISTRY.register("stone_lily", StoneLilyFeature::new);

    public static final DeferredHolder<Feature<?>, InvertedTreeFeature> INVERTED_TREE = REGISTRY.register("inverted_tree", InvertedTreeFeature::new);
    public static final DeferredHolder<Feature<?>, EldenGiantTreeFeature> ELDEN_GIANT_TREE = REGISTRY.register("elden_giant_tree", EldenGiantTreeFeature::new);

    public static final DeferredHolder<Feature<?>, MossySpikeFeature> GIANT_SPIKE = REGISTRY.register("giant_spike", MossySpikeFeature::new);

    public static final DeferredHolder<Feature<?>, GobletMossPatchFeature> GOBLET_MOSS_PATCH = REGISTRY.register("goblet_moss_patch", GobletMossPatchFeature::new);
    public static final DeferredHolder<Feature<?>, GobletSeagrassFeature> GOBLET_SEAGRASS = REGISTRY.register("goblet_seagrass", GobletSeagrassFeature::new);
    public static final DeferredHolder<Feature<?>, GobletKelpFeature> GOBLET_KELP = REGISTRY.register("goblet_kelp", GobletKelpFeature::new);

    public static final DeferredHolder<Feature<?>, LilyPadPatchFeature> SMALL_LILY_PAD_PATCH = REGISTRY.register("small_lily_pad_patch", LilyPadPatchFeature::new);
    public static final DeferredHolder<Feature<?>, LilyPadPatchFeature> BIG_LILY_PAD_PATCH = REGISTRY.register("big_lily_pad_patch", LilyPadPatchFeature::new);
    public static final DeferredHolder<Feature<?>, PowderedMossVeinFeature> POWDERED_MOSS_VEIN = REGISTRY.register("powdered_moss_vein", PowderedMossVeinFeature::new);
}
