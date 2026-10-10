package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.addElement;
import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WispberryBushBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * Wispberry bush: a stem under one big box of leaves that reaches the top of the block, as an azalea has, and a
 * smaller box at each of two opposite lower corners, a pixel up its sides. A bush with berries carries them as
 * cubes sunk into the boxes.
 */
public final class WispberryBushModels {

    private static final String[] SIDES = {"down", "up", "north", "south", "west", "east"};

    // Boxes as {from, to}, in pixels.
    private static final double[][][] SAPLING_STEM = {{{7, 0, 7}, {9, 4, 9}}};
    private static final double[][][] SAPLING_LEAVES = {{{5, 3, 5}, {11, 9, 11}}, {{3, 0, 3}, {7, 4, 7}}};
    private static final double[][][] STEM = {{{7, 0, 7}, {9, 6, 9}}};
    private static final double[][][] LEAVES = {{{3, 5, 3}, {13, 16, 13}}, {{1, 0, 1}, {7, 6, 7}}, {{10, 1, 10}, {15, 6, 15}}};

    /**
     * Berries as x, y, z of the low corner and the size of a ripe one. Two faces of the model that overlap in
     * one plane flicker, so no berry, ripe or unripe, shares a plane with a box of leaves or another berry.
     */
    private static final int[][] BERRIES = {{4, 10, 11, 4}, {12, 9, 5, 3}, {8, 14, 5, 4}, {1, 11, 6, 4}, {4, 4, 0, 4}, {12, 4, 12, 4}, {9, 11, 1, 3}};

    /** An unripe bush has the first few of them, each a pixel smaller. */
    private static final int UNRIPE_BERRIES = 3;

    private WispberryBushModels() {}

    /** One model per age, turned a random quarter: no two bushes side by side look the same way. */
    public static void createWispberryBush(@NonNull BlockModelGenerators blockModels) {
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(RRBlocks.WISPBERRY_BUSH.get())
                .with(PropertyDispatch.initial(WispberryBushBlock.AGE).generate(age -> {
                    Identifier id = RR.id("block/wispberry_bush_stage" + age);
                    JsonObject model = model(age);
                    blockModels.modelOutput.accept(id, () -> model);
                    return BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(id));
                })));
    }

    private static JsonObject model(int age) {
        JsonArray elements = new JsonArray();
        for (double[][] box : age == 0 ? SAPLING_STEM : STEM) {
            addElement(elements, "stem", box[0], box[1], "stem", 0, SIDES);
        }
        for (double[][] box : age == 0 ? SAPLING_LEAVES : LEAVES) {
            addElement(elements, "leaves", box[0], box[1], "leaves", 0, SIDES);
        }
        int berries = age == 3 ? BERRIES.length : age == 2 ? UNRIPE_BERRIES : 0;
        for (int number = 0; number < berries; number++) {
            addBerry(elements, BERRIES[number], age == 3);
        }
        JsonObject model = new JsonObject();
        // No smooth lighting: it is meant for block faces, not for boxes this small.
        model.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "runeruin:block/wispberry_bush");
        textures.addProperty("leaves", "runeruin:block/wispberry_bush");
        textures.addProperty("stem", "runeruin:block/wispberry_bush_stem");
        textures.addProperty("berries", "runeruin:block/wispberry_bush_berries");
        model.add("textures", textures);
        model.add("elements", elements);
        return model;
    }

    /**
     * block/wispberry_bush_berries.png holds one tile per kind of berry in its top row: ripe 4x4 at u 0, ripe 3x3
     * at u 5, unripe 3x3 at u 9 and unripe 2x2 at u 13. Ripe berries glow.
     */
    private static void addBerry(JsonArray elements, int[] berry, boolean ripe) {
        int size = ripe ? berry[3] : berry[3] - 1;
        int u = ripe ? (size == 4 ? 0 : 5) : (size == 3 ? 9 : 13);
        addElement(elements, "berry", new double[]{berry[0], berry[1], berry[2]},
                new double[]{berry[0] + size, berry[1] + size, berry[2] + size}, "berries", ripe ? 10 : 0, SIDES);
        JsonObject faces = elements.get(elements.size() - 1).getAsJsonObject().getAsJsonObject("faces");
        for (String side : SIDES) {
            faces.getAsJsonObject(side).add("uv", vector(u, 0, u + size, size));
        }
    }
}
