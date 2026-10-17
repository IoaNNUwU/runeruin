package ioann.uwu.runeruin.dimension.features;

import ioann.uwu.runeruin.blocks.RRBlocks;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Looks at the blocks around the origin of a feature, for checks that several features share. */
final class FeatureScans {

    private static final int[][] CEILING_ANCHORS = {{0, 3, -2}, {0, 3, 2}, {2, 3, 0}, {-2, 3, 0}};
    private static final int[][] CARDINALS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    private FeatureScans() {
    }

    /** The highest position from {@code top} down to {@code bottom} blocks above {@code column} that passes the test. */
    static @Nullable BlockPos findDown(BlockPos column, int top, int bottom, Predicate<BlockPos> test) {
        for (int dy = top; dy >= bottom; dy--) {
            BlockPos pos = column.above(dy);
            if (test.test(pos)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Whether a trunk may hang from the ceiling block at {@code origin}: three blocks up the ceiling goes on
     * two blocks to each side, and no other trunk or arcane stone stands next to the origin.
     */
    static boolean isHangingTrunkAnchor(WorldGenLevel level, BlockPos origin, BlockState ceilingBlock, BlockState trunkBlock, BlockPos.MutableBlockPos mutable) {
        if (!level.getBlockState(origin).is(ceilingBlock.getBlock())) {
            return false;
        }
        for (int[] offset : CEILING_ANCHORS) {
            if (level.isEmptyBlock(mutable.setWithOffset(origin, offset[0], offset[1], offset[2]))) {
                return false;
            }
        }
        for (int[] offset : CARDINALS) {
            BlockState neighbor = level.getBlockState(mutable.setWithOffset(origin, offset[0], 0, offset[1]));
            if (neighbor.is(trunkBlock.getBlock()) || neighbor.is(RRBlocks.ARCANE_STONE)) {
                return false;
            }
        }
        return true;
    }
}
