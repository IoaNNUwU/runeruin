package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.features.BoulderFeature;
import ioann.uwu.runeruin.dimension.features.CeilingBlockVineFeature;
import ioann.uwu.runeruin.dimension.features.MonolithFeature;
import ioann.uwu.runeruin.dimension.features.WallMushroomFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRFeatures {

    public static final DeferredRegister<Feature<?>> REGISTRY = DeferredRegister.create(Registries.FEATURE, RR.MODID);

    public static final DeferredHolder<Feature<?>, WallMushroomFeature> WALL_MUSHROOM =
            REGISTRY.register("wall_mushroom", WallMushroomFeature::new);

    public static final DeferredHolder<Feature<?>, CeilingBlockVineFeature> CEILING_BLOCK_VINE =
            REGISTRY.register("ceiling_block_wine", CeilingBlockVineFeature::new);

    public static final DeferredHolder<Feature<?>, BoulderFeature> BOULDER =
            REGISTRY.register("boulder", BoulderFeature::new);

    public static final DeferredHolder<Feature<?>, MonolithFeature> MONOLITH =
            REGISTRY.register("monolith", MonolithFeature::new);
}
