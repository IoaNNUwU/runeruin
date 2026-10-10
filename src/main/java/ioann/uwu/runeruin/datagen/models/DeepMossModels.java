package ioann.uwu.runeruin.datagen.models;

import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.DeepMossLayerBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/** Deep moss and its layers, built as vanilla builds the snow block and snow layers, on the same slab models. */
public final class DeepMossModels {

    private DeepMossModels() {}

    public static void createDeepMoss(@NonNull BlockModelGenerators blockModels) {
        Block deepMoss = RRBlocks.DEEP_MOSS.get();
        Block layer = RRBlocks.DEEP_MOSS_LAYER.get();
        blockModels.createTrivialCube(deepMoss);
        MultiVariant full = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(deepMoss));

        for (int height = 2; height < 16; height += 2) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:block/snow_height" + height);
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", "runeruin:block/deep_moss");
            textures.addProperty("texture", "runeruin:block/deep_moss");
            model.add("textures", textures);
            blockModels.modelOutput.accept(heightModel(height), () -> model);
        }
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(layer)
                .with(PropertyDispatch.initial(DeepMossLayerBlock.LAYERS)
                        .generate(layers -> layers < DeepMossLayerBlock.MAX_HEIGHT
                                ? BlockModelGenerators.plainVariant(heightModel(layers * 2))
                                : full)));
        blockModels.registerSimpleItemModel(layer, heightModel(2));
    }

    private static Identifier heightModel(int height) {
        return RR.id("block/deep_moss_layer_height" + height);
    }
}
