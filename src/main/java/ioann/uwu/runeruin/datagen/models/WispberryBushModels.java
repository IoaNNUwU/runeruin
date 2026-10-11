package ioann.uwu.runeruin.datagen.models;

import ioann.uwu.runeruin.blocks.MossLayerPlantBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WispberryBushBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/** Wispberry bush: a model file per age ({@code _stage0} ...), an azalea-like bush with cubes for berries. */
public final class WispberryBushModels {

    private WispberryBushModels() {}

    /**
     * One model per age and height of the moss the bush stands in, turned a random quarter: no two bushes side
     * by side look the same way.
     */
    public static void createWispberryBush(@NonNull BlockModelGenerators blockModels, ResourceManager resources) {
        Block block = RRBlocks.WISPBERRY_BUSH.get();
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(block);
        for (int moss = 0; moss <= MossLayerPlantBlock.MAX_MOSS; moss++) {
            for (int age : WispberryBushBlock.AGE.getPossibleValues()) {
                Identifier id = MossLayerPlantModels.lifted(blockModels, resources, block, "_stage" + age, moss);
                blockState.with(MossLayerPlantModels.moss(moss).term(WispberryBushBlock.AGE, age),
                        BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(id)));
            }
        }
        MossLayerPlantModels.addMoss(blockState);
        blockModels.blockStateOutput.accept(blockState);
    }
}
