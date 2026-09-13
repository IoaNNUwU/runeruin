package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.structures.BaobabPiece;
import ioann.uwu.runeruin.dimension.structures.BaobabTreeGenerator;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.Blocks;

public final class BaobabPreviewJob implements PreviewJob {
    @Override
    public String id() {
        return "baobab";
    }

    @Override
    public String description() {
        return "Baobab structure with grounded buttress roots and curved branch crowns. params: radius (20-40), groundGap (0-8), cover=true";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        int radius = Math.max(20, Math.min(40, args.getInt("radius", 28)));
        int groundGap = Math.max(0, Math.min(8, args.getInt("groundGap", 0)));
        boolean cover = Boolean.parseBoolean(args.get("cover", "false"));
        boolean roughGround = Boolean.parseBoolean(args.get("roughGround", "false"));
        long seed = args.seed();
        PreviewWorld world = PreviewWorld.create(seed);
        int groundRadius = (int) Math.ceil(radius * 0.4);
        int groundY = 64 - groundGap;
        if (roughGround) {
            for (int x = -groundRadius; x <= groundRadius; x++) {
                for (int z = -groundRadius; z <= groundRadius; z++) {
                    int offset = (int) Math.round(
                            Math.sin(x * 0.45) * 4.0 + Math.cos(z * 0.37) * 4.0 - 4.0
                                    + Math.sin((x + z) * 0.18) * 2.0);
                    int surfaceY = groundY + offset;
                    world.fillBox(new BoundingBox(x, groundY - 16, z, x, surfaceY, z),
                            Blocks.STONE.defaultBlockState());
                }
            }
        } else {
            world.fillBox(new BoundingBox(-groundRadius, groundY, -groundRadius, groundRadius, groundY, groundRadius),
                    Blocks.STONE.defaultBlockState());
        }
        if (cover) {
            int flowerY = groundY;
            if (roughGround) {
                flowerY += (int) Math.round(Math.sin(0.0) * 4.0 + Math.cos(0.0) * 4.0 - 4.0);
            }
            world.fillBox(new BoundingBox(-2, flowerY + 1, -2, 2, flowerY + 1, 2),
                    Blocks.PINK_PETALS.defaultBlockState());
            world.fillBox(new BoundingBox(-1, flowerY + 2, -1, 1, flowerY + 2, 1),
                    Blocks.TALL_GRASS.defaultBlockState());
        }

        int[] groundHeights = new int[BaobabTreeGenerator.groundProfileLength()];
        for (int dx = -BaobabTreeGenerator.MAX_HORIZONTAL_EXTENT;
             dx <= BaobabTreeGenerator.MAX_HORIZONTAL_EXTENT; dx++) {
            for (int dz = -BaobabTreeGenerator.MAX_HORIZONTAL_EXTENT;
                 dz <= BaobabTreeGenerator.MAX_HORIZONTAL_EXTENT; dz++) {
                int surfaceY = groundY;
                if (roughGround && Math.abs(dx) <= groundRadius && Math.abs(dz) <= groundRadius) {
                    surfaceY += (int) Math.round(
                            Math.sin(dx * 0.45) * 4.0 + Math.cos(dz * 0.37) * 4.0 - 4.0
                                    + Math.sin((dx + dz) * 0.18) * 2.0);
                }
                groundHeights[BaobabTreeGenerator.groundProfileIndex(dx, dz)] = surfaceY;
            }
        }
        int height = BaobabTreeGenerator.sampleHeight(radius, world.random());
        long treeSeed = world.random().nextLong();
        PreviewJobs.placePieceAcrossChunks(new BaobabPiece(
                0, 0, radius, height, treeSeed, groundHeights), world);

        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1), args.name("preview_baobab"), args.exportDir(), List.of(
                "job: " + id(),
                "seed: " + seed,
                "radius: " + radius,
                "ground gap: " + groundGap,
                "rough ground: " + roughGround,
                "flower cover: " + cover,
                "height: " + height + " blocks (maximum 40)",
                "origin: 0 " + (groundY + 1) + " 0"
        ));
    }
}
