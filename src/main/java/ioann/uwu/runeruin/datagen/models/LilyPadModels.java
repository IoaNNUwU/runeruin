package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.addElement;
import static ioann.uwu.runeruin.datagen.models.ModelJson.horizontalPlane;
import static ioann.uwu.runeruin.datagen.models.ModelJson.planeFace;
import static ioann.uwu.runeruin.datagen.models.ModelJson.rotateY;
import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.BigLilyPadBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WaterLilyLeafBlock;
import ioann.uwu.runeruin.client.WaterLilyStemTexture;
import java.util.List;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.NonNull;

/** Big lily pads and the water lily (root, leaf, flower). */
public final class LilyPadModels {

    private static final double ITEM_STEM_SCALE = 0.35;
    private static final double ITEM_LEAF_HALF = 3.6;

    private LilyPadModels() {}

    public static void createBigLilyPad(@NonNull BlockModelGenerators blockModels) {
        for (BigLilyPadBlock.Part part : BigLilyPadBlock.Part.values()) {
            if (part != BigLilyPadBlock.Part.SINGLE) {
                createBigLilyPartModel(blockModels, part);
            }
        }

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.BIG_LILY_PAD.get())
                        .with(PropertyDispatch.initial(BigLilyPadBlock.PART, BigLilyPadBlock.FACING)
                                .generate(LilyPadModels::orientedBigLilyVariant)
                        )
        );

        // The item is always the ordinary/single form. Its tint is the
        // default foliage color; the placed block is tinted from its biome.
        Identifier itemModel = blockModels.createFlatItemModelWithBlockTexture(
                RRBlocks.BIG_LILY_PAD.get().asItem(),
                Blocks.LILY_PAD
        );
        blockModels.registerSimpleTintedItemModel(
                RRBlocks.BIG_LILY_PAD.get(),
                itemModel,
                ItemModelUtils.constantTint(-12012264)
        );
    }

    public static void createWaterLily(@NonNull BlockModelGenerators blockModels) {
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/dark_oak_log");
        textures.addProperty("rhizome", "minecraft:block/dark_oak_log");
        textures.addProperty("roots", "minecraft:block/hanging_roots");
        model.add("textures", textures);

        JsonArray elements = new JsonArray();
        addElement(elements, "rhizome", new double[]{5, 9, 5}, new double[]{11, 13, 11}, "rhizome", 0,
                "north", "south", "east", "west", "up", "down");
        // Two tiers of hair roots hang from the rhizome below the block in an X,
        // like on a real water lily. Stems run in planes through the root axis
        // towards whole-block offsets, never at 40 or 50 degrees, so the root
        // planes never z-fight a stem.
        for (double[] plane : new double[][]{{-7, 40}, {-7, -50}, {-15, -40}, {-15, 50}}) {
            addElement(elements, "roots", new double[]{0, plane[0], 8}, new double[]{16, plane[0] + 16, 8},
                    "roots", 0, "north", "south");
            rotateY(elements.get(elements.size() - 1), plane[1]);
        }
        for (int i = 1; i < elements.size(); i++) {
            for (var face : elements.get(i).getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                face.getValue().getAsJsonObject().add("uv", vector(new double[]{0, 0, 16, 16}));
            }
        }
        model.add("elements", elements);

        Identifier rootModel = RR.id("block/water_lily_root");
        blockModels.modelOutput.accept(rootModel, () -> model);
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(RRBlocks.WATER_LILY_ROOT.get(), BlockModelGenerators.plainVariant(rootModel))
        );

        // The item shows a young plant: the rhizome, short roots and five small
        // leaves on stems. The block parent gives it the usual block rotation.
        JsonObject item = new JsonObject();
        item.addProperty("parent", "minecraft:block/block");
        JsonObject itemTextures = textures.deepCopy();
        itemTextures.addProperty("pad", "minecraft:block/lily_pad");
        itemTextures.addProperty("stem", "runeruin:block/water_lily_stems");
        item.add("textures", itemTextures);
        JsonArray itemElements = new JsonArray();
        addElement(itemElements, "rhizome", new double[]{5, 8, 5}, new double[]{11, 11, 11}, "rhizome", 0,
                "north", "south", "east", "west", "up", "down");
        // Seen from the item's diagonal angle, axis-aligned planes read as an X.
        addElement(itemElements, "roots", new double[]{2, 0, 8}, new double[]{14, 8, 8}, "roots", 0, "north", "south");
        addElement(itemElements, "roots", new double[]{8, 0, 2}, new double[]{8, 8, 14}, "roots", 0, "east", "west");
        // Three leaves around the root on flat U-shaped stems and two higher ones on
        // S-shaped stems, all cut from the stem sheet at ITEM_STEM_SCALE model pixels
        // per texel. Each stem is drawn along x (mirrored by side) and turned
        // together with its leaf; no two stems or root planes share a plane, and
        // distinct heights keep overlapping leaves from z-fighting.
        // Each stem: side along x, turn, start height, then the sheet cell: dx, dz, dy, variant.
        for (double[] stem : new double[][]{
                {1, 45, 8.5, 1, 1, 2, 0}, {-1, 45, 10, 1, 1, 1, 0}, {-1, -45, 7.5, 1, 1, 2, 2},
                {1, -15, 9.5, 1, 0, 2, 1}, {-1, 15, 9, 1, 0, 2, 2}}) {
            int dx = (int) stem[3];
            int dz = (int) stem[4];
            int dy = (int) stem[5];
            itemElements.add(itemStem(stem[0], stem[2], dx, dz, dy, (int) stem[6]));
            rotateY(itemElements.get(itemElements.size() - 1), stem[1]);
            double leafX = 8 + stem[0] * Math.sqrt(dx * dx + dz * dz) * 16 * ITEM_STEM_SCALE;
            double height = stem[2] + (dy * 16 - 13) * ITEM_STEM_SCALE;
            addElement(itemElements, "leaf", new double[]{leafX - ITEM_LEAF_HALF, height, 8 - ITEM_LEAF_HALF},
                    new double[]{leafX + ITEM_LEAF_HALF, height, 8 + ITEM_LEAF_HALF}, "pad", 0, "up", "down");
            rotateY(itemElements.get(itemElements.size() - 1), stem[1]);
        }
        for (var element : itemElements) {
            String name = element.getAsJsonObject().get("name").getAsString();
            double[] uv = switch (name) {
                case "roots" -> new double[]{2, 0, 14, 8};
                case "leaf" -> new double[]{0, 0, 16, 16};
                default -> null;
            };
            for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                if (uv != null) {
                    face.getValue().getAsJsonObject().add("uv", vector(uv));
                }
                if (name.equals("leaf") || name.equals("stem")) {
                    face.getValue().getAsJsonObject().addProperty("tintindex", 0);
                }
            }
        }
        item.add("elements", itemElements);
        Identifier itemModel = RR.id("item/water_lily_root");
        blockModels.modelOutput.accept(itemModel, () -> item);
        blockModels.registerSimpleTintedItemModel(RRBlocks.WATER_LILY_ROOT.get(), itemModel, ItemModelUtils.constantTint(-12012264));

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.WATER_LILY_LEAF.get())
                        .with(PropertyDispatch.initial(WaterLilyLeafBlock.FACING)
                                .generate(facing -> orientedBigLilyVariant(BigLilyPadBlock.Part.SINGLE, facing)))
        );
        blockModels.registerSimpleTintedItemModel(
                RRBlocks.WATER_LILY_LEAF.get(),
                blockModels.createFlatItemModelWithBlockTexture(RRBlocks.WATER_LILY_LEAF.get().asItem(), Blocks.LILY_PAD),
                ItemModelUtils.constantTint(-12012264)
        );

        // The flower lies flat just above its own pad.
        JsonObject flowerModel = new JsonObject();
        flowerModel.addProperty("ambientocclusion", false);
        JsonObject flowerTextures = new JsonObject();
        flowerTextures.addProperty("particle", "minecraft:block/lily_pad");
        flowerTextures.addProperty("texture", "minecraft:block/lily_pad");
        flowerTextures.addProperty("flower", "runeruin:block/water_lily_flower_top");
        flowerModel.add("textures", flowerTextures);
        JsonObject flowerDown = planeFace(0, 16, 16, 0);
        JsonObject flowerUp = planeFace(0, 0, 16, 16);
        for (JsonObject face : List.of(flowerDown, flowerUp)) {
            face.addProperty("texture", "#flower");
            face.remove("tintindex");
        }
        JsonArray flowerElements = new JsonArray();
        flowerElements.add(horizontalPlane(0.25, planeFace(0, 16, 16, 0), planeFace(0, 0, 16, 16)));
        flowerElements.add(horizontalPlane(0.75, flowerDown, flowerUp));
        flowerModel.add("elements", flowerElements);

        Identifier flowerModelId = RR.id("block/water_lily_flower");
        blockModels.modelOutput.accept(flowerModelId, () -> flowerModel);
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.WATER_LILY_FLOWER.get())
                        .with(PropertyDispatch.initial(WaterLilyLeafBlock.FACING)
                                .generate(facing -> rotatedTo(BlockModelGenerators.plainVariant(flowerModelId), facing)))
        );
        blockModels.registerSimpleItemModel(
                RRBlocks.WATER_LILY_FLOWER.get(),
                blockModels.createFlatItemModelWithBlockTexture(RRBlocks.WATER_LILY_FLOWER.get().asItem(), RRBlocks.WATER_LILY_FLOWER.get())
        );
    }

    private static Identifier modelFor(BigLilyPadBlock.Part part) {
        return part == BigLilyPadBlock.Part.SINGLE
                ? ModelLocationUtils.getModelLocation(Blocks.LILY_PAD)
                : RR.id("block/big_lily_pad_" + part.getSerializedName());
    }

    private static MultiVariant orientedBigLilyVariant(
            BigLilyPadBlock.Part targetPart,
            Direction facing
    ) {
        // Rotating a cell model alone rotates its texture inside the same
        // world cell. Select the inverse-rotated source cell first so that
        // the complete 2x2/3x3 texture rotates as one connected pad.
        BigLilyPadBlock.Part sourcePart = sourcePartFor(targetPart, facing);
        return rotatedTo(BlockModelGenerators.plainVariant(modelFor(sourcePart)), facing);
    }

    private static MultiVariant rotatedTo(MultiVariant variant, Direction facing) {
        return switch (facing) {
            case NORTH -> variant;
            case EAST -> variant.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> variant.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> variant.with(BlockModelGenerators.Y_ROT_270);
            default -> throw new IllegalArgumentException("Lily pad facing must be horizontal");
        };
    }

    private static BigLilyPadBlock.Part sourcePartFor(
            BigLilyPadBlock.Part targetPart,
            Direction facing
    ) {
        int size = targetPart.gridSize();
        int sourceX;
        int sourceZ;
        switch (facing) {
            case NORTH -> {
                sourceX = targetPart.x();
                sourceZ = targetPart.z();
            }
            case EAST -> {
                sourceX = targetPart.z();
                sourceZ = size - 1 - targetPart.x();
            }
            case SOUTH -> {
                sourceX = size - 1 - targetPart.x();
                sourceZ = size - 1 - targetPart.z();
            }
            case WEST -> {
                sourceX = size - 1 - targetPart.z();
                sourceZ = targetPart.x();
            }
            default -> throw new IllegalArgumentException("Big lily pad facing must be horizontal");
        }

        for (BigLilyPadBlock.Part candidate : BigLilyPadBlock.Part.values()) {
            if (candidate.gridSize() == size && candidate.x() == sourceX && candidate.z() == sourceZ) {
                return candidate;
            }
        }
        throw new IllegalStateException("No big lily pad part at " + size + "x" + size + " coordinates " + sourceX + "," + sourceZ);
    }

    /**
     * Generates a transparent plane whose UVs are one cell of a stretched
     * vanilla lily-pad texture. The cells therefore form one seamless pad,
     * while the 2x2 and 3x3 states use separate geometry/model definitions.
     */
    private static void createBigLilyPartModel(
            @NonNull BlockModelGenerators blockModels,
            BigLilyPadBlock.Part part
    ) {
        int size = part.gridSize();
        double u0 = 16.0 * part.x() / size;
        double u1 = 16.0 * (part.x() + 1) / size;
        double v0 = 16.0 * part.z() / size;
        double v1 = 16.0 * (part.z() + 1) / size;

        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/lily_pad");
        textures.addProperty("texture", "minecraft:block/lily_pad");
        model.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(horizontalPlane(0.25, planeFace(u0, v1, u1, v0), planeFace(u0, v0, u1, v1)));
        model.add("elements", elements);

        blockModels.modelOutput.accept(
                RR.id("block/big_lily_pad_" + part.getSerializedName()),
                () -> model
        );
    }

    /**
     * A flat stem plane along +x ({@code side} 1) or -x from the root axis, cut
     * from rows 44-80 of the stem sheet cell for a leaf at (dx, dy, dz), from
     * just left of the root axis to just past the leaf. The stem leaves the root
     * at {@code start}; both faces show each texel at the same spot.
     */
    private static JsonObject itemStem(double side, double start, int dx, int dz, int dy, int variant) {
        int cellX = WaterLilyStemTexture.cellX(dx, dz);
        int cellY = WaterLilyStemTexture.cellY(dy, variant);
        int startRow = WaterLilyStemTexture.CELL_HEIGHT - WaterLilyStemTexture.BELOW;
        int right = WaterLilyStemTexture.MARGIN + (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) * 16) + 4;
        double x0 = 8 + side * (2 - WaterLilyStemTexture.MARGIN) * ITEM_STEM_SCALE;
        double x1 = 8 + side * (right - WaterLilyStemTexture.MARGIN) * ITEM_STEM_SCALE;
        double u0 = 16.0 * (cellX + 2) / WaterLilyStemTexture.WIDTH;
        double u1 = 16.0 * (cellX + right) / WaterLilyStemTexture.WIDTH;
        double v0 = 16.0 * (cellY + 44) / WaterLilyStemTexture.HEIGHT;
        double v1 = 16.0 * (cellY + WaterLilyStemTexture.CELL_HEIGHT) / WaterLilyStemTexture.HEIGHT;

        JsonArray elements = new JsonArray();
        addElement(elements, "stem",
                new double[]{Math.min(x0, x1), start - (WaterLilyStemTexture.CELL_HEIGHT - startRow) * ITEM_STEM_SCALE, 8},
                new double[]{Math.max(x0, x1), start + (startRow - 44) * ITEM_STEM_SCALE, 8},
                "stem", 0, "north", "south");
        JsonObject stem = elements.get(0).getAsJsonObject();
        // The south face runs u along +x, the north face along -x.
        double[] alongPlusX = {u0, v0, u1, v1};
        double[] alongMinusX = {u1, v0, u0, v1};
        stem.getAsJsonObject("faces").getAsJsonObject("south").add("uv", vector(side > 0 ? alongPlusX : alongMinusX));
        stem.getAsJsonObject("faces").getAsJsonObject("north").add("uv", vector(side > 0 ? alongMinusX : alongPlusX));
        return stem;
    }
}
