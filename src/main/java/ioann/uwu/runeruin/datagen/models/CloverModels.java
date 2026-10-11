package ioann.uwu.runeruin.datagen.models;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import org.jspecify.annotations.NonNull;

/**
 * Clover: flat leaves at different heights, each on a crossed stem, in the four segments of a flower bed. Every
 * segment is a model file ({@code clover_1} ... {@code clover_4}): a quarter of the block texture, which is the
 * whole patch seen from above, in the order north-west, south-west, south-east, north-east.
 */
public final class CloverModels {

    private CloverModels() {}

    public static void createClover(@NonNull BlockModelGenerators blockModels) {
        blockModels.createSegmentedBlock(
                RRBlocks.CLOVER.get(),
                segment(1), BlockModelGenerators.FLOWER_BED_MODEL_1_SEGMENT_CONDITION,
                segment(2), BlockModelGenerators.FLOWER_BED_MODEL_2_SEGMENT_CONDITION,
                segment(3), BlockModelGenerators.FLOWER_BED_MODEL_3_SEGMENT_CONDITION,
                segment(4), BlockModelGenerators.FLOWER_BED_MODEL_4_SEGMENT_CONDITION
        );
        blockModels.registerSimpleFlatItemModel(RRBlocks.CLOVER.get());
    }

    private static MultiVariant segment(int number) {
        return BlockModelGenerators.plainVariant(RR.id("block/clover_" + number));
    }
}
