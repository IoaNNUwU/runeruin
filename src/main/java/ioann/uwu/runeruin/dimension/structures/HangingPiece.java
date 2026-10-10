package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.blocks.DeepMossLayerBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WispberryBushBlock;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.structures.HangingTracksLayout.Port;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * What the pieces of the hanging minecart tracks share: a local frame, the wood, the wear and the chains
 * up to the ceiling.
 *
 * <p>Local coordinates: {@code f} forward along {@link #facing}, {@code r} to the right of it, {@code u} up.
 * The origin is the deck block under the first rail of the piece.
 */
public abstract class HangingPiece extends StructurePiece {

    /** How far below its deck a piece may write: beams, lanterns and vines. */
    protected static final int BELOW_DECK = 4;

    public enum Wood {
        OAK, INVERTED_TREE;

        BlockState planks() {
            return (this == OAK ? Blocks.OAK_PLANKS : RRBlocks.INVERTED_TREE_PLANKS.get()).defaultBlockState();
        }

        BlockState log(Direction.Axis axis) {
            return (this == OAK ? Blocks.OAK_LOG : RRBlocks.INVERTED_TREE_WOOD.get()).defaultBlockState()
                    .trySetValue(RotatedPillarBlock.AXIS, axis);
        }

        BlockState fence() {
            return (this == OAK ? Blocks.OAK_FENCE : RRBlocks.INVERTED_TREE_FENCE.get()).defaultBlockState();
        }

        BlockState slab() {
            return (this == OAK ? Blocks.OAK_SLAB : RRBlocks.INVERTED_TREE_SLAB.get()).defaultBlockState();
        }
    }

    public enum Wear { INTACT, DECAYED, OVERGROWN }

    /** How a piece looks; the seed is for every random detail, so each chunk draws the same piece. */
    public record Look(Wood wood, Wear wear, long seed) {}

    protected final Port origin;
    protected final Direction facing;
    protected final Wood wood;
    protected final Wear wear;
    protected final long seed;

    protected HangingPiece(StructurePieceType type, Port port, Look look, BoundingBox box) {
        super(type, 0, box);
        this.origin = port;
        this.facing = port.facing();
        this.wood = look.wood();
        this.wear = look.wear();
        this.seed = look.seed();
    }

    protected HangingPiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.facing = Direction.from2DDataValue(tag.getIntOr("Facing", 0));
        this.origin = new Port(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0), this.facing);
        this.wood = Wood.values()[tag.getIntOr("Wood", 0)];
        this.wear = Wear.values()[tag.getIntOr("Wear", 0)];
        this.seed = tag.getLongOr("Seed", 0);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("OX", this.origin.x());
        tag.putInt("OY", this.origin.y());
        tag.putInt("OZ", this.origin.z());
        tag.putInt("Facing", this.facing.get2DDataValue());
        tag.putInt("Wood", this.wood.ordinal());
        tag.putInt("Wear", this.wear.ordinal());
        tag.putLong("Seed", this.seed);
    }

    /** The deck seen from above, in world coordinates: what the layout keeps other pieces away from. */
    public abstract BoundingBox footprint();

    /** The lowest and the highest deck level of the piece, relative to the origin. */
    public int[] deckLevels() {
        return new int[]{0, 0};
    }

    /** Air the piece needs between its highest deck and the ceiling. */
    public int headroom() {
        return HangingTracksLayout.MIN_GAP;
    }

    protected abstract void paint(Canvas canvas);

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos
    ) {
        paint(new Canvas(level, chunkBB));
    }

    /** The world box of a local rectangle, from {@code below} blocks under the origin up to the ceiling. */
    protected static BoundingBox box(Port port, int f1, int f2, int r1, int r2, int below) {
        int x1 = port.x(f1, r1);
        int x2 = port.x(f2, r2);
        int z1 = port.z(f1, r1);
        int z2 = port.z(f2, r2);
        return new BoundingBox(
                Math.min(x1, x2), port.y() - below, Math.min(z1, z2),
                Math.max(x1, x2), Const.DEEP_CAVES_CEILING_Y, Math.max(z1, z2)
        );
    }

    /** The world direction of the right (+1) or the left (-1) side. */
    protected Direction side(int sign) {
        return sign > 0 ? this.facing.getClockWise() : this.facing.getCounterClockWise();
    }

    /** A number in [0, 1) that depends only on the piece, the place and the salt. */
    protected float chance(int f, int r, int salt) {
        long h = this.seed + f * 341873128712L + r * 132897987541L + salt * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return ((h ^ (h >>> 33)) >>> 40) / (float) (1 << 24);
    }

    /**
     * Whether the deck is moss here. The deck of an overgrown piece is moss from end to end, with the rails
     * running through it; over the two rows at each end moss and wood mix, so the moss does not start along a line.
     */
    protected boolean mossy(int f, int r, int length) {
        return this.wear == Wear.OVERGROWN && (f >= 2 && f < length - 2 || chance(f, r, 36) < 0.5f);
    }

    protected static RailShape flatRail(Direction a, Direction b) {
        if (a.getAxis() == b.getAxis()) {
            return a.getAxis() == Direction.Axis.Z ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
        }
        boolean south = a == Direction.SOUTH || b == Direction.SOUTH;
        boolean east = a == Direction.EAST || b == Direction.EAST;
        if (south) {
            return east ? RailShape.SOUTH_EAST : RailShape.SOUTH_WEST;
        }
        return east ? RailShape.NORTH_EAST : RailShape.NORTH_WEST;
    }

    protected static RailShape slopeRail(Direction up) {
        return switch (up) {
            case NORTH -> RailShape.ASCENDING_NORTH;
            case SOUTH -> RailShape.ASCENDING_SOUTH;
            case WEST -> RailShape.ASCENDING_WEST;
            default -> RailShape.ASCENDING_EAST;
        };
    }

    /** Draws the part of the piece that is inside one chunk. Made anew for every chunk: chunks generate in parallel. */
    protected final class Canvas {
        private final WorldGenLevel level;
        private final BoundingBox bounds;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        private Canvas(WorldGenLevel level, BoundingBox bounds) {
            this.level = level;
            this.bounds = bounds;
        }

        WorldGenLevel level() {
            return this.level;
        }

        private boolean at(int f, int u, int r) {
            this.cursor.set(origin.x(f, r), origin.y() + u, origin.z(f, r));
            return this.bounds.isInside(this.cursor);
        }

        /** The block position, or null when it is in another chunk. */
        BlockPos pos(int f, int u, int r) {
            return at(f, u, r) ? this.cursor : null;
        }

        void put(int f, int u, int r, BlockState state) {
            if (at(f, u, r)) {
                this.level.setBlock(this.cursor, state, Block.UPDATE_CLIENTS);
            }
        }

        /**
         * A deck block: a plank or moss. The deck is never broken; its age shows in what grows on moss
         * and hangs under it, and in the cobwebs of a decayed piece.
         */
        void deck(int f, int u, int r, boolean mossy) {
            for (int i = 1; i <= HangingTracksLayout.MIN_GAP; i++) {
                clear(f, u + i, r);
            }
            block(f, u, r, mossy);
            if (mossy) {
                if (chance(f, r, 3) < 0.2f) {
                    put(f, u - 1, r, Blocks.HANGING_ROOTS.defaultBlockState());
                }
            } else if (wear == Wear.DECAYED) {
                if (chance(f, r, 4) < 0.08f) {
                    put(f, u - 1, r, Blocks.COBWEB.defaultBlockState());
                }
                if (chance(f, r, 13) < 0.03f) {
                    put(f, u + 1, r, Blocks.COBWEB.defaultBlockState());
                }
            }
        }

        /**
         * Cuts the way through whatever stands here: the spikes of the cave grow before the tracks are built,
         * and a deck keeps three blocks of air over it.
         */
        void clear(int f, int u, int r) {
            if (at(f, u, r) && !this.level.getBlockState(this.cursor).isAir()) {
                this.level.setBlock(this.cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        /** A plank, or moss with a layer of moss, a wispberry bush or an azalea on some of it. */
        void block(int f, int u, int r, boolean mossy) {
            if (!mossy) {
                put(f, u, r, wood.planks());
                return;
            }
            put(f, u, r, RRBlocks.DEEP_MOSS.get().defaultBlockState());
            float roll = chance(f, r, 2);
            if (roll < 0.08f) {
                put(f, u + 1, r, RRBlocks.DEEP_MOSS_LAYER.get().defaultBlockState()
                        .setValue(DeepMossLayerBlock.LAYERS, 1 + (int) (chance(f, r, 12) * 2)));
            } else if (roll < 0.15f) {
                put(f, u + 1, r, RRBlocks.WISPBERRY_BUSH.get().defaultBlockState()
                        .setValue(WispberryBushBlock.AGE, 1 + (int) (chance(f, r, 12) * 3)));
            } else if (roll < 0.22f) {
                put(f, u + 1, r, (chance(f, r, 12) < 0.35f ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA).defaultBlockState());
            }
        }

        void rail(int f, int u, int r, RailShape shape) {
            put(f, u, r, RRBlocks.RUNIC_RAIL.get().defaultBlockState().setValue(RailBlock.SHAPE, shape));
        }

        /** A fence joins its neighbours once the chunk is finished, as the fences of vanilla structures do. */
        void fence(int f, int u, int r) {
            if (at(f, u, r)) {
                this.level.setBlock(this.cursor, wood.fence(), Block.UPDATE_CLIENTS);
                if (this.level instanceof WorldGenRegion) {
                    this.level.getChunk(this.cursor).markPosForPostProcessing(this.cursor);
                }
            }
        }

        /** A stop at the end of the rails. */
        void buffer(int f, int u, int r, Direction.Axis across) {
            put(f, u, r, wood.log(across));
            put(f, u + 1, r, wood.log(across));
        }

        /** A chain from this block up to the ceiling; a torn one is only a stub under the ceiling. */
        void chain(int f, int u, int r) {
            if (!at(f, u, r)) {
                return;
            }
            int bottom = origin.y() + u;
            int top = bottom;
            while (top <= Const.DEEP_CAVES_CEILING_Y && this.level.getBlockState(this.cursor.setY(top)).isAir()) {
                top++;
            }
            if (wear == Wear.DECAYED && chance(f, r, 5) < 0.25f) {
                bottom = Math.max(bottom, top - 1 - (int) (chance(f, r, 6) * 3));
            }
            BlockState chain = Blocks.IRON_CHAIN.defaultBlockState();
            for (int y = bottom; y < top; y++) {
                this.level.setBlock(this.cursor.setY(y), chain, Block.UPDATE_CLIENTS);
            }
        }

        void hangingLantern(int f, int u, int r) {
            put(f, u, r, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }

        /** Vines down the side of the deck block next to this place: often on a mossy deck, sometimes on a decayed one. */
        void vines(int f, int u, int r, Direction towardsDeck, boolean mossy) {
            if (chance(f, r, 11) >= (mossy ? 0.25f : wear == Wear.DECAYED ? 0.1f : 0f)) {
                return;
            }
            BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(towardsDeck), true);
            int length = 1 + (int) (chance(f, r, 7) * 3);
            for (int i = 0; i < length; i++) {
                put(f, u - i, r, vine);
            }
        }

        void spawner(int f, int u, int r, EntityType<?> mob) {
            if (at(f, u, r)) {
                this.level.setBlock(this.cursor, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_CLIENTS);
                if (this.level.getBlockEntity(this.cursor) instanceof SpawnerBlockEntity spawner) {
                    spawner.setEntityId(mob, RandomSource.create(seed));
                }
            }
        }

        void container(int f, int u, int r, BlockState state, ResourceKey<LootTable> loot) {
            if (at(f, u, r)) {
                this.level.setBlock(this.cursor, state, Block.UPDATE_CLIENTS);
                if (this.level.getBlockEntity(this.cursor) instanceof RandomizableContainerBlockEntity container) {
                    container.setLootTable(loot);
                    container.setLootTableSeed(seed + this.cursor.asLong());
                }
            }
        }
    }
}
