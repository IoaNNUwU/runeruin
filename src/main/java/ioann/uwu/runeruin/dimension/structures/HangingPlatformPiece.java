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
 * A small dungeon that hangs by its corners: at the end of a track, with the rails running into it,
 * or on its own near a track.
 */
public final class HangingPlatformPiece extends HangingPiece {

    public enum Shape { SQUARE, L, T }

    public enum Shell { OPEN, RAILING, ROOM }

    public enum Content { STORAGE, SPAWNER, DEPOT, CAMP, WORKSHOP, LOOKOUT, COLLAPSED, TREASURE }

    /** Walls are three blocks high, the roof lies on them and the chains start above it. */
    public static final int ROOM_HEIGHT = 5;

    private final Shape shape;
    /** The side of a square; the L and the T are always 9 blocks long, with arms 5 blocks wide. */
    private final int size;
    /** The side the leg of the L points to: 1 right, -1 left. */
    private final int mirror;
    private final Shell shell;
    private final Content content;
    /** Whether a track ends here. */
    private final boolean connected;

    public HangingPlatformPiece(Port port, Shape shape, int size, int mirror, Shell shell, Content content, boolean connected, Look look) {
        super(RRStructurePieceTypes.HANGING_PLATFORM.get(), port, look, bounds(port, shape, size, mirror, BELOW_DECK));
        this.shape = shape;
        this.size = size;
        this.mirror = mirror;
        this.shell = shell;
        this.content = content;
        this.connected = connected;
    }

    public HangingPlatformPiece(CompoundTag tag) {
        super(RRStructurePieceTypes.HANGING_PLATFORM.get(), tag);
        this.shape = Shape.values()[tag.getIntOr("Shape", 0)];
        this.size = tag.getIntOr("Size", 5);
        this.mirror = tag.getIntOr("Mirror", 1);
        this.shell = Shell.values()[tag.getIntOr("Shell", 0)];
        this.content = Content.values()[tag.getIntOr("Content", 0)];
        this.connected = tag.getBooleanOr("Connected", false);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putInt("Shape", this.shape.ordinal());
        tag.putInt("Size", this.size);
        tag.putInt("Mirror", this.mirror);
        tag.putInt("Shell", this.shell.ordinal());
        tag.putInt("Content", this.content.ordinal());
        tag.putBoolean("Connected", this.connected);
    }

    /** The local rectangle around the shape: the largest f, the smallest and the largest r. */
    private static int[] extents(Shape shape, int size, int mirror) {
        return switch (shape) {
            case SQUARE -> new int[]{size - 1, -size / 2, size / 2};
            case L -> new int[]{8, mirror > 0 ? -2 : -6, mirror > 0 ? 6 : 2};
            case T -> new int[]{8, -6, 6};
        };
    }

    private static BoundingBox bounds(Port port, Shape shape, int size, int mirror, int below) {
        int[] extents = extents(shape, size, mirror);
        return box(port, 0, extents[0], extents[1], extents[2], below);
    }

    @Override
    public BoundingBox footprint() {
        return bounds(this.origin, this.shape, this.size, this.mirror, 0);
    }

    @Override
    public int headroom() {
        return this.shell == Shell.ROOM ? ROOM_HEIGHT + 1 : super.headroom();
    }

    private boolean contains(int f, int r) {
        return switch (this.shape) {
            case SQUARE -> f >= 0 && f < this.size && Math.abs(r) <= this.size / 2;
            case L -> f >= 0 && f <= 8 && (Math.abs(r) <= 2 || f >= 4 && r * this.mirror >= 3 && r * this.mirror <= 6);
            case T -> f >= 0 && f <= 8 && (Math.abs(r) <= 2 || f >= 4 && Math.abs(r) <= 6);
        };
    }

    @Override
    protected void paint(Canvas canvas) {
        int[] extents = extents(this.shape, this.size, this.mirror);
        // The rails run a few blocks in and end with a stop.
        int rails = !this.connected ? 0 : this.size == 5 && this.shape == Shape.SQUARE ? 2 : 3;
        List<int[]> spots = new ArrayList<>();

        for (int f = 0; f <= extents[0]; f++) {
            for (int r = extents[1]; r <= extents[2]; r++) {
                if (!contains(f, r)) {
                    continue;
                }
                boolean endF = !contains(f - 1, r) || !contains(f + 1, r);
                boolean endR = !contains(f, r - 1) || !contains(f, r + 1);
                boolean border = endF || endR;
                boolean corner = endF && endR;
                boolean lane = r == 0 && f <= rails;
                if (this.content == Content.COLLAPSED && !border && !lane && chance(f, r, 20) < 0.4f) {
                    continue;
                }
                canvas.deck(f, 0, r, border && !corner && !lane && this.shell != Shell.ROOM);
                if (border) {
                    wall(canvas, f, r, corner, f == 0 && r == 0);
                } else if (!lane) {
                    spots.add(new int[]{f, r});
                }
                if (this.shell == Shell.ROOM) {
                    canvas.put(f, ROOM_HEIGHT - 1, r, this.wood.slab());
                }
                if (corner) {
                    canvas.chain(f, this.shell == Shell.ROOM ? ROOM_HEIGHT : 1, r);
                }
            }
        }

        for (int f = 0; f < rails; f++) {
            canvas.rail(f, 1, 0, flatRail(this.facing, this.facing.getOpposite()));
        }
        if (this.connected) {
            canvas.buffer(rails, 1, 0, side(1).getAxis());
        }
        if (this.shell == Shell.ROOM) {
            canvas.hangingLantern(this.shape == Shape.SQUARE ? this.size / 2 : 6, ROOM_HEIGHT - 2, 0);
        }
        furnish(canvas, spots);
    }

    private void wall(Canvas canvas, int f, int r, boolean corner, boolean door) {
        if (this.shell == Shell.RAILING) {
            if (!door && !corner && hasDeck(f, r, true)) {
                canvas.fence(f, 1, r);
            }
        } else if (this.shell == Shell.ROOM) {
            for (int u = 1; u < ROOM_HEIGHT - 1; u++) {
                if (corner) {
                    canvas.put(f, u, r, this.wood.log(Direction.Axis.Y));
                } else if (door && u < 3) {
                    continue;
                } else if (u == 2 && chance(f, r, 21) < 0.3f) {
                    canvas.fence(f, u, r);
                } else {
                    canvas.put(f, u, r, this.wood.planks());
                }
            }
        }
    }

    /** Fills the free blocks inside, the same in every chunk: the order comes from the seed of the piece. */
    private void furnish(Canvas canvas, List<int[]> spots) {
        RandomSource random = RandomSource.create(this.seed);
        for (int i = spots.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] swapped = spots.get(i);
            spots.set(i, spots.get(j));
            spots.set(j, swapped);
        }
        Iterator<int[]> free = spots.iterator();
        Direction front = this.facing.getOpposite();

        switch (this.content) {
            case STORAGE -> {
                chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
                if (spots.size() > 8) {
                    chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
                }
                for (int i = 0; i < 2 + spots.size() / 6 && free.hasNext(); i++) {
                    int[] spot = free.next();
                    barrel(canvas, spot, 1);
                    if (chance(spot[0], spot[1], 22) < 0.4f) {
                        barrel(canvas, spot, 2);
                    }
                }
            }
            case SPAWNER, TREASURE -> {
                boolean treasure = this.content == Content.TREASURE;
                if (free.hasNext()) {
                    int[] spot = free.next();
                    canvas.spawner(spot[0], 1, spot[1], mob());
                }
                ResourceKey<LootTable> loot = treasure ? RRLootTables.HANGING_TRACKS_TREASURE : RRLootTables.HANGING_TRACKS_SUPPLY;
                chest(canvas, free, loot);
                if (treasure) {
                    chest(canvas, free, loot);
                }
                for (int i = 0; i < 2 + spots.size() / 4 && free.hasNext(); i++) {
                    int[] spot = free.next();
                    canvas.put(spot[0], 1 + (int) (chance(spot[0], spot[1], 22) * 2), spot[1], Blocks.COBWEB.defaultBlockState());
                }
            }
            case DEPOT -> {
                chestMinecart(canvas);
                if (free.hasNext()) {
                    barrel(canvas, free.next(), 1);
                }
            }
            case CAMP -> {
                place(canvas, free, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
                place(canvas, free, Blocks.CRAFTING_TABLE.defaultBlockState());
                place(canvas, free, facing(Blocks.FURNACE, front));
                if (free.hasNext()) {
                    barrel(canvas, free.next(), 1);
                }
            }
            case WORKSHOP -> {
                place(canvas, free, facing(Blocks.DAMAGED_ANVIL, front));
                place(canvas, free, Blocks.GRINDSTONE.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
                place(canvas, free, facing(Blocks.BLAST_FURNACE, front));
                chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
            }
            case LOOKOUT -> {
                place(canvas, free, Blocks.LANTERN.defaultBlockState());
                place(canvas, free, Blocks.LANTERN.defaultBlockState());
            }
            case COLLAPSED -> chest(canvas, free, RRLootTables.HANGING_TRACKS_SUPPLY);
        }
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

    private static void barrel(Canvas canvas, int[] spot, int u) {
        canvas.container(spot[0], u, spot[1], Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP),
                RRLootTables.HANGING_TRACKS_SUPPLY);
    }

    /** A chest minecart left on the rails, as in a vanilla mineshaft. A preview has no server and gets none. */
    private void chestMinecart(Canvas canvas) {
        BlockPos pos = canvas.pos(1, 1, 0);
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
