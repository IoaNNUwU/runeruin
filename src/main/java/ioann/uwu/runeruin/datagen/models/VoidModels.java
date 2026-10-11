package ioann.uwu.runeruin.datagen.models;

import static net.minecraft.client.data.models.BlockModelGenerators.condition;
import static net.minecraft.client.data.models.BlockModelGenerators.plainModel;
import static net.minecraft.client.data.models.BlockModelGenerators.plainVariant;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.BeadVineBlock;
import ioann.uwu.runeruin.blocks.HangingChorusFlowerBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import java.util.List;
import java.util.Map;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.NonNull;

/** Void stone and the plants that hang from it. The bead vine and the dust bloom are model files. */
public final class VoidModels {

    private VoidModels() {}

    public static void createVoid(@NonNull BlockModelGenerators blockModels) {
        blockModels.createTrivialCube(RRBlocks.VOID_STONE.get());
        createBeadVine(blockModels);
        createDustBloom(blockModels);
        createHangingChorus(blockModels);
    }

    private static void createBeadVine(BlockModelGenerators blockModels) {
        Block block = RRBlocks.BEAD_VINE.get();
        Identifier dull = RR.id("block/bead_vine");
        // Lit beads are bright in the dark on their own, not only by the light they give: the model emits light.
        Identifier glowing = RR.id("block/bead_vine_glowing");
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(
                BlockModelGenerators.createBooleanModelDispatch(BeadVineBlock.GLOWING, plainVariant(glowing), plainVariant(dull))
        ));
        blockModels.registerSimpleFlatItemModel(block, "_glowing");
    }

    private static void createDustBloom(BlockModelGenerators blockModels) {
        Block block = RRBlocks.DUST_BLOOM.get();
        // From below, where the player usually is, crossed planes are two lines: the model has the open flower on top.
        blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, plainVariant(RR.id("block/dust_bloom"))));
        blockModels.registerSimpleFlatItemModel(block, "_top");
    }

    /** The vanilla chorus models: the plant is the same from every side, so it needs none of its own. */
    private static void createHangingChorus(BlockModelGenerators blockModels) {
        Block plant = RRBlocks.HANGING_CHORUS_PLANT.get();
        MultiVariant side = plainVariant(vanilla("chorus_plant_side"));
        MultiVariant noSide = new MultiVariant(WeightedList.of(
                new Weighted<>(plainModel(vanilla("chorus_plant_noside")), 2),
                new Weighted<>(plainModel(vanilla("chorus_plant_noside1")), 1),
                new Weighted<>(plainModel(vanilla("chorus_plant_noside2")), 1),
                new Weighted<>(plainModel(vanilla("chorus_plant_noside3")), 1)
        ));
        List<Map.Entry<BooleanProperty, VariantMutator>> rotations = List.of(
                Map.entry(BlockStateProperties.NORTH, BlockModelGenerators.NOP),
                Map.entry(BlockStateProperties.EAST, BlockModelGenerators.Y_ROT_90),
                Map.entry(BlockStateProperties.SOUTH, BlockModelGenerators.Y_ROT_180),
                Map.entry(BlockStateProperties.WEST, BlockModelGenerators.Y_ROT_270),
                Map.entry(BlockStateProperties.UP, BlockModelGenerators.X_ROT_270),
                Map.entry(BlockStateProperties.DOWN, BlockModelGenerators.X_ROT_90)
        );
        MultiPartGenerator plantStates = MultiPartGenerator.multiPart(plant);
        for (Map.Entry<BooleanProperty, VariantMutator> turn : rotations) {
            VariantMutator rotation = turn.getValue();
            plantStates
                    .with(condition().term(turn.getKey(), true), side.with(rotation).with(BlockModelGenerators.UV_LOCK))
                    .with(condition().term(turn.getKey(), false), noSide.with(rotation).with(BlockModelGenerators.UV_LOCK));
        }
        blockModels.blockStateOutput.accept(plantStates);
        blockModels.registerSimpleItemModel(plant, vanilla("chorus_plant"));

        Block flower = RRBlocks.HANGING_CHORUS_FLOWER.get();
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(flower).with(
                BlockModelGenerators.createEmptyOrFullDispatch(
                        HangingChorusFlowerBlock.AGE,
                        HangingChorusFlowerBlock.DEAD_AGE,
                        plainVariant(vanilla("chorus_flower_dead")),
                        plainVariant(vanilla("chorus_flower"))
                )
        ));
        blockModels.registerSimpleItemModel(flower, vanilla("chorus_flower"));
    }

    private static Identifier vanilla(String model) {
        return Identifier.withDefaultNamespace("block/" + model);
    }
}
