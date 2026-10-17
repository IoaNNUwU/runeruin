package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.structures.HangingPiece.Look;
import ioann.uwu.runeruin.dimension.structures.HangingPiece.Wear;
import ioann.uwu.runeruin.dimension.structures.HangingPiece.Wood;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;

/**
 * Lays out one network of hanging minecart tracks the way a vanilla mineshaft grows: a station in the middle,
 * then piece after piece from every open end, each kept only if it fits. The tracks are drawn by code; the
 * platforms are templates that a person built.
 */
public final class HangingTracksLayout {

    /** What the layout needs to know of a piece to keep it apart from the others and between the ceiling and the floor. */
    public interface Part {
        /** The deck seen from above, in world coordinates: what the layout keeps other pieces away from. */
        BoundingBox footprint();

        /** The lowest deck block: the chains that hold it are at most {@link #MAX_GAP} blocks long. */
        int lowestDeck();

        /** The highest block that must stay under the ceiling. */
        int top();

        /** The lowest block, which must stay clear of the floor. */
        int bottom();
    }

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
    public static final int MAX_GAP = 28;

    private static final int MAX_DEPTH = 9;
    private static final int MAX_PIECES = 56;
    private static final int STATION_EXTENT = 4;
    /** Free blocks between two decks that are not joined. */
    private static final int SPACING = 2;
    private static final int[] SIDES = {HangingTrackPiece.LEFT, HangingTrackPiece.RIGHT, HangingTrackPiece.FORWARD};

    private final List<Part> pieces = new ArrayList<>();
    private final Map<Part, Part> parents = new IdentityHashMap<>();
    private final int centerX;
    private final int centerZ;
    private final RandomSource random;
    private final Terrain terrain;
    private final StructureTemplateManager templates;
    /** The templates of the platforms where a track ends, and of those that hang beside the tracks. */
    private final List<Identifier> platforms;
    private final List<Identifier> dungeons;
    /** Deck blocks on each side of the rails, at most: 1 makes tracks 3 blocks wide, 2 makes them 3 to 5. */
    private final int extent;
    private final Wood wood;

    private HangingTracksLayout(int centerX, int centerZ, RandomSource random, Terrain terrain, StructureTemplateManager templates) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.random = random;
        this.terrain = terrain;
        this.templates = templates;
        this.platforms = TemplatePiece.variants(templates, HangingPlatformPiece.PLATFORMS);
        this.dungeons = TemplatePiece.variants(templates, HangingPlatformPiece.DUNGEONS);
        this.extent = random.nextInt(3) == 0 ? 1 : 2;
        this.wood = random.nextInt(10) < 7 ? Wood.OAK : Wood.INVERTED_TREE;
    }

    /** The pieces of a network around this block, or nothing when the cave leaves no room for one. */
    public static List<StructurePiece> generate(int centerX, int centerZ, RandomSource random, Terrain terrain, StructureTemplateManager templates) {
        HangingTracksLayout layout = new HangingTracksLayout(centerX, centerZ, random, terrain, templates);

        int ceiling = Integer.MAX_VALUE;
        for (int dx = -STATION_EXTENT; dx <= STATION_EXTENT; dx += 2) {
            for (int dz = -STATION_EXTENT; dz <= STATION_EXTENT; dz += 2) {
                ceiling = Math.min(ceiling, terrain.ceilingY(centerX + dx, centerZ + dz));
            }
        }
        int deckY = ceiling - 1 - random.nextIntBetweenInclusive(8, 14);
        Port start = new Port(centerX, deckY, centerZ + STATION_EXTENT, Direction.NORTH);
        HangingTrackPiece.Junction station = new HangingTrackPiece.Junction(start, STATION_EXTENT, 0, layout.look(null));
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
        return layout.pieces.stream().map(StructurePiece.class::cast).toList();
    }

    /** The look of a piece that grows from this one. Moss spreads: an overgrown piece often has overgrown neighbours. */
    private Look look(HangingPiece parent) {
        int roll = this.random.nextInt(10);
        Wear wear = roll < 5 ? Wear.INTACT : roll < 8 ? Wear.DECAYED : Wear.OVERGROWN;
        if (parent != null && parent.wear == Wear.OVERGROWN && this.random.nextBoolean()) {
            wear = Wear.OVERGROWN;
        }
        return new Look(this.wood, wear, this.random.nextLong());
    }

    private void add(Part piece, Part parent) {
        this.pieces.add(piece);
        this.parents.put(piece, parent);
    }

    /** Attaches a piece to an open end and grows on from it. False when nothing fits: the end stays closed. */
    private boolean grow(HangingPiece parent, Port port, int depth) {
        // The further from the station, the sooner a track ends.
        boolean end = depth > MAX_DEPTH || this.pieces.size() >= MAX_PIECES || this.random.nextInt(2 * MAX_DEPTH) < depth - 3;
        for (int attempt = 0; attempt < 4 && !end; attempt++) {
            HangingTrackPiece track = randomTrack(parent, port, depth);
            if (fits(track, parent)) {
                add(track, parent);
                growFrom(track, depth);
                return true;
            }
        }
        // A track that cannot go on ends with a platform.
        for (int attempt = 0; attempt < 3; attempt++) {
            HangingPlatformPiece platform = randomPlatform(this.platforms, port);
            if (platform != null && fits(platform, parent)) {
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

    private HangingTrackPiece randomTrack(HangingPiece parent, Port port, int depth) {
        int roll = this.random.nextInt(100);
        int left = 1 + this.random.nextInt(this.extent);
        int right = 1 + this.random.nextInt(this.extent);
        Look look = look(parent);
        if (roll < 30) {
            return new HangingTrackPiece.Straight(port, 6 + this.random.nextInt(13), left, right, look);
        }
        if (roll < 54) {
            // A staircase climbs or drops 3 to 7 blocks.
            int steps = 3 + this.random.nextInt(5);
            // Stairs lead back to the middle of the allowed heights.
            int gap = gap(port);
            boolean up = gap - steps < MIN_GAP + 2 ? false : gap + steps > MAX_GAP - 2 || this.random.nextBoolean();
            return new HangingTrackPiece.Stairs(port, up ? steps : -steps, left, right, look);
        }
        if (roll < 66) {
            int shift = 4 + this.random.nextInt(9);
            return new HangingTrackPiece.Diagonal(port, this.random.nextBoolean() ? shift : -shift, this.extent, look);
        }
        if (roll < 78 || depth > MAX_DEPTH - 2) {
            return new HangingTrackPiece.Corner(port, this.random.nextBoolean() ? 1 : -1, this.extent, look);
        }
        return new HangingTrackPiece.Junction(port, this.extent, HangingTrackPiece.BACK, look);
    }

    /** A platform from one of the templates, turned or mirrored at random; null when there is none to take. */
    private @Nullable HangingPlatformPiece randomPlatform(List<Identifier> variants, Port port) {
        if (variants.isEmpty()) {
            return null;
        }
        Identifier template = variants.get(this.random.nextInt(variants.size()));
        Mirror mirror = this.random.nextBoolean() ? Mirror.NONE : Mirror.FRONT_BACK;
        return HangingPlatformPiece.create(this.templates, template, port, mirror, this.wood);
    }

    /** A few dungeons that hang near the tracks without touching them, up to 5 blocks higher or lower. */
    private void addDetachedPlatforms() {
        List<Part> tracks = List.copyOf(this.pieces);
        int wanted = 1 + this.random.nextInt(3) + tracks.size() / 10;
        for (int attempt = 0; attempt < 8 * wanted && wanted > 0; attempt++) {
            Part near = tracks.get(this.random.nextInt(tracks.size()));
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
            int y = near.lowestDeck() + this.random.nextIntBetweenInclusive(-5, 5);
            HangingPlatformPiece platform = randomPlatform(this.dungeons, new Port(x, y, z, away));
            if (platform != null && fits(platform, null)) {
                add(platform, null);
                wanted--;
            }
        }
    }

    /**
     * Whether a piece has room: inside the radius, clear of every other deck, and at every point
     * under the ceiling with its top, at most {@link #MAX_GAP} blocks below it and above the floor.
     */
    private boolean fits(Part piece, @Nullable Part parent) {
        BoundingBox area = piece.footprint();
        if (area.minX() < this.centerX - MAX_RADIUS || area.maxX() > this.centerX + MAX_RADIUS
                || area.minZ() < this.centerZ - MAX_RADIUS || area.maxZ() > this.centerZ + MAX_RADIUS) {
            return false;
        }
        BoundingBox apart = area.inflatedBy(SPACING);
        for (Part other : this.pieces) {
            // A piece touches the one it grows from. A track also comes close to that one's other neighbours;
            // a platform is too wide for that: their beams and chains would hang in its deck.
            boolean joined = parent != null && (other == parent || !(piece instanceof HangingPlatformPiece)
                    && (this.parents.get(other) == parent || other == this.parents.get(parent)));
            if (other.footprint().intersects(joined ? area : apart)) {
                return false;
            }
        }

        for (int x = area.minX(); x <= area.maxX(); x = x == area.maxX() ? x + 1 : Math.min(x + 2, area.maxX())) {
            for (int z = area.minZ(); z <= area.maxZ(); z = z == area.maxZ() ? z + 1 : Math.min(z + 2, area.maxZ())) {
                int ceiling = this.terrain.ceilingY(x, z);
                if (ceiling <= piece.top() || ceiling - piece.lowestDeck() - 1 > MAX_GAP
                        || this.terrain.floorReaches(x, z, piece.bottom() - 2)) {
                    return false;
                }
            }
        }
        return true;
    }
}
