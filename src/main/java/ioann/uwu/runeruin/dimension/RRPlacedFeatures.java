package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.heightproviders.VeryBiasedToBottomHeight;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class RRPlacedFeatures {

    public static final ResourceKey<PlacedFeature> MUSH_PLACED = RR.resourceKey(Registries.PLACED_FEATURE, "mush_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> ctx) {

        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = ctx.lookup(Registries.CONFIGURED_FEATURE);

        PlacedFeature placedFeature = new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MUSH_CONFIGURED),

                // https://misode.github.io/worldgen/placed-feature/
                // https://youtu.be/B4cyrSExjpc?si=8hDK_-61NfGq2K1D
                List.of(
                        CountPlacement.of(4096),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.of(VeryBiasedToBottomHeight.of(VerticalAnchor.bottom(), VerticalAnchor.belowTop(8), 8)),
                        BiomeFilter.biome()
                )
        );

        ctx.register(MUSH_PLACED, placedFeature);
    }
}
