package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRChunkGenerator;
import ioann.uwu.runeruin.preview.HeadlessTerrainGenerator;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import ioann.uwu.runeruin.region.RegionExport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import jdk.jfr.Recording;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * Times {@link RRChunkGenerator#fillFromNoise} on headless chunks, one at a time and all at once on the
 * generator's executor, under a JFR recording. The checksum of blocks and heightmaps must not change
 * when generation only gets faster.
 */
public final class TerrainBenchmarkPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "terrain_bench";
    }

    @Override
    public String description() {
        return "Time RRChunkGenerator.fillFromNoise, write a checksum and a JFR recording. params: radius, rounds, warmup";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) {
        throw new IllegalStateException("terrain_bench requires a running server registry");
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, MinecraftServer server) throws IOException {
        int radius = args.getInt("radius", 8);
        int rounds = args.getInt("rounds", 10);
        int warmup = args.getInt("warmup", 2);
        if (radius < 0 || rounds < 1 || warmup < 0) {
            throw new IllegalArgumentException("terrain_bench needs radius >= 0, rounds >= 1, warmup >= 0");
        }
        RRChunkGenerator generator = HeadlessTerrainGenerator.generator(server);
        RandomState randomState = HeadlessTerrainGenerator.randomState(server, args.seed());
        Function<ChunkPos, ProtoChunk> chunkFactory = HeadlessTerrainGenerator.chunkFactory(generator, server, randomState);
        List<ChunkPos> positions = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                positions.add(new ChunkPos(x, z));
            }
        }

        String name = RegionExport.sanitizeName(args.name("terrain_bench"));
        Files.createDirectories(args.exportDir());
        Path reportPath = args.exportDir().resolve(name + ".txt");
        Path jfrPath = args.exportDir().resolve(name + ".jfr");
        long[] chunkNanos = new long[rounds * positions.size()];
        long[] roundNanos = new long[rounds];
        List<ProtoChunk> sequential = List.of();
        List<ProtoChunk> parallel = List.of();
        try (Recording recording = new Recording()) {
            recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(1));
            for (int round = -warmup; round < rounds; round++) {
                if (round == 0) {
                    recording.start();
                }
                sequential = positions.stream().map(chunkFactory).toList();
                for (int i = 0; i < sequential.size(); i++) {
                    long start = System.nanoTime();
                    fill(generator, randomState, sequential.get(i)).join();
                    if (round >= 0) {
                        chunkNanos[round * sequential.size() + i] = System.nanoTime() - start;
                    }
                }

                parallel = positions.stream().map(chunkFactory).toList();
                long start = System.nanoTime();
                CompletableFuture.allOf(parallel.stream().map(chunk -> fill(generator, randomState, chunk)).toArray(CompletableFuture[]::new)).join();
                if (round >= 0) {
                    roundNanos[round] = System.nanoTime() - start;
                }
            }
            recording.dump(jfrPath);
        }

        long sequentialChecksum = checksum(sequential);
        long parallelChecksum = checksum(parallel);
        Arrays.sort(chunkNanos);
        Arrays.sort(roundNanos);
        double medianRoundMs = roundNanos[rounds / 2] / 1e6;
        String jfrTool = ProcessHandle.current().info().command().orElse("java").replaceFirst("java(\\.exe)?$", "jfr$1");
        String report = String.join("\n",
            "# runeruin.terrain_bench/1",
            "seed: " + args.seed(),
            "chunks: " + positions.size() + " (radius " + radius + " around chunk 0 0)",
            "rounds: " + rounds + " measured after " + warmup + " warmup",
            "processors: " + Runtime.getRuntime().availableProcessors(),
            "java: " + Runtime.version(),
            String.format(Locale.ROOT, "sequential ms/chunk: median %.2f, p90 %.2f, mean %.2f",
                chunkNanos[chunkNanos.length / 2] / 1e6, chunkNanos[chunkNanos.length * 9 / 10] / 1e6, Arrays.stream(chunkNanos).average().orElse(0) / 1e6),
            String.format(Locale.ROOT, "parallel chunks/s: %.1f (median round %.1f ms, best %.1f ms)",
                positions.size() / (medianRoundMs / 1e3), medianRoundMs, roundNanos[0] / 1e6),
            "checksum: " + Long.toHexString(sequentialChecksum),
            "jfr: " + jfrPath.toAbsolutePath(),
            "hot methods: " + jfrTool + " view hot-methods " + jfrPath.toAbsolutePath(),
            ""
        );
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
        RR.LOGGER.info("Terrain benchmark -> {}\n{}", reportPath.toAbsolutePath(), report);
        if (sequentialChecksum != parallelChecksum) {
            throw new IOException("Parallel generation gave other terrain: checksum " + Long.toHexString(parallelChecksum)
                + " instead of " + Long.toHexString(sequentialChecksum));
        }
        return new PreviewJobs.Result(PreviewWorld.create(args.seed()), null, reportPath, List.of(reportPath, jfrPath));
    }

    private static CompletableFuture<?> fill(RRChunkGenerator generator, RandomState randomState, ProtoChunk chunk) {
        return generator.fillFromNoise(Blender.empty(), randomState, null, chunk);
    }

    /** Hashes block states by name, so blocks added to the registry do not change it. */
    private static long checksum(List<ProtoChunk> chunks) {
        Map<BlockState, Integer> stateHashes = new IdentityHashMap<>();
        long hash = 0;
        for (ProtoChunk chunk : chunks) {
            for (LevelChunkSection section : chunk.getSections()) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            hash = hash * 31 + stateHashes.computeIfAbsent(section.getBlockState(x, y, z), state -> state.toString().hashCode());
                        }
                    }
                }
            }
            for (Map.Entry<Heightmap.Types, Heightmap> heightmap : chunk.getHeightmaps()) {
                hash = hash * 31 + heightmap.getKey().ordinal();
                hash = hash * 31 + Arrays.hashCode(heightmap.getValue().getRawData());
            }
        }
        return hash;
    }
}
