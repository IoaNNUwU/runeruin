package ioann.uwu.runeruin.datagen.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Builders for hand-written block model JSON. */
final class ModelJson {

    private ModelJson() {}

    static void addElement(
            JsonArray elements,
            String name,
            double[] from,
            double[] to,
            String texture,
            int lightEmission,
            String... directions
    ) {
        JsonObject element = new JsonObject();
        // Minecraft ignores unknown element fields; the analysis skill uses this stable label.
        element.addProperty("name", name);
        element.add("from", vector(from));
        element.add("to", vector(to));
        if (lightEmission > 0) {
            element.addProperty("light_emission", lightEmission);
        }

        JsonObject faces = new JsonObject();
        for (String direction : directions) {
            JsonObject face = new JsonObject();
            face.addProperty("texture", "#" + texture);
            faces.add(direction, face);
        }
        element.add("faces", faces);
        elements.add(element);
    }

    static JsonArray vector(double... coordinates) {
        JsonArray result = new JsonArray();
        for (double coordinate : coordinates) {
            result.add(coordinate);
        }
        return result;
    }

    static void rotateY(JsonElement element, double angle) {
        JsonObject rotation = new JsonObject();
        rotation.add("origin", vector(8, 8, 8));
        rotation.addProperty("axis", "y");
        rotation.addProperty("angle", angle);
        element.getAsJsonObject().add("rotation", rotation);
    }

    static JsonObject horizontalPlane(double y, JsonObject down, JsonObject up) {
        JsonObject element = new JsonObject();
        element.add("from", vector(0.0, y, 0.0));
        element.add("to", vector(16.0, y, 16.0));
        JsonObject faces = new JsonObject();
        faces.add("down", down);
        faces.add("up", up);
        element.add("faces", faces);
        return element;
    }

    static JsonObject planeFace(double u0, double v0, double u1, double v1) {
        JsonObject face = new JsonObject();
        face.add("uv", vector(u0, v0, u1, v1));
        face.addProperty("texture", "#texture");
        face.addProperty("tintindex", 0);
        return face;
    }
}
