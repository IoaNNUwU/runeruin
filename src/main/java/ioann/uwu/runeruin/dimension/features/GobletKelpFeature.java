package ioann.uwu.runeruin.dimension.features;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Grows kelp from the cup floor selected by GobletUnderwaterPlacement. */
public class GobletKelpFeature extends Feature<NoneFeatureConfiguration> {

    public GobletKelpFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        BlockState body = Blocks.KELP_PLANT.defaultBlockState();
        BlockState head = Blocks.KELP.defaultBlockState();
        int height = 1 + context.random().nextInt(10);
        int placed = 0;

        for (int i = 0; i <= height; i++, pos = pos.above()) {
            if (level.getBlockState(pos).is(Blocks.WATER)
                    && level.getBlockState(pos.above()).is(Blocks.WATER)
                    && body.canSurvive(level, pos)) {
                if (i == height) {
                    level.setBlock(pos, head.setValue(KelpBlock.AGE, context.random().nextInt(4) + 20), 2);
                    placed++;
                } else {
                    level.setBlock(pos, body, 2);
                }
            } else if (i > 0) {
                BlockPos top = pos.below();
                if (head.canSurvive(level, top) && !level.getBlockState(top.below()).is(Blocks.KELP)) {
                    level.setBlock(top, head.setValue(KelpBlock.AGE, context.random().nextInt(4) + 20), 2);
                    placed++;
                }
                break;
            }
        }

        return placed > 0;
    }
}
