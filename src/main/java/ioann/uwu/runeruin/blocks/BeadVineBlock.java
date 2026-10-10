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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BeadVineBlock extends Block {
    public static final MapCodec<BeadVineBlock> CODEC = simpleCodec(BeadVineBlock::new);
    /** Drawn once, when the block appears: a glowing block never goes out, a dull one never lights up. */
    public static final BooleanProperty GLOWING = BooleanProperty.create("glowing");
    private static final int MAX_LENGTH = 8;
    private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 16.0);

    public BeadVineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GLOWING, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static int getLightLevel(BlockState state) {
        return state.getValue(GLOWING) ? 10 : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        return aboveState.is(this) || aboveState.isFaceSturdy(level, abovePos, Direction.DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return randomState(context.getLevel().getRandom());
    }

    private BlockState randomState(RandomSource random) {
        return this.defaultBlockState().setValue(GLOWING, random.nextBoolean());
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
        if (directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos belowPos = pos.below();
        if (!level.getGameRules().get(GameRules.SPREAD_VINES)
                || belowPos.getY() < level.getMinY()
                || !level.isEmptyBlock(belowPos)
                || random.nextInt(4) != 0) {
            return;
        }

        int length = 1;
        while (length < MAX_LENGTH && level.getBlockState(pos.above(length)).is(this)) {
            length++;
        }
        if (length < MAX_LENGTH) {
            level.setBlockAndUpdate(belowPos, randomState(random));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOWING);
    }
}
