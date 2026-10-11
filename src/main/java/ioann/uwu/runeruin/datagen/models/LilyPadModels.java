package ioann.uwu.runeruin.datagen.models;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.BigLilyPadBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WaterLilyLeafBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

/**
 * Big lily pads and the water lily (root, leaf, flower). Their models are files: a cell of a big pad
 * ({@code big_lily_pad_<part>}) is a plane with one cell of the vanilla lily pad texture stretched over the
 * whole pad, so the cells form one seamless pad.
 */
public final class LilyPadModels {

    private LilyPadModels() {}

    public static void createBigLilyPad(@NonNull BlockModelGenerators blockModels) {
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.BIG_LILY_PAD.get())
                        .with(PropertyDispatch.initial(BigLilyPadBlock.PART, BigLilyPadBlock.FACING)
                                .generate(LilyPadModels::orientedBigLilyVariant)
                        )
        );

        // The item is always the ordinary/single form. Its tint is the
        // default foliage color; the placed block is tinted from its biome.
        Identifier itemModel = blockModels.createFlatItemModelWithBlockTexture(
                RRBlocks.BIG_LILY_PAD.get().asItem(),
                Blocks.LILY_PAD
        );
        blockModels.registerSimpleTintedItemModel(
                RRBlocks.BIG_LILY_PAD.get(),
                itemModel,
                ItemModelUtils.constantTint(-12012264)
        );
    }

    public static void createWaterLily(@NonNull BlockModelGenerators blockModels) {
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(RRBlocks.WATER_LILY_ROOT.get(), BlockModelGenerators.plainVariant(RR.id("block/water_lily_root")))
        );
        // The item shows a young plant: the rhizome, short roots and five small leaves on stems.
        blockModels.registerSimpleTintedItemModel(RRBlocks.WATER_LILY_ROOT.get(), RR.id("item/water_lily_root"), ItemModelUtils.constantTint(-12012264));

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.WATER_LILY_LEAF.get())
                        .with(PropertyDispatch.initial(WaterLilyLeafBlock.FACING)
                                .generate(facing -> orientedBigLilyVariant(BigLilyPadBlock.Part.SINGLE, facing)))
        );
        blockModels.registerSimpleTintedItemModel(
                RRBlocks.WATER_LILY_LEAF.get(),
                blockModels.createFlatItemModelWithBlockTexture(RRBlocks.WATER_LILY_LEAF.get().asItem(), Blocks.LILY_PAD),
                ItemModelUtils.constantTint(-12012264)
        );

        // The flower lies flat just above its own pad.
        Identifier flowerModelId = RR.id("block/water_lily_flower");
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.WATER_LILY_FLOWER.get())
                        .with(PropertyDispatch.initial(WaterLilyLeafBlock.FACING)
                                .generate(facing -> rotatedTo(BlockModelGenerators.plainVariant(flowerModelId), facing)))
        );
        blockModels.registerSimpleItemModel(
                RRBlocks.WATER_LILY_FLOWER.get(),
                blockModels.createFlatItemModelWithBlockTexture(RRBlocks.WATER_LILY_FLOWER.get().asItem(), RRBlocks.WATER_LILY_FLOWER.get())
        );
    }

    private static Identifier modelFor(BigLilyPadBlock.Part part) {
        return part == BigLilyPadBlock.Part.SINGLE
                ? ModelLocationUtils.getModelLocation(Blocks.LILY_PAD)
                : RR.id("block/big_lily_pad_" + part.getSerializedName());
    }

    private static MultiVariant orientedBigLilyVariant(
            BigLilyPadBlock.Part targetPart,
            Direction facing
    ) {
        // Rotating a cell model alone rotates its texture inside the same
        // world cell. Select the inverse-rotated source cell first so that
        // the complete 2x2/3x3 texture rotates as one connected pad.
        BigLilyPadBlock.Part sourcePart = sourcePartFor(targetPart, facing);
        return rotatedTo(BlockModelGenerators.plainVariant(modelFor(sourcePart)), facing);
    }

    private static MultiVariant rotatedTo(MultiVariant variant, Direction facing) {
        return switch (facing) {
            case NORTH -> variant;
            case EAST -> variant.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> variant.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> variant.with(BlockModelGenerators.Y_ROT_270);
            default -> throw new IllegalArgumentException("Lily pad facing must be horizontal");
        };
    }

    private static BigLilyPadBlock.Part sourcePartFor(
            BigLilyPadBlock.Part targetPart,
            Direction facing
    ) {
        int size = targetPart.gridSize();
        int sourceX;
        int sourceZ;
        switch (facing) {
            case NORTH -> {
                sourceX = targetPart.x();
                sourceZ = targetPart.z();
            }
            case EAST -> {
                sourceX = targetPart.z();
                sourceZ = size - 1 - targetPart.x();
            }
            case SOUTH -> {
                sourceX = size - 1 - targetPart.x();
                sourceZ = size - 1 - targetPart.z();
            }
            case WEST -> {
                sourceX = size - 1 - targetPart.z();
                sourceZ = targetPart.x();
            }
            default -> throw new IllegalArgumentException("Big lily pad facing must be horizontal");
        }

        for (BigLilyPadBlock.Part candidate : BigLilyPadBlock.Part.values()) {
            if (candidate.gridSize() == size && candidate.x() == sourceX && candidate.z() == sourceZ) {
                return candidate;
            }
        }
        throw new IllegalStateException("No big lily pad part at " + size + "x" + size + " coordinates " + sourceX + "," + sourceZ);
    }
}
