package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.horizontalPlane;
import static ioann.uwu.runeruin.datagen.models.ModelJson.planeFace;
import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.MossberryBushBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

/**
 * Mossberry bush: a small crossed bush that is always there and, switched on by their properties, the twigs
 * around it and one cube per berry.
 */
public final class MossberryBushModels {

    private static final String BUSH = "runeruin:block/mossberry_bush";

    private MossberryBushModels() {}

    public static void createMossberryBush(@NonNull BlockModelGenerators blockModels) {
        Block block = RRBlocks.MOSSBERRY_BUSH.get();
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(block);
        blockState.with(BlockModelGenerators.plainVariant(ModelTemplates.CROSS.create(block, TextureMapping.cross(block), blockModels.modelOutput)));

        Identifier twigs = RR.id("block/mossberry_bush_twigs");
        JsonObject twigsModel = twigsModel();
        blockModels.modelOutput.accept(twigs, () -> twigsModel);
        blockState.with(new ConditionBuilder().term(MossberryBushBlock.TWIGS, true), BlockModelGenerators.plainVariant(twigs));

        for (int number = 0; number < MossberryBushBlock.BERRIES.size(); number++) {
            MossberryBushBlock.Berry berry = MossberryBushBlock.BERRIES.get(number);
            Identifier id = RR.id("block/mossberry_bush_berry" + number);
            JsonObject berryModel = berryModel(berry);
            blockModels.modelOutput.accept(id, () -> berryModel);
            blockState.with(new ConditionBuilder().term(berry.property(), true), BlockModelGenerators.plainVariant(id));
        }
        blockModels.blockStateOutput.accept(blockState);
    }

    /** Twigs lie flat half a pixel above the ground, as a water lily flower lies above its pad. */
    private static JsonObject twigsModel() {
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", BUSH);
        textures.addProperty("texture", "runeruin:block/mossberry_bush_twigs");
        model.add("textures", textures);
        JsonObject down = planeFace(0, 16, 16, 0);
        JsonObject up = planeFace(0, 0, 16, 16);
        down.remove("tintindex");
        up.remove("tintindex");
        JsonArray elements = new JsonArray();
        elements.add(horizontalPlane(0.5, down, up));
        model.add("elements", elements);
        return model;
    }

    /**
     * One cube, leaning away from the middle of the block: side by side the cubes read as a dome. Two upright
     * planes through its middle reach a pixel out of it on every side: what is drawn there shows as spikes.
     */
    private static JsonObject berryModel(MossberryBushBlock.Berry berry) {
        double half = berry.size() / 2.0;
        double rim = half + 1;
        double[] center = {berry.x(), berry.lift(), berry.z()};
        JsonObject rotation = new JsonObject();
        rotation.add("origin", vector(center));
        // The game turns around x first: lean towards +z, then swing that lean to point away from the middle.
        rotation.addProperty("x", berry.tilt());
        rotation.addProperty("y", Math.round(Math.toDegrees(Math.atan2(berry.x() - 8, berry.z() - 8))));
        JsonArray elements = new JsonArray();
        elements.add(element(center, half, half, half, rotation, tile(berry.size(), false, false), "down", "up", "north", "south", "west", "east"));
        elements.add(spikePlane(center, rim, rim, 0, rotation, berry.size(), "north", "south"));
        elements.add(spikePlane(center, 0, rim, rim, rotation, berry.size(), "west", "east"));
        JsonObject model = new JsonObject();
        // No smooth lighting: it is meant for block faces, not for cubes this small and tilted.
        model.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", BUSH);
        textures.addProperty("berries", "runeruin:block/mossberry_bush_berries");
        model.add("textures", textures);
        model.add("elements", elements);
        return model;
    }

    private static JsonObject element(double[] center, double x, double y, double z, JsonObject rotation,
                                      JsonObject face, String... sides) {
        JsonObject element = new JsonObject();
        element.add("from", vector(center[0] - x, center[1] - y, center[2] - z));
        element.add("to", vector(center[0] + x, center[1] + y, center[2] + z));
        element.add("rotation", rotation);
        JsonObject faces = new JsonObject();
        for (String side : sides) {
            faces.add(side, face);
        }
        element.add("faces", faces);
        return element;
    }

    /** A plane seen from both sides. Its back face mirrors the tile: unmirrored, it would show the spikes in other places than the front. */
    private static JsonObject spikePlane(double[] center, double x, double y, double z, JsonObject rotation,
                                         int size, String front, String back) {
        JsonObject plane = element(center, x, y, z, rotation, tile(size, true, false), front);
        plane.getAsJsonObject("faces").add(back, tile(size, true, true));
        return plane;
    }

    /**
     * block/mossberry_bush_berries.png, per cube size a row of two tiles: the cube face at u 0, as wide as the cube, and the
     * spike plane at u 4, two pixels wider. The rows start at v 0 (4x4), 6 (3x3) and 11 (2x2).
     */
    private static JsonObject tile(int size, boolean spikes, boolean mirrored) {
        int u = spikes ? 4 : 0;
        int v = size == 4 ? 0 : size == 3 ? 6 : 11;
        int width = spikes ? size + 2 : size;
        JsonObject face = new JsonObject();
        face.add("uv", mirrored ? vector(u + width, v, u, v + width) : vector(u, v, u + width, v + width));
        face.addProperty("texture", "#berries");
        return face;
    }
}
