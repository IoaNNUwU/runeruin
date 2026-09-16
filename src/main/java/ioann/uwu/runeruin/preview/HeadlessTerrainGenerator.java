package ioann.uwu.runeruin.preview;

import ioann.uwu.runeruin.dimension.RRBiomeSource;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** The exact headless terrain path used by the preview job and {@code /rrgenerate}. */
public final class HeadlessTerrainGenerator {
    public static final int MAX_CHUNKS = 64;

    private HeadlessTerrainGenerator() {}

    public static PreviewWorld generate(MinecraftServer server, long seed, BoundingBox box) throws IOException {
        RRChunkGenerator generator;
        try {
            generator = new RRChunkGenerator(
                RRBiomeSource.newDefault(server.registryAccess().lookupOrThrow(Registries.BIOME))
            );
        } catch (IllegalStateException e) {
            throw new IOException("Runeruin biomes are missing from the loaded registries", e);
        }

        int minY = generator.getMinY();
        int maxY = minY + generator.getGenDepth() - 1;
        if (box.minY() < minY || box.maxY() > maxY) {
            throw new IOException("Region Y range must be within " + minY + ".." + maxY);
        }

        final RandomState randomState;
        try {
            randomState = RandomState.create(
                NoiseGeneratorSettings.dummy(),
                server.registryAccess().lookupOrThrow(Registries.NOISE),
                seed
            );
        } catch (IllegalStateException e) {
            throw new IOException("Runeruin noise registries are missing from the loaded registries", e);
        }

        Map<Long, ChunkAccess> chunks = generateChunks(generator, server, randomState, box);
        PreviewWorld world = PreviewWorld.create(seed);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    ChunkAccess chunk = chunks.get(ChunkPos.pack(x >> 4, z >> 4));
                    BlockState state = chunk.getBlockState(pos.set(x, y, z));
                    if (!state.isAir()) {
                        world.set(pos, state);
                    }
                }
            }
        }
        return world;
    }

    private static Map<Long, ChunkAccess> generateChunks(
        RRChunkGenerator generator,
        MinecraftServer server,
        RandomState randomState,
        BoundingBox box
    ) {
        Map<Long, ChunkAccess> chunks = new HashMap<>();
        PalettedContainerFactory containers = PalettedContainerFactory.create(server.registryAccess());
        LevelHeightAccessor height = LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth());
        int minChunkX = box.minX() >> 4;
        int maxChunkX = box.maxX() >> 4;
        int minChunkZ = box.minZ() >> 4;
        int maxChunkZ = box.maxZ() >> 4;
        long chunkCount = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (chunkCount > MAX_CHUNKS) {
            throw new IllegalArgumentException("Region spans " + chunkCount + " chunks; replay limit is " + MAX_CHUNKS);
        }

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ProtoChunk chunk = new ProtoChunk(
                    new ChunkPos(chunkX, chunkZ),
                    UpgradeData.EMPTY,
                    height,
                    containers,
                    null
                );
                chunk.fillBiomesFromNoise(generator.getBiomeSource(), randomState.sampler());
                chunk.setPersistedStatus(ChunkStatus.BIOMES);
                generator.fillFromNoise(Blender.empty(), randomState, null, chunk).join();
                chunks.put(chunk.getPos().pack(), chunk);
            }
        }
        return chunks;
    }
}
