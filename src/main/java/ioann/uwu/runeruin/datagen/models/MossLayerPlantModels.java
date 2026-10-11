package ioann.uwu.runeruin.datagen.models;

import com.google.gson.JsonObject;
import ioann.uwu.runeruin.blocks.MossLayerPlantBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/**
 * Plants that stand in a deep moss layer: for every height of the moss, the model of that layer and the models
 * of the plant lifted onto it. The plant on the ground is a model file; the lifted ones are derived from it.
 */
public final class MossLayerPlantModels {

    private MossLayerPlantModels() {}

    /**
     * Sprouts stand in one of three places in their block, each a model file ({@code _place0} ...). Every place
     * also comes turned around the middle of the block, so three models give twelve places. The block itself
     * has no random offset, as grass has: it would move the moss under the sprouts too.
     */
    private static final int PLACES = 3;

    /** Moss sprouts: a crossed plant with a flat item, in a place picked by the position of the block. */
    public static void createMossSprouts(@NonNull BlockModelGenerators blockModels, ResourceManager resources, Block block) {
        blockModels.registerSimpleFlatItemModel(block);
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(block);
        for (int moss = 0; moss <= MossLayerPlantBlock.MAX_MOSS; moss++) {
            List<Variant> places = new ArrayList<>();
            for (int place = 0; place < PLACES; place++) {
                Variant model = BlockModelGenerators.plainModel(lifted(blockModels, resources, block, "_place" + place, moss));
                places.addAll(List.of(model, model.with(BlockModelGenerators.Y_ROT_90), model.with(BlockModelGenerators.Y_ROT_180), model.with(BlockModelGenerators.Y_ROT_270)));
            }
            blockState.with(moss(moss), BlockModelGenerators.variants(places.toArray(Variant[]::new)));
        }
        addMoss(blockState);
        blockModels.blockStateOutput.accept(blockState);
    }

    static ConditionBuilder moss(int moss) {
        return new ConditionBuilder().term(MossLayerPlantBlock.MOSS, moss);
    }

    /**
     * The model of a part of the plant that stands on {@code moss} layers. Without moss it is the model file
     * of the part; on moss it is that file lifted by the height of the moss, written here.
     */
    static Identifier lifted(BlockModelGenerators blockModels, ResourceManager resources, Block block, String part, int moss) {
        Identifier file = ModelLocationUtils.getModelLocation(block, part);
        if (moss == 0) {
            return file;
        }
        Identifier id = ModelLocationUtils.getModelLocation(block, part + "_moss" + moss);
        JsonObject model = ModelJson.lifted(ModelJson.read(resources, file), moss * 2);
        blockModels.modelOutput.accept(id, () -> model);
        return id;
    }

    /** Under the plant, the deep moss layer of its height. */
    static void addMoss(MultiPartGenerator blockState) {
        for (int moss = 1; moss <= MossLayerPlantBlock.MAX_MOSS; moss++) {
            blockState.with(moss(moss), BlockModelGenerators.plainVariant(DeepMossModels.heightModel(moss * 2)));
        }
    }
}
