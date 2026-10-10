package ioann.uwu.runeruin.preview.jobs;

import com.mojang.datafixers.util.Pair;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.structures.HangingPiece;
import ioann.uwu.runeruin.dimension.structures.HangingPlatformPiece;
import ioann.uwu.runeruin.dimension.structures.HangingTrackPiece;
import ioann.uwu.runeruin.dimension.structures.HangingTracksLayout;
import ioann.uwu.runeruin.dimension.structures.HangingTracksStructure;
import ioann.uwu.runeruin.preview.HeadlessTerrainGenerator;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class HangingTracksPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "hanging_tracks";
    }

    @Override
    public String description() {
        return "Hanging minecart tracks under a wavy ceiling. params: piece (network, straight, stairs, diagonal, corner, "
                + "junction, platform), size, extent (1-2), links (junction sides, 1-15), wood (oak, inverted_tree), "
                + "wear (intact, decayed, overgrown), shape (square, l, t), shell (open, railing, room), content, gap (3-20), "
                + "terrain=real with x and z (the ceiling and the floor of the Deep caves for the seed, around that block)";
    }

    /** A ceiling like the one of the Deep caves, its lowest block 3 to 10 blocks under the plate, and no floor. */
    private static final HangingTracksLayout.Terrain WAVES = new HangingTracksLayout.Terrain() {
        @Override
        public int ceilingY(int x, int z) {
            return Const.DEEP_CAVES_CEILING_Y - 3 - (int) (3.5 + 3.5 * Math.sin(x * 0.11) * Math.cos(z * 0.13));
        }

        @Override
        public boolean floorReaches(int x, int z, int y) {
            return false;
        }
    };

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        return run(args, WAVES);
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, MinecraftServer server) throws IOException {
        if (!args.get("terrain", "waves").equals("real")) {
            return run(args, WAVES);
        }
        return run(args, HangingTracksStructure.terrain(HeadlessTerrainGenerator.randomState(server, args.seed())));
    }

    private PreviewJobs.Result run(PreviewArgs args, HangingTracksLayout.Terrain terrain) throws IOException {
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        String kind = args.get("piece", "network");

        List<HangingPiece> pieces;
        if (kind.equals("network")) {
            pieces = HangingTracksLayout.generate(args.getInt("x", 0), args.getInt("z", 0), world.random(), terrain);
            if (pieces.isEmpty()) {
                throw new IllegalStateException("No network fits around the station with seed " + seed);
            }
        } else {
            pieces = List.of(single(kind, args, seed, terrain));
        }

        for (HangingPiece piece : pieces) {
            BoundingBox box = piece.getBoundingBox();
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    int y = terrain.ceilingY(x, z);
                    world.fillBox(new BoundingBox(x, y, z, x, y + 1, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
        Map<String, Integer> kinds = new TreeMap<>();
        for (HangingPiece piece : pieces) {
            PreviewJobs.placePieceAcrossChunks(piece, world);
            kinds.merge(piece.getClass().getSimpleName(), 1, Integer::sum);
        }

        if (kind.equals("network")) {
            checkRails(world);
        }

        List<String> info = new ArrayList<>(List.of("job: " + id(), "seed: " + seed, "piece: " + kind, "pieces: " + kinds));
        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_hanging_tracks"), args.exportDir(), info);
    }

    /** What the structure promises: every rail leads to another rail or to the stop at the end of a track. */
    private static void checkRails(PreviewWorld world) {
        BoundingBox box = world.occupiedBox();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    BlockState state = world.get(pos.set(x, y, z));
                    if (!state.is(RRBlocks.RUNIC_RAIL)) {
                        continue;
                    }
                    Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(state.getValue(RailBlock.SHAPE));
                    for (Vec3i exit : List.of(exits.getFirst(), exits.getSecond())) {
                        // A sloped rail lies a block lower than a minecart rides on it.
                        int level = state.getValue(RailBlock.SHAPE).isSlope() ? y + 1 + exit.getY() : y;
                        BlockState next = world.get(new BlockPos(x + exit.getX(), level, z + exit.getZ()));
                        BlockState lower = world.get(new BlockPos(x + exit.getX(), level - 1, z + exit.getZ()));
                        boolean stop = next.is(Blocks.OAK_LOG) || next.is(RRBlocks.INVERTED_TREE_WOOD);
                        boolean slope = lower.is(RRBlocks.RUNIC_RAIL) && lower.getValue(RailBlock.SHAPE).isSlope();
                        if (!next.is(RRBlocks.RUNIC_RAIL) && !stop && !slope) {
                            throw new IllegalStateException("The rails break off at " + x + " " + y + " " + z
                                    + " (" + state.getValue(RailBlock.SHAPE) + ")");
                        }
                    }
                }
            }
        }
    }

    private static HangingPiece single(String kind, PreviewArgs args, long seed, HangingTracksLayout.Terrain terrain) {
        int extent = Math.max(1, Math.min(2, args.getInt("extent", 2)));
        int size = args.getInt("size", 0);
        HangingPiece.Look look = new HangingPiece.Look(
                HangingPiece.Wood.valueOf(args.get("wood", "oak").toUpperCase(Locale.ROOT)),
                HangingPiece.Wear.valueOf(args.get("wear", "intact").toUpperCase(Locale.ROOT)),
                seed
        );
        int gap = Math.max(HangingTracksLayout.MIN_GAP, Math.min(HangingTracksLayout.MAX_GAP, args.getInt("gap", 10)));
        HangingTracksLayout.Port port = new HangingTracksLayout.Port(0, terrain.ceilingY(0, 0) - 1 - gap, 0, Direction.NORTH);

        if (kind.equals("platform")) {
            return new HangingPlatformPiece(
                    port,
                    HangingPlatformPiece.Shape.valueOf(args.get("shape", "square").toUpperCase(Locale.ROOT)),
                    size == 0 ? 7 : size,
                    1,
                    HangingPlatformPiece.Shell.valueOf(args.get("shell", "room").toUpperCase(Locale.ROOT)),
                    HangingPlatformPiece.Content.valueOf(args.get("content", "storage").toUpperCase(Locale.ROOT)),
                    !Boolean.parseBoolean(args.get("detached", "false")),
                    look
            );
        }
        HangingTrackPiece track = switch (kind) {
            case "straight" -> new HangingTrackPiece.Straight(port, size == 0 ? 12 : size, extent, extent, look);
            case "stairs" -> new HangingTrackPiece.Stairs(port, size == 0 ? 4 : size, extent, extent, look);
            case "diagonal" -> new HangingTrackPiece.Diagonal(port, size == 0 ? 8 : size, extent, look);
            case "corner" -> new HangingTrackPiece.Corner(port, size == 0 ? 1 : size, extent, look);
            case "junction" -> new HangingTrackPiece.Junction(port, args.getInt("extent", 2), 0, look);
            default -> throw new IllegalArgumentException("Unknown piece '" + kind + "'");
        };
        track.link(args.getInt("links", kind.equals("junction") ? 15 : HangingTrackPiece.FORWARD));
        return track;
    }
}
