package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.CaveMushroomKind;
import ioann.uwu.runeruin.dimension.features.CaveMushroomFeature;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class CaveMushroomPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "cave_mushroom";
    }

    @Override
    public String description() {
        return "Big cave mushroom on a stone floor (or hanging from a ceiling). params: kind (" + Arrays.stream(CaveMushroomKind.values())
                .map(kind -> kind.name().toLowerCase(Locale.ROOT)).toList() + "), hanging";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        long seed = args.seed();
        CaveMushroomKind kind = CaveMushroomKind.valueOf(args.get("kind", "brown_dome").toUpperCase(Locale.ROOT));
        boolean hanging = Boolean.parseBoolean(args.get("hanging", "false"));
        BlockPos origin = new BlockPos(0, 64, 0);
        PreviewWorld world = PreviewWorld.create(seed);
        int supportY = origin.getY() + (hanging ? 1 : -1);
        world.fillBox(new BoundingBox(-4, supportY, -4, 4, supportY, 4), Blocks.STONE.defaultBlockState());

        boolean placed = PreviewJobs.placeFeature(new CaveMushroomFeature(), kind.config(hanging), origin, world, null);

        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_cave_mushroom"), args.exportDir(), List.of(
                "job: " + id(),
                "seed: " + seed,
                "kind: " + kind,
                "hanging: " + hanging,
                "placed: " + placed,
                "origin: 0 64 0"
        ));
    }
}
