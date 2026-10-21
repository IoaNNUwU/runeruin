package ioann.uwu.runeruin.entities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ioann.uwu.runeruin.RR;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The bones of a mob as the server sees them: a Blockbench project read the way the client builds the model from it
 * ({@code BlockbenchModel}) and its animations sampled the way the game plays the exported ones
 * ({@code KeyframeAnimation}). The server has neither, but the parts of the Colossus that players stand on
 * have to follow the moving model, so they follow the same file.
 *
 * <p>A pose is the offset of every bone from its place in the project, as {@code ModelPart} offsets: position in
 * pixels, then rotation in radians. Turned cubes count as unturned; a part only needs the box around them.</p>
 */
public final class ColossusRig {
    private final List<Bone> bones = new ArrayList<>();
    // Every cube as ModelPart builds it, in the space of its bone: min x, y, z, max x, y, z in pixels.
    private final List<float[]> cubes = new ArrayList<>();
    private final List<Integer> cubeBones = new ArrayList<>();
    private final Map<String, Animation> animations = new HashMap<>();

    private record Bone(String name, int parent, Vector3f offset, Vector3f rotation) {}

    private record Key(float time, Vector3f value, boolean catmullrom) {}

    private record Channel(int bone, boolean rotation, Key[] keys) {}

    /** @param impact when the fists hit, the time of a timeline keyframe "impact" in the project, or -1 */
    public record Animation(String name, float length, boolean looping, List<Channel> channels, float impact) {}

    public static ColossusRig load(String name) {
        String file = "/assets/" + RR.MODID + "/models/entity/" + name + ".bbmodel";
        try (InputStream stream = ColossusRig.class.getResourceAsStream(file)) {
            if (stream == null) {
                throw new IOException("no such file");
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new ColossusRig(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(file.substring(1), e);
        }
    }

    private ColossusRig(JsonObject project) {
        Map<String, JsonObject> nodes = new HashMap<>();
        for (String list : new String[]{"elements", "groups"}) {
            for (JsonElement node : project.getAsJsonArray(list)) {
                nodes.put(GsonHelper.getAsString(node.getAsJsonObject(), "uuid"), node.getAsJsonObject());
            }
        }
        Map<String, Integer> byUuid = new HashMap<>();
        // The game has the feet of a model at y 24, Blockbench at 0.
        this.addBones(project.getAsJsonArray("outliner"), -1, new float[]{0, 24, 0}, nodes, byUuid);
        for (JsonElement element : project.getAsJsonArray("animations")) {
            JsonObject json = element.getAsJsonObject();
            List<Channel> channels = new ArrayList<>();
            float impact = -1;
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("animators").entrySet()) {
                JsonObject animator = entry.getValue().getAsJsonObject();
                if ("effect".equals(GsonHelper.getAsString(animator, "type", ""))) {
                    for (JsonElement key : animator.getAsJsonArray("keyframes")) {
                        JsonObject point = key.getAsJsonObject().getAsJsonArray("data_points").get(0).getAsJsonObject();
                        if ("impact".equals(GsonHelper.getAsString(point, "script", "").strip())) {
                            impact = GsonHelper.getAsFloat(key.getAsJsonObject(), "time");
                        }
                    }
                    continue;
                }
                Integer bone = byUuid.get(entry.getKey());
                if (bone == null) {
                    continue;
                }
                for (boolean rotation : new boolean[]{true, false}) {
                    List<Key> keys = new ArrayList<>();
                    for (JsonElement key : animator.getAsJsonArray("keyframes")) {
                        JsonObject frame = key.getAsJsonObject();
                        if (!GsonHelper.getAsString(frame, "channel").equals(rotation ? "rotation" : "position")) {
                            continue;
                        }
                        JsonObject point = frame.getAsJsonArray("data_points").get(0).getAsJsonObject();
                        // Blockbench writes the values as text, they may be numbers too.
                        float x = point.get("x").getAsFloat();
                        float y = point.get("y").getAsFloat();
                        float z = point.get("z").getAsFloat();
                        // As Blockbench exports them: KeyframeAnimations.degreeVec(-x, -y, z) and posVec(-x, y, z).
                        Vector3f value = rotation
                                ? new Vector3f(-x, -y, z).mul(Mth.DEG_TO_RAD)
                                : new Vector3f(-x, -y, z);
                        keys.add(new Key(GsonHelper.getAsFloat(frame, "time"), value,
                                "catmullrom".equals(GsonHelper.getAsString(frame, "interpolation", "linear"))));
                    }
                    if (!keys.isEmpty()) {
                        keys.sort(Comparator.comparingDouble(Key::time));
                        channels.add(new Channel(bone, rotation, keys.toArray(Key[]::new)));
                    }
                }
            }
            String name = GsonHelper.getAsString(json, "name");
            this.animations.put(name, new Animation(name, GsonHelper.getAsFloat(json, "length"),
                    "loop".equals(GsonHelper.getAsString(json, "loop", "once")), channels, impact));
        }
    }

    private void addBones(JsonArray list, int parent, float[] parentPivot, Map<String, JsonObject> nodes, Map<String, Integer> byUuid) {
        for (JsonElement entry : list) {
            if (!entry.isJsonObject()) {
                continue;
            }
            String uuid = GsonHelper.getAsString(entry.getAsJsonObject(), "uuid");
            JsonObject bone = nodes.get(uuid);
            if (!GsonHelper.getAsBoolean(bone, "export", true)) {
                continue;
            }
            float[] pivot = vector(bone, "origin");
            float[] degrees = bone.has("rotation") ? vector(bone, "rotation") : new float[3];
            int index = this.bones.size();
            JsonArray children = new JsonArray();
            for (JsonElement child : entry.getAsJsonObject().getAsJsonArray("children")) {
                if (child.isJsonObject()) {
                    children.add(child);
                    continue;
                }
                JsonObject cube = nodes.get(child.getAsString());
                if (!GsonHelper.getAsBoolean(cube, "export", true)) {
                    continue;
                }
                float[] from = vector(cube, "from");
                float[] to = vector(cube, "to");
                float grow = GsonHelper.getAsFloat(cube, "inflate", 0.0F);
                // Where ModelPart puts the box: addBox(pivot.x - to.x, pivot.y - to.y, from.z - pivot.z, size)
                this.cubes.add(new float[]{
                        pivot[0] - to[0] - grow, pivot[1] - to[1] - grow, from[2] - pivot[2] - grow,
                        pivot[0] - from[0] + grow, pivot[1] - from[1] + grow, to[2] - pivot[2] + grow});
                this.cubeBones.add(index);
            }
            byUuid.put(uuid, index);
            this.bones.add(new Bone(GsonHelper.getAsString(bone, "name"), parent,
                    new Vector3f(parentPivot[0] - pivot[0], parentPivot[1] - pivot[1], pivot[2] - parentPivot[2]),
                    new Vector3f(-degrees[0], -degrees[1], degrees[2]).mul(Mth.DEG_TO_RAD)));
            this.addBones(children, index, pivot, nodes, byUuid);
        }
    }

    private static float[] vector(JsonObject node, String key) {
        JsonArray array = node.getAsJsonArray(key);
        float[] vector = new float[array.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = array.get(i).getAsFloat();
        }
        return vector;
    }

    public Animation animation(String name) {
        Animation animation = this.animations.get(name);
        if (animation == null) {
            throw new IllegalArgumentException("the project has no animation '" + name + "'");
        }
        return animation;
    }

    public int bone(String name) {
        for (int i = 0; i < this.bones.size(); i++) {
            if (this.bones.get(i).name().equals(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException("the project has no bone '" + name + "', a bone that the colossus moves");
    }

    public int boneCount() {
        return this.bones.size();
    }

    public String boneName(int bone) {
        return this.bones.get(bone).name();
    }

    public int cubeCount() {
        return this.cubes.size();
    }

    public int cubeBone(int cube) {
        return this.cubeBones.get(cube);
    }

    /** A cube in the space of its bone, in pixels as ModelPart builds it: min x, y, z, max x, y, z. */
    public float[] cube(int cube) {
        return this.cubes.get(cube).clone();
    }

    /** A pose of no animation: every bone where the project puts it. */
    public float[][] restPose() {
        return new float[this.bones.size()][6];
    }

    /**
     * Adds an animation at a time to a pose, as {@code KeyframeAnimation.apply(millis, weight)} adds it to the parts:
     * a looping one wraps around, any other stays on its last frame.
     */
    public void apply(Animation animation, float seconds, float weight, float[][] pose) {
        float time = animation.looping() ? seconds % animation.length() : seconds;
        Vector3f value = new Vector3f();
        for (Channel channel : animation.channels()) {
            Key[] keys = channel.keys();
            int prev = Math.max(0, Mth.binarySearch(0, keys.length, i -> time <= keys[i].time()) - 1);
            int next = Math.min(keys.length - 1, prev + 1);
            float alpha = next != prev ? Mth.clamp((time - keys[prev].time()) / (keys[next].time() - keys[prev].time()), 0.0F, 1.0F) : 0.0F;
            if (keys[next].catmullrom()) {
                Vector3f p0 = keys[Math.max(0, prev - 1)].value();
                Vector3f p1 = keys[prev].value();
                Vector3f p2 = keys[next].value();
                Vector3f p3 = keys[Math.min(keys.length - 1, next + 1)].value();
                value.set(Mth.catmullrom(alpha, p0.x, p1.x, p2.x, p3.x),
                        Mth.catmullrom(alpha, p0.y, p1.y, p2.y, p3.y),
                        Mth.catmullrom(alpha, p0.z, p1.z, p2.z, p3.z));
            } else {
                keys[prev].value().lerp(keys[next].value(), alpha, value);
            }
            float[] offsets = pose[channel.bone()];
            int at = channel.rotation() ? 3 : 0;
            offsets[at] += value.x * weight;
            offsets[at + 1] += value.y * weight;
            offsets[at + 2] += value.z * weight;
        }
    }

    /** The bones and the cubes of a pose, relative to the feet of the mob: where they stand in the world. */
    public record Posed(Matrix4f[] bones, AABB[] cubes) {}

    /**
     * Puts the bones as {@code LivingEntityRenderer} puts the model: turned by the body, turned over and scaled. Every
     * cube gets the box around it, its top lowered to the middle of its upper face: on a turned cube that box would
     * reach up to the highest corner, and one would stand in the air over the middle of the face.
     */
    public Posed posed(float[][] pose, float bodyRot, float scale) {
        Matrix4f[] matrices = new Matrix4f[this.bones.size()];
        Matrix4f entity = new Matrix4f()
                .rotateY((180.0F - bodyRot) * Mth.DEG_TO_RAD)
                .scale(-scale, -scale, scale)
                .translate(0.0F, -1.501F, 0.0F);
        AABB[] boxes = new AABB[this.cubes.size()];
        Vector3f point = new Vector3f();
        for (int i = 0; i < this.bones.size(); i++) {
            Bone bone = this.bones.get(i);
            float[] offsets = pose[i];
            matrices[i] = new Matrix4f(bone.parent() < 0 ? entity : matrices[bone.parent()])
                    .translate((bone.offset().x + offsets[0]) / 16.0F, (bone.offset().y + offsets[1]) / 16.0F, (bone.offset().z + offsets[2]) / 16.0F)
                    .rotate(new Quaternionf().rotationZYX(bone.rotation().z + offsets[5], bone.rotation().y + offsets[4], bone.rotation().x + offsets[3]));
        }
        for (int c = 0; c < this.cubes.size(); c++) {
            float[] cube = this.cubes.get(c);
            Matrix4f matrix = matrices[this.cubeBones.get(c)];
            double[] bounds = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            for (int corner = 0; corner < 8; corner++) {
                matrix.transformPosition(
                        ((corner & 1) == 0 ? cube[0] : cube[3]) / 16.0F,
                        ((corner & 2) == 0 ? cube[1] : cube[4]) / 16.0F,
                        ((corner & 4) == 0 ? cube[2] : cube[5]) / 16.0F, point);
                for (int axis = 0; axis < 3; axis++) {
                    bounds[axis] = Math.min(bounds[axis], point.get(axis));
                    bounds[axis + 3] = Math.max(bounds[axis + 3], point.get(axis));
                }
            }
            double top = -Double.MAX_VALUE;
            for (int face = 0; face < 6; face++) {
                float[] center = {(cube[0] + cube[3]) / 2.0F, (cube[1] + cube[4]) / 2.0F, (cube[2] + cube[5]) / 2.0F};
                center[face % 3] = cube[face % 3 + (face < 3 ? 0 : 3)];
                matrix.transformPosition(center[0] / 16.0F, center[1] / 16.0F, center[2] / 16.0F, point);
                top = Math.max(top, point.y);
            }
            boxes[c] = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], top, bounds[5]);
        }
        return new Posed(matrices, boxes);
    }

    /** The box around all cubes of a bone in a pose. */
    public AABB boneBox(Posed posed, int bone) {
        AABB box = null;
        for (int c = 0; c < this.cubes.size(); c++) {
            if (this.cubeBones.get(c) == bone) {
                box = box == null ? posed.cubes()[c] : box.minmax(posed.cubes()[c]);
            }
        }
        if (box == null) {
            throw new IllegalArgumentException("the bone '" + this.boneName(bone) + "' has no cubes");
        }
        return box;
    }
}
