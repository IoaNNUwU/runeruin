package ioann.uwu.runeruin.dimension;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.features.CaveMushroomFeature;
import ioann.uwu.runeruin.dimension.features.CaveMushroomFeature.Shape;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** The big cave mushroom varieties; every floor cave biome uses exactly one. */
public enum CaveMushroomKind {
    /** Short fat stem under a thick brown dome. */
    BROWN_DOME("brown_dome", Shape.DOME,
            BlockStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK), BlockStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK),
            UniformInt.of(4, 6), UniformInt.of(3, 4), UniformInt.of(7, 9), UniformInt.of(1, 2), 0),
    /** Tall twisting stem flaring into a trumpet. */
    TRUMPET("trumpet", Shape.FUNNEL,
            BlockStateProvider.simple(Blocks.JUNGLE_PLANKS), BlockStateProvider.simple(Blocks.JUNGLE_PLANKS),
            UniformInt.of(12, 16), ConstantInt.of(2), UniformInt.of(5, 6), UniformInt.of(3, 5), 3),
    /** Stem arching over into a wide red umbrella with yellow speckles. */
    RED_ARCH("red_arch", Shape.UMBRELLA,
            new WeightedStateProvider(WeightedList.<net.minecraft.world.level.block.state.BlockState>builder()
                    .add(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(), 9)
                    .add(Blocks.DYED_TERRACOTTA.pick(DyeColor.YELLOW).defaultBlockState(), 1)),
            BlockStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK),
            UniformInt.of(8, 11), UniformInt.of(2, 3), UniformInt.of(8, 9), UniformInt.of(6, 9), 0),
    /** Slender twisting stem under a drooping pink umbrella. */
    PINK_TWIST("pink_twist", Shape.UMBRELLA,
            BlockStateProvider.simple(Blocks.CHERRY_PLANKS), BlockStateProvider.simple(Blocks.CHERRY_PLANKS),
            UniformInt.of(12, 15), ConstantInt.of(2), UniformInt.of(6, 7), UniformInt.of(3, 5), 3),
    /** Leaning thick stem under a flat yellow-rimmed hat. */
    YELLOW_HAT("yellow_hat", Shape.DISC,
            BlockStateProvider.simple(Blocks.BROWN_MUSHROOM_BLOCK), BlockStateProvider.simple(Blocks.DYED_TERRACOTTA.pick(DyeColor.YELLOW)),
            UniformInt.of(8, 12), ConstantInt.of(3), UniformInt.of(6, 7), UniformInt.of(4, 6), 0);

    /** Everything a kind is built from; used to keep new mushrooms from growing on top of old ones. */
    public static final Block[] BLOCKS = {
            Blocks.MUSHROOM_STEM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK,
            Blocks.JUNGLE_PLANKS, Blocks.CHERRY_PLANKS, Blocks.DYED_TERRACOTTA.pick(DyeColor.YELLOW)
    };

    private final String id;
    private final Shape shape;
    private final BlockStateProvider cap;
    private final BlockStateProvider rim;
    private final IntProvider height;
    private final IntProvider stemRadius;
    private final IntProvider capRadius;
    private final IntProvider bend;
    private final int sway;

    CaveMushroomKind(String id, Shape shape, BlockStateProvider cap, BlockStateProvider rim, IntProvider height,
                     IntProvider stemRadius, IntProvider capRadius, IntProvider bend, int sway) {
        this.id = id;
        this.shape = shape;
        this.cap = cap;
        this.rim = rim;
        this.height = height;
        this.stemRadius = stemRadius;
        this.capRadius = capRadius;
        this.bend = bend;
        this.sway = sway;
    }

    public CaveMushroomFeature.Config config() {
        return new CaveMushroomFeature.Config(BlockStateProvider.simple(Blocks.MUSHROOM_STEM), this.cap, this.rim,
                this.shape, this.height, this.stemRadius, this.capRadius, this.bend, this.sway);
    }

    public ResourceKey<ConfiguredFeature<?, ?>> configuredKey() {
        return RR.resourceKey(Registries.CONFIGURED_FEATURE, featureName());
    }

    public ResourceKey<PlacedFeature> placedKey() {
        return RR.resourceKey(Registries.PLACED_FEATURE, featureName());
    }

    private String featureName() {
        return this.id + "_mushroom";
    }
}
