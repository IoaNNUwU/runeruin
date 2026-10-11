package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Deep moss stacked like snow layers; it does not melt and has none of the snow tags. */
public class DeepMossLayerBlock extends SnowLayerBlock {
    public static final MapCodec<SnowLayerBlock> CODEC = simpleCodec(DeepMossLayerBlock::new);

    public DeepMossLayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SnowLayerBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        return Block.isFaceFull(belowState.getCollisionShape(level, pos.below()), Direction.UP)
                || belowState.is(this) && belowState.getValue(LAYERS) == MAX_HEIGHT;
    }

    /** Unlike snow, a single layer gives way only to another layer of moss or to a plant that grows in it. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        return (stack.is(this.asItem()) || Block.byItem(stack.getItem()) instanceof MossLayerPlantBlock)
                && state.getValue(LAYERS) < MAX_HEIGHT
                && (!context.replacingClickedOnBlock() || context.getClickedFace() == Direction.UP);
    }

    /** A layer placed on a plant that grows in moss goes under it. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        return state.getBlock() instanceof MossLayerPlantBlock
                ? state.setValue(MossLayerPlantBlock.MOSS, state.getValue(MossLayerPlantBlock.MOSS) + 1)
                : super.getStateForPlacement(context);
    }
}
