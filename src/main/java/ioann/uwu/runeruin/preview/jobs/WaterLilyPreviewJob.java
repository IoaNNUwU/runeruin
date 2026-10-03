package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.blocks.WaterLilyRootBlock;
import ioann.uwu.runeruin.dimension.features.WaterLilyFeature;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class WaterLilyPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "water_lily";
    }

    @Override
    public String description() {
        return "Water lily on a pond: natural generation, or bonemeal=N growth steps from a bare root";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        int bonemeal = args.getInt("bonemeal", -1);
        PreviewWorld world = PreviewWorld.create(args.seed());
        world.fillBox(new BoundingBox(-6, 62, -6, 6, 63, 6), Blocks.WATER.defaultBlockState());
        BlockPos root = new BlockPos(0, 63, 0);
        if (bonemeal < 0) {
            PreviewJobs.placeFeature(new WaterLilyFeature(), NoneFeatureConfiguration.INSTANCE, root.above(), world, null);
        } else {
            world.set(root, RRBlocks.WATER_LILY_ROOT.get().defaultBlockState());
            for (int step = 0; step < bonemeal; step++) {
                WaterLilyRootBlock.grow(world.asLevel(), root, world.random());
            }
        }
        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_water_lily"), args.exportDir(), List.of(
                "job: " + id(), "seed: " + args.seed(), "bonemeal: " + bonemeal,
                "leaves: " + WaterLilyRootBlock.attachedLeaves(world.asLevel(), root)
        ));
    }
}
