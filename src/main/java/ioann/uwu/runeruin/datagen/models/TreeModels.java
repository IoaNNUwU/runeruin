package ioann.uwu.runeruin.datagen.models;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.EldenVinesBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.BlockFamily;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

/** Wood families: Elden, Baobab, the inverted tree and the Giant Goblet. */
public final class TreeModels {

    private TreeModels() {}

    public static void createElden(@NonNull BlockModelGenerators blockModels) {
        blockModels.createPlantWithDefaultItem(RRBlocks.ELDEN_SAPLING.get(), RRBlocks.POTTED_ELDEN_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);

        blockModels.createTrivialBlock(RRBlocks.ELDEN_LEAVES.get(), TexturedModel.LEAVES);
        blockModels.createLeafLitter(RRBlocks.ELDEN_LEAF_LITTER.get());

        Material bark = TextureMapping.getBlockTexture(RRBlocks.ELDEN_LOG.get());
        Material core = TextureMapping.getBlockTexture(RRBlocks.ELDEN_LOG.get(), "_top");
        blockModels.createTrivialBlock(RRBlocks.ELDEN_WOOD.get(), _ -> TexturedModel.createAllSame(bark));
        blockModels.new WoodProvider(TextureMapping.column(bark, core))
                .logWithHorizontal(RRBlocks.ELDEN_LOG.get());
        blockModels.createTrivialCube(RRBlocks.ELDEN_PLANKS.get());

        BlockFamily family = new BlockFamily.Builder(RRBlocks.ELDEN_PLANKS.get())
                .button(RRBlocks.ELDEN_BUTTON.get())
                .door(RRBlocks.ELDEN_DOOR.get())
                .fence(RRBlocks.ELDEN_FENCE.get())
                .fenceGate(RRBlocks.ELDEN_FENCE_GATE.get())
                .hangingSign(RRBlocks.ELDEN_HANGING_SIGN.get(), RRBlocks.ELDEN_WALL_HANGING_SIGN.get())
                .pressurePlate(RRBlocks.ELDEN_PRESSURE_PLATE.get())
                .sign(RRBlocks.ELDEN_SIGN.get(), RRBlocks.ELDEN_WALL_SIGN.get())
                .slab(RRBlocks.ELDEN_SLAB.get())
                .stairs(RRBlocks.ELDEN_STAIRS.get())
                .strippedLog(RRBlocks.ELDEN_LOG.get())
                .getFamily();
        blockModels.familyWithExistingFullBlock(RRBlocks.ELDEN_PLANKS.get()).generateFor(family);
        // Oak uses the non-orientable trapdoor UV templates, unlike the generic family default.
        blockModels.createTrapdoor(RRBlocks.ELDEN_TRAPDOOR.get());

        createEldenVines(blockModels);
    }

    public static void createGiantGoblet(@NonNull BlockModelGenerators blockModels) {
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

    public static void createBaobab(@NonNull BlockModelGenerators blockModels) {
        var acaciaLogSide = TextureMapping.getBlockTexture(Blocks.ACACIA_LOG);
        var acaciaLogTop = TextureMapping.getBlockTexture(Blocks.ACACIA_LOG, "_top");
        TexturedModel.Provider baobabLog = TexturedModel.COLUMN_ALT.updateTexture(mapping -> mapping
                .put(TextureSlot.SIDE, acaciaLogSide)
                .put(TextureSlot.END, acaciaLogTop)
                .put(TextureSlot.PARTICLE, acaciaLogSide));
        TexturedModel.Provider baobabLogHorizontal = TexturedModel.COLUMN_HORIZONTAL_ALT.updateTexture(mapping -> mapping
                .put(TextureSlot.SIDE, acaciaLogSide)
                .put(TextureSlot.END, acaciaLogTop)
                .put(TextureSlot.PARTICLE, acaciaLogSide));
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.BAOBAB_LOG.get(), baobabLog, baobabLogHorizontal);
        TexturedModel.Provider baobabWood = TexturedModel.CUBE.updateTexture(mapping -> mapping.put(
                TextureSlot.ALL,
                acaciaLogSide
        ));
        blockModels.createTrivialBlock(RRBlocks.BAOBAB_WOOD.get(), baobabWood);
        blockModels.registerSimpleItemModel(
                RRBlocks.BAOBAB_WOOD.get(),
                ModelLocationUtils.getModelLocation(RRBlocks.BAOBAB_WOOD.get())
        );
        blockModels.createTintedLeaves(
                RRBlocks.BAOBAB_LEAVES.get(),
                TexturedModel.LEAVES.updateTexture(mapping -> mapping.put(
                        TextureSlot.ALL,
                        TextureMapping.getBlockTexture(Blocks.ACACIA_LEAVES)
                )),
                0x6B9C3C
        );
    }

    public static void createInvertedTree(@NonNull BlockModelGenerators blockModels) {
        blockModels.createTrivialBlock(RRBlocks.INVERTED_LEAVES_1.get(), TexturedModel.LEAVES);
        blockModels.createTrivialBlock(RRBlocks.INVERTED_LEAVES_2.get(), TexturedModel.LEAVES);

        Material invertedWood = new Material(RR.id("block/inverted_tree_wood"));
        blockModels.new WoodProvider(TextureMapping.column(invertedWood, invertedWood))
                .wood(RRBlocks.INVERTED_TREE_WOOD.get());

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

    private static void createEldenVines(@NonNull BlockModelGenerators blockModels) {
        var block = RRBlocks.ELDEN_VINES.get();
        var stemTexture = new Material(RR.id("block/elden_vines"));
        var orbTexture = new Material(RR.id("block/elden_vines_orb"));
        var stemModel = blockModels.createSuffixedVariant(
                block,
                "",
                ModelTemplates.CROSS,
                _ -> TextureMapping.cross(stemTexture)
        );
        var orbModel = blockModels.createSuffixedVariant(
                block,
                "_orb",
                ModelTemplates.CROSS,
                _ -> TextureMapping.cross(orbTexture)
        );

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(BlockModelGenerators.createBooleanModelDispatch(
                                EldenVinesBlock.ORB,
                                BlockModelGenerators.plainVariant(orbModel),
                                BlockModelGenerators.plainVariant(stemModel)
                        ))
        );
        blockModels.registerSimpleItemModel(block, stemModel);
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
}
