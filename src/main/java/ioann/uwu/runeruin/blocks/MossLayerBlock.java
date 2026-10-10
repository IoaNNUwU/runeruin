package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Moss stacked like snow layers; small spiky plants grow on top of it, bone meal adds more. */
public class MossLayerBlock extends SnowLayerBlock implements BonemealableBlock {
    public static final MapCodec<SnowLayerBlock> CODEC = simpleCodec(MossLayerBlock::new);
    public static final int MAX_PLANTS = 7;
    public static final IntegerProperty PLANTS = IntegerProperty.create("plants", 0, MAX_PLANTS);

    public MossLayerBlock(Properties properties) {
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

    /** Unlike snow, a single layer gives way only to another layer of moss. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().is(this.asItem())
                && state.getValue(LAYERS) < MAX_HEIGHT
                && (!context.replacingClickedOnBlock() || context.getClickedFace() == Direction.UP);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockState nearState = level.getBlockState(near);
            if (nearState.is(this) && nearState.getValue(PLANTS) < MAX_PLANTS) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            BlockState nearState = level.getBlockState(near);
            if (nearState.is(this)) {
                // One or two on the block itself, at most one on each moss layer around it.
                int grown = random.nextInt(2) + (near.equals(pos) ? 1 : 0);
                level.setBlock(near, nearState.setValue(PLANTS, Math.min(MAX_PLANTS, nearState.getValue(PLANTS) + grown)), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLANTS);
    }
}
