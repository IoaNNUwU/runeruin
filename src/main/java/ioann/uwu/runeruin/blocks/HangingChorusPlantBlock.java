package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

/** The vanilla chorus plant turned upside down: it hangs from void stone instead of standing on end stone. */
public class HangingChorusPlantBlock extends PipeBlock {
    public static final MapCodec<HangingChorusPlantBlock> CODEC = simpleCodec(HangingChorusPlantBlock::new);

    public HangingChorusPlantBlock(Properties properties) {
        super(10.0F, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<HangingChorusPlantBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return stateWithConnections(context.getLevel(), context.getClickedPos());
    }

    public static BlockState stateWithConnections(BlockGetter level, BlockPos pos) {
        BlockState state = RRBlocks.HANGING_CHORUS_PLANT.get().defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                    connectsTo(level.getBlockState(pos.relative(direction)), direction));
        }
        return state;
    }

    private static boolean connectsTo(BlockState neighbour, Direction direction) {
        return neighbour.is(RRBlocks.HANGING_CHORUS_PLANT)
                || neighbour.is(RRBlocks.HANGING_CHORUS_FLOWER)
                || direction == Direction.UP && supports(neighbour);
    }

    static boolean supports(BlockState state) {
        return state.is(RRBlocks.VOID_STONE);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), connectsTo(neighbourState, directionToNeighbour));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState aboveState = level.getBlockState(pos.above());
        boolean blockAboveAndBelow = !aboveState.isAir() && !level.getBlockState(pos.below()).isAir();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbourPos = pos.relative(direction);
            if (level.getBlockState(neighbourPos).is(this)) {
                if (blockAboveAndBelow) {
                    return false;
                }

                BlockState aboveNeighbour = level.getBlockState(neighbourPos.above());
                if (aboveNeighbour.is(this) || supports(aboveNeighbour)) {
                    return true;
                }
            }
        }

        return aboveState.is(this) || supports(aboveState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
