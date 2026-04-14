package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.placements.WallPlacementFilter;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.heightproviders.VeryBiasedToBottomHeight;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

import static ioann.uwu.runeruin.dimension.RRChunkGenerator.*;

// https://misode.github.io/worldgen/placed-feature/
// https://youtu.be/B4cyrSExjpc?si=8hDK_-61NfGq2K1D

public class RRPlacedFeatures {

    public static final ResourceKey<PlacedFeature> SMALL_RED_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "small_red_wall_mushroom");
    public static final ResourceKey<PlacedFeature> BIG_RED_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "big_red_wall_mushroom");
    public static final ResourceKey<PlacedFeature> SMALL_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "small_brown_wall_mushroom");
    public static final ResourceKey<PlacedFeature> BIG_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "big_brown_wall_mushroom");

    public static final ResourceKey<PlacedFeature> LONG_CEILING_BLOCK_VINE = RR.resourceKey(Registries.PLACED_FEATURE, "long_ceiling_block_vine");
    public static final ResourceKey<PlacedFeature> CEILING_VINE = RR.resourceKey(Registries.PLACED_FEATURE, "ceiling_vine");

    public static final ResourceKey<PlacedFeature> TUFF_MOSS_BOULDER = RR.resourceKey(Registries.PLACED_FEATURE, "tuff_moss_boulder");

    public static final ResourceKey<PlacedFeature> MONOLITH = RR.resourceKey(Registries.PLACED_FEATURE, "monolith");

    public static final ResourceKey<PlacedFeature> MOSS_LAKE = RR.resourceKey(Registries.PLACED_FEATURE, "moss_lake");

    public static final ResourceKey<PlacedFeature> RARE_STONE_LILY = RR.resourceKey(Registries.PLACED_FEATURE, "rare_stone_lily");
    public static final ResourceKey<PlacedFeature> COMMON_STONE_LILY = RR.resourceKey(Registries.PLACED_FEATURE, "common_stone_lily");

    public static final ResourceKey<PlacedFeature> MOSS_BERRY_BUSH_PATCH = RR.resourceKey(Registries.PLACED_FEATURE, "moss_berry_bush_patch");

    public static void bootstrap(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = ctx.lookup(Registries.CONFIGURED_FEATURE);

        List<PlacementModifier> smallWallMushroomPlacement = List.of(
                CountPlacement.of(64),
                InSquarePlacement.spread(),
                HeightRangePlacement.of(VeryBiasedToBottomHeight.of(
                        VerticalAnchor.aboveBottom(BLOOMING_CAVES_CEILING_Y - CEILING_TERRAIN_HEIGHT),
                        VerticalAnchor.aboveBottom(BLOOMING_CAVES_CEILING_Y + RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT),
                        1
                )),
                new WallPlacementFilter(
                        List.of(Blocks.STONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState()),
                        List.of(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(), Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState())
                )
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
                        VerticalAnchor.aboveBottom(BLOOMING_CAVES_CEILING_Y - CEILING_TERRAIN_HEIGHT),
                        VerticalAnchor.aboveBottom(BLOOMING_CAVES_CEILING_Y + RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT),
                        1
                )),
                new WallPlacementFilter(
                        List.of(Blocks.STONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState()),
                        List.of(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(), Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState())
                )
        );

        ctx.register(BIG_RED_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.BIG_RED_WALL_MUSHROOM),
                bigWallMushroomPlacement
        ));
        ctx.register(BIG_BROWN_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.BIG_BROWN_WALL_MUSHROOM),
                bigWallMushroomPlacement
        ));

        List<PlacementModifier> ceilingBlockVinePlacement = List.of(
                CountPlacement.of(16),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                        VerticalAnchor.absolute(BLOOMING_CAVES_CEILING_Y - CEILING_TERRAIN_HEIGHT - 10),
                        VerticalAnchor.absolute(BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_TERRAIN_HEIGHT)
                ),
                EnvironmentScanPlacement.scanningFor(
                        Direction.UP,
                        BlockPredicate.hasSturdyFace(Direction.DOWN),
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        16
                )
        );

        ctx.register(LONG_CEILING_BLOCK_VINE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.LONG_CEILING_BLOCK_VINE),
                ceilingBlockVinePlacement
        ));

        ctx.register(CEILING_VINE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.CEILING_VINE),
                List.of(
                        CountPlacement.of(188),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.absolute(BLOOMING_CAVES_CEILING_Y - CEILING_TERRAIN_HEIGHT - 10),
                                VerticalAnchor.absolute(BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_TERRAIN_HEIGHT)
                        ),
                        EnvironmentScanPlacement.scanningFor(
                                Direction.UP,
                                BlockPredicate.hasSturdyFace(Direction.DOWN),
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                16
                        ),
                        RandomOffsetPlacement.vertical(ConstantInt.of(-1))
                )
        ));

        ctx.register(TUFF_MOSS_BOULDER, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.TUFF_MOSS_BOULDER),
                List.of(
                        CountPlacement.of(4),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_Y),
                                VerticalAnchor.aboveBottom(
                                        RRChunkGenerator.BLOOMING_CAVES_Y +
                                                RRChunkGenerator.TOP_LAYER_MAX_BASELINE_HEIGHT +
                                                RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT
                                )
                        ),
                        EnvironmentScanPlacement.scanningFor(
                                Direction.DOWN,
                                BlockPredicate.hasSturdyFace(Direction.UP),
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                16
                        ),
                        RandomOffsetPlacement.vertical(ConstantInt.of(-2))
                )
        ));

        ctx.register(MONOLITH, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MONOLITH),
                List.of(
                        CountPlacement.of(1),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_Y),
                                VerticalAnchor.aboveBottom(
                                        RRChunkGenerator.BLOOMING_CAVES_Y +
                                                RRChunkGenerator.TOP_LAYER_MAX_BASELINE_HEIGHT +
                                                RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT
                                )
                        ),
                        PlacementUtils.HEIGHTMAP,
                        RarityFilter.onAverageOnceEvery(128),
                        EnvironmentScanPlacement.scanningFor(
                                Direction.DOWN,
                                BlockPredicate.hasSturdyFace(Direction.UP),
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                16
                        ),
                        RandomOffsetPlacement.vertical(ConstantInt.of(-2)),
                        BiomeFilter.biome()
                )
        ));

        ctx.register(MOSS_LAKE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MOSS_POOL_WITH_DRIPLEAVES),
                List.of(
                        CountPlacement.of(16),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_Y),
                                VerticalAnchor.aboveBottom(
                                        RRChunkGenerator.BLOOMING_CAVES_Y +
                                                RRChunkGenerator.TOP_LAYER_MAX_BASELINE_HEIGHT +
                                                RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT
                                )
                        ),
                        EnvironmentScanPlacement.scanningFor(
                                Direction.DOWN,
                                BlockPredicate.hasSturdyFace(Direction.UP),
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                16
                        ),
                        BiomeFilter.biome()
                )
        ));

        ctx.register(RARE_STONE_LILY, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.STONE_LILY),
                List.of(
                        CountPlacement.of(1),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_Y),
                                VerticalAnchor.aboveBottom(
                                        RRChunkGenerator.BLOOMING_CAVES_Y +
                                                RRChunkGenerator.TOP_LAYER_MAX_BASELINE_HEIGHT +
                                                RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT
                                )
                        ),
                        EnvironmentScanPlacement.scanningFor(
                                Direction.DOWN,
                                BlockPredicate.hasSturdyFace(Direction.UP),
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                16
                        ),
                        BiomeFilter.biome()
                )
        ));

        ctx.register(COMMON_STONE_LILY, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.STONE_LILY),
                List.of(
                        CountPlacement.of(16),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_Y),
                                VerticalAnchor.aboveBottom(
                                        RRChunkGenerator.BLOOMING_CAVES_Y +
                                                RRChunkGenerator.TOP_LAYER_MAX_BASELINE_HEIGHT +
                                                RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT
                                )
                        ),
                        EnvironmentScanPlacement.scanningFor(
                                Direction.DOWN,
                                BlockPredicate.matchesBlocks(
                                        Blocks.MOSS_BLOCK,
                                        Blocks.MOSSY_COBBLESTONE,
                                        Blocks.STONE,
                                        Blocks.CLAY,
                                        Blocks.WATER
                                ),
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                16
                        ),
                        BiomeFilter.biome()
                )
        ));

        ctx.register(MOSS_BERRY_BUSH_PATCH, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MOSS_BERRY_BUSH_PATCH),
                List.of(
                        RarityFilter.onAverageOnceEvery(1),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.aboveBottom(RRChunkGenerator.BLOOMING_CAVES_Y),
                                VerticalAnchor.aboveBottom(
                                        RRChunkGenerator.BLOOMING_CAVES_Y +
                                                RRChunkGenerator.TOP_LAYER_MAX_BASELINE_HEIGHT +
                                                RRChunkGenerator.TOP_LAYER_TERRAIN_HEIGHT
                                )
                        ),
                        BiomeFilter.biome(),
                        CountPlacement.of(96),
                        RandomOffsetPlacement.ofTriangle(7, 3),
                        EnvironmentScanPlacement.scanningFor(
                                Direction.DOWN,
                                BlockPredicate.matchesBlocks(
                                        Blocks.MOSS_BLOCK,
                                        Blocks.MOSSY_COBBLESTONE,
                                        Blocks.STONE
                                ),
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                16
                        ),
                        RandomOffsetPlacement.vertical(ConstantInt.of(1))

                )
        ));
    }
}
