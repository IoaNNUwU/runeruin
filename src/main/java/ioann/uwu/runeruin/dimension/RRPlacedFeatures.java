package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.heightproviders.VeryBiasedToBottomHeight;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

// https://misode.github.io/worldgen/placed-feature/
// https://youtu.be/B4cyrSExjpc?si=8hDK_-61NfGq2K1D

public class RRPlacedFeatures {

    public static final ResourceKey<PlacedFeature> RED_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "red_wall_mushroom");
    public static final ResourceKey<PlacedFeature> BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "brown_wall_mushroom");

    public static void bootstrap(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = ctx.lookup(Registries.CONFIGURED_FEATURE);

        ctx.register(RED_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MUSH_CONFIGURED),
                List.of(
                        CountPlacement.of(128),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.of(VeryBiasedToBottomHeight.of(
                                VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_CEILING_Y - RRChunkGenerator.CEILING_TERRAIN_HEIGHT),
                                VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_CEILING_Y + RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT),
                                        1
                                )) //,
                        // BiomeFilter.biome()
                )
        ));
        /*
        ctx.register(BROWN_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MUSH_CONFIGURED),
                List.of(
                        CountPlacement.of(4096),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.of(VeryBiasedToBottomHeight.of(VerticalAnchor.bottom(), VerticalAnchor.belowTop(8), 8)),
                        BiomeFilter.biome()
                )
        ));
         */
    }
}
