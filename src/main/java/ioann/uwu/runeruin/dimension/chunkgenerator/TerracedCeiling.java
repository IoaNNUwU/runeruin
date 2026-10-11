package ioann.uwu.runeruin.dimension.chunkgenerator;

import com.google.common.collect.MapMaker;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import ioann.uwu.runeruin.dimension.noise.Noise;
import ioann.uwu.runeruin.dimension.noise.SingleNoise;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * The shape of a cave ceiling: terraces at several heights with overhanging walls and ledges between them.
 * A slow noise picks the terrace of a place. Every height reads that noise a little aside, by a shift that
 * changes slowly with the height and from place to place: this tilts the walls. Stone that no path of blocks
 * ties to the rock above would float and is left out. The layer tells what each column hangs between.
 */
public final class TerracedCeiling {

    private static final int STEPS = 3;
    // The part of a step that the rounded wall between two terraces takes.
    private static final float RISER = 0.22f;
    private static final float CONTRAST = 1.9f;
    private static final int SHIFT = 13;
    private static final int CELL = 4;
    /** Blocks around a chunk that tell a ledge held by a neighbour from a floating piece. */
    static final int MARGIN = 6;

    /**
     * What the ceiling of a column hangs between.
     *
     * @param bottom the height of the lowest terrace
     * @param depth  how far above it the highest terrace is; 0 leaves one even surface
     * @param roof   everything from this height up is rock
     * @param shift  how far aside the heights read their terrace, from 0 for upright walls to 1
     */
    public record Column(float bottom, float depth, int roof, float shift) {}

    /** The columns of a chunk and of the blocks around it; null where there is no ceiling. */
    @FunctionalInterface
    public interface Columns {
        Column at(int x, int z);
    }

    @FunctionalInterface
    public interface Layer {
        Columns around(int chunkX, int chunkZ, RandomState randomState);
    }

    private final int minY;
    private final int height;
    private final Layer layer;
    private final LazyNoise levelNoise;
    private final LazyNoise shiftXNoise;
    private final LazyNoise shiftZNoise;
    // The last chunks shaped for each level, in the slot of their low coordinates: the terrain and the
    // structures that hang under the ceiling ask about the same chunks.
    private final Map<Climate.Sampler, AtomicReferenceArray<Chunk>> chunks = new MapMaker().weakKeys().makeMap();

    /** The name seeds the noises: renaming a ceiling reshapes it. */
    public TerracedCeiling(String name, int minY, int maxY, Layer layer) {
        this.minY = minY;
        this.height = maxY - minY + 1;
        this.layer = layer;
        this.levelNoise = new LazyNoise(name + "Levels", seed -> {
            Noise wide = new SingleNoise(Noise.hashString(name + "Levels1" + seed), 1f);
            Noise narrow = new SingleNoise(Noise.hashString(name + "Levels2" + seed), 2.6f);
            return (x, y, z) -> 0.65f * wide.noise(x, y, z) + 0.35f * narrow.noise(x, y, z);
        });
        this.shiftXNoise = shiftNoise(name + "ShiftX");
        this.shiftZNoise = shiftNoise(name + "ShiftZ");
    }

    private static LazyNoise shiftNoise(String name) {
        return new LazyNoise(name, seed -> new SingleNoise(Noise.hashString(name + seed), 1.6f, 3.5f));
    }

    public Chunk chunk(int chunkX, int chunkZ, RandomState randomState) {
        AtomicReferenceArray<Chunk> shaped = this.chunks.computeIfAbsent(randomState.sampler(), sampler -> new AtomicReferenceArray<>(32 * 32));
        int slot = (chunkZ & 31) << 5 | chunkX & 31;
        Chunk chunk = shaped.get(slot);
        if (chunk == null || chunk.chunkX != chunkX || chunk.chunkZ != chunkZ) {
            BitSet stone = shape(chunkX, chunkZ, 0, randomState);
            // Stone that nothing holds in its own chunk may hang on a neighbour: only blocks around the chunk tell.
            if (stone == null) {
                stone = shape(chunkX, chunkZ, MARGIN, randomState);
            }
            chunk = new Chunk(chunkX, chunkZ, stone);
            shaped.set(slot, chunk);
        }
        return chunk;
    }

    /**
     * The stone of a chunk, shaped together with {@code margin} blocks around it. Without a margin there is
     * no telling whether stone that nothing holds floats, and the answer is null.
     */
    private BitSet shape(int chunkX, int chunkZ, int margin, RandomState randomState) {
        int size = 16 + 2 * margin;
        int minX = (chunkX << 4) - margin;
        int minZ = (chunkZ << 4) - margin;
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;
        int maxY = this.minY + this.height - 1;
        Columns columns = this.layer.around(chunkX, chunkZ, randomState);
        Grid levels = new Grid(this.levelNoise.getOrCreateNoise(randomState), minX - SHIFT, maxX + SHIFT, 0, 0, minZ - SHIFT, maxZ + SHIFT);
        Grid shiftX = new Grid(this.shiftXNoise.getOrCreateNoise(randomState), minX, maxX, this.minY, maxY, minZ, maxZ);
        Grid shiftZ = new Grid(this.shiftZNoise.getOrCreateNoise(randomState), minX, maxX, this.minY, maxY, minZ, maxZ);
        float[] columnX = new float[shiftX.sizeY];
        float[] columnZ = new float[shiftX.sizeY];

        // 0 is air, 1 is stone, 2 is stone that hangs on the roof.
        byte[] cells = new byte[size * size * this.height];
        int[] hanging = new int[cells.length];
        int count = 0;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int worldX = minX + x;
                int worldZ = minZ + z;
                Column column = columns.at(worldX, worldZ);
                if (column == null) {
                    continue;
                }
                // What lies on the edge of the margin may hang on something further away.
                boolean edge = margin > 0 && (x == 0 || z == 0 || x == size - 1 || z == size - 1);
                float reach = SHIFT * column.shift();
                if (column.depth() != 0) {
                    for (int layer = 0; layer < columnX.length; layer++) {
                        columnX[layer] = shiftX.at(worldX, layer, worldZ);
                        columnZ[layer] = shiftZ.at(worldX, layer, worldZ);
                    }
                }
                for (int y = this.minY; y <= maxY; y++) {
                    int cell = (x * size + z) * this.height + y - this.minY;
                    if (y < column.roof()) {
                        float under = column.bottom();
                        if (column.depth() != 0) {
                            int layer = Math.floorDiv(y, CELL) - shiftX.minY;
                            float part = Math.floorMod(y, CELL) / (float) CELL;
                            float level = level(levels.at(
                                    worldX + reach * (2 * Mth.lerp(part, columnX[layer], columnX[layer + 1]) - 1),
                                    0,
                                    worldZ + reach * (2 * Mth.lerp(part, columnZ[layer], columnZ[layer + 1]) - 1)
                            ));
                            under += column.depth() * level;
                        }
                        if (y < under) {
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
            int y = cell % this.height;
            int z = cell / this.height % size;
            int x = cell / this.height / size;
            count = hang(cells, hanging, count, cell - 1, y > 0);
            count = hang(cells, hanging, count, cell + 1, y < this.height - 1);
            count = hang(cells, hanging, count, cell - this.height, z > 0);
            count = hang(cells, hanging, count, cell + this.height, z < size - 1);
            count = hang(cells, hanging, count, cell - this.height * size, x > 0);
            count = hang(cells, hanging, count, cell + this.height * size, x < size - 1);
        }

        BitSet stone = new BitSet(16 * 16 * this.height);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int cell = ((x + margin) * size + z + margin) * this.height;
                for (int y = 0; y < this.height; y++) {
                    if (cells[cell + y] == 2) {
                        stone.set((x * 16 + z) * this.height + y);
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

    /** The ceiling in one chunk, by the coordinates of a block inside it. */
    public final class Chunk {
        private final int chunkX;
        private final int chunkZ;
        private final BitSet stone;

        private Chunk(int chunkX, int chunkZ, BitSet stone) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.stone = stone;
        }

        /** Whether the ceiling has a block here, between the lowest and the highest height it was made for. */
        public boolean stone(int x, int y, int z) {
            return this.stone.get(column(x, z) + y - TerracedCeiling.this.minY);
        }

        /** The lowest block of the ceiling in this column, or {@link Integer#MAX_VALUE} where there is none. */
        public int lowestY(int x, int z) {
            int column = column(x, z);
            int block = this.stone.nextSetBit(column);
            return block < 0 || block >= column + TerracedCeiling.this.height ? Integer.MAX_VALUE : TerracedCeiling.this.minY + block - column;
        }

        private int column(int x, int z) {
            return ((x & 15) * 16 + (z & 15)) * TerracedCeiling.this.height;
        }
    }

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
