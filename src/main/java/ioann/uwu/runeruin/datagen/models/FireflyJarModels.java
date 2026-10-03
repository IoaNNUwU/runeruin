package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.addElement;
import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/** Firefly in a jar: glass body, wooden lid and a crossed glowing butterfly. */
public final class FireflyJarModels {

    private FireflyJarModels() {}

    public static void createFireflyInJar(@NonNull BlockModelGenerators blockModels) {
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "runeruin:block/firefly_jar_glass");
        textures.addProperty("glass", "runeruin:block/firefly_jar_glass");
        textures.addProperty("butterfly", "runeruin:block/firefly_butterfly");
        textures.addProperty("wood", "minecraft:block/oak_planks");
        model.add("textures", textures);

        JsonArray elements = new JsonArray();
        addJarGlassElement(elements);
        addElement(elements, "jar_wood_neck", new double[]{1, 13, 1}, new double[]{15, 14, 15}, "wood", 0,
                "north", "south", "east", "west", "up", "down");
        addElement(elements, "jar_wood_lid", new double[]{0.5, 14, 0.5}, new double[]{15.5, 16, 15.5}, "wood", 0,
                "north", "south", "east", "west", "up", "down");

        // Two thin, crossed planes keep the tiny 8x8 butterfly visible from every side.
        addElement(elements, "firefly_plane_north_south", new double[]{4, 4, 7.9}, new double[]{12, 12, 8.1}, "butterfly", 15,
                "north", "south");
        addElement(elements, "firefly_plane_east_west", new double[]{7.9, 4, 4}, new double[]{8.1, 12, 12}, "butterfly", 15,
                "east", "west");
        model.add("elements", elements);

        Identifier modelId = RR.id("block/firefly_in_a_jar");
        blockModels.modelOutput.accept(modelId, () -> model);
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(
                        RRBlocks.FIREFLY_IN_A_JAR.get(),
                        BlockModelGenerators.plainVariant(modelId)
                )
        );
        blockModels.registerSimpleItemModel(RRBlocks.FIREFLY_IN_A_JAR.get(), modelId);
    }

    private static void addJarGlassElement(JsonArray elements) {
        JsonObject element = new JsonObject();
        element.addProperty("name", "jar_glass_body");
        element.add("from", vector(new double[]{1, 0, 1}));
        element.add("to", vector(new double[]{15, 13, 15}));

        JsonObject faces = new JsonObject();
        double[] sideUv = {1, 1.5, 15, 14.5};
        for (String direction : new String[]{"north", "south", "east", "west"}) {
            addJarGlassFace(faces, direction, sideUv);
        }
        element.add("faces", faces);
        elements.add(element);
    }

    private static void addJarGlassFace(JsonObject faces, String direction, double[] uv) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", "#glass");
        face.add("uv", vector(uv));
        faces.add(direction, face);
    }
}
