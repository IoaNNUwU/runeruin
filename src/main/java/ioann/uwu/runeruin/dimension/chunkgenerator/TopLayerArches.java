package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.PositionalRandomNoise;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Arches of rock over the gaps between the cliffs of the top layer. A point is scattered per square, in a random
 * direction; it makes an arch when it lies in a gap with a cliff on both sides of it. So arches come where the cliffs
 * of a biome are, and every arch stands on two of them, read from the terrain alone, the same in any chunk.
 */
final class TopLayerArches {

    private static final LazyNoise RANDOM = new LazyNoise(seed -> new PositionalRandomNoise("topLayerArches", seed));
    private static final int SPACING = 24;
    private static final int MIN_SPAN = 7;
    private static final int MAX_HALF_SPAN = 18;
    // The ends of an arch lie this far inside the cliffs, on their tops and not on the edge of their walls.
    private static final int END_INSIDE = 3;
    private static final int WALK_STEP = 3;
    private static final int MAX_END_DIFFERENCE = 9;
    private static final int LOWEST_OPENING = 3;
    private static final float CROWN_THICKNESS = 3;
    private static final float CROWN_HALF_WIDTH = 2.2f;
    // How much wider each foot of an arch is than its crown.
    private static final float FOOT_FLARE = 3.5f;
    private static final float SIDE_DROP = 1.2f;
    private static final float RISE_PER_SPAN = 0.18f;
    private static final float MAX_RISE = 7;
    private static final float MAX_BEND_PER_SPAN = 0.14f;
    private static final int REACH = MAX_HALF_SPAN + END_INSIDE + (int) (CROWN_HALF_WIDTH + FOOT_FLARE) + 1;

    private TopLayerArches() {}

    static List<Arch> around(ChunkPos pos, RandomState randomState) {
        List<Arch> arches = new ArrayList<>();
        int minCellX = Math.floorDiv(pos.getMinBlockX() - REACH, SPACING);
        int maxCellX = Math.floorDiv(pos.getMaxBlockX() + REACH, SPACING);
        int minCellZ = Math.floorDiv(pos.getMinBlockZ() - REACH, SPACING);
        int maxCellZ = Math.floorDiv(pos.getMaxBlockZ() + REACH, SPACING);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                Arch arch = archOfCell(cellX, cellZ, randomState);
                if (arch != null) {
                    arches.add(arch);
                }
            }
        }
        return arches;
    }

    private static Arch archOfCell(int cellX, int cellZ, RandomState randomState) {
        Noise random = RANDOM.getOrCreateNoise(randomState);
        int x = (cellX * SPACING) + (int) (random.noise(cellX, 0, cellZ) * SPACING);
        int z = (cellZ * SPACING) + (int) (random.noise(cellX, 1, cellZ) * SPACING);
        TopLayerAndBloomingCavesGen.TopLayerGround gap = TopLayerAndBloomingCavesGen.topLayerGroundAt(x, z, randomState);
        if (!gap.column().present() || gap.onCliff()) {
            return null;
        }
        float angle = random.noise(cellX, 2, cellZ) * Mth.PI;
        float bend = random.noise(cellX, 3, cellZ) * 2 - 1;
        // The gap may run along the first direction: then the second crosses it.
        Arch arch = archAcross(x, z, gap.column().topY(), Mth.cos(angle), Mth.sin(angle), bend, randomState);
        return arch != null ? arch : archAcross(x, z, gap.column().topY(), -Mth.sin(angle), Mth.cos(angle), bend, randomState);
    }

    private static Arch archAcross(int x, int z, int floorY, float dirX, float dirZ, float bend, RandomState randomState) {
        int after = toCliff(x, z, dirX, dirZ, randomState);
        if (after < 0) {
            return null;
        }
        int before = toCliff(x, z, -dirX, -dirZ, randomState);
        if (before < 0 || before + after < MIN_SPAN) {
            return null;
        }
        float startX = x - dirX * (before + END_INSIDE);
        float startZ = z - dirZ * (before + END_INSIDE);
        float endX = x + dirX * (after + END_INSIDE);
        float endZ = z + dirZ * (after + END_INSIDE);
        TopLayerAndBloomingCavesGen.TopLayerGround start = TopLayerAndBloomingCavesGen.topLayerGroundAt(Mth.floor(startX), Mth.floor(startZ), randomState);
        TopLayerAndBloomingCavesGen.TopLayerGround end = TopLayerAndBloomingCavesGen.topLayerGroundAt(Mth.floor(endX), Mth.floor(endZ), randomState);
        if (!start.onCliff() || !end.onCliff() || Math.abs(start.column().topY() - end.column().topY()) > MAX_END_DIFFERENCE) {
            return null;
        }
        float span = before + after + 2 * END_INSIDE;
        float rise = Math.min(RISE_PER_SPAN * span, MAX_RISE);
        int crownUnderside = (int) ((start.column().topY() + end.column().topY()) / 2f + rise - CROWN_THICKNESS);
        if (crownUnderside - floorY < LOWEST_OPENING) {
            return null;
        }
        return new Arch(startX, startZ, dirX, dirZ, span, start.column().topY(), end.column().topY(), rise, bend * MAX_BEND_PER_SPAN * span, floorY);
    }

    /** How far the wall of a cliff lies in this direction, -1 when none is near or the island ends before it. */
    private static int toCliff(int x, int z, float dirX, float dirZ, RandomState randomState) {
        for (int distance = WALK_STEP; distance <= MAX_HALF_SPAN; distance += WALK_STEP) {
            TopLayerAndBloomingCavesGen.TopLayerGround ground = TopLayerAndBloomingCavesGen.topLayerGroundAt(
                    Mth.floor(x + dirX * distance), Mth.floor(z + dirZ * distance), randomState
            );
            if (!ground.column().present()) {
                return -1;
            }
            if (ground.onCliff()) {
                return distance;
            }
        }
        return -1;
    }

    /** An arch from the point {@code x z}, along the direction {@code dirX dirZ}, over the gap whose ground lies at {@code floorY}. */
    record Arch(float x, float z, float dirX, float dirZ, float span, int startY, int endY, float rise, float bend, int floorY) {

        void place(TerrainWriter terrain, RandomState randomState) {
            ChunkPos pos = terrain.chunk().getPos();
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    placeColumn(terrain, localX, localZ, pos.getMinBlockX() + localX, pos.getMinBlockZ() + localZ, randomState);
                }
            }
        }

        private void placeColumn(TerrainWriter terrain, int localX, int localZ, int worldX, int worldZ, RandomState randomState) {
            float aside = aside(worldX, worldZ);
            // A column that touches the rest of the arch only at a corner would float.
            if (aside >= 1 || aside(worldX + 1, worldZ) >= 1 && aside(worldX - 1, worldZ) >= 1 && aside(worldX, worldZ + 1) >= 1 && aside(worldX, worldZ - 1) >= 1) {
                return;
            }
            float along = along(worldX, worldZ);
            float arc = arc(along);
            // The arch is round across as well: its sides lie lower on top and higher underneath.
            float side = SIDE_DROP * (1 - Mth.sqrt(1 - aside * aside));
            float deck = Mth.lerp(along, this.startY, this.endY) + this.rise * arc;
            float underside = this.floorY + (deck - CROWN_THICKNESS - this.floorY) * Mth.sqrt(arc);
            int top = (int) (deck - side);
            int columnTop = TopLayerAndBloomingCavesGen.topLayerSurfaceY(worldX, worldZ, randomState).orElse(Integer.MIN_VALUE);
            if (columnTop == Integer.MIN_VALUE || top <= columnTop) {
                return;
            }
            int bottom = Math.min(top - 1, Math.max(columnTop, (int) (underside + side)));
            Holder<Biome> biome = TopLayerRelief.floorBiome(worldX, worldZ, randomState);
            BlockState stone = Blocks.STONE.defaultBlockState();
            BlockState subfloor = RRTerrainSurfaces.subfloorFor(biome);
            for (int y = bottom; y < top; y++) {
                terrain.set(localX, y, localZ, y >= top - 2 ? subfloor : stone);
            }
            terrain.set(localX, top, localZ, RRTerrainSurfaces.floorFor(biome, worldX, top, worldZ, randomState));
        }

        /** How far along the arch a column lies, from 0 at its start to 1 at its end. */
        private float along(int worldX, int worldZ) {
            return ((worldX + 0.5f - this.x) * this.dirX + (worldZ + 0.5f - this.z) * this.dirZ) / this.span;
        }

        /** 1 in the middle of the arch, 0 at its ends. */
        private static float arc(float along) {
            float fromMiddle = 2 * along - 1;
            return 1 - fromMiddle * fromMiddle;
        }

        /** How far a column lies from the middle line of the arch, from 0 to 1 at its side and more outside it. */
        private float aside(int worldX, int worldZ) {
            float along = along(worldX, worldZ);
            if (along < 0 || along > 1) {
                return 1;
            }
            float arc = arc(along);
            float line = (worldZ + 0.5f - this.z) * this.dirX - (worldX + 0.5f - this.x) * this.dirZ - this.bend * arc;
            return Math.abs(line) / (CROWN_HALF_WIDTH + FOOT_FLARE * (1 - arc));
        }
    }
}
