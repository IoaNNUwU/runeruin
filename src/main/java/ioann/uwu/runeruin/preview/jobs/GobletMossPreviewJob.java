package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.features.GobletMossPatchFeature;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class GobletMossPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "goblet_moss";
    }

    @Override
    public String description() {
        return "GobletMossPatchFeature on a bud slab (full-column moss + irregular hanging strands). params: none";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        world.fillBox(new BoundingBox(-6, 70, -6, 6, 74, 6), RRBlocks.GIANT_GOBLET_BUD.get().defaultBlockState());
        for (int y = 68; y <= 74; y++) {
            world.set(new BlockPos(2, y, 0), RRBlocks.GIANT_GOBLET_STEM.get().defaultBlockState());
        }

        VegetationPatchConfiguration config = new VegetationPatchConfiguration(
                HolderSet.direct(RRBlocks.GIANT_GOBLET_BUD.get().builtInRegistryHolder()),
                BlockStateProvider.simple(Blocks.MOSS_BLOCK),
                PlacementUtils.inlinePlaced(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MOSS_CARPET))
                ),
                CaveSurface.FLOOR,
                ConstantInt.of(32),
                0.0F,
                5,
                0.8F,
                UniformInt.of(4, 7),
                0.3F
        );
        PreviewJobs.placeFeature(new GobletMossPatchFeature(), config, new BlockPos(0, 75, 0), world, null);

        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_goblet_moss"), args.exportDir(), List.of(
                "job: " + id(),
                "seed: " + seed,
                "origin: 0 75 0"
        ));
    }
}
