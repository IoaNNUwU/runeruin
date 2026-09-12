package ioann.uwu.runeruin.preview;

import ioann.uwu.runeruin.preview.jobs.BoulderPreviewJob;
import ioann.uwu.runeruin.preview.jobs.GiantGobletPreviewJob;
import ioann.uwu.runeruin.preview.jobs.GlowingBallPreviewJob;
import ioann.uwu.runeruin.preview.jobs.GobletMossPreviewJob;
import ioann.uwu.runeruin.preview.jobs.MonolithPreviewJob;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PreviewCatalog {
    public static final String LIST_NAME = "preview_jobs";

    private static final Map<String, PreviewJob> JOBS = new LinkedHashMap<>();

    static {
        register(new GiantGobletPreviewJob());
        register(new GlowingBallPreviewJob());
        register(new GobletMossPreviewJob());
        register(new BoulderPreviewJob());
        register(new MonolithPreviewJob());
    }

    private PreviewCatalog() {}

    public static void register(PreviewJob job) {
        PreviewJob previous = JOBS.putIfAbsent(job.id(), job);
        if (previous != null) {
            throw new IllegalStateException("Duplicate preview job '" + job.id() + "'");
        }
    }

    public static PreviewJob require(String id) {
        PreviewJob job = JOBS.get(id);
        if (job == null) {
            throw new IllegalArgumentException("Unknown preview job '" + id + "'. Known: " + String.join(", ", JOBS.keySet()));
        }
        return job;
    }

    public static boolean exists(String id) {
        return JOBS.containsKey(id);
    }

    public static Collection<PreviewJob> all() {
        return JOBS.values();
    }

    public static String listText() {
        StringBuilder out = new StringBuilder();
        out.append("# runeruin.preview/jobs\n");
        out.append("# pick with:  .\\gradlew.bat runPreview -Ppreview=<id>\n");
        out.append("# or in-game: /rrpreview <id> [seed] [name] [k=v]...\n");
        out.append("#\n");
        int width = 0;
        for (PreviewJob job : JOBS.values()) {
            width = Math.max(width, job.id().length());
        }
        for (PreviewJob job : JOBS.values()) {
            out.append(String.format("%-" + width + "s  %s%n", job.id(), job.description()));
        }
        return out.toString();
    }

    public static Path writeList(Path exportDir) throws IOException {
        Files.createDirectories(exportDir);
        Path path = exportDir.resolve(LIST_NAME + ".txt");
        Files.writeString(path, listText(), StandardCharsets.UTF_8);
        return path;
    }
}
