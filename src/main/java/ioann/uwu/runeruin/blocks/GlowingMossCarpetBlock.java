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
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(GlowingMossBlock.MIN_LIGHT, GlowingMossBlock.PLACEMENT_LIGHT_MIN)
                .setValue(GlowingMossBlock.LIGHT, GlowingMossBlock.PLACEMENT_LIGHT_MIN)
                .setValue(GlowingMossBlock.TARGET_LIGHT_LEVEL, GlowingMossBlock.PLACEMENT_LIGHT_MIN));
    }

    @Override
    public MapCodec<? extends CarpetBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GlowingMossBlock.MIN_LIGHT, GlowingMossBlock.LIGHT, GlowingMossBlock.TARGET_LIGHT_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return GlowingMossBlock.stateForPlacement(this.defaultBlockState(), context.getLevel().getRandom());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GlowingMossBlock.tickLight(state, level, pos, random);
    }
}
