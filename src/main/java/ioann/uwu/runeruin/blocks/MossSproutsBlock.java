package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Grass of the mossy caves: stands only on moss blocks. */
public class MossSproutsBlock extends VegetationBlock {

    public static final MapCodec<MossSproutsBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("height").forGetter(block -> block.height),
            propertiesCodec()
    ).apply(instance, MossSproutsBlock::new));

    private final int height;
    private final VoxelShape shape;

    public MossSproutsBlock(int height, BlockBehaviour.Properties properties) {
        super(properties);
        this.height = height;
        this.shape = Block.column(12.0, 0.0, height);
    }

    @Override
    protected MapCodec<MossSproutsBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.MOSS_BLOCKS);
    }
}
