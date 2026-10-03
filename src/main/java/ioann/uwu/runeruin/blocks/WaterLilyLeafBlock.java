package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Comparator;
import java.util.Optional;

/**
 * A water lily leaf held above the water by a stem. The flower is the same
 * block with a bloom in its model.
 *
 * <p>The stem is not a block: the {@link WaterLilyRootBlock} model draws it.
 * The leaf stores where it sits relative to its root, so every leaf has one
 * unambiguous owner. {@code ROOT_Y == 0} means a free leaf without a stem.</p>
 */
public class WaterLilyLeafBlock extends Block {
    public static final MapCodec<WaterLilyLeafBlock> CODEC = simpleCodec(WaterLilyLeafBlock::new);
    public static final int REACH = 3;
    public static final int MAX_HEIGHT = 4;
    public static final IntegerProperty ROOT_X = IntegerProperty.create("root_x", 0, 2 * REACH);
    public static final IntegerProperty ROOT_Y = IntegerProperty.create("root_y", 0, MAX_HEIGHT);
    public static final IntegerProperty ROOT_Z = IntegerProperty.create("root_z", 0, 2 * REACH);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);

    public WaterLilyLeafBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ROOT_X, REACH).setValue(ROOT_Y, 0).setValue(ROOT_Z, REACH)
                .setValue(FACING, Direction.NORTH));
    }

    /** Returns the root position the given state points to, if it is an attached leaf. */
    public static Optional<BlockPos> rootOf(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof WaterLilyLeafBlock) || state.getValue(ROOT_Y) == 0) {
            return Optional.empty();
        }
        return Optional.of(pos.offset(
                REACH - state.getValue(ROOT_X),
                -state.getValue(ROOT_Y),
                REACH - state.getValue(ROOT_Z)
        ));
    }

    /** Returns the state of a leaf at {@code pos} attached to the root at {@code root}. */
    public static BlockState attach(BlockState state, BlockPos pos, BlockPos root) {
        return state
                .setValue(ROOT_X, REACH + pos.getX() - root.getX())
                .setValue(ROOT_Y, pos.getY() - root.getY())
                .setValue(ROOT_Z, REACH + pos.getZ() - root.getZ());
    }

    @Override
    protected MapCodec<? extends WaterLilyLeafBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        return BlockPos.betweenClosedStream(pos.offset(-REACH, -MAX_HEIGHT, -REACH), pos.offset(REACH, -1, REACH))
                .filter(candidate -> context.getLevel().getBlockState(candidate).getBlock() instanceof WaterLilyRootBlock)
                .min(Comparator.comparingDouble(candidate -> candidate.distSqr(pos)))
                .map(root -> attach(state, pos, root))
                .orElse(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROOT_X, ROOT_Y, ROOT_Z, FACING);
    }
}
