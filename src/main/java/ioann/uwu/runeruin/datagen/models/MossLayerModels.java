package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.MossLayerBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import java.util.stream.IntStream;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/** Moss layer: a snow-like slab per height and, standing on it, one model per plant that the plant count switches on. */
public final class MossLayerModels {

    private static final String MOSS = "runeruin:block/moss_layer";

    /**
     * Per plant: x, z, cube size and height of the centre above the moss, in pixels. One big ball, three medium
     * ones around it and three small ones further out, each a little higher or lower, so that seven form a dome.
     */
    private static final double[][] PLANTS = {
            {8, 8, 4, 2.5},
            {4, 6.5, 3, 2.8},
            {11.5, 5, 3, 2},
            {9, 12.5, 3, 2.4},
            {13, 9.5, 2, 1.7},
            {4, 11.5, 2, 1.2},
            {7, 2.5, 2, 2},
    };
    /** A plant is four cubes, three of them turned 45 degrees around one axis each: the corners are its spikes. */
    private static final double[][] CUBE_TURNS = {{0, 0, 0}, {45, 0, 0}, {0, 45, 0}, {0, 0, 45}};
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

            for (int plant = 1; plant <= MossLayerBlock.MAX_PLANTS; plant++) {
                Identifier id = RR.id("block/moss_layer_plant" + plant + "_height" + height);
                JsonObject plantModel = plantModel(plant - 1, height);
                blockModels.modelOutput.accept(id, () -> plantModel);
                // Plant N stands from N plants up.
                Integer[] more = IntStream.rangeClosed(plant + 1, MossLayerBlock.MAX_PLANTS).boxed().toArray(Integer[]::new);
                blockState.with(new ConditionBuilder().term(MossLayerBlock.LAYERS, layers).term(MossLayerBlock.PLANTS, plant, more),
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

    private static JsonObject plantModel(int plant, int mossHeight) {
        int size = (int) PLANTS[plant][2];
        double[] center = {PLANTS[plant][0], mossHeight + PLANTS[plant][3], PLANTS[plant][1]};
        JsonArray elements = new JsonArray();
        for (int cube = 0; cube < CUBE_TURNS.length; cube++) {
            JsonObject element = new JsonObject();
            element.add("from", vector(center[0] - size / 2.0, center[1] - size / 2.0, center[2] - size / 2.0));
            element.add("to", vector(center[0] + size / 2.0, center[1] + size / 2.0, center[2] + size / 2.0));
            JsonObject rotation = new JsonObject();
            rotation.add("origin", vector(center));
            rotation.addProperty("x", CUBE_TURNS[cube][0]);
            // Every plant is also turned around, so that no two in a block look the same.
            rotation.addProperty("y", CUBE_TURNS[cube][1] + plant * 37);
            rotation.addProperty("z", CUBE_TURNS[cube][2]);
            element.add("rotation", rotation);
            JsonObject faces = new JsonObject();
            for (int face = 0; face < FACES.length; face++) {
                faces.add(FACES[face], tileFace(size, plant * 3 + cube * 5 + face * 7));
            }
            element.add("faces", faces);
            elements.add(element);
        }
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
