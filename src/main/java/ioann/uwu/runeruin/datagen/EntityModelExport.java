package ioann.uwu.runeruin.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import ioann.uwu.runeruin.RR;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.joml.Matrix4f;

/**
 * Writes every entity model layer, the vanilla ones included, for {@code scripts/render_entity_model.py}: the tree
 * of parts with their initial poses and the faces of their boxes as the game builds them. The script poses the
 * parts itself, so that a pose can be changed there; the matrix the game gives every part lets it check that it
 * poses them as the game does. Runs with {@code runData} and writes into {@code build/entity_models}, outside the
 * resources; {@code build.gradle} keeps this class out of the mod jar as well, so nothing else may refer to it.
 */
@EventBusSubscriber(modid = RR.MODID, value = Dist.CLIENT)
public final class EntityModelExport {

    @SubscribeEvent
    static void export(GatherDataEvent.Client event) {
        String directory = System.getProperty("runeruin.entityModels");
        if (directory == null) {
            return;
        }
        // Mods register their layers in this event, which only a starting client fires.
        ModLoader.postEvent(new EntityRenderersEvent.RegisterLayerDefinitions());
        LayerDefinitions.createRoots().forEach((location, layer) -> {
            ModelPart root = layer.bakeRoot();
            JsonObject parts = new JsonObject();
            root.visit(new PoseStack(), (pose, path, _, cube) -> {
                JsonObject part = part(parts, root, path);
                Matrix4f matrix = pose.pose();
                part.add("matrix", array(
                        matrix.m00(), matrix.m10(), matrix.m20(), matrix.m30(),
                        matrix.m01(), matrix.m11(), matrix.m21(), matrix.m31(),
                        matrix.m02(), matrix.m12(), matrix.m22(), matrix.m32()));
                for (ModelPart.Polygon polygon : cube.polygons) {
                    JsonArray vertices = new JsonArray();
                    for (ModelPart.Vertex vertex : polygon.vertices()) {
                        vertices.add(array(vertex.x(), vertex.y(), vertex.z(), vertex.u(), vertex.v()));
                    }
                    JsonObject quad = new JsonObject();
                    quad.add("normal", array(polygon.normal().x(), polygon.normal().y(), polygon.normal().z()));
                    quad.add("vertices", vertices);
                    part.getAsJsonArray("quads").add(quad);
                }
            });
            JsonObject model = new JsonObject();
            model.add("parts", parts);
            Path file = Path.of(directory, location.model().getNamespace(), location.model().getPath(), location.layer() + ".json");
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, model.toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /** The part at a path of {@link ModelPart#visit}, {@code ""} for the root; parents are added before it. */
    private static JsonObject part(JsonObject parts, ModelPart root, String path) {
        if (!parts.has(path)) {
            ModelPart part = root;
            if (!path.isEmpty()) {
                part(parts, root, path.substring(0, path.lastIndexOf('/')));
                for (String name : path.substring(1).split("/")) {
                    part = part.getChild(name);
                }
            }
            JsonObject json = new JsonObject();
            json.add("pose", array(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale));
            json.add("quads", new JsonArray());
            parts.add(path, json);
        }
        return parts.getAsJsonObject(path);
    }

    private static JsonArray array(float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(value);
        }
        return array;
    }
}
