package ioann.uwu.runeruin.preview.jobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ioann.uwu.runeruin.dimension.RRDimension;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.HeadlessTerrainGenerator;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import ioann.uwu.runeruin.region.RegionExport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Replays the real Runeruin terrain generator for an exported world region. */
public final class TerrainRegionPreviewJob implements PreviewJob {
    private static final List<String> STAGES = List.of("generated", "generated_after_modify");
    private static final int MAX_CHUNKS = 64;

    @Override
    public String id() {
        return "world_region";
    }

    @Override
    public String description() {
        return "Replay RRChunkGenerator for an exported region. params: region, stage";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) {
        throw new IllegalStateException("world_region requires a running server registry");
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, MinecraftServer server) throws IOException {
        String stage = args.get("stage", "generated");
        if (!STAGES.contains(stage)) {
            throw new IOException("stage must be one of: " + String.join(", ", STAGES));
        }
        Path input = resolveInput(args);
        RegionInput region = readRegion(input);
        String dimension = RRDimension.LEVEL.identifier().toString();
        if (!dimension.equals(region.dimension())) {
            throw new IOException("Expected a " + dimension + " export, got " + region.dimension());
        }

        PreviewWorld world = HeadlessTerrainGenerator.generate(server, region.seed(), region.box());
        BoundingBox box = region.box();

        String sourceName = input.getFileName().toString().replaceFirst("\\.json$", "");
        String outputName = RegionExport.sanitizeName(sourceName + "_" + stage);
        List<String> info = new ArrayList<>(List.of(
            "job: " + id(),
            "stage: " + stage,
            "source: " + input.toAbsolutePath(),
            "seed: " + region.seed(),
            "dimension: " + region.dimension(),
            "origin: " + box.minX() + " " + box.minY() + " " + box.minZ(),
            "size: " + box.getXSpan() + " x " + box.getYSpan() + " x " + box.getZSpan()
        ));
        return PreviewJobs.export(world, region.dimension(), box, outputName, args.exportDir(), info);
    }

    private static Path resolveInput(PreviewArgs args) throws IOException {
        String raw = args.get("region", "");
        if (raw.isBlank()) {
            throw new IOException("Pass -Pregion=exports/<name>.json from an in-game //rrexport");
        }
        Path path = Path.of(raw);
        if (path.isAbsolute()) {
            return requireFile(path);
        }

        Path fromWorkingDirectory = Path.of("").toAbsolutePath().resolve(path).normalize();
        if (Files.isRegularFile(fromWorkingDirectory)) {
            return requireFile(fromWorkingDirectory);
        }
        return requireFile(args.exportDir().resolve(path.getFileName()));
    }

    private static Path requireFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Region export not found: " + path.toAbsolutePath());
        }
        if (!path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
            throw new IOException("Pass the .json volume from //rrexport; the .txt file is only a projection");
        }
        return path.toAbsolutePath().normalize();
    }

    private static RegionInput readRegion(Path path) throws IOException {
        JsonObject root;
        try {
            root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Could not read region JSON: " + path, e);
        }
        if (!RegionExport.FORMAT.equals(root.has("format") ? root.get("format").getAsString() : "")) {
            throw new IOException("Unsupported region format; expected " + RegionExport.FORMAT);
        }
        if (!root.has("worldSeed")) {
            throw new IOException("This export has no worldSeed; run //rrexport again with the updated mod");
        }
        String dimension;
        long seed;
        try {
            dimension = root.has("dimension") ? root.get("dimension").getAsString() : "";
            seed = root.get("worldSeed").getAsLong();
        } catch (RuntimeException e) {
            throw new IOException("Region export has invalid seed or dimension metadata", e);
        }
        int[] origin = vector(root.getAsJsonArray("origin"), "origin");
        int[] size = vector(root.getAsJsonArray("size"), "size");
        if (size[0] < 1 || size[1] < 1 || size[2] < 1) {
            throw new IOException("Region size must be positive");
        }
        long volume = (long) size[0] * size[1] * size[2];
        if (volume > RegionExport.MAX_VOLUME) {
            throw new IOException("Region volume " + volume + " exceeds " + RegionExport.MAX_VOLUME);
        }
        BoundingBox box;
        try {
            box = new BoundingBox(
                origin[0],
                origin[1],
                origin[2],
                Math.addExact(origin[0], size[0] - 1),
                Math.addExact(origin[1], size[1] - 1),
                Math.addExact(origin[2], size[2] - 1)
            );
        } catch (ArithmeticException e) {
            throw new IOException("Region bounds overflow", e);
        }
        return new RegionInput(dimension, seed, box);
    }

    private static int[] vector(JsonArray value, String name) throws IOException {
        if (value == null || value.size() != 3) {
            throw new IOException("Region export has an invalid " + name);
        }
        try {
            return new int[] {value.get(0).getAsInt(), value.get(1).getAsInt(), value.get(2).getAsInt()};
        } catch (RuntimeException e) {
            throw new IOException("Region export has an invalid " + name, e);
        }
    }

    private record RegionInput(String dimension, long seed, BoundingBox box) {}
}
