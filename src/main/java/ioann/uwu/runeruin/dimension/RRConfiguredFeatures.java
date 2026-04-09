package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class RRConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_RED_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "small_red_mushroom_configured");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_RED_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "big_red_mushroom_configured");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "small_brown_mushroom_configured");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIG_BROWN_WALL_MUSHROOM = RR.resourceKey(Registries.CONFIGURED_FEATURE, "big_brown_mushroom_configured");

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
                        BlockStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK),
                        ConstantInt.of(7)
                )
        ));
        ctx.register(SMALL_BROWN_WALL_MUSHROOM, new ConfiguredFeature<>(
                RRFeatures.WALL_MUSHROOM.get(),
                new WallMushroomFeature.Config(
                        BlockStateProvider.simple(Blocks.RED_MUSHROOM_BLOCK),
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
    }
}
