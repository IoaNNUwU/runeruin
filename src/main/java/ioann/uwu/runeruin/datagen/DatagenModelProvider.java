package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.portal.RuneRuinPortalBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.NonNull;

public class DatagenModelProvider extends ModelProvider {

    public DatagenModelProvider(PackOutput output) {
        super(output, RR.MODID);
    }

    @Override
    protected void registerModels(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {

        // --- Items ---
        itemModels.generateFlatItem(RRItems.RUNE_OF_SPACE.get(), ModelTemplates.FLAT_ITEM);

        // --- Blocks ---
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE.get());
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE_BRICKS.get());
        blockModels.createTrivialCube(RRBlocks.POLISHED_ARCANE_STONE.get());
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_PILLAR.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_COLUMN.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);

        blockModels.createTrivialCube(RRBlocks.DIAMOND_ARCANE_STONE.get());

        blockModels.createPlantWithDefaultItem(RRBlocks.ELDEN_SAPLING.get(), RRBlocks.POTTED_ELDEN_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);

        blockModels.createTrivialCube(RRBlocks.ELDEN_LEAVES.get());
        blockModels.createTrivialCube(RRBlocks.ELDEN_PLANKS.get());
        createGiantGobletBlocks(blockModels);
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ELDEN_LOG.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);

        createInvertedTreeBlocks(blockModels);

        blockModels.createTrivialCube(RRBlocks.MOSS_LIGHT.get());
        blockModels.createFullAndCarpetBlocks(RRBlocks.GLOWING_MOSS.get(), RRBlocks.GLOWING_MOSS_CARPET.get());
        blockModels.createTrivialCube(RRBlocks.LAPIS_LIGHT.get());

        createRuneRuinPortal(blockModels);
        createMossBerry(blockModels, itemModels);
    }

    private static void createGiantGobletBlocks(@NonNull BlockModelGenerators blockModels) {
        Material paleOakBark = TextureMapping.getBlockTexture(Blocks.PALE_OAK_LOG);
        blockModels.createTrivialBlock(
                RRBlocks.GIANT_GOBLET_STEM.get(),
                _ -> TexturedModel.createAllSame(paleOakBark)
        );
        Material warpedWart = TextureMapping.getBlockTexture(Blocks.WARPED_WART_BLOCK);
        blockModels.createTrivialBlock(
                RRBlocks.GIANT_GOBLET_BUD.get(),
                _ -> TexturedModel.createAllSame(warpedWart)
        );
    }

    private static void createInvertedTreeBlocks(@NonNull BlockModelGenerators blockModels) {
        // Temporary textures: keep the custom block IDs while reusing vanilla assets.
        Material cherryLeaves = TextureMapping.getBlockTexture(Blocks.CHERRY_LEAVES);
        blockModels.createTrivialBlock(
                RRBlocks.INVERTED_LEAVES_1.get(),
                _ -> new TexturedModel(TextureMapping.cube(cherryLeaves), ModelTemplates.LEAVES)
        );

        Material pinkGlazedTerracotta = TextureMapping.getBlockTexture(Blocks.GLAZED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.PINK));
        blockModels.createTrivialBlock(
                RRBlocks.INVERTED_LEAVES_2.get(),
                _ -> TexturedModel.createAllSame(pinkGlazedTerracotta)
        );

        blockModels.woodProvider(Blocks.PALE_OAK_LOG).wood(RRBlocks.INVERTED_TREE_WOOD.get());

        BlockFamily blockFamily = new BlockFamily.Builder(RRBlocks.INVERTED_TREE_PLANKS.get())
                .button(RRBlocks.INVERTED_TREE_BUTTON.get())
                .fence(RRBlocks.INVERTED_TREE_FENCE.get())
                .fenceGate(RRBlocks.INVERTED_TREE_FENCE_GATE.get())
                .pressurePlate(RRBlocks.INVERTED_TREE_PRESSURE_PLATE.get())
                .slab(RRBlocks.INVERTED_TREE_SLAB.get())
                .stairs(RRBlocks.INVERTED_TREE_STAIRS.get())
                .getFamily();

        blockModels.familyWithExistingFullBlock(Blocks.PALE_OAK_PLANKS)
                .fullBlock(RRBlocks.INVERTED_TREE_PLANKS.get(), ModelTemplates.CUBE_ALL)
                .generateFor(blockFamily);

        // Reuse the complete vanilla model/item definitions, including their textures.
        createInvertedTreeDoorModels(blockModels);
        createInvertedTreeTrapdoorModels(blockModels);

        createInvertedTreeSignModels(blockModels);
    }

    private static void createInvertedTreeDoorModels(@NonNull BlockModelGenerators blockModels) {
        MultiVariant bottomLeft = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_BOTTOM_LEFT.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant bottomLeftOpen = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant bottomRight = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_BOTTOM_RIGHT.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant bottomRightOpen = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant topLeft = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_TOP_LEFT.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant topLeftOpen = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_TOP_LEFT_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant topRight = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_TOP_RIGHT.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant topRightOpen = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_TOP_RIGHT_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );

        blockModels.blockStateOutput.accept(BlockModelGenerators.createDoor(
                RRBlocks.INVERTED_TREE_DOOR.get(),
                bottomLeft,
                bottomLeftOpen,
                bottomRight,
                bottomRightOpen,
                topLeft,
                topLeftOpen,
                topRight,
                topRightOpen
        ));
        blockModels.registerSimpleItemModel(
                RRBlocks.INVERTED_TREE_DOOR.get(),
                ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_DOOR.asItem())
        );
    }

    private static void createInvertedTreeTrapdoorModels(@NonNull BlockModelGenerators blockModels) {
        MultiVariant top = BlockModelGenerators.plainVariant(
                ModelTemplates.TRAPDOOR_TOP.getDefaultModelLocation(Blocks.PALE_OAK_TRAPDOOR)
        );
        MultiVariant bottom = BlockModelGenerators.plainVariant(
                ModelTemplates.TRAPDOOR_BOTTOM.getDefaultModelLocation(Blocks.PALE_OAK_TRAPDOOR)
        );
        MultiVariant open = BlockModelGenerators.plainVariant(
                ModelTemplates.TRAPDOOR_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_TRAPDOOR)
        );

        blockModels.blockStateOutput.accept(BlockModelGenerators.createOrientableTrapdoor(
                RRBlocks.INVERTED_TREE_TRAPDOOR.get(),
                top,
                bottom,
                open
        ));
        blockModels.registerSimpleItemModel(
                RRBlocks.INVERTED_TREE_TRAPDOOR.get(),
                ModelTemplates.TRAPDOOR_BOTTOM.getDefaultModelLocation(Blocks.PALE_OAK_TRAPDOOR)
        );
    }

    private static void createInvertedTreeSignModels(@NonNull BlockModelGenerators blockModels) {
        // Reuse the vanilla Pale Oak sign models until the inverted-tree texture set exists.
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSign(
                        RRBlocks.INVERTED_TREE_SIGN.get(),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN, "_rot_0")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN, "_rot_1")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN, "_rot_2")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN, "_rot_3"))
                )
        );
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(
                                RRBlocks.INVERTED_TREE_WALL_SIGN.get(),
                                BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_WALL_SIGN))
                        )
                        .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING_ALT)
        );
        blockModels.registerSimpleItemModel(
                RRBlocks.INVERTED_TREE_SIGN.get().asItem(),
                ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN.asItem())
        );

        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createHangingSign(
                        RRBlocks.INVERTED_TREE_HANGING_SIGN.get(),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_rot_0")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_rot_1")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_rot_2")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_rot_3")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_attached_rot_0")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_attached_rot_1")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_attached_rot_2")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_attached_rot_3"))
                )
        );
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(
                                RRBlocks.INVERTED_TREE_WALL_HANGING_SIGN.get(),
                                BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_WALL_HANGING_SIGN))
                        )
                        .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING_ALT)
        );
        blockModels.registerSimpleItemModel(
                RRBlocks.INVERTED_TREE_HANGING_SIGN.get().asItem(),
                ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN.asItem())
        );
    }

    private static void createRuneRuinPortal(@NonNull BlockModelGenerators blockModels) {
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

    private static void createMossBerry(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(RRItems.MOSS_BERRY.get(), ModelTemplates.FLAT_ITEM);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.MOSS_BERRY_BUSH.get())
                        .with(PropertyDispatch.initial(BlockStateProperties.AGE_3)
                                .generate(age -> BlockModelGenerators.plainVariant(
                                        blockModels.createSuffixedVariant(
                                                RRBlocks.MOSS_BERRY_BUSH.get(),
                                                "_stage" + age,
                                                ModelTemplates.CROSS,
                                                TextureMapping::cross
                                        )
                                ))
                        )
        );
    }
}
