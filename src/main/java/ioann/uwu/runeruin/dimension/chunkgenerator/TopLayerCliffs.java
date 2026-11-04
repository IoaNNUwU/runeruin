package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import ioann.uwu.runeruin.dimension.noise.TopLevelNoise;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
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
    // The face of a wall is dug out between a foot of stone over the ground in front and a lip of stone under the soil
    // that hangs in front, and under the soil of the column itself. In a cut through the wall the niche is a bracket:
    // nothing at the foot and the lip, deepest in the middle: LOW_DEPTH blocks between a foot and a lip LOWEST_BAND
    // blocks apart, DEPTH_GROWTH more for each block more, up to NICHE_DEPTH; a lower wall stays flat. A noise bends
    // the outline by up to NICHE_BEND blocks.
    private static final int NICHE_FOOT = 1;
    private static final int NICHE_LIP = 2;
    private static final int COLUMN_SOIL = 3;
    private static final int LOWEST_BAND = 4;
    private static final float LOW_DEPTH = 1.2f;
    private static final float DEPTH_GROWTH = 0.15f;
    private static final float NICHE_DEPTH = 2.2f;
    private static final float NICHE_BEND = 0.7f;
    private static final int DEEPEST = 2;
    // How many columns of a wall each way share their foot and lip with a column.
    private static final int WALL_REACH = 2;
    private static final LazyNoise NICHE_NOISE = new LazyNoise(seed -> new SingleNoise(Noise.hashString("topLayerCliffNiches" + seed), 12f, 12f));

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
                if (!TopLayerAndBloomingCavesGen.topLayerGroundAt(worldX, worldZ, randomState).onCliff()) {
                    continue;
                }
                // Nothing is dug below the foot over the lowest ground in front.
                int lowestY = Integer.MAX_VALUE;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    for (int k = 1; k <= DEEPEST; k++) {
                        lowestY = Math.min(lowestY, walls.column(worldX + k * side.getStepX(), worldZ + k * side.getStepZ()).topY() + 1 + NICHE_FOOT);
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
        return TopLayerAndBloomingCavesGen.topLayerGroundAt(x, z, randomState).onCliff() && new Walls(randomState).isDug(x, y, z);
    }

    /** The columns around the walls of a chunk and the soil that hangs into them, each looked up once. */
    private static final class Walls {

        private final RandomState randomState;
        private final Noise noise;
        private final Map<Long, HangingTerrainGenerator.TerrainColumn> columns = new HashMap<>();
        private final Map<Long, Integer> hangingSoil = new HashMap<>();

        Walls(RandomState randomState) {
            this.randomState = randomState;
            this.noise = NICHE_NOISE.getOrCreateNoise(randomState);
        }

        HangingTerrainGenerator.TerrainColumn column(int x, int z) {
            return columns.computeIfAbsent(ChunkPos.pack(x, z), key -> TopLayerAndBloomingCavesGen.topLayerColumnAt(x, z, randomState));
        }

        private int lowestHangingSoil(int x, int z) {
            return hangingSoil.computeIfAbsent(ChunkPos.pack(x, z), key -> TopLayerAndBloomingCavesGen.lowestHangingSoilY(x, z, randomState));
        }

        /**
         * Whether a block of a cliff column is dug: it lies one or two blocks behind the open air in front of a wall,
         * with two columns of rock behind it, so that no thin cliff is left, and the bracket of that wall reaches as
         * deep at its height. Nothing is dug at the edge of an island, which keeps its shape.
         */
        boolean isDug(int x, int y, int z) {
            HangingTerrainGenerator.TerrainColumn self = column(x, z);
            if (y > self.topY()) {
                return false;
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (!column(x + side.getStepX(), z + side.getStepZ()).present()) {
                    return false;
                }
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                for (int behind = 1; behind <= DEEPEST; behind++) {
                    // The face of the wall stands behind - 1 blocks towards the side, the open air one block further.
                    int faceX = x + (behind - 1) * side.getStepX();
                    int faceZ = z + (behind - 1) * side.getStepZ();
                    HangingTerrainGenerator.TerrainColumn face = column(faceX, faceZ);
                    HangingTerrainGenerator.TerrainColumn open = column(faceX + side.getStepX(), faceZ + side.getStepZ());
                    if (face.topY() < y) {
                        break;
                    }
                    if (!open.present()) {
                        break;
                    }
                    if (open.topY() >= y) {
                        continue;
                    }
                    int footY = footY(open);
                    int lipY = lipY(faceX, faceZ, face, side);
                    if (y < footY || y > lipY || self.topY() <= lipY || !isRockBehind(x, z, side, lipY)) {
                        break;
                    }
                    // The bracket follows the wall, not each column: the soil hangs to a different length from column
                    // to column, and the ground in front steps. It spans the foot and the lip of the columns of the
                    // wall around on average, and only the column's own bound what is dug.
                    float footSum = 0;
                    float lipSum = 0;
                    int count = 0;
                    Direction along = side.getClockWise();
                    for (int k = -WALL_REACH; k <= WALL_REACH; k++) {
                        int wallX = faceX + k * along.getStepX();
                        int wallZ = faceZ + k * along.getStepZ();
                        HangingTerrainGenerator.TerrainColumn wall = column(wallX, wallZ);
                        HangingTerrainGenerator.TerrainColumn front = column(wallX + side.getStepX(), wallZ + side.getStepZ());
                        if (wall.topY() >= y && front.present() && front.topY() < y) {
                            footSum += footY(front);
                            lipSum += lipY(wallX, wallZ, wall, side);
                            count++;
                        }
                    }
                    if (bracket(faceX, y, faceZ, footSum / count, lipSum / count) >= behind) {
                        return true;
                    }
                    break;
                }
            }
            return false;
        }

        private static int footY(HangingTerrainGenerator.TerrainColumn open) {
            return open.topY() + 1 + NICHE_FOOT;
        }

        private int lipY(int faceX, int faceZ, HangingTerrainGenerator.TerrainColumn face, Direction side) {
            return Math.min(lowestHangingSoil(faceX + side.getStepX(), faceZ + side.getStepZ()), face.topY() - COLUMN_SOIL) - 1 - NICHE_LIP;
        }

        private boolean isRockBehind(int x, int z, Direction side, int lipY) {
            for (int k = 1; k <= 2; k++) {
                HangingTerrainGenerator.TerrainColumn back = column(x - k * side.getStepX(), z - k * side.getStepZ());
                if (!back.present() || back.topY() <= lipY) {
                    return false;
                }
            }
            return true;
        }

        /** How deep the niche of the face in this column reaches at {@code y}, in blocks. */
        private float bracket(int faceX, int y, int faceZ, float footY, float lipY) {
            float height = lipY - footY + 1;
            if (height < LOWEST_BAND) {
                return 0;
            }
            float t = Math.clamp((y - footY + 0.5f) / height, 0f, 1f);
            float depth = Math.min(NICHE_DEPTH, LOW_DEPTH + DEPTH_GROWTH * (height - LOWEST_BAND)) * Mth.sin(Mth.PI * t);
            return depth + NICHE_BEND * (2 * noise.noise(faceX, y, faceZ) - 1);
        }
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
