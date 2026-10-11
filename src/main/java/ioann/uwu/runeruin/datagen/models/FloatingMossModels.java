package ioann.uwu.runeruin.datagen.models;

import com.google.gson.JsonObject;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.FloatingMossBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.NonNull;

/**
 * The 3x3 floating moss mat: one multipart model per cell, wall and sink stage. A cell ({@code _cell_x_z}) and a
 * wall ({@code _wall_x_z_side}) afloat are model files; their sunk stages and the item are derived from them.
 */
public final class FloatingMossModels {

    private FloatingMossModels() {}

    public static void createFloatingMoss(@NonNull BlockModelGenerators blockModels, ResourceManager resources) {
        MultiPartGenerator blockState = MultiPartGenerator.multiPart(RRBlocks.FLOATING_MOSS.get());
        JsonObject item = null;
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                BooleanProperty cell = FloatingMossBlock.cell(x, z);
                JsonObject cap = addFloatingMossPart(blockModels, resources, blockState, "cell_" + x + "_" + z,
                        condition -> cell == null ? condition : condition.term(cell, true));
                if (cell == null) {
                    item = cap.deepCopy();
                    item.addProperty("parent", "minecraft:block/block");
                }
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    int nx = x + side.getStepX();
                    int nz = z + side.getStepZ();
                    // Walls only face outward, onto a cell of the next ring that may stay empty.
                    if (nx < 0 || nx > 2 || nz < 0 || nz > 2 || floatingMossRing(nx, nz) <= floatingMossRing(x, z)) {
                        continue;
                    }
                    BooleanProperty outer = Objects.requireNonNull(FloatingMossBlock.cell(nx, nz));
                    JsonObject wall = addFloatingMossPart(blockModels, resources, blockState, "wall_" + x + "_" + z + "_" + side.getSerializedName(),
                            condition -> cell == null ? condition.term(outer, false)
                                    : condition.term(outer, false).term(cell, true));
                    if (cell == null) {
                        item.getAsJsonArray("elements").addAll(wall.getAsJsonArray("elements"));
                    }
                }
            }
        }
        blockModels.blockStateOutput.accept(blockState);

        // The item is the middle cell with its four walls.
        JsonObject inventory = Objects.requireNonNull(item);
        blockModels.modelOutput.accept(RR.id("block/floating_moss_inventory"), () -> inventory);
        blockModels.registerSimpleItemModel(RRBlocks.FLOATING_MOSS.get(), RR.id("block/floating_moss_inventory"));
    }

    /** The model file of a part, which this returns, and every stage of it sinking under a creature. */
    private static JsonObject addFloatingMossPart(BlockModelGenerators blockModels, ResourceManager resources,
                                                  MultiPartGenerator blockState, String name,
                                                  UnaryOperator<ConditionBuilder> condition) {
        Identifier file = RR.id("block/floating_moss_" + name);
        JsonObject afloat = ModelJson.read(resources, file);
        for (int sink = 0; sink <= FloatingMossBlock.SINK_STAGES; sink++) {
            Identifier id = sink == 0 ? file : file.withSuffix("_sunk_" + sink);
            if (sink > 0) {
                JsonObject model = ModelJson.lifted(afloat, -FloatingMossBlock.sinkPixels(sink));
                blockModels.modelOutput.accept(id, () -> model);
            }
            blockState.with(condition.apply(new ConditionBuilder().term(FloatingMossBlock.SINK, sink)),
                    BlockModelGenerators.plainVariant(id));
        }
        return afloat;
    }

    /** 0 for the centre cell, 1 for side cells, 2 for corner cells. */
    private static int floatingMossRing(int x, int z) {
        return (x == 1 ? 0 : 1) + (z == 1 ? 0 : 1);
    }
}
