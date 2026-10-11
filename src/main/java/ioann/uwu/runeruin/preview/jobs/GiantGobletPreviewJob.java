package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRConfiguredFeatures;
import ioann.uwu.runeruin.dimension.placements.GobletUnderwaterPlacement;
import ioann.uwu.runeruin.dimension.structures.GiantGobletPiece;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.jspecify.annotations.Nullable;

public final class GiantGobletPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "giant_goblet";
    }

    @Override
    public String description() {
        return "GiantGobletPiece (bud bowl + stem + wiggly veins + arms + mini goblets). params: height, radius, "
            + "floating_moss=<columns tried for a floating moss patch>";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        return run(args, null);
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, @Nullable MinecraftServer server) throws IOException {
        int height = args.getInt("height", PreviewJobs.DEFAULT_HEIGHT);
        int radius = args.getInt("radius", PreviewJobs.DEFAULT_RADIUS);
        int floatingMoss = args.getInt("floating_moss", 0);
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        GiantGobletPiece piece = new GiantGobletPiece(0, 0, height, radius, seed);
        PreviewJobs.placePiece(piece, world);
        if (floatingMoss > 0) {
            if (server == null) {
                throw new IOException("floating_moss needs the server registries");
            }
            placeFloatingMoss(world, server, GiantGobletPiece.generationRadius(radius), floatingMoss);
        }
        return PreviewJobs.export(world, piece.getBoundingBox(), args.name("preview_giant_goblet"), args.exportDir(), List.of(
            "job: " + id(),
            "seed: " + seed,
            "height: " + height,
            "radius: " + radius,
            "floating_moss: " + floatingMoss,
            "center: 0 " + Const.LOST_CAVES_Y + " 0"
        ));
    }

    /** Random columns over the goblet, as the placed feature tries them: only those over cup water get a patch. */
    private static void placeFloatingMoss(PreviewWorld world, MinecraftServer server, int extent, int columns) {
        ConfiguredFeature<?, ?> patch = server.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
            .getOrThrow(RRConfiguredFeatures.FLOATING_MOSS_PATCH).value();
        ChunkGenerator generator = server.overworld().getChunkSource().getGenerator();
        PlacementContext context = new PlacementContext(world.asLevel(), generator, Optional.empty());
        RandomSource random = world.random();
        for (int i = 0; i < columns; i++) {
            BlockPos column = new BlockPos(random.nextIntBetweenInclusive(-extent, extent), 0, random.nextIntBetweenInclusive(-extent, extent));
            new GobletUnderwaterPlacement().getPositions(context, random, column)
                .forEach(origin -> patch.place(world.asLevel(), generator, random, origin));
        }
    }
}
