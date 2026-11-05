package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRConfiguredFeatures;
import ioann.uwu.runeruin.dimension.features.PowderedMossVeinFeature;
import ioann.uwu.runeruin.preview.HeadlessTerrainGenerator;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * A powdered moss patch on the real floor of the Blooming caves of the seed, with the caves under it down to the Lost caves.
 * The patch lies where the floor of deep moss around {@code x z} is the thinnest, or the thickest.
 */
public final class PowderedMossShaftPreviewJob implements PreviewJob {
    // How far from x z the floor is searched, and how far around the patch the export reaches.
    private static final int SEARCH = 24;
    private static final int AROUND = 9;
    private static final int FLOOR_TOP = Const.BLOOMING_CAVES_Y + Const.TERRAIN_HEIGHT + 2;

    /** The world with the patch, its origin, how thick the floor is there, and how many columns of deep moss around the moss goes through. */
    public record Placement(PreviewWorld world, BlockPos origin, int floor, int shaftColumns, int columns) {}

    @Override
    public String id() {
        return "powdered_moss_shaft";
    }

    @Override
    public String description() {
        return "A powdered moss patch on the real Blooming caves floor over the Deep caves. params: x, z (searched "
            + SEARCH + " blocks around), floor=thin|thick";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        throw new IOException("powdered_moss_shaft needs the server registries");
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, MinecraftServer server) throws IOException {
        boolean thin = !args.get("floor", "thin").equals("thick");
        Placement placement = place(server, args.seed(), args.getInt("x", 0), args.getInt("z", 0), thin);
        BlockPos origin = placement.origin();
        BoundingBox box = new BoundingBox(origin.getX() - AROUND, Const.LOST_CAVES_Y, origin.getZ() - AROUND,
            origin.getX() + AROUND, origin.getY() + 3, origin.getZ() + AROUND);
        return PreviewJobs.export(placement.world(), box, args.name("preview_" + id()), args.exportDir(), List.of(
            "job: " + id(),
            "seed: " + args.seed(),
            "origin: " + origin.getX() + " " + origin.getY() + " " + origin.getZ(),
            "floor under the origin: " + placement.floor() + " blocks",
            "deep moss floor that the moss goes through: " + placement.shaftColumns() + " of " + placement.columns() + " columns around"
        ));
    }

    public static Placement place(MinecraftServer server, long seed, int x, int z, boolean thin) throws IOException {
        PreviewWorld world = HeadlessTerrainGenerator.generate(server, seed, new BoundingBox(
            x - SEARCH - 16, Const.LOST_CAVES_Y, z - SEARCH - 16, x + SEARCH + 16, FLOOR_TOP, z + SEARCH + 16));
        int columns = 0;
        int shaftColumns = 0;
        @Nullable BlockPos best = null;
        int bestFloor = 0;
        for (int dx = -SEARCH; dx <= SEARCH; dx++) {
            for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                BlockPos surface = surface(world, x + dx, z + dz);
                if (surface == null) {
                    continue;
                }
                int floor = 1;
                while (!world.get(surface.below(floor)).canBeReplaced()) {
                    floor++;
                }
                columns++;
                shaftColumns += floor <= PowderedMossVeinFeature.SHAFT_DEPTH ? 1 : 0;
                if (best == null || (thin ? floor < bestFloor : floor > bestFloor)) {
                    best = surface;
                    bestFloor = floor;
                }
            }
        }
        if (best == null) {
            throw new IOException("No floor of deep moss within " + SEARCH + " blocks of " + x + " " + z + " for seed " + seed);
        }
        BlockPos origin = best.above();
        server.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(RRConfiguredFeatures.POWDERED_MOSS).value()
            .place(world.asLevel(), server.overworld().getChunkSource().getGenerator(), world.random(), origin);
        return new Placement(world, origin, bestFloor, shaftColumns, columns);
    }

    // The deep moss of the Blooming caves floor in this column, as the patch looks for it.
    private static @Nullable BlockPos surface(PreviewWorld world, int x, int z) {
        for (int y = FLOOR_TOP; y >= Const.BLOOMING_CAVES_Y; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.get(pos);
            if (!state.isAir()) {
                BlockState lower = world.get(pos.below());
                return state.is(RRBlocks.DEEP_MOSS.get()) && (lower.is(Blocks.STONE) || lower.is(RRBlocks.DEEP_MOSS.get())) ? pos : null;
            }
        }
        return null;
    }
}
