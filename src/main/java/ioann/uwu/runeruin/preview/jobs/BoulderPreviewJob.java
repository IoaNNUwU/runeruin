package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.features.BoulderFeature;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class BoulderPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "boulder";
    }

    @Override
    public String description() {
        return "BoulderFeature sphere. params: radius";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        int radius = args.getInt("radius", 8);
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        BlockPos origin = new BlockPos(0, 64, 0);
        BoulderFeature feature = new BoulderFeature();
        PreviewJobs.placeFeature(
            feature,
            new BoulderFeature.Config(
                BlockStateProvider.simple(Blocks.STONE),
                BlockStateProvider.simple(Blocks.MOSS_BLOCK),
                ConstantInt.of(radius),
                ConstantInt.of(radius)
            ),
            origin,
            world,
            null
        );
        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_boulder"), args.exportDir(), List.of(
            "job: " + id(),
            "seed: " + seed,
            "radius: " + radius,
            "origin: 0 64 0"
        ));
    }
}
