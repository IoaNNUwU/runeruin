package ioann.uwu.runeruin.datagen;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.data.PackOutput;
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
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ELDEN_LOG.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);

        blockModels.createTrivialCube(RRBlocks.MOSS_LIGHT.get());
        blockModels.createTrivialCube(RRBlocks.LAPIS_LIGHT.get());

        createMossBerry(blockModels, itemModels);
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
