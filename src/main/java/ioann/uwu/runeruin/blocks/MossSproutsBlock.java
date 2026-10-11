package ioann.uwu.runeruin.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Grass of the mossy caves: stands only on moss blocks and in deep moss layers. */
public class MossSproutsBlock extends MossLayerPlantBlock {

    public static final MapCodec<MossSproutsBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("height").forGetter(block -> block.height),
            propertiesCodec()
    ).apply(instance, MossSproutsBlock::new));

    private final int height;

    public MossSproutsBlock(int height, BlockBehaviour.Properties properties) {
        super(_ -> Block.column(12.0, 0.0, height), properties);
        this.height = height;
    }

    @Override
    protected MapCodec<MossSproutsBlock> codec() {
        return CODEC;
    }
}
