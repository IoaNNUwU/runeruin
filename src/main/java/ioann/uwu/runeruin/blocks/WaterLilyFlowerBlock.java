package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A water lily bloom lying on its own pad; it attaches to a root exactly like a leaf. */
public class WaterLilyFlowerBlock extends WaterLilyLeafBlock {
    public static final MapCodec<WaterLilyFlowerBlock> CODEC = simpleCodec(WaterLilyFlowerBlock::new);

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 2.5, 15.0);

    public WaterLilyFlowerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WaterLilyFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
