package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.structures.GiantGobletPiece;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;

public final class GiantGobletPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "giant_goblet";
    }

    @Override
    public String description() {
        return "GiantGobletPiece (bud bowl + stem + wiggly veins + arms + mini goblets). params: height, radius";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        int height = args.getInt("height", PreviewJobs.DEFAULT_HEIGHT);
        int radius = args.getInt("radius", PreviewJobs.DEFAULT_RADIUS);
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        GiantGobletPiece piece = new GiantGobletPiece(0, 0, height, radius, seed);
        PreviewJobs.placePiece(piece, world);
        return PreviewJobs.export(world, piece.getBoundingBox(), args.name("preview_giant_goblet"), args.exportDir(), List.of(
            "job: " + id(),
            "seed: " + seed,
            "height: " + height,
            "radius: " + radius,
            "center: 0 " + Const.LOST_CAVES_Y + " 0"
        ));
    }
}
