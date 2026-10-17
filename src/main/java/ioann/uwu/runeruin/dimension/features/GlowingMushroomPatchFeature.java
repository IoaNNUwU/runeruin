package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
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
            BlockPos pos = FeatureScans.findDown(column, 2, -2, p -> level.getBlockState(p).isAir()
                    && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                    && mushroom.canSurvive(level, p));
            if (pos != null) {
                level.setBlock(pos, mushroom, Block.UPDATE_CLIENTS);
                placed = true;
            }
        }
        return placed;
    }
}
