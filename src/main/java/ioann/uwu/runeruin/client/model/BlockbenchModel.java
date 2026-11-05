package ioann.uwu.runeruin.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import ioann.uwu.runeruin.RR;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.GsonHelper;

/**
 * The shape of a mob as a person keeps it: a Blockbench project of the format "Modded Entity",
 * {@code assets/runeruin/models/entity/<name>.bbmodel}. The game builds the model from the file, so a shape
 * saved in Blockbench is in the game the next time it starts. A bone becomes a part with the same name in the
 * same place of the tree, and the animation of a model finds its parts by those names.
 *
 * <p>Blockbench keeps a model with X and Y turned over and every pivot as a place in the whole model; this reads
 * the file the way Blockbench's own Java export writes it (Blockbench 5.2, {@code modded_entity.ts}).
 */
public final class BlockbenchModel {

    private BlockbenchModel() {}

    /**
     * @param animated the model that will move the parts; it is built once here, so that a bone its
     *                 animation needs and the file lacks is reported with the name of the file
     */
    public static LayerDefinition layer(String name, Function<ModelPart, ?> animated) {
        String file = "/assets/" + RR.MODID + "/models/entity/" + name + ".bbmodel";
        try {
            JsonObject project = read(file);
            if (!"modded_entity".equals(GsonHelper.getAsString(project.getAsJsonObject("meta"), "model_format"))
                    || !project.has("groups")) {
                throw new JsonParseException("expected a project of the format Modded Entity saved by Blockbench 5 or newer");
            }
            Map<String, JsonObject> nodes = new HashMap<>();
            for (String list : new String[]{"elements", "groups"}) {
                for (JsonElement node : project.getAsJsonArray(list)) {
                    nodes.put(GsonHelper.getAsString(node.getAsJsonObject(), "uuid"), node.getAsJsonObject());
                }
            }
            MeshDefinition mesh = new MeshDefinition();
            // The game has the feet of a model at y 24, Blockbench at 0.
            addBones(mesh.getRoot(), new double[]{0, 24, 0}, project.getAsJsonArray("outliner"), nodes);
            JsonObject resolution = project.getAsJsonObject("resolution");
            LayerDefinition layer = LayerDefinition.create(mesh, GsonHelper.getAsInt(resolution, "width"), GsonHelper.getAsInt(resolution, "height"));
            animated.apply(layer.bakeRoot());
            return layer;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(file.substring(1) + ": " + (e instanceof NoSuchElementException
                    ? e.getMessage() + ", a bone that the animation moves: keep its name and its place in the tree"
                    : e.getMessage()), e);
        }
    }

    private static JsonObject read(String file) throws IOException {
        try (InputStream stream = BlockbenchModel.class.getResourceAsStream(file)) {
            if (stream == null) {
                throw new IOException("no such file");
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    /** The bones of a list of the outliner as parts of {@code parent}, whose pivot is {@code pivot}. */
    private static void addBones(PartDefinition parent, double[] pivot, JsonArray list, Map<String, JsonObject> nodes) {
        for (JsonElement entry : list) {
            if (!entry.isJsonObject()) {
                throw new JsonParseException("cube '" + GsonHelper.getAsString(nodes.get(entry.getAsString()), "name") + "' is in no bone");
            }
            JsonObject bone = nodes.get(GsonHelper.getAsString(entry.getAsJsonObject(), "uuid"));
            if (!GsonHelper.getAsBoolean(bone, "export", true)) {
                continue;
            }
            double[] bonePivot = vector(bone, "origin");
            JsonArray children = entry.getAsJsonObject().getAsJsonArray("children");
            JsonArray bones = new JsonArray();
            JsonArray turned = new JsonArray();
            CubeListBuilder cubes = CubeListBuilder.create();
            for (JsonElement child : children) {
                if (child.isJsonObject()) {
                    bones.add(child);
                    continue;
                }
                JsonObject cube = nodes.get(child.getAsString());
                if (!GsonHelper.getAsBoolean(cube, "export", true)) {
                    continue;
                }
                if (cube.has("rotation") && !isZero(vector(cube, "rotation"))) {
                    turned.add(cube);
                } else {
                    addCube(cubes, cube, bonePivot);
                }
            }
            PartDefinition part = parent.addOrReplaceChild(GsonHelper.getAsString(bone, "name"), cubes, pose(bonePivot, pivot, bone));
            // A part turns as a whole, so a cube with a turn of its own is a part, as in Blockbench's export.
            for (int number = 0; number < turned.size(); number++) {
                JsonObject cube = turned.get(number).getAsJsonObject();
                double[] cubePivot = vector(cube, "origin");
                CubeListBuilder alone = CubeListBuilder.create();
                addCube(alone, cube, cubePivot);
                part.addOrReplaceChild(GsonHelper.getAsString(cube, "name") + "_r" + (number + 1), alone, pose(cubePivot, bonePivot, cube));
            }
            addBones(part, bonePivot, bones, nodes);
        }
    }

    private static void addCube(CubeListBuilder cubes, JsonObject cube, double[] pivot) {
        double[] from = vector(cube, "from");
        double[] to = vector(cube, "to");
        double[] uv = cube.has("uv_offset") ? vector(cube, "uv_offset") : new double[2];
        cubes.texOffs((int) uv[0], (int) uv[1])
                .mirror(GsonHelper.getAsBoolean(cube, "mirror_uv", false))
                .addBox((float) (pivot[0] - to[0]), (float) (pivot[1] - to[1]), (float) (from[2] - pivot[2]),
                        (float) (to[0] - from[0]), (float) (to[1] - from[1]), (float) (to[2] - from[2]),
                        new CubeDeformation(GsonHelper.getAsFloat(cube, "inflate", 0.0F)));
    }

    /** Where a bone or a turned cube with the pivot {@code pivot} stands and how it is turned, seen from its parent. */
    private static PartPose pose(double[] pivot, double[] parentPivot, JsonObject node) {
        double[] degrees = node.has("rotation") ? vector(node, "rotation") : new double[3];
        return PartPose.offsetAndRotation(
                (float) (parentPivot[0] - pivot[0]), (float) (parentPivot[1] - pivot[1]), (float) (pivot[2] - parentPivot[2]),
                (float) Math.toRadians(-degrees[0]), (float) Math.toRadians(-degrees[1]), (float) Math.toRadians(degrees[2]));
    }

    private static double[] vector(JsonObject node, String key) {
        JsonArray array = node.getAsJsonArray(key);
        double[] vector = new double[array.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = array.get(i).getAsDouble();
        }
        return vector;
    }

    private static boolean isZero(double[] vector) {
        for (double value : vector) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }
}
