package ioann.uwu.runeruin.dimension.structures;

import ioann.uwu.runeruin.blocks.DeepMossLayerBlock;
import ioann.uwu.runeruin.blocks.RRBlocks;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of a wrecked pirate ship: a three-masted hull with a hold, a gun deck, a forecastle and a stern
 * castle with the captain's cabin. It is built upright in ship space, where x runs from the stern to the bow,
 * z from port to starboard and y up from the bottom of the keel, then turned to its heading and laid on its side.
 */
public final class PirateShipwreckGenerator {

    private static final int LENGTH = 46;
    private static final int GUN_DECK = 6;
    private static final int MAIN_DECK = 10;
    private static final int FORECASTLE_DECK = 13;
    private static final int QUARTERDECK = 14;
    private static final int POOP_DECK = 18;
    // The last x of the poop and of the quarterdeck, the first of the forecastle.
    private static final int POOP_END = 6;
    private static final int QUARTERDECK_END = 14;
    private static final int FORECASTLE_START = 34;
    /** The main mast; the origin of the ship is the bottom of the keel under it. */
    private static final int MIDSHIP = 22;
    private static final int[] GUNPORTS = {12, 16, 20, 24, 28, 32};

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState HULL = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState DARK_HULL = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState DECK = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState RAIL = Blocks.DARK_OAK_FENCE.defaultBlockState();
    private static final BlockState SAIL = Blocks.WOOL.white().defaultBlockState();
    private static final BlockState DIRTY_SAIL = Blocks.WOOL.lightGray().defaultBlockState();

    /** Blocks by position relative to the bottom of the keel under the main mast, and the level part of the keel among them. */
    public record Ship(Map<BlockPos, BlockState> blocks, List<BlockPos> keel) {}

    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final long seed;
    private final RandomSource random;

    private PirateShipwreckGenerator(long seed) {
        this.seed = seed;
        this.random = RandomSource.create(seed);
    }

    /** The same seed gives the same ship. */
    public static Ship generate(long seed) {
        PirateShipwreckGenerator ship = new PirateShipwreckGenerator(seed);
        ship.hull();
        ship.rails();
        ship.stairs();
        ship.openings();
        ship.fittings();
        ship.masts();
        ship.decay();
        return ship.laidDown();
    }

    // ---- hull

    private static int deckY(int x) {
        if (x <= POOP_END) {
            return POOP_DECK;
        }
        return x <= QUARTERDECK_END ? QUARTERDECK : x >= FORECASTLE_START ? FORECASTLE_DECK : MAIN_DECK;
    }

    /** The bottom of the keel: it rises to the stem at the bow and a little under the stern. */
    private static int keelY(int x) {
        if (x < 6) {
            return Mth.floor(3.0 * Math.pow((6 - x) / 6.0, 1.5));
        }
        return x > 33 ? Mth.floor(9.0 * Mth.square((x - 33) / 12.0)) : 0;
    }

    /** Half the width of the main deck: a flat stern, a wide middle and a bluff bow. */
    private static double halfBreadth(int x) {
        if (x < 12) {
            return 5.0 + 2.0 * Mth.smoothstep(x / 12.0);
        }
        return x <= 26 ? 7.0 : 7.0 * Math.pow(1.0 - Mth.square((x - 26) / 19.5), 0.6);
    }

    private static int halfWidth(int x, int y) {
        double breadth = halfBreadth(x);
        if (y > QUARTERDECK) {
            // The poop is a block narrower than the quarterdeck under it.
            return (int) Math.round(breadth - 1.0);
        }
        if (y >= MAIN_DECK) {
            return (int) Math.round(breadth);
        }
        // Under the main deck the sides round down to the keel: a box in the middle, a wedge at the ends.
        int keel = keelY(x);
        double fullness = 0.4 + 0.5 * Mth.smoothstep(Mth.clamp((x - 26) / 19.0, 0.0, 1.0))
                + 0.25 * Mth.smoothstep(Mth.clamp((12 - x) / 12.0, 0.0, 1.0));
        return (int) Math.round(breadth * Math.pow((y - keel) / (double) (MAIN_DECK - keel), fullness));
    }

    private static boolean inHull(int x, int y, int z) {
        return x >= 0 && x < LENGTH && y >= keelY(x) && y <= deckY(x) && Math.abs(z) <= halfWidth(x, y);
    }

    private static boolean inside(int x, int y, int z) {
        return inHull(x, y, z) && inHull(x - 1, y, z) && inHull(x + 1, y, z) && inHull(x, y - 1, z)
                && inHull(x, y + 1, z) && inHull(x, y, z - 1) && inHull(x, y, z + 1);
    }

    private void hull() {
        for (int x = 0; x < LENGTH; x++) {
            for (int y = keelY(x); y <= deckY(x); y++) {
                for (int z = -halfWidth(x, y); z <= halfWidth(x, y); z++) {
                    put(x, y, z, inside(x, y, z) ? room(x, y, z) : shell(x, y, z));
                }
            }
        }
    }

    private static BlockState room(int x, int y, int z) {
        if (y == GUN_DECK || y == MAIN_DECK || y == QUARTERDECK) {
            return DECK;
        }
        // The keelson on top of the keel.
        return z == 0 && y == keelY(x) + 1 ? DARK_HULL : AIR;
    }

    private static BlockState shell(int x, int y, int z) {
        if (!inHull(x, y + 1, z)) {
            return DECK;
        }
        if (z == 0) {
            // The keel, and the stem and the sternpost it turns into.
            return log(Blocks.DARK_OAK_LOG, inHull(x, y - 1, z) ? Direction.Axis.Y : Direction.Axis.X);
        }
        return y < 4 || y == MAIN_DECK - 1 ? DARK_HULL : HULL;
    }

    /** A bulwark around the main deck and a fence around the decks above it. */
    private void rails() {
        for (int x = 0; x < LENGTH; x++) {
            for (int z = -halfWidth(x, MAIN_DECK); z <= halfWidth(x, MAIN_DECK); z++) {
                int top = deckY(x);
                while (!inHull(x, top, z)) {
                    top--;
                }
                if (!inHull(x - 1, top, z) || !inHull(x + 1, top, z) || !inHull(x, top, z - 1) || !inHull(x, top, z + 1)) {
                    put(x, top + 1, z, top == MAIN_DECK ? HULL : RAIL);
                }
            }
        }
    }

    // ---- ways through the ship

    private void stairs() {
        for (int side = -1; side <= 1; side += 2) {
            for (int z = 3; z <= 6; z++) {
                // To the quarterdeck along the sides, to the forecastle nearer to the middle.
                if (z >= 5) {
                    flight(18, MAIN_DECK + 1, side * z, -1, QUARTERDECK - MAIN_DECK, true);
                    blocks.remove(new BlockPos(QUARTERDECK_END, QUARTERDECK + 1, side * z));
                } else {
                    flight(31, MAIN_DECK + 1, side * z, 1, FORECASTLE_DECK - MAIN_DECK, true);
                    blocks.remove(new BlockPos(FORECASTLE_START, FORECASTLE_DECK + 1, side * z));
                }
            }
        }
        // The main hatch down to the gun deck, and the stairs from there into the hold.
        flight(29, GUN_DECK + 1, 0, -1, MAIN_DECK - GUN_DECK, false);
        flight(14, 2, 0, 1, GUN_DECK - 1, false);

        for (int y = QUARTERDECK + 1; y <= POOP_DECK; y++) {
            put(POOP_END + 1, y, 3, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        }
        blocks.remove(new BlockPos(POOP_END, POOP_DECK + 1, 3));
    }

    /**
     * Stairs that climb {@code steps} blocks from this block towards +x or -x, standing on planks or,
     * between decks, hanging in a hatch cut for them.
     */
    private void flight(int x, int y, int z, int towards, int steps, boolean onPlanks) {
        Direction facing = towards > 0 ? Direction.EAST : Direction.WEST;
        for (int i = 0; i < steps; i++) {
            put(x + towards * i, y + i, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
            for (int below = y; below < y + i; below++) {
                if (onPlanks) {
                    put(x + towards * i, below, z, HULL);
                }
            }
            // Headroom in the deck the flight comes up through.
            if (!onPlanks && i < steps - 1) {
                put(x + towards * i, y + steps - 1, z, AIR);
            }
        }
    }

    private void openings() {
        // Doorways of the captain's cabin, the forecastle and the chart room; the doors are long gone.
        for (int y = 1; y <= 2; y++) {
            put(QUARTERDECK_END, MAIN_DECK + y, 0, AIR);
            put(FORECASTLE_START, MAIN_DECK + y, 0, AIR);
            put(POOP_END, QUARTERDECK + y, 0, AIR);
        }

        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        for (int z = 1; z <= 3; z++) {
            for (int side = -1; side <= 1; side += 2) {
                put(0, MAIN_DECK + 2, side * z, pane);
                if (z < 3) {
                    put(0, QUARTERDECK + 2, side * z, pane);
                }
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            put(4, MAIN_DECK + 2, side * halfWidth(4, MAIN_DECK), pane);
            put(10, MAIN_DECK + 2, side * halfWidth(10, MAIN_DECK), pane);
            put(3, QUARTERDECK + 2, side * halfWidth(3, POOP_DECK), pane);

            for (int x : GUNPORTS) {
                Direction outboard = side < 0 ? Direction.NORTH : Direction.SOUTH;
                // A cannon looks out of most gunports; the rest stand open.
                put(x, GUN_DECK + 2, side * halfWidth(x, GUN_DECK + 2), random.nextFloat() < 0.6f
                        ? Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, outboard)
                        : AIR);
            }
        }
    }

    // ---- what stands on the decks

    private void fittings() {
        BlockState hangingLantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);

        // The captain's cabin under the quarterdeck: a table with a chair, charts, books and a carpet.
        int cabin = MAIN_DECK + 1;
        for (int x = 4; x <= 7; x++) {
            for (int z = -2; z <= 2; z++) {
                put(x, cabin, z, Blocks.CARPET.red().defaultBlockState());
            }
        }
        for (int z = -1; z <= 1; z++) {
            put(5, cabin, z, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        }
        put(7, cabin, 0, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        put(2, cabin, -3, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        put(1, cabin, 3, Blocks.BOOKSHELF.defaultBlockState());
        put(1, cabin + 1, 3, Blocks.BOOKSHELF.defaultBlockState());
        put(1, cabin, 4, Blocks.BOOKSHELF.defaultBlockState());
        put(12, cabin, -5, barrel);
        put(5, QUARTERDECK - 1, 0, hangingLantern);

        // The chart room under the poop.
        put(1, QUARTERDECK + 1, 2, barrel);
        put(1, QUARTERDECK + 1, -3, Blocks.BOOKSHELF.defaultBlockState());
        put(3, POOP_DECK - 1, 0, hangingLantern);

        // The helm on the quarterdeck.
        put(12, QUARTERDECK + 1, 0, RAIL);
        put(12, QUARTERDECK + 2, 0, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState()
                .setValue(TrapDoorBlock.FACING, Direction.EAST).setValue(TrapDoorBlock.OPEN, true));

        // Soul lanterns on the stern rail.
        for (int side = -1; side <= 1; side += 2) {
            put(0, POOP_DECK + 2, side * halfWidth(0, POOP_DECK), Blocks.SOUL_LANTERN.defaultBlockState());
        }

        put(20, MAIN_DECK - 1, 0, hangingLantern);
        put(32, MAIN_DECK - 1, 2, hangingLantern);
        put(10, GUN_DECK - 1, 0, hangingLantern);
        put(26, GUN_DECK - 1, 0, hangingLantern);
        put(38, FORECASTLE_DECK - 1, 2, hangingLantern);

        // Cargo: barrels in the hold, one or two high, and a few on the gun deck and in the forecastle.
        for (int i = 0; i < 26; i++) {
            int x = random.nextIntBetweenInclusive(5, 36);
            int z = random.nextIntBetweenInclusive(-4, 4);
            int y = i < 18 ? 2 : i < 23 ? GUN_DECK + 1 : MAIN_DECK + 1;
            if (i >= 23) {
                x = random.nextIntBetweenInclusive(FORECASTLE_START + 2, 41);
            }
            // Not in the way of the stairs along the middle.
            if (z == 0 || !isAir(x, y, z) || isAir(x, y - 1, z)) {
                continue;
            }
            put(x, y, z, barrel);
            if (random.nextFloat() < 0.35f && isAir(x, y + 1, z)) {
                put(x, y + 1, z, barrel);
            }
        }
    }

    // ---- masts and sails

    private void masts() {
        // One mast in three ships is broken off, and its top lies over the side.
        int broken = random.nextFloat() < 0.4f ? random.nextInt(3) : -1;

        Map<BlockPos, BlockState> main = new HashMap<>();
        mast(main, MIDSHIP, 36);
        squareSail(main, MIDSHIP + 1, 22, 9, 8);
        squareSail(main, MIDSHIP + 1, 33, 8, 6);
        for (int x = MIDSHIP - 1; x <= MIDSHIP + 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != MIDSHIP || z != 0) {
                    main.put(new BlockPos(x, 23, z), Blocks.SPRUCE_SLAB.defaultBlockState());
                    main.put(new BlockPos(x, 24, z), RAIL);
                }
            }
        }
        for (int x = MIDSHIP - 4; x < MIDSHIP; x++) {
            for (int y = 34; y <= 36; y++) {
                if (chance(x, y, 0, 1) > 0.15f * (MIDSHIP - x)) {
                    main.put(new BlockPos(x, y, 0), Blocks.WOOL.black().defaultBlockState());
                }
            }
        }
        rig(main, MIDSHIP, MAIN_DECK, broken == 0);

        Map<BlockPos, BlockState> fore = new HashMap<>();
        mast(fore, 37, 32);
        squareSail(fore, 38, 23, 7, 6);
        squareSail(fore, 38, 30, 5, 4);
        rig(fore, 37, FORECASTLE_DECK, broken == 1);

        // The mizzen carries a fore-and-aft sail between a boom and a slanting gaff.
        Map<BlockPos, BlockState> mizzen = new HashMap<>();
        mast(mizzen, 9, 31);
        for (int x = 1; x <= 8; x++) {
            int top = 29 - Math.round((8 - x) * 0.6f);
            mizzen.put(new BlockPos(x, 20, 0), log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            mizzen.put(new BlockPos(x, top + 1, 0), log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
            for (int y = 21; y <= top; y++) {
                if (!torn(x, y, 3, (y - 21) / 8f)) {
                    mizzen.put(new BlockPos(x, y, 0), chance(x, y, 0, 2) < 0.15f ? DIRTY_SAIL : SAIL);
                }
            }
        }
        rig(mizzen, 9, QUARTERDECK, broken == 2);

        // The bowsprit.
        for (int i = 0; i <= 10; i++) {
            put(LENGTH - 2 + i, FORECASTLE_DECK + 1 + i / 3, 0, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
        }
        for (int y = 2; y <= 8; y++) {
            put(-1, y, 0, DARK_HULL);
        }
    }

    private static void mast(Map<BlockPos, BlockState> rig, int x, int top) {
        for (int y = keelY(x) + 1; y <= top; y++) {
            rig.put(new BlockPos(x, y, 0), log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
        }
    }

    /** A yard across the ship with a sail of this height under it, bellied towards the bow and torn from below. */
    private void squareSail(Map<BlockPos, BlockState> rig, int x, int yardY, int height, int halfWidth) {
        for (int z = -halfWidth; z <= halfWidth; z++) {
            rig.put(new BlockPos(x, yardY, z), log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
            for (int down = 1; down <= height; down++) {
                float fromTop = down / (float) height;
                if (torn(z, yardY - down, x, 1f - fromTop)) {
                    continue;
                }
                boolean belly = Math.sin(Math.PI * fromTop) * Math.cos(Math.PI / 2 * z / halfWidth) > 0.5;
                rig.put(new BlockPos(belly ? x + 1 : x, yardY - down, z), chance(x, yardY - down, z, 2) < 0.15f ? DIRTY_SAIL : SAIL);
            }
        }
    }

    /** Whether the sail has a hole here: patches a few blocks wide, more of them far from the yard that holds it. */
    private boolean torn(int a, int b, int salt, float nearYard) {
        float fa = a / 3f;
        float fb = b / 3f;
        int ia = Mth.floor(fa);
        int ib = Mth.floor(fb);
        double patch = Mth.lerp2(fa - ia, fb - ib,
                chance(ia, ib, salt, 3), chance(ia + 1, ib, salt, 3), chance(ia, ib + 1, salt, 3), chance(ia + 1, ib + 1, salt, 3));
        return patch < 0.15f + 0.25f * (1f - Mth.clamp(nearYard, 0f, 1f));
    }

    /** Adds a mast with its sails to the ship, or the stump of a broken one with its top fallen over the side. */
    private void rig(Map<BlockPos, BlockState> rig, int x, int deck, boolean broken) {
        if (!broken) {
            blocks.putAll(rig);
            return;
        }
        int stump = deck + random.nextIntBetweenInclusive(4, 8);
        rig.forEach((pos, state) -> {
            if (pos.getY() <= stump && pos.getX() == x && pos.getZ() == 0) {
                blocks.put(pos, state);
            }
        });
        int side = random.nextBoolean() ? 1 : -1;
        int length = 16;
        int lean = random.nextIntBetweenInclusive(-4, 4);
        for (int i = 0; i <= length * 2; i++) {
            float t = i / (length * 2f);
            blocks.putIfAbsent(
                    new BlockPos(x + Math.round(lean * t), stump - Math.round(length * 0.8f * t), side * (1 + Math.round(length * t))),
                    log(Blocks.SPRUCE_LOG, Direction.Axis.Z));
        }
    }

    // ---- wear

    private void decay() {
        // A few holes knocked through the sides.
        int breaches = random.nextIntBetweenInclusive(2, 4);
        for (int i = 0; i < breaches; i++) {
            int x = random.nextIntBetweenInclusive(5, 40);
            int y = random.nextIntBetweenInclusive(3, 11);
            int z = (random.nextBoolean() ? 1 : -1) * halfWidth(x, Math.min(y, MAIN_DECK));
            double radius = 2.0 + random.nextDouble() * 1.5;
            int reach = Mth.ceil(radius);
            for (BlockPos pos : BlockPos.betweenClosed(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach)) {
                if (pos.distToCenterSqr(x + 0.5, y + 0.5, z + 0.5) <= radius * radius && isPlanking(blocks.get(pos))) {
                    rot(pos.immutable());
                }
            }
        }

        for (int x = -1; x < LENGTH; x++) {
            for (int y = 0; y <= POOP_DECK + 1; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = blocks.get(pos);
                    if (state == null) {
                        continue;
                    }
                    if (state.isAir()) {
                        // Cobwebs under the decks.
                        BlockState above = blocks.get(pos.above());
                        if (above != null && !above.isAir() && chance(x, y, z, 4) < 0.03f) {
                            blocks.put(pos, Blocks.COBWEB.defaultBlockState());
                        }
                    } else if (state == DECK) {
                        if (chance(x, y, z, 5) < 0.04f) {
                            rot(pos);
                        } else if (!blocks.containsKey(pos.above()) && chance(x, y, z, 6) < 0.08f) {
                            blocks.put(pos.above(), RRBlocks.DEEP_MOSS_LAYER.get().defaultBlockState()
                                    .setValue(DeepMossLayerBlock.LAYERS, 1 + (int) (chance(x, y, z, 7) * 2)));
                        }
                    } else if (state == HULL && chance(x, y, z, 5) < 0.02f) {
                        rot(pos);
                    } else if (state == DARK_HULL && y < 4 && chance(x, y, z, 8) < 0.15f) {
                        // Moss creeps up the bottom from the floor of the cave.
                        blocks.put(pos, RRBlocks.DEEP_MOSS.get().defaultBlockState());
                    }
                }
            }
        }
    }

    private static boolean isPlanking(BlockState state) {
        return state == HULL || state == DARK_HULL || state == DECK;
    }

    /** A plank that has fallen out: a hole in a deck, or a gap in the side that shows what is behind it. */
    private void rot(BlockPos pos) {
        if (inside(pos.getX(), pos.getY(), pos.getZ())) {
            blocks.put(pos, AIR);
        } else {
            blocks.remove(pos);
        }
    }

    // ---- the ship as it lies

    /**
     * Turns the ship to a random heading and lays it down: every column sinks or rises as the keel pitches and
     * the hull heels over, so the decks slope while everything that stood upright still does.
     */
    private Ship laidDown() {
        Rotation heading = Rotation.getRandom(random);
        double heel = (random.nextBoolean() ? 1 : -1) * (0.08 + random.nextDouble() * 0.12);
        double pitch = (random.nextDouble() - 0.5) * 0.1;

        Map<BlockPos, BlockState> laid = new HashMap<>();
        List<BlockPos> keel = new ArrayList<>();
        blocks.forEach((pos, state) -> {
            int x = pos.getX() - MIDSHIP;
            int y = pos.getY() + (int) Math.round(heel * pos.getZ() + pitch * x);
            BlockPos laidPos = new BlockPos(x, y, pos.getZ()).rotate(heading);
            laid.put(laidPos, state.rotate(heading));
            // The level part of the keel, without the stem and the stern that rise from it.
            if (pos.getZ() == 0 && pos.getY() == 0) {
                keel.add(laidPos);
            }
        });
        return new Ship(laid, keel);
    }

    // ---- helpers

    private void put(int x, int y, int z, BlockState state) {
        blocks.put(new BlockPos(x, y, z), state);
    }

    private boolean isAir(int x, int y, int z) {
        BlockState state = blocks.get(new BlockPos(x, y, z));
        return state != null && state.isAir();
    }

    private static BlockState log(Block log, Direction.Axis axis) {
        return log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    /** A number from 0 to 1 that depends only on the seed, the block and the salt, not on the order of the calls. */
    private float chance(int x, int y, int z, int salt) {
        return (HashCommon.mix(seed ^ Mth.getSeed(x, y, z) + salt * 0x9E3779B97F4A7C15L) >>> 40) / (float) (1 << 24);
    }
}
