package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/** Clover: flat leaves at different heights, each on a crossed stem, in the four segments of a flower bed. */
public final class CloverModels {

    /**
     * The clovers of each segment, in the order the segments are added (the quarters of the
     * block texture: north-west, south-west, south-east, north-east). One clover per row:
     * the corner of its square in the texture (x, z), the side of the square and its height.
     * The texture is the whole patch seen from above.
     */
    private static final int[][][] SEGMENTS = {
            {{0, 0, 4, 2}, {4, 4, 4, 3}, {5, 0, 3, 1}},
            {{0, 9, 7, 2}},
            {{12, 8, 4, 3}, {8, 12, 4, 2}, {8, 8, 3, 1}, {13, 13, 3, 1}},
            {{9, 0, 7, 3}},
    };

    private CloverModels() {}

    public static void createClover(@NonNull BlockModelGenerators blockModels) {
        MultiVariant[] models = new MultiVariant[SEGMENTS.length];
        for (int segment = 0; segment < SEGMENTS.length; segment++) {
            models[segment] = BlockModelGenerators.plainVariant(createSegment(blockModels, segment));
        }
        blockModels.createSegmentedBlock(
                RRBlocks.CLOVER.get(),
                models[0], BlockModelGenerators.FLOWER_BED_MODEL_1_SEGMENT_CONDITION,
                models[1], BlockModelGenerators.FLOWER_BED_MODEL_2_SEGMENT_CONDITION,
                models[2], BlockModelGenerators.FLOWER_BED_MODEL_3_SEGMENT_CONDITION,
                models[3], BlockModelGenerators.FLOWER_BED_MODEL_4_SEGMENT_CONDITION
        );
        blockModels.registerSimpleFlatItemModel(RRBlocks.CLOVER.get());
    }

    private static Identifier createSegment(BlockModelGenerators blockModels, int segment) {
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "runeruin:block/clover");
        textures.addProperty("clover", "runeruin:block/clover");
        textures.addProperty("stem", "runeruin:block/clover_stem");
        model.add("textures", textures);

        JsonArray elements = new JsonArray();
        for (int[] clover : SEGMENTS[segment]) {
            double x0 = clover[0];
            double z0 = clover[1];
            double x1 = x0 + clover[2];
            double z1 = z0 + clover[2];
            double height = clover[3];
            elements.add(element(new double[]{x0, height, z0}, new double[]{x1, height, z1},
                    "up", face("clover", x0, z0, x1, z1), "down", face("clover", x0, z1, x1, z0)));

            double x = (x0 + x1) / 2;
            double z = (z0 + z1) / 2;
            for (double angle : new double[]{45, -45}) {
                JsonObject stem = element(new double[]{x - 0.5, 0, z}, new double[]{x + 0.5, height, z},
                        "north", face("stem", 0, 7 - height, 1, 7), "south", face("stem", 0, 7 - height, 1, 7));
                JsonObject rotation = new JsonObject();
                rotation.add("origin", vector(x, 0, z));
                rotation.addProperty("axis", "y");
                rotation.addProperty("angle", angle);
                stem.add("rotation", rotation);
                elements.add(stem);
            }
        }
        model.add("elements", elements);

        Identifier modelId = RR.id("block/clover_" + (segment + 1));
        blockModels.modelOutput.accept(modelId, () -> model);
        return modelId;
    }

    private static JsonObject element(double[] from, double[] to, String side1, JsonObject face1, String side2, JsonObject face2) {
        JsonObject element = new JsonObject();
        element.add("from", vector(from));
        element.add("to", vector(to));
        JsonObject faces = new JsonObject();
        faces.add(side1, face1);
        faces.add(side2, face2);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject face(String texture, double u0, double v0, double u1, double v1) {
        JsonObject face = new JsonObject();
        face.add("uv", vector(u0, v0, u1, v1));
        face.addProperty("texture", "#" + texture);
        return face;
    }
}
