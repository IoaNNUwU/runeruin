package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.dimension.RRStructurePieceTypes;
import ioann.uwu.runeruin.dimension.structures.HangingTracksLayout.Port;
import ioann.uwu.runeruin.loottables.RRLootTables;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A wide platform, two blocks thick, on log beams that hang on chains: at the end of a track, with the rails
 * running onto it, or on its own near a track. What stands on it is its theme.
 */
public final class HangingPlatformPiece extends HangingPiece {

    public enum Shape { RECTANGLE, ROUND, L, T, PLUS }

    public enum Theme {
        MOSS_POND, HUTS, TREASURE_HUT, CAMP, STORAGE, SPAWNER, WORKSHOP, DEPOT, LOOKOUT;

        /** Whether a hut or an awning stands on the platform. */
        public boolean roofed() {
            return this == HUTS || this == TREASURE_HUT || this == STORAGE || this == WORKSHOP;
        }
    }

    /** The roof of a hut lies this high above the deck. */
    public static final int ROOF = 4;

    private static final int THICKNESS = 2;
    /** The arms of the L, the T and the plus are 7 blocks wide: this far to each side of their middle. */
    private static final int ARM = 3;
    private static final int HUT = 5;

    private final Shape shape;
    /** Blocks across the entrance, an odd number, and from the entrance to the far edge. */
    private final int width;
    private final int depth;
    /** The side the leg of the L points to: 1 right, -1 left. */
    private final int mirror;
    private final Theme theme;
    /** Whether a track ends here. */
    private final boolean connected;

    public HangingPlatformPiece(Port port, Shape shape, int width, int depth, int mirror, Theme theme, boolean connected, Look look) {
        super(RRStructurePieceTypes.HANGING_PLATFORM.get(), port, look, bounds(port, shape, width, depth, mirror, 1, BELOW_DECK));
        this.shape = shape;
        this.width = width;
        this.depth = depth;
        this.mirror = mirror;
        this.theme = theme;
        this.connected = connected;
    }

    public HangingPlatformPiece(CompoundTag tag) {
        super(RRStructurePieceTypes.HANGING_PLATFORM.get(), tag);
        this.shape = Shape.values()[tag.getIntOr("Shape", 0)];
        this.width = tag.getIntOr("Width", 7);
        this.depth = tag.getIntOr("Depth", 7);
        this.mirror = tag.getIntOr("Mirror", 1);
        this.theme = Theme.values()[tag.getIntOr("Theme", 0)];
        this.connected = tag.getBooleanOr("Connected", false);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putInt("Shape", this.shape.ordinal());
        tag.putInt("Width", this.width);
        tag.putInt("Depth", this.depth);
        tag.putInt("Mirror", this.mirror);
        tag.putInt("Theme", this.theme.ordinal());
        tag.putBoolean("Connected", this.connected);
    }

    /** The smallest and the largest r of the shape. */
    private static int[] sides(Shape shape, int width, int mirror) {
        if (shape != Shape.L) {
            return new int[]{-width / 2, width / 2};
        }
        return mirror > 0 ? new int[]{-ARM, width - ARM - 1} : new int[]{ARM + 1 - width, ARM};
    }

    /** The world box of the shape, with a margin for what hangs beside it: beam ends, chains and vines. */
    private static BoundingBox bounds(Port port, Shape shape, int width, int depth, int mirror, int margin, int below) {
        int[] sides = sides(shape, width, mirror);
        return box(port, 0, depth - 1 + margin, sides[0] - margin, sides[1] + margin, below);
    }

    @Override
    public BoundingBox footprint() {
        return bounds(this.origin, this.shape, this.width, this.depth, this.mirror, 0, 0);
    }

    @Override
    public int headroom() {
        return this.theme.roofed() ? ROOF + 2 : super.headroom();
    }

    private boolean contains(int f, int r) {
        if (f < 0 || f >= this.depth) {
            return false;
        }
        int half = this.width / 2;
        int far = this.depth - 2 * ARM - 1;
        return switch (this.shape) {
            case RECTANGLE -> Math.abs(r) <= half;
            case ROUND -> {
                double middle = (this.depth - 1) / 2.0;
                yield (f - middle) * (f - middle) + r * r <= (half + 0.4) * (half + 0.4);
            }
            case L -> Math.abs(r) <= ARM || f >= far && r * this.mirror > 0 && r * this.mirror < this.width - ARM;
            case T -> Math.abs(r) <= ARM || f >= far && Math.abs(r) <= half;
            case PLUS -> Math.abs(r) <= ARM || Math.abs(2 * f - this.depth + 1) <= 2 * ARM && Math.abs(r) <= half;
        };
    }

    private boolean border(int f, int r) {
        return !contains(f - 1, r) || !contains(f + 1, r) || !contains(f, r - 1) || !contains(f, r + 1);
    }

    /** The way in: the rails and the stop behind them. */
    private boolean lane(int f, int r) {
        return r == 0 && f <= rails();
    }

    /** How far the rails run onto the platform: through a depot, two blocks otherwise. */
    private int rails() {
        if (!this.connected) {
            return 0;
        }
        return this.theme == Theme.DEPOT ? this.depth - 3 : 2;
    }

    private boolean railing() {
        return this.theme == Theme.LOOKOUT || this.theme == Theme.TREASURE_HUT || chance(0, 0, 40) < 0.45f;
    }

    /** A moss pond is all moss; an overgrown platform has a round patch of it. */
    private boolean moss(int f, int r) {
        if (this.theme == Theme.MOSS_POND) {
            return true;
        }
        if (this.wear != Wear.OVERGROWN) {
            return false;
        }
        double df = f - chance(0, 0, 32) * this.depth;
        double dr = r - (chance(0, 0, 33) - 0.5) * this.width;
        double radius = 3 + chance(0, 0, 34) * 3 + chance(f, r, 35);
        return df * df + dr * dr <= radius * radius;
    }

    /** The pond lies behind the middle, clear of the rails, with at least a block of moss around it. */
    private boolean pond(int f, int r) {
        if (this.theme != Theme.MOSS_POND || !contains(f, r) || border(f, r)) {
            return false;
        }
        int middle = this.depth / 2 + 1;
        double radius = Math.min(3, middle - rails() - 2) - 0.6 + chance(f, r, 24);
        return (f - middle) * (f - middle) + r * r <= radius * radius;
    }

    @Override
    protected void paint(Canvas canvas) {
        int[] sides = sides(this.shape, this.width, this.mirror);
        boolean railing = railing();
        List<int[]> spots = new ArrayList<>();

        for (int f = 0; f < this.depth; f++) {
            // Beams two blocks from the near and the far edge, and one more under the middle of a long platform.
            boolean beam = f == 2 || f == this.depth - 3 || this.depth > 12 && f == this.depth / 2;
            for (int r = sides[0]; r <= sides[1]; r++) {
                if (!contains(f, r)) {
                    continue;
                }
                if (pond(f, r)) {
                    canvas.put(f, 0, r, Blocks.WATER.defaultBlockState());
                    canvas.put(f, -1, r, this.wood.planks());
                    if (chance(f, r, 25) < 0.2f) {
                        canvas.put(f, 1, r, Blocks.LILY_PAD.defaultBlockState());
                    }
                    continue;
                }
                boolean moss = moss(f, r);
                canvas.deck(f, 0, r, moss, THICKNESS);
                if (!border(f, r)) {
                    if (!lane(f, r)) {
                        spots.add(new int[]{f, r});
                    }
                    continue;
                }
                if (railing && !(f == 0 && Math.abs(r) <= 1)) {
                    canvas.fence(f, 1, r);
                }
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    int nf = f + step(direction, this.facing);
                    int nr = r + step(direction, side(1));
                    // Not in front, where the track is, and not where the chains of a beam hang.
                    if (nf >= 0 && !contains(nf, nr) && !beam) {
                        canvas.vines(nf, 0, nr, direction.getOpposite(), moss);
                    }
                }
            }
            if (beam) {
                beams(canvas, f, sides);
            }
        }

        int rails = rails();
        for (int f = 0; f < rails; f++) {
            canvas.rail(f, 1, 0, flatRail(this.facing, this.facing.getOpposite()));
        }
        if (this.connected) {
            canvas.buffer(rails, 1, 0, side(1).getAxis());
        }
        furnish(canvas, spots);
    }

    /** How far a step in a world direction goes along a local axis. */
    private static int step(Direction direction, Direction axis) {
        return direction == axis ? 1 : direction == axis.getOpposite() ? -1 : 0;
    }

    /** A log beam under each stretch of this row of the deck, with a chain from each end up to the ceiling. */
    private void beams(Canvas canvas, int f, int[] sides) {
        for (int r = sides[0]; r <= sides[1]; r++) {
            if (!contains(f, r)) {
                continue;
            }
            int start = r;
            while (contains(f, r + 1)) {
                r++;
            }
            for (int i = start - 1; i <= r + 1; i++) {
                canvas.put(f, -THICKNESS, i, this.wood.log(side(1).getAxis()));
            }
            canvas.chain(f, 1 - THICKNESS, start - 1);
            canvas.chain(f, 1 - THICKNESS, r + 1);
            if (chance(f, start, 8) < 0.5f) {
                canvas.hangingLantern(f, -THICKNESS - 1, chance(f, start, 9) < 0.5f ? start - 1 : r + 1);
            }
        }
    }

    /** Sets up the theme on the free blocks of the deck, the same in every chunk: the order comes from the seed. */
    private void furnish(Canvas canvas, List<int[]> spots) {
        RandomSource random = RandomSource.create(this.seed);
        for (int i = spots.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] swapped = spots.get(i);
            spots.set(i, spots.get(j));
            spots.set(j, swapped);
        }
        List<int[]> sites = new ArrayList<>();
        Direction front = this.facing.getOpposite();

        switch (this.theme) {
            case MOSS_POND -> {
                Iterator<int[]> free = spots.iterator();
                for (int i = 0; i < 2 && free.hasNext(); i++) {
                    int[] spot = free.next();
                    canvas.fence(spot[0], 1, spot[1]);
                    canvas.put(spot[0], 2, spot[1], Blocks.LANTERN.defaultBlockState());
                }
            }
            case HUTS -> {
                int[] first = site(spots, sites, HUT);
                if (first != null) {
                    hut(canvas, first, random.nextBoolean() ? Theme.CAMP : Theme.STORAGE);
                }
                int[] second = site(spots, sites, HUT);
                if (second != null) {
                    hut(canvas, second, random.nextBoolean() ? Theme.SPAWNER : Theme.WORKSHOP);
                }
                barrels(canvas, free(spots, sites), 2);
            }
            case TREASURE_HUT -> {
                int[] site = site(spots, sites, HUT);
                if (site != null) {
                    hut(canvas, site, Theme.TREASURE_HUT);
                }
                cobwebs(canvas, free(spots, sites), 4);
            }
            case CAMP -> {
                Iterator<int[]> free = spots.iterator();
                if (free.hasNext()) {
                    int[] fire = free.next();
                    canvas.put(fire[0], 1, fire[1], Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                    sites.add(new int[]{fire[0], fire[1], 1, 1});
                    // Logs to sit on, two blocks from the fire.
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        int f = fire[0] + 2 * step(direction, this.facing);
                        int r = fire[1] + 2 * step(direction, side(1));
                        if (contains(f, r) && !border(f, r) && !lane(f, r)) {
                            canvas.put(f, 1, r, this.wood.log(direction.getClockWise().getAxis()));
                            sites.add(new int[]{f, r, 1, 1});
                        }
                    }
                    free = free(spots, sites);
                }
                place(canvas, free, Blocks.CRAFTING_TABLE.defaultBlockState());
                place(canvas, free, facing(Blocks.FURNACE, front));
                barrels(canvas, free, 2);
            }
            case STORAGE, WORKSHOP -> {
                int[] site = site(spots, sites, 3);
                if (site != null) {
                    awning(canvas, site, front);
                }
                barrels(canvas, free(spots, sites), this.theme == Theme.STORAGE ? 3 + spots.size() / 12 : 1);
            }
            case SPAWNER -> {
                Iterator<int[]> free = spots.iterator();
                if (free.hasNext()) {
                    int[] spot = free.next();
                    canvas.spawner(spot[0], 1, spot[1], mob());
                }
                chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
                cobwebs(canvas, free, 3 + spots.size() / 8);
            }
            case DEPOT -> {
                chestMinecart(canvas);
                Iterator<int[]> free = spots.iterator();
                chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
                barrels(canvas, free, 3);
            }
            case LOOKOUT -> {
                Iterator<int[]> free = spots.iterator();
                place(canvas, free, Blocks.LANTERN.defaultBlockState());
                place(canvas, free, Blocks.LANTERN.defaultBlockState());
            }
        }
    }

    /** The free spots that no building stands on or opens to. */
    private static Iterator<int[]> free(List<int[]> spots, List<int[]> sites) {
        return spots.stream().filter(spot -> sites.stream().noneMatch(site ->
                spot[0] >= site[0] - 1 && spot[0] < site[0] + site[2] && spot[1] >= site[1] && spot[1] < site[1] + site[3])).iterator();
    }

    /**
     * A free rectangle {f, r, length, width} for a hut or an awning, 5 blocks wide: a block away from the edge,
     * the way in and the other buildings. Null when the platform has no room left.
     */
    private int[] site(List<int[]> spots, List<int[]> sites, int length) {
        for (int[] spot : spots) {
            if (clear(spot[0], spot[1], length, sites)) {
                int[] site = {spot[0], spot[1], length, HUT};
                sites.add(site);
                return site;
            }
        }
        return null;
    }

    private boolean clear(int f0, int r0, int length, List<int[]> sites) {
        // A building opens towards the way in, so the row in front of it stays free too.
        for (int f = f0 - 1; f < f0 + length; f++) {
            for (int r = r0; r < r0 + HUT; r++) {
                if (!contains(f, r) || border(f, r) || lane(f, r) || pond(f, r)) {
                    return false;
                }
            }
        }
        for (int[] site : sites) {
            if (f0 <= site[0] + site[2] && site[0] <= f0 + length && r0 <= site[1] + site[3] && site[1] <= r0 + HUT) {
                return false;
            }
        }
        return true;
    }

    /** A hut of 5 by 5 blocks with a door towards the way in, a window on each side and a lantern under the roof. */
    private void hut(Canvas canvas, int[] site, Theme inside) {
        int f0 = site[0];
        int r0 = site[1];
        for (int f = f0; f < f0 + HUT; f++) {
            for (int r = r0; r < r0 + HUT; r++) {
                boolean endF = f == f0 || f == f0 + HUT - 1;
                boolean endR = r == r0 || r == r0 + HUT - 1;
                canvas.put(f, ROOF, r, this.wood.slab());
                for (int u = 1; u < ROOF && (endF || endR); u++) {
                    boolean door = f == f0 && r == r0 + 2 && u < 3;
                    if (endF && endR) {
                        canvas.put(f, u, r, this.wood.log(Direction.Axis.Y));
                    } else if (endR && f == f0 + 2 && u == 2) {
                        canvas.fence(f, u, r);
                    } else if (!door) {
                        canvas.put(f, u, r, this.wood.planks());
                    }
                }
            }
        }
        canvas.hangingLantern(f0 + 2, ROOF - 1, r0 + 2);

        // Along the back wall, then in the corners beside the door.
        Iterator<int[]> free = List.of(
                new int[]{f0 + 3, r0 + 1}, new int[]{f0 + 3, r0 + 3}, new int[]{f0 + 3, r0 + 2},
                new int[]{f0 + 1, r0 + 1}, new int[]{f0 + 1, r0 + 3}
        ).iterator();
        Direction front = this.facing.getOpposite();
        switch (inside) {
            case SPAWNER, TREASURE_HUT -> {
                boolean treasure = inside == Theme.TREASURE_HUT;
                canvas.spawner(f0 + 2, 1, r0 + 2, mob());
                ResourceKey<LootTable> loot = treasure ? RRLootTables.HANGING_TRACKS_TREASURE : RRLootTables.HANGING_TRACKS_SUPPLY;
                chest(canvas, free, loot);
                if (treasure) {
                    chest(canvas, free, loot);
                }
                cobwebs(canvas, free, 2);
            }
            case WORKSHOP -> workshop(canvas, free, front);
            case CAMP -> {
                chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
                place(canvas, free, Blocks.CRAFTING_TABLE.defaultBlockState());
                place(canvas, free, facing(Blocks.FURNACE, front));
            }
            default -> {
                chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
                barrels(canvas, free, 4);
            }
        }
    }

    /** A roof of slabs on four fence posts, 3 by 5 blocks, over a row of stores or of workshop tools. */
    private void awning(Canvas canvas, int[] site, Direction front) {
        int f0 = site[0];
        int r0 = site[1];
        List<int[]> under = new ArrayList<>();
        for (int f = f0; f < f0 + 3; f++) {
            for (int r = r0; r < r0 + HUT; r++) {
                canvas.put(f, 3, r, this.wood.slab());
                if (f != f0 + 1 && (r == r0 || r == r0 + HUT - 1)) {
                    canvas.fence(f, 1, r);
                    canvas.fence(f, 2, r);
                } else if (f > f0) {
                    under.add(new int[]{f, r});
                }
            }
        }
        Iterator<int[]> free = under.iterator();
        if (this.theme == Theme.WORKSHOP) {
            workshop(canvas, free, front);
        } else {
            chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
            chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
            barrels(canvas, free, 5);
        }
    }

    private void workshop(Canvas canvas, Iterator<int[]> free, Direction front) {
        place(canvas, free, facing(Blocks.DAMAGED_ANVIL, front));
        place(canvas, free, facing(Blocks.BLAST_FURNACE, front));
        place(canvas, free, Blocks.GRINDSTONE.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
        chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
    }

    private EntityType<?> mob() {
        float roll = chance(0, 0, 23);
        return roll < 0.4f ? EntityTypes.CAVE_SPIDER : roll < 0.7f ? EntityTypes.ZOMBIE : EntityTypes.SKELETON;
    }

    private static BlockState facing(Block block, Direction front) {
        return block.defaultBlockState().trySetValue(BlockStateProperties.HORIZONTAL_FACING, front);
    }

    private static void place(Canvas canvas, Iterator<int[]> free, BlockState state) {
        if (free.hasNext()) {
            int[] spot = free.next();
            canvas.put(spot[0], 1, spot[1], state);
        }
    }

    private void chest(Canvas canvas, Iterator<int[]> free, ResourceKey<LootTable> loot) {
        if (free.hasNext()) {
            int[] spot = free.next();
            canvas.container(spot[0], 1, spot[1], facing(Blocks.CHEST, this.facing.getOpposite()), loot);
        }
    }

    /** Barrels of supplies, some of them two high. */
    private void barrels(Canvas canvas, Iterator<int[]> free, int count) {
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
        for (int i = 0; i < count && free.hasNext(); i++) {
            int[] spot = free.next();
            canvas.container(spot[0], 1, spot[1], barrel, RRLootTables.HANGING_TRACKS_SUPPLY);
            if (chance(spot[0], spot[1], 22) < 0.35f) {
                canvas.container(spot[0], 2, spot[1], barrel, RRLootTables.HANGING_TRACKS_SUPPLY);
            }
        }
    }

    private static void cobwebs(Canvas canvas, Iterator<int[]> free, int count) {
        for (int i = 0; i < count && free.hasNext(); i++) {
            int[] spot = free.next();
            canvas.put(spot[0], 1, spot[1], Blocks.COBWEB.defaultBlockState());
        }
    }

    /** A chest minecart left on the rails, as in a vanilla mineshaft. A preview has no server and gets none. */
    private void chestMinecart(Canvas canvas) {
        BlockPos pos = canvas.pos(rails() - 1, 1, 0);
        if (pos == null || canvas.level().getServer() == null) {
            return;
        }
        MinecartChest minecart = EntityTypes.CHEST_MINECART.create(canvas.level().getLevel(), EntitySpawnReason.CHUNK_GENERATION);
        if (minecart != null) {
            minecart.setInitialPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            minecart.setLootTable(RRLootTables.HANGING_TRACKS_SUPPLY, this.seed);
            canvas.level().addFreshEntity(minecart);
        }
    }
}
