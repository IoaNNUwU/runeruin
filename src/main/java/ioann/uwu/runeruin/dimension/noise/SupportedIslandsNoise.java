package ioann.uwu.runeruin.dimension.noise;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * A land noise without the islands that nothing holds up. Land is where the noise is above {@link #LAND};
 * a connected island with no support under it is lowered to {@link #LAND}, so every reader of the noise
 * sees the same land.
 */
public class SupportedIslandsNoise implements Noise {

    public static final float LAND = 0.5f;
    // An island that reaches this level is large and is not walked further: over six 8192 x 8192 areas
    // every island without a support stayed below it, in the top layer below 0.515.
    private static final float LARGE_ISLAND = 0.53f;
    private static final int MAX_SEARCHED_BLOCKS = 16384;
    private static final long[] NOTHING_DROPPED = new long[4];

    private final Noise noise;
    private final Support support;
    private final Land land;
    // The last chunks asked about, in the slot of their low coordinates: chunks generated together never
    // share a slot, and threads that race for one compute the same answer.
    private final AtomicReferenceArray<ChunkIslands> chunks = new AtomicReferenceArray<>(64 * 64);

    public SupportedIslandsNoise(Noise noise, Support support) {
        this.noise = noise;
        this.support = support;
        this.land = (x, z) -> noise.noise(x, z) > LAND;
    }

    @Override
    public float noise(float x, float y, float z) {
        float value = noise.noise(x, y, z);
        if (value <= LAND || value >= LARGE_ISLAND) {
            return value;
        }
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        return isDropped(droppedIn(blockX >> 4, blockZ >> 4), blockX, blockZ) ? LAND : value;
    }

    private long[] droppedIn(int chunkX, int chunkZ) {
        int slot = (chunkZ & 63) << 6 | chunkX & 63;
        ChunkIslands chunk = chunks.get(slot);
        if (chunk == null || chunk.chunkX != chunkX || chunk.chunkZ != chunkZ) {
            chunk = new ChunkIslands(chunkX, chunkZ, findDropped(chunkX, chunkZ));
            chunks.set(slot, chunk);
        }
        return chunk.dropped;
    }

    private static boolean isDropped(long[] dropped, int x, int z) {
        int bit = (z & 15) << 4 | x & 15;
        return (dropped[bit >> 6] >>> bit & 1) != 0;
    }

    private long[] findDropped(int chunkX, int chunkZ) {
        long[] dropped = NOTHING_DROPPED;
        LongOpenHashSet supported = new LongOpenHashSet();
        for (int z = chunkZ << 4; z < (chunkZ << 4) + 16; z++) {
            for (int x = chunkX << 4; x < (chunkX << 4) + 16; x++) {
                float value = noise.noise(x, z);
                if (value <= LAND || value >= LARGE_ISLAND || isDropped(dropped, x, z) || supported.contains(BlockPos.asLong(x, 0, z))) {
                    continue;
                }
                Long2FloatOpenHashMap island = new Long2FloatOpenHashMap();
                island.put(BlockPos.asLong(x, 0, z), value);
                if (isSupported(island, supported)) {
                    supported.addAll(island.keySet());
                    continue;
                }
                if (dropped == NOTHING_DROPPED) {
                    dropped = new long[4];
                }
                for (long block : island.keySet()) {
                    int blockX = BlockPos.getX(block);
                    int blockZ = BlockPos.getZ(block);
                    if (blockX >> 4 == chunkX && blockZ >> 4 == chunkZ) {
                        int bit = (blockZ & 15) << 4 | blockX & 15;
                        dropped[bit >> 6] |= 1L << bit;
                    }
                }
            }
        }
        return dropped;
    }

    /**
     * Walks the island from its first block, highest noise first, collecting the blocks with their noise,
     * until the island proves large or supported. Every chunk an island touches walks all of it or stops
     * for a reason the others also meet, so they agree.
     */
    private boolean isSupported(Long2FloatOpenHashMap island, LongOpenHashSet supported) {
        LongHeapPriorityQueue queue = new LongHeapPriorityQueue((a, b) -> Float.compare(island.get(b), island.get(a)));
        queue.enqueue(island.keySet().iterator().nextLong());
        while (!queue.isEmpty()) {
            long block = queue.dequeueLong();
            int x = BlockPos.getX(block);
            int z = BlockPos.getZ(block);
            boolean chunkCorner = ((x & 15) == 0 || (x & 15) == 15) && ((z & 15) == 0 || (z & 15) == 15);
            if (chunkCorner && support.under(x >> 4, z >> 4, land)) {
                return true;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int nextX = x + direction.getStepX();
                int nextZ = z + direction.getStepZ();
                long next = BlockPos.asLong(nextX, 0, nextZ);
                if (island.containsKey(next)) {
                    continue;
                }
                if (supported.contains(next)) {
                    return true;
                }
                float value = noise.noise(nextX, nextZ);
                if (value >= LARGE_ISLAND) {
                    return true;
                }
                if (value > LAND) {
                    island.put(next, value);
                    queue.enqueue(next);
                }
            }
            if (island.size() > MAX_SEARCHED_BLOCKS) {
                return true;
            }
        }
        return false;
    }

    /** One bit per block of the chunk, set where an island was dropped. */
    private record ChunkIslands(int chunkX, int chunkZ, long[] dropped) {}

    @FunctionalInterface
    public interface Land {
        boolean at(int x, int z);
    }

    /** Whether something holding the land up stands in this chunk. */
    @FunctionalInterface
    public interface Support {
        boolean under(int chunkX, int chunkZ, Land land);
    }
}
