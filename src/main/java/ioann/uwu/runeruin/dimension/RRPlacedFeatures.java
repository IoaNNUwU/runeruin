package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.placements.ChunkCenterPlacement;
import ioann.uwu.runeruin.dimension.placements.GobletUnderwaterPlacement;
import ioann.uwu.runeruin.dimension.placements.WallPlacementFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.heightproviders.VeryBiasedToBottomHeight;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.ArrayList;
import java.util.List;

import static ioann.uwu.runeruin.dimension.Const.*;

// https://misode.github.io/worldgen/placed-feature/
// https://youtu.be/B4cyrSExjpc?si=8hDK_-61NfGq2K1D

public class RRPlacedFeatures {

    public static final ResourceKey<PlacedFeature> SMALL_RED_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "small_red_wall_mushroom");
    public static final ResourceKey<PlacedFeature> BIG_RED_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "big_red_wall_mushroom");
    public static final ResourceKey<PlacedFeature> SMALL_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "small_brown_wall_mushroom");
    public static final ResourceKey<PlacedFeature> BIG_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "big_brown_wall_mushroom");
    public static final ResourceKey<PlacedFeature> ASHEN_WALL_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "ashen_wall_mushroom");
    public static final ResourceKey<PlacedFeature> ASHEN_WALL_MUSHROOM_CLUSTER = RR.resourceKey(Registries.PLACED_FEATURE, "ashen_wall_mushroom_cluster");
    public static final ResourceKey<PlacedFeature> ASHEN_WALL_MUSHROOM_UPPER = RR.resourceKey(Registries.PLACED_FEATURE, "ashen_wall_mushroom_upper");
    public static final ResourceKey<PlacedFeature> ASHEN_WALL_MUSHROOM_CLUSTER_UPPER = RR.resourceKey(Registries.PLACED_FEATURE, "ashen_wall_mushroom_cluster_upper");

    public static final ResourceKey<PlacedFeature> CEILING_VINE = RR.resourceKey(Registries.PLACED_FEATURE, "ceiling_vine");
    public static final ResourceKey<PlacedFeature> LONG_CEILING_BLOCK_VINE = RR.resourceKey(Registries.PLACED_FEATURE, "long_ceiling_block_vine");
    public static final ResourceKey<PlacedFeature> CEILING_BALL = RR.resourceKey(Registries.PLACED_FEATURE, "ceiling_ball");

    public static final ResourceKey<PlacedFeature> TUFF_MOSS_BOULDER = RR.resourceKey(Registries.PLACED_FEATURE, "tuff_moss_boulder");
    public static final ResourceKey<PlacedFeature> MINI_VOLCANO = RR.resourceKey(Registries.PLACED_FEATURE, "mini_volcano");

    public static final ResourceKey<PlacedFeature> MONOLITH = RR.resourceKey(Registries.PLACED_FEATURE, "monolith");

    public static final ResourceKey<PlacedFeature> MOSS_LAKE = RR.resourceKey(Registries.PLACED_FEATURE, "moss_lake");

    public static final ResourceKey<PlacedFeature> RARE_STONE_LILY = RR.resourceKey(Registries.PLACED_FEATURE, "rare_stone_lily");
    public static final ResourceKey<PlacedFeature> COMMON_STONE_LILY = RR.resourceKey(Registries.PLACED_FEATURE, "common_stone_lily");

    public static final ResourceKey<PlacedFeature> WISPBERRY_BUSH_PATCH = RR.resourceKey(Registries.PLACED_FEATURE, "wispberry_bush_patch");
    public static final ResourceKey<PlacedFeature> POWDERED_MOSS = RR.resourceKey(Registries.PLACED_FEATURE, "powdered_moss");

    public static final ResourceKey<PlacedFeature> GLOWING_MOSS_VEGETATION = RR.resourceKey(Registries.PLACED_FEATURE, "glowing_moss_vegetation");
    public static final ResourceKey<PlacedFeature> GLOWING_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "glowing_mushroom");
    public static final ResourceKey<PlacedFeature> SMALL_GLOWING_MUSHROOM = RR.resourceKey(Registries.PLACED_FEATURE, "small_glowing_mushroom");
    public static final ResourceKey<PlacedFeature> MOSS_VEGETATION = RR.resourceKey(Registries.PLACED_FEATURE, "moss_vegetation");

    public static final ResourceKey<PlacedFeature> DEEP_CEILING_VINE = RR.resourceKey(Registries.PLACED_FEATURE, "deep_ceiling_vine");
    public static final ResourceKey<PlacedFeature> DEEP_CEILING_BLOCK_VINE = RR.resourceKey(Registries.PLACED_FEATURE, "deep_ceiling_block_vine");

    public static final ResourceKey<PlacedFeature> INVERTED_TREE = RR.resourceKey(Registries.PLACED_FEATURE, "inverted_tree");
    public static final ResourceKey<PlacedFeature> ELDEN_GIANT_TREE = RR.resourceKey(Registries.PLACED_FEATURE, "elden_giant_tree");

    public static final ResourceKey<PlacedFeature> DRIPSTONE_SPIKE = RR.resourceKey(Registries.PLACED_FEATURE, "dripstone_spike");
    public static final ResourceKey<PlacedFeature> STONE_SPIKE = RR.resourceKey(Registries.PLACED_FEATURE, "stone_spike");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_SPIKE = RR.resourceKey(Registries.PLACED_FEATURE, "deepslate_spike");
    public static final ResourceKey<PlacedFeature> DEEP_DEEPSLATE_SPIKE = RR.resourceKey(Registries.PLACED_FEATURE, "deep_deepslate_spike");

    public static final ResourceKey<PlacedFeature> GOBLET_MOSS_PATCH = RR.resourceKey(Registries.PLACED_FEATURE, "goblet_moss_patch");
    public static final ResourceKey<PlacedFeature> GOBLET_MOSS_PATCH_UNDERWATER = RR.resourceKey(Registries.PLACED_FEATURE, "goblet_moss_patch_underwater");
    public static final ResourceKey<PlacedFeature> GOBLET_SEAGRASS = RR.resourceKey(Registries.PLACED_FEATURE, "goblet_seagrass");
    public static final ResourceKey<PlacedFeature> GOBLET_KELP = RR.resourceKey(Registries.PLACED_FEATURE, "goblet_kelp");
    public static final ResourceKey<PlacedFeature> GOBLET_DEEP_ROOTS = RR.resourceKey(Registries.PLACED_FEATURE, "goblet_deep_roots");
    public static final ResourceKey<PlacedFeature> DEEP_ROOTS_GRASS = RR.resourceKey(Registries.PLACED_FEATURE, "deep_roots_grass");
    public static final ResourceKey<PlacedFeature> SMALL_LILY_PAD_PATCH = RR.resourceKey(Registries.PLACED_FEATURE, "small_lily_pad_patch");
    public static final ResourceKey<PlacedFeature> BIG_LILY_PAD_PATCH = RR.resourceKey(Registries.PLACED_FEATURE, "big_lily_pad_patch");
    public static final ResourceKey<PlacedFeature> WATER_LILY = RR.resourceKey(Registries.PLACED_FEATURE, "water_lily");
    public static final ResourceKey<PlacedFeature> JUNGLE_MEGA_TREE_ON_NON_MOSS = RR.resourceKey(Registries.PLACED_FEATURE, "jungle_mega_tree_on_non_moss");
    public static final ResourceKey<PlacedFeature> SWAMP_JUNGLE_TREES = RR.resourceKey(Registries.PLACED_FEATURE, "swamp_jungle_trees");

    // Height bands that decorations start their surface scan from.
    private static final int BLOOMING_FLOOR_TOP = BLOOMING_CAVES_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_TERRAIN_HEIGHT;
    private static final int BLOOMING_CEILING_BOTTOM = BLOOMING_CAVES_CEILING_Y - CEILING_TERRAIN_HEIGHT - 10;
    private static final int BLOOMING_CEILING_TOP = BLOOMING_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_TERRAIN_HEIGHT;
    private static final int DEEP_CEILING_BOTTOM = DEEP_CAVES_CEILING_Y - CEILING_TERRAIN_HEIGHT - 10;
    private static final int DEEP_CEILING_TOP = DEEP_CAVES_CEILING_Y + TOP_LAYER_MAX_BASELINE_HEIGHT + TOP_LAYER_TERRAIN_HEIGHT;

    public static void bootstrap(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = ctx.lookup(Registries.CONFIGURED_FEATURE);
        BlockPredicate glowingMushroomSupport = BlockPredicate.allOf(
                BlockPredicate.hasSturdyFace(Direction.UP),
                BlockPredicate.not(BlockPredicate.matchesBlocks(
                        RRBlocks.GLOWING_MUSHROOM_CAP.get(),
                        RRBlocks.GLOWING_MUSHROOM_STEM.get(),
                        RRBlocks.GLOWING_MUSHROOM.get()
                ))
        );
        BlockPredicate bloomingGround = BlockPredicate.matchesBlocks(
                Blocks.MOSS_BLOCK,
                Blocks.MOSSY_COBBLESTONE,
                Blocks.STONE,
                Blocks.CLAY,
                Blocks.WATER
        );

        for (CaveMushroomKind kind : CaveMushroomKind.values()) {
            ctx.register(kind.placedKey(), new PlacedFeature(
                    configuredFeatures.getOrThrow(kind.configuredKey()),
                    floorPlacement(CountPlacement.of(3), LOST_CAVES_Y, BLOOMING_CAVES_CEILING_Y, BlockPredicate.allOf(
                            BlockPredicate.hasSturdyFace(Direction.UP),
                            BlockPredicate.not(BlockPredicate.matchesBlocks(CaveMushroomKind.BLOCKS))
                    ), 32, 1)
            ));
        }

        // Baobab canopy uses moss blocks, so keep mega jungle trees off that surface.
        ctx.register(JUNGLE_MEGA_TREE_ON_NON_MOSS, new PlacedFeature(
                configuredFeatures.getOrThrow(TreeFeatures.MEGA_JUNGLE_TREE),
                List.of(
                        PlacementUtils.filteredByBlockSurvival(Blocks.JUNGLE_SAPLING),
                        BlockPredicateFilter.forPredicate(BlockPredicate.not(
                                BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), Blocks.MOSS_BLOCK)
                        ))
                )
        ));

        ctx.register(SWAMP_JUNGLE_TREES, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.SWAMP_JUNGLE_TREES),
                VegetationPlacements.treePlacement(PlacementUtils.countExtra(8, 0.1F, 1))
        ));

        ctx.register(SMALL_RED_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.SMALL_RED_WALL_MUSHROOM),
                wallMushroomPlacement(64)
        ));
        ctx.register(SMALL_BROWN_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.SMALL_BROWN_WALL_MUSHROOM),
                wallMushroomPlacement(64)
        ));
        ctx.register(BIG_RED_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.BIG_RED_WALL_MUSHROOM),
                wallMushroomPlacement(32)
        ));
        ctx.register(BIG_BROWN_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.BIG_BROWN_WALL_MUSHROOM),
                wallMushroomPlacement(32)
        ));

        ctx.register(ASHEN_WALL_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.ASHEN_WALL_MUSHROOM),
                ashenWallMushroomPlacement(8)
        ));
        ctx.register(ASHEN_WALL_MUSHROOM_CLUSTER, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.ASHEN_WALL_MUSHROOM_CLUSTER),
                ashenWallMushroomPlacement(4)
        ));
        ctx.register(ASHEN_WALL_MUSHROOM_UPPER, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.ASHEN_WALL_MUSHROOM_UPPER),
                ashenWallMushroomPlacement(8)
        ));
        ctx.register(ASHEN_WALL_MUSHROOM_CLUSTER_UPPER, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.ASHEN_WALL_MUSHROOM_CLUSTER_UPPER),
                ashenWallMushroomPlacement(4)
        ));

        ctx.register(LONG_CEILING_BLOCK_VINE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.LONG_CEILING_BLOCK_VINE),
                ceilingPlacement(CountPlacement.of(16), BLOOMING_CEILING_BOTTOM, BLOOMING_CEILING_TOP, 0)
        ));
        ctx.register(CEILING_BALL, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.CEILING_BALL),
                ceilingPlacement(CountPlacement.of(8), BLOOMING_CEILING_BOTTOM, BLOOMING_CEILING_TOP, 0)
        ));
        ctx.register(CEILING_VINE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.CEILING_VINE),
                ceilingPlacement(CountPlacement.of(188), BLOOMING_CEILING_BOTTOM, BLOOMING_CEILING_TOP, -1)
        ));

        ctx.register(TUFF_MOSS_BOULDER, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.TUFF_MOSS_BOULDER),
                floorPlacement(CountPlacement.of(4), BLOOMING_CAVES_Y, BLOOMING_FLOOR_TOP, bloomingGround, 16, -2)
        ));
        ctx.register(MINI_VOLCANO, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MINI_VOLCANO),
                floorPlacement(CountPlacement.of(4), BLOOMING_CAVES_Y, BLOOMING_FLOOR_TOP,
                        BlockPredicate.matchesBlocks(Blocks.MOSS_BLOCK, Blocks.MOSSY_COBBLESTONE, Blocks.STONE, Blocks.CLAY), 16, -2)
        ));

        ctx.register(MONOLITH, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MONOLITH),
                List.of(
                        CountPlacement.of(1),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(BLOOMING_CAVES_Y), VerticalAnchor.absolute(BLOOMING_FLOOR_TOP)),
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
                floorPlacement(CountPlacement.of(16), BLOOMING_CAVES_Y, BLOOMING_FLOOR_TOP, BlockPredicate.hasSturdyFace(Direction.UP), 16, 0)
        ));
        ctx.register(RARE_STONE_LILY, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.STONE_LILY),
                floorPlacement(CountPlacement.of(1), BLOOMING_CAVES_Y, BLOOMING_FLOOR_TOP, BlockPredicate.hasSturdyFace(Direction.UP), 16, 0)
        ));
        ctx.register(COMMON_STONE_LILY, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.STONE_LILY),
                floorPlacement(CountPlacement.of(16), BLOOMING_CAVES_Y, BLOOMING_FLOOR_TOP, bloomingGround, 16, 0)
        ));

        ctx.register(WISPBERRY_BUSH_PATCH, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.WISPBERRY_BUSH_PATCH),
                List.of(
                        RarityFilter.onAverageOnceEvery(1),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(BLOOMING_CAVES_Y), VerticalAnchor.absolute(BLOOMING_FLOOR_TOP)),
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

        ctx.register(POWDERED_MOSS, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.POWDERED_MOSS),
                floorPlacement(CountPlacement.of(6), BLOOMING_CAVES_Y, BLOOMING_CAVES_CEILING_Y, BlockPredicate.matchesBlocks(Blocks.MOSS_BLOCK), 16, 0)
        ));

        ctx.register(DEEP_CEILING_VINE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.CEILING_VINE),
                ceilingPlacement(CountPlacement.of(188), DEEP_CEILING_BOTTOM, DEEP_CEILING_TOP, -1)
        ));
        ctx.register(DEEP_CEILING_BLOCK_VINE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.LONG_CEILING_BLOCK_VINE),
                ceilingPlacement(CountPlacement.of(16), DEEP_CEILING_BOTTOM, DEEP_CEILING_TOP, 0)
        ));
        ctx.register(INVERTED_TREE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.INVERTED_TREE),
                ceilingPlacement(CountPlacement.of(8), DEEP_CEILING_BOTTOM, DEEP_CAVES_CEILING_Y, 0)
        ));

        ctx.register(ELDEN_GIANT_TREE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.ELDEN_GIANT_TREE),
                List.of(
                        RarityFilter.onAverageOnceEvery(2),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                        BiomeFilter.biome(),
                        BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(
                                RRBlocks.ELDEN_SAPLING.get().defaultBlockState(),
                                BlockPos.ZERO
                        ))
                )
        ));

        ctx.register(GLOWING_MOSS_VEGETATION, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.GLOWING_MOSS_VEGETATION),
                floorPlacement(CountPlacement.of(128), DEEP_CAVES_Y, DEEP_CAVES_CEILING_Y, BlockPredicate.solid(), 12, 1)
        ));

        ctx.register(GLOWING_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.GLOWING_MUSHROOM),
                List.of(
                        CountPlacement.of(10),
                        RarityFilter.onAverageOnceEvery(2),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.absolute(DEEP_CAVES_Y),
                                VerticalAnchor.absolute(DEEP_CAVES_CEILING_Y)
                        ),
                        EnvironmentScanPlacement.scanningFor(
                                Direction.DOWN,
                                glowingMushroomSupport,
                                BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                32
                        ),
                        RandomOffsetPlacement.vertical(ConstantInt.of(1)),
                        BiomeFilter.biome()
                )
        ));
        ctx.register(SMALL_GLOWING_MUSHROOM, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.SMALL_GLOWING_MUSHROOM),
                floorPlacement(CountPlacement.of(6), DEEP_CAVES_Y, DEEP_CAVES_CEILING_Y, glowingMushroomSupport, 32, 1)
        ));

        ctx.register(MOSS_VEGETATION, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.MOSS_VEGETATION),
                floorPlacement(CountPlacement.of(200), BLOOMING_CAVES_Y, BLOOMING_CAVES_CEILING_Y, BlockPredicate.solid(), 12, 1)
        ));

        ctx.register(DRIPSTONE_SPIKE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.DRIPSTONE_SPIKE),
                bandPlacement(CountPlacement.of(UniformInt.of(10, 48)), DEEP_CAVES_Y, DEEP_CAVES_CEILING_Y)
        ));
        ctx.register(STONE_SPIKE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.STONE_SPIKE),
                bandPlacement(CountPlacement.of(UniformInt.of(10, 48)), DEEP_CAVES_Y, DEEP_CAVES_CEILING_Y)
        ));
        ctx.register(DEEPSLATE_SPIKE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.DEEPSLATE_SPIKE),
                bandPlacement(CountPlacement.of(UniformInt.of(10, 48)), LOST_CAVES_Y, LOST_CAVES_CEILING_Y)
        ));
        ctx.register(DEEP_DEEPSLATE_SPIKE, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.DEEPSLATE_SPIKE),
                bandPlacement(CountPlacement.of(UniformInt.of(10, 48)), DEEP_CAVES_Y, DEEP_CAVES_CEILING_Y)
        ));

        ctx.register(GOBLET_MOSS_PATCH, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.GOBLET_MOSS_PATCH),
                floorPlacement(CountPlacement.of(31), LOST_CAVES_Y, DEEP_CAVES_CEILING_Y,
                        BlockPredicate.matchesTag(RRTags.GOBLET_MOSS_REPLACEABLE), 32, 1)
        ));
        ctx.register(GOBLET_MOSS_PATCH_UNDERWATER, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.GOBLET_MOSS_PATCH_UNDERWATER),
                scanPlacement(CountPlacement.of(88), LOST_CAVES_Y, DEEP_CAVES_CEILING_Y, Direction.DOWN,
                        BlockPredicate.matchesTag(RRTags.GOBLET_MOSS_REPLACEABLE), BlockPredicate.matchesBlocks(Blocks.WATER), 32, 1)
        ));

        ctx.register(GOBLET_SEAGRASS, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.GOBLET_SEAGRASS),
                gobletUnderwaterPlacement(CountPlacement.of(48))
        ));
        ctx.register(GOBLET_KELP, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.GOBLET_KELP),
                gobletUnderwaterPlacement(NoiseBasedCountPlacement.of(120, 80.0, 0.0))
        ));

        ctx.register(GOBLET_DEEP_ROOTS, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.GOBLET_DEEP_ROOTS),
                floorPlacement(CountPlacement.of(64), LOST_CAVES_Y, DEEP_CAVES_CEILING_Y,
                        BlockPredicate.matchesBlocks(RRBlocks.GIANT_GOBLET_BUD.get()), 32, 1)
        ));
        ctx.register(DEEP_ROOTS_GRASS, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.DEEP_ROOTS_GRASS),
                floorPlacement(CountPlacement.of(64), DEEP_CAVES_Y, DEEP_CAVES_CEILING_Y,
                        BlockPredicate.matchesBlocks(RRBlocks.GLOWING_MOSS.get()), 32, 1)
        ));

        ctx.register(SMALL_LILY_PAD_PATCH, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.SMALL_LILY_PAD_PATCH),
                waterSurfacePlacement(CountPlacement.of(2))
        ));
        ctx.register(BIG_LILY_PAD_PATCH, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.BIG_LILY_PAD_PATCH),
                waterSurfacePlacement(CountPlacement.of(1))
        ));
        ctx.register(WATER_LILY, new PlacedFeature(
                configuredFeatures.getOrThrow(RRConfiguredFeatures.WATER_LILY),
                waterSurfacePlacement(RarityFilter.onAverageOnceEvery(3))
        ));
    }

    /** Spread over the chunk at a random height in [minY, maxY]; spikes find their own surface. */
    private static List<PlacementModifier> bandPlacement(PlacementModifier count, int minY, int maxY) {
        return List.of(
                count,
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(minY), VerticalAnchor.absolute(maxY)),
                BiomeFilter.biome()
        );
    }

    /** Scans down from the band to the first air block above {@code ground}, then shifts by {@code offsetY}. */
    private static List<PlacementModifier> floorPlacement(PlacementModifier count, int minY, int maxY, BlockPredicate ground,
                                                         int scanSteps, int offsetY) {
        return scanPlacement(count, minY, maxY, Direction.DOWN, ground, BlockPredicate.ONLY_IN_AIR_PREDICATE, scanSteps, offsetY);
    }

    /** Scans up from the band to the first air block under a sturdy ceiling, then shifts by {@code offsetY}. */
    private static List<PlacementModifier> ceilingPlacement(PlacementModifier count, int minY, int maxY, int offsetY) {
        return scanPlacement(count, minY, maxY, Direction.UP, BlockPredicate.hasSturdyFace(Direction.DOWN),
                BlockPredicate.ONLY_IN_AIR_PREDICATE, 16, offsetY);
    }

    private static List<PlacementModifier> scanPlacement(PlacementModifier count, int minY, int maxY, Direction direction,
                                                        BlockPredicate target, BlockPredicate allowed, int scanSteps, int offsetY) {
        List<PlacementModifier> placement = new ArrayList<>(List.of(
                count,
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(minY), VerticalAnchor.absolute(maxY)),
                EnvironmentScanPlacement.scanningFor(direction, target, allowed, scanSteps)
        ));
        if (offsetY != 0) {
            placement.add(RandomOffsetPlacement.vertical(ConstantInt.of(offsetY)));
        }
        placement.add(BiomeFilter.biome());
        return List.copyOf(placement);
    }

    /** Lily pads and water lilies: on top of the water surface in the lost and deep caves. */
    private static List<PlacementModifier> waterSurfacePlacement(PlacementModifier frequency) {
        return floorPlacement(frequency, LOST_CAVES_Y, DEEP_CAVES_CEILING_Y, BlockPredicate.matchesBlocks(Blocks.WATER), 32, 1);
    }

    private static List<PlacementModifier> gobletUnderwaterPlacement(PlacementModifier count) {
        return List.of(
                count,
                InSquarePlacement.spread(),
                new GobletUnderwaterPlacement(),
                BiomeFilter.biome()
        );
    }

    private static List<PlacementModifier> wallMushroomPlacement(int count) {
        return List.of(
                CountPlacement.of(count),
                InSquarePlacement.spread(),
                HeightRangePlacement.of(VeryBiasedToBottomHeight.of(
                        VerticalAnchor.absolute(BLOOMING_CAVES_CEILING_Y - CEILING_TERRAIN_HEIGHT),
                        VerticalAnchor.absolute(BLOOMING_CAVES_CEILING_Y + TOP_LAYER_TERRAIN_HEIGHT),
                        1
                )),
                new WallPlacementFilter(
                        List.of(Blocks.STONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState()),
                        List.of(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(), Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState())
                ),
                BiomeFilter.biome()
        );
    }

    private static List<PlacementModifier> ashenWallMushroomPlacement(int count) {
        return List.of(
                CountPlacement.of(count),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                        VerticalAnchor.absolute(LOST_CAVES_CEILING_Y - CEILING_TERRAIN_HEIGHT),
                        VerticalAnchor.absolute(DEEP_CAVES_Y + TOP_LAYER_TERRAIN_HEIGHT)
                ),
                WallPlacementFilter.ashenMushroom(),
                BiomeFilter.biome()
        );
    }
}
