package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.features.CeilingBallFeature;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class GlowingBallPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "glowing_ball";
    }

    @Override
    public String description() {
        return "CeilingBallFeature with a structured spherical branch web. params: max_radius, max_trunk_length";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        int maxRadius = Math.max(6, args.getInt("max_radius", 10));
        int maxTrunkLength = Math.max(8, args.getInt("max_trunk_length", 20));
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        BlockPos origin = new BlockPos(0, 64, 0);
        int supportExtent = maxRadius + 6;
        world.fillBox(
                new BoundingBox(-supportExtent, origin.getY() + 3, -supportExtent,
                        supportExtent, origin.getY() + 3, supportExtent),
                Blocks.STONE.defaultBlockState()
        );

        boolean placed = PreviewJobs.placeFeature(
                new CeilingBallFeature(),
                new CeilingBallFeature.Config(
                        BlockStateProvider.simple(Blocks.PALE_OAK_WOOD),
                        BlockStateProvider.simple(RRBlocks.LAPIS_LIGHT.get()),
                        ConstantInt.of(maxTrunkLength),
                        ConstantInt.of(maxRadius)
                ),
                origin,
                world,
                null
        );
        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_glowing_ball"), args.exportDir(), List.of(
                "job: " + id(),
                "seed: " + seed,
                "max radius: " + maxRadius,
                "max trunk length: " + maxTrunkLength,
                "placed: " + placed,
                "origin: 0 64 0"
        ));
    }
}
