package ioann.uwu.runeruin.dimension.chunkgenerator;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Writes terrain straight into the sections of a chunk that is still empty, like the vanilla noise generator:
 * every section is locked once for the whole fill instead of on every block, and the worldgen heightmaps are
 * updated in place instead of being primed by a scan of the whole chunk. Blocks and heightmaps come out the
 * same as with {@link ChunkAccess#setBlockState}. Reads go through {@link #chunk()}; writing past this class
 * while it is open fails, since the sections are locked.
 */
public final class TerrainWriter implements AutoCloseable {

    private final ChunkAccess chunk;
    private final LevelChunkSection[] sections;
    private final Heightmap oceanFloor;
    private final Heightmap worldSurface;

    public TerrainWriter(ChunkAccess chunk) {
        this.chunk = chunk;
        this.sections = chunk.getSections();
        this.oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        this.worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (LevelChunkSection section : sections) {
            section.acquire();
        }
    }

    public ChunkAccess chunk() {
        return chunk;
    }

    /** Sets a block at chunk-local {@code x} and {@code z} and world height {@code y}. */
    public void set(int x, int y, int z, BlockState state) {
        if (chunk.isOutsideBuildHeight(y)) {
            return;
        }
        LevelChunkSection section = sections[chunk.getSectionIndex(y)];
        if (section.hasOnlyAir() && state.is(Blocks.AIR)) {
            return;
        }
        section.setBlockState(x & 15, y & 15, z & 15, state, false);
        oceanFloor.update(x & 15, y, z & 15, state);
        worldSurface.update(x & 15, y, z & 15, state);
    }

    @Override
    public void close() {
        for (LevelChunkSection section : sections) {
            section.release();
        }
    }
}
