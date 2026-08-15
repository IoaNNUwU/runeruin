package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class GlowingMossCarpetBlock extends CarpetBlock {

    public static final MapCodec<GlowingMossCarpetBlock> CODEC = simpleCodec(GlowingMossCarpetBlock::new);

    public GlowingMossCarpetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GlowingMossBlock.LIGHT, GlowingMossBlock.MIN_LIGHT));
    }

    @Override
    public MapCodec<? extends CarpetBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GlowingMossBlock.LIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(GlowingMossBlock.LIGHT, GlowingMossBlock.randomLight(context.getLevel().getRandom()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GlowingMossBlock.tickLight(state, level, pos, random);
    }
}
