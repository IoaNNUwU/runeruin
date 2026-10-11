package ioann.uwu.runeruin.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plant of the mossy caves that can stand inside a deep moss layer. It remembers the layers under it, as a
 * waterlogged block remembers its water: the moss holds like a layer of that height, the plant stands on top
 * of it, and a player who breaks the plant leaves the layer.
 */
public abstract class MossLayerPlantBlock extends VegetationBlock {

    /** A layer of full height is a block of its own: a plant stands on it, not in it. */
    public static final int MAX_MOSS = DeepMossLayerBlock.MAX_HEIGHT - 1;
    public static final IntegerProperty MOSS = IntegerProperty.create("moss", 0, MAX_MOSS);

    private final VoxelShape[] shapes;

    protected MossLayerPlantBlock(int height, Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(MOSS, 0));
        this.shapes = Block.boxes(MAX_MOSS, moss -> Shapes.or(
                Block.column(16.0, 0.0, moss * 2),
                Block.column(12.0, moss * 2, moss * 2 + height)));
    }

    /** The deep moss layer a plant stands in; only for a plant that has moss. */
    public static BlockState mossLayer(BlockState state) {
        return RRBlocks.DEEP_MOSS_LAYER.get().defaultBlockState().setValue(DeepMossLayerBlock.LAYERS, state.getValue(MOSS));
    }

    /** A deep moss layer that a plant can go into: any but the full one. */
    public static boolean takesPlant(BlockState state) {
        return state.is(RRBlocks.DEEP_MOSS_LAYER) && state.getValue(DeepMossLayerBlock.LAYERS) <= MAX_MOSS;
    }

    /** The plant as it stands in the place of {@code replaced}: inside a deep moss layer, or without moss. */
    public static BlockState inPlaceOf(BlockState plant, BlockState replaced) {
        return plant.setValue(MOSS, takesPlant(replaced) ? replaced.getValue(DeepMossLayerBlock.LAYERS) : 0);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(MOSS)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(MOSS) == 0 ? Shapes.empty() : mossLayer(state).getCollisionShape(level, pos, context);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return state.getValue(MOSS) == 0 ? super.isPathfindable(state, type) : mossLayer(state).isPathfindable(type);
    }

    /** Without moss of its own: on any moss block, deep moss among them, and on a deep moss layer of full height. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.MOSS_BLOCKS)
                || state.is(RRBlocks.DEEP_MOSS_LAYER) && state.getValue(DeepMossLayerBlock.LAYERS) == DeepMossLayerBlock.MAX_HEIGHT;
    }

    /** With moss, the plant lies on what its layer can lie on. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(MOSS) == 0 ? super.canSurvive(state, level, pos) : mossLayer(state).canSurvive(level, pos);
    }

    /** A deep moss layer goes under the plant; anything else replaces only a plant without moss. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().is(RRBlocks.DEEP_MOSS_LAYER.asItem())
                ? state.getValue(MOSS) < MAX_MOSS
                : state.getValue(MOSS) == 0 && super.canBeReplaced(state, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return inPlaceOf(this.defaultBlockState(), context.getLevel().getBlockState(context.getClickedPos()));
    }

    /** A player breaks only the plant: its moss stays as a layer. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        return state.getValue(MOSS) == 0
                ? super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid)
                : level.setBlock(pos, mossLayer(state), level.isClientSide() ? Block.UPDATE_ALL_IMMEDIATE : Block.UPDATE_ALL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOSS);
    }
}
