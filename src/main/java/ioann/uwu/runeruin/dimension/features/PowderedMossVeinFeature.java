package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class PowderedMossVeinFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SEARCH_RANGE = 16;
    /** The thickest floor that the moss goes through. */
    public static final int SHAFT_DEPTH = 12;

    public PowderedMossVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockState powderedMoss = RRBlocks.POWDERED_MOSS.get().defaultBlockState();
        int radiusX = 2 + random.nextInt(3);
        int radiusZ = 2 + random.nextInt(3);
        boolean placed = false;

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double distance = (double) x * x / (radiusX * radiusX) + (double) z * z / (radiusZ * radiusZ);
                if (distance > 1 || distance > 0.75 && random.nextFloat() < 0.2F) {
                    continue;
                }

                // Deep moss on stone or moss, with no moss layer on it: a moss layer needs solid ground under it.
                BlockPos surface = FeatureScans.findDown(origin.offset(x, 0, z), SEARCH_RANGE, -SEARCH_RANGE, pos -> {
                    if (!level.getBlockState(pos).is(RRBlocks.DEEP_MOSS.get()) || level.getBlockState(pos.above()).is(RRBlocks.DEEP_MOSS_LAYER.get())) {
                        return false;
                    }
                    BlockState lower = level.getBlockState(pos.below());
                    return lower.is(Blocks.STONE) || lower.is(RRBlocks.DEEP_MOSS.get());
                });
                if (surface != null) {
                    // Through a thin floor the moss goes down to the open caves under it: whoever sinks into it falls.
                    BlockPos open = FeatureScans.findDown(surface, -2, -SHAFT_DEPTH, pos -> level.getBlockState(pos).canBeReplaced());
                    int depth = open == null ? 2 : surface.getY() - open.getY();
                    for (int dy = 0; dy < depth; dy++) {
                        level.setBlock(surface.below(dy), powderedMoss, Block.UPDATE_CLIENTS);
                    }
                    placed = true;
                }
            }
        }

        return placed;
    }
}
