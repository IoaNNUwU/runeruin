package ioann.uwu.runeruin.datagen.models;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/** Firefly in a jar, a model file: glass body, wooden lid and a crossed glowing butterfly. */
public final class FireflyJarModels {

    private FireflyJarModels() {}

    public static void createFireflyInJar(@NonNull BlockModelGenerators blockModels) {
        Identifier modelId = RR.id("block/firefly_in_a_jar");
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(
                        RRBlocks.FIREFLY_IN_A_JAR.get(),
                        BlockModelGenerators.plainVariant(modelId)
                )
        );
        blockModels.registerSimpleItemModel(RRBlocks.FIREFLY_IN_A_JAR.get(), modelId);
    }
}
