package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public class FloatingMossBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<FloatingMossBlock> CODEC = simpleCodec(FloatingMossBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty NORTH_EAST = BooleanProperty.create("north_east");
    public static final BooleanProperty SOUTH_EAST = BooleanProperty.create("south_east");
    public static final BooleanProperty SOUTH_WEST = BooleanProperty.create("south_west");
    public static final BooleanProperty NORTH_WEST = BooleanProperty.create("north_west");
    public static final double MOSS_INSET = 1;
    public static final double SOIL_INSET = 2;
    // The footprint is a 3x3 grid indexed [x][z]: the centre is always filled, the other cells
    // reach a connected neighbour; a corner fills only when both sides and the diagonal connect.
    private static final @Nullable BooleanProperty[][] CELLS = {
            {NORTH_WEST, WEST, SOUTH_WEST},
            {NORTH, null, SOUTH},
            {NORTH_EAST, EAST, SOUTH_EAST}};
    private final Function<BlockState, VoxelShape> shapes = getShapeForEachState(state -> {
        VoxelShape shape = Shapes.empty();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                BooleanProperty cell = cell(x, z);
                if (cell == null || state.getValue(cell)) {
                    shape = Shapes.or(shape, cellBox(x, z, MOSS_INSET, 14, 16), cellBox(x, z, SOIL_INSET, 8, 14));
                }
            }
        }
        return shape;
    }, WATERLOGGED);

    public FloatingMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false)
                .setValue(NORTH_EAST, false).setValue(SOUTH_EAST, false)
                .setValue(SOUTH_WEST, false).setValue(NORTH_WEST, false));
    }

    public static @Nullable BooleanProperty cell(int x, int z) {
        return CELLS[x][z];
    }

    /** Boundary {@code i} (0..3) of the grid cells along one axis, in model pixels. */
    public static double cellEdge(int i, double inset) {
        return switch (i) {
            case 0 -> 0;
            case 1 -> inset;
            case 2 -> 16 - inset;
            default -> 16;
        };
    }

    private static VoxelShape cellBox(int x, int z, double inset, double minY, double maxY) {
        return box(cellEdge(x, inset), minY, cellEdge(z, inset), cellEdge(x + 1, inset), maxY, cellEdge(z + 1, inset));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockGetter level = context.getLevel();
        return connect(defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER)), level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        // A diagonal change reaches us through the side neighbour whose own connection changed.
        return connect(state, level, pos);
    }

    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                BooleanProperty cell = cell(x, z);
                if (cell != null) {
                    state = state.setValue(cell, isMoss(level, pos, x - 1, 0) && isMoss(level, pos, 0, z - 1)
                            && isMoss(level, pos, x - 1, z - 1));
                }
            }
        }
        return state;
    }

    private boolean isMoss(BlockGetter level, BlockPos pos, int dx, int dz) {
        return dx == 0 && dz == 0 || level.getBlockState(pos.offset(dx, 0, dz)).is(this);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(true) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.apply(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST);
    }
}
