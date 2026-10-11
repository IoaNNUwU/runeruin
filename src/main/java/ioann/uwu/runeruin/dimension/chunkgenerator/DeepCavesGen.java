package ioann.uwu.runeruin.dimension.chunkgenerator;

import com.google.common.collect.MapMaker;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.BLOOMING_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.DEEP_CAVES_CEILING_Y;
import static ioann.uwu.runeruin.dimension.Const.TERRAIN_HEIGHT;

/**
 * The ceiling of the Deep caves: terraces at several heights, from {@link #LOWEST_Y} up into the floor of the
 * Blooming caves. Every height of the ceiling is shifted sideways on its own, so the steps between the
 * terraces overhang and leave ledges with pockets above them.
 */
public class DeepCavesGen {

    /** Nothing of the ceiling is lower than this. */
    public static final int LOWEST_Y = DEEP_CAVES_CEILING_Y - 12;
    // Stone that always stays under the surface of the Blooming caves floor.
    private static final int CRUST = 6;
    /** The highest {@link #roofY}: nothing is open from here up to the Blooming caves. */
    public static final int HIGHEST_Y = BLOOMING_CAVES_Y + TERRAIN_HEIGHT - CRUST;

    private static final int HEIGHT = HIGHEST_Y - LOWEST_Y + 1;
    private static final int STEPS = 3;
    // The part of a step that the rounded wall between two terraces takes.
    private static final float RISER = 0.22f;
    private static final float CONTRAST = 1.9f;
    private static final int SHIFT = 13;
    private static final int CELL = 4;
    // Blocks around a chunk that tell a ledge held by a neighbour from a floating piece.
    private static final int MARGIN = 6;

    private static final LazyNoise levelNoise = new LazyNoise("deepCavesCeilingLevels", seed -> {
        Noise wide = new SingleNoise(Noise.hashString("deepCavesCeilingLevels1" + seed), 1f);
        Noise narrow = new SingleNoise(Noise.hashString("deepCavesCeilingLevels2" + seed), 2.6f);
        return (x, y, z) -> 0.65f * wide.noise(x, y, z) + 0.35f * narrow.noise(x, y, z);
    });
    private static final LazyNoise reliefNoise = LazyNoise.single("deepCavesCeilingRelief", 9f);
    private static final LazyNoise shiftXNoise = shiftNoise("deepCavesCeilingShiftX");
    private static final LazyNoise shiftZNoise = shiftNoise("deepCavesCeilingShiftZ");

    // The last chunks shaped for each level, in the slot of their low coordinates: the terrain and the
    // structures that hang under the ceiling ask about the same chunks.
    private static final Map<Climate.Sampler, AtomicReferenceArray<Shape>> SHAPES = new MapMaker().weakKeys().makeMap();

    private static LazyNoise shiftNoise(String name) {
        return new LazyNoise(name, seed -> new SingleNoise(Noise.hashString(name + seed), 1.6f, 3.5f));
    }

    /** The lowest block that is never open: stone from here up to the floor of the Blooming caves. */
    public static int roofY(int x, int z, Climate.Sampler sampler) {
        // The floor reads its noise 8 blocks aside.
        return TopLayerAndBloomingCavesGen.bloomingCavesFloorY(x + 8, z + 8, sampler) - CRUST;
    }

    /** The lowest block of the Deep caves ceiling in this column. */
    public static int ceilingSurfaceY(int x, int z, RandomState randomState) {
        int column = column(x & 15, z & 15);
        return LOWEST_Y + shapeAt(x >> 4, z >> 4, randomState).stone.nextSetBit(column) - column;
    }

    public static void generateDeepCavesCeiling(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        ChunkPos pos = chunk.getPos();
        BitSet stone = shapeAt(pos.x(), pos.z(), randomState).stone;
        BlockState stoneState = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int column = column(x, z) - LOWEST_Y;
                for (int y = LOWEST_Y; y <= HIGHEST_Y; y++) {
                    boolean solid = stone.get(column + y);
                    // Under the Blooming caves the ceiling is built, in their floor it is dug out.
                    if (solid != y >= BLOOMING_CAVES_Y) {
                        terrain.set(x, y, z, solid ? stoneState : air);
                    }
                }
                for (int y = LOWEST_Y; y <= HIGHEST_Y; y++) {
                    if (!stone.get(column + y) || y > LOWEST_Y && stone.get(column + y - 1)) {
                        continue;
                    }
                    BlockState surface = RRTerrainSurfaces.ceilingAt(chunk, pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z, randomState);
                    if (y == HIGHEST_Y || stone.get(column + y + 1)) {
                        RRTerrainSurfaces.placeCeilingSurface(terrain, x, y, z, surface);
                    } else {
                        terrain.set(x, y, z, surface);
                    }
                }
            }
        }
    }

    private static int column(int x, int z) {
        return (x * 16 + z) * HEIGHT;
    }

    private static Shape shapeAt(int chunkX, int chunkZ, RandomState randomState) {
        AtomicReferenceArray<Shape> shapes = SHAPES.computeIfAbsent(randomState.sampler(), sampler -> new AtomicReferenceArray<>(32 * 32));
        int slot = (chunkZ & 31) << 5 | chunkX & 31;
        Shape shape = shapes.get(slot);
        if (shape == null || shape.chunkX != chunkX || shape.chunkZ != chunkZ) {
            BitSet stone = shape(chunkX, chunkZ, 0, randomState);
            // Stone that nothing holds in its own chunk may hang on a neighbour: only blocks around the chunk tell.
            if (stone == null) {
                stone = shape(chunkX, chunkZ, MARGIN, randomState);
            }
            shape = new Shape(chunkX, chunkZ, stone);
            shapes.set(slot, shape);
        }
        return shape;
    }

    /**
     * The stone of a chunk, shaped together with {@code margin} blocks around it. Stone that no path of blocks
     * ties to the roof would float and is left out; without a margin there is no telling, and the answer is null.
     */
    private static BitSet shape(int chunkX, int chunkZ, int margin, RandomState randomState) {
        int size = 16 + 2 * margin;
        int minX = (chunkX << 4) - margin;
        int minZ = (chunkZ << 4) - margin;
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;
        Grid levels = new Grid(levelNoise.getOrCreateNoise(randomState), minX - SHIFT, maxX + SHIFT, 0, 0, minZ - SHIFT, maxZ + SHIFT);
        Grid shiftX = new Grid(shiftXNoise.getOrCreateNoise(randomState), minX, maxX, LOWEST_Y, HIGHEST_Y, minZ, maxZ);
        Grid shiftZ = new Grid(shiftZNoise.getOrCreateNoise(randomState), minX, maxX, LOWEST_Y, HIGHEST_Y, minZ, maxZ);
        Noise relief = reliefNoise.getOrCreateNoise(randomState);
        float[] columnX = new float[shiftX.sizeY];
        float[] columnZ = new float[shiftX.sizeY];

        // 0 is air, 1 is stone, 2 is stone that hangs on the roof.
        byte[] cells = new byte[size * size * HEIGHT];
        int[] hanging = new int[cells.length];
        int count = 0;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int worldX = minX + x;
                int worldZ = minZ + z;
                int roof = roofY(worldX, worldZ, randomState.sampler());
                // The highest terrace stays 2 blocks under the roof, in the ceiling biome with what hangs from it.
                float bottom = LOWEST_Y + (relief.noise(worldX, worldZ) - 0.5f) * 3f;
                float depth = roof - 2 - LOWEST_Y;
                // What lies on the edge of the margin may hang on something further away.
                boolean edge = margin > 0 && (x == 0 || z == 0 || x == size - 1 || z == size - 1);
                for (int layer = 0; layer < columnX.length; layer++) {
                    columnX[layer] = shiftX.at(worldX, layer, worldZ);
                    columnZ[layer] = shiftZ.at(worldX, layer, worldZ);
                }
                for (int y = LOWEST_Y; y <= HIGHEST_Y; y++) {
                    int cell = (x * size + z) * HEIGHT + y - LOWEST_Y;
                    if (y < roof) {
                        int layer = Math.floorDiv(y, CELL) - shiftX.minY;
                        float part = Math.floorMod(y, CELL) / (float) CELL;
                        float level = level(levels.at(
                                worldX + SHIFT * (2 * Mth.lerp(part, columnX[layer], columnX[layer + 1]) - 1),
                                0,
                                worldZ + SHIFT * (2 * Mth.lerp(part, columnZ[layer], columnZ[layer + 1]) - 1)
                        ));
                        if (y < bottom + depth * level) {
                            continue;
                        }
                        if (!edge) {
                            cells[cell] = 1;
                            continue;
                        }
                    }
                    cells[cell] = 2;
                    hanging[count++] = cell;
                }
            }
        }

        while (count > 0) {
            int cell = hanging[--count];
            int y = cell % HEIGHT;
            int z = cell / HEIGHT % size;
            int x = cell / HEIGHT / size;
            count = hang(cells, hanging, count, cell - 1, y > 0);
            count = hang(cells, hanging, count, cell + 1, y < HEIGHT - 1);
            count = hang(cells, hanging, count, cell - HEIGHT, z > 0);
            count = hang(cells, hanging, count, cell + HEIGHT, z < size - 1);
            count = hang(cells, hanging, count, cell - HEIGHT * size, x > 0);
            count = hang(cells, hanging, count, cell + HEIGHT * size, x < size - 1);
        }

        BitSet stone = new BitSet(16 * 16 * HEIGHT);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int cell = ((x + margin) * size + z + margin) * HEIGHT;
                for (int y = 0; y < HEIGHT; y++) {
                    if (cells[cell + y] == 2) {
                        stone.set(column(x, z) + y);
                    } else if (cells[cell + y] == 1 && margin == 0) {
                        return null;
                    }
                }
            }
        }
        return stone;
    }

    private static int hang(byte[] cells, int[] hanging, int count, int cell, boolean inside) {
        if (inside && cells[cell] == 1) {
            cells[cell] = 2;
            hanging[count++] = cell;
        }
        return count;
    }

    /** The terrace of this noise, from 0 for the lowest to 1 for the highest, with rounded walls between them. */
    private static float level(float noise) {
        float steps = Mth.clamp((noise - 0.5f) * CONTRAST + 0.5f, 0f, 0.9999f) * STEPS;
        int step = (int) steps;
        float riser = Mth.clamp((steps - step - 0.5f) / RISER + 0.5f, 0f, 1f);
        return (step + riser * riser * (3 - 2 * riser)) / STEPS;
    }

    private record Shape(int chunkX, int chunkZ, BitSet stone) {}

    /** A slow noise sampled every {@link #CELL} blocks: between the samples it is interpolated. */
    private static final class Grid {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int sizeY;
        private final int sizeZ;
        private final float[] samples;

        Grid(Noise noise, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = Math.floorDiv(minX, CELL);
            this.minY = Math.floorDiv(minY, CELL);
            this.minZ = Math.floorDiv(minZ, CELL);
            int sizeX = Math.floorDiv(maxX, CELL) - this.minX + 2;
            // One layer for a noise taken at a single height, otherwise a sample above the highest block too.
            this.sizeY = minY == maxY ? 1 : Math.floorDiv(maxY, CELL) - this.minY + 2;
            this.sizeZ = Math.floorDiv(maxZ, CELL) - this.minZ + 2;
            this.samples = new float[sizeX * this.sizeY * this.sizeZ];
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < this.sizeY; y++) {
                    for (int z = 0; z < this.sizeZ; z++) {
                        this.samples[(x * this.sizeY + y) * this.sizeZ + z] = noise.noise(
                                (this.minX + x) * CELL, (this.minY + y) * CELL, (this.minZ + z) * CELL
                        );
                    }
                }
            }
        }

        /** The noise in a layer of samples, counted from the lowest one. */
        float at(double x, int layer, double z) {
            double gridX = x / CELL - this.minX;
            double gridZ = z / CELL - this.minZ;
            int cellX = (int) gridX;
            int cellZ = (int) gridZ;
            int nextX = this.sizeY * this.sizeZ;
            int i = cellX * nextX + layer * this.sizeZ + cellZ;
            return (float) Mth.lerp2(
                    gridX - cellX, gridZ - cellZ,
                    this.samples[i], this.samples[i + nextX], this.samples[i + 1], this.samples[i + nextX + 1]
            );
        }
    }
}
