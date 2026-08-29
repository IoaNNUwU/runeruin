package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.features.MonolithFeature;
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

public final class MonolithPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "monolith";
    }

    @Override
    public String description() {
        return "MonolithFeature cylinder. params: radius";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        int radius = args.getInt("radius", 5);
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        BlockPos origin = new BlockPos(0, 64, 0);
        MonolithFeature feature = new MonolithFeature();
        PreviewJobs.placeFeature(
            feature,
            new MonolithFeature.Config(
                BlockStateProvider.simple(Blocks.STONE),
                BlockStateProvider.simple(Blocks.DEEPSLATE),
                ConstantInt.of(radius),
                ConstantInt.of(radius),
                ConstantInt.of(0),
                ConstantInt.of(0)
            ),
            origin,
            world,
            null
        );
        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_monolith"), args.exportDir(), List.of(
            "job: " + id(),
            "seed: " + seed,
            "radius: " + radius,
            "origin: 0 64 0"
        ));
    }
}
