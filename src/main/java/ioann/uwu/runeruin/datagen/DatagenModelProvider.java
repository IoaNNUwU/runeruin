package ioann.uwu.runeruin.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.ArcaneStonePortalBlock;
import ioann.uwu.runeruin.blocks.BigLilyPadBlock;
import ioann.uwu.runeruin.blocks.EldenVinesBlock;
import ioann.uwu.runeruin.blocks.FloatingMossBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WaterLilyLeafBlock;
import ioann.uwu.runeruin.client.WaterLilyStemTexture;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.portal.RuneRuinPortalBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class DatagenModelProvider extends ModelProvider {

    public DatagenModelProvider(PackOutput output) {
        super(output, RR.MODID);
    }

    @Override
    protected void registerModels(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {

        // --- Items ---
        itemModels.generateFlatItem(RRItems.RUNE_OF_SPACE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.SNAIL_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);

        // --- Blocks ---
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE.get());
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE_BRICKS.get());
        blockModels.createTrivialCube(RRBlocks.POLISHED_ARCANE_STONE.get());
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_PILLAR.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_COLUMN.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        createArcaneStonePortal(blockModels);

        blockModels.createTrivialCube(RRBlocks.DIAMOND_ARCANE_STONE.get());
        createAshenMushroomBlock(blockModels);

        blockModels.createPlantWithDefaultItem(RRBlocks.ELDEN_SAPLING.get(), RRBlocks.POTTED_ELDEN_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);

        blockModels.createTrivialBlock(RRBlocks.ELDEN_LEAVES.get(), TexturedModel.LEAVES);
        blockModels.createLeafLitter(RRBlocks.ELDEN_LEAF_LITTER.get());
        createEldenTreeBlocks(blockModels);
        createEldenVines(blockModels);
        createGiantGobletBlocks(blockModels);

        var acaciaLogSide = TextureMapping.getBlockTexture(Blocks.ACACIA_LOG);
        var acaciaLogTop = TextureMapping.getBlockTexture(Blocks.ACACIA_LOG, "_top");
        TexturedModel.Provider baobabLog = TexturedModel.COLUMN_ALT.updateTexture(mapping -> mapping
                .put(TextureSlot.SIDE, acaciaLogSide)
                .put(TextureSlot.END, acaciaLogTop)
                .put(TextureSlot.PARTICLE, acaciaLogSide));
        TexturedModel.Provider baobabLogHorizontal = TexturedModel.COLUMN_HORIZONTAL_ALT.updateTexture(mapping -> mapping
                .put(TextureSlot.SIDE, acaciaLogSide)
                .put(TextureSlot.END, acaciaLogTop)
                .put(TextureSlot.PARTICLE, acaciaLogSide));
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.BAOBAB_LOG.get(), baobabLog, baobabLogHorizontal);
        TexturedModel.Provider baobabWood = TexturedModel.CUBE.updateTexture(mapping -> mapping.put(
                TextureSlot.ALL,
                acaciaLogSide
        ));
        blockModels.createTrivialBlock(RRBlocks.BAOBAB_WOOD.get(), baobabWood);
        blockModels.registerSimpleItemModel(
                RRBlocks.BAOBAB_WOOD.get(),
                ModelLocationUtils.getModelLocation(RRBlocks.BAOBAB_WOOD.get())
        );
        blockModels.createTintedLeaves(
                RRBlocks.BAOBAB_LEAVES.get(),
                TexturedModel.LEAVES.updateTexture(mapping -> mapping.put(
                        TextureSlot.ALL,
                        TextureMapping.getBlockTexture(Blocks.ACACIA_LEAVES)
                )),
                0x6B9C3C
        );

        createInvertedTreeBlocks(blockModels);

        blockModels.createTrivialCube(RRBlocks.MOSS_LIGHT.get());
        blockModels.createTrivialBlock(
                RRBlocks.POWDERED_MOSS.get(),
                TexturedModel.createDefault(
                        TextureMapping::defaultTexture,
                        ModelTemplates.create("powder_snow", TextureSlot.TEXTURE)
                )
        );
        blockModels.createFullAndCarpetBlocks(RRBlocks.GLOWING_MOSS.get(), RRBlocks.GLOWING_MOSS_CARPET.get());
        createFloatingMoss(blockModels);
        blockModels.createTrivialBlock(
                RRBlocks.GLOWING_MUSHROOM_CAP.get(),
                TexturedModel.CUBE.updateTexture(TextureMapping::forceAllTranslucent)
        );
        blockModels.createTrivialCube(RRBlocks.GLOWING_MUSHROOM_STEM.get());
        blockModels.createCrossBlockWithDefaultItem(
                RRBlocks.GLOWING_MUSHROOM.get(),
                BlockModelGenerators.PlantType.NOT_TINTED
        );
        blockModels.createTrivialCube(RRBlocks.LAPIS_LIGHT.get());
        createFireflyInJar(blockModels);
        createBigLilyPad(blockModels);
        createWaterLily(blockModels);

        var deepRoots = RRBlocks.DEEP_ROOTS.get();
        blockModels.registerSimpleItemModel(
                deepRoots.asItem(),
                BlockModelGenerators.PlantType.NOT_TINTED.createItemModel(blockModels, deepRoots)
        );
        Identifier deepRootsModel = RR.id("block/deep_roots");
        blockModels.modelOutput.accept(deepRootsModel, () -> JsonParser.parseString("""
                {
                  "ambientocclusion": false,
                  "textures": {"particle": "runeruin:block/deep_roots", "cross": "runeruin:block/deep_roots"},
                  "elements": [
                    {"from": [0.8, 0, 8], "to": [15.2, 16, 8], "shade": false,
                     "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": true},
                     "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                               "south": {"uv": [16, 0, 0, 16], "texture": "#cross"}}},
                    {"from": [8, 0, 0.8], "to": [8, 16, 15.2], "shade": false,
                     "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": true},
                     "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#cross"},
                               "east": {"uv": [16, 0, 0, 16], "texture": "#cross"}}}
                  ]
                }"""));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                deepRoots,
                BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(deepRootsModel))
        ));

        createRuneRuinPortal(blockModels);
        createMossBerry(blockModels, itemModels);
    }

    private static void createBigLilyPad(@NonNull BlockModelGenerators blockModels) {
        for (BigLilyPadBlock.Part part : BigLilyPadBlock.Part.values()) {
            if (part != BigLilyPadBlock.Part.SINGLE) {
                createBigLilyPartModel(blockModels, part);
            }
        }

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.BIG_LILY_PAD.get())
                        .with(PropertyDispatch.initial(BigLilyPadBlock.PART, BigLilyPadBlock.FACING)
                                .generate(DatagenModelProvider::orientedBigLilyVariant)
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

    private static final double ITEM_STEM_SCALE = 0.35;
    private static final double ITEM_LEAF_HALF = 3.6;

    private static void createWaterLily(@NonNull BlockModelGenerators blockModels) {
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/dark_oak_log");
        textures.addProperty("rhizome", "minecraft:block/dark_oak_log");
        textures.addProperty("roots", "minecraft:block/hanging_roots");
        model.add("textures", textures);

        JsonArray elements = new JsonArray();
        addJarElement(elements, "rhizome", new double[]{5, 9, 5}, new double[]{11, 13, 11}, "rhizome", 0,
                "north", "south", "east", "west", "up", "down");
        // Two tiers of hair roots hang from the rhizome below the block in an X,
        // like on a real water lily. Stems run in planes through the root axis
        // towards whole-block offsets, never at 40 or 50 degrees, so the root
        // planes never z-fight a stem.
        for (double[] plane : new double[][]{{-7, 40}, {-7, -50}, {-15, -40}, {-15, 50}}) {
            addJarElement(elements, "roots", new double[]{0, plane[0], 8}, new double[]{16, plane[0] + 16, 8},
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
        addJarElement(itemElements, "rhizome", new double[]{5, 8, 5}, new double[]{11, 11, 11}, "rhizome", 0,
                "north", "south", "east", "west", "up", "down");
        // Seen from the item's diagonal angle, axis-aligned planes read as an X.
        addJarElement(itemElements, "roots", new double[]{2, 0, 8}, new double[]{14, 8, 8}, "roots", 0, "north", "south");
        addJarElement(itemElements, "roots", new double[]{8, 0, 2}, new double[]{8, 8, 14}, "roots", 0, "east", "west");
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
            addJarElement(itemElements, "leaf", new double[]{leafX - ITEM_LEAF_HALF, height, 8 - ITEM_LEAF_HALF},
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

    private static void createFloatingMoss(@NonNull BlockModelGenerators blockModels) {
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(RRBlocks.FLOATING_MOSS.get());
        JsonArray inventory = new JsonArray();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                BooleanProperty cell = FloatingMossBlock.cell(x, z);
                JsonArray cap = new JsonArray();
                addJarElement(cap, "moss_cap", floatingMossPoint(x, z, FloatingMossBlock.MOSS_INSET, 14),
                        floatingMossPoint(x + 1, z + 1, FloatingMossBlock.MOSS_INSET, 16), "moss", 0, "up", "down");
                addJarElement(cap, "rooted_soil_floor", floatingMossPoint(x, z, FloatingMossBlock.SOIL_INSET, 8),
                        floatingMossPoint(x + 1, z + 1, FloatingMossBlock.SOIL_INSET, 14), "bottom", 0, "down");
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    int nx = x + side.getStepX();
                    int nz = z + side.getStepZ();
                    // A seam to a connected neighbour hides inside the mat until that neighbour sinks lower.
                    if (nx < 0 || nx > 2 || nz < 0 || nz > 2) {
                        addFloatingMossWall(cap, "moss_edge", x, z, side, FloatingMossBlock.MOSS_INSET, 14, 16, false);
                        addFloatingMossWall(cap, "soil", x, z, side, FloatingMossBlock.SOIL_INSET, 8, 14, false);
                    }
                }
                addFloatingMossPart(blockModels, blockState, "cell_" + x + "_" + z, cap,
                        condition -> cell == null ? condition : condition.term(cell, true));
                if (cell == null) {
                    inventory.addAll(cap);
                }
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    int nx = x + side.getStepX();
                    int nz = z + side.getStepZ();
                    // Walls only face outward, onto a cell of the next ring that may stay empty.
                    if (nx < 0 || nx > 2 || nz < 0 || nz > 2 || floatingMossRing(nx, nz) <= floatingMossRing(x, z)) {
                        continue;
                    }
                    JsonArray wall = new JsonArray();
                    addFloatingMossWall(wall, "moss_edge", x, z, side, FloatingMossBlock.MOSS_INSET, 12, 16, true);
                    addFloatingMossWall(wall, "soil", x, z, side, FloatingMossBlock.SOIL_INSET, 5, 14, true);
                    BooleanProperty outer = Objects.requireNonNull(FloatingMossBlock.cell(nx, nz));
                    if (cell == null) {
                        inventory.addAll(wall);
                    }
                    addFloatingMossPart(blockModels, blockState, "wall_" + x + "_" + z + "_" + side.getSerializedName(),
                            wall, condition -> cell == null ? condition.term(outer, false)
                                    : condition.term(outer, false).term(cell, true));
                }
            }
        }
        blockModels.blockStateOutput.accept(blockState);

        JsonObject item = floatingMossModel(inventory);
        item.addProperty("parent", "minecraft:block/block");
        blockModels.modelOutput.accept(RR.id("block/floating_moss_inventory"), () -> item);
        blockModels.registerSimpleItemModel(RRBlocks.FLOATING_MOSS.get(), RR.id("block/floating_moss_inventory"));
    }

    private static void addFloatingMossPart(BlockModelGenerators blockModels, MultiPartGenerator blockState,
                                            String name, JsonArray elements,
                                            UnaryOperator<ConditionBuilder> condition) {
        for (int sink = 0; sink <= FloatingMossBlock.SINK_STAGES; sink++) {
            JsonObject model = floatingMossModel(sunkFloatingMoss(elements, sink));
            Identifier id = RR.id("block/floating_moss_" + name + (sink == 0 ? "" : "_sunk_" + sink));
            blockModels.modelOutput.accept(id, () -> model);
            blockState.with(condition.apply(new ConditionBuilder().term(FloatingMossBlock.SINK, sink)),
                    BlockModelGenerators.plainVariant(id));
        }
    }

    private static JsonArray sunkFloatingMoss(JsonArray elements, int sink) {
        JsonArray sunk = elements.deepCopy();
        for (JsonElement element : sunk) {
            JsonArray from = element.getAsJsonObject().getAsJsonArray("from");
            JsonArray to = element.getAsJsonObject().getAsJsonArray("to");
            double[] min = {from.get(0).getAsDouble(), from.get(1).getAsDouble(), from.get(2).getAsDouble()};
            double[] max = {to.get(0).getAsDouble(), to.get(1).getAsDouble(), to.get(2).getAsDouble()};
            // Side UVs default to the element position; pin them so the texture sinks with the model.
            for (Map.Entry<String, JsonElement> face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                double[] u = switch (face.getKey()) {
                    case "north" -> new double[]{16 - max[0], 16 - min[0]};
                    case "south" -> new double[]{min[0], max[0]};
                    case "west" -> new double[]{min[2], max[2]};
                    case "east" -> new double[]{16 - max[2], 16 - min[2]};
                    default -> null;
                };
                if (u != null) {
                    face.getValue().getAsJsonObject().add("uv", vector(new double[]{u[0], 16 - max[1], u[1], 16 - min[1]}));
                }
            }
            from.set(1, new JsonPrimitive(min[1] - FloatingMossBlock.sinkPixels(sink)));
            to.set(1, new JsonPrimitive(max[1] - FloatingMossBlock.sinkPixels(sink)));
        }
        return sunk;
    }

    private static JsonObject floatingMossModel(JsonArray elements) {
        JsonObject model = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/moss_block");
        textures.addProperty("moss", "minecraft:block/moss_block");
        textures.addProperty("moss_edge", "runeruin:block/floating_moss_edge");
        textures.addProperty("soil", "runeruin:block/floating_moss_roots");
        textures.addProperty("bottom", "minecraft:block/rooted_dirt");
        model.add("textures", textures);
        model.add("elements", elements);
        return model;
    }

    /** 0 for the centre cell, 1 for side cells, 2 for corner cells. */
    private static int floatingMossRing(int x, int z) {
        return (x == 1 ? 0 : 1) + (z == 1 ? 0 : 1);
    }

    private static double[] floatingMossPoint(int x, int z, double inset, double y) {
        return new double[]{FloatingMossBlock.cellEdge(x, inset), y, FloatingMossBlock.cellEdge(z, inset)};
    }

    private static void addFloatingMossWall(JsonArray elements, String texture, int x, int z, Direction side,
                                            double inset, double minY, double maxY, boolean withInner) {
        double[] from = floatingMossPoint(x, z, inset, minY);
        double[] to = floatingMossPoint(x + 1, z + 1, inset, maxY);
        int axis = side.getAxis() == Direction.Axis.X ? 0 : 2;
        if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            from[axis] = to[axis];
        } else {
            to[axis] = from[axis];
        }
        // A quad faces only one way; the inward face keeps the far drips visible from below.
        String[] faces = withInner ? new String[]{side.getSerializedName(), side.getOpposite().getSerializedName()}
                : new String[]{side.getSerializedName()};
        addJarElement(elements, texture + "_wall", from, to, texture, 0, faces);
    }

    private static void createAshenMushroomBlock(@NonNull BlockModelGenerators blockModels) {
        var block = RRBlocks.ASHEN_MUSHROOM_BLOCK.get();
        Material cap = new Material(RR.id("block/ashen_mushroom_cap"));
        Material underside = new Material(RR.id("block/ashen_mushroom_underside"));
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.PARTICLE, cap)
                .put(TextureSlot.NORTH, cap)
                .put(TextureSlot.SOUTH, cap)
                .put(TextureSlot.EAST, cap)
                .put(TextureSlot.WEST, cap)
                .put(TextureSlot.UP, cap)
                .put(TextureSlot.DOWN, underside);

        Identifier model = ModelTemplates.CUBE.create(block, textures, blockModels.modelOutput);
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(model))
        );
        blockModels.registerSimpleItemModel(block, model);
    }

    private static void createFireflyInJar(@NonNull BlockModelGenerators blockModels) {
        JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "runeruin:block/firefly_jar_glass");
        textures.addProperty("glass", "runeruin:block/firefly_jar_glass");
        textures.addProperty("butterfly", "runeruin:block/firefly_butterfly");
        textures.addProperty("wood", "minecraft:block/oak_planks");
        model.add("textures", textures);

        JsonArray elements = new JsonArray();
        addJarGlassElement(elements);
        addJarElement(elements, "jar_wood_neck", new double[]{1, 13, 1}, new double[]{15, 14, 15}, "wood", 0,
                "north", "south", "east", "west", "up", "down");
        addJarElement(elements, "jar_wood_lid", new double[]{0.5, 14, 0.5}, new double[]{15.5, 16, 15.5}, "wood", 0,
                "north", "south", "east", "west", "up", "down");

        // Two thin, crossed planes keep the tiny 8x8 butterfly visible from every side.
        addJarElement(elements, "firefly_plane_north_south", new double[]{4, 4, 7.9}, new double[]{12, 12, 8.1}, "butterfly", 15,
                "north", "south");
        addJarElement(elements, "firefly_plane_east_west", new double[]{7.9, 4, 4}, new double[]{8.1, 12, 12}, "butterfly", 15,
                "east", "west");
        model.add("elements", elements);

        Identifier modelId = RR.id("block/firefly_in_a_jar");
        blockModels.modelOutput.accept(modelId, () -> model);
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(
                        RRBlocks.FIREFLY_IN_A_JAR.get(),
                        BlockModelGenerators.plainVariant(modelId)
                )
        );
        blockModels.registerSimpleItemModel(RRBlocks.FIREFLY_IN_A_JAR.get(), modelId);
    }

    private static void addJarGlassElement(JsonArray elements) {
        JsonObject element = new JsonObject();
        element.addProperty("name", "jar_glass_body");
        element.add("from", vector(new double[]{1, 0, 1}));
        element.add("to", vector(new double[]{15, 13, 15}));

        JsonObject faces = new JsonObject();
        double[] sideUv = {1, 1.5, 15, 14.5};
        for (String direction : new String[]{"north", "south", "east", "west"}) {
            addJarGlassFace(faces, direction, sideUv);
        }
        element.add("faces", faces);
        elements.add(element);
    }

    private static void addJarGlassFace(JsonObject faces, String direction, double[] uv) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", "#glass");
        face.add("uv", vector(uv));
        faces.add(direction, face);
    }

    private static void addJarElement(
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

    private static JsonArray vector(double[] coordinates) {
        JsonArray result = new JsonArray();
        for (double coordinate : coordinates) {
            result.add(coordinate);
        }
        return result;
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

    private static JsonArray array(double x, double y, double z) {
        JsonArray result = new JsonArray();
        result.add(x);
        result.add(y);
        result.add(z);
        return result;
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
        addJarElement(elements, "stem",
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

    private static void rotateY(JsonElement element, double angle) {
        JsonObject rotation = new JsonObject();
        rotation.add("origin", vector(new double[]{8, 8, 8}));
        rotation.addProperty("axis", "y");
        rotation.addProperty("angle", angle);
        element.getAsJsonObject().add("rotation", rotation);
    }

    private static JsonObject horizontalPlane(double y, JsonObject down, JsonObject up) {
        JsonObject element = new JsonObject();
        element.add("from", array(0.0, y, 0.0));
        element.add("to", array(16.0, y, 16.0));
        JsonObject faces = new JsonObject();
        faces.add("down", down);
        faces.add("up", up);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject planeFace(double u0, double v0, double u1, double v1) {
        JsonObject face = new JsonObject();
        JsonArray uv = new JsonArray();
        uv.add(u0);
        uv.add(v0);
        uv.add(u1);
        uv.add(v1);
        face.add("uv", uv);
        face.addProperty("texture", "#texture");
        face.addProperty("tintindex", 0);
        return face;
    }

    private static void createGiantGobletBlocks(@NonNull BlockModelGenerators blockModels) {
        Material paleOakBark = TextureMapping.getBlockTexture(Blocks.PALE_OAK_LOG);
        blockModels.createTrivialBlock(
                RRBlocks.GIANT_GOBLET_STEM.get(),
                _ -> TexturedModel.createAllSame(paleOakBark)
        );
        Material warpedWart = TextureMapping.getBlockTexture(Blocks.WARPED_WART_BLOCK);
        blockModels.createTrivialBlock(
                RRBlocks.GIANT_GOBLET_BUD.get(),
                _ -> TexturedModel.createAllSame(warpedWart)
        );
    }

    private static void createEldenTreeBlocks(@NonNull BlockModelGenerators blockModels) {
        Material bark = TextureMapping.getBlockTexture(RRBlocks.ELDEN_LOG.get());
        Material core = TextureMapping.getBlockTexture(RRBlocks.ELDEN_LOG.get(), "_top");
        blockModels.createTrivialBlock(RRBlocks.ELDEN_WOOD.get(), _ -> TexturedModel.createAllSame(bark));
        blockModels.new WoodProvider(TextureMapping.column(bark, core))
                .logWithHorizontal(RRBlocks.ELDEN_LOG.get());
        blockModels.createTrivialCube(RRBlocks.ELDEN_PLANKS.get());

        BlockFamily family = new BlockFamily.Builder(RRBlocks.ELDEN_PLANKS.get())
                .button(RRBlocks.ELDEN_BUTTON.get())
                .door(RRBlocks.ELDEN_DOOR.get())
                .fence(RRBlocks.ELDEN_FENCE.get())
                .fenceGate(RRBlocks.ELDEN_FENCE_GATE.get())
                .hangingSign(RRBlocks.ELDEN_HANGING_SIGN.get(), RRBlocks.ELDEN_WALL_HANGING_SIGN.get())
                .pressurePlate(RRBlocks.ELDEN_PRESSURE_PLATE.get())
                .sign(RRBlocks.ELDEN_SIGN.get(), RRBlocks.ELDEN_WALL_SIGN.get())
                .slab(RRBlocks.ELDEN_SLAB.get())
                .stairs(RRBlocks.ELDEN_STAIRS.get())
                .strippedLog(RRBlocks.ELDEN_LOG.get())
                .getFamily();
        blockModels.familyWithExistingFullBlock(RRBlocks.ELDEN_PLANKS.get()).generateFor(family);
        // Oak uses the non-orientable trapdoor UV templates, unlike the generic family default.
        blockModels.createTrapdoor(RRBlocks.ELDEN_TRAPDOOR.get());
    }

    private static void createInvertedTreeBlocks(@NonNull BlockModelGenerators blockModels) {
        blockModels.createTrivialBlock(RRBlocks.INVERTED_LEAVES_1.get(), TexturedModel.LEAVES);
        blockModels.createTrivialBlock(RRBlocks.INVERTED_LEAVES_2.get(), TexturedModel.LEAVES);

        Material invertedWood = new Material(RR.id("block/inverted_tree_wood"));
        blockModels.new WoodProvider(TextureMapping.column(invertedWood, invertedWood))
                .wood(RRBlocks.INVERTED_TREE_WOOD.get());

        BlockFamily blockFamily = new BlockFamily.Builder(RRBlocks.INVERTED_TREE_PLANKS.get())
                .button(RRBlocks.INVERTED_TREE_BUTTON.get())
                .fence(RRBlocks.INVERTED_TREE_FENCE.get())
                .fenceGate(RRBlocks.INVERTED_TREE_FENCE_GATE.get())
                .pressurePlate(RRBlocks.INVERTED_TREE_PRESSURE_PLATE.get())
                .slab(RRBlocks.INVERTED_TREE_SLAB.get())
                .stairs(RRBlocks.INVERTED_TREE_STAIRS.get())
                .getFamily();

        blockModels.familyWithExistingFullBlock(Blocks.PALE_OAK_PLANKS)
                .fullBlock(RRBlocks.INVERTED_TREE_PLANKS.get(), ModelTemplates.CUBE_ALL)
                .generateFor(blockFamily);

        // Reuse the complete vanilla model/item definitions, including their textures.
        createInvertedTreeDoorModels(blockModels);
        createInvertedTreeTrapdoorModels(blockModels);

        createInvertedTreeSignModels(blockModels);
    }

    private static void createInvertedTreeDoorModels(@NonNull BlockModelGenerators blockModels) {
        MultiVariant bottomLeft = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_BOTTOM_LEFT.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant bottomLeftOpen = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_BOTTOM_LEFT_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant bottomRight = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_BOTTOM_RIGHT.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant bottomRightOpen = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_BOTTOM_RIGHT_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant topLeft = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_TOP_LEFT.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant topLeftOpen = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_TOP_LEFT_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant topRight = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_TOP_RIGHT.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );
        MultiVariant topRightOpen = BlockModelGenerators.plainVariant(
                ModelTemplates.DOOR_TOP_RIGHT_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_DOOR)
        );

        blockModels.blockStateOutput.accept(BlockModelGenerators.createDoor(
                RRBlocks.INVERTED_TREE_DOOR.get(),
                bottomLeft,
                bottomLeftOpen,
                bottomRight,
                bottomRightOpen,
                topLeft,
                topLeftOpen,
                topRight,
                topRightOpen
        ));
        blockModels.registerSimpleItemModel(
                RRBlocks.INVERTED_TREE_DOOR.get(),
                ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_DOOR.asItem())
        );
    }

    private static void createInvertedTreeTrapdoorModels(@NonNull BlockModelGenerators blockModels) {
        MultiVariant top = BlockModelGenerators.plainVariant(
                ModelTemplates.TRAPDOOR_TOP.getDefaultModelLocation(Blocks.PALE_OAK_TRAPDOOR)
        );
        MultiVariant bottom = BlockModelGenerators.plainVariant(
                ModelTemplates.TRAPDOOR_BOTTOM.getDefaultModelLocation(Blocks.PALE_OAK_TRAPDOOR)
        );
        MultiVariant open = BlockModelGenerators.plainVariant(
                ModelTemplates.TRAPDOOR_OPEN.getDefaultModelLocation(Blocks.PALE_OAK_TRAPDOOR)
        );

        blockModels.blockStateOutput.accept(BlockModelGenerators.createOrientableTrapdoor(
                RRBlocks.INVERTED_TREE_TRAPDOOR.get(),
                top,
                bottom,
                open
        ));
        blockModels.registerSimpleItemModel(
                RRBlocks.INVERTED_TREE_TRAPDOOR.get(),
                ModelTemplates.TRAPDOOR_BOTTOM.getDefaultModelLocation(Blocks.PALE_OAK_TRAPDOOR)
        );
    }

    private static void createInvertedTreeSignModels(@NonNull BlockModelGenerators blockModels) {
        // Reuse the vanilla Pale Oak sign models until the inverted-tree texture set exists.
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSign(
                        RRBlocks.INVERTED_TREE_SIGN.get(),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN, "_rot_0")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN, "_rot_1")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN, "_rot_2")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN, "_rot_3"))
                )
        );
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(
                                RRBlocks.INVERTED_TREE_WALL_SIGN.get(),
                                BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_WALL_SIGN))
                        )
                        .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING_ALT)
        );
        blockModels.registerSimpleItemModel(
                RRBlocks.INVERTED_TREE_SIGN.get().asItem(),
                ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_SIGN.asItem())
        );

        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createHangingSign(
                        RRBlocks.INVERTED_TREE_HANGING_SIGN.get(),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_rot_0")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_rot_1")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_rot_2")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_rot_3")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_attached_rot_0")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_attached_rot_1")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_attached_rot_2")),
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN, "_attached_rot_3"))
                )
        );
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(
                                RRBlocks.INVERTED_TREE_WALL_HANGING_SIGN.get(),
                                BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_WALL_HANGING_SIGN))
                        )
                        .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING_ALT)
        );
        blockModels.registerSimpleItemModel(
                RRBlocks.INVERTED_TREE_HANGING_SIGN.get().asItem(),
                ModelLocationUtils.getModelLocation(Blocks.PALE_OAK_HANGING_SIGN.asItem())
        );
    }

    private static void createRuneRuinPortal(@NonNull BlockModelGenerators blockModels) {
        // Reuse vanilla nether portal models/texture until a custom portal texture exists.
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.RUNE_RUIN_PORTAL.get())
                        .with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_AXIS, RuneRuinPortalBlock.UNSTABLE)
                                .generate((axis, unstable) -> BlockModelGenerators.plainVariant(
                                        ModelLocationUtils.getModelLocation(
                                                Blocks.NETHER_PORTAL,
                                                axis == Direction.Axis.X ? "_ns" : "_ew"
                                        )
                                ))
                        )
        );
    }

    private static void createArcaneStonePortal(@NonNull BlockModelGenerators blockModels) {
        var block = RRBlocks.ARCANE_STONE_PORTAL.get();
        Identifier cornerModel = blockModels.createSuffixedVariant(
                block, "_corner", ModelTemplates.CUBE_ALL,
                _ -> TextureMapping.cube(new Material(RR.id("block/arcane_stone_portal_runes")))
        );
        Identifier columnModel = ModelTemplates.CUBE_COLUMN.createWithSuffix(
                block,
                "_column",
                TextureMapping.column(
                        new Material(RR.id("block/arcane_stone_portal_column")),
                        TextureMapping.getBlockTexture(RRBlocks.ARCANE_STONE_COLUMN.get(), "_top")
                ),
                blockModels.modelOutput
        );
        Identifier middleModel = blockModels.createSuffixedVariant(
                block, "_middle", ModelTemplates.CUBE_ALL,
                _ -> TextureMapping.cube(new Material(RR.id("block/arcane_stone_portal_middle")))
        );
        Identifier vertColumnModel = blockModels.createSuffixedVariant(
                block, "_vert_column", ModelTemplates.CUBE_ALL,
                _ -> TextureMapping.cube(new Material(RR.id("block/arcane_stone_portal_vert_column")))
        );
        Identifier middleLeftX = createPortalMiddleHalfModel(blockModels, "left", Direction.Axis.X);
        Identifier middleRightX = createPortalMiddleHalfModel(blockModels, "right", Direction.Axis.X);
        Identifier middleLeftZ = createPortalMiddleHalfModel(blockModels, "left", Direction.Axis.Z);
        Identifier middleRightZ = createPortalMiddleHalfModel(blockModels, "right", Direction.Axis.Z);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(PropertyDispatch.initial(
                                        ArcaneStonePortalBlock.PART,
                                        ArcaneStonePortalBlock.MIDDLE_SIDE,
                                        ArcaneStonePortalBlock.AXIS
                                )
                                .generate((part, middleSide, axis) -> BlockModelGenerators.plainVariant(switch (part) {
                                    case CORNER -> cornerModel;
                                    case COLUMN -> columnModel;
                                    case VERT_COLUMN -> vertColumnModel;
                                    case MIDDLE -> switch (middleSide) {
                                        case SINGLE -> middleModel;
                                        case LEFT -> axis == Direction.Axis.X ? middleLeftX : middleLeftZ;
                                        case RIGHT -> axis == Direction.Axis.X ? middleRightX : middleRightZ;
                                    };
                                })))
        );
        blockModels.registerSimpleItemModel(block, cornerModel);
    }

    private static Identifier createPortalMiddleHalfModel(
            @NonNull BlockModelGenerators blockModels,
            String side,
            Direction.Axis axis
    ) {
        var block = RRBlocks.ARCANE_STONE_PORTAL.get();
        Material stone = TextureMapping.getBlockTexture(RRBlocks.ARCANE_STONE.get());
        Material left = new Material(RR.id("block/arcane_stone_portal_middle_left"));
        Material right = new Material(RR.id("block/arcane_stone_portal_middle_right"));
        Material primary = side.equals("left") ? left : right;
        Material opposite = side.equals("left") ? right : left;

        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.ALL, stone)
                .put(TextureSlot.UP, stone)
                .put(TextureSlot.DOWN, stone);
        if (axis == Direction.Axis.X) {
            textures.put(TextureSlot.NORTH, primary).put(TextureSlot.SOUTH, opposite);
        } else {
            textures.put(TextureSlot.WEST, primary).put(TextureSlot.EAST, opposite);
        }

        return ModelTemplates.CUBE.createWithSuffix(
                block,
                "_middle_" + side + "_" + axis.getName(),
                textures,
                blockModels.modelOutput
        );
    }

    private static void createMossBerry(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(RRItems.MOSS_BERRY.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RRItems.POWDERED_MOSS_BUCKET.get(), ModelTemplates.FLAT_ITEM);

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(RRBlocks.MOSS_BERRY_BUSH.get())
                        .with(PropertyDispatch.initial(BlockStateProperties.AGE_3)
                                .generate(age -> BlockModelGenerators.plainVariant(
                                        blockModels.createSuffixedVariant(
                                                RRBlocks.MOSS_BERRY_BUSH.get(),
                                                "_stage" + age,
                                                ModelTemplates.CROSS,
                                                TextureMapping::cross
                                        )
                                ))
                        )
        );
    }

    private static void createEldenVines(@NonNull BlockModelGenerators blockModels) {
        var block = RRBlocks.ELDEN_VINES.get();
        var stemTexture = new Material(RR.id("block/elden_vines"));
        var orbTexture = new Material(RR.id("block/elden_vines_orb"));
        var stemModel = blockModels.createSuffixedVariant(
                block,
                "",
                ModelTemplates.CROSS,
                _ -> TextureMapping.cross(stemTexture)
        );
        var orbModel = blockModels.createSuffixedVariant(
                block,
                "_orb",
                ModelTemplates.CROSS,
                _ -> TextureMapping.cross(orbTexture)
        );

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(BlockModelGenerators.createBooleanModelDispatch(
                                EldenVinesBlock.ORB,
                                BlockModelGenerators.plainVariant(orbModel),
                                BlockModelGenerators.plainVariant(stemModel)
                        ))
        );
        blockModels.registerSimpleItemModel(block, stemModel);
    }
}
