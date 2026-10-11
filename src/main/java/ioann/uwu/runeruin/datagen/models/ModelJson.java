package ioann.uwu.runeruin.datagen.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The models a person keeps as files in {@code assets/runeruin/models} and edits in Blockbench. Data generation
 * builds no geometry: it only reads such a file and derives other models from it.
 */
final class ModelJson {

    private ModelJson() {}

    static JsonObject read(ResourceManager resources, Identifier model) {
        try (Reader reader = resources.openAsReader(model.withPath(path -> "models/" + path + ".json"))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The model {@code lift} pixels higher. The game takes the texture area of a side face without a "uv" from
     * where its element stands, so such a face gets the area it has in the file: the texture moves with the model.
     */
    static JsonObject lifted(JsonObject model, double lift) {
        JsonObject lifted = model.deepCopy();
        for (JsonElement entry : lifted.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            double[] min = {from.get(0).getAsDouble(), from.get(1).getAsDouble(), from.get(2).getAsDouble()};
            double[] max = {to.get(0).getAsDouble(), to.get(1).getAsDouble(), to.get(2).getAsDouble()};
            for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
                double[] u = switch (face.getKey()) {
                    case "north" -> new double[]{16 - max[0], 16 - min[0]};
                    case "south" -> new double[]{min[0], max[0]};
                    case "west" -> new double[]{min[2], max[2]};
                    case "east" -> new double[]{16 - max[2], 16 - min[2]};
                    default -> null;
                };
                if (u != null && !face.getValue().getAsJsonObject().has("uv")) {
                    JsonArray uv = new JsonArray();
                    for (double value : new double[]{u[0], 16 - max[1], u[1], 16 - min[1]}) {
                        uv.add(value);
                    }
                    face.getValue().getAsJsonObject().add("uv", uv);
                }
            }
            from.set(1, new JsonPrimitive(min[1] + lift));
            to.set(1, new JsonPrimitive(max[1] + lift));
            if (element.has("rotation")) {
                JsonArray origin = element.getAsJsonObject("rotation").getAsJsonArray("origin");
                origin.set(1, new JsonPrimitive(origin.get(1).getAsDouble() + lift));
            }
        }
        return lifted;
    }
}
