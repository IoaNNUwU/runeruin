package ioann.uwu.runeruin.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A rail on which a minecart keeps its speed ({@code RunicRailMixin}) and which lets two tracks cross. */
public class RunicRailBlock extends RailBlock {

    public RunicRailBlock(Properties properties) {
        super(properties);
    }

    /** Where rails come from all four sides, a minecart goes straight on, whichever way this rail lies. */
    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter level, BlockPos pos, @Nullable AbstractMinecart cart) {
        RailShape shape = state.getValue(SHAPE);
        if (cart == null || shape.isSlope()) {
            return shape;
        }
        Vec3 movement = cart.getDeltaMovement();
        if (movement.x == 0 && movement.z == 0 || !isCrossing(level, pos)) {
            return shape;
        }
        return Math.abs(movement.x) > Math.abs(movement.z) ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    private static boolean isCrossing(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(direction)).is(BlockTags.RAILS)) {
                return false;
            }
        }
        return true;
    }
}
