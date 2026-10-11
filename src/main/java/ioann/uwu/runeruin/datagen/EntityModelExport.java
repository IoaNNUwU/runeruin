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
import org.joml.Vector3f;

/**
 * Writes every entity model layer, the vanilla ones included, for {@code scripts/render_entity_model.py}: the faces
 * of the boxes exactly as the game builds and poses them, so the script only has to draw. Runs with {@code runData}
 * and writes into {@code build/entity_models}, outside the resources; {@code build.gradle} keeps this class out of
 * the mod jar as well, so nothing else may refer to it.
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
            JsonArray quads = new JsonArray();
            layer.bakeRoot().visit(new PoseStack(), (pose, part, _, cube) -> {
                for (ModelPart.Polygon polygon : cube.polygons) {
                    JsonArray vertices = new JsonArray();
                    for (ModelPart.Vertex vertex : polygon.vertices()) {
                        Vector3f position = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                        vertices.add(array(position.x(), position.y(), position.z(), vertex.u(), vertex.v()));
                    }
                    Vector3f normal = pose.transformNormal(polygon.normal(), new Vector3f());
                    JsonObject quad = new JsonObject();
                    quad.addProperty("part", part);
                    quad.add("normal", array(normal.x(), normal.y(), normal.z()));
                    quad.add("vertices", vertices);
                    quads.add(quad);
                }
            });
            JsonObject model = new JsonObject();
            model.add("quads", quads);
            Path file = Path.of(directory, location.model().getNamespace(), location.model().getPath(), location.layer() + ".json");
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, model.toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static JsonArray array(float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(value);
        }
        return array;
    }
}
