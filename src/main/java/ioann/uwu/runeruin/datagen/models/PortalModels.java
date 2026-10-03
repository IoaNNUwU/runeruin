package ioann.uwu.runeruin.datagen.models;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.ArcaneStonePortalBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.portal.RuneRuinPortalBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.NonNull;

/** The arcane stone portal frame and the dimension portal itself. */
public final class PortalModels {

    private PortalModels() {}

    public static void createArcaneStonePortal(@NonNull BlockModelGenerators blockModels) {
        var block = RRBlocks.ARCANE_STONE_PORTAL.get();
        Identifier cornerModel = blockModels.createSuffixedVariant(
                block, "_corner", ModelTemplates.CUBE_ALL,
                _ -> TextureMapping.cube(new Material(RR.id("block/arcane_stone_portal_runes")))
        );
        Identifier columnModel = ModelTemplates.CUBE_COLUMN.createWithSuffix(
                block,
                "_column",
                TextureMapping.column(
                        new Material(RR.id("block/arcane_stone_portal_column")),
                        TextureMapping.getBlockTexture(RRBlocks.ARCANE_STONE_COLUMN.get(), "_top")
                ),
                blockModels.modelOutput
        );
        Identifier middleModel = blockModels.createSuffixedVariant(
                block, "_middle", ModelTemplates.CUBE_ALL,
                _ -> TextureMapping.cube(new Material(RR.id("block/arcane_stone_portal_middle")))
        );
        Identifier vertColumnModel = blockModels.createSuffixedVariant(
                block, "_vert_column", ModelTemplates.CUBE_ALL,
                _ -> TextureMapping.cube(new Material(RR.id("block/arcane_stone_portal_vert_column")))
        );
        Identifier middleLeftX = createPortalMiddleHalfModel(blockModels, "left", Direction.Axis.X);
        Identifier middleRightX = createPortalMiddleHalfModel(blockModels, "right", Direction.Axis.X);
        Identifier middleLeftZ = createPortalMiddleHalfModel(blockModels, "left", Direction.Axis.Z);
        Identifier middleRightZ = createPortalMiddleHalfModel(blockModels, "right", Direction.Axis.Z);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(PropertyDispatch.initial(
                                        ArcaneStonePortalBlock.PART,
                                        ArcaneStonePortalBlock.MIDDLE_SIDE,
                                        ArcaneStonePortalBlock.AXIS
                                )
                                .generate((part, middleSide, axis) -> BlockModelGenerators.plainVariant(switch (part) {
                                    case CORNER -> cornerModel;
                                    case COLUMN -> columnModel;
                                    case VERT_COLUMN -> vertColumnModel;
                                    case MIDDLE -> switch (middleSide) {
                                        case SINGLE -> middleModel;
                                        case LEFT -> axis == Direction.Axis.X ? middleLeftX : middleLeftZ;
                                        case RIGHT -> axis == Direction.Axis.X ? middleRightX : middleRightZ;
                                    };
                                })))
        );
        blockModels.registerSimpleItemModel(block, cornerModel);
    }

    public static void createRuneRuinPortal(@NonNull BlockModelGenerators blockModels) {
        // Reuse vanilla nether portal models/texture until a custom portal texture exists.
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.RUNE_RUIN_PORTAL.get())
                        .with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_AXIS, RuneRuinPortalBlock.UNSTABLE)
                                .generate((axis, unstable) -> BlockModelGenerators.plainVariant(
                                        ModelLocationUtils.getModelLocation(
                                                Blocks.NETHER_PORTAL,
                                                axis == Direction.Axis.X ? "_ns" : "_ew"
                                        )
                                ))
                        )
        );
    }

    private static Identifier createPortalMiddleHalfModel(
            @NonNull BlockModelGenerators blockModels,
            String side,
            Direction.Axis axis
    ) {
        var block = RRBlocks.ARCANE_STONE_PORTAL.get();
        Material stone = TextureMapping.getBlockTexture(RRBlocks.ARCANE_STONE.get());
        Material left = new Material(RR.id("block/arcane_stone_portal_middle_left"));
        Material right = new Material(RR.id("block/arcane_stone_portal_middle_right"));
        Material primary = side.equals("left") ? left : right;
        Material opposite = side.equals("left") ? right : left;

        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.ALL, stone)
                .put(TextureSlot.UP, stone)
                .put(TextureSlot.DOWN, stone);
        if (axis == Direction.Axis.X) {
            textures.put(TextureSlot.NORTH, primary).put(TextureSlot.SOUTH, opposite);
        } else {
            textures.put(TextureSlot.WEST, primary).put(TextureSlot.EAST, opposite);
        }

        return ModelTemplates.CUBE.createWithSuffix(
                block,
                "_middle_" + side + "_" + axis.getName(),
                textures,
                blockModels.modelOutput
        );
    }
}
