package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.features.GlowingMushroomFeature;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class GlowingMushroomPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "glowing_mushroom";
    }

    @Override
    public String description() {
        return "Hollow tilted caps and paired mushrooms. params: cap_diameter, stem_height, waist_diameter, uneven_support";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        long seed = args.seed();
        int capDiameter = clamp(args.getInt("cap_diameter", 14), 12, 16);
        int stemHeight = clamp(args.getInt("stem_height", 7), 4, 9);
        int waistDiameter = clamp(args.getInt("waist_diameter", 2), 2, 3);
        boolean unevenSupport = Boolean.parseBoolean(args.get("uneven_support", "false"));
        BlockPos origin = new BlockPos(0, 64, 0);
        PreviewWorld world = PreviewWorld.create(seed);
        if (unevenSupport) {
            world.fillBox(
                    new BoundingBox(origin.getX() - 2, origin.getY() - 2, origin.getZ() - 2,
                            origin.getX() + 2, origin.getY() - 2, origin.getZ() + 2),
                    Blocks.STONE.defaultBlockState()
            );
            world.fillBox(
                    new BoundingBox(origin.getX() - 1, origin.getY() - 1, origin.getZ() - 1,
                            origin.getX() + 1, origin.getY() - 1, origin.getZ() + 1),
                    Blocks.STONE.defaultBlockState()
            );
        } else {
            world.fillBox(
                    new BoundingBox(origin.getX() - 2, origin.getY() - 1, origin.getZ() - 2,
                            origin.getX() + 2, origin.getY() - 1, origin.getZ() + 2),
                    Blocks.STONE.defaultBlockState()
            );
        }

        boolean placed = PreviewJobs.placeFeature(
                new GlowingMushroomFeature(),
                new GlowingMushroomFeature.Config(
                        BlockStateProvider.simple(RRBlocks.GLOWING_MUSHROOM_CAP.get()),
                        BlockStateProvider.simple(RRBlocks.GLOWING_MUSHROOM_STEM.get()),
                        ConstantInt.of(capDiameter),
                        ConstantInt.of(stemHeight),
                        ConstantInt.of(waistDiameter)
                ),
                origin,
                world,
                null
        );

        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_glowing_mushroom"), args.exportDir(), List.of(
                "job: " + id(),
                "seed: " + seed,
                "cap diameter: " + capDiameter,
                "stem height: " + stemHeight,
                "waist diameter: " + waistDiameter,
                "support: " + (unevenSupport ? "raised center and one-block lower rim" : "5x5 stone pad"),
                "placed: " + placed,
                "origin: 0 64 0"
        ));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
