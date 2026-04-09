package ioann.uwu.runeruin.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.Tags;

public class WallMushFeature extends Feature<NoneFeatureConfiguration> {

    public WallMushFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();

        if (isValidMushPlacement(level, origin)) {

            level.setBlock(origin, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);

            // TODO: Don't replace stone
            for (int x = -2; x < 3; x++) {
                for (int z = -2; z < 3; z++) {
                    level.setBlock(new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z), Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(), 1);
                }
            }

            return true;
        }
        return false;
    }

    private static boolean isValidMushPlacement(WorldGenLevel level, BlockPos origin) {

        if (!level.getBlockState(origin).is(Tags.Blocks.STONES)) {
            return false;
        }

        int holeCount = 0;

        if (level.isEmptyBlock(origin.north())) {
            holeCount++;
        }
        if (level.isEmptyBlock(origin.south())) {
            holeCount++;
        }
        if (level.isEmptyBlock(origin.west())) {
            holeCount++;
        }
        if (level.isEmptyBlock(origin.east())) {
            holeCount++;
        }

        int blockCount = 0;

        if (level.getBlockState(origin.north()).is(Tags.Blocks.STONES)) {
            blockCount++;
        }
        if (level.getBlockState(origin.south()).is(Tags.Blocks.STONES)) {
            blockCount++;
        }
        if (level.getBlockState(origin.east()).is(Tags.Blocks.STONES)) {
            blockCount++;
        }
        if (level.getBlockState(origin.west()).is(Tags.Blocks.STONES)) {
            blockCount++;
        }

        return blockCount > 0 && holeCount > 0;
    }
}
