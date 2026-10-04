package ioann.uwu.runeruin.preview;

import ioann.uwu.runeruin.dimension.RRBiomeSource;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
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
        RRChunkGenerator generator = generator(server);
        int minY = generator.getMinY();
        int maxY = minY + generator.getGenDepth() - 1;
        if (box.minY() < minY || box.maxY() > maxY) {
            throw new IOException("Region Y range must be within " + minY + ".." + maxY);
        }

        RandomState randomState = randomState(server, seed);
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

    public static RRChunkGenerator generator(MinecraftServer server) throws IOException {
        try {
            return new RRChunkGenerator(RRBiomeSource.newDefault(server.registryAccess().lookupOrThrow(Registries.BIOME)));
        } catch (IllegalStateException e) {
            throw new IOException("Runeruin biomes are missing from the loaded registries", e);
        }
    }

    /** Empty chunks with biomes, as a level hands them to {@code fillFromNoise}. */
    public static Function<ChunkPos, ProtoChunk> chunkFactory(RRChunkGenerator generator, MinecraftServer server, RandomState randomState) {
        PalettedContainerFactory containers = PalettedContainerFactory.create(server.registryAccess());
        LevelHeightAccessor height = LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth());
        return pos -> {
            ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, height, containers, null);
            chunk.fillBiomesFromNoise(generator.getBiomeSource(), randomState.sampler());
            chunk.setPersistedStatus(ChunkStatus.BIOMES);
            return chunk;
        };
    }

    /** The RandomState a level with this seed gets, with the seed bound for {@link LazyNoise} like in-game. */
    public static RandomState randomState(MinecraftServer server, long seed) throws IOException {
        try {
            RandomState randomState = RandomState.create(
                NoiseGeneratorSettings.dummy(),
                server.registryAccess().lookupOrThrow(Registries.NOISE),
                seed
            );
            LazyNoise.bindSeed(randomState, seed);
            return randomState;
        } catch (IllegalStateException e) {
            throw new IOException("Runeruin noise registries are missing from the loaded registries", e);
        }
    }

    private static Map<Long, ChunkAccess> generateChunks(
        RRChunkGenerator generator,
        MinecraftServer server,
        RandomState randomState,
        BoundingBox box
    ) {
        Map<Long, ChunkAccess> chunks = new HashMap<>();
        Function<ChunkPos, ProtoChunk> chunkFactory = chunkFactory(generator, server, randomState);
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
                ProtoChunk chunk = chunkFactory.apply(new ChunkPos(chunkX, chunkZ));
                generator.fillFromNoise(Blender.empty(), randomState, null, chunk).join();
                chunks.put(chunk.getPos().pack(), chunk);
            }
        }
        return chunks;
    }
}
