package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/** Clover: flat leaves at different heights, each on a crossed stem. */
public final class CloverModels {

    /**
     * One clover per row: the texel of its center in the block texture (x, z), how far its
     * leaves reach from the center, and its height. The texture is the patch seen from above.
     */
    private static final int[][] CLOVERS = {
            {3, 3, 2, 3}, {11, 4, 2, 2}, {7, 10, 2, 3}, {13, 12, 2, 2},
            {7, 1, 1, 2}, {14, 1, 1, 1}, {1, 8, 1, 2}, {2, 12, 1, 1}, {5, 14, 1, 2}, {9, 14, 1, 1}, {14, 7, 1, 3},
    };

    private CloverModels() {}

    public static void createClover(@NonNull BlockModelGenerators blockModels) {
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "runeruin:block/clover");
        textures.addProperty("clover", "runeruin:block/clover");
        textures.addProperty("stem", "runeruin:block/clover_stem");
        model.add("textures", textures);

        JsonArray elements = new JsonArray();
        for (int[] clover : CLOVERS) {
            double x0 = clover[0] - clover[2];
            double z0 = clover[1] - clover[2];
            double x1 = clover[0] + clover[2] + 1;
            double z1 = clover[1] + clover[2] + 1;
            double height = clover[3];
            elements.add(element(new double[]{x0, height, z0}, new double[]{x1, height, z1},
                    "up", face("clover", x0, z0, x1, z1), "down", face("clover", x0, z1, x1, z0)));

            double x = clover[0] + 0.5;
            double z = clover[1] + 0.5;
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

        Identifier modelId = RR.id("block/clover");
        blockModels.modelOutput.accept(modelId, () -> model);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                RRBlocks.CLOVER.get(),
                BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(modelId))
        ));
        blockModels.registerSimpleFlatItemModel(RRBlocks.CLOVER.get());
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
