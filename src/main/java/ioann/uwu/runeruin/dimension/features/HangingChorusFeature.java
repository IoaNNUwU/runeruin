package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.HangingChorusFlowerBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A chorus tree hanging from void stone; the origin is the air block under the stone. */
public class HangingChorusFeature extends Feature<NoneFeatureConfiguration> {

    // The tree stays within the chunks a feature may write to.
    private static final int MAX_HORIZONTAL_SPREAD = 8;

    public HangingChorusFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        if (!level.isEmptyBlock(origin) || !level.getBlockState(origin.above()).is(RRBlocks.VOID_STONE)) {
            return false;
        }
        HangingChorusFlowerBlock.generatePlant(level, origin, ctx.random(), MAX_HORIZONTAL_SPREAD);
        return true;
    }
}
