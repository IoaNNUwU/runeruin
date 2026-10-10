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
 * Wispberry bush: a stem with leaf clumps of several sizes around it, which keeps it from looking like one
 * leafy box. A bush with berries carries them as cubes sunk a pixel into the clumps.
 */
public final class WispberryBushModels {

    private static final String[] SIDES = {"down", "up", "north", "south", "west", "east"};

    // Boxes as {from, to}, in pixels.
    private static final double[][][] SAPLING_STEM = {{{7, 0, 7}, {9, 2, 9}}};
    private static final double[][][] SAPLING_LEAVES = {{{5, 2, 5}, {10, 6, 10}}, {{8, 1, 9}, {12, 4, 12}}};
    private static final double[][][] STEM = {{{7, 0, 7}, {9, 6, 9}}, {{8, 2, 5}, {9, 3, 7}}, {{9, 3, 8}, {10, 4, 9}}};
    private static final double[][][] LEAVES = {
            {{5, 5, 4}, {12, 10, 11}},
            {{7, 9, 5}, {11, 12, 9}},
            {{1, 1, 7}, {7, 6, 13}},
            {{10, 2, 8}, {15, 6, 14}},
            {{6, 1, 1}, {11, 4, 5}}
    };

    /**
     * Berries as x, y, z of the low corner and the size of a ripe one. No face of a berry may lie in the plane
     * of a clump face that looks the same way: the two would flicker.
     */
    private static final int[][] BERRIES = {{8, 6, 10, 3}, {2, 5, 9, 2}, {8, 3, 2, 2}, {0, 2, 10, 2}, {14, 3, 10, 2}, {4, 9, 6, 2}, {11, 3, 4, 2}};

    /** An unripe bush has the first few of them, each two pixels wide. */
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
     * block/wispberry_bush_berries.png holds one tile per kind of berry in its top row: ripe 3x3 at u 0, ripe 2x2
     * at u 4 and unripe 2x2 at u 8. Ripe berries glow.
     */
    private static void addBerry(JsonArray elements, int[] berry, boolean ripe) {
        int size = ripe ? berry[3] : 2;
        int u = !ripe ? 8 : size == 3 ? 0 : 4;
        addElement(elements, "berry", new double[]{berry[0], berry[1], berry[2]},
                new double[]{berry[0] + size, berry[1] + size, berry[2] + size}, "berries", ripe ? 10 : 0, SIDES);
        JsonObject faces = elements.get(elements.size() - 1).getAsJsonObject().getAsJsonObject("faces");
        for (String side : SIDES) {
            faces.getAsJsonObject(side).add("uv", vector(u, 0, u + size, size));
        }
    }
}
