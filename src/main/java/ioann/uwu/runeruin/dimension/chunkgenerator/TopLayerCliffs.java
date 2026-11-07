package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import ioann.uwu.runeruin.dimension.noise.TopLevelNoise;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * The cliffs of the top layer: one layer of them on the ground of the biomes, each biome with its own how often and
 * how winding. They stand where a noise passes the threshold of the biome, so how far it has passed it tells how far
 * a column lies from the wall: their tops rise from the wall over a shoulder.
 */
final class TopLayerCliffs {

    private static final LazyNoise WIDE = new LazyNoise(seed -> Noise.multi(
            new SingleNoise(Noise.hashString("topLayerWideCliffs1" + seed), 0.45f),
            new SingleNoise(Noise.hashString("topLayerWideCliffs2" + seed), 0.8f),
            new SingleNoise(Noise.hashString("topLayerWideCliffs3" + seed), 2f)
    ));
    // Bends the relief of the islands for winding cliffs, which it alone leaves too round and too far apart.
    private static final LazyNoise WINDING = LazyNoise.single("topLayerCliffWinding", 2.6f);
    // Cliffs stand on walls of different height: from this share of the height of their biome to all of it.
    private static final LazyNoise WALL_HEIGHT = LazyNoise.single("topLayerCliffWalls", 1.6f);
    private static final float LOWEST_WALL = 0.5f;
    // The top goes on rising from the wall by this share of the height, over this much of the noise: a small cliff
    // never reaches the whole of it.
    private static final float SHOULDER = 0.55f;
    private static final float SHOULDER_WIDTH = 0.09f;
    private static final float CALM_THRESHOLD = 0.25f;
    // The face of a wall is hollowed into a concave surface that follows the wall around: between a row of stone over
    // the ground in front and a row under the soil that hangs in front, a cut through the wall is an arc of a circle,
    // DEPTH_SHARE of its height deep and at most NICHE_DEPTH; one lower than LOWEST_BAND stays flat. How deep a block
    // lies in the wall is how far the open air nearest to it is, so the surface keeps the bends of the wall. A noise
    // bends the surface by up to NICHE_BEND blocks.
    private static final int NICHE_FOOT = 1;
    private static final int NICHE_LIP = 1;
    private static final int COLUMN_SOIL = 3;
    private static final float LOWEST_BAND = 3;
    private static final float DEPTH_SHARE = 0.25f;
    private static final float NICHE_DEPTH = 0.9f;
    private static final float NICHE_BEND = 0.25f;
    private static final LazyNoise NICHE_NOISE = new LazyNoise(seed -> new SingleNoise(Noise.hashString("topLayerCliffNiches" + seed), 8f, 8f));
    // How far the open air is looked for; the ground and the soil of the wall within BAND_REACH shape the arc, the
    // nearer the more. Rock that goes on less than THINNEST_ROCK blocks behind a block, as at the narrow end of a
    // cliff, is left whole.
    private static final int OPEN_REACH = 4;
    private static final int BAND_REACH = 3;
    private static final float BAND_SPREAD = 4.5f;
    // The ground and the soil within sqrt(NEAR) blocks bound the surface: it is at most EDGE_SLOPE blocks deep for
    // each block of height away from them.
    private static final int NEAR = 5;
    private static final float EDGE_SLOPE = 1.5f;
    // Where a wall steps, the hollow grows taller round each outer corner, so that its edges do not run level from one
    // stretch of the wall to the next: up and down by CORNER_RISE blocks for each column within sqrt(NEAR) open more
    // than in front of a straight wall, by at most CORNER_REACH.
    private static final float CORNER_RISE = 0.5f;
    private static final int CORNER_REACH = 1;
    private static final int THINNEST_ROCK = 6;
    private static final int[][] AROUND = around();
    private static final long STRAIGHT_OPEN = Arrays.stream(AROUND).filter(offset -> offset[2] <= NEAR && offset[0] < 0).count();

    private TopLayerCliffs() {}

    /** Digs the faces of the cliff walls of a chunk. */
    static void hollowWalls(TerrainWriter terrain, RandomState randomState) {
        BlockState air = Blocks.AIR.defaultBlockState();
        Walls walls = new Walls(randomState);
        int minX = terrain.chunk().getPos().getMinBlockX();
        int minZ = terrain.chunk().getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = minX + x;
                int worldZ = minZ + z;
                if (!walls.onCliff(worldX, worldZ)) {
                    continue;
                }
                // Nothing is dug below the foot over the lowest ground in front, lowered round a corner.
                int lowestY = Integer.MAX_VALUE;
                for (int[] offset : AROUND) {
                    if (offset[2] <= BAND_REACH * BAND_REACH) {
                        lowestY = Math.min(lowestY, walls.column(worldX + offset[0], worldZ + offset[1]).topY() + 1 + NICHE_FOOT - CORNER_REACH);
                    }
                }
                HangingTerrainGenerator.TerrainColumn column = walls.column(worldX, worldZ);
                for (int y = column.topY() - COLUMN_SOIL - NICHE_LIP; y >= lowestY; y--) {
                    if (walls.isDug(worldX, y, worldZ)) {
                        terrain.set(x, y, z, air);
                    }
                }
            }
        }
    }

    /** Whether this block of the top layer is dug out of the face of a cliff wall. */
    static boolean isDug(int x, int y, int z, RandomState randomState) {
        return new Walls(randomState).isDug(x, y, z);
    }

    /** The columns around the walls of a chunk, the soil that hangs into them and their dug blocks, each found once. */
    private static final class Walls {

        private final RandomState randomState;
        private final Noise noise;
        private final Map<Long, HangingTerrainGenerator.TerrainColumn> columns = new HashMap<>();
        private final Map<Long, Boolean> cliffs = new HashMap<>();
        private final Map<Long, Integer> hangingSoil = new HashMap<>();
        private final Map<Long, Boolean> hollowed = new HashMap<>();
        private final Map<Long, Boolean> cut = new HashMap<>();
        private final Map<Long, Boolean> dug = new HashMap<>();

        Walls(RandomState randomState) {
            this.randomState = randomState;
            this.noise = NICHE_NOISE.getOrCreateNoise(randomState);
        }

        HangingTerrainGenerator.TerrainColumn column(int x, int z) {
            return columns.computeIfAbsent(ChunkPos.pack(x, z), key -> TopLayerAndBloomingCavesGen.topLayerColumnAt(x, z, randomState));
        }

        boolean onCliff(int x, int z) {
            return cliffs.computeIfAbsent(ChunkPos.pack(x, z), key -> TopLayerAndBloomingCavesGen.topLayerGroundAt(x, z, randomState).onCliff());
        }

        private int lowestHangingSoil(int x, int z) {
            return hangingSoil.computeIfAbsent(ChunkPos.pack(x, z), key -> TopLayerAndBloomingCavesGen.lowestHangingSoilY(x, z, randomState));
        }

        /**
         * Whether a block of a cliff column is dug: cut, but not in a square corner of the hollow, which is left round,
         * and with the block in front of it towards the air open or dug, so that no hollow is left inside.
         */
        boolean isDug(int x, int y, int z) {
            long key = BlockPos.asLong(x, y, z);
            Boolean known = dug.get(key);
            if (known == null) {
                dug.put(key, false);
                int[] towards = isCut(x, y, z) ? AROUND[nearestOpen(x, y, z)] : null;
                known = towards != null && !isSquareCorner(x, y, z, towards)
                        && (towards[0] != 0 && isOpenOrDug(x + Integer.signum(towards[0]), y, z)
                        || towards[1] != 0 && isOpenOrDug(x, y, z + Integer.signum(towards[1])));
                dug.put(key, known);
            }
            return known;
        }

        /** Whether a block of a cliff column is cut out of the wall: hollowed, or a fin of rock that hollowing would leave. */
        private boolean isCut(int x, int y, int z) {
            long key = BlockPos.asLong(x, y, z);
            Boolean known = cut.get(key);
            if (known == null) {
                known = isHollowed(x, y, z) || isFin(x, y, z);
                cut.put(key, known);
            }
            return known;
        }

        /**
         * Whether a hollowed block of the face of a wall has the rock of the wall both over or under it and beside it
         * along the face: the corner where the hollow meets the end of a stretch of the wall, as in the inner corner of
         * a step.
         */
        private boolean isSquareCorner(int x, int y, int z, int[] towards) {
            int alongX = towards[0] == 0 ? 1 : 0;
            int alongZ = 1 - alongX;
            // A block open on two sides is an outer corner, as each one of a wall that steps by single blocks.
            if (towards[0] != 0 && towards[1] != 0 || isOpen(x + alongX, y, z + alongZ) || isOpen(x - alongX, y, z - alongZ)) {
                return false;
            }
            return (isUncut(x, y + 1, z) || isUncut(x, y - 1, z))
                    && (isUncut(x + alongX, y, z + alongZ) || isUncut(x - alongX, y, z - alongZ));
        }

        private boolean isUncut(int x, int y, int z) {
            return isSolid(x, y, z) && !isCut(x, y, z);
        }

        private boolean isHollowed(int x, int y, int z) {
            long key = BlockPos.asLong(x, y, z);
            Boolean known = hollowed.get(key);
            if (known == null) {
                known = hollows(x, y, z);
                hollowed.put(key, known);
            }
            return known;
        }

        private boolean isInWall(int x, int y, int z) {
            return y <= column(x, z).topY() - COLUMN_SOIL - NICHE_LIP && onCliff(x, z);
        }

        /**
         * Whether a block of a cliff column is hollowed: the concave surface of the wall around passes behind it, the
         * rock goes on far enough behind it, so that no thin cliff is left, and the block in front of it towards the
         * air is hollowed too, so that no hollow is left inside. Nothing is dug towards the edge of an island, which
         * keeps its shape.
         */
        private boolean hollows(int x, int y, int z) {
            HangingTerrainGenerator.TerrainColumn self = column(x, z);
            if (!isInWall(x, y, z)) {
                return false;
            }
            int nearest = nearestOpen(x, y, z);
            if (nearest < 0 || !column(x + AROUND[nearest][0], z + AROUND[nearest][1]).present()) {
                return false;
            }
            float open = distance(AROUND[nearest]);
            if (open >= NICHE_DEPTH + NICHE_BEND) {
                return false;
            }
            // The arc spans the ground and the soil of the wall around on average: the soil hangs to a different
            // length from column to column, and the ground in front steps. Next to the ground and the soil in front
            // the surface steps in by one block a row.
            float weights = 0;
            float bottom = 0;
            float top = 0;
            float nearBottom = -Float.MAX_VALUE;
            float nearTop = Float.MAX_VALUE;
            for (int[] offset : AROUND) {
                if (offset[2] > BAND_REACH * BAND_REACH) {
                    break;
                }
                HangingTerrainGenerator.TerrainColumn front = column(x + offset[0], z + offset[1]);
                int soilY = front.present() && front.topY() < y ? lowestHangingSoil(x + offset[0], z + offset[1]) : Integer.MAX_VALUE;
                if (soilY == Integer.MAX_VALUE) {
                    continue;
                }
                int footY = front.topY() + 1 + NICHE_FOOT;
                int lipY = Math.min(soilY, self.topY() - COLUMN_SOIL + 1) - NICHE_LIP;
                if (offset[2] <= NEAR) {
                    nearBottom = Math.max(nearBottom, footY);
                    nearTop = Math.min(nearTop, lipY);
                }
                float weight = (float) Math.exp(-offset[2] / BAND_SPREAD);
                weights += weight;
                bottom += weight * footY;
                top += weight * lipY;
            }
            if (weights == 0) {
                return false;
            }
            float rise = corner(x, y, z);
            float middle = y + 0.5f;
            float depth = arc(middle, bottom / weights - rise, top / weights + rise) + NICHE_BEND * (2 * noise.noise(x, y, z) - 1);
            depth = Math.min(depth, EDGE_SLOPE * Math.min(middle - nearBottom + rise, nearTop + rise - middle));
            int[] towards = AROUND[nearest];
            if (open >= depth || !isThick(x, y, z, towards)) {
                return false;
            }
            return towards[0] != 0 && isOpenOrHollowed(x + Integer.signum(towards[0]), y, z)
                    || towards[1] != 0 && isOpenOrHollowed(x, y, z + Integer.signum(towards[1]));
        }

        /** How far the hollow reaches up and down beyond its ends at this block, round an outer corner of the wall. */
        private float corner(int x, int y, int z) {
            int open = 0;
            for (int[] offset : AROUND) {
                if (offset[2] > NEAR) {
                    break;
                }
                HangingTerrainGenerator.TerrainColumn column = column(x + offset[0], z + offset[1]);
                if (!column.present() || column.topY() < y) {
                    open++;
                }
            }
            return Mth.clamp((open - STRAIGHT_OPEN) * CORNER_RISE, 0, CORNER_REACH);
        }

        /** Whether a block is a fin of rock one block thick between a hollowed block and the air or another one. */
        private boolean isFin(int x, int y, int z) {
            return isInWall(x, y, z) && (isFinAlong(x, y, z, 1, 0) || isFinAlong(x, y, z, 0, 1));
        }

        private boolean isFinAlong(int x, int y, int z, int dx, int dz) {
            boolean before = isHollowed(x - dx, y, z - dz);
            boolean after = isHollowed(x + dx, y, z + dz);
            return (before || after) && (before || isOpen(x - dx, y, z - dz)) && (after || isOpen(x + dx, y, z + dz));
        }

        private boolean isOpenOrHollowed(int x, int y, int z) {
            return isOpen(x, y, z) || isHollowed(x, y, z);
        }

        private boolean isOpenOrDug(int x, int y, int z) {
            return isOpen(x, y, z) || isDug(x, y, z);
        }

        private boolean isOpen(int x, int y, int z) {
            HangingTerrainGenerator.TerrainColumn column = column(x, z);
            return column.present() && column.topY() < y;
        }

        /** The index in {@link #AROUND} of the nearest column open at {@code y} or beyond an island, -1 if none. */
        private int nearestOpen(int x, int y, int z) {
            for (int i = 0; i < AROUND.length; i++) {
                HangingTerrainGenerator.TerrainColumn column = column(x + AROUND[i][0], z + AROUND[i][1]);
                if (!column.present() || column.topY() < y) {
                    return i;
                }
            }
            return -1;
        }

        /** Whether the rock at this height goes on THINNEST_ROCK blocks from the block away from the air it faces. */
        private boolean isThick(int x, int y, int z, int[] towards) {
            return (towards[0] == 0 || inwards(x, y, z, -Integer.signum(towards[0]), 0) >= THINNEST_ROCK)
                    && (towards[1] == 0 || inwards(x, y, z, 0, -Integer.signum(towards[1])) >= THINNEST_ROCK);
        }

        private int inwards(int x, int y, int z, int dx, int dz) {
            int length = 1;
            while (length < THINNEST_ROCK && isSolid(x + length * dx, y, z + length * dz)) {
                length++;
            }
            return length;
        }

        private boolean isSolid(int x, int y, int z) {
            HangingTerrainGenerator.TerrainColumn column = column(x, z);
            return column.present() && column.topY() >= y;
        }

        /** How deep an arc from {@code bottom} to {@code top} reaches at {@code y}, negative beyond its ends. */
        private static float arc(float y, float bottom, float top) {
            float height = top - bottom;
            if (height < LOWEST_BAND) {
                return 0;
            }
            float depth = Math.min(NICHE_DEPTH, DEPTH_SHARE * height);
            float radius = (height * height / 4 + depth * depth) / (2 * depth);
            float off = y - (bottom + top) / 2;
            return (float) Math.sqrt(Math.max(0, radius * radius - off * off)) - (radius - depth);
        }
    }

    /** How far the face of a column at this offset lies from the middle of a block, in blocks. */
    private static float distance(int[] offset) {
        return (float) Math.sqrt(offset[2]) - 0.5f;
    }

    /** The columns within {@link #OPEN_REACH} as {dx, dz, squared distance}, the nearest first. */
    private static int[][] around() {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -OPEN_REACH; dx <= OPEN_REACH; dx++) {
            for (int dz = -OPEN_REACH; dz <= OPEN_REACH; dz++) {
                int squared = dx * dx + dz * dz;
                if (squared > 0 && squared <= OPEN_REACH * OPEN_REACH) {
                    offsets.add(new int[]{dx, dz, squared});
                }
            }
        }
        offsets.sort(Comparator.comparingInt(offset -> offset[2]));
        return offsets.toArray(int[][]::new);
    }

    /** How high a cliff stands above the ground of this column, 0 where there is none. */
    static float heightAt(int x, int z, float wild, TopLayerRelief.Land land, RandomState randomState) {
        if (land.cliffHeight() <= 0 || wild <= 0) {
            return 0;
        }
        // On the way to the calm land of a biome the cliffs grow rarer and end before it.
        float threshold = land.cliffThreshold() + (1 - wild) * CALM_THRESHOLD;
        float margin = field(x, z, land.cliffWinding(), randomState) - threshold;
        if (margin <= 0) {
            return 0;
        }
        float wall = LOWEST_WALL + (1 - LOWEST_WALL) * Noise.smooth(WALL_HEIGHT.getOrCreateNoise(randomState).noise(x, z), 0.25f, 0.75f);
        return land.cliffHeight() * (wall + SHOULDER * Noise.smooth(margin, 0, SHOULDER_WIDTH));
    }

    /** What the threshold of a biome is compared with: the relief of the islands for winding cliffs, a wider noise for calm ones. */
    private static float field(float x, float z, float winding, RandomState randomState) {
        float wide = WIDE.getOrCreateNoise(randomState).noise(x, z);
        if (winding <= 0) {
            return wide;
        }
        float detail = ((TopLevelNoise) RRChunkGenerator.topLevelNoise.getOrCreateNoise(randomState)).detail(x, z);
        return Mth.lerp(winding, wide, Mth.lerp(0.45f, detail, WINDING.getOrCreateNoise(randomState).noise(x, z)));
    }
}
