package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class PowderedMossVeinFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SEARCH_RANGE = 16;

    public PowderedMossVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockState powderedMoss = RRBlocks.POWDERED_MOSS.get().defaultBlockState();
        int radiusX = 5 + random.nextInt(3);
        int radiusZ = 5 + random.nextInt(3);
        boolean placed = false;

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double distance = (double) x * x / (radiusX * radiusX) + (double) z * z / (radiusZ * radiusZ);
                if (distance > 1 || distance > 0.75 && random.nextFloat() < 0.2F) {
                    continue;
                }

                for (int y = origin.getY() + SEARCH_RANGE; y >= origin.getY() - SEARCH_RANGE; y--) {
                    BlockPos surface = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
                    if (!level.getBlockState(surface).is(Blocks.MOSS_BLOCK)) {
                        continue;
                    }

                    BlockPos lower = surface.below();
                    BlockState lowerState = level.getBlockState(lower);
                    if (!lowerState.is(Blocks.STONE) && !lowerState.is(Blocks.MOSS_BLOCK)) {
                        continue;
                    }

                    level.setBlock(surface, powderedMoss, 2);
                    level.setBlock(lower, powderedMoss, 2);
                    placed = true;
                    break;
                }
            }
        }

        return placed;
    }
}
