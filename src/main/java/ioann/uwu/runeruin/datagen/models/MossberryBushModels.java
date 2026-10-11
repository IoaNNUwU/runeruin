package ioann.uwu.runeruin.datagen.models;

import ioann.uwu.runeruin.blocks.MossLayerPlantBlock;
import ioann.uwu.runeruin.blocks.MossberryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/**
 * Mossberry bush: a small crossed bush that is always there and, switched on by their properties, the twigs
 * around it ({@code _twigs}) and one cube per berry ({@code _berry0} ...), each a model file. All of them come
 * once per height of the moss the bush stands in.
 */
public final class MossberryBushModels {

    private MossberryBushModels() {}

    public static void createMossberryBush(@NonNull BlockModelGenerators blockModels, ResourceManager resources) {
        Block block = RRBlocks.MOSSBERRY_BUSH.get();
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(block);
        for (int moss = 0; moss <= MossLayerPlantBlock.MAX_MOSS; moss++) {
            blockState.with(MossLayerPlantModels.moss(moss),
                    BlockModelGenerators.plainVariant(MossLayerPlantModels.lifted(blockModels, resources, block, "", moss)));
            blockState.with(MossLayerPlantModels.moss(moss).term(MossberryBushBlock.TWIGS, true),
                    BlockModelGenerators.plainVariant(MossLayerPlantModels.lifted(blockModels, resources, block, "_twigs", moss)));
            for (int number = 0; number < MossberryBushBlock.BERRIES.size(); number++) {
                blockState.with(MossLayerPlantModels.moss(moss).term(MossberryBushBlock.BERRIES.get(number).property(), true),
                        BlockModelGenerators.plainVariant(MossLayerPlantModels.lifted(blockModels, resources, block, "_berry" + number, moss)));
            }
        }
        MossLayerPlantModels.addMoss(blockState);
        blockModels.blockStateOutput.accept(blockState);
    }
}
