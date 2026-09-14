package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GlowingMushroomPatchFeature extends Feature<NoneFeatureConfiguration> {

    private static final int PATCH_RADIUS = 2;

    public GlowingMushroomPatchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockState mushroom = RRBlocks.GLOWING_MUSHROOM.get().defaultBlockState();
        int attempts = 4 + random.nextInt(4);
        boolean placed = false;

        for (int i = 0; i < attempts; i++) {
            BlockPos column = origin.offset(
                    random.nextInt(PATCH_RADIUS * 2 + 1) - PATCH_RADIUS,
                    0,
                    random.nextInt(PATCH_RADIUS * 2 + 1) - PATCH_RADIUS
            );
            for (int y = 2; y >= -2; y--) {
                BlockPos pos = column.offset(0, y, 0);
                BlockPos support = pos.below();
                if (level.getBlockState(pos).isAir()
                        && level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
                        && mushroom.canSurvive(level, pos)) {
                    level.setBlock(pos, mushroom, 2);
                    placed = true;
                    break;
                }
            }
        }
        return placed;
    }
}
