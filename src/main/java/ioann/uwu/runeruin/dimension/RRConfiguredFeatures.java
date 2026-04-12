package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.features.BoulderFeature;
import ioann.uwu.runeruin.dimension.features.CeilingBlockVineFeature;
import ioann.uwu.runeruin.dimension.features.WallMushroomFeature;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

import java.util.List;

public class RRConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_RED_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "small_red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_RED_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "big_red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "small_brown_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "big_brown_mushroom");

    public static final ResourceKey<ConfiguredFeature<?, ?>> LONG_CEILING_BLOCK_VINE = RR.resourceKey(Registries.CONFIGURED_FEATURE, "long_ceiling_block_vine");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CEILING_VINE = RR.resourceKey(Registries.CONFIGURED_FEATURE, "ceiling_vine");

    public static final ResourceKey<ConfiguredFeature<?, ?>> TUFF_MOSS_BOULDER = RR.resourceKey(Registries.CONFIGURED_FEATURE, "tuff_moss_boulder");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {

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
    }
}
