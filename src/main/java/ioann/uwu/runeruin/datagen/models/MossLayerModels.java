package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.MossLayerBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/** Moss layer: a snow-like slab per height and, standing on it, one model per plant that its property switches on. */
public final class MossLayerModels {

    private static final String MOSS = "runeruin:block/moss_layer";

    private MossLayerModels() {}

    public static void createMossLayer(@NonNull BlockModelGenerators blockModels) {
        Block block = RRBlocks.MOSS_LAYER.get();
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(block);
        for (int layers = 1; layers <= MossLayerBlock.MAX_HEIGHT; layers++) {
            int height = layers * 2;
            Identifier layer = RR.id("block/moss_layer_height" + height);
            JsonObject layerModel = layerModel(height);
            blockModels.modelOutput.accept(layer, () -> layerModel);
            blockState.with(new ConditionBuilder().term(MossLayerBlock.LAYERS, layers), BlockModelGenerators.plainVariant(layer));

            for (int number = 0; number < MossLayerBlock.PLANTS.size(); number++) {
                MossLayerBlock.Plant plant = MossLayerBlock.PLANTS.get(number);
                Identifier id = RR.id("block/moss_layer_plant" + number + "_height" + height);
                JsonObject plantModel = plantModel(plant, height);
                blockModels.modelOutput.accept(id, () -> plantModel);
                blockState.with(new ConditionBuilder().term(MossLayerBlock.LAYERS, layers).term(plant.property(), true),
                        BlockModelGenerators.plainVariant(id));
            }
        }
        blockModels.blockStateOutput.accept(blockState);
        blockModels.registerSimpleItemModel(block, RR.id("block/moss_layer_height2"));
    }

    private static JsonObject layerModel(int height) {
        JsonObject model = new JsonObject();
        JsonObject textures = new JsonObject();
        if (height == 16) {
            model.addProperty("parent", "minecraft:block/cube_all");
            textures.addProperty("all", MOSS);
        } else {
            model.addProperty("parent", "minecraft:block/snow_height" + height);
            textures.addProperty("particle", MOSS);
            textures.addProperty("texture", MOSS);
        }
        model.add("textures", textures);
        return model;
    }

    /**
     * One cube, leaning away from the middle of the block: side by side the cubes read as a dome. Two upright
     * planes through its middle, a tenth wider than the cube, show their dark rim around it as spikes.
     */
    private static JsonObject plantModel(MossLayerBlock.Plant plant, int mossHeight) {
        double half = plant.size() / 2.0;
        double rim = half * 1.1;
        double[] center = {plant.x(), mossHeight + plant.lift(), plant.z()};
        JsonObject rotation = new JsonObject();
        rotation.add("origin", vector(center));
        // The game turns around x first: lean towards +z, then swing that lean to point away from the middle.
        rotation.addProperty("x", plant.tilt());
        rotation.addProperty("y", Math.round(Math.toDegrees(Math.atan2(plant.x() - 8, plant.z() - 8))));
        JsonArray elements = new JsonArray();
        elements.add(element(center, half, half, half, rotation, tile(plant.size(), 0), "down", "up", "north", "south", "west", "east"));
        elements.add(element(center, rim, rim, 0, rotation, tile(plant.size(), 4), "north", "south"));
        elements.add(element(center, 0, rim, rim, rotation, tile(plant.size(), 4), "west", "east"));
        JsonObject model = new JsonObject();
        // No smooth lighting: it is meant for block faces, not for cubes this small and tilted.
        model.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", MOSS);
        textures.addProperty("plant", "runeruin:block/moss_hedgehog");
        model.add("textures", textures);
        model.add("elements", elements);
        return model;
    }

    private static JsonObject element(double[] center, double x, double y, double z, JsonObject rotation,
                                      JsonObject face, String... sides) {
        JsonObject element = new JsonObject();
        element.add("from", vector(center[0] - x, center[1] - y, center[2] - z));
        element.add("to", vector(center[0] + x, center[1] + y, center[2] + z));
        element.add("rotation", rotation);
        JsonObject faces = new JsonObject();
        for (String side : sides) {
            faces.add(side, face);
        }
        element.add("faces", faces);
        return element;
    }

    /**
     * moss_hedgehog.png, per cube size a row of two tiles as wide as the cube: its face at u 0 and its spike
     * plane at u 4. The rows start at v 0 (4x4), 4 (3x3) and 7 (2x2).
     */
    private static JsonObject tile(int size, int u) {
        int v = size == 4 ? 0 : size == 3 ? 4 : 7;
        JsonObject face = new JsonObject();
        face.add("uv", vector(u, v, u + size, v + size));
        face.addProperty("texture", "#plant");
        return face;
    }
}
