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

    private static final String[] FACES = {"down", "up", "north", "south", "west", "east"};

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
                JsonObject plantModel = plantModel(plant, number, height);
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

    /** One cube, leaning away from the middle of the block: side by side the cubes read as a dome. */
    private static JsonObject plantModel(MossLayerBlock.Plant plant, int number, int mossHeight) {
        double half = plant.size() / 2.0;
        double[] center = {plant.x(), mossHeight + plant.lift(), plant.z()};
        JsonObject element = new JsonObject();
        element.add("from", vector(center[0] - half, center[1] - half, center[2] - half));
        element.add("to", vector(center[0] + half, center[1] + half, center[2] + half));
        JsonObject rotation = new JsonObject();
        rotation.add("origin", vector(center));
        // The game turns around x first: lean towards +z, then swing that lean to point away from the middle.
        rotation.addProperty("x", plant.tilt());
        rotation.addProperty("y", Math.round(Math.toDegrees(Math.atan2(plant.x() - 8, plant.z() - 8))));
        element.add("rotation", rotation);
        JsonObject faces = new JsonObject();
        for (int face = 0; face < FACES.length; face++) {
            faces.add(FACES[face], tileFace(plant.size(), number * 3 + face * 7));
        }
        element.add("faces", faces);
        JsonArray elements = new JsonArray();
        elements.add(element);
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

    /** moss_hedgehog.png holds face tiles: eight 4x4 in rows 0-7, ten 3x3 in rows 8-13, eight 2x2 in rows 14-15. */
    private static JsonObject tileFace(int size, int number) {
        int perRow = size == 4 ? 4 : size == 3 ? 5 : 8;
        int top = size == 4 ? 0 : size == 3 ? 8 : 14;
        int tile = number % (size == 2 ? perRow : perRow * 2);
        int u = tile % perRow * size;
        int v = top + tile / perRow * size;
        JsonObject face = new JsonObject();
        face.add("uv", vector(u, v, u + size, v + size));
        face.addProperty("texture", "#plant");
        return face;
    }
}
