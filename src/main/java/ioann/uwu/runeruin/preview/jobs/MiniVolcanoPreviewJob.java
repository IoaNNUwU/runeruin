package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.features.MiniVolcanoFeature;
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

public final class MiniVolcanoPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "mini_volcano";
    }

    @Override
    public String description() {
        return "Tuff mini volcano with a hot pool. params: radius";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        int radius = args.getInt("radius", 8);
        PreviewWorld world = PreviewWorld.create(args.seed());
        PreviewJobs.placeFeature(
                new MiniVolcanoFeature(),
                new MiniVolcanoFeature.Config(BlockStateProvider.simple(Blocks.TUFF), ConstantInt.of(radius)),
                new BlockPos(0, 64, 0), world, null
        );
        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_mini_volcano"), args.exportDir(), List.of(
                "job: " + id(), "seed: " + args.seed(), "radius: " + radius
        ));
    }
}
