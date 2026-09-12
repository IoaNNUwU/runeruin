package ioann.uwu.runeruin.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.ArcaneStonePortalBlock;
import ioann.uwu.runeruin.blocks.BigLilyPadBlock;
import ioann.uwu.runeruin.blocks.EldenVinesBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.items.RRItems;
import ioann.uwu.runeruin.portal.RuneRuinPortalBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.ModelProvider;
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
import org.jspecify.annotations.NonNull;

public class DatagenModelProvider extends ModelProvider {

    public DatagenModelProvider(PackOutput output) {
        super(output, RR.MODID);
    }

    @Override
    protected void registerModels(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {

        // --- Items ---
        itemModels.generateFlatItem(RRItems.RUNE_OF_SPACE.get(), ModelTemplates.FLAT_ITEM);

        // --- Blocks ---
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE.get());
        blockModels.createTrivialCube(RRBlocks.ARCANE_STONE_BRICKS.get());
        blockModels.createTrivialCube(RRBlocks.POLISHED_ARCANE_STONE.get());
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_PILLAR.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        blockModels.createRotatedPillarWithHorizontalVariant(RRBlocks.ARCANE_STONE_COLUMN.get(), TexturedModel.COLUMN_ALT, TexturedModel.COLUMN_HORIZONTAL_ALT);
        createArcaneStonePortal(blockModels);

        blockModels.createTrivialCube(RRBlocks.DIAMOND_ARCANE_STONE.get());

        blockModels.createPlantWithDefaultItem(RRBlocks.ELDEN_SAPLING.get(), RRBlocks.POTTED_ELDEN_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);

        blockModels.createTrivialBlock(RRBlocks.ELDEN_LEAVES.get(), TexturedModel.LEAVES);
        blockModels.createTrivialCube(RRBlocks.ELDEN_PLANKS.get());
        createEldenVines(blockModels);
        Material eldenLogBark = TextureMapping.getBlockTexture(RRBlocks.ELDEN_LOG.get());
        blockModels.createTrivialBlock(RRBlocks.ELDEN_WOOD.get(), _ -> TexturedModel.createAllSame(eldenLogBark));
        createGiantGobletBlocks(blockModels);
        blockModels.new WoodProvider(TextureMapping.column(eldenLogBark, eldenLogBark))
                .wood(RRBlocks.ELDEN_LOG.get());

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
        blockModels.createFullAndCarpetBlocks(RRBlocks.GLOWING_MOSS.get(), RRBlocks.GLOWING_MOSS_CARPET.get());
        blockModels.createTrivialBlock(
                RRBlocks.GLOWING_MUSHROOM_CAP.get(),
                TexturedModel.CUBE.updateTexture(TextureMapping::forceAllTranslucent)
        );
        blockModels.createTrivialCube(RRBlocks.GLOWING_MUSHROOM_STEM.get());
        blockModels.createTrivialCube(RRBlocks.LAPIS_LIGHT.get());
        createFireflyInJar(blockModels);
        createBigLilyPad(blockModels);

        blockModels.registerSimpleItemModel(
                RRBlocks.DEEP_ROOTS.get().asItem(),
                ModelLocationUtils.getModelLocation(Blocks.WARPED_ROOTS.asItem())
        );
        blockModels.createCrossBlock(
                RRBlocks.DEEP_ROOTS.get(),
                BlockModelGenerators.PlantType.NOT_TINTED,
                TextureMapping.cross(TextureMapping.getBlockTexture(Blocks.WARPED_ROOTS))
        );

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
        MultiVariant variant = BlockModelGenerators.plainVariant(modelFor(sourcePart));
        return switch (facing) {
            case NORTH -> variant;
            case EAST -> variant.with(BlockModelGenerators.Y_ROT_90);
            case SOUTH -> variant.with(BlockModelGenerators.Y_ROT_180);
            case WEST -> variant.with(BlockModelGenerators.Y_ROT_270);
            default -> throw new IllegalArgumentException("Big lily pad facing must be horizontal");
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

        JsonObject element = new JsonObject();
        element.add("from", array(0.0, 0.25, 0.0));
        element.add("to", array(16.0, 0.25, 16.0));

        JsonObject faces = new JsonObject();
        faces.add("down", planeFace(u0, v1, u1, v0));
        faces.add("up", planeFace(u0, v0, u1, v1));
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);
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

    private static void createInvertedTreeBlocks(@NonNull BlockModelGenerators blockModels) {
        // Temporary textures: keep the custom block IDs while reusing vanilla assets.
        Material cherryLeaves = TextureMapping.getBlockTexture(Blocks.CHERRY_LEAVES);
        blockModels.createTrivialBlock(
                RRBlocks.INVERTED_LEAVES_1.get(),
                _ -> new TexturedModel(TextureMapping.cube(cherryLeaves), ModelTemplates.LEAVES)
        );

        Material pinkGlazedTerracotta = TextureMapping.getBlockTexture(Blocks.GLAZED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.PINK));
        blockModels.createTrivialBlock(
                RRBlocks.INVERTED_LEAVES_2.get(),
                _ -> TexturedModel.createAllSame(pinkGlazedTerracotta)
        );

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
