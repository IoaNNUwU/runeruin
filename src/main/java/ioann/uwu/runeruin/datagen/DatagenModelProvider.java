package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.client.GrapplingAnchorOut;
import ioann.uwu.runeruin.datagen.models.CloverModels;
import ioann.uwu.runeruin.datagen.models.DeepMossModels;
import ioann.uwu.runeruin.datagen.models.FireflyJarModels;
import ioann.uwu.runeruin.datagen.models.FloatingMossModels;
import ioann.uwu.runeruin.datagen.models.LilyPadModels;
import ioann.uwu.runeruin.datagen.models.MossLayerPlantModels;
import ioann.uwu.runeruin.datagen.models.MossberryBushModels;
import ioann.uwu.runeruin.datagen.models.PortalModels;
import ioann.uwu.runeruin.datagen.models.TreeModels;
import ioann.uwu.runeruin.datagen.models.VoidModels;
import ioann.uwu.runeruin.datagen.models.WispberryBushModels;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.NonNull;

/**
 * One line per block or block family; multi-block families live in {@code datagen/models/}, one class per
 * family, so parallel branches rarely touch this file.
 *
 * <p>A model with a geometry of its own is not built here: it is a file in {@code assets/runeruin/models} that a
 * person edits in Blockbench. This provider writes the blockstates and item definitions that refer to it, the
 * models on vanilla parents (cubes, crosses, stairs) and the models derived from such a file.
 */
public class DatagenModelProvider extends ModelProvider {

    /** The resources of the mod as they are in {@code src/main/resources}. */
    private final ResourceManager resources;

    public DatagenModelProvider(PackOutput output, ResourceManager resources) {
        super(output, RR.MODID);
        this.resources = resources;
    }

    @Override
    protected void registerModels(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {

        // --- Items ---
        itemModels.generateFlatItem(RRItems.RUNE_OF_SPACE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.SNAIL_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.MOSSY_PROPHET_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.MOSSY_WARRIOR_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.WISPBERRY.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.MOSSBERRY.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.POWDERED_MOSS_BUCKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.RUNIC_MINECART.get(), ModelTemplates.FLAT_ITEM);
        createGrapplingHook(itemModels);

        // --- Blocks ---
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE.get());
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE_BRICKS.get());
        blockModels.createTrivialCube(RRBlocks.POLISHED_ARCANE_STONE.get());
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_PILLAR.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_COLUMN.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        PortalModels.createArcaneStonePortal(blockModels);

        blockModels.createTrivialCube(RRBlocks.DIAMOND_ARCANE_STONE.get());
        blockModels.createPassiveRail(RRBlocks.RUNIC_RAIL.get());
        createAshenMushroomBlock(blockModels);

        TreeModels.createElden(blockModels);
        TreeModels.createGiantGoblet(blockModels);
        TreeModels.createBaobab(blockModels);
        TreeModels.createInvertedTree(blockModels);

        CloverModels.createClover(blockModels);

        blockModels.createTrivialCube(RRBlocks.MOSS_LIGHT.get());
        blockModels.createTrivialBlock(
                RRBlocks.POWDERED_MOSS.get(),
                TexturedModel.createDefault(
                        TextureMapping::defaultTexture,
                        ModelTemplates.create("powder_snow", TextureSlot.TEXTURE)
                )
        );
        blockModels.createFullAndCarpetBlocks(RRBlocks.GLOWING_MOSS.get(), RRBlocks.GLOWING_MOSS_CARPET.get());
        DeepMossModels.createDeepMoss(blockModels);
        MossberryBushModels.createMossberryBush(blockModels, this.resources);
        FloatingMossModels.createFloatingMoss(blockModels, this.resources);
        MossLayerPlantModels.createMossSprouts(blockModels, this.resources, RRBlocks.MOSS_SPROUTS.get());
        MossLayerPlantModels.createMossSprouts(blockModels, this.resources, RRBlocks.SMALL_MOSS_SPROUTS.get());
        blockModels.createTrivialBlock(
                RRBlocks.GLOWING_MUSHROOM_CAP.get(),
                TexturedModel.CUBE.updateTexture(TextureMapping::forceAllTranslucent)
        );
        blockModels.createTrivialCube(RRBlocks.GLOWING_MUSHROOM_STEM.get());
        blockModels.createCrossBlockWithDefaultItem(
                RRBlocks.GLOWING_MUSHROOM.get(),
                BlockModelGenerators.PlantType.NOT_TINTED
        );
        blockModels.createTrivialCube(RRBlocks.LAPIS_LIGHT.get());
        FireflyJarModels.createFireflyInJar(blockModels);
        LilyPadModels.createBigLilyPad(blockModels);
        LilyPadModels.createWaterLily(blockModels);
        createDeepRoots(blockModels);
        VoidModels.createVoid(blockModels);

        PortalModels.createRuneRuinPortal(blockModels);
        WispberryBushModels.createWispberryBush(blockModels, this.resources);
    }

    /**
     * The hook and its anchor are two files of one size and pose, so that the anchor sits at the muzzle; the anchor
     * also has an item definition of its own, which the flying anchor draws ({@code GrapplingAnchorRenderer}).
     */
    private static void createGrapplingHook(@NonNull ItemModelGenerators itemModels) {
        ItemModel.Unbaked hook = ItemModelUtils.plainModel(RR.id("item/grappling_hook"));
        ItemModel.Unbaked anchor = ItemModelUtils.plainModel(RR.id("item/grappling_anchor"));
        itemModels.itemModelOutput.accept(RRItems.GRAPPLING_HOOK.get(),
                ItemModelUtils.conditional(new GrapplingAnchorOut(), hook, ItemModelUtils.composite(hook, anchor)));
        itemModels.itemModelOutput.register(RR.id("grappling_anchor"), new ClientItem(anchor, ClientItem.Properties.DEFAULT));
    }

    private static void createAshenMushroomBlock(@NonNull BlockModelGenerators blockModels) {
        var block = RRBlocks.ASHEN_MUSHROOM_BLOCK.get();
        Material cap = new Material(RR.id("block/ashen_mushroom_cap"));
        Material underside = new Material(RR.id("block/ashen_mushroom_underside"));
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.PARTICLE, cap)
                .put(TextureSlot.NORTH, cap)
                .put(TextureSlot.SOUTH, cap)
                .put(TextureSlot.EAST, cap)
                .put(TextureSlot.WEST, cap)
                .put(TextureSlot.UP, cap)
                .put(TextureSlot.DOWN, underside);

        Identifier model = ModelTemplates.CUBE.create(block, textures, blockModels.modelOutput);
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(model))
        );
        blockModels.registerSimpleItemModel(block, model);
    }

    private static void createDeepRoots(@NonNull BlockModelGenerators blockModels) {
        var deepRoots = RRBlocks.DEEP_ROOTS.get();
        blockModels.registerSimpleItemModel(
                deepRoots.asItem(),
                BlockModelGenerators.PlantType.NOT_TINTED.createItemModel(blockModels, deepRoots)
        );
        Identifier deepRootsModel = RR.id("block/deep_roots");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                deepRoots,
                BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(deepRootsModel))
        ));
    }
}
