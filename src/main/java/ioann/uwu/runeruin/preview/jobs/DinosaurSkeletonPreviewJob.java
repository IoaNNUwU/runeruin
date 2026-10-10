package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.structures.DinosaurSkeletonGenerator;
import ioann.uwu.runeruin.dimension.structures.DinosaurSkeletonPiece;
import ioann.uwu.runeruin.dimension.structures.DinosaurSkeletonStructure;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.IntBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class DinosaurSkeletonPreviewJob implements PreviewJob {
    private static final int GROUND_Y = Const.LOST_CAVES_Y + 8;
    private static final int GROUND_RADIUS = 28;

    @Override
    public String id() {
        return "dinosaur_skeleton";
    }

    @Override
    public String description() {
        return "Dinosaur skeleton structure, 20-50% of it in the floor. params: roughGround=true";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        boolean roughGround = Boolean.parseBoolean(args.get("roughGround", "false"));
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        IntBinaryOperator floorY = (x, z) -> roughGround
                ? GROUND_Y + (int) Math.round(Math.sin(x * 0.2) * 2.5 + Math.cos(z * 0.15) * 2.5)
                : GROUND_Y;
        for (int x = -GROUND_RADIUS; x <= GROUND_RADIUS; x++) {
            for (int z = -GROUND_RADIUS; z <= GROUND_RADIUS; z++) {
                world.fillBox(new BoundingBox(x, Const.LOST_CAVES_Y, z, x, floorY.applyAsInt(x, z), z),
                        Blocks.DEEPSLATE.defaultBlockState());
            }
        }

        Map<BlockPos, BlockState> bones = DinosaurSkeletonGenerator.generate(seed);
        OptionalInt y = DinosaurSkeletonStructure.buriedOriginY(bones, 0, 0, floorY, world.random());
        if (y.isEmpty()) {
            throw new IOException("No height leaves 20-50% of the skeleton in the floor; in a world it would not generate");
        }
        BlockPos origin = new BlockPos(0, y.getAsInt(), 0);
        PreviewJobs.placePieceAcrossChunks(new DinosaurSkeletonPiece(origin, seed, bones), world);

        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_dinosaur_skeleton"), args.exportDir(), List.of(
                "job: " + id(),
                "seed: " + seed,
                "rough ground: " + roughGround,
                "bone blocks: " + bones.size(),
                "in the floor: " + Math.round(DinosaurSkeletonStructure.buriedShare(bones, origin, floorY) * 100.0) + "%",
                "origin: " + origin.toShortString()
        ));
    }
}
