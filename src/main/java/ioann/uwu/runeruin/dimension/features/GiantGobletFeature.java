package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.GeometryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.Structure;

public class GiantGobletFeature extends Feature<GiantGobletFeature.Config> {

    public GiantGobletFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {

        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        BlockState placeOn = config.placeOn.getState(level, random, origin);
        BlockState trunkBlock = config.trunkBlock.getState(level, random, origin);
        BlockState budBlock = config.budBlock.getState(level, random, origin);

        float maxHeight = config.maxHeight.sample(random);
        int height = (int) (maxHeight * 2/3 + (maxHeight / 3) * random.nextFloat());

        int baseBudRadius = height / 2;
        int budRadius = (int) (baseBudRadius + baseBudRadius * (random.nextFloat() / 5));
        int trunkRadius = budRadius / 4;

        if (!isValidPlacement(level, origin, placeOn, trunkBlock, height)) {
            return false;
        }

        GeometryUtils.cyl(level, origin, trunkBlock, trunkRadius, height);

        BlockPos budOrigin = origin.above(height);

        GeometryUtils.cyl(level, budOrigin, budBlock, budRadius / 3, 1);
        GeometryUtils.cyl(level, budOrigin.above(), budBlock, budRadius / 2, 1);
        GeometryUtils.cyl(level, budOrigin.above(2), budBlock, budRadius - 1, 1);
        GeometryUtils.cyl(level, budOrigin.above(3), budBlock, budRadius, 3);

        return true;
    }

    private static boolean isValidPlacement(WorldGenLevel level, BlockPos origin, BlockState ceilingBlock, BlockState trunkBlock, int height) {
        if (!level.getBlockState(origin).is(ceilingBlock.getBlock())) {
            return false;
        }

        return true;
    }

    public record Config(
            BlockStateProvider placeOn,
            BlockStateProvider trunkBlock,
            BlockStateProvider budBlock,
            IntProvider maxHeight
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("place_on").forGetter(Config::placeOn),
                        BlockStateProvider.CODEC.fieldOf("trunk_block").forGetter(Config::trunkBlock),
                        BlockStateProvider.CODEC.fieldOf("bud_block").forGetter(Config::budBlock),
                        IntProviders.codec(5, Const.BLOOMING_CAVES_CEILING_Y - Const.BLOOMING_CAVES_Y)
                                .fieldOf("max_height").forGetter(Config::maxHeight)
                ).apply(codec, Config::new));
    }
}
