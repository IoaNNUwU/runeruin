package ioann.uwu.runeruin.preview;

import java.io.IOException;
import net.minecraft.server.MinecraftServer;

/**
 * One named preview target. Add a class, register it in {@link PreviewCatalog},
 * then pick it with {@code -Ppreview=<id>} or {@code /rrpreview <id>}.
 */
public interface PreviewJob {
    String id();

    String description();

    PreviewJobs.Result run(PreviewArgs args) throws IOException;

    default PreviewJobs.Result run(PreviewArgs args, MinecraftServer server) throws IOException {
        return run(args);
    }
}
