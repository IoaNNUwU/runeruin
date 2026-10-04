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
import java.io.Reader;
import java.io.Writer;
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
import java.util.Properties;
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
 * generator's executor, under a JFR recording. With {@code before=<name>} it compares with a saved result
 * in a before / after / gain table. The checksum of blocks and heightmaps must not change when generation
 * only gets faster.
 */
public final class TerrainBenchmarkPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "terrain_bench";
    }

    @Override
    public String description() {
        return "Time RRChunkGenerator.fillFromNoise, compare with a saved result. params: before, radius, rounds, warmup";
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
        // Results are kept outside the checkout, so a task folder can compare with one measured in another.
        Path savedDir = Path.of(System.getProperty("user.home"), ".runeruin", "bench");
        String before = args.get("before", "");
        Properties beforeResult = before.isEmpty() ? null : load(savedDir, before, args.seed(), positions.size());
        Files.createDirectories(args.exportDir());
        Path reportPath = args.exportDir().resolve(name + ".md");
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
        Properties result = new Properties();
        result.setProperty("seed", Long.toString(args.seed()));
        result.setProperty("chunks", Integer.toString(positions.size()));
        result.setProperty("one_ms", Double.toString(chunkNanos[chunkNanos.length / 2] / 1e6));
        result.setProperty("all_ms", Double.toString(roundNanos[rounds / 2] / 1e6));
        result.setProperty("checksum", Long.toHexString(sequentialChecksum));
        String table = beforeResult == null ? table(result) : table(beforeResult, before, result, name);
        Files.createDirectories(savedDir);
        try (Writer writer = Files.newBufferedWriter(savedDir.resolve(name + ".properties"))) {
            result.store(writer, null);
        }

        String jfrTool = ProcessHandle.current().info().command().orElse("java").replaceFirst("java(\\.exe)?$", "jfr$1");
        String report = table + "\n" + String.join("\n",
            "- one chunk at a time: median " + ms(chunkNanos[chunkNanos.length / 2]) + ", p90 " + ms(chunkNanos[chunkNanos.length * 9 / 10]),
            "- all chunks at once: median " + ms(roundNanos[rounds / 2]) + ", best " + ms(roundNanos[0]),
            "- " + rounds + " rounds after " + warmup + " warm-up, chunks within " + radius + " of chunk 0 0, "
                + Runtime.getRuntime().availableProcessors() + " processors, Java " + Runtime.version(),
            "- checksum " + Long.toHexString(sequentialChecksum),
            "- CPU profile: `" + jfrTool + " view hot-methods " + jfrPath.toAbsolutePath() + "`",
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

    private static Properties load(Path savedDir, String name, long seed, int chunks) throws IOException {
        Path path = savedDir.resolve(name + ".properties");
        if (!Files.isRegularFile(path)) {
            throw new IOException("No saved result '" + name + "': run terrain_bench with -PpreviewName=" + name + " first");
        }
        Properties saved = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            saved.load(reader);
        }
        if (!Long.toString(seed).equals(saved.getProperty("seed")) || !Integer.toString(chunks).equals(saved.getProperty("chunks"))) {
            throw new IOException("'" + name + "' was measured with another seed or radius");
        }
        return saved;
    }

    private static String table(Properties result) {
        return "| " + title(result) + " | Time |\n|---|---:|\n"
            + "| One chunk at a time, ms per chunk | " + value(result, "one_ms") + " |\n"
            + "| All chunks at once, ms | " + value(result, "all_ms") + " |\n";
    }

    private static String table(Properties before, String beforeName, Properties after, String afterName) {
        return "| " + title(after) + " | Before: " + beforeName + " | After: " + afterName + " | Gain |\n|---|---:|---:|---:|\n"
            + row("One chunk at a time, ms per chunk", before, after, "one_ms")
            + row("All chunks at once, ms", before, after, "all_ms")
            + "\nTerrain: " + (before.getProperty("checksum").equals(after.getProperty("checksum")) ? "identical" : "DIFFERENT from " + beforeName)
            + ". Gain is how much faster it got; up to 5% (one at a time) and 10% (all at once) either way it is measurement noise.\n";
    }

    private static String title(Properties result) {
        return "Terrain fill, seed " + result.getProperty("seed") + ", " + result.getProperty("chunks") + " chunks";
    }

    private static String row(String label, Properties before, Properties after, String key) {
        double gain = Double.parseDouble(before.getProperty(key)) / Double.parseDouble(after.getProperty(key)) - 1;
        return "| " + label + " | " + value(before, key) + " | " + value(after, key) + " | " + String.format(Locale.ROOT, "%+.0f%%", gain * 100) + " |\n";
    }

    private static String value(Properties result, String key) {
        return String.format(Locale.ROOT, "%.2f", Double.parseDouble(result.getProperty(key)));
    }

    private static String ms(long nanos) {
        return String.format(Locale.ROOT, "%.2f ms", nanos / 1e6);
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
