package ioann.uwu.runeruin.blocks;

import ioann.uwu.runeruin.dimension.RRConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GlowingMushroomBlock extends MushroomBlock {

    private static final int MAX_GROWTH_HEIGHT = 24;
    private static final VoxelShape SHAPE = Block.column(6.5, 0.0, 13.2);

    public GlowingMushroomBlock(BlockBehaviour.Properties properties) {
        super(RRConfiguredFeatures.GLOWING_MUSHROOM, properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.isInsideBuildHeight(pos.above(MAX_GROWTH_HEIGHT));
    }
}
