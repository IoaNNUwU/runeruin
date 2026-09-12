package ioann.uwu.runeruin.dimension.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** Limits a placed feature's terrain access to its source chunk and the eight neighboring chunks. */
final class FeatureChunkBounds {
    private static final int CHUNK_RADIUS = 1;

    private final int sourceChunkX;
    private final int sourceChunkZ;

    FeatureChunkBounds(BlockPos source) {
        this.sourceChunkX = SectionPos.blockToSectionCoord(source.getX());
        this.sourceChunkZ = SectionPos.blockToSectionCoord(source.getZ());
    }

    boolean contains(BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        return Math.abs(chunkX - this.sourceChunkX) <= CHUNK_RADIUS
                && Math.abs(chunkZ - this.sourceChunkZ) <= CHUNK_RADIUS;
    }
}
