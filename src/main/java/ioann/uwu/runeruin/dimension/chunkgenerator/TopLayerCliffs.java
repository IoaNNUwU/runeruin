package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import ioann.uwu.runeruin.dimension.noise.TopLevelNoise;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
    // A tall wall keeps this many blocks at its foot and under its top, where the hanging soil goes;
    // the blocks of its face between them are dug out.
    private static final int NICHE_FOOT = 2;
    private static final int NICHE_ROOF = 5;

    private TopLayerCliffs() {}

    /** The blocks of the face of a wall that are dug out, from {@code footY} to {@code topY}. */
    record Niche(int footY, int topY) {}

    static void hollowWall(TerrainWriter terrain, int x, int z, int worldX, int worldZ, HangingTerrainGenerator.TerrainColumn column, RandomState randomState) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (Niche niche : nichesAt(worldX, worldZ, column, randomState)) {
            for (int y = niche.footY(); y <= niche.topY(); y++) {
                terrain.set(x, y, z, air);
            }
        }
    }

    /**
     * What is dug out of the face of a tall wall in this column, one block deep in the middle of the wall: the wall
     * rises, steps back and rises again. A column is dug only with two columns of rock behind it, so that no thin
     * cliff is left hanging, and not at the edge of an island, which keeps its shape.
     */
    static List<Niche> nichesAt(int worldX, int worldZ, HangingTerrainGenerator.TerrainColumn column, RandomState randomState) {
        int nicheTop = column.topY() - NICHE_ROOF;
        HangingTerrainGenerator.TerrainColumn[] around = new HangingTerrainGenerator.TerrainColumn[4];
        for (Direction side : Direction.Plane.HORIZONTAL) {
            around[side.get2DDataValue()] = TopLayerAndBloomingCavesGen.topLayerColumnAt(worldX + side.getStepX(), worldZ + side.getStepZ(), randomState);
            if (!around[side.get2DDataValue()].present()) {
                return List.of();
            }
        }
        List<Niche> niches = new ArrayList<>(2);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int footY = around[side.get2DDataValue()].topY() + 1 + NICHE_FOOT;
            if (footY <= nicheTop && isRockBehind(worldX, worldZ, side.getOpposite(), nicheTop, around[side.getOpposite().get2DDataValue()], randomState)) {
                niches.add(new Niche(footY, nicheTop));
            }
        }
        return niches;
    }

    private static boolean isRockBehind(int worldX, int worldZ, Direction back, int nicheTop, HangingTerrainGenerator.TerrainColumn first, RandomState randomState) {
        HangingTerrainGenerator.TerrainColumn second = TopLayerAndBloomingCavesGen.topLayerColumnAt(worldX + 2 * back.getStepX(), worldZ + 2 * back.getStepZ(), randomState);
        return first.topY() > nicheTop && second.present() && second.topY() > nicheTop;
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
