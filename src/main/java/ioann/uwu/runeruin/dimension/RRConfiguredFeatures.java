package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class RRConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> MUSH_CONFIGURED = RR.resourceKey(Registries.CONFIGURED_FEATURE, "mush_configured");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {

        ConfiguredFeature<?, ?> configuredFeature = new ConfiguredFeature<>(
                RRFeatures.MUSH_FEATURE.get(),
                FeatureConfiguration.NONE
        );

        ctx.register(MUSH_CONFIGURED, configuredFeature);
    }
}
