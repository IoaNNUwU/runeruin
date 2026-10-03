package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WaterLilyRootBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places a water lily root in the top water block below the origin and grows
 * it with the same steps as bone meal, so wild plants look player-grown.
 */
public class WaterLilyFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_LEAVES = 3;
    private static final int MAX_LEAVES = 12;

    public WaterLilyFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos root = context.origin().below();
        if (!level.getBlockState(root).is(Blocks.WATER) || !level.getFluidState(root).isSource() || !level.isEmptyBlock(context.origin())) {
            return false;
        }

        level.setBlock(root, RRBlocks.WATER_LILY_ROOT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        int leaves = random.nextIntBetweenInclusive(MIN_LEAVES, MAX_LEAVES);
        for (int step = 0; step < leaves * 4 && WaterLilyRootBlock.attachedLeaves(level, root).size() < leaves; step++) {
            WaterLilyRootBlock.grow(level, root, random);
        }
        return true;
    }
}
