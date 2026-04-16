package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.MossBerryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.features.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

import java.util.List;

public class RRConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_RED_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "small_red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_RED_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "big_red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "small_brown_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "big_brown_mushroom");

    public static final ResourceKey<ConfiguredFeature<?, ?>> CEILING_VINE = RR.resourceKey(Registries.CONFIGURED_FEATURE, "ceiling_vine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LONG_CEILING_BLOCK_VINE = RR.resourceKey(Registries.CONFIGURED_FEATURE, "long_ceiling_block_vine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CEILING_BALL = RR.resourceKey(Registries.CONFIGURED_FEATURE, "ceiling_ball");

    public static final ResourceKey<ConfiguredFeature<?, ?>> TUFF_MOSS_BOULDER = RR.resourceKey(Registries.CONFIGURED_FEATURE, "tuff_moss_boulder");

    public static final ResourceKey<ConfiguredFeature<?, ?>> MONOLITH = RR.resourceKey(Registries.CONFIGURED_FEATURE, "monolith");

    public static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_POOL_WITH_DRIPLEAVES = RR.resourceKey(Registries.CONFIGURED_FEATURE, "moss_pool_with_dripleaves");
    public static final ResourceKey<ConfiguredFeature<?, ?>> STONE_LILY = RR.resourceKey(Registries.CONFIGURED_FEATURE, "stone_lily");

    public static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_BERRY_BUSH_PATCH = RR.resourceKey(Registries.CONFIGURED_FEATURE, "moss_berry_bush_patch");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {

        var otherConfiguredFeatures = ctx.lookup(Registries.CONFIGURED_FEATURE);

        ctx.register(SMALL_RED_WALL_MUSHROOM, new ConfiguredFeature<>(
                RRFeatures.WALL_MUSHROOM.get(),
                new WallMushroomFeature.Config(
                        BlockStateProvider.simple(Blocks.RED_MUSHROOM_BLOCK),
                        ConstantInt.of(5)
                )
        ));
        ctx.register(BIG_RED_WALL_MUSHROOM, new ConfiguredFeature<>(
                RRFeatures.WALL_MUSHROOM.get(),
                new WallMushroomFeature.Config(
                        BlockStateProvider.simple(Blocks.RED_MUSHROOM_BLOCK),
                        ConstantInt.of(7)
                )
        ));
        ctx.register(SMALL_BROWN_WALL_MUSHROOM, new ConfiguredFeature<>(
                RRFeatures.WALL_MUSHROOM.get(),
                new WallMushroomFeature.Config(
                        BlockStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK),
                        ConstantInt.of(5)
                )
        ));
        ctx.register(BIG_BROWN_WALL_MUSHROOM, new ConfiguredFeature<>(
                RRFeatures.WALL_MUSHROOM.get(),
                new WallMushroomFeature.Config(
                        BlockStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK),
                        ConstantInt.of(7)
                )
        ));

        ctx.register(LONG_CEILING_BLOCK_VINE, new ConfiguredFeature<>(
                RRFeatures.CEILING_BLOCK_VINE.get(),
                new CeilingBlockVineFeature.Config(
                        BlockStateProvider.simple(Blocks.MOSS_BLOCK),
                        BlockStateProvider.simple(Blocks.PALE_OAK_WOOD),
                        List.of(
                                BlockStateProvider.simple(Blocks.OCHRE_FROGLIGHT),
                                BlockStateProvider.simple(Blocks.VERDANT_FROGLIGHT),
                                BlockStateProvider.simple(Blocks.PEARLESCENT_FROGLIGHT)
                        ),
                        ConstantInt.of(35)
                )
        ));

        ctx.register(CEILING_BALL, new ConfiguredFeature<>(
                RRFeatures.CEILING_BALL.get(),
                new CeilingBallFeature.Config(
                        BlockStateProvider.simple(Blocks.PALE_OAK_WOOD),
                        BlockStateProvider.simple(Blocks.PEARLESCENT_FROGLIGHT),
                        ConstantInt.of(20),
                        ConstantInt.of(10)
                )
        ));

        RandomizedIntStateProvider caveVinesHeadProvider = new RandomizedIntStateProvider(
                new WeightedStateProvider(
                        WeightedList.<BlockState>builder()
                                .add(Blocks.CAVE_VINES.defaultBlockState(), 4)
                                .add(Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, true), 1)
                ),
                CaveVinesBlock.AGE,
                UniformInt.of(23, 25)
        );
        WeightedStateProvider caveVinesBodyProvider = new WeightedStateProvider(
                WeightedList.<BlockState>builder()
                        .add(Blocks.CAVE_VINES_PLANT.defaultBlockState(), 4)
                        .add(Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, true), 1)
        );

        ctx.register(CEILING_VINE, new ConfiguredFeature<>(
                Feature.BLOCK_COLUMN,
                new BlockColumnConfiguration(
                        List.of(
                                BlockColumnConfiguration.layer(
                                        new WeightedListInt(
                                                WeightedList.<IntProvider>builder().add(UniformInt.of(0, 19), 2)
                                                        .add(UniformInt.of(0, 2), 3)
                                                        .add(UniformInt.of(0, 6), 10)
                                                        .build()
                                        ),
                                        caveVinesBodyProvider
                                ),
                                BlockColumnConfiguration.layer(ConstantInt.of(1), caveVinesHeadProvider)
                        ),
                        Direction.DOWN,
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        true
                )
        ));

        ctx.register(TUFF_MOSS_BOULDER, new ConfiguredFeature<>(
                RRFeatures.BOULDER.get(),
                new BoulderFeature.Config(
                        BlockStateProvider.simple(Blocks.TUFF),
                        BlockStateProvider.simple(Blocks.PALE_MOSS_BLOCK),
                        ConstantInt.of(5),
                        ConstantInt.of(7)
                )
        ));

        ctx.register(MONOLITH, new ConfiguredFeature<>(
                RRFeatures.MONOLITH.get(),
                new MonolithFeature.Config(
                        BlockStateProvider.simple(RRBlocks.ARCANE_STONE.get()),
                        BlockStateProvider.simple(Blocks.DIAMOND_BLOCK.defaultBlockState()),
                        ConstantInt.of(7),
                        ConstantInt.of(13),
                        ConstantInt.of(3),
                        ConstantInt.of(7)

                )
        ));

        ctx.register(MOSS_POOL_WITH_DRIPLEAVES, new ConfiguredFeature<>(
                Feature.WATERLOGGED_VEGETATION_PATCH,
                new VegetationPatchConfiguration(
                        BlockTags.LUSH_GROUND_REPLACEABLE,
                        BlockStateProvider.simple(Blocks.MOSS_BLOCK),
                        PlacementUtils.inlinePlaced(otherConfiguredFeatures.getOrThrow(CaveFeatures.DRIPLEAF)),
                        CaveSurface.FLOOR,
                        ConstantInt.of(3),
                        0.8F,
                        5,
                        0.2F,
                        UniformInt.of(4, 7),
                        0.7F
                )
        ));

        ctx.register(STONE_LILY, new ConfiguredFeature<>(
                RRFeatures.STONE_LILY.get(),
                new StoneLilyFeature.Config(
                        BlockStateProvider.simple(Blocks.MOSSY_COBBLESTONE),
                        BlockStateProvider.simple(Blocks.MOSSY_COBBLESTONE_WALL),
                        BlockStateProvider.simple(Blocks.MOSSY_COBBLESTONE_SLAB),
                        BlockStateProvider.simple(Blocks.MOSS_CARPET)
                )
        ));

        ctx.register(MOSS_BERRY_BUSH_PATCH, new ConfiguredFeature<>(
                Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(
                        new RandomizedIntStateProvider(
                                BlockStateProvider.simple(RRBlocks.MOSS_BERRY_BUSH.get()),
                                MossBerryBushBlock.AGE,
                                new UniformInt(0, 3)
                        )
                )
        ));
    }
}
