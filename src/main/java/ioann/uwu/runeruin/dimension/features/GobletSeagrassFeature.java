package ioann.uwu.runeruin.dimension.features;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;

/** Places vanilla seagrass at the exact cup-floor position, without vanilla's horizontal scatter. */
public class GobletSeagrassFeature extends Feature<ProbabilityFeatureConfiguration> {

    public GobletSeagrassFeature() {
        super(ProbabilityFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<ProbabilityFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos pos = context.origin();
        if (!level.getBlockState(pos).is(Blocks.WATER)) {
            return false;
        }

        boolean isTall = random.nextDouble() < context.config().probability;
        BlockState state = isTall ? Blocks.TALL_SEAGRASS.defaultBlockState() : Blocks.SEAGRASS.defaultBlockState();
        if (!state.canSurvive(level, pos)) {
            return false;
        }

        if (isTall) {
            BlockPos above = pos.above();
            if (!level.getBlockState(above).is(Blocks.WATER)) {
                return false;
            }
            level.setBlock(pos, state, 2);
            level.setBlock(above, state.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER), 2);
        } else {
            level.setBlock(pos, state, 2);
        }
        return true;
    }
}
