package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.features.WallMushroomFeature;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class AshenMushroomPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "ashen_mushroom";
    }

    @Override
    public String description() {
        return "WallMushroomFeature cap. params: radius (1-7)";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        int radius = clamp(args.getInt("radius", 3), 1, 7);
        int diameter = radius * 2 + 1;
        long seed = args.seed();
        BlockPos origin = new BlockPos(0, 64, 0);
        PreviewWorld world = PreviewWorld.create(seed);
        boolean placed = PreviewJobs.placeFeature(
                new WallMushroomFeature(),
                new WallMushroomFeature.Config(
                        BlockStateProvider.simple(RRBlocks.ASHEN_MUSHROOM_BLOCK.get()),
                        ConstantInt.of(diameter)
                ),
                origin,
                world,
                null
        );

        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_ashen_mushroom"), args.exportDir(), List.of(
                "job: " + id(),
                "seed: " + seed,
                "radius: " + radius,
                "diameter: " + diameter,
                "placed: " + placed,
                "origin: 0 64 0"
        ));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
