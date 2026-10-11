package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
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
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/**
 * Plants that stand in a deep moss layer: for every height of the moss, the model of that layer and the models
 * of the plant lifted onto it.
 */
public final class MossLayerPlantModels {

    private MossLayerPlantModels() {}

    /**
     * Where sprouts stand in their block: pixels from the middle along x and z, and pixels down into the ground.
     * Every place also comes turned around the middle of the block, so three models give twelve places. The
     * block itself has no random offset, as grass has: it would move the moss under the sprouts too.
     */
    private static final double[][] PLACES = {{1, 2.5, 0}, {3.5, 0.5, 1}, {2.5, -3, 2.5}};

    /** Moss sprouts: a crossed plant with a flat item, in a place picked by the position of the block. */
    public static void createMossSprouts(@NonNull BlockModelGenerators blockModels, Block block) {
        blockModels.registerSimpleFlatItemModel(block);
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(block);
        for (int moss = 0; moss <= MossLayerPlantBlock.MAX_MOSS; moss++) {
            List<Variant> places = new ArrayList<>();
            for (int place = 0; place < PLACES.length; place++) {
                double[] shift = PLACES[place];
                Variant model = BlockModelGenerators.plainModel(cross(blockModels, block, "_place" + place, moss, shift[0], shift[1], shift[2]));
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

    /** The name of a model that stands on {@code moss} layers; without moss it is the plain name. */
    static Identifier lifted(Block block, String part, int moss) {
        return ModelLocationUtils.getModelLocation(block, moss == 0 ? part : part + "_moss" + moss);
    }

    /** Under the plant, the deep moss layer of its height. */
    static void addMoss(MultiPartGenerator blockState) {
        for (int moss = 1; moss <= MossLayerPlantBlock.MAX_MOSS; moss++) {
            blockState.with(moss(moss), BlockModelGenerators.plainVariant(DeepMossModels.heightModel(moss * 2)));
        }
    }

    /** The vanilla cross with the texture of the block, standing on {@code moss} layers. */
    static Identifier cross(BlockModelGenerators blockModels, Block block, int moss) {
        return cross(blockModels, block, "", moss, 0, 0, 0);
    }

    private static Identifier cross(BlockModelGenerators blockModels, Block block, String part, int moss, double x, double z, double sink) {
        String texture = ModelLocationUtils.getModelLocation(block).toString();
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", texture);
        textures.addProperty("cross", texture);
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        double[] middle = {8 + x, moss * 2 - sink + 8, 8 + z};
        elements.add(crossPlane(middle, 7.2, 0, "north", "south"));
        elements.add(crossPlane(middle, 0, 7.2, "west", "east"));
        model.add("elements", elements);
        Identifier id = lifted(block, part, moss);
        blockModels.modelOutput.accept(id, () -> model);
        return id;
    }

    private static JsonObject crossPlane(double[] middle, double x, double z, String... sides) {
        JsonObject plane = new JsonObject();
        plane.add("from", vector(middle[0] - x, middle[1] - 8, middle[2] - z));
        plane.add("to", vector(middle[0] + x, middle[1] + 8, middle[2] + z));
        JsonObject rotation = new JsonObject();
        rotation.add("origin", vector(middle));
        rotation.addProperty("axis", "y");
        rotation.addProperty("angle", 45);
        rotation.addProperty("rescale", true);
        plane.add("rotation", rotation);
        plane.addProperty("shade", false);
        JsonObject faces = new JsonObject();
        for (String side : sides) {
            JsonObject face = new JsonObject();
            face.add("uv", vector(0, 0, 16, 16));
            face.addProperty("texture", "#cross");
            faces.add(side, face);
        }
        plane.add("faces", faces);
        return plane;
    }
}
