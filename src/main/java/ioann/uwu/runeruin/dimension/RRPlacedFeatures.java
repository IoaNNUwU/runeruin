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

    public static final ResourceKey<PlacedFeature> SMALL_RED_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "small_red_wall_mushroom");
    public static final ResourceKey<PlacedFeature> BIG_RED_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "big_red_wall_mushroom");
    public static final ResourceKey<PlacedFeature> SMALL_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "small_brown_wall_mushroom");
    public static final ResourceKey<PlacedFeature> BIG_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "big_brown_wall_mushroom");

    public static void bootstrap(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = ctx.lookup(Registries.CONFIGURED_FEATURE);

        List<PlacementModifier> smallWallMushroomPlacement = List.of(
                CountPlacement.of(64),
                InSquarePlacement.spread(),
                HeightRangePlacement.of(VeryBiasedToBottomHeight.of(
                        VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_CEILING_Y - RRChunkGenerator.CEILING_TERRAIN_HEIGHT),
                        VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_CEILING_Y + RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT),
                        1
                ))
        );

        ctx.register(SMALL_RED_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.SMALL_RED_WALL_MUSHROOM),
                smallWallMushroomPlacement
        ));
        ctx.register(SMALL_BROWN_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.SMALL_BROWN_WALL_MUSHROOM),
                smallWallMushroomPlacement
        ));

        List<PlacementModifier> bigWallMushroomPlacement = List.of(
                CountPlacement.of(32),
                InSquarePlacement.spread(),
                HeightRangePlacement.of(VeryBiasedToBottomHeight.of(
                        VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_CEILING_Y - RRChunkGenerator.CEILING_TERRAIN_HEIGHT),
                        VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_CEILING_Y + RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT),
                        1
                ))
        );

        ctx.register(BIG_RED_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.BIG_RED_WALL_MUSHROOM),
                bigWallMushroomPlacement
        ));
        ctx.register(BIG_BROWN_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.BIG_BROWN_WALL_MUSHROOM),
                bigWallMushroomPlacement
        ));
    }
}
