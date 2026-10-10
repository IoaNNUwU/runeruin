package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import ioann.uwu.runeruin.dimension.structures.HangingTracksLayout.Port;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A stretch of hanging track: a wooden deck with runic rails that run from its start to every attached end
 * without a gap.
 */
public abstract class HangingTrackPiece extends HangingPiece {

    private enum Kind { STRAIGHT, STAIRS, DIAGONAL, CORNER, JUNCTION }

    public static final int BACK = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 4;
    public static final int FORWARD = 8;

    /** Read by each kind in its own way: a length, a number of steps, a turn. */
    protected final int size;
    /** Deck blocks on each side of the rails. */
    protected final int left;
    protected final int right;
    /** What is attached: FORWARD for the far end of a track, any of the four sides of a junction. */
    protected int links;

    protected HangingTrackPiece(Port port, int size, int left, int right, Look look, BoundingBox box) {
        super(RRStructurePieceTypes.HANGING_TRACK.get(), port, look, box);
        this.size = size;
        this.left = left;
        this.right = right;
    }

    protected HangingTrackPiece(CompoundTag tag) {
        super(RRStructurePieceTypes.HANGING_TRACK.get(), tag);
        this.size = tag.getIntOr("Size", 1);
        this.left = tag.getIntOr("Left", 1);
        this.right = tag.getIntOr("Right", 1);
        this.links = tag.getIntOr("Links", 0);
    }

    public static HangingTrackPiece load(CompoundTag tag) {
        return switch (Kind.values()[tag.getIntOr("Kind", 0)]) {
            case STRAIGHT -> new Straight(tag);
            case STAIRS -> new Stairs(tag);
            case DIAGONAL -> new Diagonal(tag);
            case CORNER -> new Corner(tag);
            case JUNCTION -> new Junction(tag);
        };
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putInt("Kind", kind().ordinal());
        tag.putInt("Size", this.size);
        tag.putInt("Left", this.left);
        tag.putInt("Right", this.right);
        tag.putInt("Links", this.links);
    }

    protected abstract Kind kind();

    /** Where the piece attached to this side starts. */
    public abstract Port exit(int side);

    public void link(int side) {
        this.links |= side;
    }

    protected Port port(int f, int u, int r, Direction facing) {
        return new Port(this.origin.x(f, r), this.origin.y() + u, this.origin.z(f, r), facing);
    }

    /**
     * Rails along the cells {f, r, deck level}. The first and the last cell belong to the neighbours and only
     * give the direction; a track that nothing is attached to ends with a stop on its last cell.
     */
    protected void rails(Canvas canvas, int[][] path) {
        for (int i = 1; i < path.length - 1; i++) {
            int[] cell = path[i];
            Direction toPrevious = direction(cell, path[i - 1]);
            Direction toNext = direction(cell, path[i + 1]);
            if (i == path.length - 2 && (this.links & FORWARD) == 0) {
                canvas.buffer(cell[0], cell[2] + 1, cell[1], toPrevious.getClockWise().getAxis());
                continue;
            }
            RailShape shape;
            if (path[i + 1][2] > cell[2]) {
                shape = slopeRail(toNext);
            } else if (path[i - 1][2] > cell[2]) {
                shape = slopeRail(toPrevious);
            } else {
                shape = flatRail(toPrevious, toNext);
            }
            canvas.rail(cell[0], cell[2] + 1, cell[1], shape);
        }
    }

    private Direction direction(int[] from, int[] to) {
        if (to[0] != from[0]) {
            return to[0] > from[0] ? this.facing : this.facing.getOpposite();
        }
        return side(to[1] - from[1]);
    }

    /** A log beam under the deck, across the track, with a chain from each end up to the ceiling. */
    protected void hanger(Canvas canvas, int f, int u) {
        for (int r = -this.left - 1; r <= this.right + 1; r++) {
            canvas.put(f, u - 1, r, this.wood.log(side(1).getAxis()));
        }
        canvas.chain(f, u, -this.left - 1);
        canvas.chain(f, u, this.right + 1);
        if (chance(f, 0, 8) < 0.4f) {
            canvas.hangingLantern(f, u - 2, chance(f, 0, 9) < 0.5f ? -this.left - 1 : this.right + 1);
        }
    }

    /** One row of a deck across the track, with fences along the edges that are a block away from the rails. */
    protected void row(Canvas canvas, int f, int u, boolean mossy, boolean hanger) {
        for (int r = -this.left; r <= this.right; r++) {
            canvas.deck(f, u, r, mossy, 1);
        }
        if (hanger) {
            hanger(canvas, f, u);
        }
        for (int sign = -1; sign <= 1; sign += 2) {
            int extent = sign < 0 ? this.left : this.right;
            if (extent > 1 && chance(f >> 2, sign, 10) < 0.35f) {
                canvas.fence(f, u + 1, sign * extent);
            }
            if (!hanger) {
                canvas.vines(f, u, sign * (extent + 1), side(-sign), mossy);
            }
        }
    }

    /** A square deck of 2e + 1 blocks that hangs by its corners; e is the distance from the middle to an edge. */
    protected void squareDeck(Canvas canvas, int e) {
        for (int f = 0; f <= 2 * e; f++) {
            for (int r = -e; r <= e; r++) {
                canvas.deck(f, 0, r, mossy(f, 2 * e + 1), 1);
            }
        }
        for (int f = 0; f <= 2 * e; f += 2 * e) {
            canvas.chain(f, 1, -e);
            canvas.chain(f, 1, e);
        }
    }

    /** A straight track of {@code size} blocks. */
    public static final class Straight extends HangingTrackPiece {

        public Straight(Port port, int length, int left, int right, Look look) {
            super(port, length, left, right, look, box(port, 0, length - 1, -left - 1, right + 1, BELOW_DECK));
        }

        private Straight(CompoundTag tag) {
            super(tag);
        }

        @Override
        protected Kind kind() {
            return Kind.STRAIGHT;
        }

        @Override
        public BoundingBox footprint() {
            return box(this.origin, 0, this.size - 1, -this.left, this.right, 0);
        }

        @Override
        public Port exit(int side) {
            return port(this.size, 0, 0, this.facing);
        }

        @Override
        protected void paint(Canvas canvas) {
            int[][] path = new int[this.size + 2][];
            for (int f = -1; f <= this.size; f++) {
                path[f + 1] = new int[]{f, 0, 0};
            }
            for (int f = 0; f < this.size; f++) {
                row(canvas, f, 0, mossy(f, this.size), f % 5 == 2);
            }
            rails(canvas, path);
        }
    }

    /** A staircase track: steps two blocks long and one block high, {@code size} of them up or, when negative, down. */
    public static final class Stairs extends HangingTrackPiece {

        public Stairs(Port port, int steps, int left, int right, Look look) {
            super(port, steps, left, right, look,
                    box(port, 0, 2 * Math.abs(steps) - 1, -left - 1, right + 1, BELOW_DECK + Math.max(0, -steps)));
        }

        private Stairs(CompoundTag tag) {
            super(tag);
        }

        @Override
        protected Kind kind() {
            return Kind.STAIRS;
        }

        private int length() {
            return 2 * Math.abs(this.size);
        }

        /** Going up, a sloped rail leads onto each step; going down, it leads off it. */
        private int level(int f) {
            return Integer.signum(this.size) * ((f + 1) / 2);
        }

        @Override
        public BoundingBox footprint() {
            return box(this.origin, 0, length() - 1, -this.left, this.right, 0);
        }

        @Override
        public int[] deckLevels() {
            return new int[]{Math.min(0, this.size), Math.max(0, this.size)};
        }

        @Override
        public Port exit(int side) {
            return port(length(), this.size, 0, this.facing);
        }

        @Override
        protected void paint(Canvas canvas) {
            int length = length();
            int[][] path = new int[length + 2][];
            path[0] = new int[]{-1, 0, 0};
            path[length + 1] = new int[]{length, 0, this.size};
            for (int f = 0; f < length; f++) {
                int level = level(f);
                path[f + 1] = new int[]{f, 0, level};
                // A beam needs a flat rail above it: the upper block of a step going up, the lower one going down.
                row(canvas, f, level, mossy(f, length), f % 4 == (this.size > 0 ? 1 : 0));
            }
            rails(canvas, path);
        }
    }

    /**
     * A track at 45°: the rails zigzag {@code size} blocks to the right or, when negative, to the left,
     * and leave in the direction they came in.
     */
    public static final class Diagonal extends HangingTrackPiece {

        public Diagonal(Port port, int shift, int extent, Look look) {
            super(port, shift, extent, extent, look, box(port, 0, Math.abs(shift) + 1,
                    Math.min(0, shift) - extent, Math.max(0, shift) + extent, BELOW_DECK));
        }

        private Diagonal(CompoundTag tag) {
            super(tag);
        }

        @Override
        protected Kind kind() {
            return Kind.DIAGONAL;
        }

        private int steps() {
            return Math.abs(this.size);
        }

        @Override
        public BoundingBox footprint() {
            return box(this.origin, 0, steps() + 1, Math.min(0, this.size) - this.left, Math.max(0, this.size) + this.left, 0);
        }

        @Override
        public Port exit(int side) {
            return port(steps() + 2, 0, this.size, this.facing);
        }

        private int[][] path() {
            int steps = steps();
            int sign = Integer.signum(this.size);
            int[][] path = new int[2 * steps + 4][];
            path[0] = new int[]{-1, 0, 0};
            path[1] = new int[]{0, 0, 0};
            for (int i = 1; i <= steps; i++) {
                path[2 * i] = new int[]{i, (i - 1) * sign, 0};
                path[2 * i + 1] = new int[]{i, i * sign, 0};
            }
            path[2 * steps + 2] = new int[]{steps + 1, this.size, 0};
            path[2 * steps + 3] = new int[]{steps + 2, this.size, 0};
            return path;
        }

        /** The deck is every block within {@link #left} steps of the rails. */
        private static int distance(int[][] path, int f, int r) {
            int distance = Integer.MAX_VALUE;
            for (int i = 1; i < path.length - 1; i++) {
                distance = Math.min(distance, Math.abs(path[i][0] - f) + Math.abs(path[i][1] - r));
            }
            return distance;
        }

        @Override
        protected void paint(Canvas canvas) {
            int steps = steps();
            int sign = Integer.signum(this.size);
            int extent = this.left;
            int[][] path = path();
            for (int f = 0; f <= steps + 1; f++) {
                for (int r = Math.min(0, this.size) - extent; r <= Math.max(0, this.size) + extent; r++) {
                    if (distance(path, f, r) <= extent) {
                        canvas.deck(f, 0, r, mossy(f, steps + 2), 1);
                    }
                }
            }
            for (int i = extent; i <= steps; i += 3) {
                canvas.chain(i - extent, 1, i * sign);
                if (i + extent <= steps + 1) {
                    canvas.chain(i + extent, 1, (i - 1) * sign);
                }
            }
            rails(canvas, path);
        }
    }

    /** A square deck on which the rails turn right ({@code size} 1) or left (-1). */
    public static final class Corner extends HangingTrackPiece {

        public Corner(Port port, int turn, int extent, Look look) {
            super(port, turn, extent, extent, look, box(port, 0, 2 * extent, -extent, extent, BELOW_DECK));
        }

        private Corner(CompoundTag tag) {
            super(tag);
        }

        @Override
        protected Kind kind() {
            return Kind.CORNER;
        }

        @Override
        public BoundingBox footprint() {
            return box(this.origin, 0, 2 * this.left, -this.left, this.left, 0);
        }

        @Override
        public Port exit(int side) {
            return port(this.left, 0, this.size * (this.left + 1), side(this.size));
        }

        @Override
        protected void paint(Canvas canvas) {
            int extent = this.left;
            squareDeck(canvas, extent);
            int[][] path = new int[2 * extent + 3][];
            for (int f = -1; f <= extent; f++) {
                path[f + 1] = new int[]{f, 0, 0};
            }
            for (int i = 1; i <= extent + 1; i++) {
                path[extent + 1 + i] = new int[]{extent, i * this.size, 0};
            }
            rails(canvas, path);
        }
    }

    /**
     * A square deck where tracks meet. Three tracks make a switch with a lever, four make a crossing.
     * The largest one is the station a network grows from.
     */
    public static final class Junction extends HangingTrackPiece {

        private static final int[][] STEPS = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};

        public Junction(Port port, int extent, int links, Look look) {
            super(port, 0, extent, extent, look, box(port, 0, 2 * extent, -extent, extent, BELOW_DECK));
            this.links = links;
        }

        private Junction(CompoundTag tag) {
            super(tag);
        }

        @Override
        protected Kind kind() {
            return Kind.JUNCTION;
        }

        @Override
        public BoundingBox footprint() {
            return box(this.origin, 0, 2 * this.left, -this.left, this.left, 0);
        }

        @Override
        public Port exit(int side) {
            int extent = this.left;
            return switch (side) {
                case BACK -> port(-1, 0, 0, this.facing.getOpposite());
                case LEFT -> port(extent, 0, -extent - 1, side(-1));
                case RIGHT -> port(extent, 0, extent + 1, side(1));
                default -> port(2 * extent + 1, 0, 0, this.facing);
            };
        }

        @Override
        protected void paint(Canvas canvas) {
            int extent = this.left;
            squareDeck(canvas, extent);
            if (extent > 2) {
                stationRailing(canvas, extent);
            }

            Direction[] directions = {this.facing.getOpposite(), side(-1), side(1), this.facing};
            List<Direction> linked = new ArrayList<>();
            int free = 0;
            for (int side = 0; side < 4; side++) {
                if ((this.links & 1 << side) == 0) {
                    free = side;
                    continue;
                }
                linked.add(directions[side]);
                for (int i = 1; i <= extent; i++) {
                    canvas.rail(extent + STEPS[side][0] * i, 1, STEPS[side][1] * i,
                            flatRail(directions[side], directions[side].getOpposite()));
                }
            }
            switch (linked.size()) {
                case 1 -> canvas.buffer(extent, 1, 0, linked.get(0).getClockWise().getAxis());
                case 2 -> canvas.rail(extent, 1, 0, flatRail(linked.get(0), linked.get(1)));
                case 3 -> {
                    canvas.rail(extent, 1, 0, switchRail(linked));
                    canvas.put(extent + STEPS[free][0], 1, STEPS[free][1], Blocks.LEVER.defaultBlockState()
                            .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                            .setValue(LeverBlock.FACING, directions[free]));
                }
                // A runic rail lets a minecart cross whichever way it lies.
                case 4 -> canvas.rail(extent, 1, 0, RailShape.NORTH_SOUTH);
                default -> { }
            }
        }

        /** The curve a rail with three neighbours takes without a redstone signal; the lever beside it flips it. */
        private static RailShape switchRail(List<Direction> linked) {
            boolean east = linked.contains(Direction.EAST);
            if (linked.contains(Direction.SOUTH)) {
                return east ? RailShape.SOUTH_EAST : RailShape.SOUTH_WEST;
            }
            return east ? RailShape.NORTH_EAST : RailShape.NORTH_WEST;
        }

        private void stationRailing(Canvas canvas, int extent) {
            for (int f = 0; f <= 2 * extent; f++) {
                for (int r = -extent; r <= extent; r++) {
                    boolean border = f == 0 || f == 2 * extent || Math.abs(r) == extent;
                    boolean corner = (f == 0 || f == 2 * extent) && Math.abs(r) == extent;
                    // The rails leave through the middle of each side.
                    if (!border || corner || Math.abs(r) <= 1 || Math.abs(f - extent) <= 1) {
                        continue;
                    }
                    canvas.fence(f, 1, r);
                    if (Math.abs(r) == extent - 1 && (f == 0 || f == 2 * extent)) {
                        canvas.put(f, 2, r, Blocks.LANTERN.defaultBlockState());
                    }
                }
            }
        }
    }
}
