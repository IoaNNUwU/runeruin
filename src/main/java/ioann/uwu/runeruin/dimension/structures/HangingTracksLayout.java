package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.structures.HangingPiece.Look;
import ioann.uwu.runeruin.dimension.structures.HangingPiece.Wear;
import ioann.uwu.runeruin.dimension.structures.HangingPiece.Wood;
import ioann.uwu.runeruin.dimension.structures.HangingPlatformPiece.Content;
import ioann.uwu.runeruin.dimension.structures.HangingPlatformPiece.Shape;
import ioann.uwu.runeruin.dimension.structures.HangingPlatformPiece.Shell;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Lays out one network of hanging minecart tracks the way a vanilla mineshaft grows: a station in the middle,
 * then piece after piece from every open end, each kept only if it fits.
 */
public final class HangingTracksLayout {

    /** Where a piece starts: the deck block under its first rail and the direction the rails run in. */
    public record Port(int x, int y, int z, Direction facing) {
        /** The world X of a place {@code f} blocks forward and {@code r} blocks to the right. */
        public int x(int f, int r) {
            return this.x + this.facing.getStepX() * f + this.facing.getClockWise().getStepX() * r;
        }

        public int z(int f, int r) {
            return this.z + this.facing.getStepZ() * f + this.facing.getClockWise().getStepZ() * r;
        }
    }

    /** What the layout knows about the cave around it. */
    public interface Terrain {
        /** The lowest block of the ceiling in this column. */
        int ceilingY(int x, int z);

        /** Whether the floor of the cave rises to this height. */
        boolean floorReaches(int x, int z, int y);
    }

    /**
     * No deck goes further from the station than this, so two networks never meet:
     * the structure set keeps their stations more than twice as far apart.
     */
    public static final int MAX_RADIUS = 72;
    /** Air between a deck and the ceiling above it. */
    public static final int MIN_GAP = 3;
    public static final int MAX_GAP = 20;

    private static final int MAX_DEPTH = 9;
    private static final int MAX_PIECES = 56;
    private static final int STATION_EXTENT = 4;
    /** Free blocks between two decks that are not joined. */
    private static final int SPACING = 2;
    private static final int[] SIDES = {HangingTrackPiece.LEFT, HangingTrackPiece.RIGHT, HangingTrackPiece.FORWARD};

    private final List<HangingPiece> pieces = new ArrayList<>();
    private final Map<HangingPiece, HangingPiece> parents = new IdentityHashMap<>();
    private final int centerX;
    private final int centerZ;
    private final RandomSource random;
    private final Terrain terrain;
    /** Deck blocks on each side of the rails, at most: 1 makes tracks 3 blocks wide, 2 makes them 3 to 5. */
    private final int extent;
    private final Wood wood;

    private HangingTracksLayout(int centerX, int centerZ, RandomSource random, Terrain terrain) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.random = random;
        this.terrain = terrain;
        this.extent = random.nextInt(3) == 0 ? 1 : 2;
        this.wood = random.nextInt(10) < 7 ? Wood.OAK : Wood.INVERTED_TREE;
    }

    /** The pieces of a network around this block, or nothing when the cave leaves no room for one. */
    public static List<HangingPiece> generate(int centerX, int centerZ, RandomSource random, Terrain terrain) {
        HangingTracksLayout layout = new HangingTracksLayout(centerX, centerZ, random, terrain);

        int ceiling = Integer.MAX_VALUE;
        for (int dx = -STATION_EXTENT; dx <= STATION_EXTENT; dx += 2) {
            for (int dz = -STATION_EXTENT; dz <= STATION_EXTENT; dz += 2) {
                ceiling = Math.min(ceiling, terrain.ceilingY(centerX + dx, centerZ + dz));
            }
        }
        int deckY = ceiling - 1 - random.nextIntBetweenInclusive(8, 14);
        Port start = new Port(centerX, deckY, centerZ + STATION_EXTENT, Direction.NORTH);
        HangingTrackPiece.Junction station = new HangingTrackPiece.Junction(start, STATION_EXTENT, 0, layout.look());
        if (!layout.fits(station, null)) {
            return List.of();
        }
        layout.add(station, null);
        for (int side = HangingTrackPiece.BACK; side <= HangingTrackPiece.FORWARD; side <<= 1) {
            if (layout.grow(station, station.exit(side), 1)) {
                station.link(side);
            }
        }
        // A station with a single track is not worth a network.
        if (layout.pieces.size() < 4) {
            return List.of();
        }
        layout.addDetachedPlatforms();
        return layout.pieces;
    }

    private Look look() {
        int roll = this.random.nextInt(10);
        Wear wear = roll < 5 ? Wear.INTACT : roll < 8 ? Wear.DECAYED : Wear.OVERGROWN;
        return new Look(this.wood, wear, this.random.nextLong());
    }

    private void add(HangingPiece piece, HangingPiece parent) {
        this.pieces.add(piece);
        this.parents.put(piece, parent);
    }

    /** Attaches a piece to an open end and grows on from it. False when nothing fits: the end stays closed. */
    private boolean grow(HangingPiece parent, Port port, int depth) {
        // The further from the station, the sooner a track ends.
        boolean end = depth > MAX_DEPTH || this.pieces.size() >= MAX_PIECES || this.random.nextInt(2 * MAX_DEPTH) < depth - 3;
        for (int attempt = 0; attempt < 4 && !end; attempt++) {
            HangingTrackPiece track = randomTrack(port, depth);
            if (fits(track, parent)) {
                add(track, parent);
                growFrom(track, depth);
                return true;
            }
        }
        // A track that cannot go on ends with a platform; each attempt tries a smaller one.
        for (int attempt = 0; attempt < 3; attempt++) {
            HangingPlatformPiece platform = randomPlatform(port, true, attempt);
            if (fits(platform, parent)) {
                add(platform, parent);
                return true;
            }
        }
        return false;
    }

    private void growFrom(HangingTrackPiece track, int depth) {
        if (track instanceof HangingTrackPiece.Junction) {
            // Three tracks meet more often than four.
            int skipped = this.random.nextInt(4);
            for (int i = 0; i < SIDES.length; i++) {
                if (i != skipped && grow(track, track.exit(SIDES[i]), depth + 1)) {
                    track.link(SIDES[i]);
                }
            }
        } else if (grow(track, track.exit(HangingTrackPiece.FORWARD), depth + 1)) {
            track.link(HangingTrackPiece.FORWARD);
        }
    }

    private int gap(Port port) {
        return this.terrain.ceilingY(port.x(), port.z()) - port.y() - 1;
    }

    private HangingTrackPiece randomTrack(Port port, int depth) {
        int roll = this.random.nextInt(100);
        int left = 1 + this.random.nextInt(this.extent);
        int right = 1 + this.random.nextInt(this.extent);
        if (roll < 34) {
            return new HangingTrackPiece.Straight(port, 6 + this.random.nextInt(13), left, right, look());
        }
        if (roll < 48) {
            int steps = 2 + this.random.nextInt(4);
            // Stairs lead back to the middle of the allowed heights.
            int gap = gap(port);
            boolean up = gap - steps < MIN_GAP + 2 ? false : gap + steps > MAX_GAP - 2 || this.random.nextBoolean();
            return new HangingTrackPiece.Stairs(port, up ? steps : -steps, left, right, look());
        }
        if (roll < 62) {
            int shift = 4 + this.random.nextInt(9);
            return new HangingTrackPiece.Diagonal(port, this.random.nextBoolean() ? shift : -shift, this.extent, look());
        }
        if (roll < 75 || depth > MAX_DEPTH - 2) {
            return new HangingTrackPiece.Corner(port, this.random.nextBoolean() ? 1 : -1, this.extent, look());
        }
        return new HangingTrackPiece.Junction(port, this.extent, HangingTrackPiece.BACK, look());
    }

    private HangingPlatformPiece randomPlatform(Port port, boolean connected, int attempt) {
        Shape shape = Shape.SQUARE;
        int size = 5;
        if (attempt == 0) {
            int roll = this.random.nextInt(10);
            shape = roll < 6 ? Shape.SQUARE : roll < 8 ? Shape.L : Shape.T;
            size = 5 + 2 * this.random.nextInt(3);
        } else if (attempt == 1) {
            size = 7;
        }

        Content content = randomContent(connected);
        Shell shell;
        if (content == Content.LOOKOUT) {
            shell = Shell.RAILING;
        } else if (content == Content.COLLAPSED) {
            shell = Shell.OPEN;
        } else {
            int roll = this.random.nextInt(10);
            boolean roomFits = gap(port) > HangingPlatformPiece.ROOM_HEIGHT + 1;
            shell = roomFits && roll < 5 ? Shell.ROOM : roll < 8 ? Shell.RAILING : Shell.OPEN;
        }
        if (shape == Shape.SQUARE && size == 9 && shell == Shell.ROOM && this.random.nextInt(3) == 0) {
            content = Content.TREASURE;
        }
        return new HangingPlatformPiece(port, shape, size, this.random.nextBoolean() ? 1 : -1, shell, content, connected, look());
    }

    private Content randomContent(boolean connected) {
        int roll = this.random.nextInt(100);
        if (roll < 28) {
            return Content.STORAGE;
        }
        if (roll < 50) {
            return Content.SPAWNER;
        }
        if (roll < 64) {
            return Content.CAMP;
        }
        if (roll < 76) {
            return Content.WORKSHOP;
        }
        if (roll < 84) {
            return Content.LOOKOUT;
        }
        if (roll < 92) {
            return Content.COLLAPSED;
        }
        return connected ? Content.DEPOT : Content.STORAGE;
    }

    /** A few platforms that hang near the tracks without touching them, up to 5 blocks higher or lower. */
    private void addDetachedPlatforms() {
        List<HangingPiece> tracks = List.copyOf(this.pieces);
        int wanted = 1 + this.random.nextInt(3) + tracks.size() / 10;
        for (int attempt = 0; attempt < 8 * wanted && wanted > 0; attempt++) {
            HangingPiece near = tracks.get(this.random.nextInt(tracks.size()));
            BoundingBox area = near.footprint();
            // The platform turns its entrance to the track.
            Direction away = Direction.Plane.HORIZONTAL.getRandomDirection(this.random);
            int distance = SPACING + 1 + this.random.nextInt(4);
            int x = switch (away) {
                case EAST -> area.maxX() + distance;
                case WEST -> area.minX() - distance;
                default -> this.random.nextIntBetweenInclusive(area.minX(), area.maxX());
            };
            int z = switch (away) {
                case SOUTH -> area.maxZ() + distance;
                case NORTH -> area.minZ() - distance;
                default -> this.random.nextIntBetweenInclusive(area.minZ(), area.maxZ());
            };
            int y = near.origin.y() + this.random.nextIntBetweenInclusive(-5, 5);
            HangingPlatformPiece platform = randomPlatform(new Port(x, y, z, away), false, this.random.nextInt(3));
            if (fits(platform, null)) {
                add(platform, null);
                wanted--;
            }
        }
    }

    /**
     * Whether a piece has room: inside the radius, clear of every other deck, and at every point
     * between {@link #MIN_GAP} and {@link #MAX_GAP} blocks under the ceiling and above the floor.
     */
    private boolean fits(HangingPiece piece, HangingPiece parent) {
        BoundingBox area = piece.footprint();
        if (area.minX() < this.centerX - MAX_RADIUS || area.maxX() > this.centerX + MAX_RADIUS
                || area.minZ() < this.centerZ - MAX_RADIUS || area.maxZ() > this.centerZ + MAX_RADIUS) {
            return false;
        }
        BoundingBox apart = area.inflatedBy(SPACING);
        for (HangingPiece other : this.pieces) {
            // A piece touches the one it grows from and comes close to that one's other neighbours.
            boolean joined = parent != null
                    && (other == parent || this.parents.get(other) == parent || other == this.parents.get(parent));
            if (other.footprint().intersects(joined ? area : apart)) {
                return false;
            }
        }

        int[] levels = piece.deckLevels();
        int lowest = piece.origin.y() + levels[0];
        int highest = piece.origin.y() + levels[1];
        for (int x = area.minX(); x <= area.maxX(); x = x == area.maxX() ? x + 1 : Math.min(x + 2, area.maxX())) {
            for (int z = area.minZ(); z <= area.maxZ(); z = z == area.maxZ() ? z + 1 : Math.min(z + 2, area.maxZ())) {
                int ceiling = this.terrain.ceilingY(x, z);
                if (ceiling - highest - 1 < piece.headroom() || ceiling - lowest - 1 > MAX_GAP
                        || this.terrain.floorReaches(x, z, lowest - HangingPiece.BELOW_DECK - 2)) {
                    return false;
                }
            }
        }
        return true;
    }
}
