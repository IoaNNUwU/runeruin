package ioann.uwu.runeruin.datagen;

import com.google.gson.JsonParser;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.datagen.models.CloverModels;
import ioann.uwu.runeruin.datagen.models.DeepMossModels;
import ioann.uwu.runeruin.datagen.models.FireflyJarModels;
import ioann.uwu.runeruin.datagen.models.FloatingMossModels;
import ioann.uwu.runeruin.datagen.models.LilyPadModels;
import ioann.uwu.runeruin.datagen.models.MossberryBushModels;
import ioann.uwu.runeruin.datagen.models.PortalModels;
import ioann.uwu.runeruin.datagen.models.TreeModels;
import ioann.uwu.runeruin.datagen.models.VoidModels;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.NonNull;

/**
 * One line per block or block family. Hand-built and multi-block models live in
 * {@code datagen/models/}, one class per family, so parallel branches rarely touch this file.
 */
public class DatagenModelProvider extends ModelProvider {

    public DatagenModelProvider(PackOutput output) {
        super(output, RR.MODID);
    }

    @Override
    protected void registerModels(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {

        // --- Items ---
        itemModels.generateFlatItem(RRItems.RUNE_OF_SPACE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.SNAIL_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.MOSSBERRY.get(), ModelTemplates.FLAT_ITEM);

        // --- Blocks ---
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE.get());
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE_BRICKS.get());
        blockModels.createTrivialCube(RRBlocks.POLISHED_ARCANE_STONE.get());
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_PILLAR.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_COLUMN.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        PortalModels.createArcaneStonePortal(blockModels);

        blockModels.createTrivialCube(RRBlocks.DIAMOND_ARCANE_STONE.get());
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
        MossberryBushModels.createMossberryBush(blockModels);
        FloatingMossModels.createFloatingMoss(blockModels);
        blockModels.createCrossBlockWithDefaultItem(RRBlocks.MOSS_SPROUTS.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        blockModels.createCrossBlockWithDefaultItem(RRBlocks.SMALL_MOSS_SPROUTS.get(), BlockModelGenerators.PlantType.NOT_TINTED);
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
        createWispberry(blockModels, itemModels);
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
        blockModels.modelOutput.accept(deepRootsModel, () -> JsonParser.parseString("""
                {
                  "ambientocclusion": false,
                  "textures": {"particle": "runeruin:block/deep_roots", "cross": "runeruin:block/deep_roots"},
                  "elements": [
                    {"from": [0.8, 0, 8], "to": [15.2, 16, 8], "shade": false,
                     "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": true},
                     "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                               "south": {"uv": [16, 0, 0, 16], "texture": "#cross"}}},
                    {"from": [8, 0, 0.8], "to": [8, 16, 15.2], "shade": false,
                     "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": true},
                     "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                               "east": {"uv": [16, 0, 0, 16], "texture": "#cross"}}}
                  ]
                }"""));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                deepRoots,
                BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(deepRootsModel))
        ));
    }

    private static void createWispberry(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(RRItems.WISPBERRY.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.POWDERED_MOSS_BUCKET.get(), ModelTemplates.FLAT_ITEM);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.WISPBERRY_BUSH.get())
                        .with(PropertyDispatch.initial(BlockStateProperties.AGE_3)
                                .generate(age -> BlockModelGenerators.plainVariant(
                                        blockModels.createSuffixedVariant(
                                                RRBlocks.WISPBERRY_BUSH.get(),
                                                "_stage" + age,
                                                ModelTemplates.CROSS,
                                                TextureMapping::cross
                                        )
                                ))
                        )
        );
    }
}
