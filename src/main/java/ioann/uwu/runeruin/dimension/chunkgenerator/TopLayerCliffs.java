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
    // A tall wall keeps at least this many blocks at its foot and under its top, and the blocks of its face between
    // them are dug out. The top is one block lower than the hanging soil reaches, so that the stone steps under it
    // instead of being flush with it. Each of them grows by up to RAMP blocks, one block a column along the wall and
    // back, as a cliff steps down by itself; a slow noise shifts where the steps turn.
    private static final int NICHE_FOOT = 1;
    private static final int NICHE_ROOF = 6;
    private static final int RAMP = 2;
    private static final LazyNoise RAMP_SHIFT = LazyNoise.single("topLayerCliffNicheShift", 2f);
    private static final float RAMP_SHIFT_BLOCKS = 12;
    // A niche of fewer blocks is not worth it: such a wall stays flat.
    private static final int LOWEST_NICHE = 4;
    // Next to an inner corner a niche loses this many blocks at its foot and under its top, a block less with every
    // column away from it.
    private static final int INNER_NARROWING = 2;
    // How far along a wall its corners are looked for, and from how many columns a straight wall counts as long.
    private static final int WALL_REACH = 8;
    private static final int LONG_WALL = 4;

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
     * rises, steps back and rises again. Towards an inner corner of the wall more blocks stay at the foot and under
     * the top, so that the niche narrows to the corner instead of ending in a flat wall. A column is dug
     * only with two columns of rock behind it, so that no thin cliff is left hanging, and not at the edge of an
     * island, which keeps its shape.
     */
    static List<Niche> nichesAt(int worldX, int worldZ, HangingTerrainGenerator.TerrainColumn column, RandomState randomState) {
        Noise shift = RAMP_SHIFT.getOrCreateNoise(randomState);
        int footShift = Math.round(shift.noise(worldX, worldZ) * RAMP_SHIFT_BLOCKS);
        int roofShift = Math.round(shift.noise(worldX + 1000, worldZ + 1000) * RAMP_SHIFT_BLOCKS);
        int rockTop = column.topY() - NICHE_ROOF;
        HangingTerrainGenerator.TerrainColumn[] around = new HangingTerrainGenerator.TerrainColumn[4];
        for (Direction side : Direction.Plane.HORIZONTAL) {
            around[side.get2DDataValue()] = TopLayerAndBloomingCavesGen.topLayerColumnAt(worldX + side.getStepX(), worldZ + side.getStepZ(), randomState);
            if (!around[side.get2DDataValue()].present()) {
                return List.of();
            }
        }
        List<Niche> niches = new ArrayList<>(2);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            HangingTerrainGenerator.TerrainColumn front = around[side.get2DDataValue()];
            int footY = front.topY() + 1 + NICHE_FOOT;
            if (!isRockBehind(worldX, worldZ, side.getOpposite(), rockTop, around[side.getOpposite().get2DDataValue()], randomState)) {
                continue;
            }
            Wall wall = wallAround(worldX, worldZ, side, rockTop, spare(column, front), around, randomState);
            // The straight stretch of the wall decides whether it is dug, so that it is dug to its ends: a column a
            // block lower than the rest keeps a block less of stone over the niche. The variation only shortens it.
            int topY = Math.max(rockTop, Math.min(footY + LOWEST_NICHE - 1, rockTop + 1));
            int spare = topY - footY + 1 - LOWEST_NICHE;
            if (wall.spare() < 0 || spare < 0) {
                continue;
            }
            // The wall facing this side runs along the other axis. A long straight wall varies slower than the short
            // stretches between the steps of a corner.
            int along = side.getAxis() == Direction.Axis.X ? worldZ : worldX;
            boolean straight = wall.length() >= LONG_WALL;
            int speed = straight ? 2 : 1;
            int lessFoot = Math.min(ramp(Math.floorDiv(along + footShift, speed)), spare);
            int lessRoof = Math.min(ramp(Math.floorDiv(along + roofShift, speed)), spare - lessFoot);
            // Towards an inner corner the niche narrows a block at a time down to its tip, on the steps of a corner by
            // one block; at an outer corner it stays whole.
            int nicheFoot = footY + lessFoot;
            int nicheTop = topY - lessRoof;
            int deepest = straight ? INNER_NARROWING : 1;
            int narrow = Math.min(Math.min(Math.max(0, deepest + 1 - wall.toInner()), wall.toOuter()), (nicheTop - nicheFoot) / 2);
            niches.add(new Niche(nicheFoot + narrow, nicheTop - narrow));
        }
        return niches;
    }

    /** 0 up to {@link #RAMP} and down again, one step a block. */
    private static int ramp(int index) {
        return Math.abs(Math.floorMod(index, 2 * RAMP) - RAMP);
    }

    /** How many blocks more than the lowest niche a column can give to a wall that faces {@code front}. */
    private static int spare(HangingTerrainGenerator.TerrainColumn column, HangingTerrainGenerator.TerrainColumn front) {
        return column.topY() - NICHE_ROOF - (front.topY() + 1 + NICHE_FOOT) + 1 - LOWEST_NICHE;
    }

    /**
     * The straight stretch of the wall that faces {@code side} through this column, up to {@link #WALL_REACH} columns
     * each way: how far its inner and outer corners are (one for the column next to it), how long it is and how much
     * the best of its columns could spare.
     */
    private record Wall(int toInner, int toOuter, int length, int spare) {}

    private static Wall wallAround(int worldX, int worldZ, Direction side, int rockTop, int spare, HangingTerrainGenerator.TerrainColumn[] around, RandomState randomState) {
        int toInner = WALL_REACH + 1;
        int toOuter = WALL_REACH + 1;
        int length = 1;
        for (Direction way : new Direction[]{side.getClockWise(), side.getCounterClockWise()}) {
            for (int k = 1; k <= WALL_REACH; k++) {
                int x = worldX + k * way.getStepX();
                int z = worldZ + k * way.getStepZ();
                HangingTerrainGenerator.TerrainColumn next = k == 1 ? around[way.get2DDataValue()] : TopLayerAndBloomingCavesGen.topLayerColumnAt(x, z, randomState);
                // The wall turns back where the next column is low, and comes forward where the one before it is high.
                if (!next.present() || next.topY() <= rockTop) {
                    toOuter = Math.min(toOuter, k);
                    break;
                }
                HangingTerrainGenerator.TerrainColumn nextFront = TopLayerAndBloomingCavesGen.topLayerColumnAt(x + side.getStepX(), z + side.getStepZ(), randomState);
                if (!nextFront.present()) {
                    toOuter = Math.min(toOuter, k);
                    break;
                }
                if (nextFront.topY() > rockTop) {
                    toInner = Math.min(toInner, k);
                    break;
                }
                length++;
                spare = Math.max(spare, spare(next, nextFront));
            }
        }
        return new Wall(toInner, toOuter, length, spare);
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
