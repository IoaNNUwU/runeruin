package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
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
    public static final int SINK_STAGES = 16;
    private static final double MAX_SINK_PIXELS = 4;
    /** How far the moss is pressed down by creatures standing on or near it, in animation stages. */
    public static final IntegerProperty SINK = IntegerProperty.create("sink", 0, SINK_STAGES);
    // Velocity resets every stuck tick, so wading moves by a scaled single tick of air acceleration.
    private static final Vec3 COBWEB_STUCK_SPEED = new Vec3(0.25, 0, 0.25);
    private static final Vec3 WADE_STUCK_SPEED = new Vec3(2, 0, 2);
    // Cobweb sinking: one tick of gravity scaled by the cobweb's vertical multiplier.
    private static final double SINK_PER_TICK = 0.08 * 0.98 * 0.05;
    private static final double RISE_PER_TICK = 0.03;
    private static final double SINK_SPREAD = 2.6;
    private static final int SINK_TICK_DELAY = 1;
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
        return shape.move(0, -sinkPixels(state.getValue(SINK)) / 16, 0);
    }, WATERLOGGED);

    public FloatingMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false)
                .setValue(NORTH_EAST, false).setValue(SOUTH_EAST, false)
                .setValue(SOUTH_WEST, false).setValue(NORTH_WEST, false)
                .setValue(SINK, 0));
    }

    public static double sinkPixels(int stage) {
        return stage * MAX_SINK_PIXELS / SINK_STAGES;
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

    // Moss holds up only a struggling creature standing on top; anyone who stops sinks in.
    // The side simulating the movement decides, so the server never fights a player's client.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = shapes.apply(state);
        if (!(context instanceof EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof LivingEntity living)) {
            return shape;
        }
        return living.isLocalInstanceAuthoritative() && isStruggling(living)
                && context.isAbove(shape, pos, false) ? shape : Shapes.empty();
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof LivingEntity) {
            wake(level, pos);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(entity instanceof LivingEntity living) || entity.getY() < pos.getY()) {
            return;
        }
        wake(level, pos);
        if (!living.isLocalInstanceAuthoritative()) {
            return;
        }
        // Targets are derived from the start of the tick, so overlapping moss blocks agree.
        double top = pos.getY() + shapes.apply(state).max(Direction.Axis.Y);
        double y = entity.getY();
        double startY = entity.oldPosition().y;
        if (!isStruggling(living)) {
            // Explicit sinking: water would otherwise cancel the slowed-down gravity.
            entity.move(MoverType.SELF, new Vec3(0, Math.min(startY - SINK_PER_TICK, top) - y, 0));
            entity.makeStuckInBlock(state, COBWEB_STUCK_SPEED);
        } else if (y < top - 1.0E-5) {
            // A step or a jump slowly climbs back onto the moss, where the collision shape holds the walker again.
            entity.move(MoverType.SELF, new Vec3(0, Math.max(0, Math.min(top, startY + RISE_PER_TICK) - y), 0));
            entity.makeStuckInBlock(state, WADE_STUCK_SPEED);
        }
    }

    // Each step of the press animation is a block state; ticks keep it going until nobody is near.
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int target = targetSink(level, pos);
        int sink = state.getValue(SINK);
        // Covering a third of the remaining distance eases the press in and out.
        int next = sink + Integer.signum(target - sink) * Math.max(1, Math.abs(target - sink) / 3);
        if (next != sink) {
            level.setBlock(pos, state.setValue(SINK, next), UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE);
        }
        // Plants and other small things cannot stay on moss that is pressed under water.
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (next > 0 && !aboveState.isAir() && !aboveState.liquid() && !aboveState.isCollisionShapeFullBlock(level, above)) {
            level.destroyBlock(above, true);
        }
        if (target > 0) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                wake(level, pos.relative(side));
            }
        }
        if (target > 0 || next != target) {
            level.scheduleTick(pos, this, SINK_TICK_DELAY);
        }
    }

    private void wake(Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockState(pos).is(this) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, SINK_TICK_DELAY);
        }
    }

    /** The nearest creature presses this block fully at its centre and less towards the neighbours. */
    private static int targetSink(Level level, BlockPos pos) {
        Vec3 centre = Vec3.atBottomCenterOf(pos);
        AABB area = new AABB(pos).inflate(SINK_SPREAD - 0.5, 0, SINK_SPREAD - 0.5).expandTowards(0, 0.5, 0);
        double nearest = level.getEntitiesOfClass(LivingEntity.class, area, EntitySelector.NO_SPECTATORS).stream()
                .mapToDouble(entity -> entity.position().subtract(centre).horizontalDistance())
                .min().orElse(SINK_SPREAD);
        return (int) Math.round(SINK_STAGES * Math.max(0, 1 - nearest / SINK_SPREAD));
    }

    // Stepping or holding jump keeps a creature afloat, the way swimming does in water.
    private static boolean isStruggling(LivingEntity entity) {
        return entity.xxa != 0 || entity.zza != 0 || entity.isJumping();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST, SINK);
    }
}
