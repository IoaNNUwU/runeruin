package ioann.uwu.runeruin.datagen.models;

import static ioann.uwu.runeruin.datagen.models.ModelJson.addElement;
import static ioann.uwu.runeruin.datagen.models.ModelJson.vector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.FloatingMossBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.NonNull;

/** The 3x3 floating moss mat: one multipart model per cell, wall and sink stage. */
public final class FloatingMossModels {

    private FloatingMossModels() {}

    public static void createFloatingMoss(@NonNull BlockModelGenerators blockModels) {
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(RRBlocks.FLOATING_MOSS.get());
        JsonArray inventory = new JsonArray();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                BooleanProperty cell = FloatingMossBlock.cell(x, z);
                JsonArray cap = new JsonArray();
                addElement(cap, "moss_cap", floatingMossPoint(x, z, FloatingMossBlock.MOSS_INSET, 14),
                        floatingMossPoint(x + 1, z + 1, FloatingMossBlock.MOSS_INSET, 16), "moss", 0, "up", "down");
                addElement(cap, "rooted_soil_floor", floatingMossPoint(x, z, FloatingMossBlock.SOIL_INSET, 8),
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
        addElement(elements, texture + "_wall", from, to, texture, 0, faces);
    }
}
