package ioann.uwu.runeruin.preview;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.structures.GiantGobletPiece;
import ioann.uwu.runeruin.region.RegionExport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.jspecify.annotations.Nullable;

/**
 * Shared place + export helpers. Named targets live in {@link PreviewCatalog}.
 */
public final class PreviewJobs {
    public static final String DIMENSION = "runeruin:preview";
    public static final int DEFAULT_HEIGHT = Const.DEEP_CAVES_Y - Const.LOST_CAVES_Y;
    public static final int DEFAULT_RADIUS = Math.max(
        DEFAULT_HEIGHT / 2,
        GiantGobletPiece.PILLAR_RADIUS + GiantGobletPiece.RIM_THICKNESS + 3
    );
    public static final long DEFAULT_SEED = 1L;
    public static final int PREVIEW_MAX_VOLUME = 2_000_000;

    private PreviewJobs() {}

    public static void placePiece(StructurePiece piece, PreviewWorld world) {
        WorldGenLevel level = world.asLevel();
        BoundingBox box = piece.getBoundingBox();
        piece.postProcess(
            level,
            null,
            null,
            world.random(),
            box,
            new ChunkPos(box.minX() >> 4, box.minZ() >> 4),
                BlockPos.ZERO
        );
    }

    public static void placePieceAcrossChunks(StructurePiece piece, PreviewWorld world) {
        WorldGenLevel level = world.asLevel();
        BoundingBox box = piece.getBoundingBox();
        int minChunkX = box.minX() >> 4;
        int maxChunkX = box.maxX() >> 4;
        int minChunkZ = box.minZ() >> 4;
        int maxChunkZ = box.maxZ() >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                BoundingBox chunkBB = new BoundingBox(
                        Math.max(box.minX(), chunkX << 4),
                        box.minY(),
                        Math.max(box.minZ(), chunkZ << 4),
                        Math.min(box.maxX(), (chunkX << 4) + 15),
                        box.maxY(),
                        Math.min(box.maxZ(), (chunkZ << 4) + 15)
                );
                piece.postProcess(
                        level,
                        null,
                        null,
                        world.random(),
                        chunkBB,
                        chunkPos,
                        BlockPos.ZERO
                );
            }
        }
    }

    public static <C extends FeatureConfiguration> boolean placeFeature(
        Feature<C> feature,
        C config,
        BlockPos origin,
        PreviewWorld world,
        @Nullable ChunkGenerator chunkGenerator
    ) {
        FeaturePlaceContext<C> ctx = new FeaturePlaceContext<>(
            Optional.empty(),
            world.asLevel(),
            chunkGenerator,
            world.random(),
            origin,
            config
        );
        return feature.place(ctx);
    }

    public static BoundingBox paddedOccupied(PreviewWorld world, int pad) {
        BoundingBox box = world.occupiedBox();
        return new BoundingBox(
            box.minX() - pad,
            box.minY() - pad,
            box.minZ() - pad,
            box.maxX() + pad,
            box.maxY() + pad,
            box.maxZ() + pad
        );
    }

    public static Result export(
        PreviewWorld world,
        BoundingBox box,
        String name,
        Path exportDir,
        List<String> infoLines
    ) throws IOException {
        return export(world, DIMENSION, box, name, exportDir, infoLines);
    }

    public static Result export(
        PreviewWorld world,
        String dimension,
        BoundingBox box,
        String name,
        Path exportDir,
        List<String> infoLines
    ) throws IOException {
        int volume = box.getXSpan() * box.getYSpan() * box.getZSpan();
        if (volume > PREVIEW_MAX_VOLUME) {
            throw new IOException("Preview volume " + volume + " exceeds " + PREVIEW_MAX_VOLUME);
        }

        Files.createDirectories(exportDir);
        List<Path> files = new ArrayList<>();
        String safeName = RegionExport.sanitizeName(name);

        RegionExport.Snapshot full = world.capture(dimension, box);
        RegionExport.Result json = RegionExport.write(full, exportDir, safeName);
        files.add(json.jsonPath());

        int midX = (box.minX() + box.maxX()) / 2;
        int midZ = (box.minZ() + box.maxZ()) / 2;
        int midY = (box.minY() + box.maxY()) / 2;

        files.add(writeSlice(world, dimension, new BoundingBox(midX, box.minY(), box.minZ(), midX, box.maxY(), box.maxZ()), safeName + "_yz", exportDir));
        files.add(writeSlice(world, dimension, new BoundingBox(box.minX(), box.minY(), midZ, box.maxX(), box.maxY(), midZ), safeName + "_xy", exportDir));
        files.add(writeSlice(world, dimension, new BoundingBox(box.minX(), midY, box.minZ(), box.maxX(), midY, box.maxZ()), safeName + "_xz", exportDir));

        Path infoPath = exportDir.resolve(safeName + "_info.txt");
        StringBuilder info = new StringBuilder();
        info.append("# runeruin.preview/1\n");
        info.append("# dimension: ").append(dimension).append('\n');
        info.append("# seed: ").append(world.seed()).append('\n');
        for (String line : infoLines) {
            info.append("# ").append(line).append('\n');
        }
        info.append("# placed: ").append(world.placedCount()).append('\n');
        info.append("# origin: ").append(box.minX()).append(' ').append(box.minY()).append(' ').append(box.minZ()).append('\n');
        info.append("# size: ").append(box.getXSpan()).append(" x ").append(box.getYSpan()).append(" x ").append(box.getZSpan()).append('\n');
        info.append("# counts:\n");
        for (Map.Entry<String, Integer> entry : full.stateCounts().entrySet()) {
            if (entry.getKey().startsWith("minecraft:air")) {
                continue;
            }
            info.append("#   ").append(entry.getKey()).append("  ").append(entry.getValue()).append('\n');
        }
        info.append("# files:\n");
        for (Path file : files) {
            info.append("#   ").append(file.toAbsolutePath()).append('\n');
        }
        Files.writeString(infoPath, info.toString(), StandardCharsets.UTF_8);
        files.add(infoPath);

        RR.LOGGER.info("Preview '{}' wrote {} files to {}", safeName, files.size(), exportDir.toAbsolutePath());
        return new Result(world, full, json.jsonPath(), files);
    }

    private static Path writeSlice(PreviewWorld world, String dimension, BoundingBox slice, String name, Path dir) throws IOException {
        RegionExport.Result result = RegionExport.write(world.capture(dimension, slice), dir, name);
        return result.txtPath() != null ? result.txtPath() : result.jsonPath();
    }

    public record Result(PreviewWorld world, RegionExport.Snapshot volume, Path jsonPath, List<Path> files) {
        public int count(String statePrefix) {
            return volume.countMatching(s -> s.startsWith(statePrefix));
        }
    }
}
