package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.features.CeilingVineFeature;
import ioann.uwu.runeruin.dimension.features.WallMushroomFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class RRConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_RED_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "small_red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_RED_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "big_red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "small_brown_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "big_brown_mushroom");

    public static final ResourceKey<ConfiguredFeature<?, ?>> LONG_CEILING_BLOCK_VINE = RR.resourceKey(Registries.CONFIGURED_FEATURE, "long_ceiling_block_vine");

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
                new CeilingVineFeature.Config(
                        BlockStateProvider.simple(Blocks.MOSS_BLOCK),
                        BlockStateProvider.simple(Blocks.PALE_OAK_WOOD),
                        ConstantInt.of(35)
                )
        ));
    }
}
