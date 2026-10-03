package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static ioann.uwu.runeruin.blocks.WaterLilyLeafBlock.MAX_HEIGHT;
import static ioann.uwu.runeruin.blocks.WaterLilyLeafBlock.REACH;

/**
 * The underwater rhizome of a water lily. Its leaves and flowers are separate
 * blocks above the water; the root's model draws the stems up to them.
 *
 * <p>The plant only grows with bone meal, one leaf per step. Its reach widens
 * every few leaves, so the first leaves stay close to the root while later
 * ones fill out the rest of the plant.</p>
 */
public class WaterLilyRootBlock extends Block implements SimpleWaterloggedBlock, BonemealableBlock {
    public static final MapCodec<WaterLilyRootBlock> CODEC = simpleCodec(WaterLilyRootBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final int MAX_LEAVES = 15;
    private static final int FLOWER_MIN_LEAVES = 6;
    private static final int FLOWER_CHANCE = 4;
    private static final int SPOT_ATTEMPTS = 32;
    private static final VoxelShape SHAPE = Block.box(5.0, 9.0, 5.0, 11.0, 13.0, 11.0);

    public WaterLilyRootBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, true));
    }

    /** Leaves and flowers whose stems lead to the root at {@code root}. */
    public static List<BlockPos> attachedLeaves(BlockGetter level, BlockPos root) {
        return BlockPos.betweenClosedStream(root.offset(-REACH, 1, -REACH), root.offset(REACH, MAX_HEIGHT, REACH))
                .filter(pos -> WaterLilyLeafBlock.rootOf(pos, level.getBlockState(pos)).filter(root::equals).isPresent())
                .map(BlockPos::immutable)
                .toList();
    }

    /** Runs one bone meal growth step; returns whether the plant changed. */
    public static boolean grow(LevelAccessor level, BlockPos root, RandomSource random) {
        List<BlockPos> leaves = attachedLeaves(level, root);
        if (leaves.size() >= MAX_LEAVES) {
            return false;
        }

        boolean flower = leaves.size() >= FLOWER_MIN_LEAVES && random.nextInt(FLOWER_CHANCE) == 0;
        BlockPos target = findSpot(level, root, leaves, Math.min(1 + leaves.size() / 3, REACH), flower, random);
        if (target == null) {
            return false;
        }

        Block block = flower ? RRBlocks.WATER_LILY_FLOWER.get() : RRBlocks.WATER_LILY_LEAF.get();
        BlockState state = block.defaultBlockState().setValue(WaterLilyLeafBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        level.setBlock(target, WaterLilyLeafBlock.attach(state, target, root), Block.UPDATE_ALL);
        return true;
    }

    /**
     * Picks a free spot inside a hemisphere over the root. Half of the leaves
     * and every flower rise to the dome; the rest float on the water or fill
     * the inside, so the plant fills out at every height.
     */
    private static @Nullable BlockPos findSpot(LevelAccessor level, BlockPos root, List<BlockPos> leaves, int reach, boolean flower, RandomSource random) {
        double radius = reach + 0.5;
        for (int attempt = 0; attempt < SPOT_ATTEMPTS; attempt++) {
            int dx = random.nextIntBetweenInclusive(-reach, reach);
            int dz = random.nextIntBetweenInclusive(-reach, reach);
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq > radius * radius) {
                continue;
            }

            int dome = 1 + (int) Math.sqrt(radius * radius - distanceSq);
            int dy = switch (flower ? 0 : random.nextInt(4)) {
                case 1 -> 1;
                case 2 -> random.nextIntBetweenInclusive(1, dome);
                default -> dome;
            };
            BlockPos pos = root.offset(dx, dy, dz);
            // A stem rises vertically above the root and right below its leaf;
            // keep other leaves out of those spots so no stem pierces a leaf.
            if (level.isEmptyBlock(pos) && leaves.stream().noneMatch(leaf -> leaf.getX() == pos.getX() && leaf.getZ() == pos.getZ()
                    && (distanceSq == 0 || Math.abs(leaf.getY() - pos.getY()) < 2))) {
                return pos;
            }
        }
        return null;
    }

    @Override
    protected MapCodec<WaterLilyRootBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return fluid.is(Fluids.WATER) && fluid.isSource() ? this.defaultBlockState() : null;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return attachedLeaves(level, pos).size() < MAX_LEAVES;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, pos, random);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        for (BlockPos leaf : attachedLeaves(level, pos)) {
            level.destroyBlock(leaf, true);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }
}
