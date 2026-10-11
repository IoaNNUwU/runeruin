package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRPlacedFeatures;
import ioann.uwu.runeruin.dimension.structures.GiantGobletPiece;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

public final class GiantGobletPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "giant_goblet";
    }

    @Override
    public String description() {
        return "GiantGobletPiece (bud bowl + stem + wiggly veins + arms + mini goblets). params: height, radius, "
            + "moss=1 (goblet moss patches and floating moss, placed chunk by chunk as in the world)";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        return run(args, null);
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, @Nullable MinecraftServer server) throws IOException {
        int height = args.getInt("height", PreviewJobs.DEFAULT_HEIGHT);
        int radius = args.getInt("radius", PreviewJobs.DEFAULT_RADIUS);
        boolean moss = args.getInt("moss", 0) > 0;
        long seed = args.seed();
        PreviewWorld world = server == null ? PreviewWorld.create(seed) : PreviewWorld.create(seed, server.overworld());
        GiantGobletPiece piece = new GiantGobletPiece(0, 0, height, radius, seed);
        PreviewJobs.placePiece(piece, world);
        if (moss) {
            if (server == null) {
                throw new IOException("moss needs the server registries");
            }
            decorate(world, server, piece.getBoundingBox(), RRPlacedFeatures.GOBLET_MOSS_PATCH);
            decorate(world, server, piece.getBoundingBox(), RRPlacedFeatures.FLOATING_MOSS_PATCH);
        }
        return PreviewJobs.export(world, piece.getBoundingBox(), args.name("preview_giant_goblet"), args.exportDir(), List.of(
            "job: " + id(),
            "seed: " + seed,
            "height: " + height,
            "radius: " + radius,
            "moss: " + moss,
            "center: 0 " + Const.LOST_CAVES_Y + " 0"
        ));
    }

    /** One placed feature in every chunk of the box, with its own placement; the preview has no biomes to filter by. */
    private static void decorate(PreviewWorld world, MinecraftServer server, BoundingBox box, ResourceKey<PlacedFeature> key) {
        PlacedFeature placed = server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(key).value();
        ChunkGenerator generator = server.overworld().getChunkSource().getGenerator();
        PlacementContext context = new PlacementContext(world.asLevel(), generator, Optional.empty());
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                Stream<BlockPos> positions = Stream.of(new BlockPos(chunkX << 4, 0, chunkZ << 4));
                for (PlacementModifier modifier : placed.placement()) {
                    if (!(modifier instanceof BiomeFilter)) {
                        positions = positions.flatMap(pos -> modifier.getPositions(context, world.random(), pos));
                    }
                }
                positions.forEach(pos -> placed.feature().value().place(world.asLevel(), generator, world.random(), pos));
            }
        }
    }
}
