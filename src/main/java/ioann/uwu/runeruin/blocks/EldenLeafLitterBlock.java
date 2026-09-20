package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class EldenLeafLitterBlock extends LeafLitterBlock {
    public static final MapCodec<LeafLitterBlock> CODEC = simpleCodec(EldenLeafLitterBlock::new);

    public EldenLeafLitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<LeafLitterBlock> codec() {
        return CODEC;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }
}
