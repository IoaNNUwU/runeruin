package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RRFeatures {

    public static final DeferredRegister<Feature<?>> REGISTRY = DeferredRegister.create(Registries.FEATURE, RR.MODID);

    public static final ResourceKey<Feature<?>> MUSH_FEATURE_RESOURCE_KEY = RR.resourceKey(Registries.FEATURE, "mush_feature");

    public static final DeferredHolder<Feature<?>, WallMushFeature> MUSH_FEATURE = REGISTRY.register("mush_feature", WallMushFeature::new);
}
